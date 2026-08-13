package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import android.content.SharedPreferences

object NotificationPreferences {
    private const val PREF_NAME = "notification_prefs"
    private const val KEY_MODE = "notification_mode"
    private const val KEY_DEFAULTS_INITIALIZED = "notif_defaults_initialized"

    const val MODE_BOTH = "BOTH"      // درون‌برنامه‌ای + سیستمی (پیش‌فرض)
    const val MODE_IN_APP = "IN_APP"  // فقط درون‌برنامه‌ای

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getMode(context: Context): String {
        return getPrefs(context).getString(KEY_MODE, MODE_BOTH) ?: MODE_BOTH
    }

    fun setMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_MODE, mode).apply()
    }

    fun ensureDefaultsInitialized(context: Context) {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_DEFAULTS_INITIALIZED, false)) return

        val editor = prefs.edit()
        NotificationType.entries.forEach { type ->
            editor.putBoolean(type.prefKey, type.defaultEnabled)
        }
        editor.putBoolean(KEY_DEFAULTS_INITIALIZED, true).apply()
    }

    fun isTypeEnabled(context: Context, type: NotificationType): Boolean {
        ensureDefaultsInitialized(context)
        return getPrefs(context).getBoolean(type.prefKey, type.defaultEnabled)
    }

    fun setTypeEnabled(context: Context, type: NotificationType, enabled: Boolean) {
        ensureDefaultsInitialized(context)
        getPrefs(context).edit().putBoolean(type.prefKey, enabled).apply()
    }

    fun getAllTypeStates(context: Context): Map<NotificationType, Boolean> {
        ensureDefaultsInitialized(context)
        return NotificationType.entries.associateWith { isTypeEnabled(context, it) }
    }

    fun setCategoryEnabled(context: Context, category: NotificationCategory, enabled: Boolean) {
        ensureDefaultsInitialized(context)
        val editor = getPrefs(context).edit()
        NotificationType.entries.filter { it.category == category }.forEach { type ->
            editor.putBoolean(type.prefKey, enabled)
        }
        editor.apply()
    }

    fun isCategoryFullyEnabled(context: Context, category: NotificationCategory): Boolean {
        return NotificationType.entries
            .filter { it.category == category }
            .all { isTypeEnabled(context, it) }
    }
}
