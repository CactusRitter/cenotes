package com.cenotes.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSecretsTest {
    @Test
    fun noRealSettingKeyLooksLikeASecret() {
        SettingsRepository.AllKeys.forEach { assertFalse("$it would be left out of backups", SettingsRepository.isSecretKey(it)) }
    }

    @Test
    fun secretLookingKeysAreNeverExported() {
        listOf("master_password", "drive_token", "api_key", "apiKey", "oauth_refresh_token", "recovery_secret", "kdf_salt", "pin", "PASSWORD", "credentials")
            .forEach { assertTrue("$it must be excluded from backups", SettingsRepository.isSecretKey(it)) }
    }
}
