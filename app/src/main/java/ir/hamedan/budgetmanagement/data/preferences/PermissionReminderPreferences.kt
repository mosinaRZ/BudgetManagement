package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

object PermissionReminderPreferences {
    private const val PREFS_NAME = "settings"

    internal const val REMINDER_INTERVAL_MILLIS = 4L * 24 * 60 * 60 * 1000L
    internal const val SNOOZE_MILLIS = 1L * 24 * 60 * 60 * 1000L

    private fun lastShownKey(permissionKey: String) = "perm_reminder_last_shown_$permissionKey"
    private fun dismissedKey(permissionKey: String) = "perm_reminder_dismissed_$permissionKey"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDismissedForever(context: Context, permissionKey: String): Boolean =
        prefs(context).getBoolean(dismissedKey(permissionKey), false)

    fun dismissForever(context: Context, permissionKey: String) {
        prefs(context).edit().putBoolean(dismissedKey(permissionKey), true).apply()
    }

    fun shouldRemindNow(context: Context, permissionKey: String): Boolean {
        if (isDismissedForever(context, permissionKey)) return false
        val lastShown = prefs(context).getLong(lastShownKey(permissionKey), 0L)
        return System.currentTimeMillis() - lastShown >= REMINDER_INTERVAL_MILLIS
    }

    fun markShownNow(context: Context, permissionKey: String) {
        prefs(context).edit().putLong(lastShownKey(permissionKey), System.currentTimeMillis()).apply()
    }

    fun snooze(context: Context, permissionKey: String) {
        val fakeLastShown = System.currentTimeMillis() - REMINDER_INTERVAL_MILLIS + SNOOZE_MILLIS
        prefs(context).edit().putLong(lastShownKey(permissionKey), fakeLastShown).apply()
    }
}