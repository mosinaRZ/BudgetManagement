package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import android.content.SharedPreferences

object NotificationPreferences {
    private const val PREF_NAME = "notification_prefs"
    private const val KEY_MODE = "notification_mode"

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
}