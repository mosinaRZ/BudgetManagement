package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for sensitive settings (currently biometric flag).
 * Uses EncryptedSharedPreferences so the file is encrypted at rest.
 */
object SharedPreferences {
    private const val PREF_NAME = "budget_secure_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setBiometricEnabled(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, isEnabled).apply()
    }

    fun getBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
}