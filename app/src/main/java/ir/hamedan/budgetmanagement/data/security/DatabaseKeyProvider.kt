package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object DatabaseKeyProvider {

    private const val PREFS_NAME = "db_key_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"

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

        var passphrase = prefs.getString(KEY_PASSPHRASE, null)

        if (passphrase == null) {
            // تولید کلید تصادفی قوی (۳۲ بایت = ۲۵۶ بیت)
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            passphrase = bytes.joinToString("") { "%02x".format(it) } // یا Base64
            prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
        }

        return passphrase.toByteArray(Charsets.UTF_8)
    }
}