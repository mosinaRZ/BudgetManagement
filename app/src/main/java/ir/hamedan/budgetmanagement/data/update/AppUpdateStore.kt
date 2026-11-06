package ir.hamedan.budgetmanagement.data.update

import android.content.Context
import org.json.JSONObject

/**
 * Remembers the last update policy the server sent, so a build that is too old stays blocked
 * even when the device is offline (airplane mode must not be a way around the update screen).
 *
 * Also remembers when the server itself refused this exact build with 426, until the installed
 * version changes.
 */
class AppUpdateStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun policy(): AppUpdatePolicy? {
        val raw = prefs.getString(KEY_POLICY, null) ?: return null
        return runCatching { AppUpdatePolicy.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun save(policy: AppUpdatePolicy) {
        prefs.edit().putString(KEY_POLICY, policy.toJson().toString()).apply()
    }

    /** The versionName the server answered with 426, or null. */
    fun rejectedVersion(): String? = prefs.getString(KEY_REJECTED_VERSION, null)

    fun markRejected(versionName: String) {
        prefs.edit().putString(KEY_REJECTED_VERSION, versionName).apply()
    }

    fun clearRejected() {
        if (prefs.contains(KEY_REJECTED_VERSION)) prefs.edit().remove(KEY_REJECTED_VERSION).apply()
    }

    private companion object {
        const val PREFS_NAME = "app_update_policy"
        const val KEY_POLICY = "policy"
        const val KEY_REJECTED_VERSION = "rejected_version"
    }
}
