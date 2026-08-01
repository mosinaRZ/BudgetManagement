package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.UpcomingPaymentDao
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import kotlinx.coroutines.flow.Flow

class UpcomingPaymentRepositoryImpl(
    private val paymentDao: UpcomingPaymentDao
) : UpcomingPaymentRepository {

    override val allPayments: Flow<List<UpcomingPaymentEntity>> = paymentDao.getAllUpcomingPayments()

    override suspend fun insertOrUpdatePayment(payment: UpcomingPaymentEntity) {
        paymentDao.insertPayment(payment)
    }

    override suspend fun togglePaymentStatus(id: String, currentStatus: Boolean) {
        paymentDao.updatePaymentStatus(id, !currentStatus)
    }

    override suspend fun deletePayment(id: String) {
        paymentDao.deletePaymentById(id)
    }
}
