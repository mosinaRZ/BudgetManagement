package ir.hamedan.budgetmanagement

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.preferences.AppUsagePreferences
import ir.hamedan.budgetmanagement.data.preferences.CategorySeedPreferences
import ir.hamedan.budgetmanagement.di.AppContainer
import ir.hamedan.budgetmanagement.utils.AppNotificationManager
import ir.hamedan.budgetmanagement.worker.InactivityReminderWorker
import ir.hamedan.budgetmanagement.worker.MonthlyGoalDepositWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BudgetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppNotificationManager.createChannel(this)
        AppUsagePreferences.updateLastOpen(this)
        seedDefaultCategoriesIfNeeded()
        scheduleWorkers()
    }

    // ساخت دسته‌بندی‌های پیش‌فرض، فقط یک‌بار در طول عمر نصب اپ.
    // منتقل‌شده از AddViewModel تا دیگر وابسته به این نباشد که
    // کاربر وارد کدام صفحه شده، و اگر کاربر بعداً یکی از این
    // دسته‌بندی‌ها را حذف کند، دوباره ساخته نشود.
    private fun seedDefaultCategoriesIfNeeded() {
        if (CategorySeedPreferences.isSeeded(this)) return

        CoroutineScope(Dispatchers.IO).launch {
            val defaultCategories = listOf(
                CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true),
                CategoryEntity(title = "TRANSPORT", iconEmoji = "🚗", isExpense = true),
                CategoryEntity(title = "SHOPPING", iconEmoji = "🛍️", isExpense = true),
                CategoryEntity(title = "BILL", iconEmoji = "📄", isExpense = true),
                CategoryEntity(title = "DEBT_CREDIT_PAYABLE", iconEmoji = "💸", isExpense = true, isSystem = true), // بدهی
                CategoryEntity(title = "SALARY", iconEmoji = "💰", isExpense = false),
                CategoryEntity(title = "INVESTMENT", iconEmoji = "📈", isExpense = false),
                CategoryEntity(title = "DEBT_CREDIT_RECEIVABLE", iconEmoji = "📥", isExpense = false, isSystem = true), // طلب
                CategoryEntity(title = "SAVING_GOAL", iconEmoji = "🐷", isExpense = true, isSystem = true) // قلک/پس‌انداز ← جدید
            )

            val currentCategories = container.categoryRepository.getAllCategories().first()

            defaultCategories.forEach { category ->
                if (currentCategories.none { it.title == category.title }) {
                    container.categoryRepository.insertCategory(category)
                }
            }

            CategorySeedPreferences.setSeeded(this@BudgetApp)
        }
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // واریز خودکار ماهانه
        val monthlyWork = PeriodicWorkRequestBuilder<MonthlyGoalDepositWorker>(
            1, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "monthly_goal_deposit",
            ExistingPeriodicWorkPolicy.KEEP,
            monthlyWork
        )

        // یادآوری عدم فعالیت
        val inactivityWork = PeriodicWorkRequestBuilder<InactivityReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "inactivity_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            inactivityWork
        )
    }
}