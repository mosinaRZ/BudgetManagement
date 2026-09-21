package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalDao
import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalOperationDao
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalOperationEntity
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import kotlinx.coroutines.flow.Flow

class SavingGoalRepositoryImpl(
    private val savingGoalDao: SavingGoalDao,
    private val operationDao: SavingGoalOperationDao,
    private val syncLocalDataSource: SyncLocalDataSource
) : SavingGoalRepository {
    override fun getAllGoals(): Flow<List<SavingGoalEntity>> = savingGoalDao.getAllGoals()

    override suspend fun insertGoal(goal: SavingGoalEntity) {
        val now = System.currentTimeMillis()
        val updated = goal.copy(createdAt = if (goal.createdAt > 0L) goal.createdAt else now, updatedAt = now)
        syncLocalDataSource.transaction {
            savingGoalDao.insertGoal(updated)
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.SAVING_GOAL, updated.id, now)
            if (updated.currentAmount != 0L && operationDao.getBaseline(updated.id) == null) {
                val baseline = SavingGoalOperationEntity(
                    goalId = updated.id,
                    deltaAmount = updated.currentAmount,
                    operationType = "BASELINE",
                    createdAt = now,
                    updatedAt = now
                )
                operationDao.insert(baseline)
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.SAVING_GOAL_OPERATION, baseline.id, now)
            }
        }
    }

    override suspend fun updateGoal(goal: SavingGoalEntity) {
        val now = System.currentTimeMillis()
        val updated = goal.copy(updatedAt = now)
        syncLocalDataSource.mutate(SyncEntityType.SAVING_GOAL, updated.id, now) { savingGoalDao.updateGoal(updated) }
    }

    override suspend fun deleteGoal(goal: SavingGoalEntity) {
        val now = System.currentTimeMillis()
        syncLocalDataSource.transaction {
            val operations = operationDao.getByGoalId(goal.id)
            savingGoalDao.deleteGoal(goal)
            operations.forEach { operation ->
                operationDao.deleteById(operation.id)
                syncLocalDataSource.recordMutationInTransaction(
                    SyncEntityType.SAVING_GOAL_OPERATION, operation.id, now, true
                )
            }
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.SAVING_GOAL, goal.id, now, true)
        }
    }

    override suspend fun depositToGoal(goalId: String, amount: Long) {
        require(amount > 0L) { "Deposit amount must be greater than zero." }
        recordOperation(goalId, amount)
    }

    override suspend fun withdrawFromGoal(goalId: String, amount: Long) {
        require(amount > 0L) { "Withdrawal amount must be greater than zero." }
        val current = savingGoalDao.getById(goalId)?.currentAmount ?: return
        require(current >= amount) { "Withdrawal amount exceeds the current saved amount." }
        recordOperation(goalId, -amount)
    }

    private suspend fun recordOperation(goalId: String, delta: Long) {
        val now = System.currentTimeMillis()
        syncLocalDataSource.transaction {
            val current = savingGoalDao.getById(goalId) ?: error("Saving goal not found: $goalId")
            val next = (current.currentAmount + delta).coerceAtLeast(0L)
            savingGoalDao.setCurrentAmount(goalId, next, now)
            val operation = SavingGoalOperationEntity(
                goalId = goalId,
                deltaAmount = delta,
                operationType = "DELTA",
                createdAt = now,
                updatedAt = now
            )
            operationDao.insert(operation)
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.SAVING_GOAL_OPERATION, operation.id, now)
        }
    }

    override suspend fun updateLastAutoDepositTimestamp(goalId: String, timestamp: Long) {
        val now = System.currentTimeMillis()
        syncLocalDataSource.mutate(SyncEntityType.SAVING_GOAL, goalId, now) {
            savingGoalDao.updateLastAutoDepositTimestamp(goalId, timestamp, now)
        }
    }
}