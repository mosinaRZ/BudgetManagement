package ir.hamedan.budgetmanagement.ui.screens.debtCredit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.components.SuccessBottomSheet
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.SwipeToConfirmButton
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmount
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmountFromToman
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.screens.add.AmountHero
import ir.hamedan.budgetmanagement.ui.screens.add.MAX_AMOUNT_DIGITS
import ir.hamedan.budgetmanagement.ui.screens.add.NumericKeypad
import ir.hamedan.budgetmanagement.ui.screens.add.PrimaryActionButton
import ir.hamedan.budgetmanagement.ui.screens.add.ReviewRow
import ir.hamedan.budgetmanagement.ui.screens.add.StepTopBar
import ir.hamedan.budgetmanagement.ui.screens.add.addToAmount
import ir.hamedan.budgetmanagement.ui.screens.add.amountInWords
import ir.hamedan.budgetmanagement.ui.screens.add.appendDigits
import ir.hamedan.budgetmanagement.ui.screens.add.formatNumber
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.utils.DueDay
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val STEP_AMOUNT = 0
private const val STEP_PARTY = 1
private const val STEP_PLAN = 2
private const val STEP_REVIEW = 3
private const val TOTAL_STEPS = 4

/**
 * ثبت بدهی/طلب به‌صورت جریان تمام‌صفحه‌ی چهار‌مرحله‌ای (هم‌شکل با ثبت تراکنش):
 * ۱) مبلغ کل (با کیبورد اختصاصی) ← ۲) طرف حساب ← ۳) نحوه‌ی تسویه (یک‌جا/اقساطی) ← ۴) مرور و ثبت با کشیدن.
 *
 * @param onClose خروج بدون ثبت (برگشت به صفحه‌ی قبل)
 * @param onFinished بعد از ثبت موفق و بسته شدن شیت موفقیت
 */
@Composable
fun AddDebtCreditScreen(
    onClose: () -> Unit = {},
    onFinished: () -> Unit = {},
    viewModel: DebtCreditViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()
    val records by viewModel.debtCreditList.collectAsState()

    val form = rememberSaveable(saver = DebtCreditFormState.Saver) {
        DebtCreditFormState(
            isDebt = true,
            dueDay = DueDay.dayOfMonth(LocalDate.now(), isPersian)
        )
    }
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message -> snackbarHostState.showError(message) }
    }

    val accent by animateColorAsState(
        targetValue = if (form.isDebt) scheme.error else scheme.primary,
        animationSpec = tween(300),
        label = "accent"
    )
    val onAccent by animateColorAsState(
        targetValue = if (form.isDebt) scheme.onError else scheme.onPrimary,
        animationSpec = tween(300),
        label = "onAccent"
    )

    // نام‌های قبلی به ترتیب تکرار؛ برای پر کردن سریع نام طرف حساب.
    val knownNames = remember(records) {
        records.map { it.personName.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(12)
    }
    val similarRecords = remember(form.name, records) { viewModel.findNameMatches(form.name) }

    val planValid = form.planError(isPersian) == null
    val canSave = !isSaving && form.hasAmount && form.name.isNotBlank() && planValid

    fun goTo(step: Int) {
        focusManager.clearFocus()
        form.step = step.coerceIn(STEP_AMOUNT, STEP_REVIEW)
    }

    fun setType(debt: Boolean) {
        if (form.isDebt == debt) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        form.isDebt = debt
    }

    /** یک جمله‌ی گفتاری را به مبلغ، نام و نوع تبدیل می‌کند و فرم را تا جای ممکن جلو می‌برد. */
    fun applyVoice(spoken: String) {
        val parsed = SpokenDebtParser.parse(spoken)
        parsed.isDebt?.let { form.isDebt = it }
        val display = SpokenDebtParser.displayAmount(parsed, currencyUnit == MoneyContract.DISPLAY_RIAL)
        if (display != null && display > 0L && display.toString().length <= MAX_AMOUNT_DIGITS) {
            form.amount = display.toString()
        }
        if (parsed.personName.isNotBlank()) form.name = parsed.personName
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        when {
            form.hasAmount && form.name.isNotBlank() -> goTo(STEP_REVIEW)
            form.hasAmount -> goTo(STEP_PARTY)
            else -> scope.launch {
                snackbarHostState.showSnackbar(
                    tr(isPersian, "مبلغ رو متوجه نشدم؛ با کیبورد وارد کن.", "I couldn't catch the amount; type it in.")
                )
            }
        }
    }

    fun save() {
        if (!canSave) return
        val total = form.amount.toDoubleOrNull() ?: return
        val monthly = form.monthlyAmount.toDoubleOrNull() ?: 0.0
        val totalToman = displayToToman(total, currencyUnit)
        val monthlyToman = displayToToman(monthly, currencyUnit)
        val savedName = form.name.trim()
        val savedType = if (form.isDebt) "DEBT" else "CREDIT"
        isSaving = true
        viewModel.saveOrUpdate(
            id = null,
            type = savedType,
            personName = savedName,
            totalAmount = totalToman,
            isMonthly = form.isMonthly,
            monthlyAmount = if (form.isMonthly) monthlyToman else 0.0,
            dueDay = DueDay.encode(form.dueDay.coerceIn(1, 31), isPersian),
            oneTimeDueDateMillis = form.dueMillis,
            note = form.note.trim().ifBlank { null },
            addToBalance = form.addToBalance,
            // نام مشابه در مرحله‌ی مرور نشان داده شده؛ ادامه دادن یعنی «این مورد جدید است».
            forceDuplicateName = similarRecords.isNotEmpty(),
            onResult = { saved ->
                if (saved) {
                    successSheet = debtCreditAddedContent(
                        type = savedType,
                        personName = savedName,
                        totalAmountToman = MoneyContract.fromInput(totalToman),
                        currencyUnit = currencyUnit,
                        isPersian = isPersian
                    )
                } else {
                    // ثبت ناموفق: فرم و ورودی کاربر سر جایش می‌ماند و اسنک‌بار خطا نشان داده شده است.
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

        // هاله‌ی رنگیِ بالای صفحه که با نوع (بدهی/طلب) عوض می‌شود.
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
            StepTopBar(
                step = form.step,
                accent = accent,
                isPersian = isPersian,
                totalSteps = TOTAL_STEPS,
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
                        onContinue = { goTo(STEP_PARTY) }
                    )

                    STEP_PARTY -> PartyStep(
                        form = form,
                        knownNames = knownNames,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        onEditAmount = { goTo(STEP_AMOUNT) },
                        onContinue = { goTo(STEP_PLAN) }
                    )

                    STEP_PLAN -> PlanStep(
                        form = form,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        onEditAmount = { goTo(STEP_AMOUNT) },
                        onContinue = { goTo(STEP_REVIEW) }
                    )

                    else -> ReviewStep(
                        form = form,
                        similarRecords = similarRecords,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        canSave = canSave,
                        onEditAmount = { goTo(STEP_AMOUNT) },
                        onEditParty = { goTo(STEP_PARTY) },
                        onEditPlan = { goTo(STEP_PLAN) },
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
// مرحله ۱: مبلغ کل
// -----------------------------------------------------------------------------

@Composable
private fun AmountStep(
    form: DebtCreditFormState,
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
    val unitFactor = if (currencyUnit == MoneyContract.DISPLAY_RIAL) 10L else 1L
    val quickDeltas = remember(unitFactor) { listOf(100_000L, 500_000L, 1_000_000L).map { it * unitFactor } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        DebtTypeToggle(isDebt = form.isDebt, isPersian = isPersian, onSetType = onSetType)

        Spacer(Modifier.height(10.dp))

        DebtVoicePill(isPersian = isPersian, accent = accent, onResult = onVoiceResult)

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (form.isDebt) {
                tr(isPersian, "چقدر بدهکاری؟", "How much do you owe?")
            } else {
                tr(isPersian, "چقدر طلبکاری؟", "How much are you owed?")
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
            AmountHero(amount = form.amount, accent = accent, currencyUnit = currencyUnit, isPersian = isPersian)
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

// -----------------------------------------------------------------------------
// مرحله ۲: طرف حساب
// -----------------------------------------------------------------------------

@Composable
private fun PartyStep(
    form: DebtCreditFormState,
    knownNames: List<String>,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    onEditAmount: () -> Unit,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val nameFocus = remember { FocusRequester() }
    // خطا فقط بعد از زدن «ادامه» با نام خالی نشان داده می‌شود، نه از همان اول.
    var showNameError by remember { mutableStateOf(false) }

    val typed = form.name.trim()
    val suggestions = remember(knownNames, typed) {
        knownNames.filter { !it.equals(typed, ignoreCase = true) && (typed.isEmpty() || it.contains(typed, ignoreCase = true)) }
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
            AmountReminder(
                isDebt = form.isDebt,
                amount = form.amountValue,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                accent = accent,
                onClick = onEditAmount
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = if (form.isDebt) {
                    tr(isPersian, "به چه کسی بدهکاری؟", "Who do you owe?")
                } else {
                    tr(isPersian, "از چه کسی طلب داری؟", "Who owes you?")
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            DebtNameField(
                value = form.name,
                onValueChange = { input ->
                    form.name = input
                    if (input.isNotBlank()) showNameError = false
                },
                accent = accent,
                isPersian = isPersian,
                isError = showNameError,
                modifier = Modifier.focusRequester(nameFocus)
            )

            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr(isPersian, "نام‌های قبلی", "Previous names"),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                NameSuggestionRow(
                    names = suggestions,
                    accent = accent,
                    onPick = { picked ->
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        form.name = picked
                        showNameError = false
                    }
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
                if (form.name.isBlank()) {
                    showNameError = true
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    nameFocus.requestFocus()
                } else {
                    onContinue()
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// مرحله ۳: نحوه‌ی تسویه
// -----------------------------------------------------------------------------

@Composable
private fun PlanStep(
    form: DebtCreditFormState,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    onEditAmount: () -> Unit,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    // خطای قسط فقط بعد از زدن «ادامه» با قسط نامعتبر نشان داده می‌شود.
    var showPlanError by remember { mutableStateOf(false) }
    val planError = form.planError(isPersian)

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
            AmountReminder(
                isDebt = form.isDebt,
                amount = form.amountValue,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                accent = accent,
                onClick = onEditAmount
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = tr(isPersian, "چطور تسویه می‌شه؟", "How will it be settled?"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            PlanSection(
                form = form,
                accent = accent,
                onAccent = onAccent,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                errorText = if (showPlanError) planError else null
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        PrimaryActionButton(
            text = tr(isPersian, "ادامه", "Continue"),
            enabled = true,
            accent = accent,
            onAccent = onAccent,
            onClick = {
                if (planError != null) {
                    showPlanError = true
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
// مرحله ۴: مرور و ثبت
// -----------------------------------------------------------------------------

@Composable
private fun ReviewStep(
    form: DebtCreditFormState,
    similarRecords: List<DebtCreditEntity>,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    canSave: Boolean,
    onEditAmount: () -> Unit,
    onEditParty: () -> Unit,
    onEditPlan: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var noteOpen by rememberSaveable { mutableStateOf(form.note.isNotBlank()) }
    val words = amountInWords(form.amountValue, currencyUnit, isPersian)

    val planValue = if (form.isMonthly) {
        tr(
            isPersian,
            "اقساطی · ماهی ${formatSuccessAmount(form.monthlyValue, currencyUnit, true)}",
            "Installments · ${formatSuccessAmount(form.monthlyValue, currencyUnit, false)}/month"
        )
    } else {
        tr(isPersian, "یک‌جا", "One-time")
    }
    val encodedDay = DueDay.encode(form.dueDay.coerceIn(1, 31), isPersian)
    val lastInstallment = if (form.isMonthly) {
        lastInstallmentMillis(form.amountValue, form.monthlyValue, encodedDay)
    } else {
        null
    }
    val scheduleLabel: String
    val scheduleValue: String
    if (form.isMonthly) {
        scheduleLabel = tr(isPersian, "قسط‌ها", "Schedule")
        val count = installmentCount(form.amountValue, form.monthlyValue)
        val dayText = localDigits(form.dueDay.toLong(), isPersian)
        scheduleValue = tr(
            isPersian,
            "هر ماه روز $dayText" + (lastInstallment?.let { " · تا ${formatDueDate(it, true)}" } ?: "") +
                    " (${localDigits(count.toLong(), true)} قسط)",
            "Day $dayText each month" + (lastInstallment?.let { " · until ${formatDueDate(it, false)}" } ?: "") +
                    " ($count)"
        )
    } else {
        scheduleLabel = tr(isPersian, "مهلت تسویه", "Due date")
        scheduleValue = if (form.dueMillis > 0L) {
            formatDueDate(form.dueMillis, isPersian)
        } else {
            tr(isPersian, "بدون مهلت", "No due date")
        }
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
                    text = if (form.isDebt) tr(isPersian, "بدهی", "Debt") else tr(isPersian, "طلب", "Credit"),
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
                label = tr(isPersian, "طرف حساب", "Person"),
                value = form.name.trim(),
                leading = "👤",
                onClick = onEditParty
            )
            ReviewRow(
                label = tr(isPersian, "تسویه", "Settlement"),
                value = planValue,
                onClick = onEditPlan
            )
            ReviewRow(
                label = scheduleLabel,
                value = scheduleValue,
                onClick = onEditPlan,
                dimmed = !form.isMonthly && form.dueMillis <= 0L
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
                DebtNoteField(
                    value = form.note,
                    onValueChange = { form.note = it },
                    accent = accent,
                    isPersian = isPersian
                )
            }

            Spacer(Modifier.height(12.dp))

            BalanceToggleCard(
                isDebt = form.isDebt,
                checked = form.addToBalance,
                accent = accent,
                onAccent = onAccent,
                isPersian = isPersian,
                onCheckedChange = { form.addToBalance = it }
            )

            if (similarRecords.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SimilarRecordsCard(
                    name = form.name.trim(),
                    matches = similarRecords,
                    currencyUnit = currencyUnit,
                    isPersian = isPersian
                )
            }

            Spacer(Modifier.height(12.dp))
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

/** جایگزین دیالوگ «محاسبه در بالانس حساب»: یک کلید ساده در مرحله‌ی مرور. */
@Composable
private fun BalanceToggleCard(
    isDebt: Boolean,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, scheme.outline.copy(alpha = 0.15f), shape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr(isPersian, "در موجودی حساب هم ثبت شود", "Also record it in my balance"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    !checked -> tr(
                        isPersian,
                        "فقط به‌عنوان بدهی/طلب ثبت می‌شود و موجودی تغییر نمی‌کند",
                        "Only recorded as a debt/credit; your balance won't change"
                    )
                    isDebt -> tr(
                        isPersian,
                        "مبلغ به موجودی حساب اضافه می‌شود",
                        "The amount will be added to your balance"
                    )
                    else -> tr(
                        isPersian,
                        "مبلغ از موجودی حساب کسر می‌شود",
                        "The amount will be deducted from your balance"
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.padding(horizontal = 6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = onAccent,
                checkedTrackColor = accent
            )
        )
    }
}

/** جایگزین دیالوگ «مورد مشابه پیدا شد»: هشدار داخل صفحه، بدون وقفه در جریان ثبت. */
@Composable
private fun SimilarRecordsCard(
    name: String,
    matches: List<DebtCreditEntity>,
    currencyUnit: String,
    isPersian: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val warn = scheme.tertiary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(warn.copy(alpha = 0.10f))
            .border(1.dp, warn.copy(alpha = 0.30f), shape)
            .padding(14.dp)
    ) {
        Text(
            text = tr(isPersian, "مورد مشابه پیدا شد", "A similar record exists"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = warn
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tr(
                isPersian,
                "قبلاً یک یا چند بدهی/طلب با نام «$name» ثبت شده. با ثبت، این مورد به‌عنوان یک مورد جدید اضافه می‌شود.",
                "One or more records named “$name” already exist. Saving adds this as a new record."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        matches.take(3).forEach { existing ->
            val remaining = (existing.totalAmount - existing.paidAmount).coerceAtLeast(0L)
            val kind = if (existing.type == "DEBT") tr(isPersian, "بدهی", "Debt") else tr(isPersian, "طلب", "Credit")
            Spacer(Modifier.height(6.dp))
            Text(
                text = tr(
                    isPersian,
                    "$kind · کل ${formatSuccessAmountFromToman(existing.totalAmount, currencyUnit, true)} · مانده ${formatSuccessAmountFromToman(remaining, currencyUnit, true)}",
                    "$kind · total ${formatSuccessAmountFromToman(existing.totalAmount, currencyUnit, false)} · remaining ${formatSuccessAmountFromToman(remaining, currencyUnit, false)}"
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface
            )
        }
    }
}