package ir.hamedan.budgetmanagement.ui.screens.goals

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
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
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmount
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.screens.add.NumericKeypad
import ir.hamedan.budgetmanagement.ui.screens.add.PrimaryActionButton
import ir.hamedan.budgetmanagement.ui.screens.add.addToAmount
import ir.hamedan.budgetmanagement.ui.screens.add.amountInWords
import ir.hamedan.budgetmanagement.ui.screens.add.appendDigits
import ir.hamedan.budgetmanagement.ui.screens.add.formatNumber
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.ui.screens.add.unitLabel
import java.util.Calendar
import kotlin.math.ceil

private const val STEP_TITLE = 0
private const val STEP_ICON = 1
private const val STEP_AMOUNT = 2
private const val STEP_REVIEW = 3
private const val STEP_COUNT = 4

private const val FIELD_TARGET = 0
private const val FIELD_MONTHLY = 1

private const val MAX_GOAL_TITLE_LENGTH = 25

// -----------------------------------------------------------------------------
// عنوان‌های پیشنهادی
// -----------------------------------------------------------------------------

private class GoalSuggestion(val fa: String, val en: String, val emoji: String)

/** هر پیشنهاد، آیکون مناسب خودش را هم همراه دارد (همه روی کره موجودند). */
private val GOAL_SUGGESTIONS = listOf(
    GoalSuggestion("خرید خودرو", "Buy a car", "🚗"),
    GoalSuggestion("خرید خانه", "Buy a home", "🏠"),
    GoalSuggestion("ازدواج", "Wedding", "💍"),
    GoalSuggestion("سفر خارجی", "Trip abroad", "✈️"),
    GoalSuggestion("سفر زیارتی", "Pilgrimage", "🕋"),
    GoalSuggestion("خرید لپ‌تاپ", "Buy a laptop", "💻"),
    GoalSuggestion("خرید گوشی", "Buy a phone", "📱"),
    GoalSuggestion("هزینه تحصیل", "Tuition", "🎓"),
    GoalSuggestion("خرید موتور", "Buy a scooter", "🛵"),
    GoalSuggestion("صندوق اضطراری", "Emergency fund", "🏦"),
    GoalSuggestion("خرید طلا", "Buy gold", "🪙"),
    GoalSuggestion("بازسازی خانه", "Home renovation", "🛠️"),
    GoalSuggestion("لوازم خانگی", "Home appliances", "🧺"),
    GoalSuggestion("شروع کسب‌وکار", "Start a business", "💼"),
    GoalSuggestion("هزینه فرزند", "Baby fund", "👶"),
    GoalSuggestion("خرید هدیه", "Buy a gift", "🎁")
)

// -----------------------------------------------------------------------------
// وضعیت فرم
// -----------------------------------------------------------------------------

/**
 * وضعیت فرم ثبت/ویرایش قلک؛ با [Saver] در چرخش صفحه و بازگشت از پس‌زمینه حفظ می‌شود.
 * مبلغ‌ها فقط به‌صورت رقم‌های انگلیسی (بدون جداکننده) و **به واحد نمایشی** (تومان یا ریال) نگه‌داری می‌شوند.
 */
@Stable
internal class GoalFormState(
    title: String = "",
    icon: String = "",
    iconManual: Boolean = false,
    target: String = "",
    monthly: String = "",
    step: Int = STEP_TITLE,
    field: Int = FIELD_TARGET,
    returnToReview: Boolean = false
) {
    var title by mutableStateOf(title)
    var icon by mutableStateOf(icon)

    /** کاربر خودش روی کره آیکون انتخاب کرده؛ دیگر عنوان پیشنهادی آیکون را عوض نمی‌کند. */
    var iconManual by mutableStateOf(iconManual)
    var target by mutableStateOf(target)
    var monthly by mutableStateOf(monthly)
    var step by mutableStateOf(step)

    /** فیلد فعالِ مرحله‌ی مبلغ: هدف یا واریز ماهانه. */
    var field by mutableStateOf(field)

    /** بعد از پرش از مرور به یک مرحله، «ادامه» مستقیم به مرور برمی‌گردد. */
    var returnToReview by mutableStateOf(returnToReview)

    val targetValue: Long get() = target.toLongOrNull() ?: 0L
    val monthlyValue: Long get() = monthly.toLongOrNull() ?: 0L
    val hasIcon: Boolean get() = icon.isNotBlank()

    companion object {
        val Saver = listSaver<GoalFormState, Any>(
            save = {
                listOf(
                    it.title, it.icon, it.iconManual, it.target,
                    it.monthly, it.step, it.field, it.returnToReview
                )
            },
            restore = {
                GoalFormState(
                    title = it[0] as String,
                    icon = it[1] as String,
                    iconManual = it[2] as Boolean,
                    target = it[3] as String,
                    monthly = it[4] as String,
                    step = it[5] as Int,
                    field = it[6] as Int,
                    returnToReview = it[7] as Boolean
                )
            }
        )
    }
}

private fun displayRaw(storedToman: Long, currencyUnit: String): String =
    (if (currencyUnit == "IRR") storedToman * 10L else storedToman).toString()

private fun toStoredToman(displayValue: Long, currencyUnit: String): Long =
    if (currencyUnit == "IRR") displayValue / 10L else displayValue

private enum class TitleError { None, Blank, Duplicate }

// -----------------------------------------------------------------------------
// صفحه‌ی اصلی
// -----------------------------------------------------------------------------

/**
 * ثبت و ویرایش قلک به‌صورت جریان تمام‌صفحه‌ی مرحله‌ای (جایگزین دیالوگ قدیمی):
 * ۱) عنوان (با پیشنهادها) ← ۲) آیکون (کره‌ی سه‌بعدی) ← ۳) مبلغ هدف و واریز ماهانه ← ۴) مرور و ثبت با کشیدن.
 *
 * @param goalId null یعنی قلک جدید؛ در غیر این صورت ویرایش همان قلک (و شروع از مرحله‌ی مرور).
 * @param onClose خروج بدون ثبت
 * @param onFinished بعد از ثبت موفق و بسته شدن شیت موفقیت
 */
@Composable
fun GoalEditorScreen(
    goalId: String? = null,
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    viewModel: SavingGoalsViewModel = appViewModel()
) {
    // این ViewModel فقط وقتی لیست را می‌شناسد که کسی آن را بخواند؛ بررسی نام تکراری به آن وابسته است.
    val goals by viewModel.savingGoals.collectAsState()
    val loaded = goals

    if (goalId == null) {
        GoalEditorContent(
            goalId = null,
            original = null,
            goals = loaded.orEmpty(),
            viewModel = viewModel,
            onClose = onClose,
            onFinished = onFinished
        )
        return
    }

    val original = loaded?.firstOrNull { it.id == goalId }
    if (loaded != null && original == null) {
        // قلک حذف شده یا پیدا نشد (مثلاً بعد از همگام‌سازی)
        LaunchedEffect(Unit) { onClose() }
    }
    if (original == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { AuroraBackground() }
        return
    }
    GoalEditorContent(
        goalId = goalId,
        original = original,
        goals = loaded.orEmpty(),
        viewModel = viewModel,
        onClose = onClose,
        onFinished = onFinished
    )
}

@Composable
private fun GoalEditorContent(
    goalId: String?,
    original: SavingGoalEntity?,
    goals: List<SavingGoalEntity>,
    viewModel: SavingGoalsViewModel,
    onClose: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()
    val isEdit = original != null

    val form = rememberSaveable(goalId, saver = GoalFormState.Saver) {
        if (original == null) {
            GoalFormState()
        } else {
            GoalFormState(
                title = original.title,
                icon = original.icon,
                iconManual = true,
                target = displayRaw(original.targetAmount, currencyUnit),
                monthly = if (original.monthlyAmount > 0L) displayRaw(original.monthlyAmount, currencyUnit) else "",
                step = STEP_REVIEW
            )
        }
    }

    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.depositError.collect { message -> snackbarHostState.showError(message) }
    }

    // قلک قدیمی ممکن است آیکونی خارج از کاتالوگ داشته باشد (مثلاً همگام‌سازی از دستگاه دیگر)؛ آن را هم روی کره می‌گذاریم.
    val iconOptions = remember(original?.icon) {
        val own = original?.icon
        if (own.isNullOrBlank() || GOAL_ICON_OPTIONS.any { sameEmoji(it.emoji, own) }) {
            GOAL_ICON_OPTIONS
        } else {
            GOAL_ICON_OPTIONS + GoalIconOption(own, "فعلی", "Current")
        }
    }

    val accent = scheme.primary
    val onAccent = scheme.onPrimary

    val storedTarget = toStoredToman(form.targetValue, currencyUnit)
    val canSave = !isSaving && form.title.isNotBlank() && form.hasIcon && storedTarget > 0L

    fun goTo(step: Int) {
        focusManager.clearFocus()
        form.step = step.coerceIn(STEP_TITLE, STEP_REVIEW)
    }

    /** پرش از صفحه‌ی مرور به یک مرحله؛ «ادامه» در آن مرحله دوباره به مرور برمی‌گردد. */
    fun jumpTo(step: Int, field: Int = form.field) {
        form.returnToReview = true
        form.field = field
        goTo(step)
    }

    fun handleBack() {
        when {
            form.step != STEP_REVIEW && form.returnToReview -> goTo(STEP_REVIEW)
            form.step == STEP_REVIEW && isEdit -> onClose()
            form.step == STEP_TITLE -> onClose()
            form.step == STEP_AMOUNT && form.field == FIELD_MONTHLY -> form.field = FIELD_TARGET
            else -> goTo(form.step - 1)
        }
    }

    fun advanceFrom(step: Int) {
        goTo(if (form.returnToReview) STEP_REVIEW else step + 1)
    }

    fun save() {
        if (!canSave) return
        isSaving = true
        val title = form.title.trim()
        val target = storedTarget
        val monthly = toStoredToman(form.monthlyValue, currencyUnit)
        val icon = form.icon
        if (original != null) {
            viewModel.updateGoal(
                original.copy(title = title, targetAmount = target, monthlyAmount = monthly, icon = icon)
            ) { saved ->
                // ناموفق: فرم سر جایش می‌ماند و اسنک‌بار خطا نشان داده می‌شود.
                if (saved) successSheet = goalUpdatedContent(title, isPersian) else isSaving = false
            }
        } else {
            viewModel.addGoal(title, target, monthly, icon) { saved ->
                if (saved) {
                    successSheet = goalCreatedContent(title, target, currencyUnit, isPersian)
                } else {
                    isSaving = false
                }
            }
        }
    }

    BackHandler(enabled = successSheet == null) { handleBack() }

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
                .imePadding()
        ) {
            GoalTopBar(
                step = form.step,
                showClose = if (isEdit) form.step == STEP_REVIEW else (form.step == STEP_TITLE && !form.returnToReview),
                accent = accent,
                isPersian = isPersian,
                onBack = ::handleBack
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
                label = "goalStep"
            ) { step ->
                when (step) {
                    STEP_TITLE -> GoalTitleStep(
                        form = form,
                        goals = goals,
                        ownId = original?.id,
                        accent = accent,
                        onAccent = onAccent,
                        isPersian = isPersian,
                        onContinue = { advanceFrom(STEP_TITLE) }
                    )

                    STEP_ICON -> GoalIconStep(
                        form = form,
                        options = iconOptions,
                        accent = accent,
                        onAccent = onAccent,
                        isPersian = isPersian,
                        onContinue = { advanceFrom(STEP_ICON) }
                    )

                    STEP_AMOUNT -> GoalAmountStep(
                        form = form,
                        original = original,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        onContinue = { advanceFrom(STEP_AMOUNT) }
                    )

                    else -> GoalReviewStep(
                        form = form,
                        original = original,
                        accent = accent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        isEdit = isEdit,
                        canSave = canSave,
                        onEditTitle = { jumpTo(STEP_TITLE) },
                        onEditIcon = { jumpTo(STEP_ICON) },
                        onEditTarget = { jumpTo(STEP_AMOUNT, FIELD_TARGET) },
                        onEditMonthly = { jumpTo(STEP_AMOUNT, FIELD_MONTHLY) },
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
// نوار بالا
// -----------------------------------------------------------------------------

@Composable
private fun GoalTopBar(
    step: Int,
    showClose: Boolean,
    accent: Color,
    isPersian: Boolean,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            if (showClose) {
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
            repeat(STEP_COUNT) { index ->
                val color by animateColorAsState(
                    targetValue = if (index <= step) accent else scheme.onSurface.copy(alpha = 0.12f),
                    animationSpec = tween(300),
                    label = "goalProgress$index"
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

// -----------------------------------------------------------------------------
// مرحله ۱: عنوان + پیشنهادها
// -----------------------------------------------------------------------------

@Composable
private fun GoalTitleStep(
    form: GoalFormState,
    goals: List<SavingGoalEntity>,
    ownId: String?,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val titleFocus = remember { FocusRequester() }
    var error by remember { mutableStateOf(TitleError.None) }

    fun updateTitle(input: String) {
        form.title = input.take(MAX_GOAL_TITLE_LENGTH)
        if (input.isNotBlank()) error = TitleError.None
    }

    fun applySuggestion(suggestion: GoalSuggestion) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        updateTitle(if (isPersian) suggestion.fa else suggestion.en)
        // آیکونی که کاربر خودش انتخاب کرده با پیشنهاد عوض نمی‌شود.
        if (!form.iconManual || !form.hasIcon) {
            form.icon = suggestion.emoji
            form.iconManual = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = tr(isPersian, "برای چی پس‌انداز می‌کنی؟", "What are you saving for?"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.title,
            onValueChange = ::updateTitle,
            label = { Text(tr(isPersian, "عنوان قلک (مثلاً: خرید لپ‌تاپ)", "Goal title")) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocus),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            isError = error != TitleError.None,
            supportingText = when (error) {
                TitleError.Blank -> {
                    { Text(tr(isPersian, "یه اسم کوتاه برای قلک بنویس یا یکی از پیشنهادها رو بزن", "Add a short name or tap a suggestion")) }
                }

                TitleError.Duplicate -> {
                    { Text(tr(isPersian, "قلکی با همین نام از قبل وجود دارد", "A goal with this name already exists")) }
                }

                TitleError.None -> null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
                cursorColor = accent
            ),
            trailingIcon = {
                VoiceInputButton(
                    onResult = { spoken -> updateTitle(spoken) },
                    language = if (isPersian) "fa-IR" else "en-US"
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = tr(isPersian, "پیشنهادها", "Suggestions"),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GOAL_SUGGESTIONS.forEach { suggestion ->
                    val label = if (isPersian) suggestion.fa else suggestion.en
                    GoalSuggestionChip(
                        emoji = suggestion.emoji,
                        label = label,
                        selected = form.title.trim().equals(label, ignoreCase = true),
                        accent = accent,
                        onClick = { applySuggestion(suggestion) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = true,
            accent = accent,
            onAccent = onAccent,
            onClick = {
                val trimmed = form.title.trim()
                when {
                    trimmed.isEmpty() -> {
                        error = TitleError.Blank
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        titleFocus.requestFocus()
                    }

                    goals.any { it.id != ownId && it.title.trim().equals(trimmed, ignoreCase = true) } -> {
                        error = TitleError.Duplicate
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        titleFocus.requestFocus()
                    }

                    else -> onContinue()
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GoalSuggestionChip(
    emoji: String,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else scheme.outline.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 17.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else scheme.onSurface,
            maxLines = 1
        )
    }
}

// -----------------------------------------------------------------------------
// مرحله ۲: آیکون روی کره‌ی سه‌بعدی
// -----------------------------------------------------------------------------

@Composable
private fun GoalIconStep(
    form: GoalFormState,
    options: List<GoalIconOption>,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var showError by remember { mutableStateOf(false) }
    val current = options.firstOrNull { sameEmoji(it.emoji, form.icon) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = tr(isPersian, "یه آیکون برای قلکت انتخاب کن", "Pick an icon for your goal"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tr(
                isPersian,
                "کره رو به هر طرف بچرخون و آیکون دلخواهت رو لمس کن",
                "Spin the sphere in any direction and tap the icon you like"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoalIconSphere(
                options = options,
                selected = form.icon.ifBlank { null },
                onSelect = { emoji ->
                    form.icon = emoji
                    form.iconManual = true
                    showError = false
                },
                accent = accent,
                isPersian = isPersian,
                modifier = Modifier.fillMaxSize()
            )
        }

        // وضعیت انتخاب: نام آیکون، نشان «پیشنهادی» یا راهنما
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                showError && current == null -> Text(
                    text = tr(isPersian, "یک آیکون انتخاب کن", "Pick an icon"),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.error
                )

                current != null -> {
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .clip(shape)
                            .background(accent.copy(alpha = 0.12f))
                            .border(1.dp, accent.copy(alpha = 0.3f), shape)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = current.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = current.label(isPersian),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        if (!form.iconManual) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tr(isPersian, "✨ پیشنهاد بر اساس عنوان", "✨ Suggested from title"),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> Text(
                    text = tr(isPersian, "هنوز آیکونی انتخاب نشده", "No icon selected yet"),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
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
                if (!form.hasIcon) {
                    showError = true
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
// مرحله ۳: مبلغ هدف و واریز ماهانه
// -----------------------------------------------------------------------------

@Composable
private fun GoalAmountStep(
    form: GoalFormState,
    original: SavingGoalEntity?,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var showTargetError by remember { mutableStateOf(false) }
    val isTarget = form.field == FIELD_TARGET
    val rawValue = if (isTarget) form.target else form.monthly

    // در ریال، میانبرها ده برابر می‌شوند تا همان ارزش را داشته باشند.
    val unitFactor = if (currencyUnit == "IRR") 10L else 1L
    val quickDeltas = remember(isTarget, unitFactor) {
        (if (isTarget) listOf(1_000_000L, 10_000_000L, 100_000_000L) else listOf(100_000L, 500_000L, 1_000_000L))
            .map { it * unitFactor }
    }

    fun setRaw(value: String) {
        if (isTarget) form.target = value else form.monthly = value
        if (form.targetValue > 0L) showTargetError = false
    }

    // برآورد زمان رسیدن به هدف؛ در ویرایش، موجودی فعلیِ قلک از مبلغ باقی‌مانده کم می‌شود.
    val ownedDisplay = original?.let { it.currentAmount * unitFactor } ?: 0L
    val remaining = form.targetValue - ownedDisplay
    val months = if (form.monthlyValue > 0L && remaining > 0L) {
        ceil(remaining.toDouble() / form.monthlyValue.toDouble()).toInt()
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        GoalFieldToggle(
            field = form.field,
            isPersian = isPersian,
            onSelect = { field ->
                if (field != form.field) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    form.field = field
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = if (isTarget) {
                tr(isPersian, "هدفت چقدر پول می‌خواد؟", "How much do you need?")
            } else {
                tr(isPersian, "هر ماه چقدر کنار بذاری؟ (اختیاری)", "How much per month? (optional)")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GoalAmountHero(
                raw = rawValue,
                accent = accent,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                note = when {
                    showTargetError -> tr(isPersian, "اول مبلغ هدف رو وارد کن", "Enter the target amount first")
                    !isTarget && months > 0 -> estimateText(months, isPersian)
                    !isTarget -> tr(
                        isPersian,
                        "هر ماه این مبلغ از موجودی کم و به قلک اضافه می‌شود",
                        "This amount moves from your balance into the goal every month"
                    )

                    else -> null
                },
                noteIsError = showTargetError
            )
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
                            setRaw(addToAmount(rawValue, delta))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+" + formatNumber(delta, isPersian),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        NumericKeypad(
            isPersian = isPersian,
            onDigits = { digits -> setRaw(appendDigits(rawValue, digits)) },
            onBackspace = { setRaw(rawValue.dropLast(1)) },
            onClear = { setRaw("") }
        )

        Spacer(Modifier.height(12.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = true,
            accent = accent,
            onAccent = onAccent,
            onClick = {
                when {
                    form.targetValue <= 0L || (currencyUnit == "IRR" && form.targetValue < 10L) -> {
                        // مبلغ هدف اجباری است؛ اگر روی تب ماهانه هستیم برمی‌گردیم به هدف.
                        showTargetError = true
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        form.field = FIELD_TARGET
                    }

                    isTarget && !form.returnToReview -> {
                        // بعد از هدف، یک‌بار هم واریز ماهانه را نشان بده؛ خالی گذاشتنش مجاز است.
                        form.field = FIELD_MONTHLY
                    }

                    else -> onContinue()
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun estimateText(months: Int, isPersian: Boolean): String {
    fun n(v: Int) = if (isPersian) TransactionShareFormatter.toPersianDigits(v.toString()) else v.toString()
    val years = months / 12
    val rest = months % 12
    return when {
        months > 1200 -> tr(isPersian, "رسیدن به هدف با این مبلغ خیلی طول می‌کشد", "At this pace the goal is very far away")
        years == 0 -> tr(isPersian, "حدود ${n(months)} ماه تا رسیدن به هدف", "About ${n(months)} months to reach your goal")
        rest == 0 -> tr(isPersian, "حدود ${n(years)} سال تا رسیدن به هدف", "About ${n(years)} years to reach your goal")
        else -> tr(
            isPersian,
            "حدود ${n(years)} سال و ${n(rest)} ماه تا رسیدن به هدف",
            "About ${n(years)} years and ${n(rest)} months to reach your goal"
        )
    }
}

/** دو بخش «مبلغ هدف | واریز ماهانه»؛ هم‌سبک سوییچ هزینه/درآمد در ثبت تراکنش. */
@Composable
private fun GoalFieldToggle(field: Int, isPersian: Boolean, onSelect: (Int) -> Unit) {
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
        FieldSegment(
            label = tr(isPersian, "مبلغ هدف", "Target"),
            selected = field == FIELD_TARGET,
            onClick = { onSelect(FIELD_TARGET) },
            modifier = Modifier.weight(1f)
        )
        FieldSegment(
            label = tr(isPersian, "واریز ماهانه", "Monthly"),
            selected = field == FIELD_MONTHLY,
            onClick = { onSelect(FIELD_MONTHLY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FieldSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) scheme.primary else Color.Transparent,
        animationSpec = tween(250),
        label = "fieldSegmentBg"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "fieldSegmentFg"
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

/** عدد بزرگ مبلغ، واحد، مبلغ به حروف و یک یادداشت اختیاری زیر آن. */
@Composable
private fun GoalAmountHero(
    raw: String,
    accent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    note: String?,
    noteIsError: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val value = raw.toLongOrNull() ?: 0L
    val isEmpty = raw.isEmpty()
    val display = formatNumber(value, isPersian)
    // اندازه‌ی فونت با طول عدد کم می‌شود تا ۱۲ رقم هم در یک خط جا شود.
    val fontSize = when {
        display.length <= 8 -> 56.sp
        display.length <= 11 -> 46.sp
        display.length <= 14 -> 38.sp
        else -> 32.sp
    }

    val pop = remember { Animatable(1f) }
    LaunchedEffect(raw) {
        if (raw.isNotEmpty()) {
            pop.snapTo(1.05f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    val words = amountInWords(value, currencyUnit, isPersian)

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
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelLarge,
                color = if (noteIsError) scheme.error else accent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// مرحله ۴: مرور و ثبت
// -----------------------------------------------------------------------------

@Composable
private fun GoalReviewStep(
    form: GoalFormState,
    original: SavingGoalEntity?,
    accent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    isEdit: Boolean,
    canSave: Boolean,
    onEditTitle: () -> Unit,
    onEditIcon: () -> Unit,
    onEditTarget: () -> Unit,
    onEditMonthly: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val words = amountInWords(form.targetValue, currencyUnit, isPersian)
    val iconLabel = GOAL_ICON_OPTIONS.firstOrNull { sameEmoji(it.emoji, form.icon) }?.label(isPersian).orEmpty()
    val unitFactor = if (currencyUnit == "IRR") 10L else 1L

    // روز واریز ماهانه همان روزِ ساخت قلک است (مثل کارت قلک)؛ برای قلک جدید یعنی امروز.
    val depositDay = remember(original?.createdAt) {
        Calendar.getInstance().apply { if (original != null) timeInMillis = original.createdAt }
            .get(Calendar.DAY_OF_MONTH)
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
            Text(
                text = if (isEdit) {
                    tr(isPersian, "ویرایش قلک", "Edit goal")
                } else {
                    tr(isPersian, "همه‌چی درسته؟", "Look good?")
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            // کارت خلاصه؛ لمس آن به ویرایش مبلغ هدف می‌رود.
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(accent.copy(alpha = 0.10f))
                    .border(1.dp, accent.copy(alpha = 0.25f), cardShape)
                    .clickable(onClick = onEditTarget)
                    .padding(vertical = 22.dp, horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = form.icon, fontSize = 32.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = form.title.trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatSuccessAmount(form.targetValue, currencyUnit, isPersian),
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

            GoalReviewRow(
                label = tr(isPersian, "عنوان", "Title"),
                value = form.title.trim(),
                onClick = onEditTitle
            )
            GoalReviewRow(
                label = tr(isPersian, "آیکون", "Icon"),
                value = iconLabel,
                leading = form.icon,
                onClick = onEditIcon
            )
            GoalReviewRow(
                label = tr(isPersian, "مبلغ هدف", "Target"),
                value = formatSuccessAmount(form.targetValue, currencyUnit, isPersian),
                onClick = onEditTarget
            )
            GoalReviewRow(
                label = tr(isPersian, "واریز ماهانه", "Monthly"),
                value = if (form.monthlyValue > 0L) {
                    formatSuccessAmount(form.monthlyValue, currencyUnit, isPersian)
                } else {
                    tr(isPersian, "ندارد", "None")
                },
                dimmed = form.monthlyValue <= 0L,
                onClick = onEditMonthly
            )
            if (original != null) {
                GoalReviewRow(
                    label = tr(isPersian, "موجودی فعلی", "Saved so far"),
                    value = formatSuccessAmount(original.currentAmount * unitFactor, currencyUnit, isPersian),
                    dimmed = true,
                    onClick = null
                )
            }

            if (form.monthlyValue > 0L) {
                Spacer(Modifier.height(10.dp))
                val dayText = if (isPersian) formatNumber(depositDay.toLong(), true) else depositDay.toString()
                Text(
                    text = tr(
                        isPersian,
                        "روز واریز ماهانه: $dayText هر ماه (در ماه‌های کوتاه، روز آخر ماه)",
                        "Monthly deposit day: $dayText of each month (last day in shorter months)"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        SwipeToConfirmButton(
            text = if (isEdit) {
                tr(isPersian, "برای ذخیره بکشید", "Swipe to save")
            } else {
                tr(isPersian, "برای ساخت قلک بکشید", "Swipe to create")
            },
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
private fun GoalReviewRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    leading: String? = null,
    dimmed: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
                modifier = Modifier.width(96.dp)
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