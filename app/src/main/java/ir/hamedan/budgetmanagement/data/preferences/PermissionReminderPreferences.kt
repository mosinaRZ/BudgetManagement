package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * زمان‌بندی یادآوری مجوزهایی که کاربر نداده.
 * برای هر مجوز جداگانه نگه می‌داریم: آخرین باری که یادآوری نشونش دادیم،
 * و اینکه آیا کاربر گفته «دیگه نشون نده».
 */
object PermissionReminderPreferences {
    private const val PREFS_NAME = "settings"

    // هر چند وقت یک‌بار (در حالت عادی) اجازه داریم دوباره یادآوری کنیم
    private const val REMINDER_INTERVAL_MILLIS = 4L * 24 * 60 * 60 * 1000L // ۴ روز

    // وقتی کاربر «بعداً یادم بنداز» رو می‌زنه، این‌قدر زودتر از حالت عادی دوباره یادآوری می‌شه
    private const val SNOOZE_MILLIS = 1L * 24 * 60 * 60 * 1000L // ۱ روز

    private fun lastShownKey(permissionKey: String) = "perm_reminder_last_shown_$permissionKey"
    private fun dismissedKey(permissionKey: String) = "perm_reminder_dismissed_$permissionKey"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDismissedForever(context: Context, permissionKey: String): Boolean =
        prefs(context).getBoolean(dismissedKey(permissionKey), false)

    fun dismissForever(context: Context, permissionKey: String) {
        prefs(context).edit().putBoolean(dismissedKey(permissionKey), true).apply()
    }

    /** آیا الان وقتشه که دوباره این مجوز رو یادآوری کنیم؟ */
    fun shouldRemindNow(context: Context, permissionKey: String): Boolean {
        if (isDismissedForever(context, permissionKey)) return false
        val lastShown = prefs(context).getLong(lastShownKey(permissionKey), 0L)
        return System.currentTimeMillis() - lastShown >= REMINDER_INTERVAL_MILLIS
    }

    /** وقتی بنر یادآوری واقعاً نمایش داده شد، این را صدا بزن تا فاصلهٔ عادی شروع بشه. */
    fun markShownNow(context: Context, permissionKey: String) {
        prefs(context).edit().putLong(lastShownKey(permissionKey), System.currentTimeMillis()).apply()
    }

    /** «بعداً یادم بنداز» — با فاصلهٔ کوتاه‌تر (SNOOZE_MILLIS) دوباره نمایش داده بشه. */
    fun snooze(context: Context, permissionKey: String) {
        val fakeLastShown = System.currentTimeMillis() - REMINDER_INTERVAL_MILLIS + SNOOZE_MILLIS
        prefs(context).edit().putLong(lastShownKey(permissionKey), fakeLastShown).apply()
    }
}