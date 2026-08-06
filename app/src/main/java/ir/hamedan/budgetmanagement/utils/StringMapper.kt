package ir.hamedan.budgetmanagement.utils

object StringMapper {
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
            "SAVING_GOAL" -> if (isPersian) "قلک" else "Piggy Bank"   // ← جدید
            else -> key
        }
    }
}