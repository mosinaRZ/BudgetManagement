package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure rules behind the background due-date reminders for debts and receivables.
 *
 * A reminder is sent when 7, 3 and 1 day(s) are left to the due date. Because the phone may be off
 * or the worker may be delayed, each reminder is tied to a *step* (7, 3 or 1) rather than to an
 * exact day count: the step that applies to the days left is sent at most once per due date, and
 * a step is never sent again after a smaller one was already delivered.
 *
 * The due date of an item follows exactly what the app shows on screen:
 *  - installment plans (`isMonthly`) use the NEXT installment day in the calendar the day was
 *    saved with (Jalali or Gregorian, see [DueDay]);
 *  - one-time items use the stored `dueDateMillis`.
 */
object DebtDueReminders {

    /** Days before the due date on which a reminder is due, largest first. */
    val STEPS: List<Int> = listOf(7, 3, 1)

    data class DueInfo(val dueDate: LocalDate, val daysLeft: Int)

    /** Still owes money: neither settled nor fully paid. */
    fun isActive(item: DebtCreditEntity): Boolean =
        !item.isSettled && item.paidAmount < item.totalAmount

    /** The next due date of [item], or null when it has none (settled, or no date was ever set). */
    fun nextDueDate(item: DebtCreditEntity, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
        if (!isActive(item)) return null
        if (item.isMonthly && DueDay.isValid(item.dueDay)) {
            return DueDay.nextDueDate(item.dueDay, today)
        }
        if (item.dueDateMillis <= 0L) return null
        return Instant.ofEpochMilli(item.dueDateMillis).atZone(zone).toLocalDate()
    }

    fun dueInfo(item: DebtCreditEntity, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DueInfo? {
        val date = nextDueDate(item, today, zone) ?: return null
        return DueInfo(date, ChronoUnit.DAYS.between(today, date).toInt())
    }

    /**
     * The reminder step that applies to [daysLeft]: 7 for 4..7 days, 3 for 2..3 days and 1 for
     * today/tomorrow. Null when the due date is more than a week away or already in the past
     * (overdue items are handled by the dialog shown when the app is opened).
     */
    fun stepFor(daysLeft: Int): Int? = when (daysLeft) {
        in 4..7 -> 7
        in 2..3 -> 3
        in 0..1 -> 1
        else -> null
    }

    /** True while [step] has not been delivered yet and no smaller step was delivered before it. */
    fun shouldSend(step: Int, lastSentStep: Int?): Boolean = lastSentStep == null || step < lastSentStep

    /** What the next payment is: the installment for plans, otherwise everything still owed. */
    fun amountDue(item: DebtCreditEntity): Long {
        val remaining = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)
        return if (item.isMonthly && item.monthlyAmount > 0L) minOf(item.monthlyAmount, remaining) else remaining
    }
}