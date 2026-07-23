package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingGoalDao {

    @Query("SELECT * FROM saving_goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<SavingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingGoalEntity)

    @Query("UPDATE saving_goals SET currentAmount = currentAmount + :amount WHERE id = :goalId")
    suspend fun depositToGoal(goalId: String, amount: Double)

    @Query("UPDATE saving_goals SET currentAmount = CASE WHEN (currentAmount - :amount) < 0 THEN 0.0 ELSE (currentAmount - :amount) END WHERE id = :goalId")
    suspend fun withdrawFromGoal(goalId: String, amount: Double)
}