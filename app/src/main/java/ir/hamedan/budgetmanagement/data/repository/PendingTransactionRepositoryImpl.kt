package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.PendingTransactionDao
import ir.hamedan.budgetmanagement.data.local.models.PendingStatus
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

class PendingTransactionRepositoryImpl(
    private val dao: PendingTransactionDao
) : PendingTransactionRepository {

    override fun getPendingTransactions(): Flow<List<PendingTransactionEntity>> =
        dao.getPendingTransactions()

    override fun getPendingCount(): Flow<Int> = dao.getPendingCount()

    override suspend fun addPending(pending: PendingTransactionEntity): Boolean {
        val duplicateCount = dao.countRecentDuplicates(
            rawMessage = pending.rawMessage,
            sinceTimestamp = pending.timestamp - (2 * 60 * 1000)
        )
        if (duplicateCount > 0) return false
        dao.insert(pending)
        return true
    }

    override suspend fun confirm(id: String) = dao.updateStatus(id, PendingStatus.CONFIRMED)

    override suspend fun ignore(id: String) = dao.updateStatus(id, PendingStatus.IGNORED)

    override suspend fun delete(id: String) = dao.deleteById(id)
}
