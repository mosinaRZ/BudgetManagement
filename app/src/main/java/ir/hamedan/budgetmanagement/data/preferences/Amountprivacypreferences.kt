package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import ir.hamedan.budgetmanagement.ui.components.updateBalanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * منبع واحد حالت «مخفی بودن مبالغ» برای HomeScreen و ویجت.
 * - مقدار در SharedPreferences ذخیره می‌شود (همان فایل قبلی، پس تنظیم کاربر حفظ می‌شود)
 * - StateFlow باعث می‌شود تغییر از هر طرف (اپ یا ویجت) فوراً به طرف دیگر برسد
 * - هر تغییر، ویجت را هم به‌روز می‌کند
 */
object AmountPrivacyPreferences {
    private const val PREFS_NAME = "home_privacy_prefs"
    private const val KEY_AMOUNTS_HIDDEN = "key_amounts_hidden"

    private val _hiddenFlow = MutableStateFlow(false)
    val hiddenFlow: StateFlow<Boolean> = _hiddenFlow.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** مقدار فعلی را از دیسک می‌خواند (در provideGlance و HomeScreen صدا زده شود). */
    fun init(context: Context) {
        _hiddenFlow.value = prefs(context).getBoolean(KEY_AMOUNTS_HIDDEN, false)
    }

    fun isHidden(context: Context): Boolean {
        init(context)
        return _hiddenFlow.value
    }

    fun toggle(context: Context) {
        val appContext = context.applicationContext
        val newValue = !prefs(appContext).getBoolean(KEY_AMOUNTS_HIDDEN, false)
        prefs(appContext).edit().putBoolean(KEY_AMOUNTS_HIDDEN, newValue).commit()
        _hiddenFlow.value = newValue
        CoroutineScope(Dispatchers.Default).launch { updateBalanceWidget(appContext) }
    }
}