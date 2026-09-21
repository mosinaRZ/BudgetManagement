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

    @Query(
        """
        SELECT *
        FROM saving_goals
        ORDER BY createdAt DESC
        """
    )
    fun getAllGoals(): Flow<List<SavingGoalEntity>>

    @Query(
        """
        SELECT *
        FROM saving_goals
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): SavingGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingGoalEntity): Int

    @Delete
    suspend fun deleteGoal(goal: SavingGoalEntity): Int

    /**
     * Add money to a saving goal.
     */
    @Query(
        """
        UPDATE saving_goals
        SET currentAmount = currentAmount + :amount,
            updatedAt = :updatedAt
        WHERE id = :goalId
        """
    )
    suspend fun depositToGoal(
        goalId: String,
        amount: Long,
        updatedAt: Long
    ): Int

    /**
     * Withdraw money from a saving goal.
     *
     * Current amount will never become negative.
     */
    @Query(
        """
        UPDATE saving_goals
        SET currentAmount =
            CASE
                WHEN currentAmount - :amount < 0
                    THEN 0
                ELSE currentAmount - :amount
            END,
            updatedAt = :updatedAt
        WHERE id = :goalId
        """
    )
    suspend fun withdrawFromGoal(
        goalId: String,
        amount: Long,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE saving_goals
        SET lastAutoDepositTimestamp = :timestamp,
            updatedAt = :updatedAt
        WHERE id = :goalId
        """
    )
    suspend fun updateLastAutoDepositTimestamp(
        goalId: String,
        timestamp: Long,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM saving_goals")
    suspend fun clearAll(): Int

    @Query("""
        UPDATE saving_goals
        SET currentAmount = :amount, updatedAt = :updatedAt
        WHERE id = :goalId
    """)
    suspend fun setCurrentAmount(goalId: String, amount: Long, updatedAt: Long): Int

    @Query("SELECT * FROM saving_goals")
    suspend fun getAllGoalsSnapshot(): List<SavingGoalEntity>
}