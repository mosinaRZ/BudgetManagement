package ir.hamedan.budgetmanagement.ui.screens.add

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val context: Context
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCategories = categoryRepository.getAllCategories().first()
            val defaultCategories = listOf(
                CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true),
                CategoryEntity(title = "TRANSPORT", iconEmoji = "🚗", isExpense = true),
                CategoryEntity(title = "SHOPPING", iconEmoji = "🛍️", isExpense = true),
                CategoryEntity(title = "BILL", iconEmoji = "📄", isExpense = true),
                CategoryEntity(title = "DEBT_CREDIT_PAYABLE", iconEmoji = "💸", isExpense = true, isSystem = true), // بدهی
                CategoryEntity(title = "SALARY", iconEmoji = "💰", isExpense = false),
                CategoryEntity(title = "INVESTMENT", iconEmoji = "📈", isExpense = false),
                CategoryEntity(title = "DEBT_CREDIT_RECEIVABLE", iconEmoji = "📥", isExpense = false, isSystem = true) // طلب
            )

            defaultCategories.forEach { category ->
                if (currentCategories.none { it.title == category.title }) {
                    categoryRepository.insertCategory(category)
                }
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

            BalanceWidget().updateAll(context)

            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "تراکنش ثبت شد",
                titleEn = "Transaction Added",
                descFa = "تراکنش «${newTransaction.title}» با موفقیت ثبت شد.",
                descEn = "The transaction «${newTransaction.title}» has been added successfully.",
                tag = "TRANSACTION_ADDED_${newTransaction.id}"
            )
        }
    }
}