package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * Scheduling for reminders about permissions the user has not granted yet.
 *
 * Every permission stores the moment its next reminder is due, so the schedule is explicit and
 * does not have to be re-derived from earlier timestamps:
 *  - each reminder shown pushes the next one further away: 4 → 8 → 16 days;
 *  - after [MAX_AUTO_REMINDERS] reminders nothing is shown automatically any more;
 *  - "Remind me later" postpones the first reminder by [SNOOZE_MILLIS] (1 day); later snoozes
 *    keep the current interval so the user is not nagged;
 *  - "Don't ask again" stops reminders for that permission for good.
 *
 * Old installs only have the "last shown" timestamp. For them the next due time is derived from it
 * once, so nobody gets an extra reminder because of the upgrade.
 */
object PermissionReminderPreferences {
    private const val PREFS_NAME = "settings"

    internal const val REMINDER_INTERVAL_MILLIS = 4L * 24 * 60 * 60 * 1000L
    internal const val SNOOZE_MILLIS = 1L * 24 * 60 * 60 * 1000L

    /** Maximum number of automatic reminders per permission. */
    internal const val MAX_AUTO_REMINDERS = 4

    private fun lastShownKey(permissionKey: String) = "perm_reminder_last_shown_$permissionKey"
    private fun nextDueKey(permissionKey: String) = "perm_reminder_next_due_$permissionKey"
    private fun dismissedKey(permissionKey: String) = "perm_reminder_dismissed_$permissionKey"
    private fun countKey(permissionKey: String) = "perm_reminder_count_$permissionKey"
    private fun requestedKey(permissionKey: String) = "perm_requested_$permissionKey"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Delay before the reminder that follows the [shownCount]-th shown reminder. */
    internal fun intervalFor(shownCount: Int): Long = when {
        shownCount <= 1 -> REMINDER_INTERVAL_MILLIS
        shownCount == 2 -> REMINDER_INTERVAL_MILLIS * 2
        else -> REMINDER_INTERVAL_MILLIS * 4
    }

    fun isDismissedForever(context: Context, permissionKey: String): Boolean =
        prefs(context).getBoolean(dismissedKey(permissionKey), false)

    fun dismissForever(context: Context, permissionKey: String) {
        prefs(context).edit().putBoolean(dismissedKey(permissionKey), true).apply()
    }

    fun shownCount(context: Context, permissionKey: String): Int =
        prefs(context).getInt(countKey(permissionKey), 0)

    /** Epoch millis from which the next automatic reminder may be shown (0 = immediately). */
    fun nextDueAt(context: Context, permissionKey: String): Long {
        val p = prefs(context)
        if (p.contains(nextDueKey(permissionKey))) return p.getLong(nextDueKey(permissionKey), 0L)
        // Legacy state written before next-due timestamps existed.
        val lastShown = p.getLong(lastShownKey(permissionKey), 0L)
        if (lastShown == 0L) return 0L
        return lastShown + intervalFor(shownCount(context, permissionKey))
    }

    fun shouldRemindNow(
        context: Context,
        permissionKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (isDismissedForever(context, permissionKey)) return false
        if (shownCount(context, permissionKey) >= MAX_AUTO_REMINDERS) return false
        return nowMillis >= nextDueAt(context, permissionKey)
    }

    /** Records that a reminder was shown and schedules the one after it. */
    fun markShownNow(
        context: Context,
        permissionKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val count = shownCount(context, permissionKey) + 1
        prefs(context).edit()
            .putLong(lastShownKey(permissionKey), nowMillis)
            .putInt(countKey(permissionKey), count)
            .putLong(nextDueKey(permissionKey), nowMillis + intervalFor(count))
            .apply()
    }

    /** "Remind me later": postpones the next reminder without counting as a new one. */
    fun snooze(
        context: Context,
        permissionKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val count = shownCount(context, permissionKey)
        val delay = if (count <= 1) SNOOZE_MILLIS else intervalFor(count)
        prefs(context).edit()
            .putLong(nextDueKey(permissionKey), nowMillis + delay)
            .apply()
    }

    /** Records that the system permission request for this permission was shown at least once. */
    fun markRequested(context: Context, permissionKey: String) {
        prefs(context).edit().putBoolean(requestedKey(permissionKey), true).apply()
    }

    fun wasRequested(context: Context, permissionKey: String): Boolean =
        prefs(context).getBoolean(requestedKey(permissionKey), false)
}