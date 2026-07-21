package ir.hamedan.budgetmanagement.ui.screens.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeFilter(val titleFa: String, val titleEn: String) {
    DAILY("روزانه", "Daily"),
    WEEKLY("هفتگی", "Weekly"),
    MONTHLY("ماهانه", "Monthly"),
    ALL("کل", "All")
}

enum class TransactionTypeFilter(val titleFa: String, val titleEn: String) {
    ALL("همه", "All"),
    INCOME("واریزی (درآمد)", "Income"),
    EXPENSE("برداشتی (هزینه)", "Expense")
}

enum class SortOrder(val titleFa: String, val titleEn: String) {
    NEWEST("جدیدترین", "Newest"),
    OLDEST("قدیمی‌ترین", "Oldest"),
    HIGHEST_AMOUNT("بیشترین مبلغ", "Highest Amount"),
    LOWEST_AMOUNT("کمترین مبلغ", "Lowest Amount")
}

data class FilterState(
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isCustomFilterActive: Boolean = false
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val initialCurrency: String = "IRT"
) : ViewModel() {

    // وضعیت واحد پول برنامه (IRT یا IRR)
    private val _currencyUnit = MutableStateFlow(initialCurrency)
    val currencyUnit: StateFlow<String> = _currencyUnit.asStateFlow()

    // دریافت دسته‌بندی‌های هزینه‌ای (isExpense = true)
    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getCategoriesByExpenseStatus(isExpense = true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // دریافت دسته‌بندی‌های درآمدی (isExpense = false)
    val incomeCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getCategoriesByExpenseStatus(isExpense = false)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchQuery = MutableStateFlow("")
    val filterState = MutableStateFlow(FilterState())

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactionRepository.getAllTransactions(),
        searchQuery,
        filterState
    ) { transactions, query, filter ->
        var list = transactions.filter { transaction ->
            // ۱. فیلتر نوع تراکنش
            val matchesType = when (filter.typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.INCOME -> transaction.type == "INCOME"
                TransactionTypeFilter.EXPENSE -> transaction.type == "EXPENSE"
            }

            // ۲. فیلتر زمانی
            val matchesTime = if (filter.startDate != null && filter.endDate != null) {
                transaction.timestamp in filter.startDate..filter.endDate
            } else {
                isWithinTimeFilter(transaction.timestamp, filter.timeFilter)
            }

            // ۳. جستجوی متنی روی تراکنش‌های فیلتر شده
            val matchesSearch = query.isBlank() ||
                    transaction.title.contains(query, ignoreCase = true) ||
                    transaction.category.contains(query, ignoreCase = true)

            matchesType && matchesTime && matchesSearch
        }

        // ۴. مرتب‌سازی
        list = when (filter.sortOrder) {
            SortOrder.NEWEST -> list.sortedByDescending { it.timestamp }
            SortOrder.OLDEST -> list.sortedBy { it.timestamp }
            SortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun refreshCurrency(context: Context) {
        _currencyUnit.value = CurrencySharedPreferences.getCurrency(context)
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun setQuickTimeFilter(timeFilter: TimeFilter) {
        filterState.value = filterState.value.copy(
            timeFilter = timeFilter,
            startDate = null,
            endDate = null,
            isCustomFilterActive = false
        )
    }

    fun applyCustomFilter(
        timeFilter: TimeFilter,
        typeFilter: TransactionTypeFilter,
        sortOrder: SortOrder,
        startDate: Long?,
        endDate: Long?
    ) {
        filterState.value = FilterState(
            timeFilter = timeFilter,
            typeFilter = typeFilter,
            sortOrder = sortOrder,
            startDate = startDate,
            endDate = endDate,
            isCustomFilterActive = true
        )
    }

    fun clearFilter() {
        filterState.value = FilterState()
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
        }
    }

    private fun isWithinTimeFilter(timestamp: Long, filter: TimeFilter): Boolean {
        if (filter == TimeFilter.ALL) return true

        val txCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when (filter) {
            TimeFilter.DAILY -> {
                txCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        txCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            }
            TimeFilter.WEEKLY -> {
                txCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        txCalendar.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
            }
            TimeFilter.MONTHLY -> {
                txCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        txCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            }
            TimeFilter.ALL -> true
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(context)
            val currentCurrency = CurrencySharedPreferences.getCurrency(context)
            return TransactionViewModel(
                transactionRepository = TransactionRepository(database.transactionDao()),
                categoryRepository = CategoryRepository(
                    categoryDao = database.categoryDao(),
                    transactionDao = database.transactionDao()
                ),
                initialCurrency = currentCurrency
            ) as T
        }
    }
}