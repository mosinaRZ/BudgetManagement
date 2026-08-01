package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import kotlinx.coroutines.flow.Flow

interface SavingGoalRepository {
    fun getAllGoals(): Flow<List<SavingGoalEntity>>
    suspend fun insertGoal(goal: SavingGoalEntity)
    suspend fun updateGoal(goal: SavingGoalEntity)
    suspend fun deleteGoal(goal: SavingGoalEntity)
    suspend fun depositToGoal(goalId: String, amount: Double)
    suspend fun withdrawFromGoal(goalId: String, amount: Double)
}
