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

class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

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
}