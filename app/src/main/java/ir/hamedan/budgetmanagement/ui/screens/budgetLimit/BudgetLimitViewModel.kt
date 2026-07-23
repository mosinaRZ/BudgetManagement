package ir.hamedan.budgetmanagement.ui.screens.budget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BudgetLimitUiModel(
    val entity: BudgetLimitEntity,
    val currentSpent: Double,
    val categoryEmoji: String = "💰"
) {
    // انقضا تنها پس از عبور کامل از تاریخ و زمان پایان
    val isExpired: Boolean
        get() = System.currentTimeMillis() > entity.endDate

    val isActive: Boolean
        get() = entity.isActive && !isExpired

    val isSuccessful: Boolean
        get() = currentSpent <= entity.maxLimit
}

class BudgetLimitViewModel(
    private val budgetLimitRepository: BudgetLimitRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val currencyUnit: StateFlow<String> = CurrencySharedPreferences.currencyFlow

    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryRepository
        .getCategoriesByExpenseStatus(isExpense = true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgetLimitsWithSpent: StateFlow<List<BudgetLimitUiModel>> = combine(
        budgetLimitRepository.getAllLimits(),
        transactionRepository.getAllTransactions(),
        expenseCategories
    ) { limits, transactions, categories ->
        limits.map { limit ->
            // محاسبه هزینه‌ها تنها در صورتی که محدودیت فعال باشد و در بازه زمانی قرار گیرد
            val spent = if (limit.isActive) {
                transactions
                    .filter {
                        it.type == "EXPENSE" &&
                                it.category == limit.categoryName &&
                                it.timestamp in limit.startDate..limit.endDate
                    }
                    .sumOf { it.amount }
            } else {
                0.0
            }

            val emoji = categories.find { it.title == limit.categoryName }?.iconEmoji ?: "💰"

            BudgetLimitUiModel(
                entity = limit,
                currentSpent = spent,
                categoryEmoji = emoji
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveBudgetLimit(
        categoryName: String,
        maxLimit: Double,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            val existingLimit = budgetLimitsWithSpent.value.find { it.entity.categoryName == categoryName }?.entity

            // تنظیم تاریخ پایان تا آخرین میلی‌ثانیه همان روز (23:59:59.999)
            val adjustedEndDate = endDate + (24 * 60 * 60 * 1000L - 1)

            val limit = BudgetLimitEntity(
                id = existingLimit?.id ?: 0L,
                categoryName = categoryName,
                maxLimit = maxLimit,
                isActive = existingLimit?.isActive ?: true,
                startDate = startDate,
                endDate = adjustedEndDate
            )
            budgetLimitRepository.saveLimit(limit)
        }
    }

    fun updateLimitStatus(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            val currentItem = budgetLimitsWithSpent.value.find { it.entity.id == id }?.entity
            currentItem?.let {
                val updatedLimit = it.copy(isActive = isActive)
                budgetLimitRepository.saveLimit(updatedLimit)
            }
        }
    }

    fun deleteBudgetLimit(id: Long) {
        viewModelScope.launch {
            budgetLimitRepository.deleteLimit(id.toString())
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            return BudgetLimitViewModel(
                budgetLimitRepository = BudgetLimitRepository(db.budgetLimitDao()),
                categoryRepository = CategoryRepository(db.categoryDao(), db.transactionDao()),
                transactionRepository = TransactionRepository(db.transactionDao())
            ) as T
        }
    }
}