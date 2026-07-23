package ir.hamedan.budgetmanagement.utils

import java.util.Calendar

object PaymentDateUtils {

    /**
     * محاسبه میلی‌ثانیه سررسید بعدی بر اساس روز ماه (مثلاً روز ۲۶)
     */
    fun calculateNextDueDate(dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDayCurrentMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // اطمینان از اینکه روز در محدوده روزهای ماه جاری باشد (مثلاً ماه ۳۰ روزه روز ۳۱ نداشته باشد)
        val targetDay = dayOfMonth.coerceAtMost(maxDayCurrentMonth)

        if (currentDay > targetDay) {
            // اگر از روز سررسید این ماه گذشته، برود به ماه بعد
            calendar.add(Calendar.MONTH, 1)
        }

        val maxDayNextMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(maxDayNextMonth))
        calendar.set(Calendar.HOUR_OF_DAY, 9) // تنظیم ساعت روی ۹ صبح
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    /**
     * افزودن یک ماه به تاریخ سررسید فعلی (زمان پرداخت قسط جاری)
     */
    fun getNextMonthDueDate(currentDueDateMillis: Long, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDateMillis
            add(Calendar.MONTH, 1)
        }
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(maxDay))
        return calendar.timeInMillis
    }
}