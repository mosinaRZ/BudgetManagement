package ir.hamedan.budgetmanagement.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.hamedan.budgetmanagement.ui.screens.add.AddViewModel
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsViewModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesViewModel
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import ir.hamedan.budgetmanagement.ui.screens.home.PendingTransactionViewModel
import ir.hamedan.budgetmanagement.ui.screens.notification.NotificationViewModel
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionViewModel
import ir.hamedan.budgetmanagement.ui.viewmodels.DebtCreditViewModel

/**
 * Central ViewModel factory.
 * All ViewModels are created from here so individual Factory classes can be removed.
 */
class AppViewModelFactory(
    private val container: AppContainer,
    private val appContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(
                    context = appContext,
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    notificationRepository = container.notificationRepository
                ) as T
            }
            modelClass.isAssignableFrom(AddViewModel::class.java) -> {
                AddViewModel(
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository,
                    context = appContext
                ) as T
            }
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) -> {
                CategoriesViewModel(
                    categoryRepository = container.categoryRepository,
                    context = appContext
                ) as T
            }
            modelClass.isAssignableFrom(SavingGoalsViewModel::class.java) -> {
                SavingGoalsViewModel(
                    repository = container.savingGoalRepository,
                    transactionRepository = container.transactionRepository,
                    context = appContext
                ) as T
            }
            modelClass.isAssignableFrom(BudgetLimitViewModel::class.java) -> {
                BudgetLimitViewModel(
                    budgetLimitRepository = container.budgetLimitRepository,
                    categoryRepository = container.categoryRepository,
                    transactionRepository = container.transactionRepository,
                    notificationRepository = container.notificationRepository,
                    context = appContext
                ) as T
            }
            modelClass.isAssignableFrom(NotificationViewModel::class.java) -> {
                NotificationViewModel(
                    repository = container.notificationRepository,
                    context = appContext
                ) as T
            }
            modelClass.isAssignableFrom(PendingTransactionViewModel::class.java) -> {
                PendingTransactionViewModel(
                    context = appContext,
                    pendingRepository = container.pendingTransactionRepository,
                    transactionRepository = container.transactionRepository,
                    categoryRepository = container.categoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                AnalyticsViewModel(
                    repository = container.transactionRepository
                ) as T
            }
            modelClass.isAssignableFrom(DebtCreditViewModel::class.java) -> {
                DebtCreditViewModel(
                    debtCreditRepository = container.debtCreditRepository,
                    transactionRepository = container.transactionRepository,
                    context = appContext
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}