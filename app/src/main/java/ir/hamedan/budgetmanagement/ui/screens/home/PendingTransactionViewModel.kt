package ir.hamedan.budgetmanagement.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PendingTransactionViewModel(
    private val context: Context,
    private val pendingRepository: PendingTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val pendingTransactions: StateFlow<List<PendingTransactionEntity>> =
        pendingRepository.getPendingTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> =
        pendingRepository.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expenseCategories: StateFlow<List<CategoryEntity>> =
        categoryRepository.getCategoriesByExpenseStatus(isExpense = true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> =
        categoryRepository.getCategoriesByExpenseStatus(isExpense = false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun confirmTransaction(
        pending: PendingTransactionEntity,
        title: String,
        amount: Long,
        category: String,
        isExpense: Boolean,
        note: String = ""
    ) {
        viewModelScope.launch {
            val categoryId =
                categoryRepository.getAllCategories()
                    .first()
                    .firstOrNull {
                        it.id == category || it.title == category
                    }
                    ?.id
                    ?: throw IllegalArgumentException("Category not found: $category")

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    type = if (isExpense) "EXPENSE" else "INCOME",
                    note = note,
                    timestamp = pending.timestamp
                )
            )

            pendingRepository.updateStatus(
                id = pending.id,
                status = "CONFIRMED"
            )

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.SMS_CONFIRMED,
                type = "SMS",
                titleFa = "تراکنش ثبت شد",
                titleEn = "Transaction Added",
                descFa = "تراکنش پیامکی «$title» با موفقیت ثبت شد.",
                descEn = "SMS transaction \"$title\" has been added successfully.",
                tag = "SMS_CONFIRMED_${pending.id}"
            )
        }
    }

    fun ignoreTransaction(
        pending: PendingTransactionEntity
    ) {
        viewModelScope.launch {
            pendingRepository.updateStatus(
                id = pending.id,
                status = "IGNORED"
            )
        }
    }
}