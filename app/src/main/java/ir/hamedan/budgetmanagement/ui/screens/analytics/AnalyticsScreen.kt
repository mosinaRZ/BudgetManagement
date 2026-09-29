package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.BarChartEntry
import ir.hamedan.budgetmanagement.ui.components.ColumnBarChartCard
import ir.hamedan.budgetmanagement.ui.components.appSkeletonShimmer
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onAddScreenClick: () -> Unit = {},
    analyticsViewModel: AnalyticsViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    LaunchedEffect(isPersian) {
        analyticsViewModel.updateLocale(isPersian)
    }

    val uiState by analyticsViewModel.uiState.collectAsState()
    val selectedFilter by analyticsViewModel.selectedTimeFilter.collectAsState()
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState(initial = "IRT")

    val snackbarHostState = remember { SnackbarHostState() }
    var isFirstFilterEmission by remember { mutableStateOf(true) }
    var showAnalyticsInteractionHint by remember { mutableStateOf(true) }
    var showAnalyticsFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedFilter) {
        if (isFirstFilterEmission) {
            isFirstFilterEmission = false
        } else {
            val filterTitle = if (isPersian) selectedFilter.titleFa else selectedFilter.titleEn
            snackbarHostState.showSnackbar(
                message = if (isPersian) "فیلتر زمانی به «$filterTitle» تغییر کرد" else "Filter changed to $filterTitle"
            )
        }
    }

    // استفاده از Box به جای Scaffold برای جلوگیری از تداخل لایه‌ها
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // پس‌زمینه کاستوم شما
        AuroraBackground()

        if (uiState.isLoading) {
            AnalyticsSkeletonScreen()
        } else if (!uiState.hasAnyTransactionInDb) {
            EmptyAnalyticsView(
                isPersian = isPersian,
                onAddTransactionClick = onAddScreenClick
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // فاصله برای تاپ‌بار شناور
                Spacer(modifier = Modifier.statusBarsPadding().height(140.dp))

                SmartInsightCard(
                    isPersian = isPersian,
                    insight = uiState.smartInsight
                )

                Spacer(modifier = Modifier.height(16.dp))

                BalanceTrendChartCard(
                    isPersian = isPersian,
                    dataPoints = uiState.trendPoints,
                    hasEnoughData = uiState.trendHasEnoughData,
                    selectedFilter = selectedFilter,
                    currentTimeIndex = uiState.trendCurrentIndex,
                    isCustomRange = uiState.isCustomRange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // چارت آپدیت شده همراه با قابلیت انتخاب درآمد/هزینه
                ExpenseTimeBarChartCard(
                    isPersian = isPersian,
                    timeExpenses = uiState.timeExpenses,
                    currentIndex = uiState.currentTimeIndex,
                    selectedFilter = selectedFilter,
                    currencyUnit = currencyUnit,
                    isCustomRange = uiState.isCustomRange,
                    isIncome = uiState.isIncomeChartSelected,
                    onIncomeChange = { analyticsViewModel.setIncomeChartSelected(it) },
                    selectedIndex = uiState.selectedTimeBucketIndex,
                    onEntryClick = { analyticsViewModel.onTimeBucketSelected(it) }
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
                    currencyUnit = currencyUnit
                )

                Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
            }

            AnalyticsInteractionHint(
                isPersian = isPersian,
                visible = showAnalyticsInteractionHint && !uiState.isLoading && uiState.hasAnyTransactionInDb,
                onDismiss = { showAnalyticsInteractionHint = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 150.dp)
            )

            // تاپ‌بار شناور بدون نیاز به Scaffold
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding().padding(top = 12.dp))

                AnalyticsTopBar(
                    isPersian = isPersian,
                    isFilterActive = uiState.isCustomRange,
                    onFilterClick = { showAnalyticsFilterSheet = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TimeFilterSelector(
                    selectedFilter = selectedFilter,
                    isPersian = isPersian,
                    isCustomRange = uiState.isCustomRange,
                    onFilterSelected = { analyticsViewModel.onTimeFilterChanged(it) }
                )
            }
        }

        if (showAnalyticsFilterSheet) {
            AnalyticsFilterBottomSheet(
                isPersian = isPersian,
                isCustomRange = uiState.isCustomRange,
                customStartMillis = uiState.customStartMillis,
                customEndMillis = uiState.customEndMillis,
                compareWithPrevious = uiState.compareWithPrevious,
                onDismiss = { showAnalyticsFilterSheet = false },
                onCustomRangeSelected = { start, end ->
                    analyticsViewModel.setCustomDateRange(start, end)
                    showAnalyticsFilterSheet = false
                },
                onClearCustomRange = {
                    analyticsViewModel.clearCustomDateRange()
                    showAnalyticsFilterSheet = false
                },
                onCompareChanged = { analyticsViewModel.setCompareWithPrevious(it) }
            )
        }

        // نمایش اسنک‌بارها روی Box
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
        )
    }
}

@Composable
fun ChartTypeSwitch(
    isIncome: Boolean,
    isPersian: Boolean,
    onIncomeChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), shape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartTypeTab(
            text = if (isPersian) "هزینه" else "Expense",
            isSelected = !isIncome,
            onClick = { onIncomeChange(false) }
        )
        ChartTypeTab(
            text = if (isPersian) "درآمد" else "Income",
            isSelected = isIncome,
            onClick = { onIncomeChange(true) }
        )
    }
}

@Composable
fun ChartTypeTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        label = "SwitchBgAlpha"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha * 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ExpenseTimeBarChartCard(
    isPersian: Boolean,
    timeExpenses: List<TimeExpenseModel>,
    currentIndex: Int,
    selectedFilter: TimeFilter,
    currencyUnit: String,
    isCustomRange: Boolean = false,
    isIncome: Boolean,
    onIncomeChange: (Boolean) -> Unit,
    selectedIndex: Int? = null,
    onEntryClick: ((Int) -> Unit)? = null
) {
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val entries = timeExpenses.mapIndexed { index, model ->
        BarChartEntry(
            label = if (isPersian) model.labelFa else model.labelEn,
            value = model.totalAmount.toFloat(),
            isCurrent = model.isCurrent || index == currentIndex
        )
    }

    val typeTextFa = if (isIncome) "درآمدها" else "هزینه‌ها"
    val typeTextEn = if (isIncome) "income" else "expense"

    val title = if (isPersian) "مقایسه زمانی $typeTextFa" else "${if (isIncome) "Income" else "Expense"} Time Comparison"

    val subtitle = if (isCustomRange) {
        if (isPersian) "توزیع $typeTextFa در بازه انتخابی" else "$typeTextEn breakdown for the selected range"
    } else when (selectedFilter) {
        TimeFilter.DAILY -> if (isPersian) "توزیع $typeTextFa به تفکیک روزهای ماه جاری" else "Daily $typeTextEn breakdown for current month"
        TimeFilter.WEEKLY -> if (isPersian)
            "توزیع $typeTextFa در هفته‌های ماه جاری"
        else
            "Weekly $typeTextEn breakdown for current month"
        TimeFilter.MONTHLY -> if (isPersian) "توزیع $typeTextFa در ماه‌های سال جاری" else "Monthly $typeTextEn breakdown for current year"
        TimeFilter.ALL -> if (isPersian) "توزیع $typeTextFa به تفکیک سال" else "Yearly $typeTextEn breakdown"
    }

    val emptyText = if (isPersian) "داده‌ای برای نمایش در این دوره وجود ندارد" else "No $typeTextEn data for this period"

    ColumnBarChartCard(
        title = title,
        subtitle = subtitle,
        entries = entries,
        emptyStateText = emptyText,
        averageLabel = if (isPersian) "میانگین" else "Avg",
        yAxisLabel = if (isPersian) "مبلغ ($currencyUnit)" else "Amount ($currencyUnit)",
        xAxisLabel = if (isPersian) "زمان" else "Time",
        scrollToIndex = currentIndex,
        selectedIndex = selectedIndex,
        onEntryClick = onEntryClick,
        valueFormatter = { value ->
            val displayValue = if (currencyUnit == "IRR") (value * 10).toLong() else value.toLong()
            val raw = numberFormatter.format(displayValue)
            // برای ارقام بزرگ، فونت/عرض خود کارت امکان نمایش کامل مقدار را فراهم می‌کند؛
            // در اینجا مقدار هر ستون همچنان کامل و بدون خلاصه‌سازی نگه داشته می‌شود.
            raw
        },
        actionContent = {
            ChartTypeSwitch(
                isIncome = isIncome,
                isPersian = isPersian,
                onIncomeChange = onIncomeChange
            )
        }
    )
}

@Composable
private fun AnalyticsSkeletonScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().height(140.dp))

            // Smart Insight Card Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .appSkeletonShimmer()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Trend Chart Card Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .appSkeletonShimmer()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expense Time Bar Chart Card Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .appSkeletonShimmer()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expense Category Pie Chart Card Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .appSkeletonShimmer()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Top Expenses Card Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .appSkeletonShimmer()
            )

            Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
        }

        // Top Bar & Time Filter Selector Skeletons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().padding(top = 12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .appSkeletonShimmer()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .appSkeletonShimmer()
            )
        }
    }
}

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

@Composable
private fun AnalyticsTopBar(
    isPersian: Boolean,
    isFilterActive: Boolean,
    onFilterClick: () -> Unit
) {
    val smallShape = RoundedCornerShape(24.dp)
    val centerShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isFilterActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        smallShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                    .clip(smallShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = if (isPersian) "فیلتر" else "Filter",
                        tint = if (isFilterActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeFilterSelector(
    selectedFilter: TimeFilter,
    isPersian: Boolean,
    isCustomRange: Boolean = false,
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
        val filters = remember { TimeFilter.values() }

        filters.forEach { filter ->
            val isSelected = !isCustomRange && filter == selectedFilter
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

@Composable
private fun SmartInsightCard(
    isPersian: Boolean,
    insight: SmartInsight
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "SmartInsightArrowRotation"
    )

    val cardShape = RoundedCornerShape(20.dp)
    val headline = localizeInsightCategories(
        if (isPersian) insight.headlineFa else insight.headlineEn,
        isPersian
    )
    val summary = localizeInsightCategories(
        if (isPersian) insight.summaryFa else insight.summaryEn,
        isPersian
    )
    val action = localizeInsightCategories(
        if (isPersian) insight.actionFa else insight.actionEn,
        isPersian
    )
    val focusLabel = localizeInsightCategories(
        if (isPersian) insight.focusLabelFa else insight.focusLabelEn,
        isPersian
    )
    val focusValue = insight.focusValue.coerceIn(0.0, 100.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    )
                ),
                cardShape
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), cardShape)
            .clip(cardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "تحلیل هوشمند" else "Smart Analysis",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    if (isPersian) "بستن تحلیل هوشمند" else "Collapse smart analysis"
                } else {
                    if (isPersian) "نمایش تحلیل هوشمند" else "Expand smart analysis"
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(rotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.50f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = focusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${focusValue.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(focusValue.toFloat() / 100f)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private fun localizeInsightCategories(text: String, isPersian: Boolean): String {
    val categoryKeys = listOf(
        "FOOD",
        "TRANSPORT",
        "SHOPPING",
        "BILL",
        "SALARY",
        "INVESTMENT",
        "UNCATEGORIZED",
        "DEBT_CREDIT_PAYABLE",
        "DEBT_CREDIT_RECEIVABLE",
        "SAVING_GOAL",
        "SAVING_GOAL_RETURN"
    )

    return categoryKeys.fold(text) { result, key ->
        result.replace(Regex("(?<![A-Za-z_])${Regex.escape(key)}(?![A-Za-z_])"), StringMapper.getCategoryName(key, isPersian))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsFilterBottomSheet(
    isPersian: Boolean,
    isCustomRange: Boolean,
    customStartMillis: Long?,
    customEndMillis: Long?,
    compareWithPrevious: Boolean,
    onDismiss: () -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onClearCustomRange: () -> Unit,
    onCompareChanged: (Boolean) -> Unit
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    var pendingStart by remember(customStartMillis) { mutableStateOf(customStartMillis) }
    var pendingEnd by remember(customEndMillis) { mutableStateOf(customEndMillis) }
    var pendingCompare by remember(compareWithPrevious) { mutableStateOf(compareWithPrevious) }

    val dateRangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = pendingStart,
        initialSelectedEndDateMillis = pendingEnd
    )

    fun localDateMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val quickRanges = listOf(
        (if (isPersian) "امروز" else "Today") to {
            val end = LocalDate.now()
            pendingStart = localDateMillis(end)
            pendingEnd = localDateMillis(end)
        },
        (if (isPersian) "۷ روز اخیر" else "Last 7 days") to {
            val end = LocalDate.now()
            pendingStart = localDateMillis(end.minusDays(6))
            pendingEnd = localDateMillis(end)
        },
        (if (isPersian) "۳۰ روز اخیر" else "Last 30 days") to {
            val end = LocalDate.now()
            pendingStart = localDateMillis(end.minusDays(29))
            pendingEnd = localDateMillis(end)
        },
        (if (isPersian) "۹۰ روز اخیر" else "Last 90 days") to {
            val end = LocalDate.now()
            pendingStart = localDateMillis(end.minusDays(89))
            pendingEnd = localDateMillis(end)
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) "فیلتر هوشمند" else "Smart Filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPersian) "بازه تحلیل را انتخاب کن" else "Choose the analytics range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isCustomRange) {
                    IconButton(onClick = {
                        pendingStart = null
                        pendingEnd = null
                        pendingCompare = false
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isPersian) "حذف فیلتر" else "Clear filter"
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickRanges.forEach { (label, action) ->
                    FilterChip(
                        selected = false,
                        onClick = action,
                        label = { Text(label) }
                    )
                }
            }

            OutlinedButton(
                onClick = { showDateRangePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (pendingStart != null && pendingEnd != null) {
                        "${DateUtils.formatTimestamp(pendingStart!!, isPersian)}  —  ${DateUtils.formatTimestamp(pendingEnd!!, isPersian)}"
                    } else if (isPersian) {
                        "انتخاب بازه دلخواه"
                    } else {
                        "Choose a custom range"
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) "مقایسه با بازه قبل" else "Compare with previous period",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isPersian) "تغییرات هزینه را هم در تحلیل هوشمند ببین" else "Show spending changes in Smart Analysis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pendingCompare && pendingStart != null && pendingEnd != null,
                    onCheckedChange = { pendingCompare = it },
                    enabled = pendingStart != null && pendingEnd != null
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
                Button(
                    onClick = {
                        if (pendingStart != null && pendingEnd != null) {
                            onCustomRangeSelected(pendingStart!!, pendingEnd!!)
                            onCompareChanged(pendingCompare)
                        } else {
                            onClearCustomRange()
                            onCompareChanged(false)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPersian) "اعمال فیلتر" else "Apply Filter")
                }
            }
        }
    }

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    enabled = dateRangeState.selectedStartDateMillis != null && dateRangeState.selectedEndDateMillis != null,
                    onClick = {
                        pendingStart = dateRangeState.selectedStartDateMillis
                        pendingEnd = dateRangeState.selectedEndDateMillis
                        showDateRangePicker = false
                    }
                ) {
                    Text(if (isPersian) "تایید" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                title = { Text(if (isPersian) "انتخاب بازه تحلیل" else "Select analytics range") },
                headline = { Text(if (isPersian) "از تاریخ تا تاریخ" else "From date to date") }
            )
        }
    }
}

@Composable
private fun BalanceTrendChartCard(
    isPersian: Boolean,
    dataPoints: List<Float>,
    hasEnoughData: Boolean,
    selectedFilter: TimeFilter,
    currentTimeIndex: Int,
    isCustomRange: Boolean = false
) {
    val cardShape = RoundedCornerShape(24.dp)
    val lineColor = MaterialTheme.colorScheme.primary
    val averageColor = MaterialTheme.colorScheme.tertiary
    val isScrollable = hasEnoughData && dataPoints.size > 7
    val pointSpacing = 44.dp
    val chartWidth = if (isScrollable) pointSpacing * dataPoints.size.coerceAtLeast(1) else null
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val subtitle = if (isCustomRange) {
        if (isPersian) "روند موجودی در بازه انتخابی" else "Balance trend for the selected range"
    } else when (selectedFilter) {
        TimeFilter.DAILY -> if (isPersian) "روند موجودی از اولین تراکنش ماه تا امروز" else "Balance trend from first transaction to today"
        TimeFilter.WEEKLY -> if (isPersian) "روند موجودی در ۵ بازه ماه جاری" else "Balance trend for 5 periods of current month"
        TimeFilter.MONTHLY -> if (isPersian) "روند موجودی ماه‌به‌ماه" else "Month-by-month balance trend"
        TimeFilter.ALL -> if (isPersian) "روند موجودی به ازای هر تراکنش" else "Balance trend per transaction"
    }

    val insufficientTitle = if (isPersian) "هنوز داده کافی برای رسم روند وجود ندارد" else "Not Enough Data for Trend"
    val insufficientMessage = when (selectedFilter) {
        TimeFilter.MONTHLY -> if (isPersian) {
            "برای نمایش روند ماهانه، تراکنش‌ها باید حداقل در دو ماه مختلف ثبت شده باشند. با ثبت تراکنش در ماه‌های بیشتر، این نمودار به‌صورت خودکار فعال می‌شود."
        } else {
            "Monthly trend requires transactions in at least two different months. Keep adding transactions across months and this chart will unlock automatically."
        }
        TimeFilter.ALL -> if (isPersian) {
            "برای رسم نمودار روند، حداقل به دو تراکنش نیاز است. با ثبت تراکنش بعدی، روند موجودی شما نمایش داده می‌شود."
        } else {
            "At least two transactions are needed to draw a trend. Add one more transaction to see your balance trend."
        }
        TimeFilter.DAILY -> if (isPersian) {
            "برای رسم روند روزانه حداقل به تراکنش در دو روز متفاوت از ماه جاری نیاز است. با ثبت داده در روز بعد، نمودار فعال می‌شود."
        } else {
            "Daily trend needs transactions on at least two different days of the current month. Add data on another day to unlock the chart."
        }
        else -> if (isPersian) {
            "با ثبت تراکنش‌های بیشتر در بازه‌های زمانی مختلف، نمودار روند موجودی شما تکمیل‌تر نمایش داده می‌شود."
        } else {
            "Add more transactions across different time periods to build a richer balance trend."
        }
    }

    LaunchedEffect(currentTimeIndex, dataPoints.size, selectedFilter, hasEnoughData) {
        if (hasEnoughData && isScrollable && currentTimeIndex in dataPoints.indices) {
            val spacingPx = with(density) { pointSpacing.toPx() }
            val targetScroll = ((currentTimeIndex * spacingPx) - spacingPx * 2)
                .coerceAtLeast(0f)
                .toInt()
            scrollState.animateScrollTo(
                value = targetScroll,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }
    }

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
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasEnoughData) {
                TrendInsufficientDataView(
                    isPersian = isPersian,
                    title = insufficientTitle,
                    message = insufficientMessage
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "مبلغ" else "Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .rotate(-90f)
                            .padding(end = 4.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            if (isScrollable) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .horizontalScroll(scrollState)
                                ) {
                                    TrendLineCanvas(
                                        dataPoints = dataPoints,
                                        lineColor = lineColor,
                                        averageColor = averageColor,
                                        modifier = Modifier
                                            .width(chartWidth!!)
                                            .fillMaxHeight()
                                    )
                                }
                            } else {
                                TrendLineCanvas(
                                    dataPoints = dataPoints,
                                    lineColor = lineColor,
                                    averageColor = averageColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Text(
                            text = if (isPersian) "زمان" else "Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 6.dp)
                        )
                    }
                }

                if (dataPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Canvas(modifier = Modifier.size(20.dp, 2.dp)) {
                            drawLine(
                                color = averageColor.copy(alpha = 0.85f),
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        }
                        Text(
                            text = if (isPersian) "میانگین" else "Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendInsufficientDataView(
    isPersian: Boolean,
    title: String,
    message: String
) {
    val contentShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), contentShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentShape)
            .clip(contentShape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
            )
        }
    }
}

@Composable
private fun TrendLineCanvas(
    dataPoints: List<Float>,
    lineColor: Color,
    averageColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        val horizontalInset = 8.dp.toPx()
        val verticalInset = 10.dp.toPx()
        val chartWidth = (size.width - horizontalInset * 2).coerceAtLeast(1f)
        val chartHeight = (size.height - verticalInset * 2).coerceAtLeast(1f)

        val rawMax = dataPoints.maxOrNull() ?: 0f
        val rawMin = dataPoints.minOrNull() ?: 0f
        val range = (rawMax - rawMin).let { if (it <= 0f) kotlin.math.abs(rawMax).coerceAtLeast(1f) else it }
        val padding = range * 0.15f
        val maxVal = rawMax + padding
        val minVal = rawMin - padding

        val distanceX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth

        val average = dataPoints.average().toFloat()
        val avgNormalizedY = if (maxVal != minVal) {
            (average - minVal) / (maxVal - minVal)
        } else {
            0.5f
        }
        val avgY = verticalInset + chartHeight - (avgNormalizedY * chartHeight)

        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        drawLine(
            color = averageColor.copy(alpha = 0.85f),
            start = Offset(horizontalInset, avgY),
            end = Offset(horizontalInset + chartWidth, avgY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = dashPathEffect
        )

        val strokePath = Path()
        val fillPath = Path()
        val chartBottom = verticalInset + chartHeight

        dataPoints.forEachIndexed { index, value ->
            val x = horizontalInset + index * distanceX
            val normalizedY = if (maxVal != minVal) {
                (value - minVal) / (maxVal - minVal)
            } else {
                0.5f
            }
            val y = chartBottom - (normalizedY * chartHeight)

            if (index == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, chartBottom)
                fillPath.lineTo(x, y)
            } else {
                val prevX = horizontalInset + (index - 1) * distanceX
                val prevNormalizedY = if (maxVal != minVal) {
                    (dataPoints[index - 1] - minVal) / (maxVal - minVal)
                } else {
                    0.5f
                }
                val prevY = chartBottom - (prevNormalizedY * chartHeight)

                val controlX1 = prevX + distanceX / 2f
                val controlX2 = x - distanceX / 2f

                strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }

            if (index == dataPoints.size - 1) {
                fillPath.lineTo(x, chartBottom)
                fillPath.close()
            }
        }

        clipRect(
            left = horizontalInset - 2.dp.toPx(),
            top = verticalInset - 2.dp.toPx(),
            right = horizontalInset + chartWidth + 2.dp.toPx(),
            bottom = chartBottom + 2.dp.toPx()
        ) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = verticalInset,
                    endY = chartBottom
                )
            )

            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx())
            )

            dataPoints.forEachIndexed { index, value ->
                val x = horizontalInset + index * distanceX
                val normalizedY = if (maxVal != minVal) {
                    (value - minVal) / (maxVal - minVal)
                } else {
                    0.5f
                }
                val y = chartBottom - (normalizedY * chartHeight)

                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

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

@Composable
private fun TopExpensesCard(
    isPersian: Boolean,
    topExpenses: List<TransactionEntity>,
    currencyUnit: String
) {
    val cardShape = RoundedCornerShape(24.dp)
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }
    val currencyText = if (isPersian) {
        if (currencyUnit == "IRR") "ریال" else "تومان"
    } else {
        if (currencyUnit == "IRR") "Rial" else "Toman"
    }

    val recommendation = when {
        topExpenses.isEmpty() -> {
            if (isPersian) {
                "فعلاً هزینه‌ای که نیاز به توجه ویژه داشته باشد دیده نشد."
            } else {
                "No expense currently stands out as needing special attention."
            }
        }

        topExpenses.size == 1 -> {
            val title = topExpenses.first().title
                .takeIf { it.isNotBlank() }
                ?: StringMapper.getCategoryName(topExpenses.first().categoryId, isPersian)
            if (isPersian) {
                "بیشترین هزینه این بازه «$title» بوده؛ قبل از خریدهای مشابه، بررسی کن که واقعاً ضروری باشند."
            } else {
                "Your largest expense was $title. Before making similar purchases, consider whether they are necessary."
            }
        }

        else -> {
            if (isPersian) {
                "این چند هزینه بیشترین فشار را روی خرج این بازه گذاشته‌اند؛ اگر هدفت کاهش هزینه است، اول همین موارد را بررسی کن."
            } else {
                "These expenses account for the biggest spending pressure in this period. Review them first if you want to cut costs."
            }
        }
    }

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
                text = if (isPersian) "سنگین‌ترین هزینه‌ها" else "Top Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recommendation,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (topExpenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topExpenses.forEach { item ->
                        val displayAmount = if (currencyUnit == "IRR") item.amount * 10L else item.amount

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
                                        text = item.title
                                            .takeIf { it.isNotBlank() }
                                            ?.let { localizeInsightCategories(it, isPersian) }
                                            ?: StringMapper.getCategoryName(item.categoryId, isPersian),
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
                            }
                        }
                    }
                }
            }
        }
    }
}