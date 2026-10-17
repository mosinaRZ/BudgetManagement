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
}