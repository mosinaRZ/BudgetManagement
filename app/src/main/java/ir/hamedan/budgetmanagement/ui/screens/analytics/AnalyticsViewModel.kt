package ir.hamedan.budgetmanagement.ui.screens.analytics

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlin.math.abs

class AnalyticsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    // محاسبه زنده آمار و نمودارها بر اساس تراکنش‌های Room
    // نکته: چون Room مستقیماً Flow<List<TransactionEntity>> برمی‌گردونه، دیگه result.list لازم نیست
    val uiState: StateFlow<AnalyticsUiState> = repository.getAllTransactions()
        .map { transactions ->

            // ۱. محاسبه مجموع کل
            val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            // ۲. گروه‌بندی هزینه‌ها بر اساس دسته‌بندی برای نمودار دایره‌ای
            val categoryExpenses = transactions
                .filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .map { (category, list) ->
                    val sum = list.sumOf { it.amount }
                    val percent = if (totalExpense > 0) (sum / totalExpense * 100).toFloat() else 0f
                    CategoryExpenseModel(
                        categoryName = category,
                        totalAmount = sum,
                        percentage = percent,
                        color = generateColorForCategory(category) // تابع کمکی برای رنگ
                    )
                }

            AnalyticsUiState(
                isLoading = false,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense,
                categoryExpenses = categoryExpenses
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState(isLoading = true)
        )

    // 🎯 Factory جدید — قبلاً چون RealmDatabase یک آبجکت گلوبال بدون Context بود لازم نبود،
    // ولی Room.databaseBuilder به Context نیاز داره، پس این ویومدل هم مثل AddViewModel یک Factory می‌خواد.
    // هرجا که این ویومدل رو با viewModel() می‌سازی (مثلاً AnalyticsScreen.kt که هنوز برام نفرستادی)،
    // باید به AnalyticsViewModel.Factory(LocalContext.current) تغییرش بدی.
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(context)
            return AnalyticsViewModel(
                repository = TransactionRepository(database.transactionDao())
            ) as T
        }
    }
}


// تابع تولید رنگ یکتا و ثابت بر اساس نام دسته‌بندی
private fun generateColorForCategory(categoryName: String): Color {
    // لیست پالت رنگ‌های جذاب و استاندارد برای UI
    val colors = listOf(
        Color(0xFF6750A4), // بنفش
        Color(0xFF0288D1), // آبی
        Color(0xFF388E3C), // سبز
        Color(0xFFF57C00), // نارنجی
        Color(0xFFD32F2F), // قرمز
        Color(0xFF7B1FA2), // ارغوانی
        Color(0xFF00796B), // سبز کله‌غازی
        Color(0xFFC2185B), // صورتی پررنگ
        Color(0xFFE64A19), // نارنجی تیره
        Color(0xFF512DA8)  // نیلی
    )

    if (categoryName.isEmpty()) return colors[0]

    // نگاشت هش کدِ اسم دسته‌بندی به یکی از رنگ‌های پالت بالا برای ثابت ماندن رنگ در اجراهای بعدی
    val index = abs(categoryName.hashCode()) % colors.size
    return colors[index]
}