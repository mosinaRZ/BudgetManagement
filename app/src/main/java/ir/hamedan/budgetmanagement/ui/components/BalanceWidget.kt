package ir.hamedan.budgetmanagement.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.layout.ContentScale
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ir.hamedan.budgetmanagement.MainActivity
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        CurrencySharedPreferences.init(context.applicationContext)

        val db = AppDatabase.getInstance(context)
        val transactionDao = db.transactionDao()

        val isPersian = LocaleHelper.getLanguage(context) == "fa"
        val themeMode = ThemePreferences.getThemeMode(context)
        val isDark = when (themeMode) {
            ThemePreferences.MODE_LIGHT -> false
            ThemePreferences.MODE_DARK -> true
            else -> (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

        provideContent {
            val colors = if (isDark) AppDarkColors else AppLightColors

            val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())
            val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState(initial = "IRT")

            val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalBalance = totalIncome - totalExpense

            BalanceWidgetContent(
                balance = totalBalance,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                colors = colors
            )
        }
    }

    @Composable
    private fun BalanceWidgetContent(
        balance: Double,
        currencyUnit: String,
        isPersian: Boolean,
        colors: WidgetColors
    ) {
        val numberFormatter = NumberFormat.getNumberInstance(
            if (isPersian) Locale("fa", "IR") else Locale.US
        )

        val displayAmount = if (currencyUnit == "IRR") balance * 10.0 else balance
        val formattedAmount = numberFormatter.format(abs(displayAmount.toLong()))
        val sign = if (balance < 0) "-" else ""

        val unitText = if (isPersian) {
            if (currencyUnit == "IRR") "ریال" else "تومان"
        } else {
            if (currencyUnit == "IRR") "Rial" else "Toman"
        }

        val labelText = if (isPersian) "تراز کلی حساب" else "Total Account Balance"

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(colors.surfaceVariant)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // بنر پس‌زمینه
            Image(
                provider = ImageProvider(R.drawable.balancebanner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>())
            )

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>())
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // باکس متن‌ها برای خوانایی کامل
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .clickable(actionStartActivity<MainActivity>())

                ) {
                    Text(
                        text = labelText,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    Text(
                        text = "$sign$formattedAmount $unitText",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) colors.primary else colors.error
                        )
                    )
                }
            }
        }
    }

    // رنگ‌های دقیق برنامه (از Color.kt)
    private data class WidgetColors(
        val primary: ColorProvider,
        val error: ColorProvider,
        val surface: ColorProvider,
        val surfaceVariant: ColorProvider,
        val onSurfaceVariant: ColorProvider
    )

    private val AppLightColors = WidgetColors(
        primary = ColorProvider(day = Color(0xFF408A71), night = Color(0xFF408A71)),
        error = ColorProvider(day = Color(0xFFBA1A1A), night = Color(0xFFBA1A1A)),
        surface = ColorProvider(day = Color(0xFFF4FBF7), night = Color(0xFFF4FBF7)),
        surfaceVariant = ColorProvider(day = Color(0xFFDBE5E0), night = Color(0xFFDBE5E0)),
        onSurfaceVariant = ColorProvider(day = Color(0xFF3F4944), night = Color(0xFF3F4944))
    )

    private val AppDarkColors = WidgetColors(
        primary = ColorProvider(day = Color(0xFFB0E4CC), night = Color(0xFFB0E4CC)),
        error = ColorProvider(day = Color(0xFFFFB4AB), night = Color(0xFFFFB4AB)),
        surface = ColorProvider(day = Color(0xFF091413), night = Color(0xFF091413)),
        surfaceVariant = ColorProvider(day = Color(0xFF3F4944), night = Color(0xFF3F4944)),
        onSurfaceVariant = ColorProvider(day = Color(0xFFBFC9C3), night = Color(0xFFBFC9C3))
    )
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}

// تابع کمکی برای آپدیت فوری ویجت
suspend fun updateBalanceWidget(context: Context) {
    BalanceWidget().updateAll(context)
}