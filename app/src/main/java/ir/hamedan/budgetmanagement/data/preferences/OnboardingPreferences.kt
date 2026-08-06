package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * فلگ ساده برای اینکه دیالوگ خوش‌آمدگویی/مجوزها فقط در اولین
 * اجرای برنامه نمایش داده شود؛ دقیقاً مشابه الگوی CategorySeedPreferences.
 */
object OnboardingPreferences {
    private const val PREFS_NAME = "settings"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setCompleted(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }
}