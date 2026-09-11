package org.fossify.calendar.continuum

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * EncryptedSharedPreferences for Continuum OAuth tokens, with a one-time copy
 * off the previous plaintext prefs file.
 */
object ContinuumTokenStore {
    private const val PREFS = "continuum_google_auth"
    private const val PREFS_ENCRYPTED = "continuum_google_auth_enc"

    fun open(context: Context): SharedPreferences {
        val encrypted = runCatching { encryptedPrefs(context) }.getOrNull()
        val legacy = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (encrypted == null) return legacy
        migrate(legacy, encrypted)
        return encrypted
    }

    private fun encryptedPrefs(context: Context): SharedPreferences {
        val key = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_ENCRYPTED,
            key,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun migrate(from: SharedPreferences, to: SharedPreferences) {
        if (to.all.isNotEmpty() || from.all.isEmpty()) return
        to.edit().apply {
            for ((key, value) in from.all) {
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                }
            }
        }.apply()
        from.edit().clear().apply()
    }
}
