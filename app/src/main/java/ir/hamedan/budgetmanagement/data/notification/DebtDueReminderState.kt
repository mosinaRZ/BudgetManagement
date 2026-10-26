package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import java.time.LocalDate

/**
 * Remembers which due-date reminder step (7, 3 or 1) was already delivered for each
 * debt/receivable and due date, so a reminder is never repeated even when the user clears the
 * notification list or the worker runs several times a day.
 *
 * The key contains the due date: changing an item's due date (or the next installment month
 * arriving) automatically starts a fresh 7 -> 3 -> 1 sequence.
 */
object DebtDueReminderState {
    private const val PREFS_NAME = "debt_due_reminder_state"
    private const val MAX_ENTRIES = 500

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun key(itemId: String, dueDate: LocalDate): String = "$itemId|$dueDate"

    /** The smallest step already delivered for this item/due date, or null when none was. */
    fun lastSentStep(context: Context, key: String): Int? {
        val prefs = prefs(context)
        return if (prefs.contains(key)) prefs.getInt(key, Int.MAX_VALUE) else null
    }

    fun markSent(context: Context, key: String, step: Int) {
        prefs(context).edit().putInt(key, step).apply()
    }

    /** Drops entries of items that were deleted/settled or whose due date moved. */
    fun retainOnly(context: Context, liveKeys: Set<String>) {
        val prefs = prefs(context)
        val stale = prefs.all.keys.filter { it !in liveKeys }
        if (stale.isEmpty() && prefs.all.size <= MAX_ENTRIES) return
        val editor = prefs.edit()
        stale.forEach { editor.remove(it) }
        if (prefs.all.size - stale.size > MAX_ENTRIES) editor.clear()
        editor.apply()
    }
}