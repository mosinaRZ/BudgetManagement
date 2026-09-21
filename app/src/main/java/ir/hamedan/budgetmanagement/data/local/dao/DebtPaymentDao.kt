package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.DebtPaymentEntity

@Dao
interface DebtPaymentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payments WHERE debtCreditId = :debtCreditId AND operationType = 'BASELINE' LIMIT 1")
    suspend fun getBaseline(debtCreditId: String): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payments WHERE debtCreditId = :debtCreditId ORDER BY createdAt ASC, id ASC")
    suspend fun getByDebtCreditId(debtCreditId: String): List<DebtPaymentEntity>

    @Query("DELETE FROM debt_payments")
    suspend fun clearAll(): Int

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM debt_payments WHERE debtCreditId = :debtCreditId")
    suspend fun deleteByDebtCreditId(debtCreditId: String): Int
}