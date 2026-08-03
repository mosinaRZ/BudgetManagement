package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.*
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtCreditDao {

    @Query("SELECT * FROM debt_credits ORDER BY dueDateMillis ASC")
    fun getAllDebtCredits(): Flow<List<DebtCreditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(debtCredit: DebtCreditEntity)

    @Update
    suspend fun update(debtCredit: DebtCreditEntity)

    @Query("DELETE FROM debt_credits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE debt_credits SET isSettled = :isSettled WHERE id = :id")
    suspend fun updateSettledStatus(id: String, isSettled: Boolean)
}