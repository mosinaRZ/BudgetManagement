package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtCreditDao {

    @Query(
        """
        SELECT *
        FROM debt_credits
        ORDER BY dueDateMillis ASC
        """
    )
    fun getAllDebtCredits(): Flow<List<DebtCreditEntity>>

    @Query(
        """
        SELECT *
        FROM debt_credits
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): DebtCreditEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(debtCredit: DebtCreditEntity)

    @Update
    suspend fun update(debtCredit: DebtCreditEntity): Int

    @Delete
    suspend fun delete(debtCredit: DebtCreditEntity): Int

    @Query(
        """
        DELETE FROM debt_credits
        WHERE id = :id
        """
    )
    suspend fun deleteById(id: String): Int

    /**
     * Update settlement state and paid amount.
     */
    @Query(
        """
        UPDATE debt_credits
        SET isSettled = :isSettled,
            paidAmount = :paidAmount,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateSettledStatus(
        id: String,
        isSettled: Boolean,
        paidAmount: Long,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM debt_credits")
    suspend fun clearAll(): Int

    @Query("""
        UPDATE debt_credits
        SET paidAmount = :paidAmount,
            isSettled = :isSettled,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun setPaidAggregate(id: String, paidAmount: Long, isSettled: Boolean, updatedAt: Long): Int

    @Query("SELECT * FROM debt_credits")
    suspend fun getAllSnapshot(): List<DebtCreditEntity>
}