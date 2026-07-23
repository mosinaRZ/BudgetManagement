package ir.hamedan.budgetmanagement.ui.screens.payments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository
import ir.hamedan.budgetmanagement.utils.PaymentDateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpcomingPaymentViewModel(
    private val repository: UpcomingPaymentRepository
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
            repository.insertOrUpdatePayment(payment)
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
        }
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            repository.deletePayment(id)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            // ساخت نمونه ریپازیتوری و تزریق آن به ViewModel
            val repo = UpcomingPaymentRepository(db.upcomingPaymentDao())
            return UpcomingPaymentViewModel(repo) as T
        }
    }
}