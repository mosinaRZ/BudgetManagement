package ir.hamedan.budgetmanagement.ui.screens.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingGoalsViewModel(
    private val repository: SavingGoalRepository,
    private val context: Context
) : ViewModel() {

    // 🔥 دریافت اهداف و بررسی درصد پیشرفت برای ارسال اعلان هوشمند در صورت رسیدن به آستانه‌ها
    val savingGoals: StateFlow<List<SavingGoalEntity>?> = repository.getAllGoals()
        .map { goals ->
            goals?.onEach { goal ->
                val percentReached = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100) else 0.0

                listOf(50.0, 80.0, 100.0).forEach { threshold ->
                    if (percentReached >= threshold) {
                        val tag = "GOAL_PROGRESS_${threshold.toInt()}_${goal.id}"
                        viewModelScope.launch {
                            val isCompleted = threshold >= 100.0
                            NotificationHelper.send(
                                context = context,
                                type = if (isCompleted) "SUCCESS" else "WARNING",
                                titleFa = if (isCompleted) "تکمیل هدف پس‌انداز 🎉" else "پیشرفت هدف پس‌انداز",
                                titleEn = if (isCompleted) "Saving Goal Completed!" else "Saving Goal Progress",
                                descFa = if (isCompleted)
                                    "تبریک! به ۱۰۰٪ هدف «${goal.title}» رسیدید."
                                else
                                    "هدف «${goal.title}» به ${threshold.toInt()}٪ رسید.",
                                descEn = if (isCompleted)
                                    "Congratulations! You reached 100% of goal \"${goal.title}\"."
                                else
                                    "Goal \"${goal.title}\" reached ${threshold.toInt()}%.",
                                tag = tag
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addGoal(title: String, targetAmount: Double, icon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(
                SavingGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    icon = icon
                )
            )

            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "هدف پس‌انداز جدید ثبت شد",
                titleEn = "New Saving Goal Added",
                descFa = "هدف «$title» با مبلغ هدف $targetAmount ایجاد شد.",
                descEn = "Saving goal \"$title\" was created with target $targetAmount.",
                tag = "GOAL_ADD_${title}_${System.currentTimeMillis()}"
            )
        }
    }

    fun updateGoal(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGoal(goal)

            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = "ویرایش هدف پس‌انداز",
                titleEn = "Saving Goal Updated",
                descFa = "اطلاعات هدف «${goal.title}» ویرایش شد.",
                descEn = "Saving goal \"${goal.title}\" was updated.",
                tag = "GOAL_UPDATE_${goal.id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun deleteGoal(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)

            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = "حذف هدف پس‌انداز",
                titleEn = "Saving Goal Deleted",
                descFa = "هدف «${goal.title}» با موفقیت حذف شد.",
                descEn = "Saving goal \"${goal.title}\" was successfully deleted.",
                tag = "GOAL_DELETE_${goal.id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun deposit(goalId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentGoal = savingGoals.value?.find { it.id.toString() == goalId }
            repository.depositToGoal(goalId, amount)

            val goalTitle = currentGoal?.title ?: "پس‌انداز"

            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "واریز به هدف پس‌انداز",
                titleEn = "Deposit to Saving Goal",
                descFa = "مبلغ $amount به هدف «$goalTitle» واریز شد.",
                descEn = "Amount $amount deposited to goal \"$goalTitle\".",
                tag = "GOAL_DEPOSIT_${goalId}_${System.currentTimeMillis()}"
            )
        }
    }

    fun withdraw(goalId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentGoal = savingGoals.value?.find { it.id.toString() == goalId }
            repository.withdrawFromGoal(goalId, amount)

            val goalTitle = currentGoal?.title ?: "پس‌انداز"

            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = "برداشت از هدف پس‌انداز",
                titleEn = "Withdrawal from Saving Goal",
                descFa = "مبلغ $amount از هدف «$goalTitle» برداشت شد.",
                descEn = "Amount $amount withdrawn from goal \"$goalTitle\".",
                tag = "GOAL_WITHDRAW_${goalId}_${System.currentTimeMillis()}"
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SavingGoalsViewModel::class.java)) {
                val app = context.applicationContext as BudgetApp
                return SavingGoalsViewModel(
                    repository = app.container.savingGoalRepository,
                    context = context.applicationContext
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}