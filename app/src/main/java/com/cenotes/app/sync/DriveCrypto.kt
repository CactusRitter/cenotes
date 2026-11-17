package com.cenotes.app.sync

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * EXPERIMENT (branch experimento-drive): client-side encryption for the Drive sync.
 *
 * AES-256-GCM. A random 256-bit data key will encrypt the files; it is stored wrapped twice in the manifest: by a key
 * derived from the passphrase (PBKDF2-HMAC-SHA256, [Iterations]; Argon2id would need a library) and by a random recovery
 * key shown once. For the first milestone only [seal] and [open] with a passphrase are used.
 */
object DriveCrypto {
    const val Iterations = 600_000
    private const val KeyBits = 256
    private const val IvBytes = 12
    private const val TagBits = 128
    private val random = SecureRandom()

    fun randomBytes(n: Int) = ByteArray(n).also(random::nextBytes)

    fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = Iterations): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KeyBits)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** `iv || ciphertext+tag`. [aad] is authenticated but not stored (e.g. the file name, so files cannot be swapped). */
    fun encrypt(key: ByteArray, plain: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        val iv = randomBytes(IvBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TagBits, iv))
        c.updateAAD(aad)
        return iv + c.doFinal(plain)
    }

    /** Throws [javax.crypto.AEADBadTagException] when the key is wrong or the data was changed. */
    fun decrypt(key: ByteArray, blob: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        require(blob.size > IvBytes + TagBits / 8) { "Encrypted file too short" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TagBits, blob.copyOfRange(0, IvBytes)))
        c.updateAAD(aad)
        return c.doFinal(blob, IvBytes, blob.size - IvBytes)
    }

    /** A self-contained test envelope: salt and iterations travel with it, the key never does. */
    fun seal(passphrase: CharArray, plain: ByteArray, aad: ByteArray): ByteArray {
        val salt = randomBytes(16)
        val key = deriveKey(passphrase, salt)
        val body = encrypt(key, plain, aad)
        return JSONObject()
            .put("v", 1)
            .put("kdf", "pbkdf2-sha256")
            .put("iter", Iterations)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(body, Base64.NO_WRAP))
            .toString().toByteArray(Charsets.UTF_8)
    }

    fun open(passphrase: CharArray, envelope: ByteArray, aad: ByteArray): ByteArray {
        val o = JSONObject(String(envelope, Charsets.UTF_8))
        require(o.getInt("v") == 1 && o.getString("kdf") == "pbkdf2-sha256") { "Unknown format" }
        val key = deriveKey(passphrase, Base64.decode(o.getString("salt"), Base64.NO_WRAP), o.getInt("iter"))
        return decrypt(key, Base64.decode(o.getString("data"), Base64.NO_WRAP), aad)
    }
}
