package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CurrencySharedPreferences {
    private const val PREF_NAME = "budget_management_prefs"
    private const val KEY_CURRENCY = "app_currency"

    private val _currencyFlow = MutableStateFlow("IRT")
    val currencyFlow: StateFlow<String> = _currencyFlow.asStateFlow()

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // مقدار اولیه را در زمان اجرای برنامه تنظیم کنید
    fun init(context: Context) {
        _currencyFlow.value = getCurrency(context)
    }

    fun setCurrency(context: Context, currency: String) {
        getSharedPreferences(context).edit().putString(KEY_CURRENCY, currency).apply()
        _currencyFlow.value = currency // اطلاع‌رسانی آنی به تمام شنونده‌ها
    }

    fun getCurrency(context: Context): String {
        return getSharedPreferences(context).getString(KEY_CURRENCY, "IRT") ?: "IRT"
    }
}