package ir.hamedan.budgetmanagement.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * The due day of a monthly installment, kept in the existing `dueDay` Int so no database or sync
 * change is needed:
 *
 *  - `1..31`    Gregorian day of month (every value saved before Jalali support, so old records keep their meaning)
 *  - `101..131` Jalali day of month (day = value - 100)
 *
 * The screen picks the calendar from the app language (Persian = Jalali, English = Gregorian) and
 * all schedule maths below follows the calendar the value was saved with, so switching the app
 * language later never moves an existing installment.
 */
object DueDay {
    private const val JALALI_OFFSET = 100

    fun encode(day: Int, jalali: Boolean): Int {
        require(day in 1..31) { "Day of month must be between 1 and 31." }
        return if (jalali) day + JALALI_OFFSET else day
    }

    fun isValid(stored: Int): Boolean = stored in 1..31 || stored in (JALALI_OFFSET + 1)..(JALALI_OFFSET + 31)

    fun isJalali(stored: Int): Boolean = stored > JALALI_OFFSET

    /** The day of month (1..31) without the calendar marker. */
    fun dayOf(stored: Int): Int = if (isJalali(stored)) stored - JALALI_OFFSET else stored

    /** Today's day of month in the chosen calendar: the picker's default position. */
    fun dayOfMonth(date: LocalDate, jalali: Boolean): Int =
        if (jalali) DateUtils.toJalali(date).third else date.dayOfMonth

    /**
     * The due date that falls [monthsAhead] months after the month of [anchor], in the calendar of
     * [stored]. A day the month does not have (31 in a 30-day month) becomes the month's last day.
     */
    fun dateInMonth(stored: Int, anchor: LocalDate, monthsAhead: Int = 0): LocalDate {
        require(isValid(stored)) { "Due day is invalid." }
        val jalali = isJalali(stored)
        val (year, month) = if (jalali) {
            DateUtils.toJalali(anchor).let { it.first to it.second }
        } else {
            anchor.year to anchor.monthValue
        }
        val index = year * 12 + (month - 1) + monthsAhead
        val targetYear = Math.floorDiv(index, 12)
        val targetMonth = Math.floorMod(index, 12) + 1
        val day = dayOf(stored)
        return if (jalali) {
            DateUtils.fromJalali(targetYear, targetMonth, day)
        } else {
            val ym = YearMonth.of(targetYear, targetMonth)
            ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))
        }
    }

    /** First installment date for a new plan: this month's due day, or next month's if it has already passed. */
    fun firstDueDate(stored: Int, today: LocalDate): LocalDate {
        val passed = dayOfMonth(today, isJalali(stored)) > dayOf(stored)
        return dateInMonth(stored, today, if (passed) 1 else 0)
    }

    /** The next upcoming due date, counting today as still due. */
    fun nextDueDate(stored: Int, today: LocalDate): LocalDate {
        val thisMonth = dateInMonth(stored, today, 0)
        return if (thisMonth.isBefore(today)) dateInMonth(stored, today, 1) else thisMonth
    }

    /** Start of [date] plus the current time of day, matching how installment dates were stored before. */
    fun toMillis(date: LocalDate): Long =
        date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}