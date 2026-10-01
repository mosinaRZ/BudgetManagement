package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateUtilsTest {

    /** تبدیل LocalDate به millis در نیمه‌شب timezone سیستم */
    private fun toMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun formatTimestamp_english_returnsCorrectPattern() {
        val millis = toMillis(2024, 6, 15)
        val result = DateUtils.formatTimestamp(millis, isPersian = false)

        assertThat(result).isEqualTo("2024/06/15")
    }

    @Test
    fun formatTimestamp_persian_returnsJalaliPattern() {
        val millis = toMillis(2024, 6, 15)
        val result = DateUtils.formatTimestamp(millis, isPersian = true)

        assertThat(result).matches("""\d{4}/\d{2}/\d{2}""")
        val parts = result.split("/")
        assertThat(parts).hasSize(3)
        assertThat(parts[0].toInt()).isAtLeast(1400)
        assertThat(parts[1].toInt()).isIn(1..12)
        assertThat(parts[2].toInt()).isIn(1..31)
    }

    @Test
    fun formatTimestamp_nowruz2023_isFirstOfFarvardin1402() {
        // ۲۱ مارس ۲۰۲۳ ≈ ۱ فروردین ۱۴۰۲
        val millis = toMillis(2023, 3, 21)
        val result = DateUtils.formatTimestamp(millis, isPersian = true)

        assertThat(result).isEqualTo("1402/01/01")
    }

    @Test
    fun formatTimestamp_englishAndPersian_areDifferent() {
        val millis = toMillis(2024, 1, 1)
        val en = DateUtils.formatTimestamp(millis, isPersian = false)
        val fa = DateUtils.formatTimestamp(millis, isPersian = true)

        assertThat(en).isNotEqualTo(fa)
        assertThat(en).startsWith("2024")
        assertThat(fa).startsWith("140")
    }

    @Test
    fun daysUntil_usesCalendarDaysNotTwentyFourHourBlocks() {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.of(2026, 9, 30).atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
        val tomorrow = LocalDate.of(2026, 10, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

        assertThat(DateUtils.daysUntil(tomorrow, now, zone)).isEqualTo(1L)
    }

    @Test
    fun daysUntil_returnsNegativeForPastDate() {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.of(2026, 9, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        val yesterday = LocalDate.of(2026, 9, 29).atStartOfDay(zone).toInstant().toEpochMilli()

        assertThat(DateUtils.daysUntil(yesterday, now, zone)).isEqualTo(-1L)
    }

    @Test
    fun getFormattedHeaderDate_english_containsYear() {
        val result = DateUtils.getFormattedHeaderDate(isPersian = false)
        assertThat(result).contains("202") // سال میلادی فعلی
    }

    @Test
    fun getFormattedHeaderDate_persian_containsJalaliMonthName() {
        val result = DateUtils.getFormattedHeaderDate(isPersian = true)
        val months = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        assertThat(months.any { result.contains(it) }).isTrue()
    }
}