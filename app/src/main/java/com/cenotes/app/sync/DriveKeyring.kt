package com.cenotes.app.sync

import android.util.Base64
import org.json.JSONObject
import javax.crypto.AEADBadTagException

/**
 * EXPERIMENT (branch experimento-drive): the key header of the Drive sync, stored as `keyring.json` in the app folder.
 *
 * One random 256-bit data key (DEK) encrypts every data file. The keyring holds the DEK wrapped twice:
 * - by a key derived from the passphrase (PBKDF2, fresh salt each time the passphrase changes), and
 * - by a random 256-bit recovery key, shown once when the keyring is created.
 *
 * There is exactly one keyring per account: [create] is only called when none exists. Changing the passphrase re-wraps
 * the SAME DEK and raises [generation] by one, so the data files are not re-encrypted. A wrong passphrase simply fails
 * to unwrap (GCM tag) and changes nothing.
 */
class Keyring(
    val generation: Int,
    val iterations: Int,
    val salt: ByteArray,
    val wrapPass: ByteArray,
    val wrapRecovery: ByteArray,
) {
    fun toBytes(): ByteArray = JSONObject()
        .put("v", 1)
        .put("generation", generation)
        .put("kdf", "pbkdf2-sha256")
        .put("iter", iterations)
        .put("salt", b64(salt))
        .put("wrapPass", b64(wrapPass))
        .put("wrapRecovery", b64(wrapRecovery))
        .toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun parse(bytes: ByteArray): Keyring {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            require(o.getInt("v") == 1 && o.getString("kdf") == "pbkdf2-sha256") { "Unknown keyring format" }
            return Keyring(
                o.getInt("generation"), o.getInt("iter"), unb64(o.getString("salt")),
                unb64(o.getString("wrapPass")), unb64(o.getString("wrapRecovery")),
            )
        }

        private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
        private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
    }
}

object DriveKeyring {
    const val FileName = "keyring.json"
    private fun passAad(generation: Int) = "keyring:pass:$generation".toByteArray()
    private val recoveryAad = "keyring:recovery".toByteArray()

    class Created(val keyring: Keyring, val dek: ByteArray, val recoveryKey: String)

    /** First time only (the caller has checked that no keyring exists). */
    fun create(passphrase: CharArray): Created {
        val dek = DriveCrypto.randomBytes(32)
        val recovery = DriveCrypto.randomBytes(32)
        val salt = DriveCrypto.randomBytes(16)
        val kek = DriveCrypto.deriveKey(passphrase, salt)
        val ring = Keyring(
            generation = 1,
            iterations = DriveCrypto.Iterations,
            salt = salt,
            wrapPass = DriveCrypto.encrypt(kek, dek, passAad(1)),
            wrapRecovery = DriveCrypto.encrypt(recovery, dek, recoveryAad),
        )
        return Created(ring, dek, recovery.toHex().chunked(8).joinToString("-"))
    }

    /** The DEK, or null when [secret] is neither the passphrase nor the recovery key. Never changes anything. */
    fun unlock(ring: Keyring, secret: CharArray): ByteArray? {
        try {
            val kek = DriveCrypto.deriveKey(secret, ring.salt, ring.iterations)
            return DriveCrypto.decrypt(kek, ring.wrapPass, passAad(ring.generation))
        } catch (_: AEADBadTagException) {
        }
        val recovery = parseRecoveryKey(String(secret)) ?: return null
        return try {
            DriveCrypto.decrypt(recovery, ring.wrapRecovery, recoveryAad)
        } catch (_: AEADBadTagException) {
            null
        }
    }

    /** Same DEK, new passphrase, generation + 1. The recovery wrap stays as it was. */
    fun changePassphrase(ring: Keyring, dek: ByteArray, newPassphrase: CharArray): Keyring {
        val salt = DriveCrypto.randomBytes(16)
        val kek = DriveCrypto.deriveKey(newPassphrase, salt)
        val generation = ring.generation + 1
        return Keyring(generation, DriveCrypto.Iterations, salt, DriveCrypto.encrypt(kek, dek, passAad(generation)), ring.wrapRecovery)
    }

    private fun parseRecoveryKey(text: String): ByteArray? {
        val hex = text.filter { it != '-' && !it.isWhitespace() }
        if (hex.length != 64 || !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return ByteArray(32) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
