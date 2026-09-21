package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/** Installation-scoped device identity. It is deliberately independent of the logged-in account. */
class DeviceIdentityStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getOrCreate(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val id = UUID.randomUUID().toString()
        check(prefs.edit().putString(KEY_DEVICE_ID, id).commit()) {
            "Unable to persist device identity."
        }
        return id
    }

    companion object {
        private const val PREFS_NAME = "budget_device_identity"
        private const val KEY_DEVICE_ID = "device_id"
    }
}