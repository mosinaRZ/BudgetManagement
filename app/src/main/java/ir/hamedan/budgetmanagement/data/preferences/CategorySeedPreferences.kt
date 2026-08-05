package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * فلگ ساده برای اطمینان از اینکه seed کردن دسته‌بندی‌های پیش‌فرض
 * فقط یک‌بار در کل طول عمر نصب اپ انجام می‌شود، نه هر بار که
 * صفحه‌ای که قبلاً این کار را انجام می‌داد ساخته می‌شود.
 */
object CategorySeedPreferences {
    private const val PREFS_NAME = "settings"
    private const val KEY_DEFAULT_CATEGORIES_SEEDED = "default_categories_seeded"

    fun isSeeded(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_DEFAULT_CATEGORIES_SEEDED, false)
    }

    fun setSeeded(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putBoolean(KEY_DEFAULT_CATEGORIES_SEEDED, true).apply()
    }
}