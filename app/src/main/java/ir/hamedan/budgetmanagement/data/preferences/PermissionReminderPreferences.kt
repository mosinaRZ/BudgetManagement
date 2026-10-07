package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * زمان‌بندیِ یادآوری مجوزهای داده‌نشده.
 *
 * اصول طراحی (مزاحم نبودن، ولی فراموش‌نشدن):
 *  - فاصلهٔ یادآوری‌ها هر بار بیشتر می‌شود: ۴ روز ← ۸ روز ← ۱۶ روز.
 *  - بعد از [MAX_AUTO_REMINDERS] بار نمایش، دیگر خودکار مزاحم نمی‌شویم.
 *  - اولین «بعداً» کوتاه‌تر است ([SNOOZE_MILLIS] = ۱ روز)؛ «بعداً»های بعدی همان فاصلهٔ افزایشی را دارند.
 *  - «دیگه نشون نده» برای همیشه این مجوز را از یادآوری حذف می‌کند.
 *  - اگر کاربر قبلاً درخواست سیستمی را دیده و رد کرده باشد ([wasRequested])، دکمهٔ «فعال‌سازی»
 *    به‌جای دیالوگ سیستمی (که دیگر نمایش داده نمی‌شود) باید تنظیمات برنامه را باز کند.
 */
object PermissionReminderPreferences {
    private const val PREFS_NAME = "settings"

    internal const val REMINDER_INTERVAL_MILLIS = 4L * 24 * 60 * 60 * 1000L
    internal const val SNOOZE_MILLIS = 1L * 24 * 60 * 60 * 1000L

    /** حداکثر دفعاتی که یادآوری خودکار برای هر مجوز نشان داده می‌شود. */
    internal const val MAX_AUTO_REMINDERS = 4

    private fun lastShownKey(permissionKey: String) = "perm_reminder_last_shown_$permissionKey"
    private fun dismissedKey(permissionKey: String) = "perm_reminder_dismissed_$permissionKey"
    private fun countKey(permissionKey: String) = "perm_reminder_count_$permissionKey"
    private fun requestedKey(permissionKey: String) = "perm_requested_$permissionKey"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** فاصلهٔ لازم تا یادآوری بعدی، بر اساس تعداد دفعاتی که تا حالا نشان داده شده. */
    internal fun intervalFor(shownCount: Int): Long = when {
        shownCount <= 1 -> REMINDER_INTERVAL_MILLIS
        shownCount == 2 -> REMINDER_INTERVAL_MILLIS * 2
        else -> REMINDER_INTERVAL_MILLIS * 4
    }

    fun isDismissedForever(context: Context, permissionKey: String): Boolean =
        prefs(context).getBoolean(dismissedKey(permissionKey), false)

    fun dismissForever(context: Context, permissionKey: String) {
        prefs(context).edit().putBoolean(dismissedKey(permissionKey), true).apply()
    }

    fun shownCount(context: Context, permissionKey: String): Int =
        prefs(context).getInt(countKey(permissionKey), 0)

    fun shouldRemindNow(context: Context, permissionKey: String): Boolean {
        if (isDismissedForever(context, permissionKey)) return false
        val count = shownCount(context, permissionKey)
        if (count >= MAX_AUTO_REMINDERS) return false
        val lastShown = prefs(context).getLong(lastShownKey(permissionKey), 0L)
        return System.currentTimeMillis() - lastShown >= intervalFor(count)
    }

    fun markShownNow(context: Context, permissionKey: String) {
        prefs(context).edit()
            .putLong(lastShownKey(permissionKey), System.currentTimeMillis())
            .putInt(countKey(permissionKey), shownCount(context, permissionKey) + 1)
            .apply()
    }

    /**
     * «بعداً». فقط اولین بار یادآوری زودتر (بعد از [SNOOZE_MILLIS]) تکرار می‌شود؛
     * بعد از آن، همان فاصلهٔ افزایشی معتبر است تا کاربر اذیت نشود.
     */
    fun snooze(context: Context, permissionKey: String) {
        val count = shownCount(context, permissionKey)
        val lastShown = if (count <= 1) {
            System.currentTimeMillis() - intervalFor(count) + SNOOZE_MILLIS
        } else {
            System.currentTimeMillis()
        }
        prefs(context).edit().putLong(lastShownKey(permissionKey), lastShown).apply()
    }

    /** ثبت اینکه درخواست سیستمیِ این مجوز حداقل یک‌بار به کاربر نشان داده شده است. */
    fun markRequested(context: Context, permissionKey: String) {
        prefs(context).edit().putBoolean(requestedKey(permissionKey), true).apply()
    }

    fun wasRequested(context: Context, permissionKey: String): Boolean =
        prefs(context).getBoolean(requestedKey(permissionKey), false)
}