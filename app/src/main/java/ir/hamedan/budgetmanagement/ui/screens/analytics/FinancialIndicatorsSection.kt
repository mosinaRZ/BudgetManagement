package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

// رنگ‌های معنایی شاخص‌ها؛ در تم روشن و تیره هر دو خوانا هستند.
private val IndicatorGreen = Color(0xFF22C55E)
private val IndicatorAmber = Color(0xFFF5B301)
private val IndicatorRed = Color(0xFFEF4444)

@Composable
private fun IndicatorTone.color(): Color = when (this) {
    IndicatorTone.GOOD -> IndicatorGreen
    IndicatorTone.WARNING -> IndicatorAmber
    IndicatorTone.BAD -> IndicatorRed
    IndicatorTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun LimitStatus.tone(): IndicatorTone = when (this) {
    LimitStatus.EXCEEDED, LimitStatus.DANGER -> IndicatorTone.BAD
    LimitStatus.WATCH -> IndicatorTone.WARNING
    LimitStatus.SAFE, LimitStatus.MET -> IndicatorTone.GOOD
}

// ───────────────────────── قالب‌بندی ─────────────────────────

private class IndicatorFormat(val isPersian: Boolean, val currencyUnit: String) {
    private val locale: Locale = if (isPersian) Locale("fa", "IR") else Locale.US
    private val grouped: NumberFormat = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
    private val plain1: NumberFormat = NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 1
    }
    private val plain0: NumberFormat = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }

    val unit: String = if (isPersian) {
        if (currencyUnit == "IRR") "ریال" else "تومان"
    } else {
        if (currencyUnit == "IRR") "Rial" else "Toman"
    }

    fun int(v: Int): String = plain0.format(v.toLong())

    fun amount(toman: Long): String =
        grouped.format(MoneyContract.displayFromStorage(toman, currencyUnit)) + " " + unit

    /** مبلغ کوتاه‌شده برای جاهای تنگ: ۱۲٫۵ میلیون / 12.5M */
    fun compact(toman: Long): String {
        val v = abs(MoneyContract.displayFromStorage(toman, currencyUnit).toDouble())
        val (div, suffix) = if (isPersian) when {
            v >= 1e9 -> 1e9 to " میلیارد"
            v >= 1e6 -> 1e6 to " میلیون"
            v >= 1e3 -> 1e3 to " هزار"
            else -> 1.0 to ""
        } else when {
            v >= 1e9 -> 1e9 to "B"
            v >= 1e6 -> 1e6 to "M"
            v >= 1e3 -> 1e3 to "K"
            else -> 1.0 to ""
        }
        return (if (toman < 0) "-" else "") + plain1.format(v / div) + suffix
    }

    fun percent(v: Double, decimals: Int = 0): String {
        val nf = NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = decimals
            minimumFractionDigits = 0
        }
        return nf.format(v) + if (isPersian) "٪" else "%"
    }
}

private fun periodNoun(kind: PeriodKind, isPersian: Boolean): String = when (kind) {
    PeriodKind.DAY -> if (isPersian) "روز" else "day"
    PeriodKind.WEEK -> if (isPersian) "هفته" else "week"
    PeriodKind.MONTH -> if (isPersian) "ماه" else "month"
    PeriodKind.YEAR -> if (isPersian) "سال" else "year"
    PeriodKind.CUSTOM -> if (isPersian) "بازه" else "period"
    PeriodKind.ALL -> if (isPersian) "بازه" else "period"
}

private fun periodLabel(w: PeriodWindow, f: IndicatorFormat): String {
    fun dayMonth(d: LocalDate): String =
        if (f.isPersian) {
            val (_, m, day) = DateUtils.toJalali(d)
            "${f.int(day)} ${DateUtils.PERSIAN_MONTH_NAMES[m - 1]}"
        } else "${DateUtils.ENGLISH_MONTH_NAMES[d.monthValue - 1]} ${d.dayOfMonth}"

    return when (w.kind) {
        PeriodKind.ALL -> if (f.isPersian) "از ابتدا تا امروز" else "All time"
        PeriodKind.DAY -> dayMonth(w.start)
        PeriodKind.WEEK, PeriodKind.CUSTOM -> "${dayMonth(w.start)} – ${dayMonth(w.end)}"
        PeriodKind.MONTH ->
            if (f.isPersian) {
                val (y, m, _) = DateUtils.toJalali(w.start)
                "${DateUtils.PERSIAN_MONTH_NAMES[m - 1]} ${f.int(y)}"
            } else "${DateUtils.ENGLISH_MONTH_NAMES[w.start.monthValue - 1]} ${w.start.year}"
        PeriodKind.YEAR ->
            if (f.isPersian) f.int(DateUtils.toJalali(w.start).first) else w.start.year.toString()
    }
}

// ───────────────────────── قطعات مشترک ─────────────────────────

@Composable
private fun IndicatorCard(
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val base = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        (accent ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.10f),
                        base.copy(alpha = 0.42f)
                    )
                ),
                shape
            )
            .border(1.dp, (accent ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.20f), shape)
            .clip(shape)
            .padding(20.dp),
        content = content
    )
}

@Composable
private fun CardHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(34.dp).background(tint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** نوار پیشرفت؛ [marker] (اختیاری) خط عمودی «زمان سپری‌شده» است. */
@Composable
private fun ProgressTrack(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    marker: Float? = null
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "IndicatorProgress"
    )
    Box(
        modifier = modifier.fillMaxWidth().height(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)))
            )
        }
        if (marker != null) {
            Box(
                modifier = Modifier.fillMaxWidth(marker.coerceIn(0.02f, 1f)).fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun ExpandToggle(expanded: Boolean, hiddenCount: Int, isPersian: Boolean, f: IndicatorFormat, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "ExpandRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when {
                expanded -> if (isPersian) "نمایش کمتر" else "Show less"
                isPersian -> "نمایش ${f.int(hiddenCount)} مورد دیگر"
                else -> "Show $hiddenCount more"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).rotate(rotation)
        )
    }
}

// ───────────────────────── بخش اصلی ─────────────────────────

@Composable
fun FinancialIndicatorsSection(
    isPersian: Boolean,
    currencyUnit: String,
    indicators: FinancialIndicators?
) {
    if (indicators == null) return
    val f = remember(isPersian, currencyUnit) { IndicatorFormat(isPersian, currencyUnit) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        CollapsibleHeader(
            isPersian = isPersian,
            period = periodLabel(indicators.window, f),
            savings = indicators.savings,
            f = f,
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(350)),
            exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SavingsRateCard(isPersian, f, indicators)
                BurnRateCard(isPersian, f, indicators)
                PeriodChangeCard(isPersian, f, indicators)
                BudgetAdherenceCard(isPersian, f, indicators.budget)
                SavingGoalsCard(isPersian, f, indicators.goals)
            }
        }
    }
}

/** فیلد جمع‌شونده: با کلیک روی کل فیلد (یا پیکان) باز/بسته می‌شود و پیکان ۱۸۰ درجه می‌چرخد. */
@Composable
private fun CollapsibleHeader(
    isPersian: Boolean,
    period: String,
    savings: SavingsRateIndicator,
    f: IndicatorFormat,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "IndicatorsArrowRotation"
    )
    val shape = RoundedCornerShape(20.dp)
    val primary = MaterialTheme.colorScheme.primary
    val rate = savings.ratePercent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.10f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                ),
                shape
            )
            .border(1.dp, primary.copy(alpha = 0.20f), shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPersian) "شاخص‌های مالی" else "Financial Indicators",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = period,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // خلاصه‌ی نرخ پس‌انداز تا در حالت بسته هم یک نگاه کافی باشد.
        if (rate != null) {
            StatusChip(
                text = (if (isPersian) "پس‌انداز " else "Savings ") + f.percent(rate, 1),
                color = savings.tone.color()
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) (if (isPersian) "بستن" else "Collapse") else (if (isPersian) "باز کردن" else "Expand"),
            tint = primary,
            modifier = Modifier.size(28.dp).rotate(rotation)
        )
    }
}

// ───────────────────────── ۱) نرخ پس‌انداز ─────────────────────────

private const val GAUGE_MAX_PERCENT = 40.0

@Composable
private fun SavingsRateCard(isPersian: Boolean, f: IndicatorFormat, data: FinancialIndicators) {
    val s = data.savings
    val tone = s.tone.color()
    val rate = s.ratePercent

    IndicatorCard(accent = tone) {
        CardHeader(
            icon = Icons.Default.Savings,
            title = if (isPersian) "نرخ پس‌انداز" else "Savings Rate",
            subtitle = if (isPersian) "(درآمد − هزینه) ÷ درآمد" else "(Income − Expense) ÷ Income",
            tint = tone,
            trailing = {
                StatusChip(
                    text = when (s.tone) {
                        IndicatorTone.GOOD -> if (isPersian) "عالی" else "Excellent"
                        IndicatorTone.WARNING -> if (isPersian) "قابل بهبود" else "Fair"
                        IndicatorTone.BAD -> if (isPersian) "ضعیف" else "Weak"
                        IndicatorTone.NEUTRAL -> if (isPersian) "نامشخص" else "N/A"
                    },
                    color = tone
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SavingsGauge(rate = rate, tone = tone, f = f)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rate == null) {
                    Text(
                        text = if (isPersian) "در این بازه درآمدی ثبت نشده است؛ برای محاسبه‌ی نرخ پس‌انداز، درآمد ثبت کن."
                        else "No income recorded in this period. Add income to calculate your savings rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                } else {
                    s.deltaPoints?.let { d ->
                        val dTone = if (abs(d) < 0.1) IndicatorTone.NEUTRAL
                        else if (d > 0) IndicatorTone.GOOD else IndicatorTone.BAD
                        Text(
                            text = (if (d > 0) "+" else "") + f.percent(d, 1) +
                                    (if (isPersian) " نسبت به دوره قبل" else " vs previous period"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = dTone.color()
                        )
                    }
                    val target = if (rate < FinancialIndicatorCalculator.SAVINGS_WARNING_FROM)
                        FinancialIndicatorCalculator.SAVINGS_WARNING_FROM
                    else if (rate <= FinancialIndicatorCalculator.SAVINGS_GOOD_ABOVE)
                        FinancialIndicatorCalculator.SAVINGS_GOOD_ABOVE else null
                    if (target != null && s.income > 0L) {
                        // هزینه‌ای که باید کم شود تا نرخ به مرز بعدی برسد.
                        val cut = (s.expense - s.income * (1.0 - target / 100.0)).toLong().coerceAtLeast(0L)
                        if (cut > 0L) {
                            Text(
                                text = if (isPersian)
                                    "برای رسیدن به ${f.percent(target)} حدود ${f.amount(cut)} هزینه را کم کن."
                                else "Cut about ${f.amount(cut)} in expenses to reach ${f.percent(target)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    } else if (target == null) {
                        Text(
                            text = if (isPersian) "بالای ${f.percent(FinancialIndicatorCalculator.SAVINGS_GOOD_ABOVE)}؛ در وضعیت عالی هستی."
                            else "Above ${f.percent(FinancialIndicatorCalculator.SAVINGS_GOOD_ABOVE)}. You're in great shape.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
                ThresholdLegend(isPersian, f)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(if (isPersian) "درآمد" else "Income", f.compact(s.income), IndicatorGreen, Modifier.weight(1f))
            MiniStat(if (isPersian) "هزینه" else "Expense", f.compact(s.expense), IndicatorRed, Modifier.weight(1f))
            MiniStat(
                if (isPersian) "خالص" else "Net", f.compact(s.net),
                if (s.net >= 0) IndicatorGreen else IndicatorRed, Modifier.weight(1f)
            )
        }

        if (data.excludesSavingGoalTransfers) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Default.Info, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isPersian) "واریز و برداشت قلک در این شاخص‌ها هزینه یا درآمد حساب نمی‌شود."
                    else "Goal deposits and withdrawals are not counted as expenses or income here.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SavingsGauge(rate: Double?, tone: Color, f: IndicatorFormat) {
    val fraction = ((rate ?: 0.0).coerceIn(0.0, GAUGE_MAX_PERCENT) / GAUGE_MAX_PERCENT).toFloat()
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "SavingsGauge"
    )
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            val start = 135f
            val total = 270f

            // سه ناحیه‌ی رنگی کمرنگ: قرمز ۰–۱۰، زرد ۱۰–۲۰، سبز ۲۰ به بالا.
            val redSweep = total * (10f / GAUGE_MAX_PERCENT.toFloat())
            val amberSweep = total * (10f / GAUGE_MAX_PERCENT.toFloat())
            val greenSweep = total - redSweep - amberSweep
            var cursor = start
            listOf(
                IndicatorRed to redSweep,
                IndicatorAmber to amberSweep,
                IndicatorGreen to greenSweep
            ).forEach { (c, sweep) ->
                drawArc(
                    color = c.copy(alpha = 0.22f),
                    startAngle = cursor,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                cursor += sweep
            }

            if (animated > 0.004f) {
                drawArc(
                    color = tone,
                    startAngle = start,
                    sweepAngle = total * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (rate == null) "—" else f.percent(rate, 1),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (rate == null) onSurface else tone,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ThresholdLegend(isPersian: Boolean, f: IndicatorFormat) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(IndicatorRed, if (isPersian) "زیر ${f.percent(10.0)}" else "< ${f.percent(10.0)}")
        LegendDot(IndicatorAmber, "${f.int(10)}–${f.percent(20.0)}")
        LegendDot(IndicatorGreen, if (isPersian) "بالای ${f.percent(20.0)}" else "> ${f.percent(20.0)}")
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ───────────────────────── ۲) نرخ مصرف روزانه و پیش‌بینی ─────────────────────────

@Composable
private fun BurnRateCard(isPersian: Boolean, f: IndicatorFormat, data: FinancialIndicators) {
    val b = data.burn
    val noun = periodNoun(data.window.kind, isPersian)
    val primary = MaterialTheme.colorScheme.primary

    IndicatorCard {
        CardHeader(
            icon = Icons.Default.Speed,
            title = if (isPersian) "نرخ مصرف روزانه" else "Daily Burn Rate",
            subtitle = if (isPersian) "هزینه ÷ روزهای سپری‌شده" else "Expenses ÷ days elapsed"
        )

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = f.amount(b.dailyBurn),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = if (isPersian) "در روز" else "per day",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        val projected = b.projectedExpense
        if (projected != null) {
            Spacer(Modifier.height(14.dp))
            ProgressTrack(fraction = b.elapsedFraction, color = primary)
            Text(
                text = if (isPersian) "روز ${f.int(b.elapsedDays)} از ${f.int(b.totalDays)}"
                else "Day ${b.elapsedDays} of ${b.totalDays}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary.copy(alpha = 0.08f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isPersian) "با این روند، آخر $noun خرج کرده‌ای" else "At this pace, you'll spend by the end of the $noun",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = f.amount(projected),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
                b.projectedVsPreviousPercent?.let { p ->
                    val up = p > 0
                    val tone = if (abs(p) < 1.0) IndicatorTone.NEUTRAL else if (up) IndicatorTone.BAD else IndicatorTone.GOOD
                    Text(
                        text = if (isPersian)
                            "${f.percent(abs(p))} ${if (up) "بیشتر" else "کمتر"} از ${noun} قبل"
                        else "${f.percent(abs(p))} ${if (up) "higher" else "lower"} than last $noun",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tone.color()
                    )
                }
                if (b.lowConfidence) {
                    Text(
                        text = if (isPersian) "در روزهای اول، پیش‌بینی کم‌دقت است." else "Early in the period, the forecast is less reliable.",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndicatorAmber
                    )
                }
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    data.window.kind == PeriodKind.ALL ->
                        if (isPersian) "میانگین از اولین تراکنش تا امروز (${f.int(b.totalDays)} روز)"
                        else "Average from your first transaction until today (${b.totalDays} days)"
                    b.elapsedDays == 0 ->
                        if (isPersian) "این بازه هنوز شروع نشده است." else "This period hasn't started yet."
                    else ->
                        if (isPersian) "میانگین در ${f.int(b.totalDays)} روز این بازه؛ پیش‌بینی برای بازه‌های در جریان نمایش داده می‌شود."
                        else "Average over ${b.totalDays} days. Forecasts appear for periods still in progress."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// ───────────────────────── ۳) تغییر نسبت به دوره قبل ─────────────────────────

@Composable
private fun PeriodChangeCard(isPersian: Boolean, f: IndicatorFormat, data: FinancialIndicators) {
    val c = data.change
    IndicatorCard {
        CardHeader(
            icon = Icons.Default.SwapVert,
            title = if (isPersian) "تغییر نسبت به دوره قبل" else "Change vs Previous Period",
            subtitle = when {
                c == null -> null
                c.isLikeForLike -> {
                    val days = (ChronoUnit.DAYS.between(c.previousStart, c.previousEnd) + 1).toInt()
                    if (isPersian) "همان ${f.int(days)} روز اول ${periodNoun(data.window.kind, true)} قبل"
                    else "Same first $days days of the previous ${periodNoun(data.window.kind, false)}"
                }
                else -> if (isPersian) "${periodNoun(data.window.kind, true)} قبل" else "Previous ${periodNoun(data.window.kind, false)}"
            }
        )
        Spacer(Modifier.height(14.dp))

        when {
            c == null -> Text(
                text = if (isPersian) "برای مقایسه، فیلتر روزانه، هفتگی یا ماهانه (یا یک بازه‌ی دلخواه) را انتخاب کن."
                else "Choose a daily, weekly or monthly filter (or a custom range) to compare with the previous period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            !c.hasPreviousData -> Text(
                text = if (isPersian) "در دوره‌ی قبل تراکنشی ثبت نشده؛ مقایسه معنا ندارد."
                else "No transactions in the previous period, so there is nothing to compare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChangeTile(if (isPersian) "درآمد" else "Income", c.income, f, isPersian, Modifier.weight(1f))
                ChangeTile(if (isPersian) "هزینه" else "Expense", c.expense, f, isPersian, Modifier.weight(1f))
                ChangeTile(if (isPersian) "خالص" else "Net", c.net, f, isPersian, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChangeTile(label: String, m: ChangeMetric, f: IndicatorFormat, isPersian: Boolean, modifier: Modifier) {
    val color = m.tone.color()
    val percent = m.percent
    val icon = when {
        percent == null || abs(percent) < 1.0 -> Icons.Default.Remove
        percent > 0 -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }
    val text = when {
        percent == null -> when {
            m.current != 0L -> if (isPersian) "جدید" else "New"
            else -> "—"
        }
        abs(percent) > 999 -> if (isPersian) "بیش از ${f.percent(999.0)}" else ">${f.percent(999.0)}"
        else -> f.percent(abs(percent), if (abs(percent) < 10) 1 else 0)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.09f))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            text = f.compact(m.current),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
        Text(
            text = (if (isPersian) "قبل: " else "Prev: ") + f.compact(m.previous),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ───────────────────────── ۴) پایبندی به سقف بودجه ─────────────────────────

private const val COLLAPSED_LIMITS = 4

@Composable
private fun BudgetAdherenceCard(isPersian: Boolean, f: IndicatorFormat, budget: BudgetAdherenceIndicator?) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    IndicatorCard {
        CardHeader(
            icon = Icons.Default.AccountBalanceWallet,
            title = if (isPersian) "پایبندی به سقف بودجه" else "Budget Adherence",
            subtitle = if (isPersian) "مصرف‌شده در برابر زمان‌سپری‌شده" else "Spent vs. time elapsed"
        )
        Spacer(Modifier.height(14.dp))

        if (budget == null) {
            Text(
                text = if (isPersian) "برای این بازه سقف بودجه‌ی فعالی وجود ندارد. با تعریف سقف برای دسته‌ها، اینجا وضعیتشان را می‌بینی."
                else "No active budget limits for this period. Set limits on categories to track them here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            return@IndicatorCard
        }

        val tone = when {
            budget.percent >= 100.0 -> IndicatorTone.GOOD
            budget.percent >= 60.0 -> IndicatorTone.WARNING
            else -> IndicatorTone.BAD
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = f.percent(budget.percent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tone.color()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "${f.int(budget.respected)} از ${f.int(budget.total)} سقف رعایت شده"
                    else "${budget.respected} of ${budget.total} limits respected",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (budget.atRisk > 0) {
                    Text(
                        text = if (isPersian) "${f.int(budget.atRisk)} سقف در معرض خطر" else "${budget.atRisk} at risk",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IndicatorAmber
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val visible = if (expanded) budget.items else budget.items.take(COLLAPSED_LIMITS)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            visible.forEach { LimitRow(it, f, isPersian) }
        }

        if (budget.items.size > COLLAPSED_LIMITS) {
            Spacer(Modifier.height(4.dp))
            ExpandToggle(expanded, budget.items.size - COLLAPSED_LIMITS, isPersian, f) { expanded = !expanded }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isPersian) "خط عمودی روی نوار، زمان سپری‌شده‌ی سقف است."
            else "The vertical line on each bar marks the elapsed time.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LimitRow(p: LimitProgress, f: IndicatorFormat, isPersian: Boolean) {
    val tone = p.status.tone()
    val color = tone.color()
    val name = StringMapper.getCategoryName(p.categoryTitle, isPersian)
    val statusText = when (p.status) {
        LimitStatus.EXCEEDED -> if (isPersian) "رد شده" else "Exceeded"
        LimitStatus.DANGER -> if (isPersian) "خطر" else "At risk"
        LimitStatus.WATCH -> if (isPersian) "هشدار" else "Watch"
        LimitStatus.SAFE -> if (isPersian) "امن" else "On track"
        LimitStatus.MET -> if (isPersian) "رعایت شد" else "Met"
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(p.emoji, style = MaterialTheme.typography.titleSmall)
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            StatusChip(statusText, color)
        }
        ProgressTrack(
            fraction = p.usedFraction.toFloat(),
            color = color,
            marker = if (p.isFinished) null else p.elapsedFraction.toFloat()
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isPersian)
                    "مصرف ${f.percent(p.usedFraction * 100)}" + if (p.isFinished) "" else " · زمان ${f.percent(p.elapsedFraction * 100)}"
                else "Used ${f.percent(p.usedFraction * 100)}" + if (p.isFinished) "" else " · Time ${f.percent(p.elapsedFraction * 100)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${f.compact(p.spent)} / ${f.compact(p.maxLimit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ───────────────────────── ۵) پیشرفت اهداف پس‌انداز ─────────────────────────

private const val COLLAPSED_GOALS = 3

@Composable
private fun SavingGoalsCard(isPersian: Boolean, f: IndicatorFormat, goals: SavingGoalsIndicator?) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    IndicatorCard {
        CardHeader(
            icon = Icons.Default.Flag,
            title = if (isPersian) "پیشرفت اهداف پس‌انداز" else "Savings Goals Progress",
            subtitle = if (isPersian) "بر اساس واریز ماهانه‌ی هر هدف" else "Based on each goal's monthly deposit"
        )
        Spacer(Modifier.height(14.dp))

        if (goals == null) {
            Text(
                text = if (isPersian) "هنوز هدف پس‌اندازی تعریف نکرده‌ای. با ساخت یک هدف، پیشرفت و زمان رسیدنش را اینجا می‌بینی."
                else "No savings goals yet. Create one to track its progress and ETA here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            return@IndicatorCard
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = f.percent(goals.overallPercent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "پیشرفت کل اهداف" else "Overall progress",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPersian) "${f.int(goals.completedCount)} از ${f.int(goals.goals.size)} هدف کامل شده"
                    else "${goals.completedCount} of ${goals.goals.size} goals completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        val visible = if (expanded) goals.goals else goals.goals.take(COLLAPSED_GOALS)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            visible.forEach { GoalRow(it, f, isPersian) }
        }

        if (goals.goals.size > COLLAPSED_GOALS) {
            Spacer(Modifier.height(4.dp))
            ExpandToggle(expanded, goals.goals.size - COLLAPSED_GOALS, isPersian, f) { expanded = !expanded }
        }
    }
}

private fun durationText(months: Int, f: IndicatorFormat): String {
    val years = months / 12
    val rest = months % 12
    return if (f.isPersian) when {
        years == 0 -> "${f.int(months)} ماه"
        rest == 0 -> "${f.int(years)} سال"
        else -> "${f.int(years)} سال و ${f.int(rest)} ماه"
    } else when {
        years == 0 -> "$months mo"
        rest == 0 -> "$years yr"
        else -> "$years yr $rest mo"
    }
}

@Composable
private fun GoalRow(g: GoalProgress, f: IndicatorFormat, isPersian: Boolean) {
    val color = if (g.completed) IndicatorGreen else MaterialTheme.colorScheme.primary

    val eta: String = when {
        g.completed -> if (isPersian) "🎉 به هدف رسیده‌ای" else "🎉 Goal reached"
        g.monthlyAmount <= 0L -> {
            if (isPersian) "واریز ماهانه تعریف نشده؛ زمان رسیدن قابل برآورد نیست"
            else "No monthly deposit set, so no ETA"
        }
        g.etaMonths == null -> {
            if (isPersian) "بیش از ۱۰۰ سال تا رسیدن به هدف" else "More than 100 years to go"
        }
        else -> {
            val date: String? = if (isPersian) {
                g.etaJalali?.let { (y, m) -> "${DateUtils.PERSIAN_MONTH_NAMES[m - 1]} ${f.int(y)}" }
            } else {
                g.etaGregorian?.let { (y, m) -> "${DateUtils.ENGLISH_MONTH_NAMES[m - 1]} $y" }
            }
            val dur = durationText(g.etaMonths, f)
            val suffix = if (date != null) " · $date" else ""
            if (isPersian) "حدود $dur دیگر$suffix" else "In about $dur$suffix"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(g.icon, style = MaterialTheme.typography.titleSmall)
            Text(
                text = g.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = f.percent(g.percent),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        ProgressTrack(fraction = (g.percent / 100.0).toFloat(), color = color)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = eta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${f.compact(g.currentAmount)} / ${f.compact(g.targetAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}