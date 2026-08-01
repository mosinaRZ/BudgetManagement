package ir.hamedan.budgetmanagement.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.BudgetApp

/**
 * Convenience helper for Compose screens.
 * Creates (or remembers) the central AppViewModelFactory.
 */
@Composable
fun rememberAppViewModelFactory(): AppViewModelFactory {
    val context = LocalContext.current
    return remember {
        val app = context.applicationContext as BudgetApp
        app.container.viewModelFactory()
    }
}

/**
 * Type-safe shortcut:
 * val vm: TransactionViewModel = appViewModel()
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(): VM {
    val factory = rememberAppViewModelFactory()
    return viewModel(factory = factory)
}