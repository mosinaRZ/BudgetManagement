package ir.hamedan.budgetmanagement.ui.screens.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingGoalsViewModel(
    private val repository: SavingGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context
) : ViewModel() {

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

    private val _depositError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val depositError: SharedFlow<String> = _depositError

    fun addGoal(title: String, targetAmount: Double, monthlyAmount: Double, icon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(
                SavingGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    monthlyAmount = monthlyAmount,
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

    // حذف موقت جهت پاک‌سازی سریع از لیست UI
    fun softDelete(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
        }
    }

    // بازگردانی آیتم حذف‌شده در صورت زدن دکمه Undo
    fun restore(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(goal)
        }
    }

    // ثبت نهایی حذف و ارسال نوتیفیکیشن پس از اتمام تایمر ۵ ثانیه‌ای
    fun commitDelete(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
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
            val currentBalance = transactionRepository.getCurrentBalance()

            if (currentBalance < amount) {
                // پیام دوزبانه
                val isPersian = LocaleHelper.getLanguage(context) == "fa"
                val message = if (isPersian) {
                    "موجودی کافی نیست. موجودی فعلی: ${currentBalance.toLong()}"
                } else {
                    "Insufficient balance. Current balance: ${currentBalance.toLong()}"
                }
                _depositError.emit(message)
                return@launch
            }

            val currentGoal = savingGoals.value?.find { it.id == goalId }
            repository.depositToGoal(goalId, amount)

            val goalTitle = currentGoal?.title ?: "پس‌انداز"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = "واریز به قلک: $goalTitle",
                    amount = amount,
                    category = "SAVING_GOAL",
                    type = "EXPENSE",
                    note = "واریز ماهانه/دستی به هدف پس‌انداز «$goalTitle»"
                )
            )

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
            val currentGoal = savingGoals.value?.find { it.id == goalId }
            repository.withdrawFromGoal(goalId, amount)

            val goalTitle = currentGoal?.title ?: "پس‌انداز"

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = "برداشت از قلک: $goalTitle",
                    amount = amount,
                    category = "SAVING_GOAL",
                    type = "INCOME",
                    note = "برداشت از هدف پس‌انداز «$goalTitle»"
                )
            )

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
}