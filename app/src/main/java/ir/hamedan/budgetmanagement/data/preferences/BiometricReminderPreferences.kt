package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/** What the fingerprint reminder remembers between launches. */
data class BiometricReminderState(
    /** Epoch millis of the last time the reminder was shown (0 = never). */
    val lastShownAt: Long = 0L,
    /** How many times the reminder has been shown so far (informational). */
    val shownCount: Int = 0
)

/** Why the reminder is being considered right now. */
enum class BiometricReminderTrigger {
    /** The user just got into the app: a fresh sign-in, a local unlock or a cold start. */
    SESSION_ENTRY,

    /** The app came back to the foreground while the user was already inside. */
    RETURN_TO_APP
}

/**
 * The rules of the "enable fingerprint login" reminder, kept free of Android so they are easy to test.
 *
 *  - Every time the user gets into the app ([BiometricReminderTrigger.SESSION_ENTRY]) the reminder
 *    is due, as long as fingerprint login is still off. There is no minimum number of days, no
 *    growing delay and no cap on how often it comes back.
 *  - While the user keeps using the app, coming back to it from the background brings the reminder
 *    again once [RETURN_INTERVAL_MILLIS] have passed since it was last shown.
 *  - There is deliberately NO "don't ask again": the only thing that ends the reminder is turning
 *    fingerprint login on. (If it is turned off later, the reminder simply starts again.)
 *
 * Whether fingerprint login is already enabled, or even possible on this device, is decided by the
 * caller; this object only decides about timing.
 */
object BiometricReminderPolicy {
    /** Minimum gap between two reminders that are not triggered by getting into the app. */
    const val RETURN_INTERVAL_MILLIS = 20L * 60 * 1000

    fun shouldRemind(
        state: BiometricReminderState,
        nowMillis: Long,
        trigger: BiometricReminderTrigger
    ): Boolean = when (trigger) {
        BiometricReminderTrigger.SESSION_ENTRY -> true
        BiometricReminderTrigger.RETURN_TO_APP ->
            nowMillis - state.lastShownAt >= RETURN_INTERVAL_MILLIS
    }

    fun markShown(state: BiometricReminderState, nowMillis: Long): BiometricReminderState =
        state.copy(lastShownAt = nowMillis, shownCount = state.shownCount + 1)
}

/** Persists [BiometricReminderState]; all decisions are made by [BiometricReminderPolicy]. */
object BiometricReminderPreferences {
    private const val PREFS_NAME = "biometric_reminder"
    private const val KEY_LAST_SHOWN = "last_shown_at"
    private const val KEY_SHOWN_COUNT = "shown_count"

    // Keys of the previous scheduling scheme (active days, growing delays and a permanent
    // "don't ask again"). They are erased on first use so nobody stays opted out of the reminder.
    private val LEGACY_KEYS = listOf("active_days", "last_active_epoch_day", "next_due_at", "stopped")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun dropLegacyKeys(context: Context) {
        val p = prefs(context)
        if (LEGACY_KEYS.none { p.contains(it) }) return
        p.edit().apply { LEGACY_KEYS.forEach { remove(it) } }.apply()
    }

    internal fun load(context: Context): BiometricReminderState {
        dropLegacyKeys(context)
        val p = prefs(context)
        return BiometricReminderState(
            lastShownAt = p.getLong(KEY_LAST_SHOWN, 0L),
            shownCount = p.getInt(KEY_SHOWN_COUNT, 0)
        )
    }

    private fun save(context: Context, state: BiometricReminderState) {
        prefs(context).edit()
            .putLong(KEY_LAST_SHOWN, state.lastShownAt)
            .putInt(KEY_SHOWN_COUNT, state.shownCount)
            .apply()
    }

    fun shouldRemindNow(
        context: Context,
        trigger: BiometricReminderTrigger,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = BiometricReminderPolicy.shouldRemind(load(context), nowMillis, trigger)

    /** Records that the reminder is being shown right now. */
    fun markShownNow(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        save(context, BiometricReminderPolicy.markShown(load(context), nowMillis))
    }
}