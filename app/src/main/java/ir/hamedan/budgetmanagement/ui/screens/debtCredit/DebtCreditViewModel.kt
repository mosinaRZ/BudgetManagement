package ir.hamedan.budgetmanagement.ui.viewmodels

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class DebtCreditViewModel(
    private val debtCreditRepository: DebtCreditRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val debtCreditList: StateFlow<List<DebtCreditEntity>> = debtCreditRepository.allDebtCredits
        .map { list ->
            val now = System.currentTimeMillis()
            list.map { item ->
                // اگر از تاریخ سررسید گذشته باشد و هنوز تسویه نشده باشد، به صورت خودکار تسویه می‌شود
                if (!item.isSettled && item.dueDateMillis in 1 until now) {
                    val autoSettledItem = item.copy(
                        isSettled = true,
                        paidAmount = item.totalAmount
                    )
                    viewModelScope.launch(ioDispatcher) {
                        debtCreditRepository.insertOrUpdateDebtCredit(autoSettledItem)
                    }
                    autoSettledItem
                } else {
                    checkDueDateNotifications(item)
                    item
                }
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

            // ۱. چک کردن موجودی فقط در صورت ایجاد "طلب جدید" و درخواست ثبت در بالانس
            if (!isEdit && type == "CREDIT" && addToBalance) {
                val currentBalance = transactionRepository.getCurrentBalance()
                if (currentBalance < totalAmount) {
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

            // بررسی اینکه آیا تاریخ سررسید منقضی شده است یا خیر
            val now = System.currentTimeMillis()
            val isExpired = calculatedDueDate in 1 until now

            val entity = DebtCreditEntity(
                id = id ?: java.util.UUID.randomUUID().toString(),
                type = type,
                personName = personName,
                totalAmount = totalAmount,
                paidAmount = if (isExpired) totalAmount else (existingEntity?.paidAmount ?: 0.0),
                isMonthly = isMonthly,
                monthlyAmount = if (isMonthly) monthlyAmount else 0.0,
                dueDay = dueDay,
                dueDateMillis = calculatedDueDate,
                note = note,
                isSettled = if (isExpired) true else (existingEntity?.isSettled ?: false)
            )
            debtCreditRepository.insertOrUpdateDebtCredit(entity)

            // ۲. ثبت تراکنش مالی اولیه (فقط اگر جدید باشد و کاربر تایید کرده باشد)
            if (!isEdit && addToBalance) {
                val isDebt = type == "DEBT"

                val txType = if (isDebt) "INCOME" else "EXPENSE"
                val titlePrefix = if (isDebt) "دریافت وام/بدهی" else "پرداخت وام/طلب"

                transactionRepository.insertTransaction(
                    TransactionEntity(
                        title = "$titlePrefix: $personName",
                        amount = totalAmount,
                        category = if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE",   // ← این خط اضافه شد
                        type = txType,
                        note = note ?: "ثبت اولیه $personName",
                        timestamp = System.currentTimeMillis()
                    )
                )
                BalanceWidget().updateAll(context)
            }

            // ۳. ارسال اعلان
            val notifType = if (isEdit) "WARNING" else "SUCCESS"
            val titleFa = if (isEdit) "ویرایش بدهی/طلب" else "ثبت بدهی/طلب جدید"
            val titleEn = if (isEdit) "Record Updated" else "New Record Added"
            val descFa = if (isEdit) "اطلاعات «$personName» ویرایش شد." else "مورد جدید برای «$personName» ثبت گردید."
            val descEn = if (isEdit) "Record for '$personName' updated." else "New record for '$personName' created."

            NotificationHelper.send(
                context = context,
                type = notifType,
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "DEBT_SAVE_${entity.id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun deposit(id: String, amount: Double) {
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

            val newPaid = (item.paidAmount + amount).coerceAtMost(item.totalAmount)
            val isSettled = newPaid >= item.totalAmount

            val updated = item.copy(paidAmount = newPaid, isSettled = isSettled)
            debtCreditRepository.insertOrUpdateDebtCredit(updated)

            val txType = if (isDebt) "EXPENSE" else "INCOME"
            val txTitle = if (isDebt) "پرداخت بدهی به: ${item.personName}" else "دریافت طلب از: ${item.personName}"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = txTitle,
                    amount = amount,
                    category = if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE",
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
                type = "SUCCESS",
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "DEBT_DEPOSIT_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun withdraw(id: String, amount: Double) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            val isDebt = item.type == "DEBT"

            val newPaid = (item.paidAmount - amount).coerceAtLeast(0.0)
            val isSettled = newPaid >= item.totalAmount

            val updated = item.copy(paidAmount = newPaid, isSettled = isSettled)
            debtCreditRepository.insertOrUpdateDebtCredit(updated)

            val txType = if (isDebt) "INCOME" else "EXPENSE"
            val txTitle = if (isDebt) "اصلاح/برداشت از پرداخت بدهی: ${item.personName}" else "اصلاح/برداشت از دریافت طلب: ${item.personName}"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = txTitle,
                    amount = amount,
                    category = if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE",
                    type = txType,
                    note = "اصلاح واریزی بدهی و طلب"
                )
            )
            BalanceWidget().updateAll(context)

            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = "اصلاح واریزی بدهی/طلب",
                titleEn = "Payment Adjustment",
                descFa = "مبلغ $amount از تراکنش‌های «${item.personName}» کسر شد.",
                descEn = "Amount $amount adjusted for '${item.personName}'.",
                tag = "DEBT_WITHDRAW_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id }
            debtCreditRepository.deleteDebtCredit(id)

            val name = item?.personName ?: ""
            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = "حذف بدهی/طلب",
                titleEn = "Record Deleted",
                descFa = "اطلاعات مربوط به «$name» حذف شد.",
                descEn = "Record for '$name' was deleted.",
                tag = "DEBT_DELETE_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun toggleSettled(id: String, currentStatus: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val item = debtCreditList.value.find { it.id == id } ?: return@launch
            val newStatus = !currentStatus
            val newPaid = if (newStatus) item.totalAmount else 0.0

            val updated = item.copy(isSettled = newStatus, paidAmount = newPaid)
            debtCreditRepository.insertOrUpdateDebtCredit(updated)

            val statusFa = if (newStatus) "تسویه شد" else "از حالت تسویه خارج شد"
            val statusEn = if (newStatus) "Settled" else "Unsettled"

            NotificationHelper.send(
                context = context,
                type = if (newStatus) "SUCCESS" else "WARNING",
                titleFa = "تغییر وضعیت تسویه",
                titleEn = "Settlement Status Changed",
                descFa = "وضعیت «${item.personName}» به «$statusFa» تغییر یافت.",
                descEn = "Status of '${item.personName}' changed to $statusEn.",
                tag = "DEBT_SETTLE_${id}_${System.currentTimeMillis()}"
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