package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

/**
 * قواعد مرکزی دسته‌بندی‌ها.
 *
 * دسته‌بندی «قلک» (SAVING_GOAL) فقط توسط خودِ سیستم (واریز/برداشت قلک، واریز ماهانه و
 * حذف قلک) استفاده می‌شود و کاربر نباید بتواند آن را برای تراکنش دستی، ویرایش تراکنش یا
 * تراکنش پیامکی انتخاب کند. همه‌ی صفحه‌ها باید از همین‌جا فیلتر کنند تا قاعده فقط یک منبع داشته باشد.
 */
object CategoryRules {

    const val SAVING_GOAL_TITLE = "SAVING_GOAL"

    /** آیا کاربر مجاز است این دسته را (با کلید title) انتخاب کند؟ */
    fun isUserSelectable(title: String): Boolean =
        !title.trim().equals(SAVING_GOAL_TITLE, ignoreCase = true)

    fun isUserSelectable(category: CategoryEntity): Boolean = isUserSelectable(category.title)

    /** لیست دسته‌هایی که کاربر اجازه‌ی انتخابشان را دارد. */
    fun userSelectable(categories: List<CategoryEntity>): List<CategoryEntity> =
        categories.filter { isUserSelectable(it) }

    /** کلیدهای دسته‌بندی‌های داخلی برنامه؛ هر کدام در هر حساب فقط یک نمونه می‌تواند داشته باشد. */
    private val BUILT_IN_KEYS = setOf(
        "FOOD", "TRANSPORT", "SHOPPING", "BILL", "SALARY", "INVESTMENT",
        "DEBT_CREDIT_PAYABLE", "DEBT_CREDIT_RECEIVABLE",
        SAVING_GOAL_TITLE, "SAVING_GOAL_RETURN", "UNCATEGORIZED"
    )

    fun isBuiltInKey(title: String): Boolean = title.trim().uppercase() in BUILT_IN_KEYS

    /**
     * شناسه‌ی ثابتِ دسته‌های پیش‌فرض (روی همه‌ی دستگاه‌ها یکسان است تا همگام‌سازی و
     * بازیابی پشتیبان، نسخه‌ی تکراری نسازند).
     */
    fun stableDefaultId(title: String): String =
        java.util.UUID.nameUUIDFromBytes(
            "budget-management:default-category:$title".toByteArray(Charsets.UTF_8)
        ).toString()

    /**
     * آیا دو دسته در واقع یک دسته‌اند؟ دسته‌های داخلی فقط با کلیدشان یکی حساب می‌شوند؛
     * دسته‌های کاربر با «عنوان + نوع (درآمد/هزینه)» و بدون حساسیت به حروف/فاصله‌ی اضافه.
     */
    fun isSameIdentity(a: CategoryEntity, b: CategoryEntity): Boolean {
        val ta = a.title.trim()
        val tb = b.title.trim()
        if (ta.isEmpty() || tb.isEmpty()) return false
        if (isBuiltInKey(ta) || isBuiltInKey(tb)) return ta.equals(tb, ignoreCase = true)
        return ta.equals(tb, ignoreCase = true) && a.isExpense == b.isExpense
    }

    /**
     * از بین چند نسخه‌ی تکراریِ یک دسته، نسخه‌ی اصلی را انتخاب می‌کند:
     * اول شناسه‌ی ثابت پیش‌فرض، بعد قدیمی‌ترین، و در نهایت کوچک‌ترین id (قطعی و تکرارپذیر).
     */
    fun pickCanonical(group: List<CategoryEntity>): CategoryEntity {
        val title = group.first().title.trim().uppercase()
        val stable = stableDefaultId(title)
        return group.firstOrNull { it.id == stable }
            ?: group.sortedWith(compareBy<CategoryEntity> { it.createdAt }.thenBy { it.id }).first()
    }
}