package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * Pure scheduling rules for the "add the balance widget" prompt on the home screen.
 *
 * Kept free of Android types so it can be unit-tested on the JVM.
 *
 *  - every time the user closes the prompt, the next one is pushed further away:
 *    3 → 7 → 14 → 30 days (the last interval is reused);
 *  - after [MAX_DISMISSALS] dismissals nothing is shown automatically any more: the user has
 *    clearly said no, and the widget can still be added from the launcher's widget menu.
 */
object WidgetPromptPolicy {
    private const val DAY = 24L * 60 * 60 * 1000L

    internal const val MAX_DISMISSALS = 5

    /** Delay before the prompt may come back after the [dismissCount]-th dismissal. */
    fun intervalFor(dismissCount: Int): Long = when {
        dismissCount <= 1 -> 3 * DAY
        dismissCount == 2 -> 7 * DAY
        dismissCount == 3 -> 14 * DAY
        else -> 30 * DAY
    }

    fun shouldShow(dismissCount: Int, nextDueAt: Long, nowMillis: Long): Boolean {
        if (dismissCount >= MAX_DISMISSALS) return false
        return nowMillis >= nextDueAt
    }
}

/** Persists how often the widget prompt was closed and when it may be shown again. */
object WidgetPromptPreferences {
    private const val PREFS_NAME = "widget_prompt_prefs"
    private const val KEY_DISMISS_COUNT = "dismiss_count"
    private const val KEY_NEXT_DUE_AT = "next_due_at"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShow(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val p = prefs(context)
        return WidgetPromptPolicy.shouldShow(
            dismissCount = p.getInt(KEY_DISMISS_COUNT, 0),
            nextDueAt = p.getLong(KEY_NEXT_DUE_AT, 0L),
            nowMillis = nowMillis
        )
    }

    /** The user closed the prompt: remember it and schedule the next (later) suggestion. */
    fun dismiss(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        val count = p.getInt(KEY_DISMISS_COUNT, 0) + 1
        p.edit()
            .putInt(KEY_DISMISS_COUNT, count)
            .putLong(KEY_NEXT_DUE_AT, nowMillis + WidgetPromptPolicy.intervalFor(count))
            .apply()
    }
}