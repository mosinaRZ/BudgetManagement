package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.UpcomingPaymentDao
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import kotlinx.coroutines.flow.Flow

class UpcomingPaymentRepository(
    private val paymentDao: UpcomingPaymentDao
) {
    val allPayments: Flow<List<UpcomingPaymentEntity>> = paymentDao.getAllUpcomingPayments()

    suspend fun insertOrUpdatePayment(payment: UpcomingPaymentEntity) {
        paymentDao.insertPayment(payment)
    }

    suspend fun togglePaymentStatus(id: String, currentStatus: Boolean) {
        paymentDao.updatePaymentStatus(id, !currentStatus)
    }

    suspend fun deletePayment(id: String) {
        paymentDao.deletePaymentById(id)
    }
}