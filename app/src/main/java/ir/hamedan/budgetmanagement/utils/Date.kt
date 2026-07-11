package ir.hamedan.budgetmanagement.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ۱. تابع فرمت تاریخ میلادی (برای انگلیسی زبان‌ها)
fun getFormattedEnglishDate(): String {
    val current = LocalDate.now()
    // فرمت خروجی: Saturday, July 11, 2026
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
    return current.format(formatter)
}

// ۲. تابع ساده و بومی برای تبدیل به شمسی (با الگوریتم تقویمی بومی بدون کتابخانه اضافی)
fun getFormattedPersianDate(): String {
    val current = LocalDate.now()

    // محاسبه دقیق تاریخ شمسی بر اساس الگوریتم ریاضی تبدیل تاریخ
    val gYear = current.year
    val gMonth = current.monthValue
    val gDay = current.dayOfMonth

    val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    if ((gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)) {
        gDaysInMonth[2] = 29
    }

    var totalDays = gDay
    for (i in 1 until gMonth) {
        totalDays += gDaysInMonth[i]
    }

    val jMonthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val jDayNames = arrayOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    // پیدا کردن روز هفته (LocalDate روز هفته را از ۱ تا ۷ می‌دهد که دوشنبه ۱ است)
    // تبدیل آن به شاخص روزهای شمسی (که شنبه اول است)
    val dayOfWeekIndex = (current.dayOfWeek.value + 1) % 7
    val dayName = jDayNames[dayOfWeekIndex]

    // تبدیل سال میلادی به روزهای سپری شده از مبدا
    var jalaliYear = gYear - 621
    var jalaliMonth = 0
    var jalaliDay = 0

    // روزهای سال کبیسه میلادی و شمسی
    val gDayOfYear = totalDays

    if (gDayOfYear > 79) {
        val subDays = gDayOfYear - 79
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
        var subDays = gDayOfYear + 286
        // بررسی کبیسه بودن سال قبل
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

    // خروجی نهایی: شنبه ۲۰ تیر ۱۴۰۵
    return "$dayName $jalaliDay ${jMonthNames[jalaliMonth - 1]} $jalaliYear"
}