package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

object AppUsagePreferences {
    private const val PREFS = "app_usage_prefs"
    private const val KEY_LAST_OPEN = "last_open_timestamp"
    private const val KEY_LAST_NOTIFIED_DAYS = "last_notified_days"

    /**
     * Records that the USER opened the app, and starts a fresh inactivity cycle.
     *
     * Call this only from a user-visible entry point (MainActivity.onStart), never from
     * Application.onCreate: WorkManager also creates the process in the background to run
     * workers, and that must not count as the user being active — otherwise the inactivity
     * reminder would be reset every few minutes and could never fire.
     *
     * The notified-threshold counter is reset together with the timestamp. Without that, once
     * the 3-day reminder had been sent, no inactivity reminder would ever be sent again.
     */
    fun markOpened(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_OPEN, System.currentTimeMillis())
            .putInt(KEY_LAST_NOTIFIED_DAYS, 0)
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