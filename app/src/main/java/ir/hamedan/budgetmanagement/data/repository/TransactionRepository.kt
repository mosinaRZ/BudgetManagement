package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    // دریافت لحظه‌ای لیست تمام تراکنش‌ها به صورت Flow (واکنش‌گرا)
    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    // افزودن یا به‌روزرسانی تراکنش
    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    // حذف یک تراکنش بر اساس ID (هماهنگ شده با TransactionViewModel)
    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    // شمارش تراکنش‌های یک دسته‌بندی خاص
    suspend fun getTransactionCountForCategory(categoryTitle: String): Int {
        return transactionDao.getTransactionCountForCategory(categoryTitle)
    }

    // انتقال تراکنش‌ها از یک دسته‌بندی به دسته‌بندی جدید
    suspend fun reassignCategoryForTransactions(oldCategoryTitle: String, newCategoryTitle: String) {
        transactionDao.reassignCategoryForTransactions(oldCategoryTitle, newCategoryTitle)
    }
}