package ir.hamedan.budgetmanagement

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.preferences.AppUsagePreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.di.AppContainer
import ir.hamedan.budgetmanagement.data.notification.AppNotificationManager
import ir.hamedan.budgetmanagement.worker.InactivityReminderWorker
import ir.hamedan.budgetmanagement.worker.MonthlyGoalDepositWorker
import ir.hamedan.budgetmanagement.worker.ProfileOccasionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BudgetApp : Application() {
    @Volatile
    var isAppInForeground: Boolean = false

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationPreferences.ensureDefaultsInitialized(this)
        AppNotificationManager.createChannel(this)
        AppUsagePreferences.updateLastOpen(this)
        if (container.authRepository.isAuthenticated()) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    seedDefaultCategoriesIfNeeded()
                    container.syncEngine.sync()
                }
            }
        }
        scheduleWorkers()
        container.syncScheduler.schedulePeriodic()
        observeConnectivity()
    }

    /**
     * Runs a sync (which also validates the session) every time the device gets a working
     * internet connection. A device that was removed from the account while it was offline
     * therefore learns about it right away, instead of at the next periodic sync, and the
     * navigation guard sends it to the login screen from whatever screen it is on.
     */
    private fun observeConnectivity() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                private var validatedNetwork: Network? = null

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (validated && validatedNetwork != network) {
                        validatedNetwork = network
                        if (container.authRepository.isAuthenticated()) {
                            container.syncScheduler.enqueueNow()
                        }
                    } else if (!validated && validatedNetwork == network) {
                        validatedNetwork = null
                    }
                }

                override fun onLost(network: Network) {
                    if (validatedNetwork == network) validatedNetwork = null
                }
            })
        }
    }

    /**
     * Seed built-in categories by stable identity, not by an installation-wide flag.
     * Pull existing account data first so a second device reuses the account's
     * canonical categories instead of creating another set with random UUIDs.
     */
    suspend fun seedDefaultCategoriesIfNeeded() = withContext(Dispatchers.IO) {
        if (container.authRepository.isAuthenticated()) {
            runCatching { container.syncEngine.sync() }
        }

        val definitions = listOf(
            Triple("FOOD", "🍕", true),
            Triple("TRANSPORT", "🚗", true),
            Triple("SHOPPING", "🛍️", true),
            Triple("BILL", "📄", true),
            Triple("DEBT_CREDIT_PAYABLE", "💸", true),
            Triple("SALARY", "💰", false),
            Triple("INVESTMENT", "📈", false),
            Triple("DEBT_CREDIT_RECEIVABLE", "📥", false),
            Triple("SAVING_GOAL", "🐷", true)
        )
        val systemTitles = setOf("DEBT_CREDIT_PAYABLE", "DEBT_CREDIT_RECEIVABLE", "SAVING_GOAL")
        val currentCategories = container.categoryRepository.getAllCategories().first()
        definitions.forEach { (title, emoji, isExpense) ->
            if (currentCategories.none { it.title == title }) {
                // Stable IDs make simultaneous first sign-ins on different devices converge.
                val stableId = java.util.UUID.nameUUIDFromBytes(
                    "budget-management:default-category:$title".toByteArray(Charsets.UTF_8)
                ).toString()
                container.categoryRepository.insertCategory(
                    CategoryEntity(
                        id = stableId,
                        title = title,
                        iconEmoji = emoji,
                        isExpense = isExpense,
                        isSystem = title in systemTitles
                    )
                )
            }
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

        // مناسبت‌های شخصی پروفایل: تولد و مناسبت‌های جنسیتی.
        val profileOccasionWork = PeriodicWorkRequestBuilder<ProfileOccasionWorker>(
            1, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "profile_occasion_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            profileOccasionWork
        )
    }
}