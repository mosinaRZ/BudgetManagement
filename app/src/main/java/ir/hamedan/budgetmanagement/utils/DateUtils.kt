package ir.hamedan.budgetmanagement.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    // ۱. دریافت تاریخ امروز با نام روز هفته (برای بنر یا تاپ‌بار صفحه اصلی)
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
        val jMonthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        return "$dayName $jalaliDay ${jMonthNames[jalaliMonth - 1]} $jalaliYear"
    }

    // ۲. تابع اصلی: تبدیل Timestamp (میلی‌ثانیه) به فرمت کوتاه yyyy/MM/dd بر اساس زبان
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

    // --- الگوریتم‌های محاسباتی تقویم شمسی (بدون کتابخانه خارجی) ---

    private fun toJalali(date: LocalDate): Triple<Int, Int, Int> {
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