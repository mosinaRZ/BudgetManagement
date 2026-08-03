package ir.hamedan.budgetmanagement

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ir.hamedan.budgetmanagement.data.preferences.AppUsagePreferences
import ir.hamedan.budgetmanagement.di.AppContainer
import ir.hamedan.budgetmanagement.worker.InactivityReminderWorker
import ir.hamedan.budgetmanagement.worker.MonthlyGoalDepositWorker
import java.util.concurrent.TimeUnit

class BudgetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppUsagePreferences.updateLastOpen(this)
        scheduleWorkers()
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