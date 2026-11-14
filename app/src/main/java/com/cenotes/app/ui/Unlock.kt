package com.cenotes.app.ui

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * The phone's own lock (fingerprint, face, or its PIN, pattern or password) for the protected notes. Cenotes never
 * sees nor keeps any of it: Android checks it and only says yes or no.
 */
object Unlock {
    // Fingerprint/face OR the screen lock. (Strong biometrics together with the screen lock is not offered on
    // Android 9–10, the weak class is: both still need the phone's own lock to be set.)
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** Whether the phone has a screen lock (or a fingerprint/face) to protect notes with. */
    fun available(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS ||
            // Android 9–10 may answer "none enrolled" for biometrics while a PIN is set; the keyguard knows.
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                context.getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure)

    /** Asks for the phone's lock; [onSuccess] runs only if it was given. */
    fun ask(activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
