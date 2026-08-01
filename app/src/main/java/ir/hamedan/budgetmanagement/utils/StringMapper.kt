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
            else -> key
        }
    }
}