package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity

@Dao
interface DebtCreditDao {

    @Query("SELECT * FROM debt_credits ORDER BY dueDateMillis ASC")
    fun getAllDebtCredits(): kotlinx.coroutines.flow.Flow<List<DebtCreditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(debtCredit: DebtCreditEntity)

    @Update
    suspend fun update(debtCredit: DebtCreditEntity)

    @Query("DELETE FROM debt_credits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE debt_credits SET isSettled = :isSettled, paidAmount = :paidAmount WHERE id = :id")
    suspend fun updateSettledStatus(id: String, isSettled: Boolean, paidAmount: Double = 0.0)

    @Query("SELECT * FROM debt_credits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DebtCreditEntity?
}