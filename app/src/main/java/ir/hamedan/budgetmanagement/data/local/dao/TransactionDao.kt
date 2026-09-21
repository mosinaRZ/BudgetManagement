package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT *
        FROM transactions
        ORDER BY timestamp DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT *
        FROM transactions
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(
        transaction: TransactionEntity
    )

    @Query("""
        DELETE FROM transactions
        WHERE id = :id
    """)
    suspend fun deleteTransactionById(
        id: String
    )

    @Query("""
        SELECT *
        FROM transactions
        WHERE categoryId = :categoryId
    """)
    suspend fun getByCategoryId(categoryId: String): List<TransactionEntity>

    @Query("""
        SELECT COUNT(*)
        FROM transactions
        WHERE categoryId = :categoryId
    """)
    suspend fun getTransactionCountForCategory(
        categoryId: String
    ): Int

    @Query("""
        UPDATE transactions
        SET categoryId = :newCategoryId,
            updatedAt = :updatedAt
        WHERE categoryId = :oldCategoryId
    """)
    suspend fun reassignCategoryForTransactions(
        oldCategoryId: String,
        newCategoryId: String,
        updatedAt: Long
    )

    @Query("""
        SELECT *
        FROM transactions
        WHERE timestamp BETWEEN :start AND :end
        ORDER BY timestamp ASC
    """)
    suspend fun getTransactionsBetween(
        start: Long,
        end: Long
    ): List<TransactionEntity>

    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN type = 'INCOME' THEN amount
                    ELSE -amount
                END
            ),
            0
        )
        FROM transactions
        WHERE timestamp < :beforeDate
    """)
    suspend fun getBalanceBefore(
        beforeDate: Long
    ): Long

    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN type = 'INCOME' THEN amount
                    ELSE -amount
                END
            ),
            0
        )
        FROM transactions
    """)
    suspend fun getCurrentBalance(): Long

    @Query("DELETE FROM transactions")
    suspend fun clearAll(): Int
}