package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * فلگ ساده برای اینکه دیالوگ خوش‌آمدگویی/مجوزها فقط در اولین
 * اجرای برنامه نمایش داده شود؛ دقیقاً مشابه الگوی CategorySeedPreferences.
 */
object OnboardingPreferences {
    private const val PREFS_NAME = "settings"
    internal const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    internal const val KEY_ONBOARDING_COMPLETED_AT = "onboarding_completed_at"

    /** بعد از پایان راهنما، تا این مدت یادآوری مجوز نشان داده نمی‌شود (کاربر همین الان تصمیمش را گرفته). */
    internal const val REMINDER_GRACE_MILLIS = 24L * 60 * 60 * 1000L

    fun isCompleted(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setCompleted(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putLong(KEY_ONBOARDING_COMPLETED_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * آیا مهلتِ بعد از راهنما تمام شده و می‌شود یادآوری مجوز نشان داد؟
     * کاربران قدیمی (بدون زمان ثبت‌شده) فوراً مشمول‌اند.
     */
    fun isReminderGracePeriodOver(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val completedAt = preferences.getLong(KEY_ONBOARDING_COMPLETED_AT, 0L)
        return completedAt == 0L || System.currentTimeMillis() - completedAt >= REMINDER_GRACE_MILLIS
    }
}