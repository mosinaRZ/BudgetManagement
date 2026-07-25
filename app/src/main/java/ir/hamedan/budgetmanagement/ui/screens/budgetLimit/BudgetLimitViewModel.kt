package ir.hamedan.budgetmanagement.ui.screens.budget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
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
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
    private val context: Context
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

            // بعد از محاسبه spent برای هر limit:
            val percentUsed = if (limit.maxLimit > 0) (spent / limit.maxLimit * 100) else 0.0

            listOf(50.0, 80.0, 100.0).forEach { threshold ->
                if (percentUsed >= threshold) {
                    val tag = "BUDGET_${threshold.toInt()}_${limit.categoryName}"
                    viewModelScope.launch {
                        NotificationHelper.send(
                            context = context,
                            type = if (threshold >= 100.0) "ERROR" else "WARNING",
                            titleFa = "هشدار محدودیت بودجه",
                            titleEn = "Budget Limit Alert",
                            descFa = "دسته «${limit.categoryName}» به ${threshold.toInt()}٪ سقف رسید.",
                            descEn = "${limit.categoryName} reached ${threshold.toInt()}% of budget.",
                            tag = tag
                        )
                    }
                }
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

            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "محدودیت مالی جدید ثبت شد",
                titleEn = "New Budget Limit Added",
                descFa = "محدودیت مالی جدید برای دسته بندی «${limit.categoryName}» ثبت شد.",
                descEn = "New budget limit added for category ${limit.categoryName}.",
                tag = "BUDGET_${limit.categoryName}_${System.currentTimeMillis()}"
            )
        }
    }

    fun updateLimitStatus(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            val currentItem = budgetLimitsWithSpent.value.find { it.entity.id == id }?.entity
            currentItem?.let {
                val updatedLimit = it.copy(isActive = isActive)
                budgetLimitRepository.saveLimit(updatedLimit)

                val statusFa = if (isActive) "فعال" else "غیرفعال"
                val statusEn = if (isActive) "enabled" else "disabled"

                NotificationHelper.send(
                    context = context,
                    type = "WARNING",
                    titleFa = "تغییر وضعیت محدودیت بودجه",
                    titleEn = "Budget Limit Status Updated",
                    descFa = "محدودیت مالی دسته‌بندی «${it.categoryName}» $statusFa شد.",
                    descEn = "Budget limit for category ${it.categoryName} was $statusEn.",
                    tag = "BUDGET_STATUS_${it.categoryName}_${System.currentTimeMillis()}"
                )
            }
        }
    }

    fun deleteBudgetLimit(id: Long) {
        viewModelScope.launch {
            val currentItem = budgetLimitsWithSpent.value.find { it.entity.id == id }?.entity
            budgetLimitRepository.deleteLimit(id.toString())

            currentItem?.let {
                NotificationHelper.send(
                    context = context,
                    type = "ERROR",
                    titleFa = "حذف محدودیت بودجه",
                    titleEn = "Budget Limit Deleted",
                    descFa = "محدودیت مالی دسته‌بندی «${it.categoryName}» حذف شد.",
                    descEn = "Budget limit for category ${it.categoryName} was deleted.",
                    tag = "BUDGET_DELETE_${it.categoryName}_${System.currentTimeMillis()}"
                )
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            return BudgetLimitViewModel(
                budgetLimitRepository = BudgetLimitRepository(db.budgetLimitDao()),
                categoryRepository = CategoryRepository(db.categoryDao(), db.transactionDao()),
                transactionRepository = TransactionRepository(db.transactionDao()),
                notificationRepository = NotificationRepository(db.notificationDao()),
                context = context.applicationContext
            ) as T
        }
    }
}