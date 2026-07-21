package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.ui.graphics.Color

// ۱. مدل برای نمودار دایره‌ای (Pie Chart) - سهم هر دسته‌بندی از هزینه‌ها
data class CategoryExpenseModel(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float, // درصد از کل (مثلا ۲۵٪)
    val color: Color
)

// ۲. مدل برای نمودار میله‌ای/خطی (Bar/Line Chart) - روند درآمد/هزینه در طول زمان
data class MonthlyOverviewModel(
    val monthName: String,     // مثلاً: فروردین، اردیبهشت
    val totalIncome: Double,   // مجموع درآمد
    val totalExpense: Double   // مجموع هزینه
)

// ۳. مدل جامع برای کل State صفحه آنالیز (AnalyticsUiState)
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val categoryExpenses: List<CategoryExpenseModel> = emptyList(),
    val monthlyData: List<MonthlyOverviewModel> = emptyList(),
    val selectedPeriod: String = "MONTHLY" // WEEKLY, MONTHLY, YEARLY
)