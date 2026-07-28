package ir.hamedan.budgetmanagement.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
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
        amount: Double,
        category: String,
        isExpense: Boolean,
        note: String = ""
    ) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    category = category,
                    type = if (isExpense) "EXPENSE" else "INCOME",
                    note = note,
                    timestamp = pending.timestamp // ثبت زمان واقعی پیامک در تراکنش نهایی
                )
            )
            pendingRepository.confirm(pending.id)

            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "تراکنش ثبت شد",
                titleEn = "Transaction Added",
                descFa = "تراکنش پیامکی «$title» با موفقیت ثبت شد.",
                descEn = "SMS transaction \"$title\" has been added successfully.",
                tag = "SMS_CONFIRMED_${pending.id}"
            )
        }
    }

    fun ignoreTransaction(pending: PendingTransactionEntity) {
        viewModelScope.launch { pendingRepository.ignore(pending.id) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            return PendingTransactionViewModel(
                context = context.applicationContext,
                pendingRepository = PendingTransactionRepository(db.pendingTransactionDao()),
                transactionRepository = TransactionRepository(db.transactionDao()),
                categoryRepository = CategoryRepository(db.categoryDao(), db.transactionDao())
            ) as T
        }
    }
}