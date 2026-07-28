package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.*
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingTransactions(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pending: PendingTransactionEntity)

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    // جلوگیری از ثبت تکراری، وقتی سیستم عامل یک پیامک را دوبار تحویل می‌دهد
    @Query("SELECT COUNT(*) FROM pending_transactions WHERE rawMessage = :rawMessage AND timestamp > :sinceTimestamp")
    suspend fun countRecentDuplicates(rawMessage: String, sinceTimestamp: Long): Int
}