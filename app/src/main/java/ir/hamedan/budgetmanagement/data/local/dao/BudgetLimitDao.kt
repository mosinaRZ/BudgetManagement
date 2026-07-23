package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.*
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits")
    fun getAllLimits(): Flow<List<BudgetLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(limit: BudgetLimitEntity)

    @Delete
    suspend fun delete(limit: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE id = :id")
    suspend fun deleteById(id: String)
}