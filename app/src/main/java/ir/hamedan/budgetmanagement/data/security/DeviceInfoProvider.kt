package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import ir.hamedan.budgetmanagement.BuildConfig
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Describes this installation to the backend so the user can recognise it in the
 * "Account devices" screen. The descriptive values are display-only (never used for
 * authorization) and contain no hardware identifiers, advertising IDs or phone numbers.
 *
 * The only identifying value is [installationFingerprint]: a one-way hash that lets the server
 * recognise a reinstalled app as the same device instead of counting it twice. The raw Android ID
 * is never sent.
 */
class DeviceInfoProvider(context: Context) {
    private val appContext = context.applicationContext

    fun toJson(): JSONObject = JSONObject()
        .put("name", displayName())
        .put("model", model())
        .put("platform", "android")
        .put("os_version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        .put("app_version", BuildConfig.VERSION_NAME)
        .put("fingerprint", installationFingerprint())

    /**
     * Salted SHA-256 of the Android ID. The ID is scoped to this app's signing key and user profile,
     * so it stays the same across reinstalls and changes only on a factory reset. Empty when the
     * platform does not provide an ID.
     */
    private fun installationFingerprint(): String {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        if (androidId.isBlank()) return ""
        return MessageDigest.getInstance("SHA-256")
            .digest((FINGERPRINT_SALT + androidId).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /** User-chosen device name when available, otherwise "<Manufacturer> <Model>". */
    private fun displayName(): String {
        val userDefined = runCatching {
            Settings.Global.getString(appContext.contentResolver, "device_name")
        }.getOrNull()?.trim().orEmpty()
        return userDefined.ifBlank { model() }.take(MAX_LENGTH)
    }

    private fun model(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val combined = if (model.startsWith(manufacturer, ignoreCase = true) || manufacturer.isEmpty()) {
            model
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
        return combined.ifBlank { "Android" }.take(MAX_LENGTH)
    }

    private companion object {
        const val MAX_LENGTH = 64
        const val FINGERPRINT_SALT = "budget-management-device-v1:"
    }
}