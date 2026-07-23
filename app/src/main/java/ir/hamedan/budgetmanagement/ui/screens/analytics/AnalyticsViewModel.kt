package ir.hamedan.budgetmanagement.ui.screens.analytics

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import kotlinx.coroutines.flow.*
import java.util.Calendar
import kotlin.math.abs

class AnalyticsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val selectedTimeFilter = MutableStateFlow(TimeFilter.MONTHLY)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllTransactions(),
        selectedTimeFilter
    ) { allTransactions, timeFilter ->

        val hasAnyTransaction = allTransactions.isNotEmpty()

        // ۱. فیلتر کردن تراکنش‌ها بر اساس زمان انتخابی
        val filteredTransactions = filterTransactionsByTime(allTransactions, timeFilter)

        // ۲. محاسبه مجموع درآمد و هزینه دوره
        val totalIncome = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expensesList = filteredTransactions.filter { it.type == "EXPENSE" }
        val totalExpense = expensesList.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        // ۳. تفکیک هزینه‌ها بر اساس دسته‌بندی برای نمودار دایره‌ای
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

        // ۴. فیلتر سنگین‌ترین هزینه‌ها (فقط هزینه‌هایی که از میانگین خرج‌کرد دوره بیشتر باشند)
        val averageExpense = if (expensesList.isNotEmpty()) totalExpense / expensesList.size else 0.0
        val topExpenseEntities = expensesList
            .filter { it.amount > averageExpense } // فقط هزینه‌های بالاتر از میانگین
            .sortedByDescending { it.amount }
            .take(5)

        // ۵. محاسبه نقاط نمودار روند تغییرات (Balance Trend Points)
        val trendPoints = calculateTrendPoints(filteredTransactions)

        AnalyticsUiState(
            isLoading = false,
            hasAnyTransactionInDb = hasAnyTransaction,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance,
            categoryExpenses = categoryExpenses,
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
        filter: TimeFilter
    ): List<TransactionEntity> {
        if (filter == TimeFilter.ALL) return transactions

        val now = Calendar.getInstance()
        return transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            when (filter) {
                TimeFilter.DAILY -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            txCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
                TimeFilter.WEEKLY -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            txCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                }
                TimeFilter.MONTHLY -> {
                    txCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            txCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                TimeFilter.ALL -> true
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

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(context)
            return AnalyticsViewModel(
                repository = TransactionRepository(database.transactionDao())
            ) as T
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