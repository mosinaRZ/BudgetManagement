package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalOperationEntity

@Dao
interface SavingGoalOperationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(operation: SavingGoalOperationEntity): Long

    @Query("SELECT * FROM saving_goal_operations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavingGoalOperationEntity?

    @Query("SELECT * FROM saving_goal_operations WHERE goalId = :goalId AND operationType = 'BASELINE' LIMIT 1")
    suspend fun getBaseline(goalId: String): SavingGoalOperationEntity?

    @Query("SELECT * FROM saving_goal_operations WHERE goalId = :goalId ORDER BY createdAt ASC, id ASC")
    suspend fun getByGoalId(goalId: String): List<SavingGoalOperationEntity>

    @Query("DELETE FROM saving_goal_operations")
    suspend fun clearAll(): Int

    @Query("DELETE FROM saving_goal_operations WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM saving_goal_operations WHERE goalId = :goalId")
    suspend fun deleteByGoalId(goalId: String): Int
}