package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import java.time.LocalDate

/** Everything the fingerprint reminder needs to remember between launches. */
data class BiometricReminderState(
    /** Number of different calendar days on which the user opened the main screen. */
    val activeDays: Int = 0,
    val lastActiveEpochDay: Long = -1L,
    /** How many times the reminder has already been shown. */
    val shownCount: Int = 0,
    /** Epoch millis from which the next reminder may appear. */
    val nextDueAt: Long = 0L,
    /** Fingerprint was enabled or the user chose "don't ask again": never remind again. */
    val stopped: Boolean = false
)

/**
 * The rules of the "enable fingerprint login" reminder, kept free of Android so they are easy to test.
 *
 *  - It waits until the user has actually used the app on [MIN_ACTIVE_DAYS] different days, so a
 *    new user is never asked about a shortcut before they have a reason to want one.
 *  - Each time it is shown, the next one is pushed further away (7 -> 14 -> 30 days), and it is
 *    shown at most [MAX_REMINDERS] times in total; closing it counts as "later".
 *  - Enabling fingerprint login or choosing "don't ask again" stops it for good.
 */
object BiometricReminderPolicy {
    const val MIN_ACTIVE_DAYS = 3
    const val MAX_REMINDERS = 3

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Delay before the reminder that follows the [shownCount]-th one. */
    fun intervalAfter(shownCount: Int): Long = when {
        shownCount <= 1 -> 7 * DAY_MILLIS
        shownCount == 2 -> 14 * DAY_MILLIS
        else -> 30 * DAY_MILLIS
    }

    fun recordActiveDay(state: BiometricReminderState, epochDay: Long): BiometricReminderState =
        if (state.lastActiveEpochDay == epochDay) state
        else state.copy(activeDays = state.activeDays + 1, lastActiveEpochDay = epochDay)

    fun shouldRemind(state: BiometricReminderState, nowMillis: Long): Boolean =
        !state.stopped &&
                state.shownCount < MAX_REMINDERS &&
                state.activeDays >= MIN_ACTIVE_DAYS &&
                nowMillis >= state.nextDueAt

    fun markShown(state: BiometricReminderState, nowMillis: Long): BiometricReminderState {
        val count = state.shownCount + 1
        return state.copy(shownCount = count, nextDueAt = nowMillis + intervalAfter(count))
    }

    fun stop(state: BiometricReminderState): BiometricReminderState = state.copy(stopped = true)
}

/** Persists [BiometricReminderState]; all decisions are made by [BiometricReminderPolicy]. */
object BiometricReminderPreferences {
    private const val PREFS_NAME = "biometric_reminder"
    private const val KEY_ACTIVE_DAYS = "active_days"
    private const val KEY_LAST_ACTIVE_DAY = "last_active_epoch_day"
    private const val KEY_SHOWN_COUNT = "shown_count"
    private const val KEY_NEXT_DUE = "next_due_at"
    private const val KEY_STOPPED = "stopped"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    internal fun load(context: Context): BiometricReminderState {
        val p = prefs(context)
        return BiometricReminderState(
            activeDays = p.getInt(KEY_ACTIVE_DAYS, 0),
            lastActiveEpochDay = p.getLong(KEY_LAST_ACTIVE_DAY, -1L),
            shownCount = p.getInt(KEY_SHOWN_COUNT, 0),
            nextDueAt = p.getLong(KEY_NEXT_DUE, 0L),
            stopped = p.getBoolean(KEY_STOPPED, false)
        )
    }

    private fun save(context: Context, state: BiometricReminderState) {
        prefs(context).edit()
            .putInt(KEY_ACTIVE_DAYS, state.activeDays)
            .putLong(KEY_LAST_ACTIVE_DAY, state.lastActiveEpochDay)
            .putInt(KEY_SHOWN_COUNT, state.shownCount)
            .putLong(KEY_NEXT_DUE, state.nextDueAt)
            .putBoolean(KEY_STOPPED, state.stopped)
            .apply()
    }

    /** Counts today as a day of real use (once per calendar day). */
    fun recordActiveDay(context: Context, today: LocalDate = LocalDate.now()) {
        val state = load(context)
        val updated = BiometricReminderPolicy.recordActiveDay(state, today.toEpochDay())
        if (updated != state) save(context, updated)
    }

    fun shouldRemindNow(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean =
        BiometricReminderPolicy.shouldRemind(load(context), nowMillis)

    /** Records that the reminder is being shown and schedules the one after it. */
    fun markShownNow(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        save(context, BiometricReminderPolicy.markShown(load(context), nowMillis))
    }

    /** Fingerprint login was enabled, or the user asked not to be reminded again. */
    fun stopForever(context: Context) {
        save(context, BiometricReminderPolicy.stop(load(context)))
    }
}