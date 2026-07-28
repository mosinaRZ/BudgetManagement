package ir.hamedan.budgetmanagement.utils

import java.util.Calendar

enum class ExportPeriod(val titleFa: String, val titleEn: String) {
    CURRENT_MONTH("ماه جاری", "Current Month"),
    ONE_MONTH("یک ماه اخیر", "Last 1 Month"),
    TWO_MONTHS("دو ماه اخیر", "Last 2 Months"),
    THREE_MONTHS("سه ماه اخیر", "Last 3 Months"),
    SIX_MONTHS("شش ماه اخیر", "Last 6 Months"),
    ONE_YEAR("یک سال اخیر", "Last 1 Year")
}

data class DateRange(val startMillis: Long, val endMillis: Long)

object ExportPeriodCalculator {
    fun resolve(period: ExportPeriod): DateRange {
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        when (period) {
            ExportPeriod.CURRENT_MONTH -> start.set(Calendar.DAY_OF_MONTH, 1)
            ExportPeriod.ONE_MONTH -> start.add(Calendar.MONTH, -1)
            ExportPeriod.TWO_MONTHS -> start.add(Calendar.MONTH, -2)
            ExportPeriod.THREE_MONTHS -> start.add(Calendar.MONTH, -3)
            ExportPeriod.SIX_MONTHS -> start.add(Calendar.MONTH, -6)
            ExportPeriod.ONE_YEAR -> start.add(Calendar.YEAR, -1)
        }

        return DateRange(start.timeInMillis, end.timeInMillis)
    }
}