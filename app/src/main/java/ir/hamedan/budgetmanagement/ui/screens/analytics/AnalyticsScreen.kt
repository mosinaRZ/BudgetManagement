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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DirectionsCar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import java.text.NumberFormat
import java.util.Locale

// مدل داده دسته‌بندی هزینه‌ها
data class CategoryExpense(
    val titleFa: String,
    val titleEn: String,
    val amount: Long,
    val color: Color
)

// مدل داده بزرگ‌ترین تراکنش‌ها
data class TopExpenseItem(
    val titleFa: String,
    val titleEn: String,
    val categoryFa: String,
    val categoryEn: String,
    val amount: Long,
    val icon: ImageVector
)

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    var selectedFilter by remember { mutableStateOf(TimeFilter.MONTHLY) }

    // داده‌های نمونه تغییرات بالانس برای نمودار XY
    val balanceData = remember(selectedFilter) {
        when (selectedFilter) {
            TimeFilter.DAILY -> listOf(5f, 8f, 6f, 12f, 10f)
            TimeFilter.WEEKLY -> listOf(12f, 15f, 11f, 18f, 22f, 20f, 25f)
            TimeFilter.MONTHLY -> listOf(8f, 12f, 10f, 15f, 22f, 19f, 28f, 32f, 30f, 35f, 40f, 38f)
            TimeFilter.ALL -> listOf(10f, 18f, 25f, 22f, 35f, 48f)
        }
    }

    // داده‌های نمونه دسته‌بندی هزینه‌ها برای نمودار دایره‌ای
    val expenseCategories = remember {
        listOf(
            CategoryExpense("مسکن و اجاره", "Housing", 8500000, Color(0xFFB0E4CC)),
            CategoryExpense("خوراکی و سوپرمارکت", "Groceries", 4500000, Color(0xFF408A71)),
            CategoryExpense("تفریح و سرگرمی", "Entertainment", 2300000, Color(0xFFE2B93B)),
            CategoryExpense("حمل و نقل", "Transport", 1200000, Color(0xFF6C5CE7)),
            CategoryExpense("سایر موارد", "Others", 1800000, Color(0xFFA0AEC0))
        )
    }

    // داده‌های نمونه بزرگ‌ترین هزینه‌ها
    val topExpenses = remember {
        listOf(
            TopExpenseItem("اجاره خانه", "House Rent", "مسکن", "Housing", 8500000, Icons.Default.Home),
            TopExpenseItem("خرید هفتگی شهروند", "Weekly Grocery", "خوراکی", "Groceries", 2100000, Icons.Default.ShoppingBag),
            TopExpenseItem("سرویس دوره ماشین", "Car Maintenance", "حمل و نقل", "Transport", 1200000, Icons.Default.DirectionsCar)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        // 🚀 ۱. محتوای اسکرول‌پذیر (در لایه پایینی)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // فاصله کافی برای رفتن محتوا به زیر دو المان ثابت بالای صفحه
            Spacer(modifier = Modifier.statusBarsPadding().height(140.dp))

            // کارت تحلیل هوشمند
            SmartInsightCard(
                isPersian = isPersian,
                insightTextFa = "هزینه‌های این دوره شما ۸٪ نسبت به دوره قبل کاهش داشته است. بیشترین سهم مربوط به مسکن است.",
                insightTextEn = "Your expenses decreased by 8% compared to last period. Housing accounts for the largest share."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // نمودار خطی XY (روند تغییرات بالانس)
            BalanceTrendChartCard(
                isPersian = isPersian,
                dataPoints = balanceData
            )

            Spacer(modifier = Modifier.height(16.dp))

            // نمودار دایره‌ای دسته‌بندی هزینه‌ها
            ExpenseCategoryPieChartCard(
                isPersian = isPersian,
                categories = expenseCategories
            )

            Spacer(modifier = Modifier.height(16.dp))

            // بزرگ‌ترین هزینه‌های دوره
            TopExpensesCard(
                isPersian = isPersian,
                topExpenses = topExpenses,
                totalExpense = expenseCategories.sumOf { it.amount }
            )

            Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
        }

        // 🚀 ۲. لایه ثابت بالای صفحه (دو کپسول کاملاً مجزا و بدون هیچ‌گونه پس‌زمینه مشترک)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Spacer(modifier = Modifier.statusBarsPadding()
                .padding(top = 12.dp))

            // المان ۱: هدر صفحه
            AnalyticsTopBar(isPersian = isPersian)

            Spacer(modifier = Modifier.height(8.dp))

            // المان ۲: انتخاب‌گر زمان مجزا
            TimeFilterSelector(
                selectedFilter = selectedFilter,
                isPersian = isPersian,
                onFilterSelected = { selectedFilter = it }
            )
        }
    }
}

// --- هدر بالای صفحه ---
@Composable
private fun AnalyticsTopBar(
    isPersian: Boolean
) {
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

// --- انتخاب‌گر زمان (کاملاً مجزا) ---
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
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha * 0.15f)
                    )
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
    insightTextFa: String,
    insightTextEn: String
) {
    val cardShape = RoundedCornerShape(20.dp)

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

// --- کارت نمودار خطی XY ---
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
                val maxVal = (dataPoints.maxOrNull() ?: 1f) * 1.15f
                val minVal = (dataPoints.minOrNull() ?: 0f) * 0.85f

                val distanceX = width / (dataPoints.size - 1)

                val strokePath = Path()
                val fillPath = Path()

                dataPoints.forEachIndexed { index, value ->
                    val x = index * distanceX
                    val normalizedY = (value - minVal) / (maxVal - minVal)
                    val y = height - (normalizedY * height)

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (index - 1) * distanceX
                        val prevNormalizedY = (dataPoints[index - 1] - minVal) / (maxVal - minVal)
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
                    val normalizedY = (value - minVal) / (maxVal - minVal)
                    val y = height - (normalizedY * height)

                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

// --- کارت نمودار دایره‌ای دسته‌بندی هزینه‌ها ---
@Composable
private fun ExpenseCategoryPieChartCard(
    isPersian: Boolean,
    categories: List<CategoryExpense>
) {
    val cardShape = RoundedCornerShape(24.dp)
    val totalExpense = remember(categories) { categories.sumOf { it.amount } }
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

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
                            val sweepAngle = (category.amount.toFloat() / totalExpense) * 360f

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
                            text = "${formatter.format(totalExpense / 1000000)}M",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val percentage = (category.amount.toFloat() / totalExpense * 100).toInt()

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
                                    text = if (isPersian) category.titleFa else category.titleEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "$percentage%",
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

// --- کارت بزرگ‌ترین هزینه‌های دوره ---
@Composable
private fun TopExpensesCard(
    isPersian: Boolean,
    topExpenses: List<TopExpenseItem>,
    totalExpense: Long
) {
    val cardShape = RoundedCornerShape(24.dp)
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

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
                text = if (isPersian) "بزرگ‌ترین تراکنش‌های خروجی" else "Largest outgoing transactions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topExpenses.forEach { item ->
                    val sharePercentage = ((item.amount.toFloat() / totalExpense) * 100).toInt()

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
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isPersian) item.titleFa else item.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPersian) item.categoryFa else item.categoryEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatter.format(item.amount)} ${if (isPersian) "تومان" else "Toman"}",
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