package ir.hamedan.budgetmanagement.ui.screens.upcomings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import ir.hamedan.budgetmanagement.utils.PaymentDateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpcomingPaymentViewModel(
    private val repository: UpcomingPaymentRepository,
    private val context: Context
) : ViewModel() {

    val payments: StateFlow<List<UpcomingPaymentEntity>> = repository.allPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currencyUnit: StateFlow<String> = CurrencySharedPreferences.currencyFlow

    fun addOrUpdatePayment(payment: UpcomingPaymentEntity) {
        viewModelScope.launch {
            val isEdit = payments.value.any { it.id == payment.id }
            repository.insertOrUpdatePayment(payment)

            val type = if (isEdit) "WARNING" else "SUCCESS"
            val titleFa = if (isEdit) "ویرایش پرداخت آتی" else "ثبت پرداخت آتی جدید"
            val titleEn = if (isEdit) "Upcoming Payment Updated" else "New Upcoming Payment Added"
            val descFa = if (isEdit)
                "پرداخت آتی «${payment.title}» به‌روزرسانی شد."
            else
                "پرداخت آتی «${payment.title}» با مبلغ ${payment.amount} ثبت شد."
            val descEn = if (isEdit)
                "Upcoming payment \"${payment.title}\" was updated."
            else
                "Upcoming payment \"${payment.title}\" added with amount ${payment.amount}."

            NotificationHelper.send(
                context = context,
                type = type,
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "UPCOMING_SAVE_${payment.id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun togglePaymentStatus(payment: UpcomingPaymentEntity) {
        viewModelScope.launch {
            val newStatus = !payment.isPaid
            var newDueDate = payment.dueDate

            // اگر وضعیت به "پرداخت شده" تغییر کرد، تاریخ سررسید ۱ ماه به جلو منتقل می‌شود
            if (newStatus) {
                newDueDate = PaymentDateUtils.getNextMonthDueDate(payment.dueDate, payment.dueDay)
            }

            val updatedPayment = payment.copy(
                isPaid = newStatus,
                dueDate = newDueDate
            )
            repository.insertOrUpdatePayment(updatedPayment)

            val statusFa = if (newStatus) "پرداخت شده" else "پرداخت نشده"
            val statusEn = if (newStatus) "paid" else "unpaid"

            NotificationHelper.send(
                context = context,
                type = if (newStatus) "SUCCESS" else "WARNING",
                titleFa = "تغییر وضعیت پرداخت آتی",
                titleEn = "Upcoming Payment Status Changed",
                descFa = "وضعیت پرداخت «${payment.title}» به «$statusFa» تغییر یافت.",
                descEn = "Status of payment \"${payment.title}\" changed to $statusEn.",
                tag = "UPCOMING_STATUS_${payment.id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            val targetPayment = payments.value.find { it.id.toString() == id }
            repository.deletePayment(id)

            val title = targetPayment?.title ?: "پرداخت آتی"

            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = "حذف پرداخت آتی",
                titleEn = "Upcoming Payment Deleted",
                descFa = "پرداخت «$title» با موفقیت حذف شد.",
                descEn = "Payment \"$title\" was successfully deleted.",
                tag = "UPCOMING_DELETE_${id}_${System.currentTimeMillis()}"
            )
        }
    }
    }
}