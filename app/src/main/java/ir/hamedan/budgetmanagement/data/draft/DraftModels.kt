package ir.hamedan.budgetmanagement.data.draft

import ir.hamedan.budgetmanagement.data.money.MoneyContract
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * جریان‌های ثبتی که پیش‌نویس دارند. برای هر نوع فقط **یک** پیش‌نویس نگه داشته می‌شود؛
 * ترتیب اعلام همین ترتیب نمایش در فهرست پیش‌نویس‌هاست.
 *
 * [key] در نام فایل و داده‌ی احراز اصالتِ رمزنگاری می‌آید و نباید عوض شود.
 */
enum class DraftType(val key: String) {
    TRANSACTION("transaction"),
    LOAN_RECORD("loan_record"),
    CATEGORY_EXPENSE("category_expense"),
    CATEGORY_INCOME("category_income"),
    BUDGET_LIMIT("budget_limit"),
    SAVING_GOAL("saving_goal");

    companion object {
        fun fromKey(key: String): DraftType? = values().firstOrNull { it.key == key }
    }
}

/**
 * یک پیش‌نویسِ ذخیره‌شده. [payload] حالت فرم است (مبلغ‌ها به واحد نمایشیِ زمان ذخیره، با کلید `unit`)
 * و [updatedAt] آخرین زمان ذخیره‌ی آن به میلی‌ثانیه است.
 */
class Draft(
    val type: DraftType,
    val payload: JsonObject,
    val updatedAt: Long
) {
    /** برای پیش‌نویس تراکنش: هزینه بوده یا درآمد (مسیر ادامه با همین نوع باز می‌شود). */
    val isExpense: Boolean get() = payload.bool("isExpense", true)
}

// ───────────────────────── خواندن امن از JSON ─────────────────────────
// پیش‌نویس ممکن است از نسخه‌ی قدیمی‌تر برنامه باشد؛ هر کلید گم‌شده یا با نوع نادرست، مقدار پیش‌فرض می‌گیرد.

internal fun JsonObject.str(key: String, default: String = ""): String =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content ?: default

internal fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

internal fun JsonObject.long(key: String, default: Long = 0L): Long =
    (this[key] as? JsonPrimitive)?.longOrNull ?: default

internal fun JsonObject.int(key: String, default: Int = 0): Int =
    (this[key] as? JsonPrimitive)?.intOrNull ?: default

/** تبدیل مبلغِ فرم بین واحدهای نمایشی (تومان/ریال) وقتی کاربر بین ذخیره و بازیابی واحد را عوض کرده باشد. */
object DraftMoney {
    private const val DEFAULT_MAX_DIGITS = 12

    /**
     * [raw] فقط رقم‌های انگلیسی است. خروجی هم همین‌طور است؛ خالی یعنی «مبلغی نیست»
     * (صفر، ورودی نامعتبر، یا بیش از [maxDigits] رقم بعد از تبدیل).
     */
    fun convert(raw: String, fromUnit: String, toUnit: String, maxDigits: Int = DEFAULT_MAX_DIGITS): String {
        val digits = raw.filter { it in '0'..'9' }
        val value = digits.toLongOrNull() ?: return ""
        val converted = when {
            fromUnit == toUnit -> value
            fromUnit == MoneyContract.DISPLAY_RIAL -> value / MoneyContract.RIALS_PER_TOMAN
            toUnit == MoneyContract.DISPLAY_RIAL -> value * MoneyContract.RIALS_PER_TOMAN
            else -> value
        }
        if (converted <= 0L) return ""
        val text = converted.toString()
        return if (text.length <= maxDigits) text else ""
    }
}
