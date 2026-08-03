package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object DatabaseKeyProvider {

    private const val PREFS_NAME = "db_key_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase_b64"

    fun getPassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val storedB64 = prefs.getString(KEY_PASSPHRASE, null)

        return if (storedB64 != null) {
            Base64.decode(storedB64, Base64.NO_WRAP)
        } else {
            val bytes = ByteArray(32) // 256-bit
            SecureRandom().nextBytes(bytes)
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(KEY_PASSPHRASE, encoded).apply()
            bytes
        }
    }
}