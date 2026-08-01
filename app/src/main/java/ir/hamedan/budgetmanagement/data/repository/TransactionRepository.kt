package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun deleteTransactionById(id: String)
    suspend fun getTransactionCountForCategory(categoryTitle: String): Int
    suspend fun reassignCategoryForTransactions(oldCategoryTitle: String, newCategoryTitle: String)
    suspend fun getTransactionsBetween(start: Long, end: Long): List<TransactionEntity>
    suspend fun getBalanceBefore(beforeDate: Long): Double
}
