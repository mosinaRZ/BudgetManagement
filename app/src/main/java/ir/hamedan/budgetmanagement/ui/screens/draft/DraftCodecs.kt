package ir.hamedan.budgetmanagement.ui.screens.draft

import ir.hamedan.budgetmanagement.data.draft.Draft
import ir.hamedan.budgetmanagement.data.draft.DraftMoney
import ir.hamedan.budgetmanagement.data.draft.DraftType
import ir.hamedan.budgetmanagement.data.draft.bool
import ir.hamedan.budgetmanagement.data.draft.int
import ir.hamedan.budgetmanagement.data.draft.long
import ir.hamedan.budgetmanagement.data.draft.str
import ir.hamedan.budgetmanagement.ui.screens.add.AddTransactionFormState
import ir.hamedan.budgetmanagement.ui.screens.add.formatNumber
import ir.hamedan.budgetmanagement.ui.screens.add.unitLabel
import ir.hamedan.budgetmanagement.ui.screens.budgetLimit.LimitFormState
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoryFormState
import ir.hamedan.budgetmanagement.ui.screens.debtCredit.DebtCreditFormState
import ir.hamedan.budgetmanagement.ui.screens.goals.GoalFormState
import ir.hamedan.budgetmanagement.utils.CategoryNameRules
import ir.hamedan.budgetmanagement.utils.StringMapper
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * تبدیل حالت هر فرم به پیش‌نویس و برعکس.
 *
 * - `snapshot` (نام‌های بدون پیشوند) `null` برمی‌گرداند وقتی فرم هنوز «محتوای واقعی» ندارد؛
 *   پیش‌نویسِ خالی هرگز ذخیره نمی‌شود.
 * - `apply…` مقدارها را اعتبارسنجی و محدود می‌کند: مرحله‌ای که پیش‌نیازش پر نیست به مرحله‌ی قبل برمی‌گردد،
 *   مبلغ‌ها به واحد فعلیِ کاربر تبدیل می‌شوند و تاریخ‌های نسبی (امروز/بازه) دوباره محاسبه می‌شوند.
 * - ثابت‌های مرحله‌ها اینجا عدد خام‌اند چون در فایل صفحه‌ها `private` هستند؛ با آن‌ها هم‌خوان نگه دارید.
 */
internal object DraftCodecs {

    // حداکثر شماره‌ی مرحله‌ی هر جریان (مرحله‌ی «مرور» آخرین مرحله است).
    private const val TX_LAST_STEP = 2
    private const val LOAN_LAST_STEP = 3
    private const val CATEGORY_LAST_STEP = 2
    private const val LIMIT_LAST_STEP = 3
    private const val GOAL_LAST_STEP = 3
    private const val TX_TITLE_MAX = 40
    private const val GOAL_TITLE_MAX = 25
    private const val DEFAULT_LIMIT_DAYS = 30L

    // ───────────────────────── تراکنش ─────────────────────────

    fun transaction(form: AddTransactionFormState, currencyUnit: String): JsonObject? {
        if (form.amount.isBlank() && form.title.isBlank() && form.note.isBlank()) return null
        // تاریخ انتخابی به‌صورت «روزِ مطلق» ذخیره می‌شود، نه «چند روز قبل»؛ تا فردا جابه‌جا نشود.
        val selectedDay = LocalDate.now().toEpochDay() - form.dayOffset
        return buildJsonObject {
            put("unit", currencyUnit)
            put("isExpense", form.isExpense)
            put("amount", form.amount)
            put("categoryKey", form.categoryKey)
            put("title", form.title)
            put("note", form.note)
            put("manualCategory", form.manualCategory)
            put("autoSuggested", form.autoSuggested)
            put("step", form.step)
            put("epochDay", selectedDay)
        }
    }

    fun applyTransaction(form: AddTransactionFormState, d: JsonObject, currencyUnit: String) {
        val savedUnit = d.str("unit", currencyUnit)
        form.isExpense = d.bool("isExpense", true)
        form.amount = DraftMoney.convert(d.str("amount"), savedUnit, currencyUnit)
        form.categoryKey = d.str("categoryKey")
        form.title = d.str("title").take(TX_TITLE_MAX)
        form.note = d.str("note")
        form.manualCategory = d.bool("manualCategory")
        form.autoSuggested = d.bool("autoSuggested")
        val today = LocalDate.now().toEpochDay()
        form.dayOffset = (today - d.long("epochDay", today)).coerceIn(0L, MAX_BACKDATE_DAYS).toInt()

        var step = d.int("step").coerceIn(0, TX_LAST_STEP)
        if (step >= 1 && !form.hasAmount) step = 0
        if (step >= 2 && (!form.hasCategory || form.title.isBlank())) step = 1
        form.step = step
    }

    // ───────────────────────── بدهی / طلب ─────────────────────────

    fun loan(form: DebtCreditFormState, currencyUnit: String): JsonObject? {
        if (form.amount.isBlank() && form.name.isBlank() && form.note.isBlank()) return null
        return buildJsonObject {
            put("unit", currencyUnit)
            put("isDebt", form.isDebt)
            put("amount", form.amount)
            put("name", form.name)
            put("isMonthly", form.isMonthly)
            put("monthlyAmount", form.monthlyAmount)
            put("dueDay", form.dueDay)
            put("dueMillis", form.dueMillis)
            put("note", form.note)
            put("addToBalance", form.addToBalance)
            put("step", form.step)
        }
    }

    fun applyLoan(form: DebtCreditFormState, d: JsonObject, currencyUnit: String) {
        val savedUnit = d.str("unit", currencyUnit)
        form.isDebt = d.bool("isDebt", true)
        form.amount = DraftMoney.convert(d.str("amount"), savedUnit, currencyUnit)
        form.name = d.str("name")
        form.isMonthly = d.bool("isMonthly")
        form.monthlyAmount = DraftMoney.convert(d.str("monthlyAmount"), savedUnit, currencyUnit)
        form.dueDay = d.int("dueDay", form.dueDay).coerceIn(1, 31)
        form.dueMillis = d.long("dueMillis", form.dueMillis)
        form.note = d.str("note")
        form.addToBalance = d.bool("addToBalance", true)

        var step = d.int("step").coerceIn(0, LOAN_LAST_STEP)
        if (step >= 1 && !form.hasAmount) step = 0
        if (step >= 2 && form.name.isBlank()) step = 1
        if (step >= 3 && form.planError(false) != null) step = 2
        form.step = step
    }

    // ───────────────────────── دسته‌بندی ─────────────────────────

    fun category(form: CategoryFormState): JsonObject? {
        if (form.title.isBlank() && !form.iconManual) return null
        return buildJsonObject {
            put("title", form.title)
            put("emoji", form.emoji)
            put("iconManual", form.iconManual)
            put("step", form.step)
        }
    }

    fun applyCategory(form: CategoryFormState, d: JsonObject) {
        form.title = d.str("title").take(CategoryNameRules.MAX_TITLE_LENGTH)
        form.emoji = d.str("emoji")
        form.iconManual = d.bool("iconManual")

        var step = d.int("step").coerceIn(0, CATEGORY_LAST_STEP)
        if (step >= 1 && form.title.isBlank()) step = 0
        if (step >= 2 && !form.hasIcon) step = 1
        form.step = step
    }

    // ───────────────────────── سقف بودجه ─────────────────────────

    fun limit(form: LimitFormState, currencyUnit: String): JsonObject? {
        if (!form.hasCategory && form.amount.isBlank()) return null
        return buildJsonObject {
            put("unit", currencyUnit)
            put("categoryId", form.categoryId)
            put("amount", form.amount)
            put("startDay", form.startDay)
            put("endDay", form.endDay)
            put("step", form.step)
        }
    }

    fun applyLimit(form: LimitFormState, d: JsonObject, currencyUnit: String) {
        val savedUnit = d.str("unit", currencyUnit)
        form.categoryId = d.str("categoryId")
        form.amount = DraftMoney.convert(d.str("amount"), savedUnit, currencyUnit)

        val today = LocalDate.now().toEpochDay()
        var start = d.long("startDay", today)
        var end = d.long("endDay", today + DEFAULT_LIMIT_DAYS - 1)
        if (end < start) end = start
        // بازه‌ای که کاملاً گذشته است سقف بی‌معنایی می‌سازد؛ طولش را نگه می‌داریم و از امروز شروع می‌کنیم.
        if (end < today) {
            val length = end - start
            start = today
            end = today + length
        }
        form.startDay = start
        form.endDay = end

        var step = d.int("step").coerceIn(0, LIMIT_LAST_STEP)
        if (step >= 1 && !form.hasCategory) step = 0
        if (step >= 2 && !form.hasAmount) step = 1
        form.step = step
    }

    // ───────────────────────── قلک ─────────────────────────

    fun goal(form: GoalFormState, currencyUnit: String): JsonObject? {
        if (form.title.isBlank() && form.target.isBlank() && form.monthly.isBlank() && !form.iconManual) return null
        return buildJsonObject {
            put("unit", currencyUnit)
            put("title", form.title)
            put("icon", form.icon)
            put("iconManual", form.iconManual)
            put("target", form.target)
            put("monthly", form.monthly)
            put("step", form.step)
            put("field", form.field)
        }
    }

    fun applyGoal(form: GoalFormState, d: JsonObject, currencyUnit: String) {
        val savedUnit = d.str("unit", currencyUnit)
        form.title = d.str("title").take(GOAL_TITLE_MAX)
        form.icon = d.str("icon")
        form.iconManual = d.bool("iconManual")
        form.target = DraftMoney.convert(d.str("target"), savedUnit, currencyUnit)
        form.monthly = DraftMoney.convert(d.str("monthly"), savedUnit, currencyUnit)
        form.field = d.int("field").coerceIn(0, 1)
        form.returnToReview = false

        var step = d.int("step").coerceIn(0, GOAL_LAST_STEP)
        if (step >= 1 && form.title.isBlank()) step = 0
        if (step >= 2 && !form.hasIcon) step = 1
        if (step >= 3 && form.targetValue <= 0L) step = 2
        form.step = step
    }

    private const val MAX_BACKDATE_DAYS = 3650L
}

// ───────────────────────── خلاصه‌ی نمایشی ─────────────────────────

/** متنِ کارت یک پیش‌نویس در فهرست و شیت «ادامه»؛ [emoji] داخل حباب گرادیانی نمایش داده می‌شود. */
internal data class DraftSummary(val emoji: String, val title: String, val detail: String)

/** مبلغ ذخیره‌شده (به واحد زمان ذخیره) با واحد فعلی، قالب‌بندی‌شده؛ خالی اگر مبلغی نبود. */
private fun amountText(raw: String, savedUnit: String, currentUnit: String, isPersian: Boolean): String {
    val converted = DraftMoney.convert(raw, savedUnit, currentUnit).toLongOrNull() ?: return ""
    return formatNumber(converted, isPersian) + " " + unitLabel(currentUnit, isPersian)
}

private fun joinDetail(vararg parts: String): String = parts.filter { it.isNotBlank() }.joinToString(" · ")

internal fun describeDraft(draft: Draft, isPersian: Boolean, currentUnit: String): DraftSummary {
    val d = draft.payload
    val savedUnit = d.str("unit", currentUnit)
    return when (draft.type) {
        DraftType.TRANSACTION -> {
            val expense = d.bool("isExpense", true)
            val categoryKey = d.str("categoryKey")
            DraftSummary(
                emoji = if (expense) "🧾" else "💰",
                title = if (isPersian) {
                    if (expense) "هزینه‌ی ناتمام" else "درآمد ناتمام"
                } else {
                    if (expense) "Unfinished expense" else "Unfinished income"
                },
                detail = joinDetail(
                    amountText(d.str("amount"), savedUnit, currentUnit, isPersian),
                    d.str("title").trim(),
                    if (categoryKey.isBlank()) "" else StringMapper.getCategoryName(categoryKey, isPersian)
                )
            )
        }

        DraftType.LOAN_RECORD -> {
            val debt = d.bool("isDebt", true)
            DraftSummary(
                emoji = "🤝",
                title = if (isPersian) {
                    if (debt) "بدهی ناتمام" else "طلب ناتمام"
                } else {
                    if (debt) "Unfinished debt" else "Unfinished credit"
                },
                detail = joinDetail(
                    d.str("name").trim(),
                    amountText(d.str("amount"), savedUnit, currentUnit, isPersian)
                )
            )
        }

        DraftType.CATEGORY_EXPENSE, DraftType.CATEGORY_INCOME -> {
            val expense = draft.type == DraftType.CATEGORY_EXPENSE
            DraftSummary(
                emoji = d.str("emoji").ifBlank { "🗂️" },
                title = if (isPersian) {
                    if (expense) "دسته‌ی هزینه‌ی ناتمام" else "دسته‌ی درآمد ناتمام"
                } else {
                    if (expense) "Unfinished expense category" else "Unfinished income category"
                },
                detail = d.str("title").trim()
            )
        }

        DraftType.BUDGET_LIMIT -> {
            val days = (d.long("endDay") - d.long("startDay") + 1L).coerceAtLeast(0L)
            val period = when {
                days <= 0L -> ""
                isPersian -> "${formatNumber(days, true)} روزه"
                else -> "$days-day period"
            }
            DraftSummary(
                emoji = "🎯",
                title = if (isPersian) "سقف بودجه‌ی ناتمام" else "Unfinished budget limit",
                detail = joinDetail(
                    amountText(d.str("amount"), savedUnit, currentUnit, isPersian),
                    period
                )
            )
        }

        DraftType.SAVING_GOAL -> DraftSummary(
            emoji = d.str("icon").ifBlank { "🐷" },
            title = if (isPersian) "قلک ناتمام" else "Unfinished savings goal",
            detail = joinDetail(
                d.str("title").trim(),
                amountText(d.str("target"), savedUnit, currentUnit, isPersian)
            )
        )
    }
}