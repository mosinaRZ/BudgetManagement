package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * قواعد خالصِ «نکته‌ی دکمه‌ی +» در صفحه‌ی دسته‌بندی‌ها (بدون وابستگی به Android تا روی JVM تست شود).
 *
 *  - در هر بار ورود به صفحه، اولین نکته بعد از چند ثانیه می‌آید و بعدی‌ها با فاصله‌ی بیشتر،
 *    حداکثر [MAX_PER_VISIT] بار؛ تا مزاحم نشود؛
 *  - وقتی کاربر چند بار خودش از همین دکمه دسته‌ی جدید شروع کرد ([LEARNED_AFTER_USES])، یعنی یاد گرفته
 *    و دیگر نکته نمی‌آید؛
 *  - سقف کلِ نمایش‌ها هم [MAX_TOTAL] است تا کاربری که هیچ‌وقت دسته نمی‌سازد هم برای همیشه اذیت نشود.
 */
object CategoryAddHintPolicy {
    const val FIRST_DELAY_MS = 2_500L
    const val REPEAT_INTERVAL_MS = 24_000L
    const val VISIBLE_MS = 4_800L
    const val MAX_PER_VISIT = 3
    const val MAX_TOTAL = 30
    const val LEARNED_AFTER_USES = 3

    fun delayBeforeNext(shownThisVisit: Int): Long =
        if (shownThisVisit == 0) FIRST_DELAY_MS else REPEAT_INTERVAL_MS

    fun shouldShow(shownThisVisit: Int, totalShown: Int, fabUses: Int): Boolean =
        shownThisVisit < MAX_PER_VISIT && totalShown < MAX_TOTAL && fabUses < LEARNED_AFTER_USES
}

/** شمارنده‌های نکته‌ی دکمه‌ی +؛ فقط عدد نگه می‌دارد، هیچ داده‌ی مالی یا شخصی‌ای نه. */
object CategoryHintPreferences {
    private const val PREFS_NAME = "category_hint_prefs"
    private const val KEY_TOTAL_SHOWN = "total_shown"
    private const val KEY_FAB_USES = "fab_uses"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun totalShown(context: Context): Int = prefs(context).getInt(KEY_TOTAL_SHOWN, 0)

    fun fabUses(context: Context): Int = prefs(context).getInt(KEY_FAB_USES, 0)

    fun recordShown(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_TOTAL_SHOWN, p.getInt(KEY_TOTAL_SHOWN, 0) + 1).apply()
    }

    /** کاربر خودش از دکمه‌ی + (یا از نکته) ساخت دسته‌ی جدید را شروع کرد. */
    fun recordFabUse(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_FAB_USES, p.getInt(KEY_FAB_USES, 0) + 1).apply()
    }
}