package ir.hamedan.budgetmanagement.ui.screens.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingGoalsViewModel(
    private val repository: SavingGoalRepository
) : ViewModel() {

    // 🔥 تغییر تایپ به List<SavingGoalEntity>? و مقدار اولیه به null جهت جلوگیری از فلش زدن CTA
    val savingGoals: StateFlow<List<SavingGoalEntity>?> = repository.getAllGoals()
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
        }
    }

    fun updateGoal(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
        }
    }

    fun deposit(goalId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.depositToGoal(goalId, amount)
        }
    }

    fun withdraw(goalId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.withdrawFromGoal(goalId, amount)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SavingGoalsViewModel::class.java)) {
                val db = AppDatabase.getInstance(context)
                // 🔥 دریافت DAO از دیتابیس و پاس دادن آن به ریپازیتوری
                val repository = SavingGoalRepository(db.savingGoalDao())
                return SavingGoalsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}