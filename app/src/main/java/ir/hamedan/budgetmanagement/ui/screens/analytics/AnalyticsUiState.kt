package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.ui.graphics.Color
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity

data class CategoryExpenseModel(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val color: Color
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val hasAnyTransactionInDb: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val categoryExpenses: List<CategoryExpenseModel> = emptyList(),
    val topExpenses: List<TransactionEntity> = emptyList(),
    val averageExpense: Double = 0.0,
    val trendPoints: List<Float> = emptyList(),
    val selectedPeriod: String = "MONTHLY"
)