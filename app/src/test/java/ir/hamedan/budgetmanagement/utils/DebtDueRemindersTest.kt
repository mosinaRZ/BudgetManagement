package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DebtDueRemindersTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 10, 25)

    private fun millis(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun oneTime(
        due: LocalDate?,
        total: Long = 1_000_000L,
        paid: Long = 0L,
        settled: Boolean = false,
        type: String = "DEBT"
    ) = DebtCreditEntity(
        type = type,
        personName = "Ali",
        totalAmount = total,
        paidAmount = paid,
        dueDateMillis = due?.let { millis(it) } ?: 0L,
        isSettled = settled
    )

    private fun installment(
        dueDay: Int,
        total: Long = 12_000_000L,
        monthly: Long = 1_000_000L,
        paid: Long = 0L,
        finalDate: LocalDate = LocalDate.of(2027, 10, 28)
    ) = DebtCreditEntity(
        type = "DEBT",
        personName = "Ali",
        totalAmount = total,
        paidAmount = paid,
        isMonthly = true,
        monthlyAmount = monthly,
        dueDay = dueDay,
        dueDateMillis = millis(finalDate)
    )

    // ---------------------------------------------------------------- steps

    @Test
    fun stepFor_mapsDaysLeftToSevenThreeOne() {
        assertThat(DebtDueReminders.stepFor(8)).isNull()
        assertThat(DebtDueReminders.stepFor(7)).isEqualTo(7)
        assertThat(DebtDueReminders.stepFor(4)).isEqualTo(7)
        assertThat(DebtDueReminders.stepFor(3)).isEqualTo(3)
        assertThat(DebtDueReminders.stepFor(2)).isEqualTo(3)
        assertThat(DebtDueReminders.stepFor(1)).isEqualTo(1)
        assertThat(DebtDueReminders.stepFor(0)).isEqualTo(1)
        assertThat(DebtDueReminders.stepFor(-1)).isNull()
    }

    @Test
    fun shouldSend_neverRepeatsAndNeverGoesBackwards() {
        assertThat(DebtDueReminders.shouldSend(7, null)).isTrue()
        assertThat(DebtDueReminders.shouldSend(7, 7)).isFalse()
        assertThat(DebtDueReminders.shouldSend(3, 7)).isTrue()
        assertThat(DebtDueReminders.shouldSend(3, 3)).isFalse()
        assertThat(DebtDueReminders.shouldSend(7, 3)).isFalse()
        assertThat(DebtDueReminders.shouldSend(1, 3)).isTrue()
        assertThat(DebtDueReminders.shouldSend(1, 1)).isFalse()
        assertThat(DebtDueReminders.shouldSend(3, 1)).isFalse()
    }

    private fun simulate(runsAtDaysLeft: List<Int>): List<Int> {
        var last: Int? = null
        val sent = mutableListOf<Int>()
        for (daysLeft in runsAtDaysLeft) {
            val step = DebtDueReminders.stepFor(daysLeft) ?: continue
            if (DebtDueReminders.shouldSend(step, last)) {
                sent += step
                last = step
            }
        }
        return sent
    }

    @Test
    fun dailyRuns_sendExactlySevenThreeAndOneOnce() {
        // 12 days before the due date up to 2 days after it, twice a day.
        val runs = (12 downTo -2).flatMap { listOf(it, it) }
        assertThat(simulate(runs)).containsExactly(7, 3, 1).inOrder()
    }

    @Test
    fun missedRuns_areCaughtUpWithoutDuplicates() {
        // The phone was off: the worker only ran 5, 2 and 0 days before the due date.
        assertThat(simulate(listOf(5, 2, 0))).containsExactly(7, 3, 1).inOrder()
        // Item created 2 days before it is due: a single reminder, then the one for tomorrow.
        assertThat(simulate(listOf(2, 2, 1, 0))).containsExactly(3, 1).inOrder()
        // Created 10 days ahead but the app was reinstalled 1 day before.
        assertThat(simulate(listOf(1, 0))).containsExactly(1).inOrder()
    }

    // ----------------------------------------------------------- due dates

    @Test
    fun oneTimeItem_usesTheStoredDueDate() {
        val info = DebtDueReminders.dueInfo(oneTime(today.plusDays(3)), today, zone)!!
        assertThat(info.dueDate).isEqualTo(today.plusDays(3))
        assertThat(info.daysLeft).isEqualTo(3)
    }

    @Test
    fun oneTimeItem_dueTodayHasZeroDaysLeftAndOverdueIsNegative() {
        assertThat(DebtDueReminders.dueInfo(oneTime(today), today, zone)!!.daysLeft).isEqualTo(0)
        assertThat(DebtDueReminders.dueInfo(oneTime(today.minusDays(2)), today, zone)!!.daysLeft).isEqualTo(-2)
        assertThat(DebtDueReminders.stepFor(-2)).isNull()
    }

    @Test
    fun itemsWithoutADueDateOrAlreadyPaidAreIgnored() {
        assertThat(DebtDueReminders.dueInfo(oneTime(null), today, zone)).isNull()
        assertThat(DebtDueReminders.dueInfo(oneTime(today.plusDays(3), settled = true), today, zone)).isNull()
        assertThat(
            DebtDueReminders.dueInfo(oneTime(today.plusDays(3), total = 500L, paid = 500L), today, zone)
        ).isNull()
    }

    @Test
    fun installment_gregorianUsesTheNextInstallmentDayNotTheFinalDate() {
        // Final installment is a year away, but the next one is on the 28th of this month.
        val info = DebtDueReminders.dueInfo(installment(dueDay = 28), today, zone)!!
        assertThat(info.dueDate).isEqualTo(LocalDate.of(2026, 10, 28))
        assertThat(info.daysLeft).isEqualTo(3)
    }

    @Test
    fun installment_dueTodayCountsAsToday_andAPassedDayRollsToNextMonth() {
        assertThat(DebtDueReminders.dueInfo(installment(dueDay = 25), today, zone)!!.daysLeft).isEqualTo(0)

        val passed = DebtDueReminders.dueInfo(installment(dueDay = 24), today, zone)!!
        assertThat(passed.dueDate).isEqualTo(LocalDate.of(2026, 11, 24))
        assertThat(passed.daysLeft).isEqualTo(30)
    }

    @Test
    fun installment_jalaliDayFollowsTheJalaliCalendar() {
        val due = DateUtils.fromJalali(1405, 8, 10)
        val stored = DueDay.encode(10, jalali = true)

        val weekBefore = DebtDueReminders.dueInfo(installment(stored), due.minusDays(7), zone)!!
        assertThat(weekBefore.dueDate).isEqualTo(due)
        assertThat(weekBefore.daysLeft).isEqualTo(7)

        val dayBefore = DebtDueReminders.dueInfo(installment(stored), due.minusDays(1), zone)!!
        assertThat(dayBefore.daysLeft).isEqualTo(1)
    }

    @Test
    fun installment_withAnInvalidDayFallsBackToTheStoredDate() {
        val item = installment(dueDay = 0, finalDate = today.plusDays(5))
        assertThat(DebtDueReminders.dueInfo(item, today, zone)!!.daysLeft).isEqualTo(5)
    }

    // -------------------------------------------------------------- amounts

    @Test
    fun amountDue_isTheInstallmentOrEverythingStillOwed() {
        assertThat(DebtDueReminders.amountDue(installment(28, total = 12_000_000L, monthly = 1_000_000L, paid = 0L)))
            .isEqualTo(1_000_000L)
        // Last installment is smaller than the monthly amount.
        assertThat(DebtDueReminders.amountDue(installment(28, total = 12_000_000L, monthly = 1_000_000L, paid = 11_600_000L)))
            .isEqualTo(400_000L)
        assertThat(DebtDueReminders.amountDue(oneTime(today, total = 900_000L, paid = 200_000L)))
            .isEqualTo(700_000L)
        assertThat(DebtDueReminders.amountDue(installment(28, total = 5_000L, monthly = 0L, paid = 1_000L)))
            .isEqualTo(4_000L)
    }
}