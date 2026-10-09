package ir.hamedan.budgetmanagement.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MonthlyGoalDepositWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BudgetApp
        val goalRepository = app.container.savingGoalRepository
        val transactionRepository = app.container.transactionRepository
        val categoryRepository = app.container.categoryRepository
        val isPersian = LocaleHelper.getLanguage(applicationContext) == "fa"

        // Resolve the canonical system category before mutating any goal.
        // The previous code deposited first and looked up SAVING_GOAL afterwards;
        // during first-launch seeding this could make the deposit succeed while
        // the transaction was silently skipped by `?: continue`.
        //
        // Self-heal the category if it is temporarily missing. The title is the
        // same stable key used by BudgetApp, so existing seeded categories are
        // always reused rather than replaced.
        val savingGoalCategory = categoryRepository.getAllCategories().first()
            .firstOrNull { it.title == "SAVING_GOAL" }
            ?: CategoryEntity(
                title = "SAVING_GOAL",
                iconEmoji = "🐷",
                isExpense = true,
                isSystem = true
            ).also { categoryRepository.insertCategory(it) }

        val goals = goalRepository.getAllGoals().first()
        var currentBalance = transactionRepository.getCurrentBalance()
        val now = System.currentTimeMillis()

        val today = Calendar.getInstance().apply { timeInMillis = now }
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)

        for (goal in goals) {
            if (goal.monthlyAmount <= 0L) continue

            // The monthly due date is the day of month on which the goal was created.
            // Clamp dates such as the 31st to the final day of shorter months.
            val created = Calendar.getInstance().apply { timeInMillis = goal.createdAt }
            val preferredDay = created.get(Calendar.DAY_OF_MONTH)
            val dueDay = preferredDay.coerceAtMost(today.getActualMaximum(Calendar.DAY_OF_MONTH))
            if (dayOfMonth < dueDay) continue

            val alreadyProcessedThisMonth = goal.lastAutoDepositTimestamp > 0L &&
                    Calendar.getInstance().apply { timeInMillis = goal.lastAutoDepositTimestamp }.let {
                        it.get(Calendar.YEAR) == year && it.get(Calendar.MONTH) == month
                    }
            if (alreadyProcessedThisMonth) continue

            val monthTag = "%04d-%02d".format(year, month + 1)
            if (currentBalance < goal.monthlyAmount) {
                NotificationHelper.send(
                    context = applicationContext,
                    notificationType = NotificationType.GOAL_AUTO_DEPOSIT,
                    type = "WARNING",
                    titleFa = "واریز ماهانه قلک انجام نشد",
                    titleEn = "Monthly Goal Deposit Skipped",
                    descFa = "برای واریز ${goal.monthlyAmount} به قلک «${goal.title}» در روز $dueDay ماه، موجودی کافی نیست. پس از تأمین موجودی، واریز در اجرای بعدی دوباره بررسی می‌شود.",
                    descEn = "The balance is too low to deposit ${goal.monthlyAmount} to \"${goal.title}\" on day $dueDay. The deposit will be retried automatically when the worker runs again.",
                    tag = "GOAL_AUTO_FAIL_${goal.id}_$monthTag"
                )
                continue
            }

            try {
                goalRepository.depositToGoal(goal.id, goal.monthlyAmount)
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        title = if (isPersian) "واریز خودکار ماهانه به قلک: ${goal.title}"
                        else "Auto Monthly Deposit to: ${goal.title}",
                        amount = goal.monthlyAmount,
                        categoryId = savingGoalCategory.id,
                        type = "EXPENSE",
                        note = if (isPersian) "واریز خودکار ماهانه" else "Automatic monthly deposit"
                    )
                )
                goalRepository.updateLastAutoDepositTimestamp(goal.id, now)
                currentBalance -= goal.monthlyAmount

                NotificationHelper.send(
                    context = applicationContext,
                    notificationType = NotificationType.GOAL_AUTO_DEPOSIT,
                    type = "SUCCESS",
                    titleFa = "واریز خودکار ماهانه انجام شد",
                    titleEn = "Monthly Goal Deposit Completed",
                    descFa = "مبلغ ${goal.monthlyAmount} در روز $dueDay ماه به قلک «${goal.title}» واریز شد.",
                    descEn = "${goal.monthlyAmount} was deposited to \"${goal.title}\" on day $dueDay of the month.",
                    tag = "GOAL_AUTO_SUCCESS_${goal.id}_$monthTag"
                )
            } catch (error: Exception) {
                NotificationHelper.send(
                    context = applicationContext,
                    notificationType = NotificationType.GOAL_AUTO_DEPOSIT,
                    type = "WARNING",
                    titleFa = "خطا در واریز ماهانه قلک",
                    titleEn = "Monthly Goal Deposit Error",
                    descFa = "واریز ماهانه قلک «${goal.title}» کامل نشد. لطفاً تراکنش‌ها و موجودی را بررسی کنید؛ برنامه دوباره تلاش خواهد کرد.",
                    descEn = "The monthly deposit to \"${goal.title}\" did not complete. Please review the balance and transactions; the app will retry.",
                    tag = "GOAL_AUTO_ERROR_${goal.id}_$monthTag"
                )
                return Result.retry()
            }
        }

        return Result.success()
    }
}