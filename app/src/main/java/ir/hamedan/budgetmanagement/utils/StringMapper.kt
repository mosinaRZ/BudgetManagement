package ir.hamedan.budgetmanagement.utils

object StringMapper {
    private val categoryKeys = listOf(
        "DEBT_CREDIT_RECEIVABLE",
        "DEBT_CREDIT_PAYABLE",
        "SAVING_GOAL_RETURN",
        "UNCATEGORIZED",
        "TRANSPORT",
        "INVESTMENT",
        "SAVING_GOAL",
        "SHOPPING",
        "SALARY",
        "FOOD",
        "BILL"
    )

    fun getCategoryName(key: String, isPersian: Boolean): String {
        return when (key.uppercase()) {
            "FOOD" -> if (isPersian) "خوراکی و رستوران" else "Food & Dining"
            "TRANSPORT" -> if (isPersian) "حمل و نقل" else "Transportation"
            "SHOPPING" -> if (isPersian) "خرید" else "Shopping"
            "BILL" -> if (isPersian) "قبوض و اجاره" else "Bills & Rent"
            "SALARY" -> if (isPersian) "حقوق و درآمد" else "Salary"
            "INVESTMENT" -> if (isPersian) "سرمایه‌گذاری" else "Investment"
            "UNCATEGORIZED" -> if (isPersian) "دسته‌بندی نشده" else "Uncategorized"
            "DEBT_CREDIT_PAYABLE" -> if (isPersian) "بدهی و وام" else "Debt & Payables"
            "DEBT_CREDIT_RECEIVABLE" -> if (isPersian) "طلب و مطالبات" else "Receivables"
            "SAVING_GOAL" -> if (isPersian) "قلک" else "Piggy Bank"
            "SAVING_GOAL_RETURN" -> if (isPersian) "بازگشت مبلغ قلک" else "Savings Goal Return"
            else -> key
        }
    }

    fun localizeCategoryKeysInText(text: String, isPersian: Boolean): String {
        return categoryKeys.fold(text) { result, key ->
            result.replace(
                Regex("(?<![A-Za-z_])${Regex.escape(key)}(?![A-Za-z_])", RegexOption.IGNORE_CASE),
                getCategoryName(key, isPersian)
            )
        }
    }
}