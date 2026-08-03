package ir.hamedan.budgetmanagement.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
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
        val isPersian = LocaleHelper.getLanguage(applicationContext) == "fa"

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
                    type = "WARNING",
                    titleFa = "موجودی ناکافی برای قلک",
                    titleEn = "Insufficient Balance for Goal",
                    descFa = "موجودی کافی برای واریز ماهانه به «${goal.title}» وجود ندارد.",
                    descEn = "Not enough balance to deposit monthly amount to \"${goal.title}\".",
                    tag = "GOAL_AUTO_FAIL_${goal.id}"
                )
                continue
            }

            // واریز
            goalRepository.depositToGoal(goal.id, goal.monthlyAmount)
            // آپدیت زمان آخرین واریز
            goalRepository.updateGoal(
                goal.copy(
                    currentAmount = goal.currentAmount + goal.monthlyAmount,
                    lastAutoDepositTimestamp = now
                )
            )

            currentBalance -= goal.monthlyAmount

            transactionRepository.insertTransaction(
                TransactionEntity(
                    title = if (isPersian) "واریز خودکار ماهانه به قلک: ${goal.title}"
                    else "Auto Monthly Deposit to: ${goal.title}",
                    amount = goal.monthlyAmount,
                    category = "قلک",
                    type = "EXPENSE",
                    note = if (isPersian) "واریز خودکار ماهانه" else "Automatic monthly deposit"
                )
            )

            NotificationHelper.send(
                context = applicationContext,
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