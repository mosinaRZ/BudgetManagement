package ir.hamedan.budgetmanagement.data.share

import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything the share card needs, already formatted for the active app language
 * and currency. Keeping this separate from the renderer means the formatting rules
 * (digits, calendar, currency, sign) are plain Kotlin and unit-testable.
 */
data class TransactionShareContent(
    val title: String,
    val amountText: String,
    val currencyLabel: String,
    val typeLabel: String,
    val isExpense: Boolean,
    val categoryTitle: String,
    val categoryEmoji: String,
    val dateTimeText: String,
    val note: String,
    val referenceId: String,
    val isPersian: Boolean
)

object TransactionShareFormatter {

    private const val LRM = "\u200E"

    fun build(
        transaction: TransactionEntity,
        categoryTitle: String,
        categoryEmoji: String,
        currencyUnit: String,
        isPersian: Boolean,
        zone: ZoneId = ZoneId.systemDefault()
    ): TransactionShareContent {
        val isExpense = transaction.type == "EXPENSE"
        return TransactionShareContent(
            title = transaction.title.trim().ifBlank { categoryTitle },
            amountText = formatAmount(transaction.amount, currencyUnit, isExpense, isPersian),
            currencyLabel = currencyLabel(currencyUnit, isPersian),
            typeLabel = typeLabel(isExpense, isPersian),
            isExpense = isExpense,
            categoryTitle = categoryTitle,
            categoryEmoji = categoryEmoji.ifBlank { "📁" },
            dateTimeText = formatDateTime(transaction.timestamp, isPersian, zone),
            note = transaction.note.trim(),
            referenceId = referenceId(transaction.id),
            isPersian = isPersian
        )
    }

    /**
     * Amounts are stored in Toman; IRR is a display conversion (see [MoneyContract]).
     * The leading LRM keeps the sign on the left of the digits in RTL as well.
     */
    fun formatAmount(
        storedToman: Long,
        currencyUnit: String,
        isExpense: Boolean,
        isPersian: Boolean
    ): String {
        val display = MoneyContract.displayFromStorage(storedToman, currencyUnit)
        val locale = if (isPersian) Locale.forLanguageTag("fa-IR") else Locale.US
        val formatter = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        val sign = if (isExpense) "-" else "+"
        return "$LRM$sign${formatter.format(display)}"
    }

    fun currencyLabel(currencyUnit: String, isPersian: Boolean): String {
        val isRial = currencyUnit == MoneyContract.DISPLAY_RIAL
        return if (isPersian) {
            if (isRial) "ریال" else "تومان"
        } else {
            if (isRial) "Rial" else "Toman"
        }
    }

    fun typeLabel(isExpense: Boolean, isPersian: Boolean): String = when {
        isPersian && isExpense -> "هزینه"
        isPersian -> "درآمد"
        isExpense -> "Expense"
        else -> "Income"
    }

    fun formatDateTime(millis: Long, isPersian: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val zoned = Instant.ofEpochMilli(millis).atZone(zone)
        val time = zoned.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
        return if (isPersian) {
            val (year, month, day) = DateUtils.toJalali(zoned.toLocalDate())
            val date = "$day ${DateUtils.PERSIAN_MONTH_NAMES[month - 1]} $year"
            toPersianDigits("$date • $time")
        } else {
            val date = zoned.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
            "$date • $time"
        }
    }

    /** Short, human-friendly tracking code derived from the transaction id. */
    fun referenceId(id: String): String =
        id.filter { it.isLetterOrDigit() }.take(8).uppercase(Locale.ROOT)

    fun toPersianDigits(value: String): String {
        val out = StringBuilder(value.length)
        for (ch in value) {
            out.append(if (ch in '0'..'9') '\u06F0' + (ch - '0') else ch)
        }
        return out.toString()
    }
}