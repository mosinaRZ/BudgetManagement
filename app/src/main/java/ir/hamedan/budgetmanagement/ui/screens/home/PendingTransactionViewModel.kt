package ir.hamedan.budgetmanagement.ui.screens.home

import android.content.Context
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import ir.hamedan.budgetmanagement.utils.viewModelErrorHandler
import ir.hamedan.budgetmanagement.utils.rethrowIfCancelled
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.utils.CategoryRules
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
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

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    /** Last line of defence: an unexpected failure becomes a localized message, never a crash. */
    private val errorHandler = viewModelErrorHandler(context, "PendingTransactionVM") { _errorMessage.tryEmit(it) }

    private val isPersian: Boolean get() = LocaleHelper.getLanguage(context) == "fa"

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
        viewModelScope.launch(errorHandler) {
            val resolvedCategory =
                categoryRepository.getAllCategories()
                    .first()
                    .firstOrNull {
                        it.id == category || it.title == category
                    }
            // دسته‌ی «قلک» فقط توسط سیستم استفاده می‌شود؛ حتی اگر UI دور زده شود ثبت نمی‌کنیم.
            if (resolvedCategory == null || !CategoryRules.isUserSelectable(resolvedCategory)) {
                _errorMessage.emit(
                    if (isPersian) "دسته‌بندی انتخاب‌شده معتبر نیست؛ دسته‌بندی دیگری انتخاب کنید."
                    else "The selected category is not valid. Please choose another one."
                )
                return@launch
            }
            val categoryId = resolvedCategory.id

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

            runCatching {
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
            }.onFailure { it.rethrowIfCancelled() }
        }
    }

    fun ignoreTransaction(
        pending: PendingTransactionEntity
    ) {
        viewModelScope.launch(errorHandler) {
            pendingRepository.updateStatus(
                id = pending.id,
                status = "IGNORED"
            )
        }
    }
}