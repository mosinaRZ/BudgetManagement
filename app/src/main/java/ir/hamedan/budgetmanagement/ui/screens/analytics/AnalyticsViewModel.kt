package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.utils.DateUtils
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class AnalyticsViewModel(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val selectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val isPersianState = MutableStateFlow(true)
    val isIncomeChartSelectedState = MutableStateFlow(false)
    val selectedTimeBucketIndex = MutableStateFlow<Int?>(null)
    val customStartMillis = MutableStateFlow<Long?>(null)
    val customEndMillis = MutableStateFlow<Long?>(null)
    val compareWithPreviousState = MutableStateFlow(false)

    fun updateLocale(isPersian: Boolean) {
        isPersianState.value = isPersian
    }

    fun setIncomeChartSelected(isSelected: Boolean) {
        isIncomeChartSelectedState.value = isSelected
        selectedTimeBucketIndex.value = null
    }

    fun onTimeBucketSelected(index: Int) {
        selectedTimeBucketIndex.value =
            if (selectedTimeBucketIndex.value == index) null else index
    }

    private data class AnalyticsControls(
        val timeFilter: TimeFilter,
        val isPersian: Boolean,
        val isIncomeChartSelected: Boolean,
        val selectedBucketIndex: Int?,
        val customStartMillis: Long?,
        val customEndMillis: Long?,
        val compareWithPrevious: Boolean
    )

    private val controls: StateFlow<AnalyticsControls> = combine(
        combine(
            selectedTimeFilter,
            isPersianState,
            isIncomeChartSelectedState,
            selectedTimeBucketIndex,
            customStartMillis
        ) { timeFilter, isPersian, isIncomeChartSelected, selectedBucketIndex, customStart ->
            AnalyticsControls(
                timeFilter = timeFilter,
                isPersian = isPersian,
                isIncomeChartSelected = isIncomeChartSelected,
                selectedBucketIndex = selectedBucketIndex,
                customStartMillis = customStart,
                customEndMillis = null,
                compareWithPrevious = false
            )
        },
        customEndMillis,
        compareWithPreviousState
    ) { partial, customEnd, compareWithPrevious ->
        partial.copy(
            customEndMillis = customEnd,
            compareWithPrevious = compareWithPrevious
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AnalyticsControls(TimeFilter.ALL, true, false, null, null, null, false)
    )

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        controls
    ) { allTransactions, categories, controls ->
        val timeFilter = controls.timeFilter
        val isPersian = controls.isPersian
        val isIncomeChartSelected = controls.isIncomeChartSelected
        val selectedBucketIndex = controls.selectedBucketIndex
        val isCustomRange = controls.customStartMillis != null && controls.customEndMillis != null

        val hasAnyTransaction = allTransactions.isNotEmpty()

        val filteredTransactions = if (isCustomRange) {
            val start = controls.customStartMillis!!
            val end = controls.customEndMillis!!
            allTransactions.filter { it.timestamp in start..end }.sortedBy { it.timestamp }
        } else {
            filterTransactionsByTime(allTransactions, timeFilter, isPersian)
        }
        val selectedTransactions = if (isCustomRange) {
            filteredTransactions
        } else {
            selectedBucketIndex?.let {
                filterTransactionsByBucket(allTransactions, timeFilter, it, isPersian)
            } ?: filteredTransactions
        }

        val totalIncome = selectedTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }.toDouble()
        val expensesList = selectedTransactions.filter { it.type == "EXPENSE" }
        val totalExpense = expensesList.sumOf { it.amount }.toDouble()
        val balance = totalIncome - totalExpense

        val categoryExpenses = expensesList
            .groupBy { it.categoryId }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }.toDouble()
                val percent = if (totalExpense > 0) (sum / totalExpense * 100).toFloat() else 0f
                CategoryExpenseModel(
                    categoryName = categories.firstOrNull { it.id == category }?.title ?: category,
                    totalAmount = sum,
                    percentage = percent,
                    color = generateColorForCategory(categories.firstOrNull { it.id == category }?.title ?: category)
                )
            }
            .sortedByDescending { it.totalAmount }

        val timeExpenses = if (isCustomRange) {
            calculateCustomTimeExpenses(
                transactions = filteredTransactions,
                startMillis = controls.customStartMillis!!,
                endMillis = controls.customEndMillis!!,
                isPersian = isPersian,
                isIncome = isIncomeChartSelected
            )
        } else {
            calculateTimeExpenses(allTransactions, timeFilter, isPersian, isIncomeChartSelected)
        }
        val currentIndex = if (isCustomRange || timeFilter == TimeFilter.DAILY) {
            timeExpenses.indexOfLast { it.totalAmount > 0.0 }.let { if (it == -1) 0 else it }
        } else {
            timeExpenses.indexOfFirst { it.isCurrent }.let { if (it == -1) 0 else it }
        }

        val averageExpense = if (expensesList.isNotEmpty()) totalExpense / expensesList.size else 0.0
        val heavyExpenseThreshold = calculateHeavyExpenseThreshold(expensesList)
        val heavyExpenseEntities = expensesList
            .filter { it.amount >= heavyExpenseThreshold && heavyExpenseThreshold > 0.0 }
            .sortedByDescending { it.amount }
        val topExpenseEntities = heavyExpenseEntities.take(5)

        val trendChartData = if (isCustomRange || selectedBucketIndex != null) {
            calculateSelectedTrendChartData(selectedTransactions)
        } else {
            calculateTrendChartData(allTransactions, timeFilter, isPersian)
        }

        val previousTransactions = if (controls.compareWithPrevious && isCustomRange) {
            previousPeriodTransactions(allTransactions, controls.customStartMillis!!, controls.customEndMillis!!)
        } else emptyList()
        val previousExpense = previousTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }.toDouble()
        val expenseChangePercent = if (controls.compareWithPrevious && previousTransactions.isNotEmpty() && previousExpense > 0.0) {
            ((totalExpense - previousExpense) / previousExpense) * 100.0
        } else null
        val smartInsight = buildSmartInsight(
            transactions = selectedTransactions,
            categories = categoryExpenses,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            averageExpense = averageExpense,
            heavyExpenseCount = heavyExpenseEntities.size,
            topExpense = topExpenseEntities.firstOrNull(),
            expenseChangePercent = expenseChangePercent
        )

        AnalyticsUiState(
            isLoading = false,
            hasAnyTransactionInDb = hasAnyTransaction,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance,
            categoryExpenses = categoryExpenses,
            timeExpenses = timeExpenses,
            currentTimeIndex = currentIndex,
            topExpenses = topExpenseEntities,
            averageExpense = averageExpense,
            expenseTransactionCount = expensesList.size,
            heavyExpenseCount = heavyExpenseEntities.size,
            heavyExpenseThreshold = heavyExpenseThreshold,
            selectedTimeBucketIndex = selectedBucketIndex,
            trendPoints = trendChartData.points,
            trendHasEnoughData = trendChartData.hasEnoughData,
            trendCurrentIndex = trendChartData.currentIndex,
            selectedPeriod = if (isCustomRange) "CUSTOM" else timeFilter.name,
            isIncomeChartSelected = isIncomeChartSelected,
            isCustomRange = isCustomRange,
            customStartMillis = controls.customStartMillis,
            customEndMillis = controls.customEndMillis,
            compareWithPrevious = controls.compareWithPrevious,
            smartInsight = smartInsight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun onTimeFilterChanged(filter: TimeFilter) {
        selectedTimeBucketIndex.value = null
        customStartMillis.value = null
        customEndMillis.value = null
        compareWithPreviousState.value = false
        selectedTimeFilter.value = filter
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val start = normalizeStartOfDay(startMillis)
        val end = normalizeEndOfDay(endMillis)
        if (start > end) return
        selectedTimeBucketIndex.value = null
        customStartMillis.value = start
        customEndMillis.value = end
        selectedTimeFilter.value = TimeFilter.ALL
    }

    fun clearCustomDateRange() {
        selectedTimeBucketIndex.value = null
        customStartMillis.value = null
        customEndMillis.value = null
        compareWithPreviousState.value = false
    }

    fun setCompareWithPrevious(enabled: Boolean) {
        compareWithPreviousState.value = enabled
    }

    private fun normalizeStartOfDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun normalizeEndOfDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L

    private fun previousPeriodTransactions(
        transactions: List<TransactionEntity>,
        startMillis: Long,
        endMillis: Long
    ): List<TransactionEntity> {
        val duration = (endMillis - startMillis + 1L).coerceAtLeast(1L)
        val previousEnd = startMillis - 1L
        val previousStart = previousEnd - duration + 1L
        return transactions.filter { it.timestamp in previousStart..previousEnd }
    }

    private fun buildSmartInsight(
        transactions: List<TransactionEntity>,
        categories: List<CategoryExpenseModel>,
        totalIncome: Double,
        totalExpense: Double,
        averageExpense: Double,
        heavyExpenseCount: Int,
        topExpense: TransactionEntity?,
        expenseChangePercent: Double?
    ): SmartInsight {
        if (transactions.isEmpty()) {
            return SmartInsight(
                headlineFa = "هنوز داده کافی برای تحلیل نیست",
                headlineEn = "Not enough data for a strong insight",
                summaryFa = "با ثبت چند تراکنش دیگر، الگوی هزینه‌کرد و نقاط قابل‌توجه دقیق‌تر مشخص می‌شود.",
                summaryEn = "Add a few more transactions to reveal clearer spending patterns.",
                actionFa = "بعد از چند تراکنش دوباره این بخش را بررسی کن.",
                actionEn = "Check this section again after a few more transactions.",
                focusLabelFa = "تراکنش‌های این بازه",
                focusLabelEn = "Transactions in range",
                focusValue = 0.0,
                expenseChangePercent = expenseChangePercent
            )
        }

        val expenseRatio = if (totalIncome > 0.0) totalExpense / totalIncome else null
        val topCategory = categories.firstOrNull()
        val topCategoryShare = topCategory?.percentage?.toDouble() ?: 0.0
        val largestAmount = topExpense?.amount?.toDouble() ?: 0.0
        val largestVsAverage = if (averageExpense > 0.0) largestAmount / averageExpense else 0.0

        val headlineFa: String
        val headlineEn: String
        val summaryFa: String
        val summaryEn: String
        val actionFa: String
        val actionEn: String

        when {
            expenseRatio != null && expenseRatio >= 0.85 -> {
                headlineFa = "بخش بزرگی از درآمد درگیر هزینه‌هاست"
                headlineEn = "A large share of income is going to expenses"
                summaryFa = "در این بازه ${topCategory?.categoryName?.let { "«$it»" } ?: "یک دسته اصلی"} بیشترین سهم هزینه را داشته و مجموع خرج به بخش قابل‌توجهی از درآمد رسیده است."
                summaryEn = "${topCategory?.categoryName ?: "One main category"} takes the largest spending share, while expenses consume a substantial part of income."
                actionFa = "اگر می‌خواهی حاشیه امن مالی بیشتری داشته باشی، اول همین دسته و چند هزینه بزرگ را بررسی کن."
                actionEn = "For more financial breathing room, review this category and the largest purchases first."
            }
            topCategoryShare >= 45.0 -> {
                headlineFa = "یک دسته، بخش اصلی خرج را به خود اختصاص داده"
                headlineEn = "One category dominates your spending"
                summaryFa = "تمرکز هزینه‌ها روی ${topCategory?.categoryName ?: "یک دسته اصلی"} بالاست؛ این الگو بیشترین اثر را روی نتیجه مالی این بازه دارد."
                summaryEn = "Spending is highly concentrated in ${topCategory?.categoryName ?: "one main category"}, making it the biggest driver of this period's result."
                actionFa = "اگر قصد کاهش هزینه داری، از همین دسته شروع کن؛ حتی تغییر کوچک در آن اثر محسوسی دارد."
                actionEn = "If you want to reduce spending, start here; even a small change can have a noticeable impact."
            }
            largestVsAverage >= 2.2 -> {
                headlineFa = "یک هزینه غیرعادی بیشتر از بقیه به چشم می‌آید"
                headlineEn = "One unusually large expense stands out"
                summaryFa = "بزرگ‌ترین هزینه این بازه حدود ${largestVsAverage.roundToInt()} برابر میانگین هزینه‌ها بوده و بخش مهمی از فشار مالی را ایجاد کرده است."
                summaryEn = "Your largest expense is about ${largestVsAverage.roundToInt()}× the average, making it a major source of spending pressure."
                actionFa = "این خرید را جداگانه بررسی کن؛ اگر تکرارشونده نیست، بهتر است اثر آن را از هزینه‌های عادی جدا ببینی."
                actionEn = "Review this purchase separately; if it is unusual, treat it differently from your normal spending pattern."
            }
            heavyExpenseCount >= 3 -> {
                headlineFa = "چند هزینه بزرگ، الگوی این بازه را شکل داده‌اند"
                headlineEn = "Several large expenses shape this period"
                summaryFa = "چند تراکنش سنگین هم‌زمان دیده می‌شود؛ بنابراین کنترل همین موارد می‌تواند بیشترین تفاوت را ایجاد کند."
                summaryEn = "Several large transactions stand out together, so managing these items can make the biggest difference."
                actionFa = "به‌جای بررسی همه هزینه‌ها، ابتدا همین چند مورد بزرگ را مرور کن."
                actionEn = "Instead of reviewing everything, start with these few high-impact transactions."
            }
            else -> {
                headlineFa = "الگوی هزینه‌کردت فعلاً متعادل‌تر به نظر می‌رسد"
                headlineEn = "Your spending pattern looks relatively balanced"
                summaryFa = "هزینه‌ها پراکندگی متعادل‌تری دارند و فعلاً نشانه خیلی پررنگی از تمرکز شدید روی یک مورد دیده نمی‌شود."
                summaryEn = "Spending is more distributed, with no single pattern dominating the period."
                actionFa = "همین روند را حفظ کن و بیشتر حواست به تغییرات ناگهانی در دسته‌های اصلی باشد."
                actionEn = "Keep the pattern steady and watch for sudden changes in your main categories."
            }
        }

        val comparisonSummary = expenseChangePercent?.let { change ->
            val directionFa = if (change > 0) "بیشتر" else "کمتر"
            val directionEn = if (change > 0) "higher" else "lower"
            " در مقایسه با بازه قبل، هزینه‌ها حدود ${kotlin.math.abs(change).roundToInt()}٪ $directionFa شده‌اند." to
                    " Compared with the previous period, expenses are about ${kotlin.math.abs(change).roundToInt()}% $directionEn."
        }

        return SmartInsight(
            headlineFa = headlineFa,
            headlineEn = headlineEn,
            summaryFa = summaryFa + (comparisonSummary?.first ?: ""),
            summaryEn = summaryEn + (comparisonSummary?.second ?: ""),
            actionFa = actionFa,
            actionEn = actionEn,
            focusLabelFa = if (topCategory != null) "سهم «${topCategory.categoryName}» از هزینه‌ها" else "کل هزینه این بازه",
            focusLabelEn = if (topCategory != null) "${topCategory.categoryName} share of expenses" else "Total expenses in range",
            focusValue = topCategoryShare,
            expenseChangePercent = expenseChangePercent
        )
    }

    private fun filterTransactionsByBucket(
        transactions: List<TransactionEntity>,
        filter: TimeFilter,
        bucketIndex: Int,
        isPersian: Boolean
    ): List<TransactionEntity> {
        val now = LocalDate.now()
        val (jYearNow, jMonthNow, _) = DateUtils.toJalali(now)
        return transactions.filter { tx ->
            val date = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isPersian) {
                val (jy, jm, jd) = DateUtils.toJalali(date)
                when (filter) {
                    TimeFilter.DAILY -> jy == jYearNow && jm == jMonthNow && jd == bucketIndex + 1
                    TimeFilter.WEEKLY -> jy == jYearNow && jm == jMonthNow && ((jd - 1) / 7).coerceIn(0, 4) == bucketIndex
                    TimeFilter.MONTHLY -> jy == jYearNow && jm == bucketIndex + 1
                    TimeFilter.ALL -> {
                        val years = transactions.map { item ->
                            val d = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            DateUtils.toJalali(d).first
                        }.distinct().sorted()
                        years.getOrNull(bucketIndex)?.let { it == jy } ?: false
                    }
                }
            } else {
                when (filter) {
                    TimeFilter.DAILY -> date.year == now.year && date.monthValue == now.monthValue && date.dayOfMonth == bucketIndex + 1
                    TimeFilter.WEEKLY -> date.year == now.year && date.monthValue == now.monthValue && ((date.dayOfMonth - 1) / 7).coerceIn(0, 4) == bucketIndex
                    TimeFilter.MONTHLY -> date.year == now.year && date.monthValue == bucketIndex + 1
                    TimeFilter.ALL -> {
                        val years = transactions.map { item ->
                            Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().year
                        }.distinct().sorted()
                        years.getOrNull(bucketIndex)?.let { it == date.year } ?: false
                    }
                }
            }
        }.sortedBy { it.timestamp }
    }

    private fun calculateSelectedTrendChartData(transactions: List<TransactionEntity>): TrendChartData {
        if (transactions.isEmpty()) return TrendChartData(emptyList(), false, 0)
        val sorted = transactions.sortedBy { it.timestamp }
        var running = 0.0
        val points = sorted.map { tx ->
            running += if (tx.type == "INCOME") tx.amount else -tx.amount
            running.toFloat()
        }
        return TrendChartData(points, true, (points.size - 1).coerceAtLeast(0))
    }

    private fun calculateHeavyExpenseThreshold(expenses: List<TransactionEntity>): Double {
        if (expenses.size < 2) return Double.POSITIVE_INFINITY
        val values = expenses.map { it.amount.toDouble() }.sorted()
        fun percentile(p: Double): Double {
            val position = (values.size - 1) * p
            val lower = position.toInt()
            val upper = kotlin.math.ceil(position).toInt().coerceAtMost(values.lastIndex)
            if (lower == upper) return values[lower]
            val fraction = position - lower
            return values[lower] + (values[upper] - values[lower]) * fraction
        }
        val q1 = percentile(0.25)
        val q3 = percentile(0.75)
        val iqr = (q3 - q1).coerceAtLeast(0.0)
        val average = values.average()
        return maxOf(q3 + 1.5 * iqr, average * 1.5)
    }

    private fun filterTransactionsByTime(
        transactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean
    ): List<TransactionEntity> {
        if (filter == TimeFilter.ALL) return transactions

        val now = LocalDate.now()
        val (jYearNow, jMonthNow, jDayNow) = DateUtils.toJalali(now)

        return transactions.filter { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

            if (isPersian) {
                val (jYearTx, jMonthTx, jDayTx) = DateUtils.toJalali(txDate)
                when (filter) {
                    TimeFilter.DAILY -> jYearTx == jYearNow && jMonthTx == jMonthNow && jDayTx == jDayNow
                    TimeFilter.WEEKLY -> {
                        if (jYearTx != jYearNow || jMonthTx != jMonthNow) false
                        else {
                            val nowWeekIndex = (jDayNow - 1) / 7
                            val txWeekIndex = (jDayTx - 1) / 7
                            nowWeekIndex == txWeekIndex
                        }
                    }
                    TimeFilter.MONTHLY -> jYearTx == jYearNow && jMonthTx == jMonthNow
                    TimeFilter.ALL -> true
                }
            } else {
                when (filter) {
                    TimeFilter.DAILY -> txDate == now
                    TimeFilter.WEEKLY -> {
                        val weekFields = WeekFields.of(Locale.US)
                        txDate.year == now.year &&
                                txDate.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear())
                    }
                    TimeFilter.MONTHLY -> txDate.year == now.year && txDate.monthValue == now.monthValue
                    TimeFilter.ALL -> true
                }
            }
        }
    }

    private fun calculateCustomTimeExpenses(
        transactions: List<TransactionEntity>,
        startMillis: Long,
        endMillis: Long,
        isPersian: Boolean,
        isIncome: Boolean
    ): List<TimeExpenseModel> {
        val targetType = if (isIncome) "INCOME" else "EXPENSE"
        val targetTransactions = transactions
            .asSequence()
            .filter { it.type == targetType && it.timestamp in startMillis..endMillis }
            .toList()

        if (startMillis > endMillis) return emptyList()

        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val dayCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

        // For short ranges show one bar per day; for longer ranges aggregate into
        // practical weekly buckets so the chart remains readable.
        val bucketSizeDays = when {
            dayCount <= 31 -> 1
            dayCount <= 120 -> 7
            else -> 30
        }
        val bucketCount = ((dayCount + bucketSizeDays - 1) / bucketSizeDays).coerceAtLeast(1)
        val sums = DoubleArray(bucketCount)

        targetTransactions.forEach { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
            val offset = java.time.temporal.ChronoUnit.DAYS.between(startDate, txDate).toInt()
            if (offset in 0 until dayCount) {
                val bucket = (offset / bucketSizeDays).coerceIn(0, bucketCount - 1)
                sums[bucket] += tx.amount.toDouble()
            }
        }

        val today = LocalDate.now()
        return (0 until bucketCount).map { index ->
            val bucketStart = startDate.plusDays((index * bucketSizeDays).toLong())
            val bucketEnd = bucketStart.plusDays((bucketSizeDays - 1).toLong())
                .let { if (it.isAfter(endDate)) endDate else it }

            val (labelFa, labelEn) = if (bucketSizeDays == 1) {
                if (isPersian) {
                    val (_, month, day) = DateUtils.toJalali(bucketStart)
                    day.toString() to day.toString()
                } else {
                    bucketStart.dayOfMonth.toString() to bucketStart.dayOfMonth.toString()
                }
            } else {
                if (isPersian) {
                    val (_, startMonth, startDay) = DateUtils.toJalali(bucketStart)
                    val (_, endMonth, endDay) = DateUtils.toJalali(bucketEnd)
                    if (startMonth == endMonth) {
                        "$startDay–$endDay" to "$startDay–$endDay"
                    } else {
                        "$startMonth/$startDay–$endMonth/$endDay" to
                                "$startMonth/$startDay–$endMonth/$endDay"
                    }
                } else {
                    "${bucketStart.monthValue}/${bucketStart.dayOfMonth}–${bucketEnd.monthValue}/${bucketEnd.dayOfMonth}" to
                            "${bucketStart.monthValue}/${bucketStart.dayOfMonth}–${bucketEnd.monthValue}/${bucketEnd.dayOfMonth}"
                }
            }

            TimeExpenseModel(
                labelFa = labelFa,
                labelEn = labelEn,
                totalAmount = sums[index],
                isCurrent = today in bucketStart..bucketEnd
            )
        }
    }

    private fun calculateTimeExpenses(
        allTransactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean,
        isIncomeChartSelected: Boolean
    ): List<TimeExpenseModel> {
        val targetType = if (isIncomeChartSelected) "INCOME" else "EXPENSE"
        val targetTransactions = allTransactions.filter { it.type == targetType }
        val now = LocalDate.now()

        if (isPersian) {
            val (currentJalaliYear, currentJalaliMonth, currentJalaliDay) = DateUtils.toJalali(now)
            val daysInJalaliMonth = DateUtils.getDaysInJalaliMonth(currentJalaliYear, currentJalaliMonth)

            return when (filter) {
                TimeFilter.DAILY -> {
                    val daysInMonth = daysInJalaliMonth
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }

                    val dailySums = Array(daysInMonth) { 0.0 }
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        if (day in 1..daysInMonth) {
                            dailySums[day - 1] += tx.amount.toDouble()
                        }
                    }

                    (1..daysInMonth).map { day ->
                        TimeExpenseModel(
                            labelFa = day.toString(),
                            labelEn = day.toString(),
                            totalAmount = dailySums[day - 1],
                            isCurrent = (day == currentJalaliDay),
                        )
                    }
                }

                TimeFilter.WEEKLY -> {
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }

                    val weeks = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        when {
                            day in 1..7 -> weeks[0] += tx.amount.toDouble()
                            day in 8..14 -> weeks[1] += tx.amount.toDouble()
                            day in 15..21 -> weeks[2] += tx.amount.toDouble()
                            day in 22..28 -> weeks[3] += tx.amount.toDouble()
                            day >= 29 -> weeks[4] += tx.amount.toDouble()
                        }
                    }

                    val currentWeekIndex = ((currentJalaliDay - 1) / 7).coerceAtMost(4)
                    val weekCount = when {
                        daysInJalaliMonth <= 28 -> 4
                        else -> 5
                    }

                    (0 until weekCount).map { index ->
                        val (labelFa, labelEn) = when (index) {
                            0 -> "روز ۱–۷" to "Days 1–7"
                            1 -> "روز ۸–۱۴" to "Days 8–14"
                            2 -> "روز ۱۵–۲۱" to "Days 15–21"
                            3 -> "روز ۲۲–۲۸" to "Days 22–28"
                            else -> {
                                val endDayFa = daysInJalaliMonth
                                    .toString()
                                    .map { char ->
                                        when (char) {
                                            '0' -> '۰'
                                            '1' -> '۱'
                                            '2' -> '۲'
                                            '3' -> '۳'
                                            '4' -> '۴'
                                            '5' -> '۵'
                                            '6' -> '۶'
                                            '7' -> '۷'
                                            '8' -> '۸'
                                            '9' -> '۹'
                                            else -> char
                                        }
                                    }
                                    .joinToString("")

                                "روز ۲۹-$endDayFa" to "Days 29–$daysInJalaliMonth"
                            }
                        }

                        TimeExpenseModel(
                            labelFa = labelFa,
                            labelEn = labelEn,
                            totalAmount = weeks[index],
                            isCurrent = currentWeekIndex == index
                        )
                    }
                }

                TimeFilter.MONTHLY -> {
                    val currentYearExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, _, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear
                    }

                    val months = Array(12) { 0.0 }
                    currentYearExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, jMonth, _) = DateUtils.toJalali(txDate)
                        if (jMonth in 1..12) {
                            months[jMonth - 1] += tx.amount.toDouble()
                        }
                    }

                    DateUtils.PERSIAN_MONTH_NAMES.indices.map { index ->
                        TimeExpenseModel(
                            labelFa = DateUtils.PERSIAN_MONTH_NAMES[index],
                            labelEn = DateUtils.PERSIAN_MONTH_NAMES[index],
                            totalAmount = months[index],
                            isCurrent = (index == currentJalaliMonth - 1),
                        )
                    }
                }

                TimeFilter.ALL -> {
                    val yearGrouped = targetTransactions.groupBy { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, _, _) = DateUtils.toJalali(txDate)
                        jYear
                    }

                    if (yearGrouped.isEmpty()) {
                        listOf(TimeExpenseModel(currentJalaliYear.toString(), currentJalaliYear.toString(), 0.0, isCurrent = true))
                    } else {
                        yearGrouped.keys.sorted().map { year ->
                            val sum = (yearGrouped[year]?.sumOf { it.amount } ?: 0L).toDouble()
                            TimeExpenseModel(
                                labelFa = year.toString(),
                                labelEn = year.toString(),
                                totalAmount = sum,
                                isCurrent = (year == currentJalaliYear),
                            )
                        }
                    }
                }
            }
        } else {
            val currentGYear = now.year
            val currentGMonth = now.monthValue
            val currentGDay = now.dayOfMonth
            val daysInMonth = now.lengthOfMonth()

            return when (filter) {
                TimeFilter.DAILY -> {
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }

                    val dailySums = Array(daysInMonth) { 0.0 }
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        if (day in 1..daysInMonth) {
                            dailySums[day - 1] += tx.amount.toDouble()
                        }
                    }

                    (1..daysInMonth).map { day ->
                        TimeExpenseModel(
                            labelFa = day.toString(),
                            labelEn = day.toString(),
                            totalAmount = dailySums[day - 1],
                            isCurrent = (day == currentGDay),
                        )
                    }
                }

                TimeFilter.WEEKLY -> {
                    val currentMonthExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }

                    val weeks = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
                    currentMonthExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        when {
                            day in 1..7 -> weeks[0] += tx.amount.toDouble()
                            day in 8..14 -> weeks[1] += tx.amount.toDouble()
                            day in 15..21 -> weeks[2] += tx.amount.toDouble()
                            day in 22..28 -> weeks[3] += tx.amount.toDouble()
                            day >= 29 -> weeks[4] += tx.amount.toDouble()
                        }
                    }

                    val currentWeekIndex = ((currentGDay - 1) / 7).coerceAtMost(4)
                    val weekCount = if (daysInMonth >= 29) 5 else 4

                    (0 until weekCount).map { index ->
                        val labelEn = when (index) {
                            0 -> "Days 1–7"
                            1 -> "Days 8–14"
                            2 -> "Days 15–21"
                            3 -> "Days 22–28"
                            else -> "Days 29–$daysInMonth"
                        }
                        TimeExpenseModel(
                            labelFa = labelEn,
                            labelEn = labelEn,
                            totalAmount = weeks[index],
                            isCurrent = currentWeekIndex == index
                        )
                    }
                }

                TimeFilter.MONTHLY -> {
                    val currentYearExpenses = targetTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear
                    }

                    val months = Array(12) { 0.0 }
                    currentYearExpenses.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val gMonth = txDate.monthValue
                        if (gMonth in 1..12) {
                            months[gMonth - 1] += tx.amount.toDouble()
                        }
                    }

                    DateUtils.ENGLISH_MONTH_NAMES.indices.map { index ->
                        TimeExpenseModel(
                            labelFa = DateUtils.ENGLISH_MONTH_NAMES[index],
                            labelEn = DateUtils.ENGLISH_MONTH_NAMES[index],
                            totalAmount = months[index],
                            isCurrent = (index == currentGMonth - 1),
                        )
                    }
                }

                TimeFilter.ALL -> {
                    val yearGrouped = targetTransactions.groupBy { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year
                    }

                    if (yearGrouped.isEmpty()) {
                        listOf(TimeExpenseModel(currentGYear.toString(), currentGYear.toString(), 0.0, isCurrent = true))
                    } else {
                        yearGrouped.keys.sorted().map { year ->
                            val sum = (yearGrouped[year]?.sumOf { it.amount } ?: 0L).toDouble()
                            TimeExpenseModel(
                                labelFa = year.toString(),
                                labelEn = year.toString(),
                                totalAmount = sum,
                                isCurrent = (year == currentGYear),
                            )
                        }
                    }
                }
            }
        }
    }

    private data class TrendChartData(
        val points: List<Float>,
        val hasEnoughData: Boolean,
        val currentIndex: Int = 0
    )

    private fun countDistinctCalendarMonths(
        transactions: List<TransactionEntity>,
        isPersian: Boolean
    ): Int {
        if (transactions.isEmpty()) return 0
        return transactions.map { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isPersian) {
                val (year, month, _) = DateUtils.toJalali(txDate)
                year * 100 + month
            } else {
                txDate.year * 100 + txDate.monthValue
            }
        }.distinct().count()
    }

    private fun countDistinctCalendarDaysInCurrentMonth(
        transactions: List<TransactionEntity>,
        isPersian: Boolean
    ): Int {
        val now = LocalDate.now()
        val (currentYear, currentMonth, _) = DateUtils.toJalali(now)
        return transactions.mapNotNull { tx ->
            val date = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isPersian) {
                val (y, m, d) = DateUtils.toJalali(date)
                if (y == currentYear && m == currentMonth) d else null
            } else {
                if (date.year == now.year && date.monthValue == now.monthValue) date.dayOfMonth else null
            }
        }.distinct().size
    }

    private fun calculateTrendChartData(
        allTransactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean
    ): TrendChartData {
        val distinctMonths = countDistinctCalendarMonths(allTransactions, isPersian)
        val hasEnoughData = when (filter) {
            TimeFilter.DAILY -> countDistinctCalendarDaysInCurrentMonth(allTransactions, isPersian) >= 2
            TimeFilter.WEEKLY -> true
            TimeFilter.MONTHLY -> distinctMonths >= 2
            TimeFilter.ALL -> allTransactions.size >= 2
        }

        if (!hasEnoughData) {
            return TrendChartData(points = emptyList(), hasEnoughData = false, currentIndex = 0)
        }

        val points = calculateTrendPoints(allTransactions, filter, isPersian)
        val currentIndex = calculateTrendCurrentIndex(points.size, filter, isPersian)
        return TrendChartData(points = points, hasEnoughData = true, currentIndex = currentIndex)
    }

    private fun calculateTrendCurrentIndex(
        pointCount: Int,
        filter: TimeFilter,
        isPersian: Boolean
    ): Int {
        if (pointCount <= 0) return 0

        val now = LocalDate.now()
        return when (filter) {
            TimeFilter.DAILY -> pointCount - 1
            TimeFilter.WEEKLY -> {
                val currentDay = if (isPersian) {
                    DateUtils.toJalali(now).third
                } else {
                    now.dayOfMonth
                }
                ((currentDay - 1) / 7).coerceIn(0, pointCount - 1)
            }
            TimeFilter.MONTHLY -> pointCount - 1
            TimeFilter.ALL -> pointCount - 1
        }
    }

    private fun calculateTrendPoints(
        allTransactions: List<TransactionEntity>,
        filter: TimeFilter,
        isPersian: Boolean
    ): List<Float> {
        val now = LocalDate.now()
        val netAmount: (TransactionEntity) -> Double = { tx ->
            if (tx.type == "INCOME") {
                tx.amount.toDouble()
            } else {
                -tx.amount.toDouble()
            }
        }

        if (isPersian) {
            val (currentJalaliYear, currentJalaliMonth, currentJalaliDay) = DateUtils.toJalali(now)

            return when (filter) {
                TimeFilter.DAILY -> calculateDailyTrendPoints(
                    allTransactions = allTransactions,
                    isPersian = true,
                    currentDay = currentJalaliDay,
                    netAmount = netAmount
                )

                TimeFilter.WEEKLY -> {
                    val monthTransactions = allTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                        jYear == currentJalaliYear && jMonth == currentJalaliMonth
                    }
                    val weeklyNet = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
                    monthTransactions.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val (_, _, day) = DateUtils.toJalali(txDate)
                        val weekIndex = when {
                            day in 1..7 -> 0
                            day in 8..14 -> 1
                            day in 15..21 -> 2
                            day in 22..28 -> 3
                            else -> 4
                        }
                        weeklyNet[weekIndex] += netAmount(tx)
                    }
                    var cumulative = 0.0
                    weeklyNet.map { weekNet ->
                        cumulative += weekNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.MONTHLY -> {
                    val monthGroups = allTransactions
                        .groupBy { tx ->
                            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                            jYear * 100 + jMonth
                        }
                        .toSortedMap()

                    var cumulative = 0.0
                    monthGroups.values.map { monthTransactions ->
                        val monthNet = monthTransactions.sumOf { netAmount(it) }
                        cumulative += monthNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.ALL -> {
                    if (allTransactions.isEmpty()) return emptyList()
                    val sorted = allTransactions.sortedBy { it.timestamp }
                    var runningBalance = 0.0
                    sorted.map { tx ->
                        runningBalance += netAmount(tx)
                        runningBalance.toFloat()
                    }
                }
            }
        } else {
            val currentGYear = now.year
            val currentGMonth = now.monthValue
            val currentGDay = now.dayOfMonth

            return when (filter) {
                TimeFilter.DAILY -> calculateDailyTrendPoints(
                    allTransactions = allTransactions,
                    isPersian = false,
                    currentDay = currentGDay,
                    netAmount = netAmount
                )

                TimeFilter.WEEKLY -> {
                    val monthTransactions = allTransactions.filter { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        txDate.year == currentGYear && txDate.monthValue == currentGMonth
                    }
                    val weeklyNet = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
                    monthTransactions.forEach { tx ->
                        val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val day = txDate.dayOfMonth
                        val weekIndex = when {
                            day in 1..7 -> 0
                            day in 8..14 -> 1
                            day in 15..21 -> 2
                            day in 22..28 -> 3
                            else -> 4
                        }
                        weeklyNet[weekIndex] += netAmount(tx)
                    }
                    var cumulative = 0.0
                    weeklyNet.map { weekNet ->
                        cumulative += weekNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.MONTHLY -> {
                    val monthGroups = allTransactions
                        .groupBy { tx ->
                            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            txDate.year * 100 + txDate.monthValue
                        }
                        .toSortedMap()

                    var cumulative = 0.0
                    monthGroups.values.map { monthTransactions ->
                        val monthNet = monthTransactions.sumOf { netAmount(it) }
                        cumulative += monthNet
                        cumulative.toFloat()
                    }
                }

                TimeFilter.ALL -> {
                    if (allTransactions.isEmpty()) return emptyList()
                    val sorted = allTransactions.sortedBy { it.timestamp }
                    var runningBalance = 0.0
                    sorted.map { tx ->
                        runningBalance += netAmount(tx)
                        runningBalance.toFloat()
                    }
                }
            }
        }
    }

    private fun calculateDailyTrendPoints(
        allTransactions: List<TransactionEntity>,
        isPersian: Boolean,
        currentDay: Int,
        netAmount: (TransactionEntity) -> Double
    ): List<Float> {
        val now = LocalDate.now()
        val monthTransactions = allTransactions.filter { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isPersian) {
                val (jYear, jMonth, _) = DateUtils.toJalali(txDate)
                val (currentJYear, currentJMonth, _) = DateUtils.toJalali(now)
                jYear == currentJYear && jMonth == currentJMonth
            } else {
                txDate.year == now.year && txDate.monthValue == now.monthValue
            }
        }

        if (monthTransactions.isEmpty()) return emptyList()

        fun transactionDay(tx: TransactionEntity): Int {
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            return if (isPersian) {
                DateUtils.toJalali(txDate).third
            } else {
                txDate.dayOfMonth
            }
        }

        val firstDay = monthTransactions.minOf(::transactionDay)
        val dailyNet = mutableMapOf<Int, Double>()

        monthTransactions.forEach { tx ->
            val day = transactionDay(tx)
            if (day in firstDay..currentDay) {
                dailyNet[day] = (dailyNet[day] ?: 0.0) + netAmount(tx)
            }
        }

        val lastDataDay = dailyNet.keys.maxOrNull() ?: firstDay
        val endDay = lastDataDay.coerceAtLeast(firstDay).coerceAtMost(currentDay)
        var cumulative = 0.0
        return (firstDay..endDay).map { day ->
            cumulative += dailyNet[day] ?: 0.0
            cumulative.toFloat()
        }
    }
}

private fun generateColorForCategory(categoryName: String): Color {
    val colors = listOf(
        Color(0xFF6750A4), Color(0xFF0288D1), Color(0xFF388E3C),
        Color(0xFFF57C00), Color(0xFFD32F2F), Color(0xFF7B1FA2),
        Color(0xFF00796B), Color(0xFFC2185B), Color(0xFFE64A19), Color(0xFF512DA8)
    )
    if (categoryName.isEmpty()) return colors[0]
    val index = abs(categoryName.hashCode()) % colors.size
    return colors[index]
}