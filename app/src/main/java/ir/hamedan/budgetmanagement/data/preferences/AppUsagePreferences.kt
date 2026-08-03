package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

object AppUsagePreferences {
    private const val PREFS = "app_usage_prefs"
    private const val KEY_LAST_OPEN = "last_open_timestamp"
    private const val KEY_LAST_NOTIFIED_DAYS = "last_notified_days"

    fun updateLastOpen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_OPEN, System.currentTimeMillis())
            .apply()
    }

    fun getLastOpen(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_OPEN, 0L)
    }

    fun getLastNotifiedDays(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_NOTIFIED_DAYS, 0)
    }

    fun setLastNotifiedDays(context: Context, days: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_NOTIFIED_DAYS, days)
            .apply()
    }
}