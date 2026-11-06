package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

/** یک عنوان پیشنهادی همراه با آیکون هماهنگ با آن. [emoji] همیشه عضو [CategoryIconCatalog] است. */
data class CategorySuggestion(
    val titleFa: String,
    val titleEn: String,
    val emoji: String
) {
    fun title(isPersian: Boolean): String = if (isPersian) titleFa else titleEn
}

/**
 * عنوان‌های پیشنهادیِ مرحله‌ی «نام» در ثبت دسته‌بندی؛ برای شروع سریع، بدون تایپ.
 * پیشنهادهایی که کاربر همین حالا (به هر نوع) نامشان را دارد حذف می‌شوند.
 */
object CategoryTitleSuggestions {

    val expense: List<CategorySuggestion> = listOf(
        CategorySuggestion("ورزش", "Sports", "🏋️"),
        CategorySuggestion("دانشگاه", "University", "🎓"),
        CategorySuggestion("کلاس و آموزش", "Courses", "📚"),
        CategorySuggestion("سلامت و دارو", "Health", "💊"),
        CategorySuggestion("درمان", "Medical", "🏥"),
        CategorySuggestion("سینما و تفریح", "Entertainment", "🎬"),
        CategorySuggestion("سفر", "Travel", "✈️"),
        CategorySuggestion("هدیه", "Gifts", "🎁"),
        CategorySuggestion("لوازم خانه", "Home", "🛋️"),
        CategorySuggestion("حیوان خانگی", "Pets", "🐶"),
        CategorySuggestion("آرایشگاه", "Beauty", "💈"),
        CategorySuggestion("پوشاک", "Clothing", "👕"),
        CategorySuggestion("موبایل و اینترنت", "Mobile & Data", "📱"),
        CategorySuggestion("کافه", "Café", "☕"),
        CategorySuggestion("کتاب", "Books", "📖"),
        CategorySuggestion("خودرو", "Car", "🚗"),
        CategorySuggestion("خیریه", "Charity", "❤️"),
        CategorySuggestion("بازی", "Gaming", "🎮")
    )

    val income: List<CategorySuggestion> = listOf(
        CategorySuggestion("فریلنس", "Freelance", "💻"),
        CategorySuggestion("پاداش", "Bonus", "🏆"),
        CategorySuggestion("اجاره‌ی دریافتی", "Rental Income", "🏠"),
        CategorySuggestion("سود بانکی", "Interest", "🏦"),
        CategorySuggestion("فروش", "Sales", "🛒"),
        CategorySuggestion("هدیه‌ی نقدی", "Cash Gift", "🎁"),
        CategorySuggestion("کمک‌هزینه", "Allowance", "🤝"),
        CategorySuggestion("بازگشت وجه", "Refund", "💸"),
        CategorySuggestion("سود سرمایه", "Dividends", "📈"),
        CategorySuggestion("درآمد جانبی", "Side Income", "💼")
    )

    fun forType(
        isExpense: Boolean,
        isPersian: Boolean,
        existing: List<CategoryEntity>
    ): List<CategorySuggestion> =
        (if (isExpense) expense else income).filterNot { suggestion ->
            CategoryNameRules.isDuplicate(suggestion.title(isPersian), existing, editingId = null, isPersian = isPersian)
        }
}