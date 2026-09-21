package ir.hamedan.budgetmanagement.ui.screens.add

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val context: Context
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTransaction(title: String, amount: Double, categoryKey: String, isExpense: Boolean, note: String = "") {
        viewModelScope.launch {
            val categoryId = categoryRepository.getAllCategories().first().firstOrNull { it.id == categoryKey || it.title == categoryKey }?.id
                ?: throw IllegalArgumentException("Category not found: $categoryKey")
            val newTransaction = TransactionEntity(
                title = title,
                amount = MoneyContract.fromInput(amount),
                categoryId = categoryId,
                type = if (isExpense) "EXPENSE" else "INCOME",
                note = note
            )
            transactionRepository.insertTransaction(newTransaction)

            BalanceWidget().updateAll(context)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.TRANSACTION_ADD,
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