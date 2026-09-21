package ir.hamedan.budgetmanagement.ui.viewmodels

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
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
import kotlin.math.ceil

class DebtCreditViewModel(
    private val debtCreditRepository: DebtCreditRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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
        id: String? = null,
        type: String, // "DEBT" یا "CREDIT"
        personName: String,
        totalAmount: Double,
        isMonthly: Boolean,
        monthlyAmount: Double,
        dueDay: Int,
        oneTimeDueDateMillis: Long,
        note: String?,
        addToBalance: Boolean = true
    ) {
        viewModelScope.launch(ioDispatcher) {
            val isEdit = id != null
            val calculatedDueDate = if (isMonthly && monthlyAmount > 0) {
                calculateExpirationDate(totalAmount, monthlyAmount, dueDay)
            } else {
                oneTimeDueDateMillis
            }

            if (!isEdit && type == "CREDIT" && addToBalance) {
                val currentBalance = transactionRepository.getCurrentBalance()
                if (currentBalance < MoneyContract.fromInput(totalAmount)) {
                    val isPersian = LocaleHelper.getLanguage(context) == "fa"
                    val msg = if (isPersian) {
                        "موجودی کافی نیست! موجودی فعلی: ${currentBalance.toLong()}"
                    } else {
                        "Insufficient balance! Current balance: ${currentBalance.toLong()}"
                    }
                    _errorMessage.emit(msg)
                    return@launch
                }
            }

            val existingEntity = if (isEdit) debtCreditList.value.find { it.id == id } else null

            val categoryId = categoryRepository.getAllCategories().first().firstOrNull { it.title == if (type == "DEBT") "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }?.id
                ?: throw IllegalStateException("Required debt/credit category is missing.")

            val entity = DebtCreditEntity(
                id = id ?: java.util.UUID.randomUUID().toString(),
                type = type,
                personName = personName,
                totalAmount = MoneyContract.fromInput(totalAmount),
                paidAmount = existingEntity?.paidAmount ?: 0L,
                isMonthly = isMonthly,
                monthlyAmount = if (isMonthly) MoneyContract.fromInput(monthlyAmount) else 0L,
                dueDay = dueDay,
                dueDateMillis = calculatedDueDate,
                note = note,
                isSettled = existingEntity?.isSettled ?: false
            )
            debtCreditRepository.insertOrUpdate(entity)

            if (!isEdit && addToBalance) {
                val isDebt = type == "DEBT"

                val txType = if (isDebt) "INCOME" else "EXPENSE"
                val titlePrefix = if (isDebt) "دریافت وام/بدهی" else "پرداخت وام/طلب"

                transactionRepository.insertTransaction(
                    TransactionEntity(
                        title = "$titlePrefix: $personName",
                        amount = MoneyContract.fromInput(totalAmount),
                        categoryId = categoryId,
                        type = txType,
                        note = note ?: "ثبت اولیه $personName",
                        timestamp = System.currentTimeMillis()
                    )
                )
                BalanceWidget().updateAll(context)
            }

            val notifType = if (isEdit) "WARNING" else "SUCCESS"
            val titleFa = if (isEdit) "ویرایش بدهی/طلب" else "ثبت بدهی/طلب جدید"
            val titleEn = if (isEdit) "Record Updated" else "New Record Added"
            val descFa = if (isEdit) "اطلاعات «$personName» ویرایش شد." else "مورد جدید برای «$personName» ثبت گردید."
            val descEn = if (isEdit) "Record for '$personName' updated." else "New record for '$personName' created."

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_ADD,
                type = notifType,
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "DEBT_SAVE_${entity.id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun deposit(id: String, amount: Long) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            val isDebt = item.type == "DEBT"

            if (isDebt) {
                val currentBalance = transactionRepository.getCurrentBalance()
                if (currentBalance < amount) {
                    val isPersian = LocaleHelper.getLanguage(context) == "fa"
                    val msg = if (isPersian) {
                        "موجودی حساب برای پرداخت این بدهی کافی نیست! بالانس فعلی: ${currentBalance.toLong()}"
                    } else {
                        "Insufficient balance for debt payment! Current balance: ${currentBalance.toLong()}"
                    }
                    _errorMessage.emit(msg)
                    return@launch
                }
            }

            val newPaid = (item.paidAmount + amount)
                .coerceAtMost(item.totalAmount)
            val isSettled = newPaid >= item.totalAmount

            val updated = item.copy(paidAmount = newPaid, isSettled = isSettled)
            debtCreditRepository.insertOrUpdate(updated)

            val txType = if (isDebt) "EXPENSE" else "INCOME"
            val txTitle = if (isDebt) "پرداخت بدهی به: ${item.personName}" else "دریافت طلب از: ${item.personName}"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = txTitle,
                    amount = amount,
                    categoryId = categoryRepository.getAllCategories().first().firstOrNull { it.title == if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }?.id
                        ?: throw IllegalStateException("Required debt/credit category is missing."),
                    type = txType,
                    note = "ثبت واریزی/پرداختی بدهی و طلب"
                )
            )
            BalanceWidget().updateAll(context)

            val titleFa = if (isDebt) "پرداخت بدهی" else "دریافت طلب"
            val titleEn = if (isDebt) "Debt Payment" else "Credit Received"
            val descFa = "مبلغ $amount به حساب «${item.personName}» ثبت شد."
            val descEn = "Amount $amount registered for '${item.personName}'."

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_PAYMENT,
                type = "SUCCESS",
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "DEBT_DEPOSIT_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun withdraw(id: String, amount: Long) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            val isDebt = item.type == "DEBT"

            val newPaid =
                (item.paidAmount - amount)
                    .coerceAtLeast(0L)
            val isSettled = newPaid >= item.totalAmount

            val updated = item.copy(paidAmount = newPaid, isSettled = isSettled)
            debtCreditRepository.insertOrUpdate(updated)

            val txType = if (isDebt) "INCOME" else "EXPENSE"
            val txTitle = if (isDebt) "اصلاح/برداشت از پرداخت بدهی: ${item.personName}" else "اصلاح/برداشت از دریافت طلب: ${item.personName}"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = txTitle,
                    amount = amount,
                    categoryId = categoryRepository.getAllCategories().first().firstOrNull { it.title == if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }?.id
                        ?: throw IllegalStateException("Required debt/credit category is missing."),
                    type = txType,
                    note = "اصلاح واریزی بدهی و طلب"
                )
            )
            BalanceWidget().updateAll(context)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEBT_ADJUSTMENT,
                type = "WARNING",
                titleFa = "اصلاح واریزی بدهی/طلب",
                titleEn = "Payment Adjustment",
                descFa = "مبلغ $amount از تراکنش‌های «${item.personName}» کسر شد.",
                descEn = "Amount $amount adjusted for '${item.personName}'.",
                tag = "DEBT_WITHDRAW_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun softDelete(item: DebtCreditEntity) {
        viewModelScope.launch(ioDispatcher) {
            debtCreditRepository.deleteById(item.id)
        }
    }

    fun restore(item: DebtCreditEntity) {
        viewModelScope.launch(ioDispatcher) {
            debtCreditRepository.insertOrUpdate(item)
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
            val newPaid = if (newStatus) item.totalAmount else 0L

            val updated = item.copy(isSettled = newStatus, paidAmount = newPaid)
            debtCreditRepository.insertOrUpdate(updated)

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
            val txTitle = if (isDebt) "پرداخت بدهی به: ${item.personName}" else "دریافت طلب از: ${item.personName}"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = txTitle,
                    amount = remaining,
                    categoryId = categoryRepository.getAllCategories().first().firstOrNull { it.title == if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }?.id
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
            val updated = item.copy(dueDateMillis = newDueDateMillis)
            debtCreditRepository.insertOrUpdate(updated)

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

    fun calculateExpirationDate(totalAmount: Double, monthlyAmount: Double, dueDay: Int): Long {
        if (monthlyAmount <= 0) return System.currentTimeMillis()
        val monthsRequired = ceil(totalAmount / monthlyAmount).toInt()
        val calendar = Calendar.getInstance().apply {
            val currentDay = get(Calendar.DAY_OF_MONTH)
            if (currentDay > dueDay) {
                add(Calendar.MONTH, 1)
            }
            set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            add(Calendar.MONTH, monthsRequired - 1)
        }
        return calendar.timeInMillis
    }
}