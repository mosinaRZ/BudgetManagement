package ir.hamedan.budgetmanagement.ui.screens.debtCredit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmountFromToman
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.screens.add.AmountHero
import ir.hamedan.budgetmanagement.ui.screens.add.MAX_AMOUNT_DIGITS
import ir.hamedan.budgetmanagement.ui.screens.add.NumericKeypad
import ir.hamedan.budgetmanagement.ui.screens.add.PrimaryActionButton
import ir.hamedan.budgetmanagement.ui.screens.add.appendDigits
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.utils.DueDay
import kotlinx.coroutines.launch

/**
 * ویرایش بدهی/طلب به‌صورت صفحه‌ی تمام‌صفحه (جایگزین دیالوگ قدیمی)؛ هم‌زبان با ویرایش تراکنش:
 *
 * - مبلغ بزرگ و لمس‌پذیر است؛ با لمسش کیبورد اختصاصی باز می‌شود.
 * - نام، نحوه‌ی تسویه (یک‌جا/اقساطی) و یادداشت در یک پنل قابل‌اسکرول هستند.
 * - دکمه‌ی سوایپ تا وقتی تغییری ایجاد نشده غیرفعال است.
 * - اگر پرداخت/دریافتی ثبت شده باشد، نوع قفل است و مبلغ کل نمی‌تواند کمتر از آن شود.
 */
@Composable
fun EditDebtCreditScreen(
    recordId: String,
    onClose: () -> Unit = {},
    viewModel: DebtCreditViewModel = appViewModel()
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    var original by remember { mutableStateOf<DebtCreditEntity?>(null) }

    LaunchedEffect(recordId) {
        val found = viewModel.findDebtCredit(recordId)
        // مورد (مثلاً بین دو صفحه) حذف شده؛ بی‌صدا برمی‌گردیم.
        if (found == null) onClose() else original = found
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()
        val item = original
        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditorContent(item = item, isPersian = isPersian, viewModel = viewModel, onClose = onClose)
        }
    }
}

@Composable
private fun EditorContent(
    item: DebtCreditEntity,
    isPersian: Boolean,
    viewModel: DebtCreditViewModel,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val nameFocus = remember { FocusRequester() }

    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message -> snackbarHostState.showError(message) }
    }

    // مقادیر اولیه‌ی فرم از خودِ مورد؛ مبلغ‌ها به واحد نمایشی کاربر (تومان/ریال).
    val initialDay = remember(item.id, isPersian) { initialDueDayFor(item, isPersian) }
    val initialNote = item.note.orEmpty()

    val form = rememberSaveable(item.id, saver = DebtCreditFormState.Saver) {
        DebtCreditFormState(
            isDebt = item.type == "DEBT",
            amount = storedToDigits(item.totalAmount, currencyUnit),
            name = item.personName,
            isMonthly = item.isMonthly,
            monthlyAmount = if (item.isMonthly && item.monthlyAmount > 0L) {
                storedToDigits(item.monthlyAmount, currencyUnit)
            } else {
                ""
            },
            dueDay = initialDay,
            dueMillis = if (item.dueDateMillis > 0L) {
                item.dueDateMillis
            } else if (item.isMonthly) {
                defaultDueMillis()
            } else {
                0L
            },
            note = initialNote
        )
    }
    var editingAmount by rememberSaveable { mutableStateOf(false) }
    var noteOpen by rememberSaveable { mutableStateOf(initialNote.isNotBlank()) }
    var showNameError by remember { mutableStateOf(false) }
    var showPlanError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }
    var successSheet by remember { mutableStateOf<SuccessSheetContent?>(null) }

    // با ثبت پرداخت/دریافت، جابه‌جایی بین بدهی و طلب تراکنش‌های قبلی را ناسازگار می‌کند.
    val typeLocked = item.paidAmount > 0L

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

    // مقادیر نهایی و تشخیص تغییر
    val parsedTotal = form.amount.toDoubleOrNull() ?: 0.0
    val newTotalStored = MoneyContract.inputToStorage(parsedTotal, currencyUnit)
    val parsedMonthly = form.monthlyAmount.toDoubleOrNull() ?: 0.0
    val newMonthlyStored = if (form.isMonthly) MoneyContract.inputToStorage(parsedMonthly, currencyUnit) else 0L
    val planError = form.planError(isPersian)

    val hasChanges = form.isDebt != (item.type == "DEBT") ||
            form.name.trim() != item.personName.trim() ||
            newTotalStored != item.totalAmount ||
            form.isMonthly != item.isMonthly ||
            (form.isMonthly && (newMonthlyStored != item.monthlyAmount || form.dueDay != initialDay)) ||
            (!form.isMonthly && form.dueMillis != item.dueDateMillis) ||
            form.note.trim() != initialNote.trim()

    fun setType(debt: Boolean) {
        if (form.isDebt == debt) return
        if (typeLocked) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            scope.launch {
                snackbarHostState.showSnackbar(
                    tr(
                        isPersian,
                        "به‌خاطر پرداخت/دریافت‌های ثبت‌شده، نوع قابل تغییر نیست",
                        "The type can't be changed because payments are already recorded"
                    )
                )
            }
            return
        }
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        form.isDebt = debt
    }

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun save() {
        val nameOk = form.name.isNotBlank()
        val amountOk = parsedTotal > 0.0
        val aboveOrEqualPaid = newTotalStored >= item.paidAmount
        if (!nameOk || !amountOk || !aboveOrEqualPaid || planError != null) {
            // دستگیره‌ی سوایپ به ابتدا برمی‌گردد و مشکل نشان داده می‌شود.
            resetKey++
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            when {
                !amountOk -> {
                    editingAmount = true
                    toast(tr(isPersian, "مبلغ رو وارد کن", "Enter an amount"))
                }
                !aboveOrEqualPaid -> {
                    editingAmount = true
                    val paid = formatSuccessAmountFromToman(item.paidAmount, currencyUnit, isPersian)
                    toast(
                        tr(
                            isPersian,
                            "مبلغ کل نمی‌تواند کمتر از مبلغ ${if (item.type == "DEBT") "پرداخت‌شده" else "دریافت‌شده"} ($paid) باشد",
                            "The total can't be less than the amount already ${if (item.type == "DEBT") "paid" else "received"} ($paid)"
                        )
                    )
                }
                !nameOk -> {
                    showNameError = true
                    nameFocus.requestFocus()
                }
                else -> {
                    showPlanError = true
                    toast(planError.orEmpty())
                }
            }
            return
        }
        val savedName = form.name.trim()
        // روز سررسید دست‌نخورده، با همان کدگذاری و تقویمِ ذخیره‌شده می‌ماند تا قسط جابه‌جا نشود.
        val dueDayToSave = when {
            !form.isMonthly && DueDay.isValid(item.dueDay) -> item.dueDay
            item.isMonthly && form.dueDay == initialDay && DueDay.isValid(item.dueDay) -> item.dueDay
            else -> DueDay.encode(form.dueDay.coerceIn(1, 31), isPersian)
        }
        isSaving = true
        viewModel.saveOrUpdate(
            id = item.id,
            type = if (form.isDebt) "DEBT" else "CREDIT",
            personName = savedName,
            totalAmount = displayToToman(parsedTotal, currencyUnit),
            isMonthly = form.isMonthly,
            monthlyAmount = if (form.isMonthly) displayToToman(parsedMonthly, currencyUnit) else 0.0,
            dueDay = dueDayToSave,
            oneTimeDueDateMillis = form.dueMillis,
            note = form.note.trim().ifBlank { null },
            // ویرایش هیچ‌وقت موجودی حساب را دست نمی‌زند (مثل رفتار قبلی).
            addToBalance = false,
            onResult = { saved ->
                if (saved) {
                    successSheet = debtCreditUpdatedContent(savedName, isPersian)
                } else {
                    isSaving = false
                    resetKey++
                }
            }
        )
    }

    BackHandler(enabled = editingAmount && successSheet == null) { editingAmount = false }

    Box(modifier = Modifier.fillMaxSize()) {
        // هاله‌ی رنگیِ بالا که با نوع (بدهی/طلب) عوض می‌شود.
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
            EditTopBar(
                title = tr(isPersian, "ویرایش بدهی / طلب", "Edit debt / credit"),
                closeLabel = tr(isPersian, "بستن", "Close"),
                onClose = { if (editingAmount) editingAmount = false else onClose() }
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                DebtTypeToggle(isDebt = form.isDebt, isPersian = isPersian, onSetType = ::setType)
            }

            // مبلغ: لمس‌پذیر؛ با لمس، کیبورد اختصاصی جای فیلدها را می‌گیرد.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !editingAmount) {
                        focusManager.clearFocus()
                        editingAmount = true
                    }
                    .padding(vertical = 8.dp)
            ) {
                AmountHero(amount = form.amount, accent = accent, currencyUnit = currencyUnit, isPersian = isPersian)
                if (!editingAmount) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(isPersian, "برای تغییر مبلغ لمس کن", "Tap to change the amount"),
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
                label = "editorPanel"
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
                            enabled = form.amount.toLongOrNull()?.let { it > 0L } == true &&
                                    form.amount.length <= MAX_AMOUNT_DIGITS,
                            accent = accent,
                            onAccent = onAccent,
                            onClick = { editingAmount = false }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    DetailsPanel(
                        form = form,
                        item = item,
                        typeLocked = typeLocked,
                        accent = accent,
                        onAccent = onAccent,
                        currencyUnit = currencyUnit,
                        isPersian = isPersian,
                        noteOpen = noteOpen,
                        onNoteOpen = { noteOpen = true },
                        showNameError = showNameError,
                        onNameChanged = { input ->
                            form.name = input
                            if (input.isNotBlank()) showNameError = false
                        },
                        nameFocus = nameFocus,
                        planErrorText = if (showPlanError) planError else null
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
private fun EditTopBar(title: String, closeLabel: String, onClose: () -> Unit) {
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
private fun DetailsPanel(
    form: DebtCreditFormState,
    item: DebtCreditEntity,
    typeLocked: Boolean,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    noteOpen: Boolean,
    onNoteOpen: () -> Unit,
    showNameError: Boolean,
    onNameChanged: (String) -> Unit,
    nameFocus: FocusRequester,
    planErrorText: String?,
    action: @Composable () -> Unit
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
            Spacer(Modifier.height(8.dp))

            if (typeLocked) {
                // مثل قفل دسته در تراکنش‌های قلک: دلیل محدودیت همان‌جا گفته می‌شود.
                val shape = RoundedCornerShape(16.dp)
                val paid = formatSuccessAmountFromToman(item.paidAmount, currencyUnit, isPersian)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.15f), shape)
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (item.type == "DEBT") {
                            tr(isPersian, "🔒 تاکنون $paid پرداخت شده", "🔒 $paid has been paid so far")
                        } else {
                            tr(isPersian, "🔒 تاکنون $paid دریافت شده", "🔒 $paid has been received so far")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tr(
                            isPersian,
                            "نوع قابل تغییر نیست و مبلغ کل نمی‌تواند از این مقدار کمتر شود.",
                            "The type can't be changed and the total can't go below this amount."
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            DebtNameField(
                value = form.name,
                onValueChange = onNameChanged,
                accent = accent,
                isPersian = isPersian,
                isError = showNameError,
                modifier = Modifier.focusRequester(nameFocus)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = tr(isPersian, "نحوه‌ی تسویه", "Settlement"),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            PlanSection(
                form = form,
                accent = accent,
                onAccent = onAccent,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                errorText = planErrorText
            )

            Spacer(Modifier.height(12.dp))

            if (!noteOpen) {
                Text(
                    text = tr(isPersian, "＋ افزودن یادداشت", "＋ Add a note"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNoteOpen)
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                )
            } else {
                DebtNoteField(
                    value = form.note,
                    onValueChange = { form.note = it },
                    accent = accent,
                    isPersian = isPersian
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        action()
        Spacer(Modifier.height(16.dp))
    }
}