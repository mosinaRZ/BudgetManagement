package ir.hamedan.budgetmanagement.data.security

import android.content.Context

/**
 * Stores only the local app-lock state. It deliberately does not touch
 * authentication/session tokens, so access/refresh token lifetimes remain
 * controlled exclusively by the existing auth/session layer.
 */
class AppLockPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun markBackgrounded(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_BACKGROUND_AT, timestamp).apply()
    }

    fun shouldLockNow(now: Long = System.currentTimeMillis()): Boolean {
        if (prefs.getBoolean(KEY_LOCK_REQUIRED, false)) return true

        val backgroundAt = prefs.getLong(KEY_BACKGROUND_AT, 0L)
        return backgroundAt > 0L && now - backgroundAt >= LOCK_AFTER_MS
    }

    fun markLockRequired() {
        prefs.edit()
            .putBoolean(KEY_LOCK_REQUIRED, true)
            .remove(KEY_BACKGROUND_AT)
            .apply()
    }

    fun clearLock() {
        prefs.edit()
            .putBoolean(KEY_LOCK_REQUIRED, false)
            .remove(KEY_BACKGROUND_AT)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "budget_app_lock"
        private const val KEY_BACKGROUND_AT = "background_at"
        private const val KEY_LOCK_REQUIRED = "lock_required"
        private const val LOCK_AFTER_MS = 2 * 60 * 1000L
    }
}