package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.PendingTransactionDao
import ir.hamedan.budgetmanagement.data.local.models.PendingStatus
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

class PendingTransactionRepository(private val dao: PendingTransactionDao) {

    fun getPendingTransactions(): Flow<List<PendingTransactionEntity>> = dao.getPendingTransactions()

    fun getPendingCount(): Flow<Int> = dao.getPendingCount()

    // درج تراکنش پیامکی جدید با جلوگیری از تکراری‌ها در بازه ۲ دقیقه قبل از دریافت پیامک
    suspend fun addPending(pending: PendingTransactionEntity): Boolean {
        val duplicateCount = dao.countRecentDuplicates(
            rawMessage = pending.rawMessage,
            sinceTimestamp = pending.timestamp - (2 * 60 * 1000)
        )
        if (duplicateCount > 0) return false

        dao.insert(pending)
        return true
    }

    suspend fun confirm(id: String) = dao.updateStatus(id, PendingStatus.CONFIRMED)

    suspend fun ignore(id: String) = dao.updateStatus(id, PendingStatus.IGNORED)

    suspend fun delete(id: String) = dao.deleteById(id)
}