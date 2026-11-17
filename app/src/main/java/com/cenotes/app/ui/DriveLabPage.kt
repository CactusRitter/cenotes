package com.cenotes.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cenotes.app.sync.AuthStep
import com.cenotes.app.sync.DriveApi
import com.cenotes.app.sync.DriveAuth
import com.cenotes.app.sync.DriveCrypto
import com.cenotes.app.CenotesApp
import com.cenotes.app.sync.DriveKeyring
import com.cenotes.app.sync.DriveSync
import com.cenotes.app.sync.Keyring
import com.cenotes.app.ui.theme.VisibilityIcon
import com.cenotes.app.ui.theme.VisibilityOffIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TestFile = "lab-test.enc"
private const val MinPassphrase = 8

/** What the lab keeps in memory (not on disk): it survives a recomposition or rotation, not the process. */
private object LabSession {
    var token by mutableStateOf<String?>(null)
    var keyring by mutableStateOf<Keyring?>(null)
    var keyringId by mutableStateOf<String?>(null)
    var checked by mutableStateOf(false)
    var dek by mutableStateOf<ByteArray?>(null)
}

/**
 * EXPERIMENT (branch experimento-drive): milestone 2. Sign in (drive.appdata only), then the key header: ONE keyring per
 * account, created once with a passphrase typed twice (plus a recovery key shown once), unlocked on any device, and the
 * passphrase changeable by re-wrapping the same data key. A data test file is encrypted with the data key.
 * Not part of any release.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriveLabPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LabSession
    var pass by rememberSaveable { mutableStateOf("") }
    var pass2 by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    var changing by rememberSaveable { mutableStateOf(false) }
    var log by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var letter by rememberSaveable { mutableStateOf("A") }
    val app = context.applicationContext as CenotesApp
    fun say(text: String) {
        log = (log + text + "\n").takeLast(3000)
        android.widget.Toast.makeText(context, text.take(160), android.widget.Toast.LENGTH_LONG).show()
    }

    /** Reads the keyring from Drive into the session. Locks if the passphrase was changed elsewhere. */
    suspend fun refresh(api: DriveApi) {
        val f = api.list().firstOrNull { it.name == DriveKeyring.FileName }
        val remote = f?.let { Keyring.parse(api.get(it.id)) }
        val local = s.keyring
        s.keyringId = f?.id
        s.keyring = remote
        s.checked = true
        if (remote == null) {
            s.dek = null
            say("No keyring in Drive yet: create the passphrase.")
        } else if (s.dek != null && local != null && remote.generation != local.generation) {
            s.dek = null
            say("The passphrase was changed on another device (generation ${remote.generation}). Type the new one and unlock.")
        } else if (local == null) {
            say("Keyring found (generation ${remote.generation}). Unlock it with your passphrase.")
        }
    }

    fun drive(block: suspend (DriveApi) -> Unit) {
        val t = s.token ?: return say("Sign in first")
        if (busy) return
        busy = true
        scope.launch {
            try { block(DriveApi(t)) } catch (e: Exception) { say("ERROR ${e.javaClass.simpleName}: ${e.message}") }
            busy = false
        }
    }

    fun gotToken(token: String) {
        s.token = token
        say("Signed in (token ${token.length} chars)")
        drive { refresh(it) }
    }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        runCatching { DriveAuth.fromIntent(context, r.data) }
            .onSuccess { if (it is AuthStep.Token) gotToken(it.accessToken) else say("Still no token") }
            .onFailure { say("Consent failed: ${it.javaClass.simpleName} ${it.message}") }
    }

    val hidden = if (show) VisualTransformation.None else PasswordVisualTransformation()
    @Composable
    fun field(value: String, label: String, onChange: (String) -> Unit) = OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, visualTransformation = hidden,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) VisibilityOffIcon else VisibilityIcon, contentDescription = null) } },
    )

    val ring = s.keyring
    val unlocked = s.dek != null
    SettingsCard {
        Text("Drive lab (experiment)", style = MaterialTheme.typography.titleSmall)
        Text(
            "drive.appdata only. One keyring (passphrase) per account; data files are encrypted with its data key.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val status = when {
            s.token == null -> "Not signed in"
            !s.checked -> "Looking for the keyring…"
            ring == null -> "No keyring yet"
            unlocked -> "Unlocked (generation ${ring.generation})"
            else -> "Locked (generation ${ring.generation})"
        }
        Text(status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        if (s.token != null && s.checked) {
            if (ring == null) {
                field(pass, "New passphrase (min $MinPassphrase)") { pass = it }
                field(pass2, "Repeat the passphrase") { pass2 = it }
            } else if (!unlocked) {
                field(pass, "Passphrase (or recovery key)") { pass = it }
            }
            if (unlocked) {
                OutlinedTextField(value = letter, onValueChange = { letter = it.take(1).uppercase() }, label = { Text("Lab note letter (A-E)") }, singleLine = true)
            }
            if (unlocked && !changing) {
                field(newPass, "Lab: phrase for the simulated change") { newPass = it }
            }
            if (unlocked && changing) {
                field(pass, "Current passphrase") { pass = it }
                field(newPass, "New passphrase (min $MinPassphrase)") { newPass = it }
                field(pass2, "Repeat the new passphrase") { pass2 = it }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("Sign in", s.token != null, enabled = !busy) {
                scope.launch {
                    try {
                        when (val step = DriveAuth.authorize(context)) {
                            is AuthStep.Token -> gotToken(step.accessToken)
                            is AuthStep.NeedsConsent -> { say("Consent screen…"); consentLauncher.launch(IntentSenderRequest.Builder(step.intentSender).build()) }
                        }
                    } catch (e: Exception) { say("Sign-in failed: ${e.javaClass.simpleName} ${e.message}") }
                }
            }
            ChoiceChip("Check keyring", false, enabled = s.token != null && !busy) { drive { refresh(it) } }

            if (s.token != null && s.checked && ring == null) {
                ChoiceChip("Create passphrase", false, enabled = !busy && pass.length >= MinPassphrase && pass == pass2) {
                    drive { api ->
                        refresh(api) // never create a second keyring: look again right before
                        if (s.keyring != null) return@drive say("A keyring already exists: unlock it instead. Nothing was created.")
                        val created = withContext(Dispatchers.Default) { DriveKeyring.create(pass.toCharArray()) }
                        s.keyringId = api.put(DriveKeyring.FileName, created.keyring.toBytes())
                        s.keyring = created.keyring
                        s.dek = created.dek
                        pass = ""; pass2 = ""
                        say("Keyring created (generation 1). Unlocked.\nRECOVERY KEY (write it down, shown once):\n${created.recoveryKey}")
                    }
                }
            }
            if (s.token != null && ring != null && !unlocked) {
                ChoiceChip("Unlock", false, enabled = !busy && pass.isNotEmpty()) {
                    drive { api ->
                        refresh(api)
                        val current = s.keyring ?: return@drive
                        val dek = withContext(Dispatchers.Default) { DriveKeyring.unlock(current, pass.toCharArray()) }
                        if (dek == null) say("Wrong passphrase. Nothing was changed.")
                        else { s.dek = dek; pass = ""; say("Unlocked (generation ${current.generation}).") }
                    }
                }
            }
            if (unlocked) {
                ChoiceChip(if (changing) "Cancel change" else "Change passphrase", changing, enabled = !busy) {
                    changing = !changing; pass = ""; pass2 = ""; newPass = ""
                }
                if (changing) {
                    ChoiceChip("Save new passphrase", false, enabled = !busy && pass.isNotEmpty() && newPass.length >= MinPassphrase && newPass == pass2) {
                        drive { api ->
                            val before = s.keyring ?: return@drive
                            val dek = s.dek ?: return@drive
                            // The current passphrase must be right (the recovery key does not count here).
                            val ok = withContext(Dispatchers.Default) {
                                runCatching { DriveCrypto.decrypt(DriveCrypto.deriveKey(pass.toCharArray(), before.salt, before.iterations), before.wrapPass, "keyring:pass:${before.generation}".toByteArray()) }.isSuccess
                            }
                            if (!ok) return@drive say("The current passphrase is wrong. Nothing was changed.")
                            // Do not overwrite a change made somewhere else in the meantime.
                            val f = api.list().firstOrNull { it.name == DriveKeyring.FileName } ?: return@drive say("The keyring is gone from Drive.")
                            val remote = Keyring.parse(api.get(f.id))
                            if (remote.generation != before.generation) {
                                s.keyring = remote; s.dek = null
                                return@drive say("The passphrase was changed elsewhere (generation ${remote.generation}). Nothing was overwritten; unlock with the new one.")
                            }
                            val next = withContext(Dispatchers.Default) { DriveKeyring.changePassphrase(before, dek, newPass.toCharArray()) }
                            api.put(DriveKeyring.FileName, next.toBytes(), f.id)
                            s.keyring = next
                            changing = false; pass = ""; pass2 = ""; newPass = ""
                            say("Passphrase changed (generation ${next.generation}). Data files were not touched.")
                        }
                    }
                }
                ChoiceChip("Upload", false, enabled = !busy) {
                    drive { api ->
                        refresh(api)
                        val dek = s.dek ?: return@drive
                        val plain = "Hello from Cenotes ${System.currentTimeMillis()}".toByteArray()
                        val blob = DriveCrypto.encrypt(dek, plain, TestFile.toByteArray())
                        val existing = api.list().firstOrNull { it.name == TestFile }?.id
                        api.put(TestFile, blob, existing)
                        say("Uploaded ${blob.size} B (${if (existing == null) "new" else "replaced"}).")
                    }
                }
                ChoiceChip("Download", false, enabled = !busy) {
                    drive { api ->
                        refresh(api)
                        val dek = s.dek ?: return@drive
                        val f = api.list().firstOrNull { it.name == TestFile } ?: return@drive say("No test file in Drive")
                        val plain = runCatching { DriveCrypto.decrypt(dek, api.get(f.id), TestFile.toByteArray()) }
                        say(plain.fold({ "Downloaded and decrypted: ${String(it)}" }, { "Cannot decrypt (${it.javaClass.simpleName})" }))
                    }
                }
                ChoiceChip("Sync now", false, enabled = !busy) {
                    drive { api ->
                        refresh(api)
                        val dek = s.dek ?: return@drive
                        val prefs = context.getSharedPreferences("drive_sync", android.content.Context.MODE_PRIVATE)
                        val r = DriveSync(api, dek, app.database.noteDao(), app.images, prefs).sync()
                        say(r.toString())
                    }
                }
                ChoiceChip("Lab: seed 5 notes", false, enabled = !busy) { scope.launch { say(labSeed(app)) } }
                ChoiceChip("Lab: new note", false, enabled = !busy) { scope.launch { say(labNew(app)) } }
                ChoiceChip("Lab: edit note $letter", false, enabled = !busy) { scope.launch { say(labEdit(app, letter)) } }
                ChoiceChip("Lab: trash note $letter", false, enabled = !busy) { scope.launch { say(labTrash(app, letter)) } }
                ChoiceChip("Lab: delete note $letter", false, enabled = !busy) { scope.launch { say(labDelete(app, letter)) } }
                ChoiceChip("Lab: show notes", false, enabled = !busy) { scope.launch { say(labShow(app)) } }
                ChoiceChip("Delete test", false, enabled = !busy) {
                    drive { api ->
                        val f = api.list().filter { it.name == TestFile }
                        f.forEach { api.delete(it.id) }
                        say("Deleted ${f.size} test file(s).")
                    }
                }
            }
            ChoiceChip("List", false, enabled = s.token != null && !busy) {
                drive { api -> api.list().let { l -> say("${l.size} file(s) in Drive: " + l.joinToString { "${it.name} (${it.size} B)" }) } }
            }
            ChoiceChip("Reset keyring (lab)", false, enabled = s.token != null && !busy) {
                drive { api ->
                    val all = api.list().filter { it.name == DriveKeyring.FileName || it.name == TestFile }
                    all.forEach { api.delete(it.id) }
                    s.keyring = null; s.keyringId = null; s.dek = null; s.checked = true
                    say("Reset: deleted ${all.size} file(s) (keyring and test data).")
                }
            }
            ChoiceChip("Lab: simulate change elsewhere", false, enabled = unlocked && newPass.length >= MinPassphrase && !busy) {
                drive { api ->
                    val before = s.keyring ?: return@drive
                    val dek = s.dek ?: return@drive
                    val f = api.list().firstOrNull { it.name == DriveKeyring.FileName } ?: return@drive
                    val next = withContext(Dispatchers.Default) { DriveKeyring.changePassphrase(Keyring.parse(api.get(f.id)), dek, newPass.toCharArray()) }
                    api.put(DriveKeyring.FileName, next.toBytes(), f.id)
                    say("Lab: another device uploaded generation ${next.generation} (this device still has ${before.generation}).")
                }
            }
            ChoiceChip("Lock", false, enabled = unlocked && !busy) { s.dek = null; say("Locked.") }
        }

        Text(
            if (log.isEmpty()) "Result: nothing yet" else log.trimEnd(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(14.dp),
        )
    }
}
