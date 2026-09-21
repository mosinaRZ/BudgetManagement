package ir.hamedan.budgetmanagement.ui.screens.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SavingGoalsViewModel(
    private val repository: SavingGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
    private val context: Context
) : ViewModel() {

    val savingGoals: StateFlow<List<SavingGoalEntity>?> = repository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _depositError = MutableSharedFlow<String>()
    val depositError: SharedFlow<String> = _depositError

    fun addGoal(title: String, targetAmount: Long, monthlyAmount: Long, icon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newGoal = SavingGoalEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0L,
                monthlyAmount = monthlyAmount,
                icon = icon
            )

            repository.insertGoal(newGoal)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.GOAL_ADD,
                type = "GOALS",
                titleFa = "هدف پس‌انداز جدید",
                titleEn = "New Saving Goal",
                descFa = "هدف «$title» با موفقیت ایجاد شد.",
                descEn = "Saving goal '$title' was created successfully."
            )
        }
    }

    fun updateGoal(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGoal(goal)

            // ارسال اعلان ویرایش هدف
            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.GOAL_UPDATE,
                type = "GOALS",
                titleFa = "ویرایش هدف پس‌انداز",
                titleEn = "Saving Goal Updated",
                descFa = "اطلاعات هدف «${goal.title}» به‌روزرسانی شد.",
                descEn = "Goal '${goal.title}' details were updated."
            )
        }
    }

    fun deposit(goalId: String, amount: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (amount <= 0) {
                val isPersian = LocaleHelper.getLanguage(context) == "fa"
                val msg = if (isPersian) "مبلغ واریز باید بیشتر از صفر باشد" else "Deposit amount must be greater than zero"
                _depositError.emit(msg)
                return@launch
            }

            repository.depositToGoal(goalId, amount)
            // یافتن هدف برای محاسبه درصد پیشرفت و ارسال اعلان
            val currentGoal = savingGoals.value?.find { it.id == goalId }
            val goalTitle = currentGoal?.title ?: ""

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.GOAL_DEPOSIT,
                type = "GOALS",
                titleFa = "واریز به هدف پس‌انداز",
                titleEn = "Deposit to Saving Goal",
                descFa = "مبلغ $amount به هدف «$goalTitle» واریز شد.",
                descEn = "Amount $amount deposited to goal '$goalTitle'."
            )

            // بررسی درصد پیشرفت پس از واریز
            if (currentGoal != null && currentGoal.targetAmount > 0) {
                val newAmount = currentGoal.currentAmount + amount
                val newRatio = newAmount.toDouble() / currentGoal.targetAmount.toDouble()

                val (percentageTag, percentageText) = when {
                    newRatio >= 1.0 -> "100" to "۱۰۰٪"
                    newRatio >= 0.8 -> "80" to "۸۰٪"
                    newRatio >= 0.5 -> "50" to "۵۰٪"
                    else -> null to null
                }

                if (percentageTag != null) {
                    NotificationHelper.send(
                        context = context,
                        notificationType = NotificationType.GOAL_PROGRESS,
                        type = "GOALS",
                        titleFa = "پیشرفت هدف پس‌انداز",
                        titleEn = "Saving Goal Progress",
                        descFa = "هدف «$goalTitle» به پیشرفت $percentageText رسید!",
                        descEn = "Goal '$goalTitle' reached $percentageText progress!",
                        tag = "goal_progress_${goalId}_$percentageTag"
                    )
                }
            }
        }
    }

    fun withdraw(goalId: String, amount: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (amount <= 0) return@launch
            repository.withdrawFromGoal(goalId, amount)
            val goalTitle = savingGoals.value?.find { it.id == goalId }?.title ?: ""

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.GOAL_WITHDRAW,
                type = "GOALS",
                titleFa = "برداشت از هدف پس‌انداز",
                titleEn = "Withdrawal from Saving Goal",
                descFa = "مبلغ $amount از هدف «$goalTitle» برداشت شد.",
                descEn = "Amount $amount withdrawn from goal '$goalTitle'."
            )
        }
    }

    fun softDelete(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
            sendDeleteNotification(goal.title)
        }
    }

    fun restore(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(goal)
        }
    }

    fun commitDelete(goal: SavingGoalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGoal(goal)
            sendDeleteNotification(goal.title)
        }
    }

    private fun sendDeleteNotification(title: String) {
        NotificationHelper.send(
            context = context,
            notificationType = NotificationType.GOAL_DELETE,
            type = "GOALS",
            titleFa = "حذف هدف پس‌انداز",
            titleEn = "Saving Goal Deleted",
            descFa = "هدف «$title» حذف شد.",
            descEn = "Saving goal '$title' was deleted."
        )
    }

    fun updateLastAutoDepositTimestamp(goalId: String, timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLastAutoDepositTimestamp(goalId, timestamp)
        }
    }
}