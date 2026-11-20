package ir.hamedan.budgetmanagement.ui.screens.analytics

import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * شاخص‌های مالی صفحه‌ی آنالیز.
 *
 * این فایل عمداً «خالص» است (بدون Compose/Android) تا کاملاً با تست واحد قابل بررسی باشد.
 *
 * قرارداد محاسبه:
 *  - واریز به قلک (هزینه) و برداشت از قلک (درآمد) انتقال داخلی‌اند، نه مصرف و نه درآمد؛
 *    پس از نرخ پس‌انداز، نرخ مصرف و مقایسه‌ها حذف می‌شوند. (واریز به قلک خودش «پس‌انداز» است.)
 *  - همه‌ی مبالغ Long و بر حسب واحد ذخیره‌سازی (تومان) هستند؛ تبدیل به ریال فقط هنگام نمایش.
 */

enum class IndicatorTone { GOOD, WARNING, BAD, NEUTRAL }

enum class PeriodKind { DAY, WEEK, MONTH, YEAR, CUSTOM, ALL }

/** بازه‌ی تحلیل (بسته از هر دو سو) و بازه‌ی قبلی هم‌طول/هم‌نوع آن. */
data class PeriodWindow(
    val kind: PeriodKind,
    val start: LocalDate,
    val end: LocalDate,
    val previousStart: LocalDate?,
    val previousEnd: LocalDate?
) {
    val totalDays: Int
        get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)

    /** تعداد روزهای سپری‌شده از بازه تا [today] (۰ اگر بازه هنوز شروع نشده). */
    fun elapsedDays(today: LocalDate): Int = when {
        today.isBefore(start) -> 0
        today.isAfter(end) -> totalDays
        else -> (ChronoUnit.DAYS.between(start, today) + 1).toInt()
    }
}

// ───────────────────────── مدل‌های خروجی ─────────────────────────

data class SavingsRateIndicator(
    val income: Long,
    val expense: Long,
    val net: Long,
    /** null یعنی درآمدی ثبت نشده و نرخ تعریف‌شده نیست. یک رقم اعشار گرد شده است. */
    val ratePercent: Double?,
    val tone: IndicatorTone,
    val previousRatePercent: Double?,
    /** اختلاف نرخ با دوره‌ی قبل بر حسب «واحد درصد». */
    val deltaPoints: Double?
)

data class BurnRateIndicator(
    val expense: Long,
    val dailyBurn: Long,
    val elapsedDays: Int,
    val totalDays: Int,
    val elapsedFraction: Float,
    /** پیش‌بینی هزینه‌ی پایان بازه؛ null اگر بازه تمام شده/قابل پیش‌بینی نیست. */
    val projectedExpense: Long?,
    /** در چند روز اول بازه‌های طولانی، پیش‌بینی پرنوسان است. */
    val lowConfidence: Boolean,
    val previousFullExpense: Long?,
    val projectedVsPreviousPercent: Double?
)

data class ChangeMetric(
    val current: Long,
    val previous: Long,
    /** null وقتی دوره‌ی قبل صفر بوده و درصد معنی ندارد. */
    val percent: Double?,
    val tone: IndicatorTone
)

data class PeriodChangeIndicator(
    val income: ChangeMetric,
    val expense: ChangeMetric,
    val net: ChangeMetric,
    val previousStart: LocalDate,
    val previousEnd: LocalDate,
    /** true: مقایسه با همان تعداد روزِ اولِ دوره‌ی قبل (منصفانه برای بازه‌ی در جریان). */
    val isLikeForLike: Boolean,
    val hasPreviousData: Boolean
)

enum class LimitStatus { EXCEEDED, DANGER, WATCH, SAFE, MET }

data class LimitProgress(
    val limitId: String,
    val categoryId: String,
    /** کلید/عنوان دسته؛ UI آن را با StringMapper محلی‌سازی می‌کند. */
    val categoryTitle: String,
    val emoji: String,
    val maxLimit: Long,
    val spent: Long,
    val usedFraction: Double,
    val elapsedFraction: Double,
    val status: LimitStatus,
    val isFinished: Boolean
)

data class BudgetAdherenceIndicator(
    val total: Int,
    val respected: Int,
    val percent: Double,
    val atRisk: Int,
    val items: List<LimitProgress>
)

data class GoalProgress(
    val id: String,
    val title: String,
    val icon: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val monthlyAmount: Long,
    val percent: Double,
    val remaining: Long,
    val completed: Boolean,
    val etaMonths: Int?,
    /** (سال، ماه) میلادی/جلالی رسیدن تقریبی به هدف. */
    val etaGregorian: Pair<Int, Int>?,
    val etaJalali: Pair<Int, Int>?
)

data class SavingGoalsIndicator(
    val goals: List<GoalProgress>,
    val overallPercent: Double,
    val completedCount: Int
)

data class FinancialIndicators(
    val window: PeriodWindow,
    val savings: SavingsRateIndicator,
    val burn: BurnRateIndicator,
    val change: PeriodChangeIndicator?,
    val budget: BudgetAdherenceIndicator?,
    val goals: SavingGoalsIndicator?,
    /** آیا انتقال‌های قلک از محاسبات کنار گذاشته شده‌اند (برای یادداشت زیر کارت). */
    val excludesSavingGoalTransfers: Boolean
)

// ───────────────────────── تعیین بازه ─────────────────────────

object PeriodWindowResolver {

    /** بازه‌ی تحلیل را دقیقاً هم‌معنا با فیلتر/ستون انتخاب‌شده در صفحه برمی‌گرداند. */
    fun resolve(
        filter: TimeFilter,
        isPersian: Boolean,
        bucketIndex: Int?,
        customStart: LocalDate?,
        customEnd: LocalDate?,
        today: LocalDate,
        transactionDates: List<LocalDate>
    ): PeriodWindow {
        if (customStart != null && customEnd != null) {
            val len = ChronoUnit.DAYS.between(customStart, customEnd) + 1
            return PeriodWindow(
                PeriodKind.CUSTOM, customStart, customEnd,
                customStart.minusDays(len), customStart.minusDays(1)
            )
        }
        return if (bucketIndex != null) bucketWindow(filter, isPersian, bucketIndex, today, transactionDates)
        else currentWindow(filter, isPersian, today, transactionDates)
    }

    private fun currentWindow(
        filter: TimeFilter,
        isPersian: Boolean,
        today: LocalDate,
        dates: List<LocalDate>
    ): PeriodWindow = when (filter) {
        TimeFilter.DAILY -> dayWindow(today)
        TimeFilter.WEEKLY ->
            if (isPersian) {
                val (jy, jm, jd) = DateUtils.toJalali(today)
                persianWeekChunk(jy, jm, ((jd - 1) / 7).coerceAtMost(4))
            } else {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                weekWindow(start, start.plusDays(6))
            }
        TimeFilter.MONTHLY -> monthWindowOf(today, isPersian)
        TimeFilter.ALL -> PeriodWindow(
            PeriodKind.ALL, dates.minOrNull() ?: today, today, null, null
        )
    }

    private fun bucketWindow(
        filter: TimeFilter,
        isPersian: Boolean,
        bucket: Int,
        today: LocalDate,
        dates: List<LocalDate>
    ): PeriodWindow = when (filter) {
        TimeFilter.DAILY -> {
            if (isPersian) {
                val (jy, jm, _) = DateUtils.toJalali(today)
                val day = (bucket + 1).coerceIn(1, DateUtils.getDaysInJalaliMonth(jy, jm))
                dayWindow(DateUtils.fromJalali(jy, jm, day))
            } else {
                val ym = YearMonth.from(today)
                dayWindow(ym.atDay((bucket + 1).coerceIn(1, ym.lengthOfMonth())))
            }
        }
        TimeFilter.WEEKLY -> {
            if (isPersian) {
                val (jy, jm, _) = DateUtils.toJalali(today)
                persianWeekChunk(jy, jm, bucket.coerceIn(0, 4))
            } else {
                val ym = YearMonth.from(today)
                val b = bucket.coerceIn(0, 4)
                val s = (7 * b + 1).coerceAtMost(ym.lengthOfMonth())
                val e = if (b >= 4) ym.lengthOfMonth() else min(7 * b + 7, ym.lengthOfMonth())
                weekWindow(ym.atDay(s), ym.atDay(e))
            }
        }
        TimeFilter.MONTHLY -> {
            if (isPersian) {
                val (jy, _, _) = DateUtils.toJalali(today)
                jalaliMonthWindow(jy, (bucket + 1).coerceIn(1, 12))
            } else {
                val ym = YearMonth.of(today.year, (bucket + 1).coerceIn(1, 12))
                gregorianMonthWindow(ym)
            }
        }
        TimeFilter.ALL -> {
            if (isPersian) {
                val years = dates.map { DateUtils.toJalali(it).first }.distinct().sorted()
                val year = years.getOrNull(bucket) ?: DateUtils.toJalali(today).first
                jalaliYearWindow(year)
            } else {
                val years = dates.map { it.year }.distinct().sorted()
                val year = years.getOrNull(bucket) ?: today.year
                PeriodWindow(
                    PeriodKind.YEAR,
                    LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
                    LocalDate.of(year - 1, 1, 1), LocalDate.of(year - 1, 12, 31)
                )
            }
        }
    }

    private fun dayWindow(day: LocalDate) =
        PeriodWindow(PeriodKind.DAY, day, day, day.minusDays(1), day.minusDays(1))

    private fun weekWindow(start: LocalDate, end: LocalDate): PeriodWindow {
        val len = ChronoUnit.DAYS.between(start, end) + 1
        return PeriodWindow(PeriodKind.WEEK, start, end, start.minusDays(len), start.minusDays(1))
    }

    /** هفته‌ی ماه جلالی: ۱–۷، ۸–۱۴، ۱۵–۲۱، ۲۲–۲۸ و ۲۹ تا آخر ماه (همان تقسیم نمودار). */
    private fun persianWeekChunk(jy: Int, jm: Int, chunk: Int): PeriodWindow {
        val dim = DateUtils.getDaysInJalaliMonth(jy, jm)
        val s = (7 * chunk + 1).coerceAtMost(dim)
        val e = if (chunk >= 4) dim else min(7 * chunk + 7, dim)
        return weekWindow(DateUtils.fromJalali(jy, jm, s), DateUtils.fromJalali(jy, jm, e))
    }

    private fun monthWindowOf(day: LocalDate, isPersian: Boolean): PeriodWindow =
        if (isPersian) {
            val (jy, jm, _) = DateUtils.toJalali(day)
            jalaliMonthWindow(jy, jm)
        } else gregorianMonthWindow(YearMonth.from(day))

    private fun gregorianMonthWindow(ym: YearMonth): PeriodWindow {
        val prev = ym.minusMonths(1)
        return PeriodWindow(
            PeriodKind.MONTH, ym.atDay(1), ym.atEndOfMonth(),
            prev.atDay(1), prev.atEndOfMonth()
        )
    }

    private fun jalaliMonthRange(jy: Int, jm: Int): Pair<LocalDate, LocalDate> =
        DateUtils.fromJalali(jy, jm, 1) to
                DateUtils.fromJalali(jy, jm, DateUtils.getDaysInJalaliMonth(jy, jm))

    private fun jalaliMonthWindow(jy: Int, jm: Int): PeriodWindow {
        val (s, e) = jalaliMonthRange(jy, jm)
        val (py, pm) = if (jm == 1) (jy - 1) to 12 else jy to (jm - 1)
        val (ps, pe) = jalaliMonthRange(py, pm)
        return PeriodWindow(PeriodKind.MONTH, s, e, ps, pe)
    }

    private fun jalaliYearWindow(jy: Int): PeriodWindow {
        fun range(y: Int) = DateUtils.fromJalali(y, 1, 1) to jalaliMonthRange(y, 12).second
        val (s, e) = range(jy)
        val (ps, pe) = range(jy - 1)
        return PeriodWindow(PeriodKind.YEAR, s, e, ps, pe)
    }
}

// ───────────────────────── محاسبه ─────────────────────────

object FinancialIndicatorCalculator {

    const val SAVING_GOAL_KEY = "SAVING_GOAL"

    /** آستانه‌های رنگ نرخ پس‌انداز: بالای ۲۰ سبز، ۱۰ تا ۲۰ زرد، زیر ۱۰ قرمز. */
    const val SAVINGS_GOOD_ABOVE = 20.0
    const val SAVINGS_WARNING_FROM = 10.0

    private const val CHANGE_DEADBAND_PERCENT = 1.0
    private const val MAX_ETA_MONTHS = 1200

    internal data class Sums(val income: Long, val expense: Long)

    fun build(
        window: PeriodWindow,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        limits: List<BudgetLimitEntity>,
        goals: List<SavingGoalEntity>,
        today: LocalDate,
        nowMillis: Long,
        zone: ZoneId
    ): FinancialIndicators {
        val excludedIds = categories
            .filter { it.title.trim().equals(SAVING_GOAL_KEY, ignoreCase = true) }
            .map { it.id }
            .toSet()
        val operational = if (excludedIds.isEmpty()) transactions
        else transactions.filter { it.categoryId !in excludedIds }

        val current = sums(operational, window.start, window.end, zone)

        val elapsed = window.elapsedDays(today)
        val ongoing = elapsed in 1 until window.totalDays

        // مقایسه با دوره‌ی قبل: برای بازه‌ی در جریان فقط همان تعداد روزِ اول.
        val prevStart = window.previousStart
        val prevEnd = window.previousEnd
        val prevCompareEnd = if (prevStart != null && prevEnd != null) {
            if (ongoing) minOf(prevEnd, prevStart.plusDays((elapsed - 1).toLong())) else prevEnd
        } else null
        val previous = if (prevStart != null && prevCompareEnd != null)
            sums(operational, prevStart, prevCompareEnd, zone) else null

        return FinancialIndicators(
            window = window,
            savings = savingsRate(current, previous),
            burn = burnRate(window, current, elapsed, ongoing, operational, zone),
            change = if (prevStart != null && prevCompareEnd != null && previous != null && elapsed > 0)
                periodChange(current, previous, prevStart, prevCompareEnd, ongoing) else null,
            budget = budgetAdherence(window, transactions, categories, limits, nowMillis, zone),
            goals = savingGoals(goals, today),
            excludesSavingGoalTransfers = excludedIds.isNotEmpty()
        )
    }

    // ── مجموع درآمد/هزینه در بازه ──

    private fun sums(txs: List<TransactionEntity>, start: LocalDate, end: LocalDate, zone: ZoneId): Sums {
        val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val toExclusive = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var income = 0L
        var expense = 0L
        for (tx in txs) {
            if (tx.timestamp < from || tx.timestamp >= toExclusive) continue
            if (tx.type == "INCOME") income += tx.amount else if (tx.type == "EXPENSE") expense += tx.amount
        }
        return Sums(income, expense)
    }

    // ── ۱) نرخ پس‌انداز ──

    internal fun rateOf(s: Sums): Double? =
        if (s.income > 0L) ((s.income - s.expense).toDouble() / s.income * 100.0 * 10).roundToLong() / 10.0
        else null

    internal fun toneForSavingsRate(rate: Double?): IndicatorTone = when {
        rate == null -> IndicatorTone.NEUTRAL
        rate > SAVINGS_GOOD_ABOVE -> IndicatorTone.GOOD
        rate >= SAVINGS_WARNING_FROM -> IndicatorTone.WARNING
        else -> IndicatorTone.BAD
    }

    private fun savingsRate(cur: Sums, prev: Sums?): SavingsRateIndicator {
        val rate = rateOf(cur)
        val prevRate = prev?.let { rateOf(it) }
        return SavingsRateIndicator(
            income = cur.income,
            expense = cur.expense,
            net = cur.income - cur.expense,
            ratePercent = rate,
            tone = toneForSavingsRate(rate),
            previousRatePercent = prevRate,
            deltaPoints = if (rate != null && prevRate != null) ((rate - prevRate) * 10).roundToLong() / 10.0 else null
        )
    }

    // ── ۲) نرخ مصرف روزانه و پیش‌بینی ──

    private fun burnRate(
        window: PeriodWindow,
        cur: Sums,
        elapsed: Int,
        ongoing: Boolean,
        operational: List<TransactionEntity>,
        zone: ZoneId
    ): BurnRateIndicator {
        val total = window.totalDays
        val daily = if (elapsed > 0) (cur.expense.toDouble() / elapsed).roundToLong() else 0L
        val projectable = window.kind != PeriodKind.ALL && total > 1 && ongoing
        val projected = if (projectable) (cur.expense.toDouble() / elapsed * total).roundToLong() else null

        val prevFull = if (window.previousStart != null && window.previousEnd != null)
            sums(operational, window.previousStart, window.previousEnd, zone).expense.takeIf { it > 0L }
        else null

        return BurnRateIndicator(
            expense = cur.expense,
            dailyBurn = daily,
            elapsedDays = elapsed,
            totalDays = total,
            elapsedFraction = (elapsed.toFloat() / total).coerceIn(0f, 1f),
            projectedExpense = projected,
            lowConfidence = projectable && elapsed < 3 && total > 7,
            previousFullExpense = prevFull,
            projectedVsPreviousPercent =
                if (projected != null && prevFull != null) (projected - prevFull).toDouble() / prevFull * 100.0 else null
        )
    }

    // ── ۳) تغییر نسبت به دوره‌ی قبل ──

    private enum class Polarity { UP_IS_GOOD, UP_IS_BAD }

    private fun metric(cur: Long, prev: Long, polarity: Polarity): ChangeMetric {
        val percent = if (prev != 0L) (cur - prev).toDouble() / abs(prev.toDouble()) * 100.0 else null
        val tone = when {
            percent == null || abs(percent) < CHANGE_DEADBAND_PERCENT -> IndicatorTone.NEUTRAL
            (percent > 0) == (polarity == Polarity.UP_IS_GOOD) -> IndicatorTone.GOOD
            else -> IndicatorTone.BAD
        }
        return ChangeMetric(cur, prev, percent, tone)
    }

    private fun periodChange(
        cur: Sums,
        prev: Sums,
        prevStart: LocalDate,
        prevEnd: LocalDate,
        likeForLike: Boolean
    ) = PeriodChangeIndicator(
        income = metric(cur.income, prev.income, Polarity.UP_IS_GOOD),
        expense = metric(cur.expense, prev.expense, Polarity.UP_IS_BAD),
        net = metric(cur.income - cur.expense, prev.income - prev.expense, Polarity.UP_IS_GOOD),
        previousStart = prevStart,
        previousEnd = prevEnd,
        isLikeForLike = likeForLike,
        hasPreviousData = prev.income != 0L || prev.expense != 0L
    )

    // ── ۴) پایبندی به سقف بودجه ──

    /**
     * وضعیت یک سقف: «مصرف‌شده» در برابر «زمان‌سپری‌شده».
     * ۸۰٪ مصرف در نیمه‌ی بازه (فاصله ۳۰ واحد) → خطر، حتی اگر هنوز رد نشده باشد.
     */
    internal fun limitStatus(used: Double, elapsed: Double, finished: Boolean): LimitStatus {
        if (used > 1.0) return LimitStatus.EXCEEDED
        if (finished) return LimitStatus.MET
        // گرد کردن تا خطای ممیز شناور (مثلاً 0.6 - 0.5 = 0.0999…) مرز را جابه‌جا نکند.
        val gap = ((used - elapsed) * 1000).roundToLong() / 1000.0
        return when {
            used >= 0.4 && gap >= 0.25 -> LimitStatus.DANGER
            used >= 0.9 && gap >= 0.10 -> LimitStatus.DANGER
            used >= 0.25 && gap >= 0.10 -> LimitStatus.WATCH
            else -> LimitStatus.SAFE
        }
    }

    private fun budgetAdherence(
        window: PeriodWindow,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        limits: List<BudgetLimitEntity>,
        nowMillis: Long,
        zone: ZoneId
    ): BudgetAdherenceIndicator? {
        val winStart = window.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val winEnd = window.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

        val items = limits
            .filter {
                it.isActive && it.maxLimit > 0L && it.startDate <= nowMillis &&
                        it.startDate <= winEnd && it.endDate >= winStart
            }
            .map { limit ->
                val spent = transactions
                    .filter {
                        it.type == "EXPENSE" && it.categoryId == limit.categoryId &&
                                it.timestamp in limit.startDate..limit.endDate
                    }
                    .sumOf { it.amount }
                val used = spent.toDouble() / limit.maxLimit
                val span = (limit.endDate - limit.startDate).coerceAtLeast(1L)
                val elapsed = ((nowMillis - limit.startDate).toDouble() / span).coerceIn(0.0, 1.0)
                val finished = nowMillis > limit.endDate
                val category = categories.firstOrNull { it.id == limit.categoryId }
                LimitProgress(
                    limitId = limit.id,
                    categoryId = limit.categoryId,
                    categoryTitle = category?.title ?: limit.categoryId,
                    emoji = category?.iconEmoji ?: "💰",
                    maxLimit = limit.maxLimit,
                    spent = spent,
                    usedFraction = used,
                    elapsedFraction = elapsed,
                    status = limitStatus(used, elapsed, finished),
                    isFinished = finished
                )
            }
            .sortedWith(compareBy<LimitProgress> { it.status.ordinal }.thenByDescending { it.usedFraction })

        if (items.isEmpty()) return null
        val respected = items.count { it.status != LimitStatus.EXCEEDED }
        return BudgetAdherenceIndicator(
            total = items.size,
            respected = respected,
            percent = respected.toDouble() / items.size * 100.0,
            atRisk = items.count { it.status == LimitStatus.DANGER || it.status == LimitStatus.WATCH },
            items = items
        )
    }

    // ── ۵) پیشرفت اهداف پس‌انداز ──

    private fun savingGoals(goals: List<SavingGoalEntity>, today: LocalDate): SavingGoalsIndicator? {
        val valid = goals.filter { it.targetAmount > 0L }
        if (valid.isEmpty()) return null

        val (jy, jm, _) = DateUtils.toJalali(today)
        val todayYm = YearMonth.from(today)

        val progress = valid.map { g ->
            val current = g.currentAmount.coerceAtLeast(0L)
            val completed = current >= g.targetAmount
            val remaining = (g.targetAmount - current).coerceAtLeast(0L)
            val months = if (!completed && g.monthlyAmount > 0L)
                ceil(remaining.toDouble() / g.monthlyAmount).toInt().takeIf { it in 1..MAX_ETA_MONTHS } else null
            val etaG = months?.let { todayYm.plusMonths(it.toLong()).let { ym -> ym.year to ym.monthValue } }
            val etaJ = months?.let {
                val idx = jy * 12 + (jm - 1) + it
                (idx / 12) to (idx % 12 + 1)
            }
            GoalProgress(
                id = g.id, title = g.title, icon = g.icon,
                targetAmount = g.targetAmount, currentAmount = current, monthlyAmount = g.monthlyAmount,
                percent = min(current.toDouble() / g.targetAmount * 100.0, 100.0),
                remaining = remaining, completed = completed,
                etaMonths = months, etaGregorian = etaG, etaJalali = etaJ
            )
        }.sortedWith(compareBy<GoalProgress> { it.completed }.thenByDescending { it.percent })

        val totalTarget = valid.sumOf { it.targetAmount }
        val totalSaved = valid.sumOf { it.currentAmount.coerceIn(0L, it.targetAmount) }
        return SavingGoalsIndicator(
            goals = progress,
            overallPercent = totalSaved.toDouble() / totalTarget * 100.0,
            completedCount = progress.count { it.completed }
        )
    }
}

/** گردکردن درصد برای نمایش (بدون اعشار). */
internal fun Double.roundedPercent(): Int = roundToInt()