package ir.hamedan.budgetmanagement.ui.tour

import androidx.compose.ui.graphics.Color
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsUiState
import ir.hamedan.budgetmanagement.ui.screens.analytics.CategoryExpenseModel
import ir.hamedan.budgetmanagement.ui.screens.analytics.FinancialIndicatorCalculator
import ir.hamedan.budgetmanagement.ui.screens.analytics.PeriodWindow
import ir.hamedan.budgetmanagement.ui.screens.analytics.PeriodWindowResolver
import ir.hamedan.budgetmanagement.ui.screens.analytics.TimeExpenseModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitUiModel
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * داده‌های نمونه‌ی تور.
 *
 * فقط در حافظه ساخته می‌شوند و هرگز به دیتابیس، همگام‌سازی یا اعلان‌ها راه پیدا نمی‌کنند؛
 * صفحه‌ها تا وقتی تور در جریان است این‌ها را به‌جای داده‌ی واقعی نشان می‌دهند و با پایان تور
 * همه‌چیز به حالت واقعی برمی‌گردد. شناسه‌ها با `tour_` شروع می‌شوند.
 */
object TourSampleData {

    private const val HOUR = 60L * 60L * 1000L
    private const val DAY = 24L * HOUR

    const val CAT_FOOD = "tour_cat_food"
    const val CAT_TRANSPORT = "tour_cat_transport"
    const val CAT_SHOPPING = "tour_cat_shopping"
    const val CAT_BILLS = "tour_cat_bills"
    const val CAT_FUN = "tour_cat_fun"
    const val CAT_SALARY = "tour_cat_salary"
    const val CAT_FREELANCE = "tour_cat_freelance"

    // ───────────────────────── دسته‌ها ─────────────────────────

    fun categories(isPersian: Boolean): List<CategoryEntity> = listOf(
        CategoryEntity(id = CAT_FOOD, title = if (isPersian) "خوراک" else "Food", iconEmoji = "🍔", isExpense = true),
        CategoryEntity(id = CAT_TRANSPORT, title = if (isPersian) "رفت‌وآمد" else "Transport", iconEmoji = "🚕", isExpense = true),
        CategoryEntity(id = CAT_SHOPPING, title = if (isPersian) "خرید" else "Shopping", iconEmoji = "🛍️", isExpense = true),
        CategoryEntity(id = CAT_BILLS, title = if (isPersian) "قبض‌ها" else "Bills", iconEmoji = "💡", isExpense = true),
        CategoryEntity(id = CAT_FUN, title = if (isPersian) "تفریح" else "Fun", iconEmoji = "🎬", isExpense = true),
        CategoryEntity(id = CAT_SALARY, title = if (isPersian) "حقوق" else "Salary", iconEmoji = "💼", isExpense = false),
        CategoryEntity(id = CAT_FREELANCE, title = if (isPersian) "پروژه" else "Freelance", iconEmoji = "💻", isExpense = false)
    )

    // ───────────────────────── تراکنش‌ها (خانه و تراکنش‌ها) ─────────────────────────

    /** از جدید به قدیم؛ زمان‌ها نسبت به «همین الان» تا سرتیترهای «امروز/دیروز/…» طبیعی دیده شوند. */
    fun transactions(isPersian: Boolean): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        fun tx(
            id: String, titleFa: String, titleEn: String, amount: Long, categoryId: String,
            income: Boolean, hoursAgo: Long, noteFa: String = "", noteEn: String = ""
        ) = TransactionEntity(
            id = id,
            title = if (isPersian) titleFa else titleEn,
            amount = amount,
            categoryId = categoryId,
            type = if (income) "INCOME" else "EXPENSE",
            timestamp = now - hoursAgo * HOUR,
            note = if (isPersian) noteFa else noteEn,
            createdAt = now - hoursAgo * HOUR,
            updatedAt = now - hoursAgo * HOUR
        )
        return listOf(
            tx("tour_tx_1", "قهوه با دوستا", "Coffee with friends", 90_000L, CAT_FOOD, false, 1, "کافه‌ی سر کوچه", "The corner café"),
            tx("tour_tx_2", "تاکسی اینترنتی", "Ride share", 180_000L, CAT_TRANSPORT, false, 4),
            tx("tour_tx_3", "ناهار", "Lunch", 320_000L, CAT_FOOD, false, 27),
            tx("tour_tx_4", "پروژه‌ی فریلنس", "Freelance project", 6_000_000L, CAT_FREELANCE, true, 50, "تحویل طراحی لوگو", "Logo design delivery"),
            tx("tour_tx_5", "کفش ورزشی", "Running shoes", 2_200_000L, CAT_SHOPPING, false, 74),
            tx("tour_tx_6", "قبض برق و اینترنت", "Electricity & internet", 650_000L, CAT_BILLS, false, 98),
            tx("tour_tx_7", "سینما", "Movie night", 450_000L, CAT_FUN, false, 122),
            tx("tour_tx_8", "حقوق ماهانه", "Monthly salary", 25_000_000L, CAT_SALARY, true, 170)
        )
    }

    // ───────────────────────── قلک‌ها ─────────────────────────

    fun goals(isPersian: Boolean): List<SavingGoalEntity> = listOf(
        SavingGoalEntity(
            id = "tour_goal_trip", title = if (isPersian) "سفر شمال" else "Beach trip",
            targetAmount = 40_000_000L, currentAmount = 18_500_000L, monthlyAmount = 3_000_000L, icon = "🏖️"
        ),
        SavingGoalEntity(
            id = "tour_goal_laptop", title = if (isPersian) "لپ‌تاپ جدید" else "New laptop",
            targetAmount = 60_000_000L, currentAmount = 12_000_000L, monthlyAmount = 4_000_000L, icon = "💻"
        )
    )

    // ───────────────────────── محدودیت‌ها ─────────────────────────

    fun limits(): List<BudgetLimitUiModel> {
        val now = System.currentTimeMillis()
        return listOf(
            BudgetLimitUiModel(
                entity = BudgetLimitEntity(
                    id = "tour_limit_food", categoryId = CAT_FOOD, maxLimit = 3_000_000L,
                    startDate = now - 10 * DAY, endDate = now + 20 * DAY
                ),
                currentSpent = 1_450_000.0,
                categoryEmoji = "🍔"
            ),
            BudgetLimitUiModel(
                entity = BudgetLimitEntity(
                    id = "tour_limit_shopping", categoryId = CAT_SHOPPING, maxLimit = 2_500_000L,
                    startDate = now - 10 * DAY, endDate = now + 20 * DAY
                ),
                currentSpent = 2_200_000.0,
                categoryEmoji = "🛍️"
            )
        )
    }

    // ───────────────────────── بدهی و طلب ─────────────────────────

    fun debts(isPersian: Boolean): List<DebtCreditEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            DebtCreditEntity(
                id = "tour_debt_ali", type = "DEBT", personName = if (isPersian) "علی" else "Ali",
                totalAmount = 5_000_000L, paidAmount = 2_000_000L, dueDateMillis = now + 5 * DAY
            ),
            DebtCreditEntity(
                id = "tour_credit_maryam", type = "CREDIT", personName = if (isPersian) "مریم" else "Maryam",
                totalAmount = 3_000_000L, paidAmount = 500_000L, dueDateMillis = now + 18 * DAY
            )
        )
    }

    /** تعداد اعلان‌های نخوانده‌ی نمایشی روی زنگ خانه. */
    const val SAMPLE_UNREAD_COUNT = 3

    // ───────────────────────── آمار ─────────────────────────

    /** اندیس ستونِ «این هفته» در نمودار ستونی (هفته‌های ماه، ۰ تا ۴). */
    fun currentWeekBucket(isPersian: Boolean): Int {
        val today = LocalDate.now()
        val day = if (isPersian) DateUtils.toJalali(today).third else today.dayOfMonth
        return ((day - 1) / 7).coerceIn(0, 4)
    }

    /** ستونی که تور خودکار روی آن «می‌زند»: هفته‌ی قبل تا بازه‌اش کاملاً در گذشته و پر از داده باشد. */
    fun demoBucket(isPersian: Boolean): Int {
        val current = currentWeekBucket(isPersian)
        return if (current > 0) current - 1 else current
    }

    /**
     * وضعیت کامل صفحه‌ی آمار با داده‌ی نمونه.
     * شاخص‌ها با همان ماشین‌حساب واقعی برنامه ساخته می‌شوند تا کارت‌ها دقیقاً مثل حالت واقعی دیده شوند؛
     * تراکنش‌های این بخش به بازه‌ی انتخاب‌شده «لنگر» می‌شوند تا همیشه داده‌ای برای نمایش باشد.
     *
     * @param selectedBucket اندیس ستونِ انتخاب‌شده یا null برای حالت پیش‌فرض.
     */
    fun analyticsState(isPersian: Boolean, selectedBucket: Int?): AnalyticsUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val nowMillis = System.currentTimeMillis()
        val cats = categories(isPersian)
        val currentIndex = currentWeekBucket(isPersian)

        val window = PeriodWindowResolver.resolve(
            filter = TimeFilter.WEEKLY,
            isPersian = isPersian,
            bucketIndex = selectedBucket,
            customStart = null,
            customEnd = null,
            today = today,
            transactionDates = listOf(today)
        )
        val indicatorTransactions = windowAnchoredTransactions(window, today, zone)

        val indicators = FinancialIndicatorCalculator.build(
            window = window,
            transactions = indicatorTransactions,
            categories = cats,
            limits = limits().map { it.entity },
            goals = goals(isPersian),
            today = today,
            nowMillis = nowMillis,
            zone = zone
        )

        val barValues = doubleArrayOf(1_400_000.0, 2_650_000.0, 1_950_000.0, 3_300_000.0, 900_000.0)
        val timeExpenses = barValues.mapIndexed { index, value ->
            TimeExpenseModel(
                labelFa = "هفته ${toPersianDigits(index + 1)}",
                labelEn = "Week ${index + 1}",
                totalAmount = value,
                isCurrent = index == currentIndex
            )
        }

        val totalExpense = if (selectedBucket != null) barValues[selectedBucket.coerceIn(0, 4)] else 7_900_000.0
        val totalIncome = if (selectedBucket != null) 9_500_000.0 else 31_000_000.0
        val shares = listOf(
            Triple(if (isPersian) "خوراک" else "Food", 0.38f, Color(0xFF26A69A)),
            Triple(if (isPersian) "خرید" else "Shopping", 0.29f, Color(0xFF7E57C2)),
            Triple(if (isPersian) "قبض‌ها" else "Bills", 0.17f, Color(0xFFFFB300)),
            Triple(if (isPersian) "رفت‌وآمد" else "Transport", 0.10f, Color(0xFF42A5F5)),
            Triple(if (isPersian) "تفریح" else "Fun", 0.06f, Color(0xFFEF5350))
        )
        val categoryExpenses = shares.map { (name, share, color) ->
            CategoryExpenseModel(
                categoryName = name,
                totalAmount = totalExpense * share,
                percentage = share * 100f,
                color = color
            )
        }

        val topExpenses = transactions(isPersian)
            .filter { it.type == "EXPENSE" }
            .sortedByDescending { it.amount }
            .take(3)

        val trend = if (selectedBucket != null) {
            listOf(400_000f, 900_000f, 1_500_000f, 2_100_000f, 2_650_000f)
        } else {
            listOf(3_200_000f, 7_800_000f, 6_900_000f, 11_400_000f, 14_900_000f)
        }

        return AnalyticsUiState(
            isLoading = false,
            hasAnyTransactionInDb = true,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            categoryExpenses = categoryExpenses,
            timeExpenses = timeExpenses,
            currentTimeIndex = currentIndex,
            topExpenses = topExpenses,
            averageExpense = totalExpense / 8.0,
            expenseTransactionCount = 8,
            heavyExpenseCount = topExpenses.size,
            heavyExpenseThreshold = totalExpense / 8.0,
            selectedTimeBucketIndex = selectedBucket,
            trendPoints = trend,
            trendHasEnoughData = true,
            trendCurrentIndex = if (selectedBucket != null) trend.lastIndex else currentIndex,
            selectedPeriod = TimeFilter.WEEKLY.name,
            isIncomeChartSelected = false,
            isCustomRange = false,
            indicators = indicators
        )
    }

    /**
     * تراکنش‌هایی که روی روزهای واقعیِ [window] (و آغازِ بازه‌ی قبلی برای مقایسه) نشانده می‌شوند.
     * هیچ‌کدام در آینده نیستند؛ اگر بازه تازه شروع شده باشد همه روی همان روز می‌افتند.
     */
    private fun windowAnchoredTransactions(
        window: PeriodWindow,
        today: LocalDate,
        zone: ZoneId
    ): List<TransactionEntity> {
        val lastDay = if (window.end.isBefore(today)) window.end else today
        val span = ChronoUnit.DAYS.between(window.start, lastDay).toInt().coerceAtLeast(0)

        fun millisAt(day: LocalDate, hour: Int): Long =
            day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

        fun inWindow(offset: Int, hour: Int): Long =
            millisAt(window.start.plusDays(offset.coerceIn(0, span).toLong()), hour)

        fun tx(id: String, amount: Long, categoryId: String, income: Boolean, at: Long) = TransactionEntity(
            id = id, title = id, amount = amount, categoryId = categoryId,
            type = if (income) "INCOME" else "EXPENSE", timestamp = at, createdAt = at, updatedAt = at
        )

        val current = listOf(
            tx("tour_an_1", 9_500_000L, CAT_SALARY, true, inWindow(0, 9)),
            tx("tour_an_2", 1_200_000L, CAT_FOOD, false, inWindow(span / 3, 10)),
            tx("tour_an_3", 2_200_000L, CAT_SHOPPING, false, inWindow(span / 2, 11)),
            tx("tour_an_4", 650_000L, CAT_BILLS, false, inWindow(span / 2, 12)),
            tx("tour_an_5", 380_000L, CAT_TRANSPORT, false, inWindow(span, 9))
        )

        val previousStart = window.previousStart ?: return current
        val previous = listOf(
            tx("tour_an_p1", 6_000_000L, CAT_FREELANCE, true, millisAt(previousStart, 9)),
            tx("tour_an_p2", 1_700_000L, CAT_FOOD, false, millisAt(previousStart, 12)),
            tx("tour_an_p3", 900_000L, CAT_SHOPPING, false, millisAt(previousStart, 15))
        )
        return current + previous
    }

    private fun toPersianDigits(value: Int): String =
        value.toString().map { if (it in '0'..'9') ('\u06F0' + (it - '0')) else it }.joinToString("")
}
