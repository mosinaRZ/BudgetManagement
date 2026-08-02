package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Resolves export date windows.
 *
 * Contract:
 * - end = today 23:59:59.999
 * - start = day 00:00:00.000 adjusted by period
 * - CURRENT_MONTH → day-of-month = 1
 * - ONE_MONTH → start.add(MONTH, -1) from today midnight
 * - etc.
 *
 * Note: tests use real wall-clock "now". Assertions are relative, not absolute dates.
 */
class ExportPeriodCalculatorTest {

    private fun calFrom(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    private fun assertEndIsEndOfToday(endMillis: Long) {
        val end = calFrom(endMillis)
        val now = Calendar.getInstance()
        assertThat(end.get(Calendar.YEAR)).isEqualTo(now.get(Calendar.YEAR))
        assertThat(end.get(Calendar.DAY_OF_YEAR)).isEqualTo(now.get(Calendar.DAY_OF_YEAR))
        assertThat(end.get(Calendar.HOUR_OF_DAY)).isEqualTo(23)
        assertThat(end.get(Calendar.MINUTE)).isEqualTo(59)
        assertThat(end.get(Calendar.SECOND)).isEqualTo(59)
        assertThat(end.get(Calendar.MILLISECOND)).isEqualTo(999)
    }

    private fun assertStartIsMidnight(startMillis: Long) {
        val start = calFrom(startMillis)
        assertThat(start.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(start.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(start.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(start.get(Calendar.MILLISECOND)).isEqualTo(0)
    }

    // -------------------------------------------------------------------------
    // Shared end / start shape
    // -------------------------------------------------------------------------

    @Test
    fun allPeriods_endAtEndOfToday() {
        ExportPeriod.entries.forEach { period ->
            val range = ExportPeriodCalculator.resolve(period)
            assertEndIsEndOfToday(range.endMillis)
            assertThat(range.endMillis).isAtLeast(range.startMillis)
        }
    }

    @Test
    fun allPeriods_startAtMidnight() {
        ExportPeriod.entries.forEach { period ->
            assertStartIsMidnight(ExportPeriodCalculator.resolve(period).startMillis)
        }
    }

    // -------------------------------------------------------------------------
    // CURRENT_MONTH
    // -------------------------------------------------------------------------

    @Test
    fun currentMonth_startsOnFirstDayOfMonth() {
        val range = ExportPeriodCalculator.resolve(ExportPeriod.CURRENT_MONTH)
        val start = calFrom(range.startMillis)
        val now = Calendar.getInstance()

        assertThat(start.get(Calendar.YEAR)).isEqualTo(now.get(Calendar.YEAR))
        assertThat(start.get(Calendar.MONTH)).isEqualTo(now.get(Calendar.MONTH))
        assertThat(start.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
        assertStartIsMidnight(range.startMillis)
        assertEndIsEndOfToday(range.endMillis)
    }

    // -------------------------------------------------------------------------
    // Relative month windows
    // -------------------------------------------------------------------------

    @Test
    fun oneMonth_startIsApproximatelyOneMonthBeforeTodayMidnight() {
        val range = ExportPeriodCalculator.resolve(ExportPeriod.ONE_MONTH)
        val start = calFrom(range.startMillis)
        val expected = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }

        assertThat(start.get(Calendar.YEAR)).isEqualTo(expected.get(Calendar.YEAR))
        assertThat(start.get(Calendar.MONTH)).isEqualTo(expected.get(Calendar.MONTH))
        assertThat(start.get(Calendar.DAY_OF_MONTH)).isEqualTo(expected.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun twoMonths_isBeforeOneMonthStart() {
        val one = ExportPeriodCalculator.resolve(ExportPeriod.ONE_MONTH)
        val two = ExportPeriodCalculator.resolve(ExportPeriod.TWO_MONTHS)
        assertThat(two.startMillis).isLessThan(one.startMillis)
        assertThat(two.endMillis).isEqualTo(one.endMillis)
    }

    @Test
    fun threeMonths_isBeforeTwoMonthsStart() {
        val two = ExportPeriodCalculator.resolve(ExportPeriod.TWO_MONTHS)
        val three = ExportPeriodCalculator.resolve(ExportPeriod.THREE_MONTHS)
        assertThat(three.startMillis).isLessThan(two.startMillis)
    }

    @Test
    fun sixMonths_isBeforeThreeMonthsStart() {
        val three = ExportPeriodCalculator.resolve(ExportPeriod.THREE_MONTHS)
        val six = ExportPeriodCalculator.resolve(ExportPeriod.SIX_MONTHS)
        assertThat(six.startMillis).isLessThan(three.startMillis)
    }

    @Test
    fun oneYear_startIsApproximatelyOneYearBefore() {
        val range = ExportPeriodCalculator.resolve(ExportPeriod.ONE_YEAR)
        val start = calFrom(range.startMillis)
        val expected = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.YEAR, -1)
        }

        assertThat(start.get(Calendar.YEAR)).isEqualTo(expected.get(Calendar.YEAR))
        assertThat(start.get(Calendar.MONTH)).isEqualTo(expected.get(Calendar.MONTH))
        assertThat(start.get(Calendar.DAY_OF_MONTH)).isEqualTo(expected.get(Calendar.DAY_OF_MONTH))
    }

    // -------------------------------------------------------------------------
    // Ordering / span sanity
    // -------------------------------------------------------------------------

    @Test
    fun periodStarts_areStrictlyOrdered_fromShortestToLongest() {
        val current = ExportPeriodCalculator.resolve(ExportPeriod.CURRENT_MONTH).startMillis
        val one = ExportPeriodCalculator.resolve(ExportPeriod.ONE_MONTH).startMillis
        val two = ExportPeriodCalculator.resolve(ExportPeriod.TWO_MONTHS).startMillis
        val three = ExportPeriodCalculator.resolve(ExportPeriod.THREE_MONTHS).startMillis
        val six = ExportPeriodCalculator.resolve(ExportPeriod.SIX_MONTHS).startMillis
        val year = ExportPeriodCalculator.resolve(ExportPeriod.ONE_YEAR).startMillis

        // longer period → earlier (smaller) start
        // CURRENT_MONTH may be after or before ONE_MONTH depending on day-of-month;
        // only assert the relative add(MONTH) chain:
        assertThat(two).isLessThan(one)
        assertThat(three).isLessThan(two)
        assertThat(six).isLessThan(three)
        assertThat(year).isLessThan(six)
        // CURRENT_MONTH start is always on the 1st; still must be ≤ end
        assertThat(current).isAtMost(
            ExportPeriodCalculator.resolve(ExportPeriod.CURRENT_MONTH).endMillis
        )
    }

    @Test
    fun oneYear_spanIsAtLeast360Days() {
        val range = ExportPeriodCalculator.resolve(ExportPeriod.ONE_YEAR)
        val days = TimeUnit.MILLISECONDS.toDays(range.endMillis - range.startMillis)
        assertThat(days).isAtLeast(360)
        assertThat(days).isAtMost(370) // leap-year / DST tolerance
    }

    @Test
    fun currentMonth_spanIsAtMost31Days() {
        val range = ExportPeriodCalculator.resolve(ExportPeriod.CURRENT_MONTH)
        val days = TimeUnit.MILLISECONDS.toDays(range.endMillis - range.startMillis)
        assertThat(days).isAtMost(31)
        assertThat(days).isAtLeast(0)
    }

    // -------------------------------------------------------------------------
    // Titles (UI labels)
    // -------------------------------------------------------------------------

    @Test
    fun periodTitles_areNonBlank() {
        ExportPeriod.entries.forEach { period ->
            assertThat(period.titleFa).isNotEmpty()
            assertThat(period.titleEn).isNotEmpty()
        }
    }
}