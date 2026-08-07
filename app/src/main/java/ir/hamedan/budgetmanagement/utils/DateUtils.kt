package ir.hamedan.budgetmanagement.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val ENGLISH_MONTH_NAMES = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun getFormattedHeaderDate(isPersian: Boolean): String {
        return if (isPersian) getFormattedPersianDate() else getFormattedEnglishDate()
    }

    private fun getFormattedEnglishDate(): String {
        val current = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        return current.format(formatter)
    }

    private fun getFormattedPersianDate(): String {
        val current = LocalDate.now()
        val (jalaliYear, jalaliMonth, jalaliDay, dayName) = toJalaliFull(current)
        return "$dayName $jalaliDay ${PERSIAN_MONTH_NAMES[jalaliMonth - 1]} $jalaliYear"
    }

    fun formatTimestamp(millis: Long, isPersian: Boolean): String {
        val localDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return if (isPersian) {
            val (jYear, jMonth, jDay) = toJalali(localDate)
            "$jYear/${jMonth.toString().padStart(2, '0')}/${jDay.toString().padStart(2, '0')}"
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH)
            localDate.format(formatter)
        }
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    private fun isJalaliLeapYear(year: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
            1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        val jp = breaks.firstOrNull { year < it } ?: breaks.last()
        var jg = year - jp
        if (jg < 0) jg += 33
        val leap = (jg % 33) % 4
        return leap == 1
    }

    fun toJalali(date: LocalDate): Triple<Int, Int, Int> {
        val gYear = date.year
        val gMonth = date.monthValue
        val gDay = date.dayOfMonth

        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if ((gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)) {
            gDaysInMonth[2] = 29
        }

        var totalDays = gDay
        for (i in 1 until gMonth) {
            totalDays += gDaysInMonth[i]
        }

        var jalaliYear = gYear - 621
        var jalaliMonth: Int
        var jalaliDay: Int

        if (totalDays > 79) {
            val subDays = totalDays - 79
            if (subDays <= 186) {
                jalaliMonth = (subDays - 1) / 31 + 1
                jalaliDay = (subDays - 1) % 31 + 1
            } else {
                val subDays2 = subDays - 186
                jalaliMonth = (subDays2 - 1) / 30 + 7
                jalaliDay = (subDays2 - 1) % 30 + 1
            }
        } else {
            jalaliYear -= 1
            var subDays = totalDays + 286
            val prevGYear = gYear - 1
            val isPrevLeap = (prevGYear % 4 == 0 && prevGYear % 100 != 0) || (prevGYear % 400 == 0)
            if (isPrevLeap) subDays += 1

            if (subDays <= 186) {
                jalaliMonth = (subDays - 1) / 31 + 1
                jalaliDay = (subDays - 1) % 31 + 1
            } else {
                val subDays2 = subDays - 186
                jalaliMonth = (subDays2 - 1) / 30 + 7
                jalaliDay = (subDays2 - 1) % 30 + 1
            }
        }

        return Triple(jalaliYear, jalaliMonth, jalaliDay)
    }

    private fun toJalaliFull(date: LocalDate): Quadruple<Int, Int, Int, String> {
        val (year, month, day) = toJalali(date)
        val jDayNames = arrayOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
        val dayOfWeekIndex = (date.dayOfWeek.value + 1) % 7
        return Quadruple(year, month, day, jDayNames[dayOfWeekIndex])
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}