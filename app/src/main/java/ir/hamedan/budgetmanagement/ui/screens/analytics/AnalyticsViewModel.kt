package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
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
    private val repository: TransactionRepository
) : ViewModel() {

    val selectedTimeFilter = MutableStateFlow(TimeFilter.MONTHLY)
    val isPersianState = MutableStateFlow(true)

    fun updateLocale(isPersian: Boolean) {
        isPersianState.value = isPersian
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllTransactions(),
        selectedTimeFilter,
        isPersianState
    ) { allTransactions, timeFilter, isPersian ->

        val hasAnyTransaction = allTransactions.isNotEmpty()

        val filteredTransactions = filterTransactionsByTime(allTransactions, timeFilter, isPersian)

        val totalIncome = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expensesList = filteredTransactions.filter { it.type == "EXPENSE" }
        val totalExpense = expensesList.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val categoryExpenses = expensesList
            .groupBy { it.category }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }
                val percent = if (totalExpense > 0) (sum / totalExpense * 100).toFloat() else 0f
                CategoryExpenseModel(
                    categoryName = category,
                    totalAmount = sum,
                    percentage = percent,
                    color = generateColorForCategory(category)
                )
            }
            .sortedByDescending { it.totalAmount }

        val timeExpenses = calculateTimeExpenses(allTransactions, timeFilter, isPersian)
        val currentIndex = timeExpenses.indexOfFirst { it.isCurrent }.let { if (it == -1) 0 else it }

        val averageExpense = if (expensesList.isNotEmpty()) totalExpense / expensesList.size else 0.0
        val topExpenseEntities = expensesList
            .filter { it.amount > averageExpense }
            .sortedByDescending { it.amount }
            .take(5)

        val trendPoints = calculateTrendPoints(filteredTransactions)

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
            trendPoints = trendPoints,
            selectedPeriod = timeFilter.name
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
        isPersian: Boolean
    ): List<TimeExpenseModel> {
        val expenses = allTransactions.filter { it.type == "EXPENSE" }
        val now = LocalDate.now()

        if (isPersian) {
            val (currentJalaliYear, currentJalaliMonth, currentJalaliDay) = DateUtils.toJalali(now)

            return when (filter) {
                TimeFilter.DAILY -> {
                    val daysInMonth = DateUtils.getDaysInJalaliMonth(currentJalaliYear, currentJalaliMonth)
                    val currentMonthExpenses = expenses.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }

                    val dailySums = Array(daysInMonth) { 0.0 }
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        if (day in 1..daysInMonth) {
                            dailySums[day - 1] += tx.amount
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
                    val currentMonthExpenses = expenses.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }

                    val weeks = arrayOf(0.0, 0.0, 0.0, 0.0)
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        when {
                            day in 1..7 -> weeks[0] += tx.amount
                            day in 8..14 -> weeks[1] += tx.amount
                            day in 15..21 -> weeks[2] += tx.amount
                            day >= 22 -> weeks[3] += tx.amount
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
                    val currentYearExpenses = expenses.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, _, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear
                    }

                    val months = Array(12) { 0.0 }
                    currentYearExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, jMonth, _) = DateUtils.toJalali(txDate)
                        if (jMonth in 1..12) {
                            months[jMonth - 1] += tx.amount
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
                    val yearGrouped = expenses.groupBy { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, _, _) = DateUtils.toJalali(txDate)
                        jYear
                    }

                    if (yearGrouped.isEmpty()) {
                        listOf(TimeExpenseModel(currentJalaliYear.toString(), currentJalaliYear.toString(), 0.0, isCurrent = true))
                    } else {
                        yearGrouped.keys.sorted().map { year ->
                            val sum = yearGrouped[year]?.sumOf { it.amount } ?: 0.0
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
            // تقویم میلادی برای زبان انگلیسی
            val currentGYear = now.year
            val currentGMonth = now.monthValue
            val currentGDay = now.dayOfMonth

            return when (filter) {
                TimeFilter.DAILY -> {
                    val daysInMonth = now.lengthOfMonth()
                    val currentMonthExpenses = expenses.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }

                    val dailySums = Array(daysInMonth) { 0.0 }
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        if (day in 1..daysInMonth) {
                            dailySums[day - 1] += tx.amount
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
                    val currentMonthExpenses = expenses.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }

                    val weeks = arrayOf(0.0, 0.0, 0.0, 0.0)
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        when {
                            day in 1..7 -> weeks[0] += tx.amount
                            day in 8..14 -> weeks[1] += tx.amount
                            day in 15..21 -> weeks[2] += tx.amount
                            day >= 22 -> weeks[3] += tx.amount
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
                    val currentYearExpenses = expenses.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear
                    }

                    val months = Array(12) { 0.0 }
                    currentYearExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val gMonth = txDate.monthValue
                        if (gMonth in 1..12) {
                            months[gMonth - 1] += tx.amount
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
                    val yearGrouped = expenses.groupBy { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year
                    }

                    if (yearGrouped.isEmpty()) {
                        listOf(TimeExpenseModel(currentGYear.toString(), currentGYear.toString(), 0.0, isCurrent = true))
                    } else {
                        yearGrouped.keys.sorted().map { year ->
                            val sum = yearGrouped[year]?.sumOf { it.amount } ?: 0.0
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

    private fun calculateTrendPoints(transactions: List<TransactionEntity>): List<Float> {
        if (transactions.isEmpty()) return listOf(0f)
        val sorted = transactions.sortedBy { it.timestamp }
        var runningBalance = 0.0
        val points = mutableListOf<Float>()

        sorted.forEach { tx ->
            if (tx.type == "INCOME") runningBalance += tx.amount
            else runningBalance -= tx.amount
            points.add(runningBalance.toFloat())
        }
        return if (points.size < 2) listOf(points.firstOrNull() ?: 0f, points.firstOrNull() ?: 0f) else points
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