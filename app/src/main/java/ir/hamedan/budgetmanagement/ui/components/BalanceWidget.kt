package ir.hamedan.budgetmanagement.ui.components

import android.content.Context
import android.content.res.Configuration
import android.text.Layout
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.MainActivity
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.preferences.AmountPrivacyPreferences
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.ThemePreferences
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.theme.DarkColorScheme
import ir.hamedan.budgetmanagement.ui.theme.LightColorScheme
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// ───────────────────────────── Widget ─────────────────────────────

class BalanceWidget : GlanceAppWidget() {

    // اندازه‌ی واقعی ویجت را می‌گیریم تا فیت مبلغ دقیق باشد و ویجت وقتی بلندتر شد خلاصه‌ی درآمد/هزینه را نشان دهد
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        CurrencySharedPreferences.init(context.applicationContext)

        val app = context.applicationContext as BudgetApp
        val repository = app.container.transactionRepository

        // مقدار اولیه‌ی واقعی؛ بدون این، ویجت لحظه‌ای «۰» نشان می‌داد و بعد می‌پرید
        val initialTransactions = repository.getAllTransactions().first()

        val isPersian = LocaleHelper.getLanguage(context) == "fa"
        val isDark = when (ThemePreferences.getThemeMode(context)) {
            ThemePreferences.MODE_LIGHT -> false
            ThemePreferences.MODE_DARK -> true
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        }
        AmountPrivacyPreferences.init(context)
        val palette = WidgetPalette.from(if (isDark) DarkColorScheme else LightColorScheme)

        provideContent {
            val transactions by repository.getAllTransactions()
                .collectAsState(initial = initialTransactions)
            val currencyUnit by CurrencySharedPreferences.currencyFlow
                .collectAsState(initial = CurrencySharedPreferences.getCurrency(context))
            val amountsHidden by AmountPrivacyPreferences.hiddenFlow.collectAsState()

            val totals = remember(transactions) {
                val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                Triple(income - expense, income, expense)
            }

            BalanceWidgetContent(
                balance = totals.first,
                income = totals.second,
                expense = totals.third,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                amountsHidden = amountsHidden,
                palette = palette
            )
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}

/** به‌روزرسانی فوری ویجت (بدون تغییر نسبت به قبل). */
suspend fun updateBalanceWidget(context: Context) {
    BalanceWidget().updateAll(context)
}

// ───────────────────────────── UI ─────────────────────────────

private val CardRadius = 24.dp     // همان balanceShape در HomeScreen
private val ChipRadius = 20.dp     // همان summaryCardShape در HomeScreen
private val BorderWidth = 1.dp
private const val HEADER_HEIGHT_DP = 28f
private const val CHIP_HEIGHT_DP = 58f
private const val CHIP_GAP_DP = 10f
private const val H_PADDING_DP = 20f
private const val TALL_WIDGET_MIN_HEIGHT_DP = 150f
private const val LINE_HEIGHT_RATIO = 1.6f   // نسبت ارتفاع خط به اندازه‌ی فونت (وزیرمتن ≈ 1.56)

@Composable
private fun BalanceWidgetContent(
    balance: Long,
    income: Long,
    expense: Long,
    currencyUnit: String,
    isPersian: Boolean,
    amountsHidden: Boolean,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isTall = size.height.value >= TALL_WIDGET_MIN_HEIGHT_DP

    // ── اندازه‌ها ──
    val padV = if (isTall) 16f else 12f
    val innerWidth = size.width.value - 2 * BorderWidth.value - 2 * H_PADDING_DP
    val reservedBelow = if (isTall) CHIP_HEIGHT_DP + CHIP_GAP_DP else 0f
    val availableForAmount =
        size.height.value - 2 * BorderWidth.value - 2 * padV - HEADER_HEIGHT_DP - reservedBelow - 6f
    val heroMaxSp = (availableForAmount / LINE_HEIGHT_RATIO).coerceIn(16f, 28f)

    // ── متن‌ها ──
    val formatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }
    val heroUnit = unitLabel(currencyUnit, isPersian, short = false)
    val chipUnit = unitLabel(currencyUnit, isPersian, short = true)
    val label = if (isPersian) "تراز کلی حساب" else "Total Account Balance"

    val heroTexts = amountTexts(balance, currencyUnit, isPersian, formatter, amountsHidden, signed = true)
    val heroColor = if (balance >= 0) palette.primary else palette.error

    val hero = remember(heroTexts, heroUnit, heroColor, innerWidth, heroMaxSp, isPersian) {
        WidgetTextRenderer.renderAmount(
            context = context,
            amountText = heroTexts.full,
            compactText = heroTexts.compact,
            unitText = heroUnit,
            isPersian = isPersian,
            amountColor = heroColor.toArgb(),
            unitColor = heroColor.copy(alpha = 0.75f).toArgb(),
            maxWidthDp = innerWidth,
            maxSp = heroMaxSp,
            minSp = 14f,
            unitScale = 0.5f,
            alignment = Layout.Alignment.ALIGN_NORMAL
        )
    }

    val openApp = actionStartActivity<MainActivity>()

    // حلقه‌ی بیرونی = رنگ border، حلقه‌ی داخلی = رنگ کارت  ⇒ همان border ‏۱dp با شفافیت ۰٫۱۵ در HomeScreen
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(CardRadius)
            .background(ColorProvider(palette.border))
            .padding(BorderWidth)
            .clickable(openApp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(CardRadius - BorderWidth)
                .background(ColorProvider(palette.card))
                .padding(horizontal = H_PADDING_DP.dp, vertical = padV.dp)
        ) {
            // ── هدر: عنوان + دکمه‌ی چشم (همگام با HomeScreen) ──
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT_DP.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(palette.label)
                    ),
                    maxLines = 1
                )
                Box(
                    modifier = GlanceModifier
                        .size(HEADER_HEIGHT_DP.dp)
                        .clickable(actionRunCallback<ToggleAmountsVisibilityAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            if (amountsHidden) R.drawable.ic_widget_visibility_off
                            else R.drawable.ic_widget_visibility
                        ),
                        contentDescription = if (isPersian) {
                            if (amountsHidden) "نمایش مبالغ" else "مخفی کردن مبالغ"
                        } else {
                            if (amountsHidden) "Show amounts" else "Hide amounts"
                        },
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(palette.icon))
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // ── مبلغ اصلی ──
            Image(
                provider = ImageProvider(hero.bitmap),
                contentDescription = if (amountsHidden) {
                    if (isPersian) "$label: مخفی" else "$label: hidden"
                } else {
                    "$label: ${heroTexts.full} $heroUnit"
                },
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxWidth().height(hero.heightDp.dp)
            )

            // ── خلاصه‌ی درآمد/هزینه (فقط وقتی ویجت به اندازه‌ی کافی بلند است) ──
            if (isTall) {
                Spacer(modifier = GlanceModifier.height(CHIP_GAP_DP.dp))
                Row(modifier = GlanceModifier.fillMaxWidth().height(CHIP_HEIGHT_DP.dp)) {
                    val chipWidth = (innerWidth - CHIP_GAP_DP) / 2f
                    SummaryChip(
                        title = if (isPersian) "درآمد" else "Income",
                        value = income,
                        color = palette.primary,
                        unit = chipUnit,
                        textWidthDp = chipWidth - 2 * BorderWidth.value - 20f,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        hidden = amountsHidden,
                        formatter = formatter,
                        palette = palette
                    )
                    Spacer(modifier = GlanceModifier.width(CHIP_GAP_DP.dp))
                    SummaryChip(
                        title = if (isPersian) "هزینه" else "Expense",
                        value = expense,
                        color = palette.error,
                        unit = chipUnit,
                        textWidthDp = chipWidth - 2 * BorderWidth.value - 20f,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        hidden = amountsHidden,
                        formatter = formatter,
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SummaryChip(
    title: String,
    value: Long,
    color: Color,
    unit: String,
    textWidthDp: Float,
    currencyUnit: String,
    isPersian: Boolean,
    hidden: Boolean,
    formatter: NumberFormat,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val texts = amountTexts(value, currencyUnit, isPersian, formatter, hidden, signed = false)
    val rendered = remember(texts, unit, color, textWidthDp, isPersian) {
        WidgetTextRenderer.renderAmount(
            context = context,
            amountText = texts.full,
            compactText = texts.compact,
            unitText = unit,
            isPersian = isPersian,
            amountColor = color.toArgb(),
            unitColor = color.copy(alpha = 0.75f).toArgb(),
            maxWidthDp = textWidthDp,
            maxSp = 15f,
            minSp = 10f,
            unitScale = 0.75f,
            alignment = Layout.Alignment.ALIGN_CENTER
        )
    }

    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .cornerRadius(ChipRadius)
            .background(ColorProvider(palette.border))
            .padding(BorderWidth)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(ChipRadius - BorderWidth)
                .background(ColorProvider(palette.card))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(palette.label)
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(3.dp))
            Image(
                provider = ImageProvider(rendered.bitmap),
                contentDescription = if (hidden) title else "$title: ${texts.full} $unit",
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxWidth().height(rendered.heightDp.dp)
            )
        }
    }
}

private fun ColorProvider(color: Color) =
    androidx.glance.color.ColorProvider(day = color, night = color)

// ───────────────────────────── Palette ─────────────────────────────

/**
 * رنگ‌ها مستقیم از ColorScheme خود اپ (ui/theme/Color.kt) ساخته می‌شوند؛
 * یعنی اگر پالت اپ عوض شود ویجت هم خودکار عوض می‌شود.
 * پس‌زمینه‌ی ویجت شفاف نیست (روی والپیپر قرار می‌گیرد)، پس آلفاهای HomeScreen
 * (surfaceVariant ‏۰٫۴ و outline ‏۰٫۱۵) از قبل روی رنگ پایه ترکیب می‌شوند.
 */
private class WidgetPalette(
    val card: Color,
    val border: Color,
    val primary: Color,
    val error: Color,
    val label: Color,
    val icon: Color
) {
    companion object {
        fun from(scheme: ColorScheme): WidgetPalette {
            val card = scheme.surfaceVariant.copy(alpha = 0.4f).compositeOver(scheme.background)
            return WidgetPalette(
                card = card,
                border = scheme.outline.copy(alpha = 0.15f).compositeOver(card),
                primary = scheme.primary,
                error = scheme.error,
                label = scheme.onSurfaceVariant.copy(alpha = 0.8f).compositeOver(card),
                icon = scheme.onSurfaceVariant.copy(alpha = 0.85f).compositeOver(card)
            )
        }
    }
}

// ───────────────────────────── Amount formatting ─────────────────────────────

private data class AmountTexts(val full: String, val compact: String)

/**
 * مبلغ را برای نمایش آماده می‌کند (بدون واحد):
 *  - تبدیل تومان ⇄ ریال با MoneyContract
 *  - متن فشرده برای وقتی که عدد کامل جا نمی‌شود
 *  - حالت حریم خصوصی (ستاره)
 */
private fun amountTexts(
    storageValue: Long,
    currencyUnit: String,
    isPersian: Boolean,
    formatter: NumberFormat,
    hidden: Boolean,
    signed: Boolean
): AmountTexts {
    val display = MoneyContract.displayFromStorage(storageValue, currencyUnit)
    val sign = if (signed && storageValue < 0) "-" else ""
    val full = "$sign${formatter.format(abs(display))}"

    if (hidden) {
        val stars = "●".repeat(full.length.coerceIn(4, 9)).toList().joinToString("\u2009")
        return AmountTexts(stars, stars)
    }
    return AmountTexts(full, "$sign${compactAmount(display, isPersian)}")
}

private fun unitLabel(currencyUnit: String, isPersian: Boolean, short: Boolean): String = when {
    isPersian -> if (currencyUnit == "IRR") "ریال" else "تومان"
    short -> if (currencyUnit == "IRR") "R" else "T"
    else -> if (currencyUnit == "IRR") "Rial" else "Toman"
}

/** همان compactAmount در HomeScreen: ۱۲۵٬۰۰۰٬۰۰۰٬۰۰۰ ← «۱۲۵ میلیارد» / "125 B" */
private fun compactAmount(value: Long, isPersian: Boolean): String {
    val absValue = abs(value).toDouble()
    val formatter = NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
        .apply { maximumFractionDigits = 2 }
    val (divisor, suffix) = when {
        absValue >= 1e12 -> 1e12 to (if (isPersian) "تریلیون" else "Tn")
        absValue >= 1e9 -> 1e9 to (if (isPersian) "میلیارد" else "B")
        absValue >= 1e6 -> 1e6 to (if (isPersian) "میلیون" else "M")
        absValue >= 1e3 -> 1e3 to (if (isPersian) "هزار" else "K")
        else -> return formatter.format(absValue)
    }
    return "${formatter.format(absValue / divisor)} $suffix"
}

// ───────────────────────────── Privacy toggle ─────────────────────────────

class ToggleAmountsVisibilityAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        AmountPrivacyPreferences.toggle(context)   // ذخیره + همگام‌سازی با اپ + آپدیت ویجت
    }
}