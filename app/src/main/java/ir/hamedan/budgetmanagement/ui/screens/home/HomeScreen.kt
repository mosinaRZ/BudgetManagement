package ir.hamedan.budgetmanagement.ui.screens.home

import ir.hamedan.budgetmanagement.di.appViewModel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.BalanceWidgetReceiver
import ir.hamedan.budgetmanagement.ui.screens.upcomings.UpcomingPaymentViewModel
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionViewModel
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.ui.screens.notification.NotificationViewModel
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
    transactionViewModel: TransactionViewModel = appViewModel(),
    upcomingViewModel: UpcomingPaymentViewModel = appViewModel(),
    goalsViewModel: SavingGoalsViewModel = appViewModel(),
    budgetViewModel: BudgetLimitViewModel = appViewModel(),
    pendingViewModel: PendingTransactionViewModel = appViewModel(),
    notificationViewModel: NotificationViewModel = appViewModel()
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

    val pendingTransactions by pendingViewModel.pendingTransactions.collectAsState()
    val pendingCount by pendingViewModel.pendingCount.collectAsState()

    var showPendingSheet by remember { mutableStateOf(false) }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val transactionsList by transactionViewModel.filteredTransactions.collectAsState()
    val paymentsList by upcomingViewModel.payments.collectAsState()
    val goalsList by goalsViewModel.savingGoals.collectAsState(initial = emptyList())
    val limitsList by budgetViewModel.budgetLimitsWithSpent.collectAsState(initial = emptyList())

    val currencyUnit by transactionViewModel.currencyUnit.collectAsState(initial = "IRT")

    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val categories by transactionViewModel.expenseCategories.collectAsState()

    // چک کردن وضعیت ویجت (قابل آپدیت)
    var isWidgetAdded by remember {
        mutableStateOf(
            run {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, BalanceWidgetReceiver::class.java)
                )
                widgetIds.isNotEmpty()
            }
        )
    }

// هر بار که صفحه resume می‌شود دوباره چک می‌کنیم (بعد از پین کردن ویجت)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, BalanceWidgetReceiver::class.java)
                )
                isWidgetAdded = widgetIds.isNotEmpty()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // بعد از گرفتن paymentsList
    LaunchedEffect(paymentsList) {
        val now = System.currentTimeMillis()
        val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
        val calendarDue = Calendar.getInstance()

        paymentsList.filter { !it.isPaid }.forEach { payment ->
            calendarDue.timeInMillis = payment.dueDate

            // فقط روز و ماه و سال را مقایسه می‌کنیم (ساعت را نادیده می‌گیریم)
            calendarNow.set(Calendar.HOUR_OF_DAY, 0)
            calendarNow.set(Calendar.MINUTE, 0)
            calendarNow.set(Calendar.SECOND, 0)
            calendarNow.set(Calendar.MILLISECOND, 0)

            calendarDue.set(Calendar.HOUR_OF_DAY, 0)
            calendarDue.set(Calendar.MINUTE, 0)
            calendarDue.set(Calendar.SECOND, 0)
            calendarDue.set(Calendar.MILLISECOND, 0)

            val daysLeft = ((calendarDue.timeInMillis - calendarNow.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

            if (daysLeft in listOf(1, 3, 7)) {
                notificationViewModel.addNotification(
                    NotificationEntity(
                        type = "WARNING",
                        titleFa = "یادآوری سررسید",
                        titleEn = "Payment Due Reminder",
                        descFa = "${payment.title} تا $daysLeft روز دیگر سررسید دارد.",
                        descEn = "${payment.title} is due in $daysLeft day(s).",
                        tag = "DUE_${payment.id}_${daysLeft}"
                    )
                )
            }
        }
    }

    // تراز کلی حساب (بدون هیچ فیلتر زمانی) - مجموع تمام تراکنش‌ها
    val totalAllIncome = transactionsList.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalAllExpense = transactionsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalBalance = totalAllIncome - totalAllExpense

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
            item {
                // اگر CTA هست ارتفاع بیشتر می‌دهیم تا محتوا زیر CTA نره
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(if (isWidgetAdded) 55.dp else 110.dp)
                )
            }
            // ---------------------------------------------------------------------
            // کارت بالانس اصلی (تراز کلی حساب)
            // ---------------------------------------------------------------------
            item {
                val balanceShape = RoundedCornerShape(24.dp)

                val periodLabel = if (isPersian) {
                    "تراز کلی حساب"
                } else {
                    "Total Account Balance"
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

                        Spacer(Modifier.height(24.dp))

                        val displayBalance = if (currencyUnit == "IRR") (totalBalance * 10).toLong() else totalBalance.toLong()
                        val currencyText = if (isPersian) {
                            if (currencyUnit == "IRR") "ریال" else "تومان"
                        } else {
                            if (currencyUnit == "IRR") "Rial" else "Toman"
                        }

                        // محاسبه علامت منفی در صورت منفی بودن بالانس
                        val formattedAmount = numberFormatter.format(abs(displayBalance))
                        val sign = if (totalBalance < 0) "-" else ""

                        Text(
                            text = "$sign$formattedAmount $currencyText",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = if (totalBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

// ---------------------------------------------------------------------
// خلاصه درآمد و هزینه
// ---------------------------------------------------------------------
            item {
                val summaryCardShape = RoundedCornerShape(20.dp)
                val income = totalAllIncome
                val expense = totalAllExpense

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
                                text = if (isPersian) "درآمد" else "Income",
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
                                text = if (isPersian) "هزینه" else "Expense",
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
                                        text = "${goal.icon} ${goal.title} (${(progress * 100).toInt()}%)",
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
            // بخش موعد سررسید
            // ---------------------------------------------------------------------
            item {
                val now = System.currentTimeMillis()
                val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
                val calendarDue = Calendar.getInstance()

                // فقط روز و ماه و سال را مقایسه می‌کنیم
                calendarNow.set(Calendar.HOUR_OF_DAY, 0)
                calendarNow.set(Calendar.MINUTE, 0)
                calendarNow.set(Calendar.SECOND, 0)
                calendarNow.set(Calendar.MILLISECOND, 0)

                val dueItems = upcomingPayments.map { payment ->
                    calendarDue.timeInMillis = payment.dueDate
                    calendarDue.set(Calendar.HOUR_OF_DAY, 0)
                    calendarDue.set(Calendar.MINUTE, 0)
                    calendarDue.set(Calendar.SECOND, 0)
                    calendarDue.set(Calendar.MILLISECOND, 0)

                    val daysLeft = ((calendarDue.timeInMillis - calendarNow.timeInMillis) / (24 * 60 * 60 * 1000))
                        .toInt()
                        .coerceAtLeast(0)

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
                                        text = if (isPersian) "موعد پرداخت‌ها" else "Upcoming Payments",
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
            // بخش آخرین تراکنش‌ها
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
                        text = if (isPersian) "سیدنا" else "Cidna",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showPendingSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Message, // آیکون پیامک
                            contentDescription = if (isPersian) "تراکنش‌های در انتظار" else "Pending Transactions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (pendingCount > 9) "9+" else pendingCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

                    // Badge تعداد پیام‌های خوانده نشده
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
// CTA اضافه کردن ویجت (فقط وقتی ویجت اضافه نشده باشد)
// ---------------------------------------------------------------------
        if (!isWidgetAdded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
                    .padding(horizontal = 24.dp)
            ) {
                val capsuleShape = RoundedCornerShape(50)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            shape = capsuleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = capsuleShape
                        )
                        .clip(capsuleShape)
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                val provider = ComponentName(context, BalanceWidgetReceiver::class.java)

                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(provider, null, null)
                                } else {
                                    Toast.makeText(
                                        context,
                                        if (isPersian) "لطفاً از منوی ویجت‌ها، ویجت سیدنا را اضافه کنید"
                                        else "Please add Cidna widget from the widget menu",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isPersian) "لطفاً از منوی ویجت‌ها، ویجت سیدنا را اضافه کنید"
                                    else "Please add Cidna widget from the widget menu",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) "ویجت موجودی را اضافه کنید" else "Add Balance Widget",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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

                        // دستگیره کشیدن
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

                        // عنوان + تعداد پیام جدید + دکمه «همه را خواندم»
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isPersian) "$unreadCount پیام جدید" else "$unreadCount New",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // دکمه همه را خواندم
                                if (unreadCount > 0) {
                                    TextButton(
                                        onClick = { notificationViewModel.markAllAsRead() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isPersian) "همه را خواندم" else "Mark all",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        if (notifications.isEmpty()) {
                            // حالت خالی بودن لیست
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = if (isPersian) "اعلانی وجود ندارد" else "No notifications yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                            ) {
                                items(notifications, key = { it.id }) { item ->
                                    // وقتی این آیتم رندر شد/دیده شد، اگر خوانده نشده بود علامت‌گذاری می‌شود
                                    LaunchedEffect(item.id, item.isRead) {
                                        if (!item.isRead) {
                                            notificationViewModel.markAsRead(item.id)
                                        }
                                    }

                                    val itemShape = RoundedCornerShape(18.dp)

                                    // رنگ و آیکون بر اساس type
                                    val (icon, iconColor) = when (item.type.uppercase()) {
                                        "SUCCESS" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                                        "ERROR" -> Icons.Default.Error to Color(0xFFF44336)
                                        "WARNING" -> Icons.Default.Warning to Color(0xFFFF9800)
                                        "REWARD" -> Icons.Default.CardGiftcard to Color(0xFF9C27B0)
                                        else -> Icons.Default.Info to MaterialTheme.colorScheme.primary // SYSTEM
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (!item.isRead)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                                else
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                                itemShape
                                            )
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                                itemShape
                                            )
                                            .clip(itemShape)
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // آیکون رنگی
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    iconColor.copy(alpha = 0.12f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = iconColor,
                                                modifier = Modifier.size(22.dp)
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
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                // نشانگر خوانده‌نشده
                                                if (!item.isRead) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary,
                                                                CircleShape
                                                            )
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                text = if (isPersian) item.descFa else item.descEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
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

        if (showPendingSheet) {
            PendingTransactionsBottomSheet(
                pendingList = pendingTransactions,
                categories = categories,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                onDismiss = { showPendingSheet = false },
                onConfirmFinal = { pending, title, amount, category, isExpense, note ->
                    pendingViewModel.confirmTransaction(
                        pending = pending,
                        title = title,
                        amount = amount,
                        category = category,
                        isExpense = isExpense,
                        note = note
                    )
                },
                onIgnore = { pending ->
                    pendingViewModel.ignoreTransaction(pending)
                }
            )
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