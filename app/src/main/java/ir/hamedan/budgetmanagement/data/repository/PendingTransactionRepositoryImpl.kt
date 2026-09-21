package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.PendingTransactionDao
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

/** Device-local SMS parsing inbox; raw messages are intentionally never cloud-synchronized. */
class PendingTransactionRepositoryImpl(
    private val pendingTransactionDao: PendingTransactionDao
) : PendingTransactionRepository {
    override fun getPendingTransactions(): Flow<List<PendingTransactionEntity>> = pendingTransactionDao.getPendingTransactions()
    override fun getPendingCount(): Flow<Int> = pendingTransactionDao.getPendingCount()

    override suspend fun insert(pending: PendingTransactionEntity) {
        val now = System.currentTimeMillis()
        pendingTransactionDao.insert(pending.copy(createdAt = if (pending.createdAt > 0L) pending.createdAt else now, updatedAt = now))
    }

    override suspend fun updateStatus(id: String, status: String) {
        pendingTransactionDao.updateStatus(id, status, System.currentTimeMillis())
    }

    override suspend fun deleteById(id: String) {
        pendingTransactionDao.deleteById(id)
    }

    override suspend fun countRecentDuplicates(rawMessage: String, sinceTimestamp: Long): Int =
        pendingTransactionDao.countRecentDuplicates(rawMessage, sinceTimestamp)
}