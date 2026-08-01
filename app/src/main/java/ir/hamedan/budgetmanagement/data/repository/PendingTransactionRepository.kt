package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

interface PendingTransactionRepository {
    fun getPendingTransactions(): Flow<List<PendingTransactionEntity>>
    fun getPendingCount(): Flow<Int>
    suspend fun addPending(pending: PendingTransactionEntity): Boolean
    suspend fun confirm(id: String)
    suspend fun ignore(id: String)
    suspend fun delete(id: String)
}
