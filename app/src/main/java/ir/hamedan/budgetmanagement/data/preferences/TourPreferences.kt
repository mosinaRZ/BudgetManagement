package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

/**
 * وضعیت «تور معرفی برنامه» (راهنمای تعاملی با اسپات‌لایت).
 *
 * قرارداد:
 *  - تور در اولین ورود به صفحه‌ی اصلی برای هر حسابی که هنوز آن را ندیده شروع می‌شود (حساب‌های جدید و
 *    قدیمی). لحظه‌ی ثبت‌نام موفق هم شناسه‌ی کاربر ثبت می‌شود ([markPending]) که فقط برای پیگیری است.
 *  - هر کاربر فقط یک‌بار تور را می‌بیند: پایان یا «رد کردن» تور، شناسه‌ی او را در فهرست کاربران
 *    تمام‌کرده ثبت می‌کند ([markCompleted]).
 *  - پیشرفت وسط تور عمداً ذخیره نمی‌شود؛ اگر برنامه وسط تور بسته شود، چون هنوز «تمام‌شده» ثبت نشده،
 *    دفعه‌ی بعد تور از ابتدا شروع می‌شود.
 */
object TourPreferences {
    private const val PREFS_NAME = "cidna_app_tour"
    /** شناسه‌ی ثابت فضای مهمان (مهمان حساب سروری ندارد). */
    const val GUEST_ID = "guest"
    internal const val KEY_PENDING_USER = "pending_user_id"
    internal const val KEY_COMPLETED_USERS = "completed_user_ids"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** بعد از ثبت‌نام موفق صدا زده می‌شود تا تور برای همین کاربر در اولین ورود نمایش داده شود. */
    fun markPending(context: Context, userId: String?) {
        if (userId.isNullOrBlank()) return
        if (isCompleted(context, userId)) return
        prefs(context).edit().putString(KEY_PENDING_USER, userId).apply()
    }

    /**
     * آیا باید تور را همین حالا برای [userId] شروع کرد؟
     * هر کاربرِ واردشده‌ای که هنوز تور را کامل نکرده (یا رد نکرده) آن را می‌بیند؛ یعنی کاربران قدیمی که
     * قبل از این نسخه ثبت‌نام کرده‌اند هم در اولین ورود بعد از به‌روزرسانی، یک‌بار تور را می‌بینند.
     * مهمان‌ها با شناسه‌ی ثابت [GUEST_ID] یک‌بار تور را می‌بینند.
     */
    fun shouldShow(context: Context, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return !isCompleted(context, userId)
    }

    fun isCompleted(context: Context, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val done = prefs(context).getStringSet(KEY_COMPLETED_USERS, emptySet()).orEmpty()
        return userId in done
    }

    /** پایان تور (یا رد کردن آن): دیگر هرگز برای این کاربر نمایش داده نمی‌شود. */
    fun markCompleted(context: Context, userId: String?) {
        if (userId.isNullOrBlank()) return
        val preferences = prefs(context)
        // مجموعه‌ی برگشتی از SharedPreferences نباید مستقیم تغییر کند؛ کپی می‌گیریم.
        val done = preferences.getStringSet(KEY_COMPLETED_USERS, emptySet()).orEmpty().toMutableSet()
        done.add(userId)
        val editor = preferences.edit().putStringSet(KEY_COMPLETED_USERS, done)
        if (preferences.getString(KEY_PENDING_USER, null) == userId) editor.remove(KEY_PENDING_USER)
        editor.apply()
    }
}