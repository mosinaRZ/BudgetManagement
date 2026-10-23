package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DueDayTest {

    @Test
    fun encode_marksJalaliDaysAndKeepsGregorianDaysPlain() {
        assertThat(DueDay.encode(15, jalali = false)).isEqualTo(15)
        assertThat(DueDay.encode(15, jalali = true)).isEqualTo(115)
        assertThat(DueDay.isJalali(115)).isTrue()
        assertThat(DueDay.isJalali(15)).isFalse()
        assertThat(DueDay.dayOf(115)).isEqualTo(15)
        assertThat(DueDay.dayOf(15)).isEqualTo(15)
    }

    @Test
    fun isValid_acceptsOnlyTheTwoRanges() {
        assertThat(DueDay.isValid(0)).isFalse()
        assertThat(DueDay.isValid(1)).isTrue()
        assertThat(DueDay.isValid(31)).isTrue()
        assertThat(DueDay.isValid(32)).isFalse()
        assertThat(DueDay.isValid(100)).isFalse()
        assertThat(DueDay.isValid(101)).isTrue()
        assertThat(DueDay.isValid(131)).isTrue()
        assertThat(DueDay.isValid(132)).isFalse()
    }

    @Test
    fun dayOfMonth_followsTheChosenCalendar() {
        val date = LocalDate.of(2026, 10, 20)
        assertThat(DueDay.dayOfMonth(date, jalali = false)).isEqualTo(20)
        assertThat(DueDay.dayOfMonth(date, jalali = true)).isEqualTo(DateUtils.toJalali(date).third)
    }

    @Test
    fun fromJalali_isTheInverseOfToJalali() {
        var date = LocalDate.of(2025, 1, 1)
        repeat(900) {
            val (y, m, d) = DateUtils.toJalali(date)
            assertThat(DateUtils.fromJalali(y, m, d)).isEqualTo(date)
            date = date.plusDays(1)
        }
    }

    @Test
    fun dateInMonth_gregorianClampsToMonthEnd() {
        val anchor = LocalDate.of(2026, 1, 10)
        assertThat(DueDay.dateInMonth(31, anchor, 1)).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(DueDay.dateInMonth(31, anchor, 2)).isEqualTo(LocalDate.of(2026, 3, 31))
    }

    @Test
    fun dateInMonth_jalaliKeepsTheJalaliDayEveryMonth() {
        val anchor = LocalDate.of(2026, 10, 20)
        repeat(14) { ahead ->
            val due = DueDay.dateInMonth(DueDay.encode(5, jalali = true), anchor, ahead)
            assertThat(DateUtils.toJalali(due).third).isEqualTo(5)
        }
    }

    @Test
    fun firstDueDate_movesToNextMonthOnlyWhenTheDayHasPassed() {
        val today = LocalDate.of(2026, 10, 20)
        assertThat(DueDay.firstDueDate(25, today)).isEqualTo(LocalDate.of(2026, 10, 25))
        assertThat(DueDay.firstDueDate(20, today)).isEqualTo(today)
        assertThat(DueDay.firstDueDate(10, today)).isEqualTo(LocalDate.of(2026, 11, 10))
    }

    @Test
    fun nextDueDate_countsTodayAsStillDue() {
        val today = LocalDate.of(2026, 10, 20)
        assertThat(DueDay.nextDueDate(20, today)).isEqualTo(today)
        assertThat(DueDay.nextDueDate(19, today)).isEqualTo(LocalDate.of(2026, 11, 19))
    }
}