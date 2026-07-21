package ir.hamedan.budgetmanagement.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    // تبدیل داده‌های دیتابیس به State قابل لمس برای Compose UI
    // نکته: دیگه نیازی به .map { result -> result.list } نیست چون Room مستقیماً Flow<List<T>> برمی‌گردونه
    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTransaction(title: String, amount: Double, category: String, isExpense: Boolean) {
        viewModelScope.launch {
            val newTransaction = TransactionEntity(
                title = title,
                amount = amount,
                category = category,
                type = if (isExpense) "EXPENSE" else "INCOME"
            )
            repository.insertTransaction(newTransaction)
        }
    }

//    fun deleteTransaction(transaction: TransactionEntity) {
//        viewModelScope.launch {
//            repository.deleteTransaction(transaction.id)
//        }
//    }

    // 🎯 Factory جهت جلوگیری از کرش موقع نمونه‌سازی — Room به Context نیاز داره
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(context)
            return HomeViewModel(
                repository = TransactionRepository(database.transactionDao())
            ) as T
        }
    }
}