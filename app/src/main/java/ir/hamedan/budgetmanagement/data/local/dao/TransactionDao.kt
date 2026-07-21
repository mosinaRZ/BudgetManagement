package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    // شمارش تعداد تراکنش‌های متصل به یک عنوان دسته‌بندی
    @Query("SELECT COUNT(*) FROM transactions WHERE category = :categoryTitle")
    suspend fun getTransactionCountForCategory(categoryTitle: String): Int

    // جابه‌جایی دسته تراکنش‌ها به عنوان دسته‌بندی جدید
    @Query("UPDATE transactions SET category = :newCategoryTitle WHERE category = :oldCategoryTitle")
    suspend fun reassignCategoryForTransactions(oldCategoryTitle: String, newCategoryTitle: String)
}