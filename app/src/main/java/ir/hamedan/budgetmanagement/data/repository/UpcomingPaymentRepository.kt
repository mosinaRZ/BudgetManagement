package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import kotlinx.coroutines.flow.Flow

interface UpcomingPaymentRepository {
    val allPayments: Flow<List<UpcomingPaymentEntity>>
    suspend fun insertOrUpdatePayment(payment: UpcomingPaymentEntity)
    suspend fun togglePaymentStatus(id: String, currentStatus: Boolean)
    suspend fun deletePayment(id: String)
}
