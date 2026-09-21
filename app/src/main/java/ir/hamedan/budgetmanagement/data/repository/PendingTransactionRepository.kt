package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

interface PendingTransactionRepository {

    fun getPendingTransactions(): Flow<List<PendingTransactionEntity>>

    fun getPendingCount(): Flow<Int>

    suspend fun insert(
        pending: PendingTransactionEntity
    )

    suspend fun updateStatus(
        id: String,
        status: String
    )

    suspend fun deleteById(
        id: String
    )

    suspend fun countRecentDuplicates(
        rawMessage: String,
        sinceTimestamp: Long
    ): Int
}