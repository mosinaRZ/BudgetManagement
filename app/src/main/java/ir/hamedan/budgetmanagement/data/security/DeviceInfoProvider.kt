package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import ir.hamedan.budgetmanagement.BuildConfig
import org.json.JSONObject

/**
 * Describes this installation to the backend so the user can recognise it in the
 * "Account devices" screen. The values are display-only (never used for authorization)
 * and contain no unique hardware identifiers, advertising IDs or phone numbers.
 */
class DeviceInfoProvider(context: Context) {
    private val appContext = context.applicationContext

    fun toJson(): JSONObject = JSONObject()
        .put("name", displayName())
        .put("model", model())
        .put("platform", "android")
        .put("os_version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        .put("app_version", BuildConfig.VERSION_NAME)

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
    }
}