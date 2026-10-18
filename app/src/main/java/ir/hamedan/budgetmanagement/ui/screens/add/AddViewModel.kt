package ir.hamedan.budgetmanagement.ui.screens.add

import android.content.Context
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CancellationException
import ir.hamedan.budgetmanagement.utils.viewModelErrorHandler
import ir.hamedan.budgetmanagement.utils.rethrowIfCancelled
import ir.hamedan.budgetmanagement.utils.ErrorMessages
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import ir.hamedan.budgetmanagement.utils.CategoryRules
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

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    /** Last line of defence: an unexpected failure becomes a localized message, never a crash. */
    private val errorHandler = viewModelErrorHandler(context, "AddVM") { _errorMessage.tryEmit(it) }

    private val isPersian: Boolean get() = LocaleHelper.getLanguage(context) == "fa"

    /**
     * Saves the transaction. [onResult] is called on the main thread with `true` only when the
     * transaction really was stored, so the screen never shows a success animation for a failed save.
     */
    fun addTransaction(
        title: String,
        amount: Double,
        categoryKey: String,
        isExpense: Boolean,
        note: String = "",
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(errorHandler) {
            val category = try {
                categoryRepository.getAllCategories().first().firstOrNull { it.id == categoryKey || it.title == categoryKey }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.emit(ErrorMessages.of(e, isPersian))
                onResult(false)
                return@launch
            }
            // دسته‌ی قلک فقط توسط سیستم استفاده می‌شود؛ حتی اگر UI دور زده شود ثبت نمی‌کنیم.
            if (category == null || !CategoryRules.isUserSelectable(category)) {
                _errorMessage.emit(
                    if (isPersian) "دسته‌بندی انتخاب‌شده معتبر نیست؛ دسته‌بندی دیگری انتخاب کنید."
                    else "The selected category is not valid. Please choose another one."
                )
                onResult(false)
                return@launch
            }
            val newTransaction = TransactionEntity(
                title = title,
                amount = MoneyContract.fromInput(amount),
                categoryId = category.id,
                type = if (isExpense) "EXPENSE" else "INCOME",
                note = note
            )
            try {
                transactionRepository.insertTransaction(newTransaction)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.emit(ErrorMessages.of(e, isPersian))
                onResult(false)
                return@launch
            }

            // The transaction is stored at this point: a widget or notification problem must not
            // turn a successful save into a reported failure.
            runCatching { BalanceWidget().updateAll(context) }.onFailure { it.rethrowIfCancelled() }
            runCatching {
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
            }.onFailure { it.rethrowIfCancelled() }
            onResult(true)
        }
    }
}