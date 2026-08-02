package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class PaymentDateUtilsTest {

    @Test
    fun getNextMonthDueDate_addsOneMonth() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis

        val next = PaymentDateUtils.getNextMonthDueDate(current, dayOfMonth = 15)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertThat(nextCal.get(Calendar.MONTH)).isEqualTo(Calendar.FEBRUARY)
        assertThat(nextCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(15)
        assertThat(nextCal.get(Calendar.YEAR)).isEqualTo(2024)
    }

    @Test
    fun getNextMonthDueDate_handlesDay31InShortMonth() {
        // ۳۱ ژانویه → فوریه حداکثر ۲۹/۲۸ روز دارد
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 31, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis

        val next = PaymentDateUtils.getNextMonthDueDate(current, dayOfMonth = 31)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertThat(nextCal.get(Calendar.MONTH)).isEqualTo(Calendar.FEBRUARY)
        // نباید بیشتر از آخرین روز فوریه باشد
        assertThat(nextCal.get(Calendar.DAY_OF_MONTH))
            .isAtMost(nextCal.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun calculateNextDueDate_returnsFutureOrTodayTimestamp() {
        val now = System.currentTimeMillis()
        val result = PaymentDateUtils.calculateNextDueDate(dayOfMonth = 15)

        // باید حداقل نزدیک به امروز یا آینده باشد (نه گذشته دور)
        assertThat(result).isAtLeast(now - 24 * 60 * 60 * 1000L)
    }

    @Test
    fun calculateNextDueDate_setsHourTo9() {
        val result = PaymentDateUtils.calculateNextDueDate(dayOfMonth = 10)
        val cal = Calendar.getInstance().apply { timeInMillis = result }

        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(9)
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0)
    }

    @Test
    fun getNextMonthDueDate_fromDecember_goesToNextYear() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 20, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val next = PaymentDateUtils.getNextMonthDueDate(cal.timeInMillis, dayOfMonth = 20)

        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertThat(nextCal.get(Calendar.YEAR)).isEqualTo(2025)
        assertThat(nextCal.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY)
        assertThat(nextCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(20)
    }
}