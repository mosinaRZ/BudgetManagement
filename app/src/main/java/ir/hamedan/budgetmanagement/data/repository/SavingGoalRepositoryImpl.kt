package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalDao
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import kotlinx.coroutines.flow.Flow

class SavingGoalRepositoryImpl(
    private val savingGoalDao: SavingGoalDao
) : SavingGoalRepository {

    override fun getAllGoals(): Flow<List<SavingGoalEntity>> = savingGoalDao.getAllGoals()

    override suspend fun insertGoal(goal: SavingGoalEntity) = savingGoalDao.insertGoal(goal)

    override suspend fun updateGoal(goal: SavingGoalEntity) = savingGoalDao.updateGoal(goal)

    override suspend fun deleteGoal(goal: SavingGoalEntity) = savingGoalDao.deleteGoal(goal)

    override suspend fun depositToGoal(goalId: String, amount: Double) =
        savingGoalDao.depositToGoal(goalId, amount)

    override suspend fun withdrawFromGoal(goalId: String, amount: Double) =
        savingGoalDao.withdrawFromGoal(goalId, amount)

    override suspend fun updateLastAutoDepositTimestamp(goalId: String, timestamp: Long) {
        savingGoalDao.updateLastAutoDepositTimestamp(goalId, timestamp)
    }
}
