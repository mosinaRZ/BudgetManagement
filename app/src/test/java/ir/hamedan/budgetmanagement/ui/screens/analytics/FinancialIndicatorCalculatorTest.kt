package ir.hamedan.budgetmanagement.ui.screens.analytics

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialIndicatorCalculatorTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.of(2026, 11, 20)
    private val nowMillis: Long = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private val food = CategoryEntity(id = "cat-food", title = "FOOD")
    private val goalCat = CategoryEntity(id = "cat-goal", title = "SAVING_GOAL")
    private val categories = listOf(food, goalCat)

    private fun millis(d: LocalDate) = d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun tx(d: LocalDate, type: String, amount: Long, cat: String = "cat-food") =
        TransactionEntity(title = "t", amount = amount, categoryId = cat, type = type, timestamp = millis(d))

    private fun build(
        txs: List<TransactionEntity>,
        window: PeriodWindow = PeriodWindowResolver.resolve(TimeFilter.MONTHLY, false, null, null, null, today, emptyList()),
        limits: List<BudgetLimitEntity> = emptyList(),
        goals: List<SavingGoalEntity> = emptyList()
    ) = FinancialIndicatorCalculator.build(window, txs, categories, limits, goals, today, nowMillis, zone)

    private val nov = LocalDate.of(2026, 11, 5)
    private val oct = LocalDate.of(2026, 10, 5)

    // ── ۱) نرخ پس‌انداز ──

    @Test
    fun savingsRate_thresholds() {
        fun toneFor(expense: Long) = build(listOf(tx(nov, "INCOME", 1000), tx(nov, "EXPENSE", expense))).savings
        assertThat(toneFor(700).ratePercent).isEqualTo(30.0)
        assertThat(toneFor(700).tone).isEqualTo(IndicatorTone.GOOD)
        assertThat(toneFor(800).tone).isEqualTo(IndicatorTone.WARNING)   // دقیقاً ۲۰٪ زرد است
        assertThat(toneFor(900).tone).isEqualTo(IndicatorTone.WARNING)   // دقیقاً ۱۰٪ زرد است
        assertThat(toneFor(901).tone).isEqualTo(IndicatorTone.BAD)
        assertThat(toneFor(1500).tone).isEqualTo(IndicatorTone.BAD)      // منفی
    }

    @Test
    fun savingsRate_withoutIncome_isNullAndNeutral() {
        val s = build(listOf(tx(nov, "EXPENSE", 100))).savings
        assertThat(s.ratePercent).isNull()
        assertThat(s.tone).isEqualTo(IndicatorTone.NEUTRAL)
    }

    @Test
    fun goalTransfers_areExcludedFromIncomeAndExpense() {
        val s = build(
            listOf(
                tx(nov, "INCOME", 1000),
                tx(nov, "EXPENSE", 500, cat = "cat-goal"),
                tx(nov, "INCOME", 300, cat = "cat-goal")
            )
        )
        assertThat(s.savings.income).isEqualTo(1000L)
        assertThat(s.savings.expense).isEqualTo(0L)
        assertThat(s.excludesSavingGoalTransfers).isTrue()
    }

    // ── ۲) نرخ مصرف ──

    @Test
    fun burnRate_projectsMonthEnd() {
        val b = build(listOf(tx(nov, "EXPENSE", 2000))).burn
        assertThat(b.elapsedDays).isEqualTo(20)
        assertThat(b.totalDays).isEqualTo(30)
        assertThat(b.dailyBurn).isEqualTo(100L)
        assertThat(b.projectedExpense).isEqualTo(3000L)
    }

    @Test
    fun burnRate_pastMonth_hasNoProjection() {
        val window = PeriodWindowResolver.resolve(TimeFilter.MONTHLY, false, 9, null, null, today, emptyList()) // اکتبر
        val b = build(listOf(tx(oct, "EXPENSE", 3100)), window).burn
        assertThat(b.projectedExpense).isNull()
        assertThat(b.dailyBurn).isEqualTo(100L)
    }

    @Test
    fun burnRate_allPeriod_hasNoProjection() {
        val window = PeriodWindowResolver.resolve(TimeFilter.ALL, false, null, null, null, today, listOf(oct))
        assertThat(build(listOf(tx(oct, "EXPENSE", 100)), window).burn.projectedExpense).isNull()
    }

    // ── ۳) تغییر نسبت به دوره‌ی قبل ──

    @Test
    fun periodChange_isLikeForLike_andToned() {
        val txs = listOf(
            tx(nov, "INCOME", 2000), tx(nov, "EXPENSE", 1500),
            tx(oct, "INCOME", 1000), tx(oct, "EXPENSE", 1000),
            tx(LocalDate.of(2026, 10, 28), "EXPENSE", 9999) // خارج از ۲۰ روز اول؛ نباید حساب شود
        )
        val c = build(txs).change!!
        assertThat(c.isLikeForLike).isTrue()
        assertThat(c.income.percent).isEqualTo(100.0)
        assertThat(c.income.tone).isEqualTo(IndicatorTone.GOOD)
        assertThat(c.expense.percent).isEqualTo(50.0)
        assertThat(c.expense.tone).isEqualTo(IndicatorTone.BAD)
        assertThat(c.net.current).isEqualTo(500L)
        assertThat(c.net.previous).isEqualTo(0L)
        assertThat(c.net.percent).isNull()
    }

    @Test
    fun periodChange_absentForAllPeriod() {
        val window = PeriodWindowResolver.resolve(TimeFilter.ALL, false, null, null, null, today, listOf(oct))
        assertThat(build(listOf(tx(oct, "INCOME", 10)), window).change).isNull()
    }

    // ── ۴) سقف‌های بودجه ──

    @Test
    fun limitStatus_rules() {
        val f = FinancialIndicatorCalculator
        assertThat(f.limitStatus(0.8, 0.5, false)).isEqualTo(LimitStatus.DANGER)
        assertThat(f.limitStatus(0.5, 0.5, false)).isEqualTo(LimitStatus.SAFE)
        assertThat(f.limitStatus(0.6, 0.5, false)).isEqualTo(LimitStatus.WATCH)
        assertThat(f.limitStatus(1.2, 0.5, false)).isEqualTo(LimitStatus.EXCEEDED)
        assertThat(f.limitStatus(0.9, 1.0, true)).isEqualTo(LimitStatus.MET)
        assertThat(f.limitStatus(1.01, 1.0, true)).isEqualTo(LimitStatus.EXCEEDED)
    }

    @Test
    fun budgetAdherence_countsRespectedLimits() {
        fun limit(id: String, max: Long) = BudgetLimitEntity(
            id = id, categoryId = "cat-food", maxLimit = max,
            startDate = millis(LocalDate.of(2026, 11, 1)), endDate = millis(LocalDate.of(2026, 11, 30))
        )
        // مصرف دسته‌ی خوراکی = ۸۰۰
        val txs = listOf(tx(nov, "EXPENSE", 800))
        val b = build(txs, limits = listOf(limit("a", 1000), limit("b", 500))).budget!!
        assertThat(b.total).isEqualTo(2)
        assertThat(b.respected).isEqualTo(1)
        assertThat(b.percent).isEqualTo(50.0)
        assertThat(b.items.first().status).isEqualTo(LimitStatus.EXCEEDED) // شدیدترین اول
    }

    @Test
    fun budgetAdherence_ignoresInactiveAndFutureLimits() {
        val start = millis(LocalDate.of(2026, 12, 1))
        val future = BudgetLimitEntity(categoryId = "cat-food", maxLimit = 100, startDate = start, endDate = start + 1000)
        val inactive = BudgetLimitEntity(
            categoryId = "cat-food", maxLimit = 100, isActive = false,
            startDate = millis(LocalDate.of(2026, 11, 1)), endDate = millis(LocalDate.of(2026, 11, 30))
        )
        assertThat(build(emptyList(), limits = listOf(future, inactive)).budget).isNull()
    }

    // ── ۵) اهداف پس‌انداز ──

    @Test
    fun goals_progressAndEta() {
        val goals = listOf(
            SavingGoalEntity(id = "g1", title = "سفر", targetAmount = 1000, currentAmount = 400, monthlyAmount = 200),
            SavingGoalEntity(id = "g2", title = "بی‌برنامه", targetAmount = 1000, currentAmount = 100, monthlyAmount = 0),
            SavingGoalEntity(id = "g3", title = "تمام", targetAmount = 500, currentAmount = 700, monthlyAmount = 50)
        )
        val g = build(emptyList(), goals = goals).goals!!
        val byId = g.goals.associateBy { it.id }
        assertThat(byId.getValue("g1").percent).isEqualTo(40.0)
        assertThat(byId.getValue("g1").etaMonths).isEqualTo(3)
        assertThat(byId.getValue("g1").etaGregorian).isEqualTo(2027 to 2)   // نوامبر ۲۰۲۶ + ۳ ماه
        assertThat(byId.getValue("g2").etaMonths).isNull()
        assertThat(byId.getValue("g3").completed).isTrue()
        assertThat(byId.getValue("g3").percent).isEqualTo(100.0)
        assertThat(g.completedCount).isEqualTo(1)
        assertThat(g.goals.last().id).isEqualTo("g3") // تمام‌شده‌ها آخر
    }

    // ── بازه‌ها ──

    @Test
    fun window_persianMonth_matchesJalaliCalendar() {
        // ۲۹ آبان ۱۴۰۵ = ۲۰ نوامبر ۲۰۲۶
        val w = PeriodWindowResolver.resolve(TimeFilter.MONTHLY, true, null, null, null, today, emptyList())
        assertThat(w.start).isEqualTo(LocalDate.of(2026, 10, 23))
        assertThat(w.end).isEqualTo(LocalDate.of(2026, 11, 21))
        assertThat(w.previousEnd).isEqualTo(LocalDate.of(2026, 10, 22))
        assertThat(w.elapsedDays(today)).isEqualTo(29)
        assertThat(w.totalDays).isEqualTo(30)
    }

    @Test
    fun window_customRange_previousIsEqualLength() {
        val w = PeriodWindowResolver.resolve(
            TimeFilter.ALL, false, null, LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 20), today, emptyList()
        )
        assertThat(w.kind).isEqualTo(PeriodKind.CUSTOM)
        assertThat(w.previousStart).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(w.previousEnd).isEqualTo(LocalDate.of(2026, 11, 10))
    }

    @Test
    fun window_englishWeek_startsOnSunday() {
        val w = PeriodWindowResolver.resolve(TimeFilter.WEEKLY, false, null, null, null, today, emptyList())
        assertThat(w.start).isEqualTo(LocalDate.of(2026, 11, 15)) // یکشنبه
        assertThat(w.end).isEqualTo(LocalDate.of(2026, 11, 21))
    }
}