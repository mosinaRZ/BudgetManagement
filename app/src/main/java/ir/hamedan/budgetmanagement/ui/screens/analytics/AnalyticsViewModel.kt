package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.DateUtils
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

class AnalyticsViewModel(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val selectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val isPersianState = MutableStateFlow(true)
    val isIncomeChartSelectedState = MutableStateFlow(false)

    fun updateLocale(isPersian: Boolean) {
        isPersianState.value = isPersian
    }

    fun setIncomeChartSelected(isSelected: Boolean) {
        isIncomeChartSelectedState.value = isSelected
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        selectedTimeFilter,
        isPersianState,
        isIncomeChartSelectedState
    ) { allTransactions, categories, timeFilter, isPersian, isIncomeChartSelected ->

        val hasAnyTransaction = allTransactions.isNotEmpty()

        val filteredTransactions = filterTransactionsByTime(allTransactions, timeFilter, isPersian)

        val totalIncome = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }.toDouble()
        val expensesList = filteredTransactions.filter { it.type == "EXPENSE" }
        val totalExpense = expensesList.sumOf { it.amount }.toDouble()
        val balance = totalIncome - totalExpense

        val categoryExpenses = expensesList
            .groupBy { it.categoryId }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }.toDouble()
                val percent = if (totalExpense > 0) (sum / totalExpense * 100).toFloat() else 0f
                CategoryExpenseModel(
                    categoryName = categories.firstOrNull { it.id == category }?.title ?: category,
                    totalAmount = sum,
                    percentage = percent,
                    color = generateColorForCategory(categories.firstOrNull { it.id == category }?.title ?: category)
                )
            }
            .sortedByDescending { it.totalAmount }

        val timeExpenses = calculateTimeExpenses(allTransactions, timeFilter, isPersian, isIncomeChartSelected)
        val currentIndex = timeExpenses.indexOfFirst { it.isCurrent }.let { if (it == -1) 0 else it }

        val averageExpense = if (expensesList.isNotEmpty()) totalExpense / expensesList.size else 0.0
        val topExpenseEntities = expensesList
            .filter { it.amount > averageExpense }
            .sortedByDescending { it.amount }
            .take(5)

        val trendChartData = calculateTrendChartData(allTransactions, timeFilter, isPersian)

        AnalyticsUiState(
            isLoading = false,
            hasAnyTransactionInDb = hasAnyTransaction,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance,
            categoryExpenses = categoryExpenses,
            timeExpenses = timeExpenses,
            currentTimeIndex = currentIndex,
            topExpenses = topExpenseEntities,
            averageExpense = averageExpense,
            trendPoints = trendChartData.points,
            trendHasEnoughData = trendChartData.hasEnoughData,
            trendCurrentIndex = trendChartData.currentIndex,
            selectedPeriod = timeFilter.name,
            isIncomeChartSelected = isIncomeChartSelected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun onTimeFilterChanged(filter: TimeFilter) {
        selectedTimeFilter.value = filter
    }

    private fun filterTransactionsByTime(
        transactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean
    ): List<TransactionEntity> {
        if (filter == TimeFilter.ALL) return transactions

        val now = LocalDate.now()
        val (jYearNow, jMonthNow, jDayNow) = DateUtils.toJalali(now)

        return transactions.filter { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

            if (isPersian) {
                val (jYearTx, jMonthTx, jDayTx) = DateUtils.toJalali(txDate)
                when (filter) {
                    TimeFilter.DAILY -> jYearTx == jYearNow && jMonthTx == jMonthNow && jDayTx == jDayNow
                    TimeFilter.WEEKLY -> {
                        if (jYearTx != jYearNow || jMonthTx != jMonthNow) false
                        else {
                            val nowWeekIndex = (jDayNow - 1) / 7
                            val txWeekIndex = (jDayTx - 1) / 7
                            nowWeekIndex == txWeekIndex
                        }
                    }
                    TimeFilter.MONTHLY -> jYearTx == jYearNow && jMonthTx == jMonthNow
                    TimeFilter.ALL -> true
                }
            } else {
                when (filter) {
                    TimeFilter.DAILY -> txDate == now
                    TimeFilter.WEEKLY -> {
                        val weekFields = WeekFields.of(Locale.US)
                        txDate.year == now.year &&
                                txDate.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear())
                    }
                    TimeFilter.MONTHLY -> txDate.year == now.year && txDate.monthValue == now.monthValue
                    TimeFilter.ALL -> true
                }
            }
        }
    }

    private fun calculateTimeExpenses(
        allTransactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean,
        isIncomeChartSelected: Boolean
    ): List<TimeExpenseModel> {
        val targetType = if (isIncomeChartSelected) "INCOME" else "EXPENSE"
        val targetTransactions = allTransactions.filter { it.type == targetType }
        val now = LocalDate.now()

        if (isPersian) {
            val (currentJalaliYear, currentJalaliMonth, currentJalaliDay) = DateUtils.toJalali(now)

            return when (filter) {
                TimeFilter.DAILY -> {
                    val daysInMonth = DateUtils.getDaysInJalaliMonth(currentJalaliYear, currentJalaliMonth)
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }

                    val dailySums = Array(daysInMonth) { 0.0 }
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        if (day in 1..daysInMonth) {
                            dailySums[day - 1] += tx.amount.toDouble()
                        }
                    }

                    (1..daysInMonth).map { day ->
                        TimeExpenseModel(
                            labelFa = day.toString(),
                            labelEn = day.toString(),
                            totalAmount = dailySums[day - 1],
                            isCurrent = (day == currentJalaliDay)
                        )
                    }
                }

                TimeFilter.WEEKLY -> {
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }

                    val weeks = arrayOf(0.0, 0.0, 0.0, 0.0)
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        when {
                            day in 1..7 -> weeks[0] += tx.amount.toDouble()
                            day in 8..14 -> weeks[1] += tx.amount.toDouble()
                            day in 15..21 -> weeks[2] += tx.amount.toDouble()
                            day >= 22 -> weeks[3] += tx.amount.toDouble()
                        }
                    }

                    val currentWeekIndex = (currentJalaliDay - 1) / 7

                    listOf(
                        TimeExpenseModel("هفته ۱", "Week 1", weeks[0], isCurrent = currentWeekIndex == 0),
                        TimeExpenseModel("هفته ۲", "Week 2", weeks[1], isCurrent = currentWeekIndex == 1),
                        TimeExpenseModel("هفته ۳", "Week 3", weeks[2], isCurrent = currentWeekIndex == 2),
                        TimeExpenseModel("هفته ۴", "Week 4", weeks[3], isCurrent = currentWeekIndex == 3)
                    )
                }

                TimeFilter.MONTHLY -> {
                    val currentYearExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, _, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear
                    }

                    val months = Array(12) { 0.0 }
                    currentYearExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, jMonth, _) = DateUtils.toJalali(txDate)
                        if (jMonth in 1..12) {
                            months[jMonth - 1] += tx.amount.toDouble()
                        }
                    }

                    DateUtils.PERSIAN_MONTH_NAMES.indices.map { index ->
                        TimeExpenseModel(
                            labelFa = DateUtils.PERSIAN_MONTH_NAMES[index],
                            labelEn = DateUtils.PERSIAN_MONTH_NAMES[index],
                            totalAmount = months[index],
                            isCurrent = (index == currentJalaliMonth - 1)
                        )
                    }
                }

                TimeFilter.ALL -> {
                    val yearGrouped = targetTransactions.groupBy { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, _, _) = DateUtils.toJalali(txDate)
                        jYear
                    }

                    if (yearGrouped.isEmpty()) {
                        listOf(TimeExpenseModel(currentJalaliYear.toString(), currentJalaliYear.toString(), 0.0, isCurrent = true))
                    } else {
                        yearGrouped.keys.sorted().map { year ->
                            val sum = (yearGrouped[year]?.sumOf { it.amount } ?: 0L).toDouble()
                            TimeExpenseModel(
                                labelFa = year.toString(),
                                labelEn = year.toString(),
                                totalAmount = sum,
                                isCurrent = (year == currentJalaliYear)
                            )
                        }
                    }
                }
            }
        } else {
            val currentGYear = now.year
            val currentGMonth = now.monthValue
            val currentGDay = now.dayOfMonth

            return when (filter) {
                TimeFilter.DAILY -> {
                    val daysInMonth = now.lengthOfMonth()
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }

                    val dailySums = Array(daysInMonth) { 0.0 }
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        if (day in 1..daysInMonth) {
                            dailySums[day - 1] += tx.amount.toDouble()
                        }
                    }

                    (1..daysInMonth).map { day ->
                        TimeExpenseModel(
                            labelFa = day.toString(),
                            labelEn = day.toString(),
                            totalAmount = dailySums[day - 1],
                            isCurrent = (day == currentGDay)
                        )
                    }
                }

                TimeFilter.WEEKLY -> {
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }

                    val weeks = arrayOf(0.0, 0.0, 0.0, 0.0)
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        when {
                            day in 1..7 -> weeks[0] += tx.amount.toDouble()
                            day in 8..14 -> weeks[1] += tx.amount.toDouble()
                            day in 15..21 -> weeks[2] += tx.amount.toDouble()
                            day >= 22 -> weeks[3] += tx.amount.toDouble()
                        }
                    }

                    val currentWeekIndex = (currentGDay - 1) / 7

                    listOf(
                        TimeExpenseModel("Week 1", "Week 1", weeks[0], isCurrent = currentWeekIndex == 0),
                        TimeExpenseModel("Week 2", "Week 2", weeks[1], isCurrent = currentWeekIndex == 1),
                        TimeExpenseModel("Week 3", "Week 3", weeks[2], isCurrent = currentWeekIndex == 2),
                        TimeExpenseModel("Week 4", "Week 4", weeks[3], isCurrent = currentWeekIndex == 3)
                    )
                }

                TimeFilter.MONTHLY -> {
                    val currentYearExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear
                    }

                    val months = Array(12) { 0.0 }
                    currentYearExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val gMonth = txDate.monthValue
                        if (gMonth in 1..12) {
                            months[gMonth - 1] += tx.amount.toDouble()
                        }
                    }

                    DateUtils.ENGLISH_MONTH_NAMES.indices.map { index ->
                        TimeExpenseModel(
                            labelFa = DateUtils.ENGLISH_MONTH_NAMES[index],
                            labelEn = DateUtils.ENGLISH_MONTH_NAMES[index],
                            totalAmount = months[index],
                            isCurrent = (index == currentGMonth - 1)
                        )
                    }
                }

                TimeFilter.ALL -> {
                    val yearGrouped = targetTransactions.groupBy { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year
                    }

                    if (yearGrouped.isEmpty()) {
                        listOf(TimeExpenseModel(currentGYear.toString(), currentGYear.toString(), 0.0, isCurrent = true))
                    } else {
                        yearGrouped.keys.sorted().map { year ->
                            val sum = (yearGrouped[year]?.sumOf { it.amount } ?: 0L).toDouble()
                            TimeExpenseModel(
                                labelFa = year.toString(),
                                labelEn = year.toString(),
                                totalAmount = sum,
                                isCurrent = (year == currentGYear)
                            )
                        }
                    }
                }
            }
        }
    }

    private data class TrendChartData(
        val points: List<Float>,
        val hasEnoughData: Boolean,
        val currentIndex: Int = 0
    )

    private fun countDistinctCalendarMonths(
        transactions: List<TransactionEntity>,
        isPersian: Boolean
    ): Int {
        if (transactions.isEmpty()) return 0
        return transactions.map { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isPersian) {
                val (year, month, _) = DateUtils.toJalali(txDate)
                year * 100 + month
            } else {
                txDate.year * 100 + txDate.monthValue
            }
        }.distinct().count()
    }

    private fun calculateTrendChartData(
        allTransactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean
    ): TrendChartData {
        val distinctMonths = countDistinctCalendarMonths(allTransactions, isPersian)
        val hasEnoughData = when (filter) {
            TimeFilter.DAILY, TimeFilter.WEEKLY -> true
            TimeFilter.MONTHLY -> distinctMonths >= 2
            TimeFilter.ALL -> allTransactions.size >= 2
        }

        if (!hasEnoughData) {
            return TrendChartData(points = emptyList(), hasEnoughData = false, currentIndex = 0)
        }

        val points = calculateTrendPoints(allTransactions, filter, isPersian)
        val currentIndex = calculateTrendCurrentIndex(points.size, filter, isPersian)
        return TrendChartData(points = points, hasEnoughData = true, currentIndex = currentIndex)
    }

    private fun calculateTrendCurrentIndex(
        pointCount: Int,
        filter: TimeFilter,
        isPersian: Boolean
    ): Int {
        if (pointCount <= 0) return 0

        val now = LocalDate.now()
        return when (filter) {
            TimeFilter.DAILY -> pointCount - 1
            TimeFilter.WEEKLY -> {
                val currentDay = if (isPersian) {
                    DateUtils.toJalali(now).third
                } else {
                    now.dayOfMonth
                }
                ((currentDay - 1) / 7).coerceIn(0, pointCount - 1)
            }
            TimeFilter.MONTHLY -> pointCount - 1
            TimeFilter.ALL -> pointCount - 1
        }
    }

    private fun calculateTrendPoints(
        allTransactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean
    ): List<Float> {
        val now = LocalDate.now()
        val netAmount: (TransactionEntity) -> Double = { tx ->
            if (tx.type == "INCOME") {
                tx.amount.toDouble()
            } else {
                -tx.amount.toDouble()
            }
        }

        if (isPersian) {
            val (currentJalaliYear, currentJalaliMonth, currentJalaliDay) = DateUtils.toJalali(now)

            return when (filter) {
                TimeFilter.DAILY -> calculateDailyTrendPoints(
                    allTransactions = allTransactions,
                    isPersian = true,
                    currentDay = currentJalaliDay,
                    netAmount = netAmount
                )

                TimeFilter.WEEKLY -> {
                    val monthTransactions = allTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }
                    val weeklyNet = arrayOf(0.0, 0.0, 0.0, 0.0)
                    monthTransactions.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        val weekIndex = when {
                            day in 1..7 -> 0
                            day in 8..14 -> 1
                            day in 15..21 -> 2
                            else -> 3
                        }
                        weeklyNet[weekIndex] += netAmount(tx)
                    }
                    var cumulative = 0.0
                    weeklyNet.map { weekNet ->
                        cumulative += weekNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.MONTHLY -> {
                    val monthGroups = allTransactions
                        .groupBy { tx ->
                            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                            jYear * 100 + jMonth
                        }
                        .toSortedMap()

                    var cumulative = 0.0
                    monthGroups.values.map { monthTransactions ->
                        val monthNet = monthTransactions.sumOf { netAmount(it) }
                        cumulative += monthNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.ALL -> {
                    if (allTransactions.isEmpty()) return emptyList()
                    val sorted = allTransactions.sortedBy { it.timestamp }
                    var runningBalance = 0.0
                    sorted.map { tx ->
                        runningBalance += netAmount(tx)
                        runningBalance.toFloat()
                    }
                }
            }
        } else {
            val currentGYear = now.year
            val currentGMonth = now.monthValue
            val currentGDay = now.dayOfMonth

            return when (filter) {
                TimeFilter.DAILY -> calculateDailyTrendPoints(
                    allTransactions = allTransactions,
                    isPersian = false,
                    currentDay = currentGDay,
                    netAmount = netAmount
                )

                TimeFilter.WEEKLY -> {
                    val monthTransactions = allTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }
                    val weeklyNet = arrayOf(0.0, 0.0, 0.0, 0.0)
                    monthTransactions.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        val weekIndex = when {
                            day in 1..7 -> 0
                            day in 8..14 -> 1
                            day in 15..21 -> 2
                            else -> 3
                        }
                        weeklyNet[weekIndex] += netAmount(tx)
                    }
                    var cumulative = 0.0
                    weeklyNet.map { weekNet ->
                        cumulative += weekNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.MONTHLY -> {
                    val monthGroups = allTransactions
                        .groupBy { tx ->
                            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            txDate.year * 100 + txDate.monthValue
                        }
                        .toSortedMap()

                    var cumulative = 0.0
                    monthGroups.values.map { monthTransactions ->
                        val monthNet = monthTransactions.sumOf { netAmount(it) }
                        cumulative += monthNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.ALL -> {
                    if (allTransactions.isEmpty()) return emptyList()
                    val sorted = allTransactions.sortedBy { it.timestamp }
                    var runningBalance = 0.0
                    sorted.map { tx ->
                        runningBalance += netAmount(tx)
                        runningBalance.toFloat()
                    }
                }
            }
        }
    }

    private fun calculateDailyTrendPoints(
        allTransactions: List<TransactionEntity>,
        isPersian: Boolean,
        currentDay: Int,
        netAmount: (TransactionEntity) -> Double
    ): List<Float> {
        val now = LocalDate.now()
        val monthTransactions = allTransactions.filter { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isPersian) {
                val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                val (currentJYear, currentJMonth, _) = DateUtils.toJalali(now)
                jYear == currentJYear && jMonth == currentJMonth
            } else {
                txDate.year == now.year && txDate.monthValue == now.monthValue
            }
        }

        if (monthTransactions.isEmpty()) return emptyList()

        fun transactionDay(tx: TransactionEntity): Int {
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            return if (isPersian) {
                DateUtils.toJalali(txDate).third
            } else {
                txDate.dayOfMonth
            }
        }

        val firstDay = monthTransactions.minOf(::transactionDay)
        val dailyNet = mutableMapOf<Int, Double>()

        monthTransactions.forEach { tx ->
            val day = transactionDay(tx)
            if (day in firstDay..currentDay) {
                dailyNet[day] = (dailyNet[day] ?: 0.0) + netAmount(tx)
            }
        }

        var cumulative = 0.0
        return (firstDay..currentDay).map { day ->
            cumulative += dailyNet[day] ?: 0.0
            cumulative.toFloat()
        }
    }
}

private fun generateColorForCategory(categoryName: String): Color {
    val colors = listOf(
        Color(0xFF6750A4), Color(0xFF0288D1), Color(0xFF388E3C),
        Color(0xFFF57C00), Color(0xFFD32F2F), Color(0xFF7B1FA2),
        Color(0xFF00796B), Color(0xFFC2185B), Color(0xFFE64A19), Color(0xFF512DA8)
    )
    if (categoryName.isEmpty()) return colors[0]
    val index = abs(categoryName.hashCode()) % colors.size
    return colors[index]
}