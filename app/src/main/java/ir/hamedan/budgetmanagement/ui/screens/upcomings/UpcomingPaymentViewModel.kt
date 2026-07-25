package ir.hamedan.budgetmanagement.ui.screens.payments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository
import ir.hamedan.budgetmanagement.utils.AppNotificationManager
import ir.hamedan.budgetmanagement.utils.PaymentDateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class UpcomingPaymentViewModel(
    private val context: Context,
    private val repository: UpcomingPaymentRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val payments: StateFlow<List<UpcomingPaymentEntity>> = repository.allPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currencyUnit: StateFlow<String> = CurrencySharedPreferences.currencyFlow

    init {
        checkUpcomingDueDates()
    }

    fun addOrUpdatePayment(payment: UpcomingPaymentEntity) {
        viewModelScope.launch {
            repository.insertOrUpdatePayment(payment)

            val titleFa = "موعد سررسید"
            val titleEn = "Upcoming Payment"
            val descFa = "اطلاعات موعد سررسید «${payment.title}» ثبت/ویرایش شد."
            val descEn = "Upcoming payment \"${payment.title}\" was saved."

            notificationRepository.addNotification(
                NotificationEntity(
                    type = "INFO",
                    titleFa = titleFa,
                    titleEn = titleEn,
                    descFa = descFa,
                    descEn = descEn,
                    tag = "DUE_SAVE_${payment.id}"
                )
            )
            AppNotificationManager.sendPushIfAllowed(context, titleFa, titleEn, descFa, descEn)
            checkUpcomingDueDates()
        }
    }

    fun togglePaymentStatus(payment: UpcomingPaymentEntity) {
        viewModelScope.launch {
            val newStatus = !payment.isPaid
            var newDueDate = payment.dueDate

            if (newStatus) {
                newDueDate = PaymentDateUtils.getNextMonthDueDate(payment.dueDate, payment.dueDay)
            }

            val updatedPayment = payment.copy(
                isPaid = newStatus,
                dueDate = newDueDate
            )
            repository.insertOrUpdatePayment(updatedPayment)
        }
    }

    fun deletePayment(payment: UpcomingPaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment.id)

            val titleFa = "حذف موعد سررسید"
            val titleEn = "Upcoming Payment Deleted"
            val descFa = "موعد سررسید «${payment.title}» حذف شد."
            val descEn = "Upcoming payment \"${payment.title}\" was deleted."

            notificationRepository.addNotification(
                NotificationEntity(
                    type = "WARNING",
                    titleFa = titleFa,
                    titleEn = titleEn,
                    descFa = descFa,
                    descEn = descEn,
                    tag = "DUE_DEL_${payment.id}"
                )
            )
            AppNotificationManager.sendPushIfAllowed(context, titleFa, titleEn, descFa, descEn)
        }
    }

    private fun checkUpcomingDueDates() {
        viewModelScope.launch {
            val list = payments.value
            val now = System.currentTimeMillis()

            list.filter { !it.isPaid }.forEach { payment ->
                val diffMillis = payment.dueDate - now
                val daysLeft = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

                if (daysLeft in listOf(1, 3, 7)) {
                    val tag = "DUE_ALERT_${payment.id}_DAYS_$daysLeft"
                    val titleFa = "یادآوری موعد سررسید"
                    val titleEn = "Payment Reminder"
                    val descFa = "تنها $daysLeft روز تا موعد سررسید «${payment.title}» باقی مانده است."
                    val descEn = "Only $daysLeft days left until \"${payment.title}\" payment due."

                    notificationRepository.addNotification(
                        NotificationEntity(
                            type = "WARNING",
                            titleFa = titleFa,
                            titleEn = titleEn,
                            descFa = descFa,
                            descEn = descEn,
                            tag = tag
                        )
                    )
                    AppNotificationManager.sendPushIfAllowed(context, titleFa, titleEn, descFa, descEn)
                }
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            return UpcomingPaymentViewModel(
                context = context,
                repository = UpcomingPaymentRepository(db.upcomingPaymentDao()),
                notificationRepository = NotificationRepository(db.notificationDao())
            ) as T
        }
    }
}