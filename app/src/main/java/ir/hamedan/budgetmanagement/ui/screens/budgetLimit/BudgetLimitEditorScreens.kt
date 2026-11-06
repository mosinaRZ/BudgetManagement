package ir.hamedan.budgetmanagement.ui.screens.budgetLimit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.WheelDatePickerDialog
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmount
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmountFromToman
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.screens.add.AmountHero
import ir.hamedan.budgetmanagement.ui.screens.add.CategoryTile
import ir.hamedan.budgetmanagement.ui.screens.add.MAX_AMOUNT_DIGITS
import ir.hamedan.budgetmanagement.ui.screens.add.ManageCategoriesTile
import ir.hamedan.budgetmanagement.ui.screens.add.NumericKeypad
import ir.hamedan.budgetmanagement.ui.screens.add.PrimaryActionButton
import ir.hamedan.budgetmanagement.ui.screens.add.StepTopBar
import ir.hamedan.budgetmanagement.ui.screens.add.addToAmount
import ir.hamedan.budgetmanagement.ui.screens.add.amountInWords
import ir.hamedan.budgetmanagement.ui.screens.add.appendDigits
import ir.hamedan.budgetmanagement.ui.screens.add.formatNumber
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.ui.screens.budget.endOfDayMillis
import ir.hamedan.budgetmanagement.ui.screens.budget.findOverlappingLimit
import ir.hamedan.budgetmanagement.utils.CategoryRules
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.StringMapper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// -----------------------------------------------------------------------------
// ثبت و ویرایش سقف بودجه
//
// ثبت: جریان تمام‌صفحه‌ی چهار‌مرحله‌ای، با همان زبان طراحی ثبت تراکنش
//      ۱) دسته‌بندی ← ۲) سقف مبلغ ← ۳) بازه‌ی زمانی ← ۴) مرور و ثبت با کشیدن.
// ویرایش: صفحه‌ی تمام‌صفحه‌ی تک‌پنل، با همان زبان طراحی ویرایش تراکنش
//      (مبلغ لمس‌پذیر با کیبورد اختصاصی، ردیف افقی دسته‌ها، دکمه‌ی سوایپ که تا زمان
//      تغییر غیرفعال است).
//
// سقف بودجه همیشه برای «هزینه» است؛ پس رنگ تأکید همان رنگ هزینه (tertiary) است.
// -----------------------------------------------------------------------------

private const val STEP_CATEGORY = 0
private const val STEP_AMOUNT = 1
private const val STEP_PERIOD = 2
private const val STEP_REVIEW = 3
private const val STEP_COUNT = 4

private const val DEFAULT_PERIOD_DAYS = 30

// -----------------------------------------------------------------------------
// وضعیت فرم
// -----------------------------------------------------------------------------

/**
 * وضعیت فرم سقف بودجه. با [Saver] در چرخش صفحه و بازگشت از «مدیریت دسته‌ها» حفظ می‌شود.
 *
 * - [amount] فقط رقم‌های انگلیسی و بدون جداکننده است، به واحد نمایشی (تومان یا ریال).
 * - روزها به‌صورت «روز از epoch» نگه‌داری می‌شوند (نه میلی‌ثانیه) تا ساعت و منطقه‌ی زمانی
 *   در مقایسه‌ی بازه‌ها دخالت نکند. هر دو سر بازه «شامل» است؛ یعنی شروع و پایانِ یکسان = ۱ روز.
 */
@Stable
class LimitFormState(
    categoryId: String = "",
    amount: String = "",
    startDay: Long,
    endDay: Long,
    step: Int = STEP_CATEGORY
) {
    var categoryId by mutableStateOf(categoryId)
    var amount by mutableStateOf(amount)
    var startDay by mutableLongStateOf(startDay)
    var endDay by mutableLongStateOf(endDay)
    var step by mutableIntStateOf(step)

    val amountValue: Long get() = amount.toLongOrNull() ?: 0L
    val hasAmount: Boolean get() = amountValue > 0L
    val hasCategory: Boolean get() = categoryId.isNotBlank()
    val isPeriodValid: Boolean get() = endDay >= startDay
    val dayCount: Int get() = (endDay - startDay + 1L).coerceAtLeast(0L).toInt()

    /** با عوض‌شدن شروع، اگر پایان از آن عقب‌تر شد همراهش جلو می‌آید. */
    fun setStart(day: Long) {
        startDay = day
        if (endDay < day) endDay = day
    }

    fun setEnd(day: Long) {
        endDay = day.coerceAtLeast(startDay)
    }

    companion object {
        val Saver = listSaver<LimitFormState, Any>(
            save = { listOf(it.categoryId, it.amount, it.startDay, it.endDay, it.step) },
            restore = {
                LimitFormState(
                    categoryId = it[0] as String,
                    amount = it[1] as String,
                    startDay = it[2] as Long,
                    endDay = it[3] as Long,
                    step = it[4] as Int
                )
            }
        )
    }
}

// -----------------------------------------------------------------------------
// کمک‌های تاریخ، دسته و مبلغ
// -----------------------------------------------------------------------------

private fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

private fun dayToMillis(day: Long): Long =
    LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun millisToDay(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** پایان (۲۳:۵۹:۵۹.۹۹۹) روزِ [day]؛ همان چیزی که در دیتابیس برای پایان بازه ذخیره می‌شود. */
private fun dayEndMillis(day: Long): Long = endOfDayMillis(dayToMillis(day))

private fun dayLabel(day: Long, isPersian: Boolean): String {
    val text = DateUtils.formatTimestamp(dayToMillis(day), isPersian)
    return if (isPersian) TransactionShareFormatter.toPersianDigits(text) else text
}

private fun localizedCount(value: Int, isPersian: Boolean): String =
    if (isPersian) TransactionShareFormatter.toPersianDigits(value.toString()) else value.toString()

private fun daysText(days: Int, isPersian: Boolean): String =
    if (isPersian) "${localizedCount(days, true)} روز" else if (days == 1) "1 day" else "$days days"

private fun percentText(percent: Int, isPersian: Boolean): String =
    if (isPersian) TransactionShareFormatter.toPersianDigits("$percent") + "٪" else "$percent%"

/** دسته‌هایی که کاربر می‌تواند برایشان سقف بگذارد (بدون «قلک» و «دسته‌بندی نشده»). */
private fun selectableLimitCategories(all: List<CategoryEntity>): List<CategoryEntity> =
    all.filter { category ->
        CategoryRules.isUserSelectable(category) &&
                !category.title.trim().equals("UNCATEGORIZED", ignoreCase = true) &&
                category.title != "دسته‌بندی نشده" &&
                category.title != "دسته بندی نشده"
    }

/** مبلغ نمایشی (تومان یا ریال) ← تومانِ ذخیره‌شده. */
private fun displayToStored(displayAmount: Long, currencyUnit: String): Long =
    MoneyContract.inputToStorage(displayAmount.toDouble(), currencyUnit)

/** پیشنهاد سقف را تا دو رقم بامعنا گرد می‌کند (۱٬۲۳۴٬۵۶۷ ← ۱٬۳۰۰٬۰۰۰). */
private fun roundUpNice(value: Long): Long {
    if (value <= 0L) return 0L
    val digits = value.toString().length
    if (digits <= 2) return value
    var magnitude = 1L
    repeat(digits - 2) { magnitude *= 10L }
    return ((value + magnitude - 1L) / magnitude) * magnitude
}

private class PeriodPreset(
    val labelFa: String,
    val labelEn: String,
    val startDay: Long,
    val endDay: Long
)

/** میانبرهای بازه؛ ماه‌ها در فارسی شمسی و در انگلیسی میلادی حساب می‌شوند. */
private fun buildPresets(today: Long, isPersian: Boolean): List<PeriodPreset> {
    val todayDate = LocalDate.ofEpochDay(today)
    val thisMonthEnd: LocalDate
    val nextMonthStart: LocalDate
    val nextMonthEnd: LocalDate
    if (isPersian) {
        val (year, month, _) = DateUtils.toJalali(todayDate)
        val nextYear = if (month == 12) year + 1 else year
        val nextMonth = if (month == 12) 1 else month + 1
        thisMonthEnd = DateUtils.fromJalali(year, month, DateUtils.getDaysInJalaliMonth(year, month))
        nextMonthStart = DateUtils.fromJalali(nextYear, nextMonth, 1)
        nextMonthEnd = DateUtils.fromJalali(nextYear, nextMonth, DateUtils.getDaysInJalaliMonth(nextYear, nextMonth))
    } else {
        val month = YearMonth.from(todayDate)
        thisMonthEnd = month.atEndOfMonth()
        nextMonthStart = month.plusMonths(1).atDay(1)
        nextMonthEnd = month.plusMonths(1).atEndOfMonth()
    }
    return listOf(
        PeriodPreset("۷ روز", "7 days", today, today + 6L),
        PeriodPreset("۳۰ روز", "30 days", today, today + 29L),
        PeriodPreset("تا آخر ماه", "Rest of month", today, thisMonthEnd.toEpochDay()),
        PeriodPreset("ماه بعد", "Next month", nextMonthStart.toEpochDay(), nextMonthEnd.toEpochDay())
    )
}

/** متن شیت موفقیت برای ثبت یا ویرایش سقف. */
private fun budgetLimitSavedContent(
    isEdit: Boolean,
    categoryTitle: String,
    maxLimitToman: Long,
    currencyUnit: String,
    isPersian: Boolean
): SuccessSheetContent {
    val limit = formatSuccessAmountFromToman(maxLimitToman, currencyUnit, isPersian)
    return if (isPersian) {
        if (isEdit) {
            SuccessSheetContent(
                title = "محدودیت مالی به‌روزرسانی شد",
                message = "تغییرات محدودیت دسته‌بندی «$categoryTitle» با سقف $limit با موفقیت ذخیره شد."
            )
        } else {
            SuccessSheetContent(
                title = "محدودیت مالی ثبت شد",
                message = "سقف $limit برای دسته‌بندی «$categoryTitle» با موفقیت تعیین شد.",
                hint = "پیشرفت خرج خود را در همین صفحه دنبال کنید."
            )
        }
    } else {
        if (isEdit) {
            SuccessSheetContent(
                title = "Budget limit updated",
                message = "Your changes to the “$categoryTitle” limit (now $limit) were saved successfully."
            )
        } else {
            SuccessSheetContent(
                title = "Budget limit set",
                message = "A limit of $limit was set for “$categoryTitle” successfully.",
                hint = "Track your spending against it right on this page."
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ثبت سقف جدید (جریان مرحله‌ای)
// -----------------------------------------------------------------------------

/**
 * @param onClose خروج بدون ثبت
 * @param onFinished بعد از ثبت موفق و بسته شدن شیت موفقیت
 * @param onManageCategories رفتن به صفحه‌ی مدیریت دسته‌ها (فرم هنگام بازگشت حفظ می‌شود)
 */
@Composable
fun AddBudgetLimitScreen(
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    viewModel: BudgetLimitViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()
    val rawCategories by viewModel.expenseCategories.collectAsState()
    val limits by viewModel.allLimits.collectAsState()
    val categories = remember(rawCategories) { selectableLimitCategories(rawCategories) }

    val today = remember { todayEpochDay() }
    val form = rememberSaveable(saver = LimitFormState.Saver) {
        LimitFormState(startDay = today, endDay = today + (DEFAULT_PERIOD_DAYS - 1).toLong())
    }
    val presets = remember(today, isPersian) { buildPresets(today, isPersian) }

    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }
    var recentSpendToman by remember { mutableLongStateOf(0L) }
    var periodSpentToman by remember { mutableLongStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message -> snackbarHostState.showError(message) }
    }

    val accent = scheme.tertiary
    val onAccent = scheme.onTertiary

    val selectedCategory = categories.find { it.id == form.categoryId }

    // دسته‌ای که بعد از بازگشت از «مدیریت دسته‌ها» حذف شده باشد، از فرم هم پاک می‌شود
    // (فقط بعد از رسیدن فهرست؛ در لحظه‌ی اول فهرست خالی است و نباید انتخاب بازیابی‌شده را پاک کند).
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && form.hasCategory && categories.none { it.id == form.categoryId }) {
            form.categoryId = ""
        }
    }

    LaunchedEffect(form.categoryId) {
        recentSpendToman = if (form.hasCategory) viewModel.recentMonthlySpending(form.categoryId) else 0L
    }
    LaunchedEffect(form.categoryId, form.startDay, form.endDay) {
        periodSpentToman = if (form.hasCategory && form.isPeriodValid) {
            viewModel.spentBetween(form.categoryId, dayToMillis(form.startDay), dayEndMillis(form.endDay))
        } else 0L
    }

    val conflict = remember(limits, form.categoryId, form.startDay, form.endDay) {
        if (!form.hasCategory || !form.isPeriodValid) null
        else findOverlappingLimit(
            limits = limits,
            categoryId = form.categoryId,
            startMillis = dayToMillis(form.startDay),
            endMillis = dayEndMillis(form.endDay)
        )
    }
    // سقفِ جاری یا آینده‌ی همین دسته؛ فقط برای هشدار زودهنگام در مرحله‌ی دسته.
    val existingForCategory = remember(limits, form.categoryId) {
        val now = System.currentTimeMillis()
        limits.filter { it.categoryId == form.categoryId && it.endDate >= now }
            .minByOrNull { it.startDate }
    }

    fun goTo(step: Int) {
        form.step = step.coerceIn(STEP_CATEGORY, STEP_REVIEW)
    }

    val canSave = !isSaving && form.hasAmount && selectedCategory != null &&
            form.isPeriodValid && conflict == null

    fun save() {
        val category = selectedCategory
        if (!canSave || category == null) {
            resetKey++
            return
        }
        isSaving = true
        val storedToman = displayToStored(form.amountValue, currencyUnit)
        val categoryTitle = StringMapper.getCategoryName(category.title, isPersian)
        viewModel.saveBudgetLimit(
            categoryName = category.id,
            maxLimit = storedToman.toDouble(),
            startDate = dayToMillis(form.startDay),
            endDate = dayToMillis(form.endDay),
            onResult = { saved ->
                if (saved) {
                    successSheet = budgetLimitSavedContent(
                        isEdit = false,
                        categoryTitle = categoryTitle,
                        maxLimitToman = storedToman,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian
                    )
                } else {
                    // ثبت ناموفق: فرم سر جایش می‌ماند و خطا در اسنک‌بار نشان داده شده است.
                    isSaving = false
                    resetKey++
                }
            }
        )
    }

    BackHandler(enabled = form.step > STEP_CATEGORY && successSheet == null) {
        goTo(form.step - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        AuroraBackground()

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
        ) {
            StepTopBar(
                step = form.step,
                accent = accent,
                isPersian = isPersian,
                onBack = { if (form.step == STEP_CATEGORY) onClose() else goTo(form.step - 1) },
                totalSteps = STEP_COUNT
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
                label = "limitStep"
            ) { step ->
                when (step) {
                    STEP_CATEGORY -> LimitCategoryStep(
                        categories = categories,
                        selectedId = form.categoryId,
                        existingLimit = existingForCategory,
                        accent = accent,
                        onAccent = onAccent,
                        isPersian = isPersian,
                        onPick = { category ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            form.categoryId = category.id
                        },
                        onManage = onManageCategories,
                        onContinue = { goTo(STEP_AMOUNT) }
                    )

                    STEP_AMOUNT -> LimitAmountStep(
                        amount = form.amount,
                        category = selectedCategory,
                        recentSpendDisplay = MoneyContract.displayFromStorage(recentSpendToman, currencyUnit),
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        onAmountChange = { form.amount = it },
                        onChangeCategory = { goTo(STEP_CATEGORY) },
                        onContinue = { goTo(STEP_PERIOD) }
                    )

                    STEP_PERIOD -> LimitPeriodStep(
                        form = form,
                        presets = presets,
                        today = today,
                        conflict = conflict,
                        categoryName = selectedCategory?.let { StringMapper.getCategoryName(it.title, isPersian) }.orEmpty(),
                        accent = accent,
                        onAccent = onAccent,
                        isPersian = isPersian,
                        onContinue = { goTo(STEP_REVIEW) }
                    )

                    else -> LimitReviewStep(
                        form = form,
                        category = selectedCategory,
                        periodSpentToman = periodSpentToman,
                        accent = accent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        canSave = canSave,
                        resetKey = resetKey,
                        onEditAmount = { goTo(STEP_AMOUNT) },
                        onEditCategory = { goTo(STEP_CATEGORY) },
                        onEditPeriod = { goTo(STEP_PERIOD) },
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
            }
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۱: دسته‌بندی
// -----------------------------------------------------------------------------

@Composable
private fun LimitCategoryStep(
    categories: List<CategoryEntity>,
    selectedId: String,
    existingLimit: BudgetLimitEntity?,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onPick: (CategoryEntity) -> Unit,
    onManage: () -> Unit,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    // خطا فقط بعد از زدن «ادامه» بدون انتخاب نشان داده می‌شود، نه از همان اول.
    var showPickError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = tr(isPersian, "سقف برای کدوم دسته‌ست؟", "Which category is this limit for?"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tr(
                isPersian,
                "خرج‌های همین دسته با سقفی که تعیین می‌کنی مقایسه می‌شود.",
                "Spending in this category will be measured against the cap you set."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )

        if (showPickError) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = tr(isPersian, "یک دسته‌بندی انتخاب کن", "Pick a category"),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.error
            )
        }

        if (existingLimit != null) {
            Spacer(Modifier.height(10.dp))
            InfoBanner(
                text = tr(
                    isPersian,
                    "این دسته الان یک سقف دارد (از ${dayLabel(millisToDay(existingLimit.startDate), true)} تا ${dayLabel(millisToDay(existingLimit.endDate), true)}). بازه‌ی سقف جدید نباید با آن هم‌پوشانی داشته باشد.",
                    "This category already has a limit (${dayLabel(millisToDay(existingLimit.startDate), false)} – ${dayLabel(millisToDay(existingLimit.endDate), false)}). The new period must not overlap it."
                ),
                color = accent
            )
        }

        Spacer(Modifier.height(8.dp))

        if (categories.isEmpty()) {
            Text(
                text = tr(isPersian, "هنوز دسته‌بندی هزینه‌ای نداری؛ از «مدیریت» یکی بساز.", "You have no expense categories yet; create one from “Manage”."),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            // فاصله‌ی اطراف جای بزرگ‌شدنِ کارتِ انتخاب‌شده است تا بریده نشود.
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items = categories, key = { it.id }) { category ->
                CategoryTile(
                    emoji = category.iconEmoji,
                    name = StringMapper.getCategoryName(category.title, isPersian),
                    selected = category.id == selectedId,
                    suggested = false,
                    accent = accent,
                    onAccent = onAccent,
                    suggestedLabel = "",
                    onClick = {
                        showPickError = false
                        onPick(category)
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

        Spacer(Modifier.height(8.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = true,
            accent = accent,
            onAccent = onAccent,
            onClick = {
                if (selectedId.isBlank()) {
                    showPickError = true
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                } else {
                    onContinue()
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// مرحله ۲: سقف مبلغ
// -----------------------------------------------------------------------------

@Composable
private fun LimitAmountStep(
    amount: String,
    category: CategoryEntity?,
    recentSpendDisplay: Long,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    onAmountChange: (String) -> Unit,
    onChangeCategory: () -> Unit,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    // در ریال، میانبرها ده برابر می‌شوند تا همان ارزش را داشته باشند.
    val unitFactor = if (currencyUnit == MoneyContract.DISPLAY_RIAL) 10L else 1L
    val quickDeltas = remember(unitFactor) { listOf(100_000L, 500_000L, 1_000_000L).map { it * unitFactor } }
    val suggestion = remember(recentSpendDisplay) { roundUpNice(recentSpendDisplay) }
    val amountValue = amount.toLongOrNull() ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = tr(isPersian, "سقف خرجت چقدر باشه؟", "What's your spending cap?"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(10.dp))

        if (category != null) {
            // دسته‌ی انتخاب‌شده؛ لمسش به مرحله‌ی دسته برمی‌گردد.
            val shape = RoundedCornerShape(14.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(shape)
                    .background(accent.copy(alpha = 0.10f))
                    .border(1.dp, accent.copy(alpha = 0.25f), shape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = tr(isPersian, "تغییر دسته‌بندی", "Change category"),
                        onClick = onChangeCategory
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = category.iconEmoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = StringMapper.getCategoryName(category.title, isPersian),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AmountHero(amount = amount, accent = accent, currencyUnit = currencyUnit, isPersian = isPersian)
        }

        if (suggestion > 0L) {
            SuggestionPill(
                text = tr(
                    isPersian,
                    "خرج ۳۰ روز اخیر این دسته: ${formatSuccessAmount(recentSpendDisplay, currencyUnit, true)}",
                    "Last 30 days in this category: ${formatSuccessAmount(recentSpendDisplay, currencyUnit, false)}"
                ),
                actionLabel = tr(isPersian, "استفاده", "Use"),
                accent = accent,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    if (suggestion.toString().length <= MAX_AMOUNT_DIGITS) onAmountChange(suggestion.toString())
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickDeltas.forEach { delta ->
                val chipShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(chipShape)
                        .background(accent.copy(alpha = 0.10f))
                        .border(1.dp, accent.copy(alpha = 0.25f), chipShape)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAmountChange(addToAmount(amount, delta))
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
            onDigits = { digits -> onAmountChange(appendDigits(amount, digits)) },
            onBackspace = { onAmountChange(amount.dropLast(1)) },
            onClear = { onAmountChange("") }
        )

        Spacer(Modifier.height(12.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = amountValue > 0L,
            accent = accent,
            onAccent = onAccent,
            onClick = onContinue
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** پیشنهاد کوچک و لمس‌پذیر بالای کیبورد؛ کل ردیف کلیک می‌شود، نه فقط برچسب. */
@Composable
private fun SuggestionPill(text: String, actionLabel: String, accent: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.2f), shape)
            .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = "💡", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۳: بازه‌ی زمانی
// -----------------------------------------------------------------------------

@Composable
private fun LimitPeriodStep(
    form: LimitFormState,
    presets: List<PeriodPreset>,
    today: Long,
    conflict: BudgetLimitEntity?,
    categoryName: String,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

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
                text = tr(isPersian, "این سقف برای چه بازه‌ای است؟", "For which period?"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tr(
                    isPersian,
                    "فقط خرج‌های همین بازه حساب می‌شود. بعد از پایان بازه می‌توانی آن را تمدید کنی.",
                    "Only spending inside this period counts. You can renew it once it ends."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            PeriodEditor(
                startDay = form.startDay,
                endDay = form.endDay,
                minStartDay = today,
                presets = presets,
                accent = accent,
                isPersian = isPersian,
                conflict = conflict,
                conflictCategoryName = categoryName,
                onPreset = { preset ->
                    form.setStart(preset.startDay)
                    form.setEnd(preset.endDay)
                },
                onStartChange = form::setStart,
                onEndChange = form::setEnd
            )
        }

        Spacer(Modifier.height(8.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = form.isPeriodValid && conflict == null,
            accent = accent,
            onAccent = onAccent,
            onClick = onContinue
        )
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * ویرایشگر بازه: میانبرها + تاریخ شروع و پایان + مدت + هشدار هم‌پوشانی.
 * در صفحه‌ی ثبت و صفحه‌ی ویرایش یکسان استفاده می‌شود.
 */
@Composable
private fun PeriodEditor(
    startDay: Long,
    endDay: Long,
    minStartDay: Long,
    presets: List<PeriodPreset>,
    accent: Color,
    isPersian: Boolean,
    conflict: BudgetLimitEntity?,
    conflictCategoryName: String,
    onPreset: (PeriodPreset) -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    // ۰ = انتخاب تاریخ شروع، ۱ = تاریخ پایان؛ null = بسته.
    var picker by remember { mutableStateOf<Int?>(null) }
    val days = (endDay - startDay + 1L).coerceAtLeast(0L).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.chunked(2).forEach { rowPresets ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowPresets.forEach { preset ->
                    LimitChip(
                        label = tr(isPersian, preset.labelFa, preset.labelEn),
                        selected = preset.startDay == startDay && preset.endDay == endDay,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { onPreset(preset) }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DateCard(
                label = tr(isPersian, "از تاریخ", "From"),
                value = dayLabel(startDay, isPersian),
                accent = accent,
                modifier = Modifier.weight(1f),
                onClick = { picker = 0 }
            )
            DateCard(
                label = tr(isPersian, "تا تاریخ", "To"),
                value = dayLabel(endDay, isPersian),
                accent = accent,
                modifier = Modifier.weight(1f),
                onClick = { picker = 1 }
            )
        }

        Text(
            text = tr(isPersian, "مدت: ${daysText(days, true)}", "Duration: ${daysText(days, false)}"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (conflict != null) {
            InfoBanner(
                text = tr(
                    isPersian,
                    "برای «$conflictCategoryName» از ${dayLabel(millisToDay(conflict.startDate), true)} تا ${dayLabel(millisToDay(conflict.endDate), true)} سقف دیگری ثبت شده و با این بازه هم‌پوشانی دارد. تاریخ‌ها را تغییر بده.",
                    "“$conflictCategoryName” already has a limit from ${dayLabel(millisToDay(conflict.startDate), false)} to ${dayLabel(millisToDay(conflict.endDate), false)} that overlaps this period. Adjust the dates."
                ),
                color = scheme.error,
                showWarningIcon = true
            )
        }
    }

    when (picker) {
        0 -> WheelDatePickerDialog(
            isPersian = isPersian,
            title = tr(isPersian, "تاریخ شروع", "Start date"),
            initialMillis = dayToMillis(startDay),
            minDate = LocalDate.ofEpochDay(minOf(minStartDay, startDay)),
            onConfirm = { millis ->
                onStartChange(millisToDay(millis))
                picker = null
            },
            onDismiss = { picker = null }
        )

        1 -> WheelDatePickerDialog(
            isPersian = isPersian,
            title = tr(isPersian, "تاریخ پایان", "End date"),
            initialMillis = dayToMillis(endDay),
            minDate = LocalDate.ofEpochDay(startDay),
            onConfirm = { millis ->
                onEndChange(millisToDay(millis))
                picker = null
            },
            onDismiss = { picker = null }
        )
    }
}

@Composable
private fun LimitChip(
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
            .height(42.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else scheme.outline.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DateCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, scheme.outline.copy(alpha = 0.15f), shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/** نوار اطلاعات/هشدار کوچک؛ با [showWarningIcon] برای خطا و هم‌پوشانی. */
@Composable
private fun InfoBanner(text: String, color: Color, showWarningIcon: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), shape)
            .padding(12.dp)
    ) {
        if (showWarningIcon) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۴: مرور و ثبت
// -----------------------------------------------------------------------------

@Composable
private fun LimitReviewStep(
    form: LimitFormState,
    category: CategoryEntity?,
    periodSpentToman: Long,
    accent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    canSave: Boolean,
    resetKey: Int,
    onEditAmount: () -> Unit,
    onEditCategory: () -> Unit,
    onEditPeriod: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
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
                    text = tr(isPersian, "سقف مجاز", "Spending cap"),
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

            LimitReviewRow(
                label = tr(isPersian, "دسته‌بندی", "Category"),
                value = categoryName,
                leading = category?.iconEmoji,
                onClick = onEditCategory
            )
            LimitReviewRow(
                label = tr(isPersian, "بازه", "Period"),
                value = "${dayLabel(form.startDay, isPersian)}  ${if (isPersian) "←" else "→"}  ${dayLabel(form.endDay, isPersian)}",
                onClick = onEditPeriod
            )
            LimitInsightRows(
                days = form.dayCount,
                capDisplay = form.amountValue,
                periodSpentToman = periodSpentToman,
                currencyUnit = currencyUnit,
                isPersian = isPersian
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))

        SwipeToConfirmButton(
            text = tr(isPersian, "برای ثبت سقف بکشید", "Swipe to set the limit"),
            isPersian = isPersian,
            resetTrigger = resetKey,
            onConfirm = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            accentColor = accent
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LimitReviewRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    leading: String? = null,
    valueColor: Color = Color.Unspecified
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
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
                color = if (valueColor == Color.Unspecified) scheme.onSurface else valueColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
    }
}

/**
 * جزئیات مخصوص سقف: مدت، سقف روزانه و مصرف تا امروز در همین بازه.
 * اگر هم‌اکنون از سقف جدید گذشته باشی، همان‌جا هشدار داده می‌شود.
 */
@Composable
private fun LimitInsightRows(
    days: Int,
    capDisplay: Long,
    periodSpentToman: Long,
    currencyUnit: String,
    isPersian: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val capStored = displayToStored(capDisplay, currencyUnit)
    val perDay = if (days > 0) capDisplay / days else 0L
    val spentDisplay = MoneyContract.displayFromStorage(periodSpentToman, currencyUnit)
    val isOver = capStored > 0L && periodSpentToman > capStored
    val percent = if (capStored > 0L) (periodSpentToman * 100L / capStored).toInt() else 0

    LimitReviewRow(
        label = tr(isPersian, "مدت", "Duration"),
        value = daysText(days, isPersian)
    )
    if (perDay > 0L) {
        LimitReviewRow(
            label = tr(isPersian, "سقف روزانه", "Daily allowance"),
            value = tr(
                isPersian,
                "حدود ${formatSuccessAmount(perDay, currencyUnit, true)}",
                "About ${formatSuccessAmount(perDay, currencyUnit, false)}"
            )
        )
    }
    if (periodSpentToman > 0L) {
        LimitReviewRow(
            label = tr(isPersian, "خرج تا امروز", "Spent so far"),
            value = "${formatSuccessAmount(spentDisplay, currencyUnit, isPersian)} (${percentText(percent, isPersian)})",
            valueColor = if (isOver) scheme.error else Color.Unspecified
        )
    }
    if (isOver) {
        Spacer(Modifier.height(8.dp))
        InfoBanner(
            text = tr(
                isPersian,
                "با این سقف، همین الان از حد گذشته‌ای؛ سقف را بالاتر ببر یا بازه را از تاریخ دیگری شروع کن.",
                "You're already over this cap. Raise it or start the period on a later date."
            ),
            color = scheme.error,
            showWarningIcon = true
        )
    }
}

// -----------------------------------------------------------------------------
// ویرایش سقف (صفحه‌ی تک‌پنل)
// -----------------------------------------------------------------------------

/**
 * ویرایش تمام‌صفحه‌ی یک سقف، با همان زبان طراحی ویرایش تراکنش:
 * مبلغ بزرگ و لمس‌پذیر (با لمس، کیبورد اختصاصی باز می‌شود)، ردیف افقی دسته‌ها،
 * ویرایشگر بازه و دکمه‌ی سوایپ که تا زمان ایجاد تغییر غیرفعال است.
 */
@Composable
fun EditBudgetLimitScreen(
    limitId: String,
    onClose: () -> Unit = {},
    viewModel: BudgetLimitViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    var original by remember { mutableStateOf<BudgetLimitEntity?>(null) }

    LaunchedEffect(limitId) {
        val found = viewModel.findLimit(limitId)
        // سقف (مثلاً بین دو صفحه) حذف یا منقضی شده؛ بی‌صدا برمی‌گردیم.
        if (found == null || System.currentTimeMillis() > found.endDate) onClose() else original = found
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()
        val limit = original
        if (limit == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LimitEditorContent(limit = limit, isPersian = isPersian, viewModel = viewModel, onClose = onClose)
        }
    }
}

@Composable
private fun LimitEditorContent(
    limit: BudgetLimitEntity,
    isPersian: Boolean,
    viewModel: BudgetLimitViewModel,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()
    val rawCategories by viewModel.expenseCategories.collectAsState()
    val limits by viewModel.allLimits.collectAsState()
    val categories = remember(rawCategories) { selectableLimitCategories(rawCategories) }

    val today = remember { todayEpochDay() }
    val initialStartDay = remember(limit.id) { millisToDay(limit.startDate) }
    val initialEndDay = remember(limit.id) { millisToDay(limit.endDate) }
    val initialAmount = remember(limit.id, currencyUnit) {
        MoneyContract.displayFromStorage(limit.maxLimit, currencyUnit).toString().take(MAX_AMOUNT_DIGITS)
    }
    val presets = remember(today, isPersian) { buildPresets(today, isPersian) }

    val form = rememberSaveable(limit.id, saver = LimitFormState.Saver) {
        LimitFormState(
            categoryId = limit.categoryId,
            amount = initialAmount,
            startDay = initialStartDay,
            endDay = initialEndDay
        )
    }
    var editingAmount by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var periodSpentToman by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message -> snackbarHostState.showError(message) }
    }

    val accent = scheme.tertiary
    val onAccent = scheme.onTertiary
    val selectedCategory = categories.find { it.id == form.categoryId }

    LaunchedEffect(form.categoryId, form.startDay, form.endDay) {
        periodSpentToman = if (form.hasCategory && form.isPeriodValid) {
            viewModel.spentBetween(form.categoryId, dayToMillis(form.startDay), dayEndMillis(form.endDay))
        } else 0L
    }

    val conflict = remember(limits, form.categoryId, form.startDay, form.endDay) {
        if (!form.hasCategory || !form.isPeriodValid) null
        else findOverlappingLimit(
            limits = limits,
            categoryId = form.categoryId,
            startMillis = dayToMillis(form.startDay),
            endMillis = dayEndMillis(form.endDay),
            excludeId = limit.id
        )
    }

    // مقادیر نهایی و تشخیص تغییر
    val newStoredAmount = displayToStored(form.amountValue, currencyUnit)
    val hasChanges = form.categoryId != limit.categoryId ||
            newStoredAmount != limit.maxLimit ||
            form.startDay != initialStartDay ||
            form.endDay != initialEndDay

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun save() {
        val category = selectedCategory
        // دستگیره‌ی سوایپ به ابتدا برمی‌گردد و مشکل نشان داده می‌شود.
        if (!form.hasAmount || category == null || !form.isPeriodValid || conflict != null) {
            resetKey++
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            when {
                !form.hasAmount -> {
                    editingAmount = true
                    toast(tr(isPersian, "مبلغ رو وارد کن", "Enter an amount"))
                }

                category == null -> toast(tr(isPersian, "یک دسته‌بندی انتخاب کن", "Pick a category"))
                conflict != null -> toast(
                    tr(
                        isPersian,
                        "بازه با سقف دیگری از همین دسته هم‌پوشانی دارد",
                        "The period overlaps another limit in this category"
                    )
                )

                else -> toast(tr(isPersian, "بازه‌ی تاریخ معتبر نیست", "The date range isn't valid"))
            }
            return
        }
        isSaving = true
        val categoryTitle = StringMapper.getCategoryName(category.title, isPersian)
        viewModel.saveBudgetLimit(
            categoryName = category.id,
            maxLimit = newStoredAmount.toDouble(),
            startDate = dayToMillis(form.startDay),
            endDate = dayToMillis(form.endDay),
            limitId = limit.id,
            onResult = { saved ->
                if (saved) {
                    successSheet = budgetLimitSavedContent(
                        isEdit = true,
                        categoryTitle = categoryTitle,
                        maxLimitToman = newStoredAmount,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian
                    )
                } else {
                    isSaving = false
                    resetKey++
                }
            }
        )
    }

    BackHandler(enabled = editingAmount && successSheet == null) { editingAmount = false }

    Box(modifier = Modifier.fillMaxSize()) {
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
        ) {
            LimitEditTopBar(
                title = tr(isPersian, "ویرایش سقف بودجه", "Edit budget limit"),
                closeLabel = tr(isPersian, "بستن", "Close"),
                onClose = { if (editingAmount) editingAmount = false else onClose() }
            )

            // مبلغ: لمس‌پذیر؛ با لمس، کیبورد اختصاصی جای فیلدها را می‌گیرد.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !editingAmount) { editingAmount = true }
                    .padding(vertical = 8.dp)
            ) {
                AmountHero(amount = form.amount, accent = accent, currencyUnit = currencyUnit, isPersian = isPersian)
                if (!editingAmount) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(isPersian, "برای تغییر سقف لمس کن", "Tap to change the cap"),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }

            AnimatedContent(
                targetState = editingAmount,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    (slideInVertically(tween(260)) { it / 6 } + fadeIn(tween(260)))
                        .togetherWith(slideOutVertically(tween(200)) { it / 6 } + fadeOut(tween(160)))
                },
                label = "limitEditorPanel"
            ) { editing ->
                if (editing) {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        NumericKeypad(
                            isPersian = isPersian,
                            onDigits = { digits -> form.amount = appendDigits(form.amount, digits) },
                            onBackspace = { form.amount = form.amount.dropLast(1) },
                            onClear = { form.amount = "" }
                        )
                        Spacer(Modifier.height(12.dp))
                        PrimaryActionButton(
                            text = tr(isPersian, "تأیید مبلغ", "Confirm amount"),
                            enabled = form.hasAmount && form.amount.length <= MAX_AMOUNT_DIGITS,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { editingAmount = false }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    LimitDetailsPanel(
                        form = form,
                        categories = categories,
                        presets = presets,
                        minStartDay = today,
                        conflict = conflict,
                        categoryName = selectedCategory?.let { StringMapper.getCategoryName(it.title, isPersian) }.orEmpty(),
                        periodSpentToman = periodSpentToman,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        onCategoryPicked = { category ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            form.categoryId = category.id
                        }
                    ) {
                        SwipeToConfirmButton(
                            text = tr(isPersian, "برای ذخیره بکشید", "Swipe to save"),
                            isPersian = isPersian,
                            resetTrigger = resetKey,
                            onConfirm = ::save,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasChanges && !isSaving,
                            accentColor = accent,
                            onBlockedAttempt = {
                                if (!isSaving) {
                                    toast(tr(isPersian, "هنوز تغییری ایجاد نکردی", "Nothing has changed yet"))
                                }
                            }
                        )
                    }
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
                onClose()
            }
        )
    }
}

@Composable
private fun LimitEditTopBar(title: String, closeLabel: String, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = closeLabel, tint = scheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        // هم‌وزن دکمه‌ی بستن تا عنوان وسط بماند.
        Spacer(Modifier.width(48.dp))
    }
}

/** فیلدهای قابل‌ویرایش؛ دکمه‌ی ذخیره از بیرون در [action] داده می‌شود تا زیر اسکرول ثابت بماند. */
@Composable
private fun LimitDetailsPanel(
    form: LimitFormState,
    categories: List<CategoryEntity>,
    presets: List<PeriodPreset>,
    minStartDay: Long,
    conflict: BudgetLimitEntity?,
    categoryName: String,
    periodSpentToman: Long,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    onCategoryPicked: (CategoryEntity) -> Unit,
    action: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // دسته‌ی فعلی در ردیف افقی بیرون از دید نماند.
    LaunchedEffect(categories.size) {
        val index = categories.indexOfFirst { it.id == form.categoryId }
        if (index > 0) listState.scrollToItem(index)
    }

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
            Spacer(Modifier.height(8.dp))

            Text(
                text = tr(isPersian, "دسته‌بندی", "Category"),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                // فاصله‌ی اطراف جای بزرگ‌شدنِ کارتِ انتخاب‌شده است تا بریده نشود.
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(items = categories, key = { it.id }) { category ->
                    Box(modifier = Modifier.width(100.dp)) {
                        CategoryTile(
                            emoji = category.iconEmoji,
                            name = StringMapper.getCategoryName(category.title, isPersian),
                            selected = category.id == form.categoryId,
                            suggested = false,
                            accent = accent,
                            onAccent = onAccent,
                            suggestedLabel = "",
                            onClick = { onCategoryPicked(category) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = tr(isPersian, "بازه‌ی زمانی", "Period"),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            PeriodEditor(
                startDay = form.startDay,
                endDay = form.endDay,
                minStartDay = minStartDay,
                presets = presets,
                accent = accent,
                isPersian = isPersian,
                conflict = conflict,
                conflictCategoryName = categoryName,
                onPreset = { preset ->
                    form.setStart(preset.startDay)
                    form.setEnd(preset.endDay)
                },
                onStartChange = form::setStart,
                onEndChange = form::setEnd
            )

            Spacer(Modifier.height(8.dp))

            LimitInsightRows(
                days = form.dayCount,
                capDisplay = form.amountValue,
                periodSpentToman = periodSpentToman,
                currencyUnit = currencyUnit,
                isPersian = isPersian
            )

            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        action()
        Spacer(Modifier.height(16.dp))
    }
}