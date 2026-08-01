package ir.hamedan.budgetmanagement.utils

import android.content.Context
import ir.hamedan.budgetmanagement.R

object StringMapper {

    /**
     * Maps DB category keys to localized display names.
     * Prefer this over hard-coded fa/en branches.
     */
    fun getCategoryName(context: Context, key: String): String {
        val resId = when (key.uppercase()) {
            "FOOD" -> R.string.category_food
            "TRANSPORT" -> R.string.category_transport
            "SHOPPING" -> R.string.category_shopping
            "BILL" -> R.string.category_bill
            "SALARY" -> R.string.category_salary
            "INVESTMENT" -> R.string.category_investment
            "UNCATEGORIZED" -> R.string.category_uncategorized
            else -> null
        }
        return resId?.let { context.getString(it) } ?: key
    }

    /** @deprecated Use getCategoryName(context, key) */
    @Deprecated("Use getCategoryName(context, key) with string resources")
    fun getCategoryName(key: String, isPersian: Boolean): String {
        return when (key.uppercase()) {
            "FOOD" -> if (isPersian) "خوراکی و رستوران" else "Food & Dining"
            "TRANSPORT" -> if (isPersian) "حمل و نقل" else "Transportation"
            "SHOPPING" -> if (isPersian) "خرید" else "Shopping"
            "BILL" -> if (isPersian) "قبوض و اجاره" else "Bills & Rent"
            "SALARY" -> if (isPersian) "حقوق و درآمد" else "Salary"
            "INVESTMENT" -> if (isPersian) "سرمایه‌گذاری" else "Investment"
            "UNCATEGORIZED" -> if (isPersian) "دسته‌بندی نشده" else "Uncategorized"
            else -> key
        }
    }
}