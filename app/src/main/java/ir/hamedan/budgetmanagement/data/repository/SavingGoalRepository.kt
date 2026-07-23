package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalDao
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import kotlinx.coroutines.flow.Flow

class SavingGoalRepository(private val savingGoalDao: SavingGoalDao) {

    fun getAllGoals(): Flow<List<SavingGoalEntity>> = savingGoalDao.getAllGoals()

    suspend fun insertGoal(goal: SavingGoalEntity) = savingGoalDao.insertGoal(goal)

    suspend fun updateGoal(goal: SavingGoalEntity) = savingGoalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: SavingGoalEntity) = savingGoalDao.deleteGoal(goal)

    suspend fun depositToGoal(goalId: String, amount: Double) = savingGoalDao.depositToGoal(goalId, amount)

    suspend fun withdrawFromGoal(goalId: String, amount: Double) = savingGoalDao.withdrawFromGoal(goalId, amount)
}