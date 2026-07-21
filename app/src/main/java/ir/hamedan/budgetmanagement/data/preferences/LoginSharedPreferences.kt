package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context

object SharedPreferences {
    private const val PREF_NAME = "budget_management_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)


    // مدیریت وضعیت اثر انگشت (چک‌باکس تنظیمات)
    fun setBiometricEnabled(context: Context, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, isEnabled).apply()
    }

    fun getBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
}