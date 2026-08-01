package ir.hamedan.budgetmanagement.di

import android.content.Context
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository

/**
 * Simple manual DI container.
 * Holds all repositories. ViewModels are created via [AppViewModelFactory].
 *
 * Later this can be replaced by Hilt / Koin when moving to full Clean Architecture.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database = AppDatabase.getInstance(appContext)

    val transactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    val categoryRepository by lazy {
        CategoryRepository(database.categoryDao(), database.transactionDao())
    }

    val savingGoalRepository by lazy {
        SavingGoalRepository(database.savingGoalDao())
    }

    val budgetLimitRepository by lazy {
        BudgetLimitRepository(database.budgetLimitDao())
    }

    val upcomingPaymentRepository by lazy {
        UpcomingPaymentRepository(database.upcomingPaymentDao())
    }

    val notificationRepository by lazy {
        NotificationRepository(database.notificationDao())
    }

    val pendingTransactionRepository by lazy {
        PendingTransactionRepository(database.pendingTransactionDao())
    }

    /** Convenience factory for Compose / Activities */
    fun viewModelFactory(): AppViewModelFactory {
        return AppViewModelFactory(this, appContext)
    }
}
