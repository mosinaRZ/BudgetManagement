package ir.hamedan.budgetmanagement.domain.usecase

import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository

class SavingGoalUseCase(private val repository: SavingGoalRepository) {
    suspend fun create(title: String, targetAmount: Long, monthlyAmount: Long, icon: String): SavingGoalEntity {
        require(title.isNotBlank()) { "Saving goal title is required." }
        require(targetAmount > 0L) { "Saving goal target must be greater than zero." }
        require(monthlyAmount >= 0L) { "Monthly amount cannot be negative." }
        val goal = SavingGoalEntity(title = title.trim(), targetAmount = targetAmount, monthlyAmount = monthlyAmount, icon = icon)
        repository.insertGoal(goal)
        return goal
    }

    suspend fun update(goal: SavingGoalEntity) {
        require(goal.id.isNotBlank()) { "Saving goal id is required." }
        require(goal.title.isNotBlank()) { "Saving goal title is required." }
        require(goal.targetAmount > 0L) { "Saving goal target must be greater than zero." }
        repository.updateGoal(goal)
    }

    suspend fun deposit(goalId: String, amount: Long) {
        require(amount > 0L) { "Deposit amount must be greater than zero." }
        repository.depositToGoal(goalId, amount)
    }

    suspend fun withdraw(goalId: String, amount: Long) {
        require(amount > 0L) { "Withdrawal amount must be greater than zero." }
        repository.withdrawFromGoal(goalId, amount)
    }

    suspend fun delete(goal: SavingGoalEntity) = repository.deleteGoal(goal)
    suspend fun restore(goal: SavingGoalEntity) = repository.insertGoal(goal)
    suspend fun updateAutoDeposit(goalId: String, timestamp: Long) = repository.updateLastAutoDepositTimestamp(goalId, timestamp)
}