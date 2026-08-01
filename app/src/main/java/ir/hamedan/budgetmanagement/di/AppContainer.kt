package ir.hamedan.budgetmanagement.di

import android.content.Context
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepositoryImpl

/**
 * Manual DI container.
 * Exposes repository interfaces; constructs *Impl implementations.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database = AppDatabase.getInstance(appContext)

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao(), database.transactionDao())
    }

    val savingGoalRepository: SavingGoalRepository by lazy {
        SavingGoalRepositoryImpl(database.savingGoalDao())
    }

    val budgetLimitRepository: BudgetLimitRepository by lazy {
        BudgetLimitRepositoryImpl(database.budgetLimitDao())
    }

    val upcomingPaymentRepository: UpcomingPaymentRepository by lazy {
        UpcomingPaymentRepositoryImpl(database.upcomingPaymentDao())
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(database.notificationDao())
    }

    val pendingTransactionRepository: PendingTransactionRepository by lazy {
        PendingTransactionRepositoryImpl(database.pendingTransactionDao())
    }

    fun viewModelFactory(): AppViewModelFactory {
        return AppViewModelFactory(this, appContext)
    }
}