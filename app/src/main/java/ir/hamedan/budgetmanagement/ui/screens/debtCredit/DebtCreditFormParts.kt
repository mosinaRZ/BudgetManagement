package ir.hamedan.budgetmanagement.ui.screens.debtCredit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.ui.components.SuccessSheetContent
import ir.hamedan.budgetmanagement.ui.components.VoiceInputButton
import ir.hamedan.budgetmanagement.ui.components.WheelDatePickerDialog
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmount
import ir.hamedan.budgetmanagement.ui.components.formatSuccessAmountFromToman
import ir.hamedan.budgetmanagement.ui.components.rememberVoiceInputLauncher
import ir.hamedan.budgetmanagement.ui.screens.add.DateChip
import ir.hamedan.budgetmanagement.ui.screens.add.MAX_AMOUNT_DIGITS
import ir.hamedan.budgetmanagement.ui.screens.add.ThousandsSeparatorTransformation
import ir.hamedan.budgetmanagement.ui.screens.add.TwoOptionToggle
import ir.hamedan.budgetmanagement.ui.screens.add.amountInWords
import ir.hamedan.budgetmanagement.ui.screens.add.tr
import ir.hamedan.budgetmanagement.ui.screens.add.unitLabel
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.DueDay
import ir.hamedan.budgetmanagement.utils.SpokenTransactionParser
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal const val MAX_NAME_LENGTH = 40
internal const val MAX_NOTE_LENGTH = 120

/** بیش از این تعداد قسط (۱۰۰ سال) دیگر تاریخ آخرین قسط محاسبه و نمایش داده نمی‌شود. */
private const val MAX_PREVIEW_INSTALLMENTS = 1200

// -----------------------------------------------------------------------------
// وضعیت فرم (مشترک بین ثبت و ویرایش)
// -----------------------------------------------------------------------------

/**
 * وضعیت فرم بدهی/طلب؛ با [Saver] در چرخش صفحه و بازگشت از صفحه‌های دیگر حفظ می‌شود.
 *
 * مبلغ‌ها فقط به‌صورت رقم‌های بدون جداکننده و **به واحد نمایشی کاربر** (تومان یا ریال) نگه‌داری می‌شوند؛
 * تبدیل به تومان هنگام ذخیره انجام می‌شود (مثل فرم ثبت تراکنش).
 */
@Stable
class DebtCreditFormState(
    isDebt: Boolean,
    amount: String = "",
    name: String = "",
    isMonthly: Boolean = false,
    monthlyAmount: String = "",
    dueDay: Int = 1,
    dueMillis: Long = defaultDueMillis(),
    note: String = "",
    addToBalance: Boolean = true,
    step: Int = 0
) {
    var isDebt by mutableStateOf(isDebt)
    var amount by mutableStateOf(amount)
    var name by mutableStateOf(name)
    var isMonthly by mutableStateOf(isMonthly)
    var monthlyAmount by mutableStateOf(monthlyAmount)

    /** روز سررسید قسط در ماه (۱..۳۱) به تقویم زبان برنامه؛ هنگام ذخیره با [DueDay.encode] کدگذاری می‌شود. */
    var dueDay by mutableStateOf(dueDay)

    /** مهلت تسویه‌ی یک‌جا؛ ۰ یعنی بدون مهلت. */
    var dueMillis by mutableStateOf(dueMillis)
    var note by mutableStateOf(note)

    /** فقط در ثبت‌ی جدید: ثبت مبلغ در موجودی حساب هم انجام شود؟ */
    var addToBalance by mutableStateOf(addToBalance)
    var step by mutableStateOf(step)

    val amountValue: Long get() = amount.toLongOrNull() ?: 0L
    val monthlyValue: Long get() = monthlyAmount.toLongOrNull() ?: 0L
    val hasAmount: Boolean get() = amountValue > 0L

    /** متن خطای بخش «نحوه‌ی تسویه»؛ null یعنی معتبر است. */
    fun planError(isPersian: Boolean): String? = when {
        !isMonthly -> null
        monthlyValue <= 0L -> tr(isPersian, "مبلغ هر قسط رو وارد کن", "Enter the installment amount")
        monthlyValue > amountValue -> tr(
            isPersian,
            "قسط نمی‌تواند از مبلغ کل بیشتر باشد",
            "The installment can't be more than the total"
        )
        else -> null
    }

    companion object {
        val Saver = listSaver<DebtCreditFormState, Any>(
            save = {
                listOf(
                    it.isDebt, it.amount, it.name, it.isMonthly, it.monthlyAmount,
                    it.dueDay, it.dueMillis, it.note, it.addToBalance, it.step
                )
            },
            restore = {
                DebtCreditFormState(
                    isDebt = it[0] as Boolean,
                    amount = it[1] as String,
                    name = it[2] as String,
                    isMonthly = it[3] as Boolean,
                    monthlyAmount = it[4] as String,
                    dueDay = it[5] as Int,
                    dueMillis = it[6] as Long,
                    note = it[7] as String,
                    addToBalance = it[8] as Boolean,
                    step = it[9] as Int
                )
            }
        )
    }
}

// -----------------------------------------------------------------------------
// ابزارهای کوچک
// -----------------------------------------------------------------------------

private fun millisToDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** مهلت پیش‌فرض یک بدهی/طلب تازه: یک ماه بعد. */
internal fun defaultDueMillis(): Long = dateToMillis(LocalDate.now().plusMonths(1))

internal fun localDigits(value: Long, isPersian: Boolean): String =
    if (isPersian) TransactionShareFormatter.toPersianDigits(value.toString()) else value.toString()

internal fun formatDueDate(millis: Long, isPersian: Boolean): String =
    DateUtils.formatTimestamp(millis, isPersian).let {
        if (isPersian) TransactionShareFormatter.toPersianDigits(it) else it
    }

/** مبلغِ نمایشیِ کاربر (تومان/ریال) را به تومان برای ViewModel برمی‌گرداند. */
internal fun displayToToman(display: Double, currencyUnit: String): Double =
    if (currencyUnit == MoneyContract.DISPLAY_RIAL) display / 10.0 else display

/** مبلغ ذخیره‌شده (تومان) را به رشته‌ی ارقامِ واحد نمایشی کاربر برای فرم تبدیل می‌کند. */
internal fun storedToDigits(stored: Long, currencyUnit: String): String =
    MoneyContract.displayFromStorage(stored, currencyUnit).toString().take(MAX_AMOUNT_DIGITS)

internal fun installmentCount(total: Long, monthly: Long): Int =
    if (total <= 0L || monthly <= 0L) 0 else ((total + monthly - 1) / monthly).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()

/** تاریخ آخرین قسط (همان محاسبه‌ی DebtCreditUseCase)؛ اگر قابل محاسبه نباشد null. */
internal fun lastInstallmentMillis(total: Long, monthly: Long, dueDayEncoded: Int): Long? {
    val count = installmentCount(total, monthly)
    if (count <= 0 || count > MAX_PREVIEW_INSTALLMENTS || !DueDay.isValid(dueDayEncoded)) return null
    return runCatching {
        val first = DueDay.firstDueDate(dueDayEncoded, LocalDate.now())
        dateToMillis(DueDay.dateInMonth(dueDayEncoded, first, count - 1))
    }.getOrNull()
}

/** روز پیش‌فرضِ انتخابگر برای یک مورد موجود (همان منطق دیالوگ قدیمی: تبدیل تقویم در صورت نیاز). */
internal fun initialDueDayFor(item: DebtCreditEntity?, isPersian: Boolean): Int {
    val today = LocalDate.now()
    val stored = item?.dueDay ?: 0
    return when {
        item == null || !item.isMonthly || !DueDay.isValid(stored) -> DueDay.dayOfMonth(today, isPersian)
        DueDay.isJalali(stored) == isPersian -> DueDay.dayOf(stored)
        else -> DueDay.dayOfMonth(DueDay.nextDueDate(stored, today), isPersian)
    }
}

// -----------------------------------------------------------------------------
// تحلیل جمله‌ی گفتاری: «۵۰۰ هزار تومان بدهی به علی»
// -----------------------------------------------------------------------------

/**
 * مبلغ و نام طرف حساب را از [SpokenTransactionParser] می‌گیرد و نوع (بدهی/طلب) را از کلیدواژه‌ها
 * تشخیص می‌دهد؛ اگر نوعی گفته نشده باشد null می‌ماند تا انتخاب فعلی حفظ شود.
 */
internal object SpokenDebtParser {

    class Result(
        val base: SpokenTransactionParser.Result,
        val personName: String,
        val isDebt: Boolean?
    )

    private val debtWords = setOf("بدهی", "بدهکارم", "بدهیم", "debt", "owe", "borrowed")
    private val creditWords = setOf("طلب", "طلبکارم", "طلبکار", "بستانکار", "بستانکارم", "بدهکاره", "owes", "lent", "credit")
    private val loanNouns = setOf("قرض", "وام")
    private val gaveWords = setOf("دادم", "lent")
    private val tookWords = setOf("گرفتم", "borrowed")
    private val nameFillers = setOf(
        "از", "من", "به", "برای", "قرض", "وام", "گرفتم", "دادم",
        "me", "i", "to", "from", "for", "owe", "owes", "lent", "borrowed", "debt", "credit"
    ) + debtWords + creditWords

    fun parse(spoken: String): Result {
        val base = SpokenTransactionParser.parse(spoken)
        val words = spoken.lowercase().split(Regex("[\\s،,.!؟?]+")).filter { it.isNotBlank() }
        val isDebt = when {
            words.any { it in creditWords } -> false
            words.any { it in debtWords } -> true
            words.any { it in loanNouns } && words.any { it in gaveWords } -> false
            words.any { it in loanNouns } && words.any { it in tookWords } -> true
            else -> null
        }
        val name = base.title.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.lowercase() !in nameFillers }
            .joinToString(" ")
            .take(MAX_NAME_LENGTH)
            .trim()
        return Result(base = base, personName = name, isDebt = isDebt)
    }

    fun displayAmount(result: Result, displayIsRial: Boolean): Long? =
        SpokenTransactionParser.displayAmount(result.base, displayIsRial)
}

// -----------------------------------------------------------------------------
// متن شیت‌های موفقیت
// -----------------------------------------------------------------------------

internal fun debtCreditAddedContent(
    type: String,
    personName: String,
    totalAmountToman: Long,
    currencyUnit: String,
    isPersian: Boolean
): SuccessSheetContent {
    val amount = formatSuccessAmountFromToman(totalAmountToman, currencyUnit, isPersian)
    val isDebt = type == "DEBT"
    return if (isPersian) {
        SuccessSheetContent(
            title = if (isDebt) "بدهی ثبت شد" else "طلب ثبت شد",
            message = if (isDebt) "بدهی شما به «$personName» به مبلغ $amount با موفقیت ثبت شد."
            else "طلب شما از «$personName» به مبلغ $amount با موفقیت ثبت شد.",
            hint = if (isDebt) "هر زمان پرداختی انجام دادید، از همین صفحه ثبتش کنید."
            else "هر زمان مبلغی دریافت کردید، از همین صفحه ثبتش کنید."
        )
    } else {
        SuccessSheetContent(
            title = if (isDebt) "Debt recorded" else "Receivable recorded",
            message = if (isDebt) "Your debt to “$personName” of $amount was recorded successfully."
            else "Your receivable from “$personName” of $amount was recorded successfully.",
            hint = if (isDebt) "Record each payment from this page whenever you make one."
            else "Record each amount you receive from this page."
        )
    }
}

internal fun debtCreditUpdatedContent(personName: String, isPersian: Boolean): SuccessSheetContent =
    if (isPersian) {
        SuccessSheetContent(
            title = "تغییرات ذخیره شد",
            message = "اطلاعات «$personName» با موفقیت به‌روزرسانی شد."
        )
    } else {
        SuccessSheetContent(
            title = "Changes saved",
            message = "The details for “$personName” were updated successfully."
        )
    }

// -----------------------------------------------------------------------------
// کلید نوع، یادآور مبلغ، میکروفون
// -----------------------------------------------------------------------------

/** بدهی با رنگ خطا (مثل کارت‌های فهرست) و طلب با رنگ اصلی. */
@Composable
internal fun DebtTypeToggle(isDebt: Boolean, isPersian: Boolean, onSetType: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    TwoOptionToggle(
        firstLabel = tr(isPersian, "بدهی (پرداختی)", "Debt"),
        secondLabel = tr(isPersian, "طلب (دریافتی)", "Credit"),
        firstSelected = isDebt,
        firstColor = scheme.error,
        onFirstColor = scheme.onError,
        secondColor = scheme.primary,
        onSecondColor = scheme.onPrimary,
        onSelectFirst = { onSetType(true) },
        onSelectSecond = { onSetType(false) }
    )
}

/** نوار کوچک «بدهی ۵۰۰٬۰۰۰ تومان» بالای مرحله‌های بعدی؛ با لمس به مرحله‌ی مبلغ برمی‌گردد. */
@Composable
internal fun AmountReminder(
    isDebt: Boolean,
    amount: Long,
    currencyUnit: String,
    isPersian: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isDebt) tr(isPersian, "بدهی", "Debt") else tr(isPersian, "طلب", "Credit"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatSuccessAmount(amount, currencyUnit, isPersian),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ثبت با یک جمله: «۵۰۰ هزار تومان بدهی به علی». کل ردیف لمس‌پذیر است. */
@Composable
internal fun DebtVoicePill(isPersian: Boolean, accent: Color, onResult: (String) -> Unit) {
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
            text = tr(isPersian, "بگو: «۵۰۰ هزار تومان بدهی به علی»", "Say: “owe Ali 500k”"),
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

// -----------------------------------------------------------------------------
// فیلدهای نام و یادداشت
// -----------------------------------------------------------------------------

@Composable
internal fun DebtNameField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    isPersian: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= MAX_NAME_LENGTH) onValueChange(input) },
        label = { Text(tr(isPersian, "نام طرف حساب", "Person name")) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            {
                Text(
                    tr(
                        isPersian,
                        "نام طرف حساب رو فراموش کردی؛ یه اسم کوتاه بنویس",
                        "You forgot the person's name; add a short one"
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
                onResult = { spoken -> onValueChange(spoken.take(MAX_NAME_LENGTH)) },
                language = if (isPersian) "fa-IR" else "en-US"
            )
        }
    )
}

@Composable
internal fun DebtNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= MAX_NOTE_LENGTH) onValueChange(it) },
        label = { Text(tr(isPersian, "یادداشت (اختیاری)", "Note (optional)")) },
        modifier = modifier
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
                onResult = { spoken -> onValueChange(spoken.take(MAX_NOTE_LENGTH)) },
                language = if (isPersian) "fa-IR" else "en-US"
            )
        },
        supportingText = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "${value.length}/$MAX_NOTE_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    )
}

/** نام‌های قبلیِ پرتکرار؛ لمس یکی از آن‌ها نام را پر می‌کند (مثل دسته‌های پرکاربرد در ثبت تراکنش). */
@Composable
internal fun NameSuggestionRow(names: List<String>, accent: Color, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items = names, key = { it }) { name ->
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(accent.copy(alpha = 0.10f))
                    .border(1.dp, accent.copy(alpha = 0.25f), shape)
                    .clickable { onPick(name) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤 $name",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// نحوه‌ی تسویه: یک‌جا یا اقساطی
// -----------------------------------------------------------------------------

/**
 * بخش «نحوه‌ی تسویه» که هم در مرحله‌ی سوم ثبت و هم در صفحه‌ی ویرایش استفاده می‌شود:
 * کلید یک‌جا/اقساطی، و بسته به آن مهلت تسویه یا مبلغ و روز سررسید قسط.
 */
@Composable
internal fun PlanSection(
    form: DebtCreditFormState,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    errorText: String?
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxWidth()) {
        TwoOptionToggle(
            firstLabel = tr(isPersian, "یک‌جا", "One-time"),
            secondLabel = tr(isPersian, "اقساطی", "Installments"),
            firstSelected = !form.isMonthly,
            firstColor = accent,
            onFirstColor = onAccent,
            secondColor = accent,
            onSecondColor = onAccent,
            onSelectFirst = {
                if (form.isMonthly) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    form.isMonthly = false
                }
            },
            onSelectSecond = {
                if (!form.isMonthly) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    form.isMonthly = true
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        if (form.isMonthly) {
            MonthlyPlan(
                form = form,
                accent = accent,
                onAccent = onAccent,
                currencyUnit = currencyUnit,
                isPersian = isPersian,
                errorText = errorText
            )
        } else {
            OneTimePlan(form = form, accent = accent, isPersian = isPersian)
        }
    }
}

@Composable
private fun OneTimePlan(form: DebtCreditFormState, accent: Color, isPersian: Boolean) {
    val scheme = MaterialTheme.colorScheme
    var pickerOpen by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val weekDate = remember(today) { today.plusWeeks(1) }
    val monthDate = remember(today) { today.plusMonths(1) }

    val due = form.dueMillis
    val dueDate = if (due > 0L) millisToDate(due) else null
    val isWeek = dueDate == weekDate
    val isMonth = dueDate == monthDate
    val isCustom = dueDate != null && !isWeek && !isMonth

    Text(
        text = tr(isPersian, "مهلت تسویه", "Due date"),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DateChip(tr(isPersian, "بدون مهلت", "No date"), due <= 0L, accent, Modifier.weight(1f)) {
            form.dueMillis = 0L
        }
        DateChip(tr(isPersian, "۱ هفته", "1 week"), isWeek, accent, Modifier.weight(1f)) {
            form.dueMillis = dateToMillis(weekDate)
        }
        DateChip(tr(isPersian, "۱ ماه", "1 month"), isMonth, accent, Modifier.weight(1f)) {
            form.dueMillis = dateToMillis(monthDate)
        }
        DateChip(
            if (isCustom) formatDueDate(due, isPersian) else tr(isPersian, "📅 انتخاب", "📅 Pick"),
            isCustom,
            accent,
            Modifier.weight(1.3f)
        ) { pickerOpen = true }
    }

    if (dueDate != null) {
        val daysLeft = ChronoUnit.DAYS.between(today, dueDate)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (daysLeft >= 0L) {
                tr(
                    isPersian,
                    "سررسید ${formatDueDate(due, isPersian)} · ${localDigits(daysLeft, isPersian)} روز دیگر",
                    "Due ${formatDueDate(due, isPersian)} · in $daysLeft days"
                )
            } else {
                tr(
                    isPersian,
                    "سررسید ${formatDueDate(due, isPersian)} · گذشته",
                    "Due ${formatDueDate(due, isPersian)} · overdue"
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    } else {
        Spacer(Modifier.height(10.dp))
        Text(
            text = tr(
                isPersian,
                "بدون مهلت، یادآوری سررسید برای این مورد ارسال نمی‌شود.",
                "With no due date, no due reminders are sent for this record."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }

    if (pickerOpen) {
        WheelDatePickerDialog(
            isPersian = isPersian,
            title = tr(isPersian, "مهلت تسویه", "Due date"),
            initialMillis = if (due > 0L) due else dateToMillis(monthDate),
            onConfirm = { picked ->
                form.dueMillis = picked
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false }
        )
    }
}

@Composable
private fun MonthlyPlan(
    form: DebtCreditFormState,
    accent: Color,
    onAccent: Color,
    currencyUnit: String,
    isPersian: Boolean,
    errorText: String?
) {
    val scheme = MaterialTheme.colorScheme
    val total = form.amountValue
    val monthly = form.monthlyValue
    val words = amountInWords(monthly, currencyUnit, isPersian)
    val supporting = errorText ?: words

    Text(
        text = tr(isPersian, "مبلغ هر قسط", "Installment amount"),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = form.monthlyAmount,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.trimStart('0')
            if (digits.length <= MAX_AMOUNT_DIGITS) form.monthlyAmount = digits
        },
        label = { Text(tr(isPersian, "قسط ماهانه (${unitLabel(currencyUnit, true)})", "Monthly installment (${unitLabel(currencyUnit, false)})")) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        isError = errorText != null,
        supportingText = if (supporting.isNotEmpty()) {
            { Text(supporting) }
        } else {
            null
        },
        visualTransformation = ThousandsSeparatorTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            focusedLabelColor = accent,
            cursorColor = accent
        )
    )

    if (total > 0L) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = tr(isPersian, "تقسیم سریع مبلغ کل", "Split the total quickly"),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(3L, 6L, 12L, 24L).forEach { count ->
                val perInstallment = (total + count - 1) / count
                DateChip(
                    label = tr(isPersian, "${localDigits(count, true)} قسط", "${count}×"),
                    selected = perInstallment > 0L && monthly == perInstallment,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) {
                    if (perInstallment.toString().length <= MAX_AMOUNT_DIGITS) {
                        form.monthlyAmount = perInstallment.toString()
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = tr(isPersian, "روز سررسید هر قسط", "Due day of each month"),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    DayGrid(
        selectedDay = form.dueDay,
        accent = accent,
        onAccent = onAccent,
        isPersian = isPersian,
        onSelect = { form.dueDay = it }
    )
    if (form.dueDay >= 29) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = tr(
                isPersian,
                "در ماه‌هایی که این روز را ندارند، آخرین روز ماه در نظر گرفته می‌شود.",
                "In months without this day, the last day of the month is used."
            ),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant
        )
    }

    if (monthly in 1L..total) {
        val count = installmentCount(total, monthly)
        val encodedDay = DueDay.encode(form.dueDay.coerceIn(1, 31), isPersian)
        val last = remember(total, monthly, encodedDay) { lastInstallmentMillis(total, monthly, encodedDay) }
        val shape = RoundedCornerShape(16.dp)
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(accent.copy(alpha = 0.10f))
                .border(1.dp, accent.copy(alpha = 0.25f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = tr(
                    isPersian,
                    "${localDigits(count.toLong(), true)} قسط",
                    if (count == 1) "1 installment" else "$count installments"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (last != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tr(
                        isPersian,
                        "آخرین قسط: ${formatDueDate(last, true)}",
                        "Last installment: ${formatDueDate(last, false)}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

/** انتخابگر روز ماه به شکل تقویم کوچک (۷ ستون)؛ جایگزین چرخ اسکرولیِ دیالوگ قدیمی. */
@Composable
private fun DayGrid(
    selectedDay: Int,
    accent: Color,
    onAccent: Color,
    isPersian: Boolean,
    onSelect: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, scheme.outline.copy(alpha = 0.15f), shape)
            .padding(8.dp)
    ) {
        for (row in 0 until 5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val day = row * 7 + column + 1
                    if (day <= 31) {
                        val selected = day == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (selected) accent else Color.Transparent)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onSelect(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localDigits(day.toLong(), isPersian),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) onAccent else scheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}