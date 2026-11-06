package ir.hamedan.budgetmanagement.ui.screens.add

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionShareSheet
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.ui.components.rememberVoiceInputLauncher
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.components.WheelDatePickerDialog
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmount
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.utils.AmountInWords
import ir.hamedan.budgetmanagement.utils.CategoryRules
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.SpokenTransactionParser
import ir.hamedan.budgetmanagement.utils.CategorySuggestionHelper
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import java.util.Locale

internal const val MAX_AMOUNT_DIGITS = 12
private const val STEP_AMOUNT = 0
private const val STEP_CATEGORY = 1
private const val STEP_REVIEW = 2

/**
 * وضعیت فرم ثبت تراکنش. با [Saver] در چرخش صفحه و بازگشت از صفحه‌ی «مدیریت دسته‌ها» حفظ می‌شود.
 * مبلغ فقط به‌صورت رقم‌های انگلیسی (بدون جداکننده) نگه‌داری می‌شود.
 */
@Stable
class AddTransactionFormState(
    isExpense: Boolean,
    amount: String = "",
    categoryKey: String = "",
    title: String = "",
    note: String = "",
    manualCategory: Boolean = false,
    autoSuggested: Boolean = false,
    step: Int = STEP_AMOUNT,
    dayOffset: Int = 0
) {
    var isExpense by mutableStateOf(isExpense)
    var amount by mutableStateOf(amount)
    var categoryKey by mutableStateOf(categoryKey)
    var title by mutableStateOf(title)
    var note by mutableStateOf(note)

    /** کاربر خودش دسته را انتخاب کرده؛ پیشنهاد خودکار دیگر آن را عوض نمی‌کند. */
    var manualCategory by mutableStateOf(manualCategory)

    /** دسته‌ی فعلی از پیشنهاد خودکار آمده (برای نمایش نشان ✨). */
    var autoSuggested by mutableStateOf(autoSuggested)
    var step by mutableStateOf(step)

    /** تاریخ تراکنش به‌صورت «چند روز قبل از امروز»؛ ۰ یعنی همین الان. */
    var dayOffset by mutableStateOf(dayOffset)

    val amountValue: Long get() = amount.toLongOrNull() ?: 0L
    val hasAmount: Boolean get() = amountValue > 0L
    val hasCategory: Boolean get() = categoryKey.isNotBlank()

    companion object {
        val Saver = listSaver<AddTransactionFormState, Any>(
            save = {
                listOf(
                    it.isExpense, it.amount, it.categoryKey, it.title,
                    it.note, it.manualCategory, it.autoSuggested, it.step, it.dayOffset
                )
            },
            restore = {
                AddTransactionFormState(
                    isExpense = it[0] as Boolean,
                    amount = it[1] as String,
                    categoryKey = it[2] as String,
                    title = it[3] as String,
                    note = it[4] as String,
                    manualCategory = it[5] as Boolean,
                    autoSuggested = it[6] as Boolean,
                    step = it[7] as Int,
                    dayOffset = it[8] as Int
                )
            }
        )
    }
}

internal fun tr(isPersian: Boolean, fa: String, en: String) = if (isPersian) fa else en

internal fun formatNumber(value: Long, isPersian: Boolean): String =
    NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US).format(value)

internal fun unitLabel(currencyUnit: String, isPersian: Boolean): String = when {
    currencyUnit == "IRR" && isPersian -> "ریال"
    currencyUnit == "IRR" -> "Rial"
    isPersian -> "تومان"
    else -> "Toman"
}

internal fun amountInWords(value: Long, currencyUnit: String, isPersian: Boolean): String {
    val words = if (isPersian) AmountInWords.persian(value) else AmountInWords.englishCompact(value)
    return if (words.isEmpty()) "" else "$words ${unitLabel(currencyUnit, isPersian)}"
}

internal fun appendDigits(current: String, add: String): String {
    val next = if (current.isEmpty()) add.trimStart('0') else current + add
    return if (next.length <= MAX_AMOUNT_DIGITS) next else current
}

internal fun addToAmount(current: String, delta: Long): String {
    val next = ((current.toLongOrNull() ?: 0L) + delta).toString()
    return if (next.length <= MAX_AMOUNT_DIGITS) next else current
}

// -----------------------------------------------------------------------------
// صفحه‌ی اصلی
// -----------------------------------------------------------------------------

/**
 * ثبت تراکنش به‌صورت جریان تمام‌صفحه‌ی سه‌مرحله‌ای:
 * ۱) مبلغ (با کیبورد اختصاصی) ← ۲) دسته‌بندی ← ۳) مرور و ثبت با کشیدن.
 *
 * @param onClose خروج بدون ثبت (برگشت به صفحه‌ی قبل)
 * @param onFinished بعد از ثبت موفق و بسته شدن شیت موفقیت
 */
@Composable
fun AddTransactionScreen(
    initialIsExpense: Boolean = true,
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    viewModel: AddViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme

    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val usage by viewModel.categoryUsage.collectAsState()
    val scope = rememberCoroutineScope()

    val form = rememberSaveable(saver = AddTransactionFormState.Saver) {
        AddTransactionFormState(isExpense = initialIsExpense)
    }
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var insight by remember { mutableStateOf(SaveInsight()) }
    var sharePayload by remember { mutableStateOf<SharePayload?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message -> snackbarHostState.showError(message) }
    }

    val accent by animateColorAsState(
        targetValue = if (form.isExpense) scheme.tertiary else scheme.primary,
        animationSpec = tween(300),
        label = "accent"
    )
    val onAccent by animateColorAsState(
        targetValue = if (form.isExpense) scheme.onTertiary else scheme.onPrimary,
        animationSpec = tween(300),
        label = "onAccent"
    )

    // پرکاربردترین دسته‌های خودِ کاربر اول می‌آیند؛ در تساوی، ترتیب اصلی حفظ می‌شود.
    val selectable = remember(categories, form.isExpense, usage) {
        categories
            .filter { it.isExpense == form.isExpense && CategoryRules.isUserSelectable(it) }
            .sortedByDescending { usage[it.id] ?: 0 }
    }
    val selectedCategory = categories.find { it.title == form.categoryKey }

    fun goTo(step: Int) {
        focusManager.clearFocus()
        form.step = step.coerceIn(STEP_AMOUNT, STEP_REVIEW)
    }

    fun suggestFor(title: String, expense: Boolean = form.isExpense) {
        if (form.manualCategory) return
        val available = categories.filter { it.isExpense == expense && CategoryRules.isUserSelectable(it) }
        val suggested = CategorySuggestionHelper.suggestCategory(
            title = title,
            availableCategories = available,
            isPersian = isPersian
        )
        if (suggested != null) {
            form.categoryKey = suggested
            form.autoSuggested = true
        }
    }

    fun setType(expense: Boolean) {
        if (form.isExpense == expense) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        form.isExpense = expense
        // دسته‌های هزینه و درآمد جدا هستند؛ انتخاب قبلی معتبر نیست.
        form.categoryKey = ""
        form.manualCategory = false
        form.autoSuggested = false
    }

    /** یک جمله‌ی گفتاری را به مبلغ، عنوان و نوع تبدیل می‌کند و فرم را تا جای ممکن جلو می‌برد. */
    fun applyVoice(spoken: String) {
        val parsed = SpokenTransactionParser.parse(spoken)
        val expense = parsed.isExpense ?: form.isExpense
        if (expense != form.isExpense) {
            form.isExpense = expense
            form.categoryKey = ""
            form.autoSuggested = false
        }
        val display = SpokenTransactionParser.displayAmount(parsed, currencyUnit == "IRR")
        if (display != null && display > 0L && display.toString().length <= MAX_AMOUNT_DIGITS) {
            form.amount = display.toString()
        }
        if (parsed.title.isNotBlank()) {
            form.title = parsed.title
            form.manualCategory = false
            suggestFor(parsed.title, expense)
        }
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        when {
            form.hasAmount && form.hasCategory && form.title.isNotBlank() -> goTo(STEP_REVIEW)
            form.hasAmount -> goTo(STEP_CATEGORY)
            else -> scope.launch {
                snackbarHostState.showSnackbar(
                    tr(isPersian, "مبلغ رو متوجه نشدم؛ با کیبورد وارد کن.", "I couldn't catch the amount; type it in.")
                )
            }
        }
    }

    fun save() {
        if (isSaving || !form.hasAmount || !form.hasCategory || form.title.isBlank()) return
        val parsed = form.amount.toDoubleOrNull() ?: return
        val amount = if (currencyUnit == "IRR") parsed / 10.0 else parsed
        val savedTitle = form.title.trim()
        val savedDisplayAmount = parsed.toLong()
        val savedIsExpense = form.isExpense
        val savedCategoryName = StringMapper.getCategoryName(form.categoryKey, isPersian)
        val savedNote = form.note.trim()
        // تاریخِ انتخابی با ساعتِ همین لحظه ذخیره می‌شود تا ترتیب تراکنش‌های یک روز طبیعی بماند.
        val timestamp = if (form.dayOffset <= 0) {
            System.currentTimeMillis()
        } else {
            LocalDate.now().minusDays(form.dayOffset.toLong())
                .atTime(LocalTime.now())
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        isSaving = true
        viewModel.addTransaction(
            title = savedTitle,
            amount = amount,
            categoryKey = form.categoryKey,
            isExpense = savedIsExpense,
            note = form.note.trim(),
            timestamp = timestamp,
            onSaved = { insight = it },
            onResult = { saved ->
                // ثبت ناموفق: فرم و ورودی کاربر سر جایش می‌ماند و اسنک‌بار خطا نشان داده می‌شود.
                if (saved) {
                    // کارتِ قابل‌اشتراک از همین مقادیر ساخته می‌شود (فقط برای تصویر؛ در دیتابیس نمی‌رود).
                    sharePayload = SharePayload(
                        transaction = TransactionEntity(
                            title = savedTitle,
                            amount = MoneyContract.inputToStorage(parsed, currencyUnit),
                            categoryId = selectedCategory?.id ?: form.categoryKey,
                            type = if (savedIsExpense) "EXPENSE" else "INCOME",
                            timestamp = timestamp,
                            note = savedNote
                        ),
                        categoryTitle = savedCategoryName,
                        categoryEmoji = selectedCategory?.iconEmoji ?: "📁"
                    )
                    successSheet = transactionSavedContent(
                        title = savedTitle,
                        displayAmount = savedDisplayAmount,
                        isExpense = savedIsExpense,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        categoryName = savedCategoryName,
                        insight = insight
                    )
                } else {
                    isSaving = false
                }
            }
        )
    }

    BackHandler(enabled = form.step > STEP_AMOUNT && successSheet == null) {
        goTo(form.step - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        AuroraBackground()

        // هاله‌ی رنگیِ بالای صفحه که با نوع تراکنش عوض می‌شود.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            TopBar(
                step = form.step,
                accent = accent,
                isPersian = isPersian,
                onBack = { if (form.step == STEP_AMOUNT) onClose() else goTo(form.step - 1) }
            )

            AnimatedContent(
                targetState = form.step,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    val dir = if (targetState > initialState) {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.End
                    }
                    (slideIntoContainer(dir, tween(260), initialOffset = { it / 4 }) + fadeIn(tween(260)))
                        .togetherWith(
                            slideOutOfContainer(dir, tween(260), targetOffset = { it / 4 }) + fadeOut(tween(160))
                        )
                },
                label = "step"
            ) { step ->
                when (step) {
                    STEP_AMOUNT -> AmountStep(
                        form = form,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        onSetType = ::setType,
                        onVoiceResult = ::applyVoice,
                        onContinue = { goTo(STEP_CATEGORY) }
                    )

                    STEP_CATEGORY -> CategoryStep(
                        form = form,
                        categories = selectable,
                        accent = accent,
                        onAccent = onAccent,
                        isPersian = isPersian,
                        onTitleChanged = { input ->
                            if (input.length <= 40) {
                                form.title = input
                                suggestFor(input)
                            }
                        },
                        onCategoryPicked = { category ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            form.categoryKey = category.title
                            form.manualCategory = true
                            form.autoSuggested = false
                        },
                        onManage = onManageCategories,
                        onContinue = { goTo(STEP_REVIEW) }
                    )

                    else -> ReviewStep(
                        form = form,
                        category = selectedCategory,
                        accent = accent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        canSave = !isSaving && form.hasAmount && form.hasCategory && form.title.isNotBlank(),
                        onEditAmount = { goTo(STEP_AMOUNT) },
                        onEditCategory = { goTo(STEP_CATEGORY) },
                        onConfirm = ::save
                    )
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            isPersian = isPersian,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp)
        )
    }

    successSheet?.let { content ->
        SuccessBottomSheet(
            content = content,
            isPersian = isPersian,
            onDismiss = {
                successSheet = null
                onFinished()
            },
            onShare = sharePayload?.let { { showShareSheet = true } }
        )
    }

    if (showShareSheet) {
        sharePayload?.let { payload ->
            TransactionShareSheet(
                transaction = payload.transaction,
                categoryTitle = payload.categoryTitle,
                categoryEmoji = payload.categoryEmoji,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

/** داده‌ی لازم برای ساخت کارت اشتراک‌گذاریِ تراکنشِ تازه ثبت‌شده. */
private class SharePayload(
    val transaction: TransactionEntity,
    val categoryTitle: String,
    val categoryEmoji: String
)

/** متن شیت موفقیتِ ثبت تراکنش؛ با درصد مصرف بودجه‌ی دسته و streak روزهای پشت‌سرهم. */
internal fun transactionSavedContent(
    title: String,
    displayAmount: Long,
    isExpense: Boolean,
    currencyUnit: String,
    isPersian: Boolean,
    categoryName: String = "",
    insight: SaveInsight = SaveInsight()
): SuccessSheetContent {
    val amountText = formatSuccessAmount(displayAmount, currencyUnit, isPersian)
    val budgetLine = budgetLine(insight.budgetPercent, categoryName, isPersian)
    val streakLine = if (insight.streakDays >= 2) {
        val days = if (isPersian) {
            TransactionShareFormatter.toPersianDigits(insight.streakDays.toString())
        } else {
            insight.streakDays.toString()
        }
        tr(isPersian, "🔥 $days روز پشت‌سرهم تراکنش ثبت کردی!", "🔥 You've logged transactions $days days in a row!")
    } else {
        null
    }
    return if (isPersian) {
        SuccessSheetContent(
            title = "تراکنش ثبت شد",
            message = "${if (isExpense) "هزینه" else "درآمد"} «$title» به مبلغ $amountText با موفقیت ثبت شد." +
                    (budgetLine?.let { " $it" } ?: ""),
            hint = streakLine ?: "موجودی و گزارش‌های شما به‌روز است."
        )
    } else {
        SuccessSheetContent(
            title = "Transaction recorded",
            message = "Your ${if (isExpense) "expense" else "income"} “$title” of $amountText was recorded successfully." +
                    (budgetLine?.let { " $it" } ?: ""),
            hint = streakLine ?: "Your balance and reports are up to date."
        )
    }
}

private fun budgetLine(percent: Int?, categoryName: String, isPersian: Boolean): String? {
    if (percent == null || categoryName.isBlank()) return null
    val p = if (isPersian) TransactionShareFormatter.toPersianDigits("$percent") + "٪" else "$percent%"
    return when {
        percent >= 100 -> tr(
            isPersian,
            "از سقف «$categoryName» عبور کردی ($p).",
            "You've passed the “$categoryName” budget ($p)."
        )
        percent >= 80 -> tr(
            isPersian,
            "به $p سقف «$categoryName» رسیدی؛ کمی مراقب باش.",
            "You've used $p of your “$categoryName” budget; take it easy."
        )
        else -> tr(
            isPersian,
            "$p از سقف «$categoryName» این دوره مصرف شده.",
            "$p of your “$categoryName” budget is used this period."
        )
    }
}

// -----------------------------------------------------------------------------
// نوار بالا
// -----------------------------------------------------------------------------

@Composable
private fun TopBar(step: Int, accent: Color, isPersian: Boolean, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            if (step == STEP_AMOUNT) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = tr(isPersian, "بستن", "Close"),
                    tint = scheme.onSurface
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr(isPersian, "مرحله‌ی قبل", "Previous step"),
                    tint = scheme.onSurface
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val color by animateColorAsState(
                    targetValue = if (index <= step) accent else scheme.onSurface.copy(alpha = 0.12f),
                    animationSpec = tween(300),
                    label = "progress$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
        // هم‌وزن با دکمه‌ی سمت مقابل تا نوار پیشرفت وسط بماند.
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
internal fun PrimaryActionButton(
    text: String,
    enabled: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = onAccent,
            disabledContainerColor = scheme.surfaceVariant.copy(alpha = 0.7f),
            disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// -----------------------------------------------------------------------------
// مرحله ۱: مبلغ
// -----------------------------------------------------------------------------

@Composable
private fun AmountStep(
    form: AddTransactionFormState,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    onSetType: (Boolean) -> Unit,
    onVoiceResult: (String) -> Unit,
    onContinue: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // در ریال، میانبرها ده برابر می‌شوند تا همان ارزش را داشته باشند.
    val unitFactor = if (currencyUnit == "IRR") 10L else 1L
    val quickDeltas = remember(unitFactor) { listOf(10_000L, 50_000L, 100_000L).map { it * unitFactor } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        TypeToggle(isExpense = form.isExpense, isPersian = isPersian, onSetType = onSetType)

        Spacer(Modifier.height(10.dp))

        VoicePill(isPersian = isPersian, accent = accent, onResult = onVoiceResult)

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (form.isExpense) {
                tr(isPersian, "چقدر خرج کردی؟", "How much did you spend?")
            } else {
                tr(isPersian, "چقدر درآمد داشتی؟", "How much did you earn?")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AmountHero(form = form, accent = accent, currencyUnit = currencyUnit, isPersian = isPersian)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickDeltas.forEach { delta ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.10f))
                        .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            form.amount = addToAmount(form.amount, delta)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+" + formatNumber(delta, isPersian),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        NumericKeypad(
            isPersian = isPersian,
            onDigits = { digits -> form.amount = appendDigits(form.amount, digits) },
            onBackspace = { form.amount = form.amount.dropLast(1) },
            onClear = { form.amount = "" }
        )

        Spacer(Modifier.height(12.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = form.hasAmount,
            accent = accent,
            onAccent = onAccent,
            onClick = onContinue
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** ثبت با یک جمله: «ناهار ۲۵۰ هزار تومان». کل ردیف لمس‌پذیر است، نه فقط آیکن میکروفون. */
@Composable
private fun VoicePill(isPersian: Boolean, accent: Color, onResult: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val launchSpeech = rememberVoiceInputLauncher(
        onResult = onResult,
        language = if (isPersian) "fa-IR" else "en-US"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.2f), shape)
            .clickable(
                role = Role.Button,
                onClickLabel = tr(isPersian, "شروع ضبط صدا", "Start voice input"),
                onClick = launchSpeech
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tr(isPersian, "بگو: «ناهار ۲۵۰ هزار تومان»", "Say: “lunch 25k”"),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.MicNone,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
internal fun TypeToggle(isExpense: Boolean, isPersian: Boolean, onSetType: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, scheme.outline.copy(alpha = 0.15f), shape)
            .padding(4.dp)
    ) {
        ToggleSegment(
            label = tr(isPersian, "هزینه", "Expense"),
            selected = isExpense,
            selectedColor = scheme.tertiary,
            onSelectedColor = scheme.onTertiary,
            onClick = { onSetType(true) },
            modifier = Modifier.weight(1f)
        )
        ToggleSegment(
            label = tr(isPersian, "درآمد", "Income"),
            selected = !isExpense,
            selectedColor = scheme.primary,
            onSelectedColor = scheme.onPrimary,
            onClick = { onSetType(false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onSelectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.Transparent,
        animationSpec = tween(250),
        label = "segmentBg"
    )
    val content by animateColorAsState(
        targetValue = if (selected) onSelectedColor else scheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "segmentFg"
    )
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = content, fontWeight = FontWeight.Bold)
    }
}

/** عدد بزرگ مبلغ، واحد و مبلغ به حروف. */
@Composable
internal fun AmountHero(
    form: AddTransactionFormState,
    accent: Color,
    currencyUnit: String,
    isPersian: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val isEmpty = form.amount.isEmpty()
    val display = formatNumber(form.amountValue, isPersian)
    // اندازه‌ی فونت با طول عدد کم می‌شود تا ۱۲ رقم هم در یک خط جا شود.
    val fontSize = when {
        display.length <= 8 -> 56.sp
        display.length <= 11 -> 46.sp
        display.length <= 14 -> 38.sp
        else -> 32.sp
    }

    val pop = remember { Animatable(1f) }
    LaunchedEffect(form.amount) {
        if (form.amount.isNotEmpty()) {
            pop.snapTo(1.05f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    val words = amountInWords(form.amountValue, currencyUnit, isPersian)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = display,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = if (isEmpty) scheme.onSurface.copy(alpha = 0.25f) else accent,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = unitLabel(currencyUnit, isPersian),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        AnimatedVisibility(
            visible = words.isNotEmpty(),
            enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(120)) + slideOutVertically { it / 2 }
        ) {
            Text(
                text = words,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

/** کیبورد عددی اختصاصی؛ همیشه چپ‌به‌راست چیده می‌شود تا ۱ ۲ ۳ در هر زبانی یکسان باشد. */
@Composable
internal fun NumericKeypad(
    isPersian: Boolean,
    onDigits: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val rows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("000", "0", "⌫")
        )
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val isBackspace = key == "⌫"
                        KeypadKey(
                            label = if (isBackspace) key else if (isPersian) TransactionShareFormatter.toPersianDigits(key) else key,
                            description = if (isBackspace) tr(isPersian, "حذف", "Delete") else null,
                            onClick = { if (isBackspace) onBackspace() else onDigits(key) },
                            onLongClick = if (isBackspace) onClear else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    label: String,
    description: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.5f))
            .combinedClickable(
                onClickLabel = description,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۲: دسته‌بندی
// -----------------------------------------------------------------------------

@Composable
private fun CategoryStep(
    form: AddTransactionFormState,
    categories: List<CategoryEntity>,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onTitleChanged: (String) -> Unit,
    onCategoryPicked: (CategoryEntity) -> Unit,
    onManage: () -> Unit,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val titleFocus = remember { FocusRequester() }
    // خطا فقط بعد از زدن «ادامه» با عنوان خالی نشان داده می‌شود، نه از همان اول.
    var showTitleError by remember { mutableStateOf(false) }
    var showCategoryError by remember { mutableStateOf(false) }

    fun updateTitle(input: String) {
        onTitleChanged(input)
        if (input.isNotBlank()) showTitleError = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = if (form.isExpense) {
                tr(isPersian, "برای چی بود؟", "What was it for?")
            } else {
                tr(isPersian, "از کجا بود؟", "Where did it come from?")
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.title,
            onValueChange = ::updateTitle,
            label = { Text(tr(isPersian, "عنوان", "Title")) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocus),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            isError = showTitleError,
            supportingText = if (showTitleError) {
                {
                    Text(
                        tr(
                            isPersian,
                            "عنوان رو فراموش کردی؛ یه اسم کوتاه بنویس",
                            "You forgot the title; add a short name"
                        )
                    )
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
                cursorColor = accent
            ),
            trailingIcon = {
                VoiceInputButton(
                    onResult = { spoken -> updateTitle(spoken.take(40)) },
                    language = if (isPersian) "fa-IR" else "en-US"
                )
            }
        )

        if (showCategoryError) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = tr(isPersian, "یک دسته‌بندی انتخاب کن", "Pick a category"),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.error
            )
        }

        Spacer(Modifier.height(8.dp))

        if (categories.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = tr(isPersian, "دسته‌بندی‌ای پیدا نشد", "No categories found"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                // فاصله‌ی بالا/کناره‌ها جای بزرگ‌شدنِ کارتِ انتخاب‌شده است تا زیر فیلد عنوان یا لبه‌ها بریده نشود.
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = categories, key = { it.id }) { category ->
                    val selected = category.title == form.categoryKey
                    CategoryTile(
                        emoji = category.iconEmoji,
                        name = StringMapper.getCategoryName(category.title, isPersian),
                        selected = selected,
                        suggested = selected && form.autoSuggested,
                        accent = accent,
                        onAccent = onAccent,
                        suggestedLabel = tr(isPersian, "✨ پیشنهاد", "✨ Suggested"),
                        onClick = {
                            showCategoryError = false
                            onCategoryPicked(category)
                        }
                    )
                }
                item(key = "manage") {
                    ManageCategoriesTile(
                        label = tr(isPersian, "مدیریت", "Manage"),
                        onClick = onManage
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = true,
            accent = accent,
            onAccent = onAccent,
            onClick = {
                when {
                    form.title.isBlank() -> {
                        showTitleError = true
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        titleFocus.requestFocus()
                    }
                    !form.hasCategory -> {
                        showCategoryError = true
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    }
                    else -> onContinue()
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * کارت دسته: ایموجی داخل دایره، نام تا دو خط، و برای دسته‌ی پیشنهادی یک نوار پررنگ
 * پایین کارت (متن روی رنگ اصلی با کنتراست کامل، نه متن ریز روی زمینه‌ی کم‌رنگ).
 */
@Composable
internal fun CategoryTile(
    emoji: String,
    name: String,
    selected: Boolean,
    suggested: Boolean,
    accent: Color,
    onAccent: Color,
    suggestedLabel: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tileScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.14f) else scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else scheme.outline.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = if (suggested) 24.dp else 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) accent.copy(alpha = 0.18f) else scheme.onSurface.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else scheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
        if (suggested) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestedLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = onAccent,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ManageCategoriesTile(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(shape)
            .border(1.dp, scheme.outline.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// -----------------------------------------------------------------------------
// مرحله ۳: مرور و ثبت
// -----------------------------------------------------------------------------

@Composable
private fun ReviewStep(
    form: AddTransactionFormState,
    category: CategoryEntity?,
    accent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    canSave: Boolean,
    onEditAmount: () -> Unit,
    onEditCategory: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var noteOpen by rememberSaveable { mutableStateOf(form.note.isNotBlank()) }
    val categoryName = category?.let { StringMapper.getCategoryName(it.title, isPersian) }.orEmpty()
    val words = amountInWords(form.amountValue, currencyUnit, isPersian)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = tr(isPersian, "همه‌چی درسته؟", "Look good?"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            // کارت خلاصه؛ لمس آن به ویرایش مبلغ می‌رود.
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(accent.copy(alpha = 0.10f))
                    .border(1.dp, accent.copy(alpha = 0.25f), cardShape)
                    .clickable(onClick = onEditAmount)
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = if (form.isExpense) tr(isPersian, "هزینه", "Expense") else tr(isPersian, "درآمد", "Income"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatSuccessAmount(form.amountValue, currencyUnit, isPersian),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    textAlign = TextAlign.Center
                )
                if (words.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = words,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ReviewRow(
                label = tr(isPersian, "دسته‌بندی", "Category"),
                value = categoryName,
                leading = category?.iconEmoji,
                onClick = onEditCategory
            )
            ReviewRow(
                label = tr(isPersian, "عنوان", "Title"),
                value = form.title.trim(),
                onClick = onEditCategory
            )
            DateRow(
                dayOffset = form.dayOffset,
                accent = accent,
                isPersian = isPersian,
                onSelectOffset = { form.dayOffset = it }
            )

            Spacer(Modifier.height(4.dp))

            if (!noteOpen) {
                Text(
                    text = tr(isPersian, "＋ افزودن یادداشت", "＋ Add a note"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { noteOpen = true }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                )
            }
            AnimatedVisibility(visible = noteOpen) {
                OutlinedTextField(
                    value = form.note,
                    onValueChange = { if (it.length <= 120) form.note = it },
                    label = { Text(tr(isPersian, "یادداشت (اختیاری)", "Note (optional)")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        focusedLabelColor = accent,
                        cursorColor = accent
                    ),
                    trailingIcon = {
                        VoiceInputButton(
                            onResult = { spoken -> form.note = spoken.take(120) },
                            language = if (isPersian) "fa-IR" else "en-US"
                        )
                    },
                    supportingText = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "${form.note.length}/120",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SwipeToConfirmButton(
            text = tr(isPersian, "برای ثبت بکشید", "Swipe to save"),
            isPersian = isPersian,
            resetTrigger = canSave,
            onConfirm = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            accentColor = accent
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReviewRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    leading: String? = null,
    dimmed: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.width(88.dp)
            )
            if (leading != null) {
                Text(text = leading, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (dimmed) scheme.onSurfaceVariant.copy(alpha = 0.6f) else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
    }
}

// -----------------------------------------------------------------------------
// تاریخ تراکنش
// -----------------------------------------------------------------------------

/** امروز | دیروز | انتخاب دستی با چرخ تاریخ (شمسی در فارسی). */
@Composable
internal fun DateRow(
    dayOffset: Int,
    accent: Color,
    isPersian: Boolean,
    onSelectOffset: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var pickerOpen by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val selectedMillis = today.minusDays(dayOffset.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    val customLabel = if (dayOffset >= 2) {
        DateUtils.formatTimestamp(selectedMillis, isPersian).let {
            if (isPersian) TransactionShareFormatter.toPersianDigits(it) else it
        }
    } else {
        tr(isPersian, "📅 انتخاب", "📅 Pick")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(isPersian, "تاریخ", "Date"),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateChip(tr(isPersian, "امروز", "Today"), dayOffset == 0, accent, Modifier.weight(1f)) {
                    onSelectOffset(0)
                }
                DateChip(tr(isPersian, "دیروز", "Yesterday"), dayOffset == 1, accent, Modifier.weight(1f)) {
                    onSelectOffset(1)
                }
                DateChip(customLabel, dayOffset >= 2, accent, Modifier.weight(1.2f)) {
                    pickerOpen = true
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
    }

    if (pickerOpen) {
        WheelDatePickerDialog(
            isPersian = isPersian,
            title = tr(isPersian, "تاریخ تراکنش", "Transaction date"),
            initialMillis = maxOf(
                selectedMillis,
                today.minusYears(5).atStartOfDay(zone).toInstant().toEpochMilli()
            ),
            onConfirm = { millis ->
                val picked = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                onSelectOffset(ChronoUnit.DAYS.between(picked, today).toInt().coerceAtLeast(0))
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
            minDate = today.minusYears(5),
            maxDate = today
        )
    }
}

@Composable
private fun DateChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else scheme.outline.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}