package ir.hamedan.budgetmanagement.ui.viewmodels

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.domain.usecase.DebtCreditUseCase
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DebtCreditViewModel(
    private val debtCreditRepository: DebtCreditRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val useCase: DebtCreditUseCase
) : ViewModel() {

    val debtCreditList: StateFlow<List<DebtCreditEntity>> = debtCreditRepository.getAllDebtCredits()
        .map { list ->
            list.map { item ->
                checkDueDateNotifications(item)
                item
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage

    fun saveOrUpdate(
        id: String? = null, type: String, personName: String, totalAmount: Double,
        isMonthly: Boolean, monthlyAmount: Double, dueDay: Int, oneTimeDueDateMillis: Long,
        note: String?, addToBalance: Boolean = true
    ) {
        viewModelScope.launch(ioDispatcher) {
            val isEdit = id != null
            if (!isEdit && type == "CREDIT" && addToBalance) {
                val currentBalance = transactionRepository.getCurrentBalance()
                if (currentBalance < ir.hamedan.budgetmanagement.data.money.MoneyContract.fromInput(
                        totalAmount
                    )
                ) {
                    val isPersian = LocaleHelper.getLanguage(context) == "fa"
                    _errorMessage.emit(if (isPersian) "موجودی کافی نیست! موجودی فعلی: ${currentBalance.toLong()}" else "Insufficient balance! Current balance: ${currentBalance.toLong()}")
                    return@launch
                }
            }
            runCatching {
                useCase.saveOrUpdate(
                    id,
                    type,
                    personName,
                    totalAmount,
                    isMonthly,
                    monthlyAmount,
                    dueDay,
                    oneTimeDueDateMillis,
                    note,
                    addToBalance
                )
            }
                .onSuccess { entity ->
                    NotificationHelper.send(
                        context,
                        NotificationType.DEBT_ADD,
                        if (isEdit) "WARNING" else "SUCCESS",
                        if (isEdit) "ویرایش بدهی/طلب" else "ثبت بدهی/طلب جدید",
                        if (isEdit) "Record Updated" else "New Record Added",
                        if (isEdit) "اطلاعات «$personName» ویرایش شد." else "مورد جدید برای «$personName» ثبت گردید.",
                        if (isEdit) "Record for '$personName' updated." else "New record for '$personName' created.",
                        "DEBT_SAVE_${entity.id}_${System.currentTimeMillis()}"
                    )
                    if (!isEdit && addToBalance) BalanceWidget().updateAll(context)
                }
                .onFailure { _errorMessage.emit((it as? ir.hamedan.budgetmanagement.data.network.ApiException)?.userMessage(LocaleHelper.getLanguage(context) == "fa") ?: it.message.orEmpty()) }
        }
    }

    fun deposit(id: String, amount: Long) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            if (item.type == "DEBT" && transactionRepository.getCurrentBalance() < amount) {
                val isPersian = LocaleHelper.getLanguage(context) == "fa"
                _errorMessage.emit(if (isPersian) "موجودی حساب برای پرداخت این بدهی کافی نیست!" else "Insufficient balance for debt payment!")
                return@launch
            }
            runCatching { useCase.deposit(item, amount) }
                .onSuccess { BalanceWidget().updateAll(context) }
                .onFailure { _errorMessage.emit((it as? ir.hamedan.budgetmanagement.data.network.ApiException)?.userMessage(LocaleHelper.getLanguage(context) == "fa") ?: it.message.orEmpty()) }
        }
    }

    fun withdraw(id: String, amount: Long) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            runCatching { useCase.withdraw(item, amount) }
                .onSuccess { BalanceWidget().updateAll(context) }
                .onFailure { _errorMessage.emit((it as? ir.hamedan.budgetmanagement.data.network.ApiException)?.userMessage(LocaleHelper.getLanguage(context) == "fa") ?: it.message.orEmpty()) }
        }
    }

    fun softDelete(item: DebtCreditEntity) {
        viewModelScope.launch(ioDispatcher) {
            useCase.delete(item)
        }
    }

    fun restore(item: DebtCreditEntity) {
        viewModelScope.launch(ioDispatcher) {
            useCase.restore(item)
        }
    }

    fun commitDelete(id: String) {
        viewModelScope.launch(ioDispatcher) {
            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_DELETE,
                type = "ERROR",
                titleFa = "حذف بدهی/طلب",
                titleEn = "Record Deleted",
                descFa = "اطلاعات با موفقیت حذف گردید.",
                descEn = "Record was successfully deleted.",
                tag = "DEBT_DELETE_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun toggleSettled(id: String, currentStatus: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            val newStatus = !currentStatus
            useCase.toggleSettled(item, newStatus)

            val statusFa = if (newStatus) "تسویه شد" else "از حالت تسویه خارج شد"
            val statusEn = if (newStatus) "Settled" else "Unsettled"

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_SETTLE,
                type = if (newStatus) "SUCCESS" else "WARNING",
                titleFa = "تغییر وضعیت تسویه",
                titleEn = "Settlement Status Changed",
                descFa = "وضعیت «${item.personName}» به «$statusFa» تغییر یافت.",
                descEn = "Status of '${item.personName}' changed to $statusEn.",
                tag = "DEBT_SETTLE_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun settleDueReminder(id: String) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            val remaining = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)
            if (remaining <= 0L) {
                if (!item.isSettled) {
                    debtCreditRepository.insertOrUpdate(item.copy(isSettled = true))
                }
                return@launch
            }

            val isDebt = item.type == "DEBT"
            val updated = item.copy(paidAmount = item.totalAmount, isSettled = true)
            debtCreditRepository.insertOrUpdate(updated)

            val txType = if (isDebt) "EXPENSE" else "INCOME"
            val txTitle =
                if (isDebt) "پرداخت بدهی به: ${item.personName}" else "دریافت طلب از: ${item.personName}"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = txTitle,
                    amount = remaining,
                    categoryId = categoryRepository.getAllCategories().first()
                        .firstOrNull { it.title == if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }?.id
                        ?: throw IllegalStateException("Required debt/credit category is missing."),
                    type = txType,
                    note = "تسویه از طریق یادآوری سررسید"
                )
            )
            BalanceWidget().updateAll(context)

            val titleFa = if (isDebt) "پرداخت بدهی" else "دریافت طلب"
            val titleEn = if (isDebt) "Debt Payment" else "Credit Received"
            val descFa = "«${item.personName}» با موفقیت تسویه شد."
            val descEn = "'${item.personName}' was fully settled."

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_DUE_SETTLE,
                type = "SUCCESS",
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "DEBT_DUE_SETTLE_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun updateDueDate(id: String, newDueDateMillis: Long) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            useCase.updateDueDate(item, newDueDateMillis)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_DUE_DATE_CHANGE,
                type = "WARNING",
                titleFa = "تغییر تاریخ سررسید",
                titleEn = "Due Date Changed",
                descFa = "تاریخ سررسید «${item.personName}» تغییر یافت.",
                descEn = "Due date for '${item.personName}' was changed.",
                tag = "DEBT_DUE_CHANGE_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    private fun checkDueDateNotifications(item: DebtCreditEntity) {
        if (item.isSettled || item.dueDateMillis <= 0) return

        val now = System.currentTimeMillis()
        val diffMillis = item.dueDateMillis - now
        val daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

        if (daysLeft in listOf(1, 3, 7)) {
            val tag = "DEBT_DUE_REMINDER_${daysLeft}_${item.id}"
            val isDebt = item.type == "DEBT"
            val titleFa = if (isDebt) "یادآوری موعد بدهی ⚠️" else "یادآوری موعد طلب 🪙"
            val titleEn = if (isDebt) "Debt Due Reminder" else "Credit Due Reminder"

            val descFa = if (isDebt) {
                "تنها $daysLeft روز تا سررسید بدهی به «${item.personName}» باقی مانده است."
            } else {
                "تنها $daysLeft روز تا موعد دریافت طلب از «${item.personName}» باقی مانده است."
            }

            val descEn = "Only $daysLeft days left until due date for '${item.personName}'."

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_DUE_REMINDER,
                type = "WARNING",
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = tag
            )
        }
    }

    fun calculateExpirationDate(
        totalAmount: Long,
        monthlyAmount: Long,
        dueDay: Int
    ): Long {
        if (totalAmount <= 0L || monthlyAmount <= 0L) {
            return System.currentTimeMillis()
        }

        val monthsRequired =
            ((totalAmount + monthlyAmount - 1L) / monthlyAmount).coerceAtLeast(1L)

        val calendar = Calendar.getInstance()

        // اولین سررسید
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        if (currentDay > dueDay) {
            calendar.add(Calendar.MONTH, 1)
        }

        // تعداد ماه‌های باقی‌مانده تا آخرین قسط
        calendar.add(Calendar.MONTH, monthsRequired.toInt() - 1)

        // حالا که ماه مقصد مشخص است، آخرین روز مجاز همان ماه را محاسبه می‌کنیم
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendar.set(
            Calendar.DAY_OF_MONTH,
            dueDay.coerceIn(1, lastDayOfMonth)
        )

        return calendar.timeInMillis
    }
}