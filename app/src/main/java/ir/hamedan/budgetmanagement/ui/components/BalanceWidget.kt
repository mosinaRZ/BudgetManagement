package ir.hamedan.budgetmanagement.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class BalanceWidget : GlanceAppWidget() {

    // کلمه کلیدی async حذف شد و کلمه suspend اضافه گردید
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // مقداردهی اولیه حتمی جهت جلوگیری از کرش هنگام بوت یا اجرای ایزوله
        CurrencySharedPreferences.init(context.applicationContext)

        val db = AppDatabase.getInstance(context)
        val transactionDao = db.transactionDao()

        provideContent {
            GlanceTheme {
                val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())
                val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState(initial = "TOMAN")

                // محاسبه بالانس کلی
                val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val totalBalance = totalIncome - totalExpense

                BalanceWidgetContent(balance = totalBalance, currencyUnit = currencyUnit)
            }
        }
    }

    @Composable
    private fun BalanceWidgetContent(balance: Double, currencyUnit: String) {
        val numberFormatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))

        val displayAmount = if (currencyUnit == "IRR") balance * 10.0 else balance
        val formattedAmount = numberFormatter.format(abs(displayAmount.toLong()))
        val sign = if (balance < 0) "-" else ""
        val unitText = if (currencyUnit == "IRR") "ریال" else "تومان"

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(GlanceTheme.colors.surface),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "موجودی کل (سیدنا)",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Text(
                text = "$sign$formattedAmount $unitText",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) GlanceTheme.colors.primary else GlanceTheme.colors.error
                )
            )
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}