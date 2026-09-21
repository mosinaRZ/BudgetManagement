package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val syncLocalDataSource: SyncLocalDataSource
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        val updated = transaction.copy(
            createdAt = if (transaction.createdAt > 0L) transaction.createdAt else now,
            updatedAt = now
        )
        syncLocalDataSource.mutate(SyncEntityType.TRANSACTION, updated.id, now) {
            transactionDao.insertTransaction(updated)
        }
    }

    override suspend fun deleteTransactionById(id: String) {
        val now = System.currentTimeMillis()
        syncLocalDataSource.delete(SyncEntityType.TRANSACTION, id, now) {
            transactionDao.deleteTransactionById(id)
        }
    }

    override suspend fun getTransactionCountForCategory(categoryId: String): Int =
        transactionDao.getTransactionCountForCategory(categoryId)

    override suspend fun reassignCategoryForTransactions(oldCategoryId: String, newCategoryId: String) {
        val now = System.currentTimeMillis()
        val affected = transactionDao.getByCategoryId(oldCategoryId)
        syncLocalDataSource.transaction {
            transactionDao.reassignCategoryForTransactions(oldCategoryId, newCategoryId, now)
            affected.forEach { transaction ->
                syncLocalDataSource.recordMutationInTransaction(
                    SyncEntityType.TRANSACTION,
                    transaction.id,
                    now
                )
            }
        }
    }

    override suspend fun getTransactionsBetween(start: Long, end: Long): List<TransactionEntity> {
        require(start <= end) { "Start timestamp cannot be greater than end timestamp." }
        return transactionDao.getTransactionsBetween(start, end)
    }

    override suspend fun getBalanceBefore(beforeDate: Long): Long = transactionDao.getBalanceBefore(beforeDate)
    override suspend fun getCurrentBalance(): Long = transactionDao.getCurrentBalance()
}