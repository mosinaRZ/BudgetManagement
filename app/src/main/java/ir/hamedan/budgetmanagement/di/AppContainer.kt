package ir.hamedan.budgetmanagement.di

import android.content.Context
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepositoryImpl

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database = AppDatabase.getInstance(appContext)

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }

    val budgetLimitRepository: BudgetLimitRepository by lazy {
        BudgetLimitRepositoryImpl(database.budgetLimitDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao(), database.transactionDao(), budgetLimitRepository)
    }

    val savingGoalRepository: SavingGoalRepository by lazy {
        SavingGoalRepositoryImpl(database.savingGoalDao())
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(database.notificationDao())
    }

    val pendingTransactionRepository: PendingTransactionRepository by lazy {
        PendingTransactionRepositoryImpl(database.pendingTransactionDao())
    }

    val debtCreditRepository: DebtCreditRepository by lazy {
        DebtCreditRepositoryImpl(database.debtCreditDao())
    }

    fun viewModelFactory(): AppViewModelFactory {
        return AppViewModelFactory(this, appContext)
    }
}