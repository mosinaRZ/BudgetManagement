package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
    onNavigateToAddTransaction: () -> Unit = {},
    analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModel.Factory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    val uiState by analyticsViewModel.uiState.collectAsState()
    val selectedFilter by analyticsViewModel.selectedTimeFilter.collectAsState()
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState(initial = "IRT")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (!uiState.hasAnyTransactionInDb) {
            // حالت ۱: کلاً هیچ تراکنشی در برنامه ثبت نشده است (بدون دکمه‌های فیلتر)
            EmptyAnalyticsView(
                isPersian = isPersian,
                onAddTransactionClick = onNavigateToAddTransaction
            )
        } else {
            // حالت ۲: تراکنش وجود دارد (نمایش محتوا)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.statusBarsPadding().height(140.dp))

                SmartInsightCard(
                    isPersian = isPersian,
                    totalExpense = uiState.totalExpense,
                    topCategory = uiState.categoryExpenses.firstOrNull()?.categoryName ?: "",
                    currencyUnit = currencyUnit
                )

                Spacer(modifier = Modifier.height(16.dp))

                BalanceTrendChartCard(
                    isPersian = isPersian,
                    dataPoints = uiState.trendPoints
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExpenseCategoryPieChartCard(
                    isPersian = isPersian,
                    categories = uiState.categoryExpenses,
                    totalExpense = uiState.totalExpense,
                    currencyUnit = currencyUnit
                )

                Spacer(modifier = Modifier.height(16.dp))

                TopExpensesCard(
                    isPersian = isPersian,
                    topExpenses = uiState.topExpenses,
                    totalExpense = uiState.totalExpense,
                    averageExpense = uiState.averageExpense,
                    currencyUnit = currencyUnit
                )

                Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
            }

            // هدر بالای صفحه و فیلترها (فقط زمانی نشان داده می‌شود که داده در دیتابیس وجود دارد)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding().padding(top = 12.dp))

                AnalyticsTopBar(isPersian = isPersian)

                Spacer(modifier = Modifier.height(8.dp))

                TimeFilterSelector(
                    selectedFilter = selectedFilter,
                    isPersian = isPersian,
                    onFilterSelected = { analyticsViewModel.onTimeFilterChanged(it) }
                )
            }
        }
    }
}

// --- نمای صفحه خالی و CTA ثبت تراکنش ---
@Composable
private fun EmptyAnalyticsView(
    isPersian: Boolean,
    onAddTransactionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isPersian) "هیچ تراکنشی ثبت نشده است" else "No Transactions Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isPersian)
                "برای مشاهده نمودارها، آنالیز هوشمند و تفکیک هزینه‌ها، اولین درآمد یا هزینه خود را ثبت کنید."
            else
                "Add your first income or expense to unlock smart analytics and financial insights.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddTransactionClick,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPersian) "ثبت اولین تراکنش" else "Add First Transaction",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- هدر بالای صفحه ---
@Composable
private fun AnalyticsTopBar(isPersian: Boolean) {
    val centerShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), centerShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), centerShape)
                .clip(centerShape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPersian) "آنالیز و تحلیل مالی" else "Financial Analytics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- انتخاب‌گر زمان ---
@Composable
private fun TimeFilterSelector(
    selectedFilter: TimeFilter,
    isPersian: Boolean,
    onFilterSelected: (TimeFilter) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val filters = remember { TimeFilter.values().filter { it != TimeFilter.DAILY } }

        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val title = if (isPersian) filter.titleFa else filter.titleEn

            val backgroundAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                label = "TabBgAlpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha * 0.15f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                        shape = shape
                    )
                    .clickable { onFilterSelected(filter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- کارت تحلیل هوشمند ---
@Composable
private fun SmartInsightCard(
    isPersian: Boolean,
    totalExpense: Double,
    topCategory: String,
    currencyUnit: String
) {
    val cardShape = RoundedCornerShape(20.dp)
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val displayExpense = if (currencyUnit == "IRR") (totalExpense * 10).toLong() else totalExpense.toLong()
    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "Toman")
    val mappedCategory = StringMapper.getCategoryName(topCategory, isPersian)

    val insightTextFa = if (totalExpense > 0) {
        "مجموع هزینه‌های این دوره ${numberFormatter.format(displayExpense)} $currencyText است. بیشترین سهم مربوط به دسته‌بندی «$mappedCategory» می‌باشد."
    } else {
        "هیچ هزینه‌ای برای دوره زمانی انتخاب‌شده ثبت نشده است."
    }

    val insightTextEn = if (totalExpense > 0) {
        "Total expenses for this period are ${numberFormatter.format(displayExpense)} $currencyText. Top spending category is '$mappedCategory'."
    } else {
        "No expenses recorded for the selected period."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), cardShape)
            .clip(cardShape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "تحلیل رفتار مالی" else "Smart Financial Insight",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isPersian) insightTextFa else insightTextEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- کارت نمودار خطی ---
@Composable
private fun BalanceTrendChartCard(
    isPersian: Boolean,
    dataPoints: List<Float>
) {
    val cardShape = RoundedCornerShape(24.dp)
    val lineColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = if (isPersian) "روند موجودی کل" else "Balance Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPersian) "تغییرات موجودی بر حسب زمان" else "Balance over time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (dataPoints.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val maxVal = (dataPoints.maxOrNull() ?: 1f).let { if (it == 0f) 1f else it } * 1.15f
                val minVal = (dataPoints.minOrNull() ?: 0f) * 0.85f

                val distanceX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

                val strokePath = Path()
                val fillPath = Path()

                dataPoints.forEachIndexed { index, value ->
                    val x = index * distanceX
                    val normalizedY = if (maxVal != minVal) (value - minVal) / (maxVal - minVal) else 0.5f
                    val y = height - (normalizedY * height)

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (index - 1) * distanceX
                        val prevNormalizedY = if (maxVal != minVal) (dataPoints[index - 1] - minVal) / (maxVal - minVal) else 0.5f
                        val prevY = height - (prevNormalizedY * height)

                        val controlX1 = prevX + distanceX / 2f
                        val controlX2 = x - distanceX / 2f

                        strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    }

                    if (index == dataPoints.size - 1) {
                        fillPath.lineTo(x, height)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.0f)
                        )
                    )
                )

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                dataPoints.forEachIndexed { index, value ->
                    val x = index * distanceX
                    val normalizedY = if (maxVal != minVal) (value - minVal) / (maxVal - minVal) else 0.5f
                    val y = height - (normalizedY * height)

                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }
        }
    }
}

// --- کارت نمودار دایره‌ای دسته‌بندی هزینه‌ها ---
@Composable
private fun ExpenseCategoryPieChartCard(
    isPersian: Boolean,
    categories: List<CategoryExpenseModel>,
    totalExpense: Double,
    currencyUnit: String
) {
    val cardShape = RoundedCornerShape(24.dp)
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val displayTotal = if (currencyUnit == "IRR") (totalExpense * 10).toLong() else totalExpense.toLong()
    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "Toman")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = if (isPersian) "تفکیک هزینه‌ها" else "Expenses Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPersian) "سهم هر دسته‌بندی از کل خرج‌کرد" else "Expense share by category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (categories.isEmpty()) {
                Text(
                    text = if (isPersian) "داده‌ای برای نمایش در این دوره وجود ندارد" else "No expense data for this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 20.dp.toPx()

                            categories.forEach { category ->
                                val sweepAngle = if (totalExpense > 0) (category.totalAmount.toFloat() / totalExpense.toFloat()) * 360f else 0f

                                drawArc(
                                    color = category.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isPersian) "مجموع" else "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = numberFormatter.format(displayTotal),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currencyText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(category.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = StringMapper.getCategoryName(category.categoryName, isPersian),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${category.percentage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- کارت سنگین‌ترین هزینه‌های دوره (فیلتر بر اساس میانگین) ---
@Composable
private fun TopExpensesCard(
    isPersian: Boolean,
    topExpenses: List<TransactionEntity>,
    totalExpense: Double,
    averageExpense: Double,
    currencyUnit: String
) {
    val cardShape = RoundedCornerShape(24.dp)
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val displayAvg = if (currencyUnit == "IRR") (averageExpense * 10).toLong() else averageExpense.toLong()
    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = if (isPersian) "سنگین‌ترین هزینه‌های دوره" else "Top Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPersian)
                    "هزینه‌های بالاتر از میانگین (${numberFormatter.format(displayAvg)} $currencyText)"
                else
                    "Expenses above average (${numberFormatter.format(displayAvg)} $currencyText)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (topExpenses.isEmpty()) {
                Text(
                    text = if (isPersian) "هزینه‌ای بالاتر از میانگین در این دوره ثبت نشده است" else "No heavy expenses found above average",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topExpenses.forEach { item ->
                        val sharePercentage = if (totalExpense > 0) ((item.amount / totalExpense) * 100).toInt() else 0
                        val displayAmount = if (currencyUnit == "IRR") (item.amount * 10).toLong() else item.amount.toLong()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.title.ifEmpty { StringMapper.getCategoryName(item.category, isPersian) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = DateUtils.formatTimestamp(item.timestamp, isPersian),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${numberFormatter.format(displayAmount)} $currencyText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$sharePercentage% ${if (isPersian) "از کل" else "of total"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}