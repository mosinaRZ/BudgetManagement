package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import android.content.SharedPreferences

object CurrencySharedPreferences {
    private const val PREF_NAME = "budget_management_prefs"
    private const val KEY_CURRENCY = "app_currency"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setCurrency(context: Context, currency: String) {
        getSharedPreferences(context).edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getCurrency(context: Context): String {
        return getSharedPreferences(context).getString(KEY_CURRENCY, "IRT") ?: "IRT"
    }
}