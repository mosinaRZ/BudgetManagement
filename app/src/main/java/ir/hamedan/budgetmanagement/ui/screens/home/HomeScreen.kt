package ir.hamedan.budgetmanagement.ui.screens.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.DueReminderPreferences
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.appSkeletonShimmer
import ir.hamedan.budgetmanagement.ui.components.BalanceWidgetReceiver
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesViewModel
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import ir.hamedan.budgetmanagement.ui.screens.notification.NotificationViewModel
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionViewModel
import ir.hamedan.budgetmanagement.utils.AppHapticType
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.rememberAppHaptics
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.screens.debtCredit.DebtCreditViewModel
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

data class HomeDueItem(
    val id: String,
    val title: String,
    val amount: Long,
    val daysLeft: Int,
    val type: String
)

// ===== حریم خصوصی مبالغ (نمایش/عدم‌نمایش تراز، درآمد و هزینه) =====
private const val PRIVACY_PREFS_NAME = "home_privacy_prefs"
private const val KEY_AMOUNTS_HIDDEN = "key_amounts_hidden"

/**
 * جایگزینی مبلغ نمایشی با الگوی ستاره‌ای هنگام فعال بودن حالت حریم خصوصی.
 * تعداد ستاره‌ها بر اساس تعداد ارقام مبلغ واقعی تنظیم می‌شود تا حس طبیعی حفظ شود.
 */
private fun maskedStars(realLength: Int): String {
    val starCount = realLength.coerceIn(4, 9)
    return "*".repeat(starCount)
}

/**
 * فرمت فشرده‌ی مبلغ برای زمانی که عدد کامل حتی با کوچک‌ترین اندازه‌ی فونت هم در کارت جا نمی‌شود.
 * مثال: ۱۲۵٬۰۰۰٬۰۰۰٬۰۰۰ ← «۱۲۵ میلیارد» / "125 B"
 */
private fun compactAmount(value: Long, isPersian: Boolean): String {
    val absValue = abs(value).toDouble()
    val locale = if (isPersian) Locale("fa", "IR") else Locale.US
    val formatter = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
    val (divisor, suffix) = when {
        absValue >= 1e12 -> 1e12 to (if (isPersian) "تریلیون" else "Tn")
        absValue >= 1e9 -> 1e9 to (if (isPersian) "میلیارد" else "B")
        absValue >= 1e6 -> 1e6 to (if (isPersian) "میلیون" else "M")
        absValue >= 1e3 -> 1e3 to (if (isPersian) "هزار" else "K")
        else -> return formatter.format(absValue)
    }
    return "${formatter.format(absValue / divisor)} $suffix"
}

/**
 * متن مبلغِ تک‌خطی که خودش را با عرض موجود تنظیم می‌کند:
 * ۱) اندازه‌ی فونت تا حداقل مجاز کوچک می‌شود،
 * ۲) اگر هنوز جا نشد، متن فشرده (fallbackText) نمایش داده می‌شود.
 * تا زمان نهایی شدن اندازه، متن رسم نمی‌شود تا پرش/چشمک دیده نشود.
 */
@Composable
private fun AutoFitAmountText(
    text: String,
    fallbackText: String?,
    style: TextStyle,
    color: Color,
    maxFontSize: Float,
    minFontSize: Float,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    var fontSize by remember(text, fallbackText, maxFontSize) { mutableFloatStateOf(maxFontSize) }
    var useFallback by remember(text, fallbackText, maxFontSize) { mutableStateOf(false) }
    var isReady by remember(text, fallbackText, maxFontSize) { mutableStateOf(false) }

    Text(
        text = if (useFallback && fallbackText != null) fallbackText else text,
        modifier = modifier.drawWithContent { if (isReady) drawContent() },
        style = style.copy(fontSize = fontSize.sp, letterSpacing = letterSpacing),
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (!result.didOverflowWidth) {
                isReady = true
            } else if (fontSize > minFontSize + 0.1f) {
                fontSize = maxOf(minFontSize, fontSize * 0.92f)
            } else if (!useFallback && fallbackText != null) {
                useFallback = true
                fontSize = maxFontSize
            } else {
                isReady = true
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTransactionClick: (TransactionEntity) -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onSeeAllTransactionsClick: () -> Unit = {},
    onAddScreenClickDebt: () -> Unit = {},
    onAddScreenClickPiggy: () -> Unit = {},
    onAddScreenClickLimit: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    transactionViewModel: TransactionViewModel = appViewModel(),
    goalsViewModel: SavingGoalsViewModel = appViewModel(),
    budgetViewModel: BudgetLimitViewModel = appViewModel(),
    pendingViewModel: PendingTransactionViewModel = appViewModel(),
    notificationViewModel: NotificationViewModel = appViewModel(),
    debtCreditViewModel: DebtCreditViewModel = appViewModel(),
    categoriesViewModel: CategoriesViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = LocaleHelper.getLanguage(context) == "fa"
    val haptic = rememberAppHaptics()

    // ===== وضعیت مخفی/نمایان بودن مبالغ (تراز کلی، درآمد و هزینه) با SharedPreferences =====
    val privacyPrefs = remember {
        context.getSharedPreferences(PRIVACY_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }
    var isAmountsHidden by remember {
        mutableStateOf(privacyPrefs.getBoolean(KEY_AMOUNTS_HIDDEN, false))
    }
    // یک انیمیشن کوچک «چشمک» روی آیکون به هنگام هر تغییر وضعیت
    var eyeToggleTrigger by remember { mutableIntStateOf(0) }
    val eyeBlinkScale = remember { Animatable(1f) }

    fun toggleAmountsVisibility() {
        haptic.perform(AppHapticType.LongPress)
        isAmountsHidden = !isAmountsHidden
        privacyPrefs.edit().putBoolean(KEY_AMOUNTS_HIDDEN, isAmountsHidden).apply()
        eyeToggleTrigger++
    }

    LaunchedEffect(eyeToggleTrigger) {
        if (eyeToggleTrigger > 0) {
            // شبیه‌سازی «چشمک زدن» چشم: جمع شدن سریع و باز شدن دوباره
            eyeBlinkScale.animateTo(0.55f, animationSpec = tween(90, easing = FastOutSlowInEasing))
            eyeBlinkScale.animateTo(1f, animationSpec = tween(140, easing = FastOutSlowInEasing))
        }
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(
                context,
                if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val pendingTransactions by pendingViewModel.pendingTransactions.collectAsState()
    val pendingCount by pendingViewModel.pendingCount.collectAsState()

    var showPendingSheet by remember { mutableStateOf(false) }

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    val transactionsList by transactionViewModel.filteredTransactions.collectAsState()
    val goalsList by goalsViewModel.savingGoals.collectAsState(initial = null)
    val limitsList by budgetViewModel.budgetLimitsWithSpent.collectAsState(initial = null)
    val categoriesList by categoriesViewModel.categories.collectAsState()
    val debtCreditList by debtCreditViewModel.debtCreditList.collectAsState()

    val currencyUnit by transactionViewModel.currencyUnit.collectAsState(initial = "IRT")

    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val expensePendingCategories by pendingViewModel.expenseCategories.collectAsState()
    val incomePendingCategories by pendingViewModel.incomeCategories.collectAsState()
    val allPendingCategories = remember(expensePendingCategories, incomePendingCategories) {
        expensePendingCategories + incomePendingCategories
    }

    // بررسی وضعیت بارگذاری کلی (اگر یکی از لیست‌های اصلی null باشد، در حالت Loading قرار می‌گیرد)
    val isLoading = categoriesList == null || goalsList == null || limitsList == null

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

    // تراز کلی حساب
    val totalAllIncome = transactionsList.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalAllExpense = transactionsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalBalance = totalAllIncome - totalAllExpense

    val recentTransactions = remember(transactionsList) {
        transactionsList.sortedByDescending { it.timestamp }.take(3)
    }

    // فقط موارد فعال در داشبورد خانه نمایش داده می‌شوند:
    // قلک‌های تکمیل‌نشده، محدودیت‌های فعالِ داخل بازه و بدهی/طلب‌های تسویه‌نشده.
    val now = System.currentTimeMillis()
    val allGoals = remember(goalsList) {
        (goalsList ?: emptyList()).filter { it.currentAmount < it.targetAmount }
    }
    val allLimits = remember(limitsList, now) {
        (limitsList ?: emptyList()).filter { limit ->
            limit.entity.isActive && now in limit.entity.startDate..limit.entity.endDate
        }
    }
    val activeDebtCreditList = remember(debtCreditList) {
        debtCreditList.filter { it.paidAmount < it.totalAmount && !it.isSettled }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // ===== یادآوری سررسید بدهی/طلب =====
    // به محض ورود کاربر به این صفحه، هر آیتم بدهی/طلبی که سررسیدش گذشته و هنوز
    // تسویه نشده، صف می‌شود تا دیالوگ یادآوری برایش نمایش داده شود (یکی‌یکی).
    var hasCapturedDueReminders by remember { mutableStateOf(false) }
    var currentDueReminder by remember { mutableStateOf<DebtCreditEntity?>(null) }
    var pendingDueReminders by remember { mutableStateOf<List<DebtCreditEntity>>(emptyList()) }
    var dueReminderTotal by remember { mutableIntStateOf(0) }
    var showChangeDueDateDialog by remember { mutableStateOf(false) }

    fun goToNextDueReminder() {
        currentDueReminder = pendingDueReminders.firstOrNull()
        pendingDueReminders = pendingDueReminders.drop(1)
    }

    LaunchedEffect(debtCreditList) {
        if (!hasCapturedDueReminders && debtCreditList.isNotEmpty() && !DueReminderPreferences.hasShownOnce(context)) {
            hasCapturedDueReminders = true
            DueReminderPreferences.markShownOnce(context)

            val now = System.currentTimeMillis()
            val threeDaysAheadMillis = now + 3 * 24 * 60 * 60 * 1000L
            // شامل هم بدهی/طلب‌های سررسیدگذشته و هم آن‌هایی که تا ۳ روز آینده سررسید می‌شوند
            val relevant = debtCreditList
                .filter { !it.isSettled && it.dueDateMillis > 0 && it.dueDateMillis <= threeDaysAheadMillis }
                .sortedBy { it.dueDateMillis }
            if (relevant.isNotEmpty()) {
                dueReminderTotal = relevant.size
                currentDueReminder = relevant.first()
                pendingDueReminders = relevant.drop(1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(if (isWidgetAdded) 55.dp else 110.dp)
                )
            }

            if (isLoading) {
                // =============== نمایش حالت Skeleton کامل صفحه ===============

                // 1. کارت بالانس اصلی
                item {
                    val balanceShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), balanceShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), balanceShape)
                            .clip(balanceShape)
                            .padding(20.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).appSkeletonShimmer())
                            Spacer(Modifier.height(28.dp))
                            Box(modifier = Modifier.width(180.dp).height(28.dp).clip(RoundedCornerShape(6.dp)).appSkeletonShimmer())
                        }
                    }
                }

                // 2. خلاصه درآمد و هزینه
                item {
                    val summaryCardShape = RoundedCornerShape(20.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), summaryCardShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), summaryCardShape)
                                    .clip(summaryCardShape)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(50.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).appSkeletonShimmer())
                                    Spacer(Modifier.height(8.dp))
                                    Box(modifier = Modifier.width(80.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).appSkeletonShimmer())
                                }
                            }
                        }
                    }
                }

                // 3. قلک‌ها
                item {
                    val cardShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), cardShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), cardShape)
                            .padding(20.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).appSkeletonShimmer())
                            Spacer(Modifier.height(20.dp))
                            repeat(2) {
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).appSkeletonShimmer())
                                Spacer(Modifier.height(16.dp))
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).appSkeletonShimmer())
                        }
                    }
                }

                // 4. بدهی‌ها
                item {
                    val cardShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), cardShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), cardShape)
                            .padding(20.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.width(100.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).appSkeletonShimmer())
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(2) {
                                    Box(modifier = Modifier.width(140.dp).height(80.dp).clip(RoundedCornerShape(16.dp)).appSkeletonShimmer())
                                }
                            }
                        }
                    }
                }

                // 5. آخرین تراکنش‌ها
                item {
                    Box(modifier = Modifier.width(130.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).appSkeletonShimmer())
                }
                items(3) {
                    RecentTransactionSkeletonItem()
                }

            } else {
                // =============== نمایش محتوای اصلی بعد از بارگذاری کامل ===============

                // کارت بالانس اصلی
                item {
                    val balanceShape = RoundedCornerShape(24.dp)
                    val periodLabel = if (isPersian) "تراز کلی حساب" else "Total Account Balance"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), balanceShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), balanceShape)
                            .clip(balanceShape)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = periodLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )

                                // آیکون چشمی برای مخفی/نمایان‌سازی مبالغ (ذخیره در SharedPreferences)
                                IconButton(
                                    onClick = { toggleAmountsVisibility() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .scale(eyeBlinkScale.value)
                                ) {
                                    AnimatedContent(
                                        targetState = isAmountsHidden,
                                        transitionSpec = {
                                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(180)))
                                                .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)))
                                        },
                                        label = "eye_icon"
                                    ) { hidden ->
                                        Icon(
                                            imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (isPersian) {
                                                if (hidden) "نمایش مبالغ" else "مخفی کردن مبالغ"
                                            } else {
                                                if (hidden) "Show amounts" else "Hide amounts"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            val displayBalance = if (currencyUnit == "IRR") totalBalance * 10L else totalBalance
                            val currencyText = if (isPersian) {
                                if (currencyUnit == "IRR") "ریال" else "تومان"
                            } else {
                                if (currencyUnit == "IRR") "Rial" else "Toman"
                            }

                            val formattedAmount = numberFormatter.format(abs(displayBalance))
                            val sign = if (totalBalance < 0) "-" else ""
                            val fullBalanceText = "$sign$formattedAmount $currencyText"
                            val balanceColor = if (totalBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                            val compactBalanceText = "$sign${compactAmount(displayBalance, isPersian)} $currencyText"

                            AnimatedContent(
                                targetState = isAmountsHidden,
                                modifier = Modifier.fillMaxWidth(),
                                transitionSpec = {
                                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.92f, animationSpec = tween(160)))
                                },
                                label = "balance_amount"
                            ) { hidden ->
                                AutoFitAmountText(
                                    text = if (hidden) "${maskedStars(formattedAmount.length)} $currencyText" else fullBalanceText,
                                    fallbackText = if (hidden) null else compactBalanceText,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = balanceColor,
                                    maxFontSize = 28f,
                                    minFontSize = 16f,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                    letterSpacing = if (hidden) 2.sp else 0.sp
                                )
                            }
                        }
                    }
                }

                // خلاصه درآمد و هزینه
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = if (isPersian) "درآمد" else "Income", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                val displayIncome = if (currencyUnit == "IRR") income * 10L else income
                                val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "R" else "T")
                                val formattedIncome = numberFormatter.format(displayIncome)
                                AnimatedContent(
                                    targetState = isAmountsHidden,
                                    modifier = Modifier.fillMaxWidth(),
                                    transitionSpec = {
                                        (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                                            .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.92f, animationSpec = tween(160)))
                                    },
                                    label = "income_amount"
                                ) { hidden ->
                                    AutoFitAmountText(
                                        text = if (hidden) "${maskedStars(formattedIncome.length)} $currencyText" else "$formattedIncome $currencyText",
                                        fallbackText = if (hidden) null else "${compactAmount(displayIncome, isPersian)} $currencyText",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxFontSize = 16f,
                                        minFontSize = 10f,
                                        modifier = Modifier.fillMaxWidth(),
                                        letterSpacing = if (hidden) 1.5.sp else 0.sp
                                    )
                                }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = if (isPersian) "هزینه" else "Expense", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                val displayExpense = if (currencyUnit == "IRR") expense * 10L else expense
                                val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "R" else "T")
                                val formattedExpense = numberFormatter.format(displayExpense)
                                AnimatedContent(
                                    targetState = isAmountsHidden,
                                    modifier = Modifier.fillMaxWidth(),
                                    transitionSpec = {
                                        (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                                            .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.92f, animationSpec = tween(160)))
                                    },
                                    label = "expense_amount"
                                ) { hidden ->
                                    AutoFitAmountText(
                                        text = if (hidden) "${maskedStars(formattedExpense.length)} $currencyText" else "$formattedExpense $currencyText",
                                        fallbackText = if (hidden) null else "${compactAmount(displayExpense, isPersian)} $currencyText",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                        maxFontSize = 16f,
                                        minFontSize = 10f,
                                        modifier = Modifier.fillMaxWidth(),
                                        letterSpacing = if (hidden) 1.5.sp else 0.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // قلک‌های پس‌انداز
                item {
                    val piggyShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), piggyShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), piggyShape)
                            .clip(piggyShape)
                    ) {

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
                                    Icon(painterResource(R.drawable.ic_goal), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(text = if (isPersian) "قلک‌های پس‌انداز" else "Savings Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = if (isPersian) "پیشرفت هدف‌های مالی شما" else "Your financial targets progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            allGoals.forEach { goal ->
                                val progress = if (goal.targetAmount > 0L) (goal.currentAmount.toDouble() / goal.targetAmount.toDouble()).toFloat() else 0f
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "${goal.icon} ${goal.title} (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                        val curr = if (currencyUnit == "IRR") 10L else 1L
                                        Text(text = "${numberFormatter.format(goal.currentAmount * curr)} / ${numberFormatter.format(goal.targetAmount * curr)} ${if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            if (allGoals.isEmpty()) {
                                Text(
                                    text = if (isPersian) "قلک فعالی ندارید" else "No active savings goals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            Button(
                                onClick = onAddScreenClickPiggy,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = if (isPersian) "ساخت یک قلک جدید" else "Create a New Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // محدودیت‌های خرج‌کرد
                item {
                    val budgetShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), budgetShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), budgetShape)
                            .clip(budgetShape)
                    ) {
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
                                    Icon(painterResource(R.drawable.ic_limit), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(text = if (isPersian) "محدودیت‌های خرج‌کرد" else "Expense Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = if (isPersian) "مدیریت سقف بودجه دسته‌ها" else "Manage category budget ceilings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            allLimits.forEach { limit ->
                                val progress = if (limit.entity.maxLimit > 0L) (limit.currentSpent.toDouble() / limit.entity.maxLimit.toDouble()).toFloat() else 0f
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val categories = categoriesList.orEmpty()

                                        val categoryTitle = categories
                                            .find { it.id == limit.entity.categoryId }
                                            ?.title
                                            .orEmpty()

                                        val categoryName = StringMapper.getCategoryName(
                                            categoryTitle,
                                            isPersian
                                        )
                                        Text(
                                            text = "${limit.categoryEmoji} $categoryName (${(progress * 100).toInt()}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val curr = if (currencyUnit == "IRR") 10L else 1L
                                        Text(text = "${numberFormatter.format(limit.currentSpent * curr)} / ${numberFormatter.format(limit.entity.maxLimit * curr)} ${if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")}", style = MaterialTheme.typography.labelMedium, color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

                            if (allLimits.isEmpty()) {
                                Text(
                                    text = if (isPersian) "محدودیت خرج‌کرد فعالی ندارید" else "No active expense limits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            Button(
                                onClick = onAddScreenClickLimit,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = if (isPersian) "تنظیم محدودیت جدید" else "Set a New Budget Limit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // بخش بدهی و طلب‌ها
                item {
                    val dueShape = RoundedCornerShape(24.dp)

                    // فقط موارد فعال، مرتب‌شده بر اساس نزدیک‌ترین سررسید
                    val dueSorted = remember(activeDebtCreditList) {
                        activeDebtCreditList.sortedBy { daysUntilDue(it) }
                    }
                    val debtRemaining = remember(activeDebtCreditList) {
                        activeDebtCreditList.filter { it.type == "DEBT" }
                            .sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0L) }
                    }
                    val creditRemaining = remember(activeDebtCreditList) {
                        activeDebtCreditList.filter { it.type != "DEBT" }
                            .sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0L) }
                    }
                    val debtCount = activeDebtCreditList.count { it.type == "DEBT" }
                    val creditCount = activeDebtCreditList.size - debtCount
                    val curr = if (currencyUnit == "IRR") 10L else 1L
                    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "T")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), dueShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), dueShape)
                            .clip(dueShape)
                    ) {
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
                                    Icon(painterResource(R.drawable.ic_debtcredit), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(text = if (isPersian) "بدهی و طلب‌ها" else "Debts & Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = if (isPersian) "مدیریت اقساط و بدهی‌های شخصی" else "Manage personal debts and credits", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            if (dueSorted.isNotEmpty()) {
                                // خلاصه: مجموع مانده‌ی بدهی‌ها و طلب‌ها
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf(
                                        Triple(true, debtRemaining, debtCount),
                                        Triple(false, creditRemaining, creditCount)
                                    ).forEach { (isDebtTile, remaining, count) ->
                                        val tileColor = if (isDebtTile) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        val tileShape = RoundedCornerShape(16.dp)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(tileColor.copy(alpha = 0.08f), tileShape)
                                                .border(1.dp, tileColor.copy(alpha = 0.18f), tileShape)
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = if (isDebtTile) "💸" else "💰", style = MaterialTheme.typography.labelLarge)
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (isPersian) (if (isDebtTile) "مانده بدهی" else "مانده طلب") else (if (isDebtTile) "Debt left" else "Credit left"),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = numberFormatter.format(remaining * curr),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = tileColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isPersian) "$currencyText • ${numberFormatter.format(count.toLong())} مورد" else "$currencyText • $count item${if (count == 1) "" else "s"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(dueSorted, key = { it.id }) { debt ->
                                        val daysLeft = daysUntilDue(debt)
                                        val isDebt = debt.type == "DEBT"
                                        val typeColor = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        val icon = if (isDebt) "💸" else "💰"
                                        val typeFa = if (isDebt) "بدهی" else "طلب"
                                        val typeEn = if (isDebt) "Debt" else "Credit"

                                        val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
                                        val paidFraction = if (debt.totalAmount > 0L)
                                            (debt.paidAmount.toDouble() / debt.totalAmount.toDouble()).toFloat().coerceIn(0f, 1f)
                                        else 0f
                                        val paidPercent = (paidFraction * 100).toInt()

                                        val isUrgent = daysLeft <= 7
                                        val dueColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        val dueText = when {
                                            daysLeft < 0 -> if (isPersian) "${numberFormatter.format((-daysLeft).toLong())} روز گذشته" else "${-daysLeft}d overdue"
                                            daysLeft == 0 -> if (isPersian) "امروز سررسید" else "Due today"
                                            else -> if (isPersian) "${numberFormatter.format(daysLeft.toLong())} روز مونده" else "${daysLeft}d left"
                                        }

                                        val cardShape = RoundedCornerShape(16.dp)
                                        Box(
                                            modifier = Modifier
                                                .width(190.dp)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), cardShape)
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
                                                .clip(cardShape)
                                        ) {
                                            // نوار رنگی نوع (بدهی/طلب) در لبه‌ی بالای کارت
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .background(typeColor.copy(alpha = 0.7f))
                                            )
                                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 15.dp, bottom = 12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = icon, fontSize = MaterialTheme.typography.titleMedium.fontSize)

                                                    Box(
                                                        modifier = Modifier
                                                            .background(typeColor.copy(alpha = 0.12f), CircleShape)
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(text = if (isPersian) typeFa else typeEn, style = MaterialTheme.typography.labelSmall, color = typeColor, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Spacer(Modifier.height(10.dp))

                                                Text(text = debt.personName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                                Spacer(Modifier.height(8.dp))

                                                Text(text = if (isPersian) "باقی‌مانده" else "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = "${numberFormatter.format(remaining * curr)} $currencyText",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = typeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (isPersian) "از ${numberFormatter.format(debt.totalAmount * curr)}" else "of ${numberFormatter.format(debt.totalAmount * curr)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(Modifier.height(10.dp))

                                                LinearProgressIndicator(
                                                    progress = { paidFraction },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                                    color = typeColor,
                                                    trackColor = typeColor.copy(alpha = 0.1f)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = if (isPersian) "${numberFormatter.format(paidPercent.toLong())}٪ ${if (isDebt) "پرداخت شده" else "دریافت شده"}" else "$paidPercent% ${if (isDebt) "paid" else "received"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Spacer(Modifier.height(10.dp))

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(dueColor.copy(alpha = if (isUrgent) 0.12f else 0.08f), CircleShape)
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(text = "⏳ $dueText", style = MaterialTheme.typography.labelSmall, color = dueColor, fontWeight = FontWeight.Bold)
                                                    }
                                                    if (debt.isMonthly) {
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(text = "🔁", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))
                            } else {
                                Text(
                                    text = if (isPersian) "بدهی یا طلب فعالی ندارید" else "No active debts or credits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            Button(
                                onClick = onAddScreenClickDebt,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = if (isPersian) "ثبت بدهی/طلب جدید" else "Add New Debt or Credit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // آخرین تراکنش‌ها (اگر هیچ تراکنشی ثبت نشده باشد، کل این بخش نمایش داده نمی‌شود)
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
                        val categories = categoriesList.orEmpty()

                        val emoji = getCategoryEmoji(
                            categories.find { it.id == transaction.categoryId }?.title.orEmpty(),
                            categories
                        )
                        val isExpense = transaction.type == "EXPENSE"
                        val displayAmount = if (currencyUnit == "IRR") transaction.amount * 10L else transaction.amount
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
                                val categories = categoriesList.orEmpty()
                                Text(
                                    text = transaction.title.ifEmpty {
                                        categories.find { it.id == transaction.categoryId }?.title.orEmpty()
                                    },
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
                                Text(text = if (isPersian) "مشاهده همه تراکنش‌ها" else "See All Transactions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
        }

        // هدر بالای صفحه
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
                        Icon(imageVector = Icons.Default.Brightness6, contentDescription = if (isPersian) "تغییر تم" else "Toggle Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(text = if (isPersian) "سیدنا" else "Cidna", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                        Icon(imageVector = Icons.Default.Message, contentDescription = if (isPersian) "تراکنش‌های در انتظار" else "Pending Transactions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(text = if (pendingCount > 9) "9+" else pendingCount.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White, fontWeight = FontWeight.Bold)
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
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = if (isPersian) "اعلان‌ها" else "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (unreadCount > 9) "9+" else unreadCount.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CTA ویجت
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
                        .background(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f), shape = capsuleShape)
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), shape = capsuleShape)
                        .clip(capsuleShape)
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                val provider = ComponentName(context, BalanceWidgetReceiver::class.java)

                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(provider, null, null)
                                } else {
                                    Toast.makeText(context, if (isPersian) "لطفاً از منوی ویجت‌ها، ویجت سیدنا را اضافه کنید" else "Please add Cidna widget from the widget menu", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, if (isPersian) "لطفاً از منوی ویجت‌ها، ویجت سیدنا را اضافه کنید" else "Please add Cidna widget from the widget menu", Toast.LENGTH_LONG).show()
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(painterResource(R.drawable.ic_widget), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (isPersian) "ویجت موجودی را اضافه کنید" else "Add Balance Widget", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // Bottom Sheet اعلان‌ها
        if (showBottomSheet) {
            // زمان نسبی اعلان‌ها؛ کوچک و داخل کادر هر اعلان نمایش داده می‌شود.
            var notificationNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(showBottomSheet) {
                while (showBottomSheet) {
                    notificationNow = System.currentTimeMillis()
                    kotlinx.coroutines.delay(30_000L)
                }
            }

            fun relativeNotificationTime(timestamp: Long): String {
                val elapsed = (notificationNow - timestamp).coerceAtLeast(0L)
                val minute = 60_000L
                val hour = 60L * minute
                val day = 24L * hour
                val week = 7L * day
                return when {
                    elapsed < minute -> if (isPersian) "همین حالا" else "Just now"
                    elapsed < hour -> {
                        val n = elapsed / minute
                        if (isPersian) "$n دقیقه پیش" else "$n min ago"
                    }
                    elapsed < day -> {
                        val n = elapsed / hour
                        if (isPersian) "$n ساعت پیش" else "$n hr ago"
                    }
                    elapsed < week -> {
                        val n = elapsed / day
                        if (isPersian) "$n روز پیش" else "$n day${if (n == 1L) "" else "s"} ago"
                    }
                    else -> {
                        val n = elapsed / week
                        if (isPersian) "$n هفته پیش" else "$n week${if (n == 1L) "" else "s"} ago"
                    }
                }
            }

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
                        .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f), shape = RoundedCornerShape(32.dp))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape = RoundedCornerShape(32.dp))
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
                                    .background(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isPersian) "اعلان‌ها و رویدادها" else "System Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = if (isPersian) "$unreadCount پیام جدید" else "$unreadCount New", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (unreadCount > 0) {
                                    TextButton(
                                        onClick = { notificationViewModel.markAllAsRead() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = if (isPersian) "همه را خواندم" else "Mark all", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        if (notifications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(text = if (isPersian) "اعلانی وجود ندارد" else "No notifications yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                                    LaunchedEffect(item.id, item.isRead) {
                                        if (!item.isRead) notificationViewModel.markAsRead(item.id)
                                    }

                                    val itemShape = RoundedCornerShape(18.dp)
                                    val (icon, iconColor) = when (item.type.uppercase()) {
                                        "SUCCESS" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                                        "ERROR" -> Icons.Default.Error to Color(0xFFF44336)
                                        "WARNING" -> Icons.Default.Warning to Color(0xFFFF9800)
                                        "REWARD" -> Icons.Default.CardGiftcard to Color(0xFF9C27B0)
                                        else -> Icons.Default.Info to MaterialTheme.colorScheme.primary
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (!item.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                                itemShape
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), itemShape)
                                            .clip(itemShape)
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(iconColor.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Box(modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(end = 62.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = StringMapper.localizeCategoryKeysInText(if (isPersian) item.titleFa else item.titleEn, isPersian), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))

                                                    if (!item.isRead) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.height(4.dp))

                                                Text(text = StringMapper.localizeCategoryKeysInText(if (isPersian) item.descFa else item.descEn, isPersian), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2)
                                            }

                                            Surface(
                                                modifier = Modifier.align(Alignment.TopEnd),
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                            ) {
                                                Text(
                                                    text = relativeNotificationTime(item.timestamp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

        if (showPendingSheet) {
            PendingTransactionsBottomSheet(
                pendingList = pendingTransactions,
                categories = allPendingCategories,
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
                },
                onCategoriesClick = onCategoriesClick
            )
        }

        currentDueReminder?.let { reminder ->
            if (!showChangeDueDateDialog) {
                DueDateReminderDialog(
                    isPersian = isPersian,
                    item = reminder,
                    position = dueReminderTotal - pendingDueReminders.size,
                    total = dueReminderTotal,
                    currencyUnit = currencyUnit,
                    numberFormatter = numberFormatter,
                    onMarkAsPaid = {
                        debtCreditViewModel.settleDueReminder(reminder.id)
                        goToNextDueReminder()
                    },
                    onChangeDueDate = { showChangeDueDateDialog = true },
                    onDismiss = { goToNextDueReminder() }
                )
            }
        }

        if (showChangeDueDateDialog) {
            currentDueReminder?.let { reminder ->
                ChangeDueDateDialog(
                    isPersian = isPersian,
                    initialDueDateMillis = reminder.dueDateMillis,
                    onConfirm = { newDateMillis ->
                        debtCreditViewModel.updateDueDate(reminder.id, newDateMillis)
                        showChangeDueDateDialog = false
                        goToNextDueReminder()
                    },
                    onDismiss = { showChangeDueDateDialog = false }
                )
            }
        }
    }
}

/**
 * دیالوگ یادآوری سررسید بدهی/طلب؛ هم‌زبان با کارت «بدهی و طلب‌ها» در صفحه‌ی خانه:
 * همان نوار رنگی نوع در بالا، کاشی‌های خلاصه‌ی رنگی، نوار پیشرفت پرداخت و نشان وضعیت سررسید.
 * برای هر آیتمِ سررسیدگذشته یا نزدیک به سررسید (یکی‌یکی) نمایش داده می‌شود و دو اقدام اصلی
 * دارد: «ثبت پرداخت» (کشیدن) و «تغییر تاریخ سررسید».
 */
@Composable
private fun DueDateReminderDialog(
    isPersian: Boolean,
    item: DebtCreditEntity,
    position: Int,
    total: Int,
    currencyUnit: String,
    numberFormatter: NumberFormat,
    onMarkAsPaid: () -> Unit,
    onChangeDueDate: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDebt = item.type == "DEBT"
    val curr = if (currencyUnit == "IRR") 10L else 1L
    val currencyText = if (isPersian) {
        if (currencyUnit == "IRR") "ریال" else "تومان"
    } else {
        if (currencyUnit == "IRR") "Rial" else "T"
    }

    val remaining = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)
    val paidFraction = if (item.totalAmount > 0L)
        (item.paidAmount.toDouble() / item.totalAmount.toDouble()).toFloat().coerceIn(0f, 1f)
    else 0f
    val paidPercent = (paidFraction * 100).toInt()

    val daysLeft = daysUntilDue(item)
    val isOverdue = daysLeft < 0
    val isToday = daysLeft == 0

    val typeColor = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        daysLeft <= 1 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val title = when {
        isOverdue -> if (isPersian) "سررسید گذشته است" else "Payment overdue"
        isToday -> if (isPersian) "امروز سررسید است" else "Due today"
        else -> if (isPersian) "سررسید نزدیک است" else "Due soon"
    }
    val subtitle = if (isPersian) {
        if (isDebt) "بدهی شما به «${item.personName}»" else "طلب شما از «${item.personName}»"
    } else {
        if (isDebt) "You owe '${item.personName}'" else "'${item.personName}' owes you"
    }
    val dueText = when {
        isOverdue -> if (isPersian) "${numberFormatter.format((-daysLeft).toLong())} روز گذشته" else "${-daysLeft}d overdue"
        isToday -> if (isPersian) "امروز سررسید" else "Due today"
        else -> if (isPersian) "${numberFormatter.format(daysLeft.toLong())} روز مانده" else "${daysLeft}d left"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // کلید روی آیتم: با رفتن به یادآوری بعدی، ورود کارت دوباره انیمیت می‌شود.
        key(item.id) {
            val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.92f, animationSpec = tween(240))
            ) {
                val cardShape = RoundedCornerShape(28.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, cardShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), cardShape)
                        .clip(cardShape)
                ) {
                    // نوار رنگی نوع (بدهی/طلب) در لبه‌ی بالا، مثل کارت‌های صفحه‌ی خانه
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(typeColor.copy(alpha = 0.7f))
                    )

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        // ===== سربرگ =====
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(statusColor.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (total > 1) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isPersian) "${numberFormatter.format(position.toLong())} از ${numberFormatter.format(total.toLong())}"
                                        else "$position of $total",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ===== نشان‌ها: نوع، وضعیت سررسید، ماهانه =====
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(typeColor.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = (if (isDebt) "💸 " else "💰 ") +
                                            if (isPersian) (if (isDebt) "بدهی" else "طلب") else (if (isDebt) "Debt" else "Credit"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = typeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = if (isOverdue || daysLeft <= 1) 0.12f else 0.08f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "⏳ $dueText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (item.isMonthly) {
                                Spacer(Modifier.width(6.dp))
                                Text(text = "🔁", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ===== کاشی‌های خلاصه: باقی‌مانده و مبلغ کل =====
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DueReminderTile(
                                emoji = "🧾",
                                label = if (isPersian) "باقی‌مانده" else "Remaining",
                                amount = remaining * curr,
                                currencyText = currencyText,
                                isPersian = isPersian,
                                numberFormatter = numberFormatter,
                                color = typeColor,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            DueReminderTile(
                                emoji = "💼",
                                label = if (isPersian) "مبلغ کل" else "Total",
                                amount = item.totalAmount * curr,
                                currencyText = currencyText,
                                isPersian = isPersian,
                                numberFormatter = numberFormatter,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // ===== پیشرفت پرداخت =====
                        LinearProgressIndicator(
                            progress = { paidFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = typeColor,
                            trackColor = typeColor.copy(alpha = 0.1f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isPersian) {
                                "${numberFormatter.format(paidPercent.toLong())}٪ ${if (isDebt) "پرداخت شده" else "دریافت شده"}"
                            } else {
                                "$paidPercent% ${if (isDebt) "paid" else "received"}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        // ===== تاریخ سررسید و یادداشت =====
                        val infoShape = RoundedCornerShape(16.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), infoShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), infoShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DueReminderInfoRow(
                                label = if (isPersian) "📅 تاریخ سررسید" else "📅 Due date",
                                value = DateUtils.formatTimestamp(item.dueDateMillis, isPersian)
                            )
                            val noteText = item.note
                            if (!noteText.isNullOrBlank()) {
                                DueReminderInfoRow(
                                    label = if (isPersian) "📝 یادداشت" else "📝 Note",
                                    value = noteText
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        SwipeToConfirmButton(
                            text = if (isPersian) {
                                if (isDebt) "برای ثبت پرداخت بکشید" else "برای ثبت دریافت بکشید"
                            } else {
                                if (isDebt) "Swipe to confirm payment" else "Swipe to confirm receipt"
                            },
                            isPersian = isPersian,
                            resetTrigger = false,
                            onConfirm = onMarkAsPaid,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onChangeDueDate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "📅", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "تغییر تاریخ سررسید" else "Change due date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (total > 1 && position < total) {
                                    if (isPersian) "بعداً · مورد بعدی" else "Later · next reminder"
                                } else {
                                    if (isPersian) "بعداً یادم بنداز" else "Remind me later"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** کاشی‌ی خلاصه‌ی رنگی (هم‌سبک کاشی‌های «مانده بدهی / مانده طلب» صفحه‌ی خانه). */
@Composable
private fun DueReminderTile(
    emoji: String,
    label: String,
    amount: Long,
    currencyText: String,
    isPersian: Boolean,
    numberFormatter: NumberFormat,
    color: Color,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(16.dp)
    val formatted = numberFormatter.format(amount)
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), tileShape)
            .border(1.dp, color.copy(alpha = 0.18f), tileShape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        AutoFitAmountText(
            text = formatted,
            fallbackText = compactAmount(amount, isPersian),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxFontSize = 16f,
            minFontSize = 10f,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Text(
            text = currencyText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DueReminderInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false)
        )
    }
}

/**
 * دیالوگ انتخاب تاریخ سررسید جدید (دکمه «تغییر تاریخ سررسید» در DueDateReminderDialog).
 * دقیقاً هم‌سبک با DatePickerDialog موجود در صفحه‌ی افزودن/ویرایش بدهی و طلب.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeDueDateDialog(
    isPersian: Boolean,
    initialDueDateMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDueDateMillis.coerceAtLeast(todayStartMillis),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayStartMillis
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onConfirm(it) }
            }) {
                Text(if (isPersian) "تایید" else "OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * دریافت ایموجی مرتبط با دسته‌بندی با جستجو در لیست دسته‌بندی‌های پویا یا Fallback
 */
private fun getCategoryEmoji(categoryName: String, categoriesList: List<CategoryEntity>?): String {
    val matchedCategory = categoriesList?.firstOrNull {
        it.title.equals(categoryName, ignoreCase = true)
    }
    if (matchedCategory != null && matchedCategory.iconEmoji.isNotBlank()) {
        return matchedCategory.iconEmoji
    }

    return when (categoryName.uppercase()) {
        "FOOD", "RESTAURANT", "خوراکی", "غذا" -> "🍔"
        "TRANSPORT", "CAR", "حمل و نقل", "خودرو" -> "⛽"
        "SHOPPING", "خرید" -> "🛍️"
        "BILL", "قبوض" -> "📄"
        "SALARY", "حقوق" -> "💰"
        "INVESTMENT", "سرمایه گذاری" -> "📈"
        else -> "📌"
    }
}

/**
 * کامپوننت SkeletonScreen برای آیتم‌های لیست تراکنش
 */
@Composable
fun RecentTransactionSkeletonItem() {
    val rowShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), rowShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), rowShape)
            .clip(rowShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .appSkeletonShimmer()
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.55f)
                    .clip(RoundedCornerShape(6.dp))
                    .appSkeletonShimmer()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.35f)
                    .clip(RoundedCornerShape(4.dp))
                    .appSkeletonShimmer()
            )
        }
        Box(
            modifier = Modifier
                .height(18.dp)
                .width(75.dp)
                .clip(RoundedCornerShape(6.dp))
                .appSkeletonShimmer()
        )
    }
}

/**
 * تعداد روز تا سررسید بعدیِ یک بدهی/طلب (مقدار منفی یعنی از سررسید گذشته).
 * برای موارد ماهانه همیشه سررسید بعدی محاسبه می‌شود؛ برای موارد تک‌بار، تاریخ ثبت‌شده.
 */
private fun daysUntilDue(item: DebtCreditEntity): Int {
    fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val now = Calendar.getInstance().clearTime()
    val due = Calendar.getInstance().apply {
        if (item.isMonthly) {
            set(Calendar.DAY_OF_MONTH, 1)
            clearTime()
            set(Calendar.DAY_OF_MONTH, item.dueDay.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
            if (before(now)) {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, item.dueDay.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
        } else {
            timeInMillis = item.dueDateMillis
            clearTime()
        }
    }
    return Math.round((due.timeInMillis - now.timeInMillis) / 86_400_000.0).toInt()
}