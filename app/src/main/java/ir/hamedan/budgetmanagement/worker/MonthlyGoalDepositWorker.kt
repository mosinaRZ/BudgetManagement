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
import java.util.concurrent.TimeUnit

class MonthlyGoalDepositWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val DAYS_THRESHOLD = 30L
    }

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

        for (goal in goals) {
            if (goal.monthlyAmount <= 0) continue

            val daysSinceLastDeposit = if (goal.lastAutoDepositTimestamp == 0L) {
                DAYS_THRESHOLD // اولین بار اجازه بده
            } else {
                TimeUnit.MILLISECONDS.toDays(now - goal.lastAutoDepositTimestamp)
            }

            if (daysSinceLastDeposit < DAYS_THRESHOLD) continue

            if (currentBalance < goal.monthlyAmount) {
                NotificationHelper.send(
                    context = applicationContext,
                    notificationType = NotificationType.GOAL_AUTO_DEPOSIT,
                    type = "WARNING",
                    titleFa = "موجودی ناکافی برای قلک",
                    titleEn = "Insufficient Balance for Goal",
                    descFa = "موجودی کافی برای واریز ماهانه به «${goal.title}» وجود ندارد.",
                    descEn = "Not enough balance to deposit monthly amount to \"${goal.title}\".",
                    tag = "GOAL_AUTO_FAIL_${goal.id}"
                )
                continue
            }

            // واریز مبلغ به قلک
            goalRepository.depositToGoal(goal.id, goal.monthlyAmount)

            currentBalance -= goal.monthlyAmount

            // ثبت تراکنش با همان دسته‌بندی سیستمی SAVING_GOAL.
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

            // فقط بعد از ثبت موفق تراکنش، زمان آخرین واریز را ثبت کن.
            goalRepository.updateLastAutoDepositTimestamp(goal.id, now)

            NotificationHelper.send(
                context = applicationContext,
                notificationType = NotificationType.GOAL_AUTO_DEPOSIT,
                type = "SUCCESS",
                titleFa = "واریز خودکار ماهانه",
                titleEn = "Auto Monthly Deposit",
                descFa = "مبلغ ${goal.monthlyAmount.toLong()} به قلک «${goal.title}» واریز شد.",
                descEn = "Amount ${goal.monthlyAmount.toLong()} deposited to \"${goal.title}\".",
                tag = "GOAL_AUTO_SUCCESS_${goal.id}"
            )
        }

        return Result.success()
    }
}