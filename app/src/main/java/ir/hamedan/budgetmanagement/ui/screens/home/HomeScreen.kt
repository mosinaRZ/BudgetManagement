package ir.hamedan.budgetmanagement.ui.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.payments.UpcomingPaymentViewModel
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionViewModel
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String,
    val timeFa: String,
    val timeEn: String,
    val isRead: Boolean = false
)

enum class NotificationType {
    SUCCESS, ERROR, REWARD, SYSTEM
}

data class HomeDueItem(
    val id: String,
    val title: String,
    val amount: Double,
    val daysLeft: Int,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onSeeAllTransactionsClick: () -> Unit = {},
    onAddScreenClickDue: () -> Unit = {},
    onAddScreenClickPiggy: () -> Unit = {},
    onAddScreenClickLimit: () -> Unit = {},
    transactionViewModel: TransactionViewModel = viewModel(factory = TransactionViewModel.Factory(LocalContext.current)),
    upcomingViewModel: UpcomingPaymentViewModel = viewModel(factory = UpcomingPaymentViewModel.Factory(LocalContext.current)),
    goalsViewModel: SavingGoalsViewModel = viewModel(factory = SavingGoalsViewModel.Factory(LocalContext.current)),
    budgetViewModel: BudgetLimitViewModel = viewModel(factory = BudgetLimitViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val isPersian = LocaleHelper.getLanguage(context) == "fa"

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val transactionsList by transactionViewModel.filteredTransactions.collectAsState()
    val paymentsList by upcomingViewModel.payments.collectAsState()
    val goalsList by goalsViewModel.savingGoals.collectAsState(initial = emptyList())
    val limitsList by budgetViewModel.budgetLimitsWithSpent.collectAsState(initial = emptyList())

    val currencyUnit by transactionViewModel.currencyUnit.collectAsState(initial = "IRT")

    // ---------------------------------------------------------------------
    // محاسبه دقیق و پویا: ابتدا و انتهای ماه جاری و ماه قبل
    // ---------------------------------------------------------------------
    val (currentMonthStart, currentMonthEnd, prevMonthStart, prevMonthEnd) = remember {
        val calendar = Calendar.getInstance()

        val startCur = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCur = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startPrev = (startCur.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val endPrev = (startCur.clone() as Calendar).apply { add(Calendar.MILLISECOND, -1) }

        Quadruple(startCur.timeInMillis, endCur.timeInMillis, startPrev.timeInMillis, endPrev.timeInMillis)
    }

    val periodTransactions = remember(transactionsList, currentMonthStart, currentMonthEnd) {
        transactionsList.filter { it.timestamp in currentMonthStart..currentMonthEnd }
    }

    val prevPeriodTransactions = remember(transactionsList, prevMonthStart, prevMonthEnd) {
        transactionsList.filter { it.timestamp in prevMonthStart..prevMonthEnd }
    }

    val totalIncome = periodTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = periodTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    val prevIncome = prevPeriodTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val prevExpense = prevPeriodTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val prevBalance = prevIncome - prevExpense
    val diffBalance = balance - prevBalance

    val recentTransactions = remember(transactionsList) {
        transactionsList.sortedByDescending { it.timestamp }.take(3)
    }

    val upcomingPayments = remember(paymentsList) {
        paymentsList.filter { !it.isPaid }.sortedBy { it.dueDate }
    }

    val allGoals = remember(goalsList) { goalsList ?: emptyList() }
    val allLimits = remember(limitsList) { limitsList }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.statusBarsPadding().height(55.dp)) }

            // ---------------------------------------------------------------------
            // کارت بالانس اصلی
            // ---------------------------------------------------------------------
            item {
                val balanceShape = RoundedCornerShape(24.dp)

                val periodLabel = if (isPersian) {
                    "عملکرد ماه جاری (${DateUtils.formatTimestamp(currentMonthStart, true)} تا ${DateUtils.formatTimestamp(currentMonthEnd, true)})"
                } else {
                    "Current Month (${DateUtils.formatTimestamp(currentMonthStart, false)} - ${DateUtils.formatTimestamp(currentMonthEnd, false)})"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), balanceShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), balanceShape)
                        .clip(balanceShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.balancebanner),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.15f
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(Modifier.height(8.dp))

                        val displayBalance = if (currencyUnit == "IRR") (balance * 10).toLong() else balance.toLong()
                        val currencyText = if (isPersian) {
                            if (currencyUnit == "IRR") "ریال" else "تومان"
                        } else {
                            if (currencyUnit == "IRR") "Rial" else "Toman"
                        }

                        // محاسبه علامت منفی در صورت منفی بودن بالانس
                        val formattedAmount = numberFormatter.format(abs(displayBalance))
                        val sign = if (balance < 0) "-" else ""

                        Text(
                            text = "$sign$formattedAmount $currencyText",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(12.dp))

                        val diffDisplay = if (currencyUnit == "IRR") (abs(diffBalance) * 10).toLong() else abs(diffBalance).toLong()
                        val isPositive = diffBalance >= 0

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPositive) "▲" else "▼",
                                color = if (isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )

                            val comparisonText = if (isPersian) {
                                val state = if (isPositive) "افزایش" else "کاهش"
                                " $state ${numberFormatter.format(diffDisplay)} $currencyText نسبت به ماه قبل"
                            } else {
                                val state = if (isPositive) "Increased by" else "Decreased by"
                                " $state ${numberFormatter.format(diffDisplay)} $currencyText vs last month"
                            }

                            Text(
                                text = comparisonText,
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------------------
            // خلاصه درآمد و هزینه
            // ---------------------------------------------------------------------
            item {
                val summaryCardShape = RoundedCornerShape(20.dp)
                val income = totalIncome
                val expense = totalExpense

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), summaryCardShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), summaryCardShape)
                            .clip(summaryCardShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.incomebanner),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.12f
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isPersian) "درآمد این ماه" else "Income",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            val displayIncome = if (currencyUnit == "IRR") (income * 10).toLong() else income.toLong()
                            val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")
                            Text(
                                text = "${numberFormatter.format(displayIncome)} $currencyText",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), summaryCardShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), summaryCardShape)
                            .clip(summaryCardShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.expensebanner),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.12f
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isPersian) "هزینه این ماه" else "Expense",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            val displayExpense = if (currencyUnit == "IRR") (expense * 10).toLong() else expense.toLong()
                            val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")
                            Text(
                                text = "${numberFormatter.format(displayExpense)} $currencyText",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------------------
            // بخش قلک‌های پس‌انداز
            // ---------------------------------------------------------------------
            item {
                val piggyShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), piggyShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), piggyShape)
                        .clip(piggyShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.goalbanner),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.12f
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎯", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isPersian) "قلک‌های پس‌انداز" else "Savings Goals",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPersian) "پیشرفت هدف‌های مالی شما" else "Your financial targets progress",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        allGoals.forEach { goal ->
                            val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${goal.icon} ${goal.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val curr = if (currencyUnit == "IRR") 10 else 1
                                    Text(
                                        text = "${numberFormatter.format((goal.currentAmount * curr).toLong())} / ${numberFormatter.format((goal.targetAmount * curr).toLong())} ${if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        Button(
                            onClick = onAddScreenClickPiggy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPersian) "ساخت یک قلک جدید" else "Create a New Goal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------------------
            // بخش محدودیت‌های خرج‌کرد
            // ---------------------------------------------------------------------
            item {
                val budgetShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), budgetShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), budgetShape)
                        .clip(budgetShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.limitbanner),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.12f
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚠️", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isPersian) "محدودیت‌های خرج‌کرد" else "Expense Limits",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPersian) "مدیریت سقف بودجه دسته‌ها" else "Manage category budget ceilings",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        allLimits.forEach { limit ->
                            val progress = if (limit.entity.maxLimit > 0) (limit.currentSpent / limit.entity.maxLimit).toFloat() else 0f
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${limit.categoryEmoji} ${StringMapper.getCategoryName(limit.entity.categoryName, isPersian)} (${(progress * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val curr = if (currencyUnit == "IRR") 10 else 1
                                    Text(
                                        text = "${numberFormatter.format((limit.currentSpent * curr).toLong())} / ${numberFormatter.format((limit.entity.maxLimit * curr).toLong())} ${if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = (if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        Button(
                            onClick = onAddScreenClickLimit,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPersian) "تنظیم محدودیت جدید" else "Set a New Budget Limit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------------------
            // بخش موعد سررسید (طراحی کلی هماهنگ + نمایش داخلی به صورت LazyRow و بدون ProgressBar)
            // ---------------------------------------------------------------------
            item {
                val dueItems = upcomingPayments.map { payment ->
                    val daysLeft = ((payment.dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                    HomeDueItem(payment.id, payment.title, payment.amount, daysLeft, "installment")
                }

                val dueShape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), dueShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), dueShape)
                        .clip(dueShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.duebanner),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.12f
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📅", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isPersian) "موعد پرداخت‌های نزدیک" else "Upcoming Payments",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPersian) "مدیریت اقساط و قبوض سررسید" else "Manage upcoming bills & installments",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // نمایش به صورت LazyRow و بدون استفاده از ProgressBar
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dueItems) { due ->
                                val cardShape = RoundedCornerShape(16.dp)
                                val isUrgent = due.daysLeft <= 3
                                val statusColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                val icon = "🏦"

                                Box(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), cardShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
                                        .clip(cardShape)
                                        .clickable { }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = icon, fontSize = MaterialTheme.typography.titleMedium.fontSize)

                                            Box(
                                                modifier = Modifier
                                                    .background(statusColor.copy(alpha = 0.12f), CircleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isPersian) "${due.daysLeft} روز" else "${due.daysLeft}d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = statusColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))

                                        Text(
                                            text = due.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(2.dp))

                                        val curr = if (currencyUnit == "IRR") 10 else 1
                                        Text(
                                            text = "${numberFormatter.format((due.amount * curr).toLong())} ${if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = onAddScreenClickDue,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPersian) "ساخت موعد پرداخت جدید" else "Create New Payment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------------------
            // بخش آخرین تراکنش‌ها (عدم نمایش در صورت خالی بودن)
            // ---------------------------------------------------------------------
            if (recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = if (isPersian) "آخرین تراکنش‌ها" else "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(recentTransactions, key = { it.id }) { transaction ->
                    val emoji = getCategoryEmoji(transaction.category)
                    val isExpense = transaction.type == "EXPENSE"
                    val displayAmount = if (currencyUnit == "IRR") (transaction.amount * 10).toLong() else transaction.amount.toLong()
                    val rowShape = RoundedCornerShape(20.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), rowShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), rowShape)
                            .clip(rowShape)
                            .clickable { onTransactionClick(transaction) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.title.ifEmpty { transaction.category },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DateUtils.formatTimestamp(transaction.timestamp, isPersian),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = "${if (isExpense) "-" else "+"}${numberFormatter.format(abs(displayAmount))} ${if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onSeeAllTransactionsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isPersian) "مشاهده همه تراکنش‌ها" else "See All Transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        // ---------------------------------------------------------------------
        // هدر بالای صفحه
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            val smallShape = RoundedCornerShape(24.dp)
            val centerShape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), smallShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                        .clip(smallShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = if (isPersian) "تغییر تم" else "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                        text = if (isPersian) "مدیریت هزینه‌ها" else "Expense Manager",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), smallShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), smallShape)
                        .clip(smallShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = if (isPersian) "اعلان‌ها" else "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Bottom Sheet اعلان‌ها
        // ---------------------------------------------------------------------
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                scrimColor = Color.Black.copy(alpha = 0.4f),
                dragHandle = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPersian) "اعلان‌ها و رویدادها" else "System Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isPersian) "۴ پیام جدید" else "4 New",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                        ) {
                            items(getDummyNotifications(), key = { it.id }) { item ->
                                val itemShape = RoundedCornerShape(18.dp)
                                val (icon, iconColor, bgColor) = when (item.type) {
                                    NotificationType.SUCCESS -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.08f))
                                    NotificationType.ERROR -> Triple(Icons.Default.Error, Color(0xFFE53935), Color(0xFFE53935).copy(alpha = 0.08f))
                                    NotificationType.REWARD -> Triple(Icons.Default.CardGiftcard, Color(0xFFFFB300), Color(0xFFFFB300).copy(alpha = 0.08f))
                                    NotificationType.SYSTEM -> Triple(Icons.Default.Info, Color(0xFF1E88E5), Color(0xFF1E88E5).copy(alpha = 0.08f))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), itemShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), itemShape)
                                        .clip(itemShape)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(bgColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isPersian) item.titleFa else item.titleEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isPersian) item.timeFa else item.timeEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (isPersian) item.descFa else item.descEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.15
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun getCategoryEmoji(category: String): String {
    return when (category.uppercase()) {
        "FOOD", "RESTAURANT" -> "🍔"
        "TRANSPORT", "CAR" -> "⛽"
        "SHOPPING" -> "🛍️"
        "BILL" -> "📄"
        "SALARY" -> "💰"
        "INVESTMENT" -> "📈"
        else -> "📌"
    }
}

private fun getDummyNotifications(): List<NotificationItem> = listOf(
    NotificationItem("n1", NotificationType.SUCCESS, "ثبت تراکنش موفقیت‌آمیز", "Transaction Registered", "تراکنش مربوط به خرید اینترنت با موفقیت در پایگاه داده ذخیره شد.", "Your internet purchase transaction has been successfully recorded.", "۱۰ دقیقه پیش", "10m ago"),
    NotificationItem("n2", NotificationType.REWARD, "جایزه چالش پس‌انداز ماهانه 🏆", "Savings Challenge Reward 🏆", "تبریک! کد تخفیف اختصاصی دریافت کردید.", "Congratulations! You received an exclusive discount code.", "۱ ساعت پیش", "1h ago"),
    NotificationItem("n3", NotificationType.ERROR, "هشدار عبور از سقف بودجه!", "Budget Threshold Alert!", "هزینه‌های رستوران به ۸۵٪ سقف رسیده است.", "Your Food expenses reached 85% of the limit.", "دیروز", "Yesterday"),
    NotificationItem("n4", NotificationType.SYSTEM, "بروزرسانی نسخه جدید", "New Feature Release", "نسخه جدید برنامه منتشر شد.", "Version 2.4 is live.", "۲ روز پیش", "2 days ago")
)