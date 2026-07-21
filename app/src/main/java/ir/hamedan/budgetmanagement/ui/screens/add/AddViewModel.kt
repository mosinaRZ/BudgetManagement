package ir.hamedan.budgetmanagement.ui.screens.add

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    init {
        // ایجاد دسته‌بندی‌های پیش‌فرض در صورت خالی بودن دیتابیس
        viewModelScope.launch {
            val currentCategories = categoryRepository.getAllCategories().first()
            if (currentCategories.isEmpty()) {
                val defaults = listOf(
                    CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true),
                    CategoryEntity(title = "TRANSPORT", iconEmoji = "🚗", isExpense = true),
                    CategoryEntity(title = "SHOPPING", iconEmoji = "🛍️", isExpense = true),
                    CategoryEntity(title = "BILL", iconEmoji = "📄", isExpense = true),
                    CategoryEntity(title = "SALARY", iconEmoji = "💰", isExpense = false),
                    CategoryEntity(title = "INVESTMENT", iconEmoji = "📈", isExpense = false)
                )
                defaults.forEach { categoryRepository.insertCategory(it) }
            }
        }
    }

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTransaction(title: String, amount: Double, categoryKey: String, isExpense: Boolean, note: String = "") {
        viewModelScope.launch {
            val newTransaction = TransactionEntity(
                title = title,
                amount = amount,
                category = categoryKey,
                type = if (isExpense) "EXPENSE" else "INCOME",
                note = note
            )
            transactionRepository.insertTransaction(newTransaction)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(context)
            return AddViewModel(
                transactionRepository = TransactionRepository(database.transactionDao()),
                categoryRepository = CategoryRepository(
                    categoryDao = database.categoryDao(),
                    transactionDao = database.transactionDao()
                )
            ) as T
        }
    }
}