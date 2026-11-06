package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

/**
 * قواعد نام دسته‌بندی در فرم ثبت/ویرایش.
 *
 * بررسی نهایی و قطعیِ تکراری‌بودن همچنان در `CategoriesViewModel` است؛ این‌جا فقط برای
 * بازخورد فوری در فرم، کمی سخت‌گیرانه‌تر مقایسه می‌کنیم:
 *  - فاصله‌ی اضافه، نیم‌فاصله و تفاوت «ي/ی» و «ك/ک» نادیده گرفته می‌شود؛
 *  - نام نمایشیِ دسته‌های داخلی (مثل «خرید») هم حساب می‌شود، نه فقط کلید انگلیسی‌شان.
 */
object CategoryNameRules {

    const val MAX_TITLE_LENGTH = 20

    /** شکل مقایسه‌ای یک نام: یکدست‌شده و بی‌حساس به حروف. */
    fun comparisonKey(title: String): String =
        title
            .replace('\u064A', '\u06CC') // ي عربی ← ی فارسی
            .replace('\u0643', '\u06A9') // ك عربی ← ک فارسی
            .replace("\u200C", "")       // نیم‌فاصله
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    fun isDuplicate(
        candidate: String,
        existing: List<CategoryEntity>,
        editingId: String?,
        isPersian: Boolean
    ): Boolean {
        val key = comparisonKey(candidate)
        if (key.isEmpty()) return false
        if (CategoryRules.isBuiltInKey(candidate)) return true
        return existing.any { other ->
            other.id != editingId &&
                    (comparisonKey(other.title) == key ||
                            comparisonKey(StringMapper.getCategoryName(other.title, isPersian)) == key)
        }
    }
}