package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query(
        """
        SELECT *
        FROM pending_transactions
        WHERE status = 'PENDING'
        ORDER BY timestamp DESC
        """
    )
    fun getPendingTransactions(): Flow<List<PendingTransactionEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM pending_transactions
        WHERE status = 'PENDING'
        """
    )
    fun getPendingCount(): Flow<Int>

    @Query(
        """
        SELECT *
        FROM pending_transactions
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): PendingTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pending: PendingTransactionEntity)

    @Query(
        """
        UPDATE pending_transactions
        SET status = :status,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        DELETE FROM pending_transactions
        WHERE id = :id
        """
    )
    suspend fun deleteById(id: String): Int

    /**
     * Prevent duplicate SMS records when Android delivers
     * the same message more than once.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM pending_transactions
        WHERE rawMessage = :rawMessage
          AND timestamp > :sinceTimestamp
        """
    )
    suspend fun countRecentDuplicates(
        rawMessage: String,
        sinceTimestamp: Long
    ): Int

    @Query("DELETE FROM pending_transactions")
    suspend fun clearAll(): Int
}