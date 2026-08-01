package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    override suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun getTransactionCountForCategory(categoryTitle: String): Int {
        return transactionDao.getTransactionCountForCategory(categoryTitle)
    }

    override suspend fun reassignCategoryForTransactions(oldCategoryTitle: String, newCategoryTitle: String) {
        transactionDao.reassignCategoryForTransactions(oldCategoryTitle, newCategoryTitle)
    }

    override suspend fun getTransactionsBetween(start: Long, end: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsBetween(start, end)
    }

    override suspend fun getBalanceBefore(beforeDate: Long): Double {
        return transactionDao.getBalanceBefore(beforeDate)
    }
}
