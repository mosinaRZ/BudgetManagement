package ir.hamedan.budgetmanagement.platform.locale

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import ir.hamedan.budgetmanagement.MainActivity
import ir.hamedan.budgetmanagement.R
import java.util.Locale

/**
 * میان‌برهای لانچر (نگه‌داشتن انگشت روی آیکن برنامه).
 *
 * میان‌برهای استاتیک (shortcuts.xml) برچسب‌شان را خودِ لانچر و با «زبان سیستم» از resourceها
 * می‌خواند، نه با زبان داخل برنامه؛ برای همین با تغییر زبان برنامه همان انگلیسی/فارسیِ سیستم
 * می‌ماندند. اینجا میان‌برها داینامیک ساخته می‌شوند و برچسب‌ها با زبان انتخاب‌شده در برنامه
 * (LocaleHelper) خوانده می‌شوند. باید در شروع برنامه و بعد از هر تغییر زبان صدا زده شود.
 */
object ShortcutsHelper {

    fun update(context: Context) {
        runCatching {
            val appContext = context.applicationContext
            val lang = LocaleHelper.getLanguage(appContext)
            val config = Configuration(appContext.resources.configuration).apply {
                setLocale(Locale(lang))
            }
            val localized = appContext.createConfigurationContext(config)

            fun build(
                id: String,
                action: String,
                iconRes: Int,
                shortRes: Int,
                longRes: Int,
            ): ShortcutInfoCompat =
                ShortcutInfoCompat.Builder(appContext, id)
                    .setShortLabel(localized.getString(shortRes))
                    .setLongLabel(localized.getString(longRes))
                    .setIcon(IconCompat.createWithResource(appContext, iconRes))
                    .setIntent(
                        Intent(appContext, MainActivity::class.java).setAction(action)
                    )
                    .build()

            val shortcuts = listOf(
                build(
                    "add_transaction", MainActivity.ACTION_ADD_TRANSACTION,
                    R.drawable.ic_shortcut_add_transaction,
                    R.string.shortcut_add_transaction_short, R.string.shortcut_add_transaction_long
                ),
                build(
                    "open_goals", MainActivity.ACTION_OPEN_GOALS,
                    R.drawable.ic_shortcut_goals,
                    R.string.shortcut_goals_short, R.string.shortcut_goals_long
                ),
                build(
                    "open_debt", MainActivity.ACTION_OPEN_DEBT,
                    R.drawable.ic_shortcut_debt,
                    R.string.shortcut_debt_short, R.string.shortcut_debt_long
                ),
                build(
                    "open_limits", MainActivity.ACTION_OPEN_LIMITS,
                    R.drawable.ic_shortcut_limits,
                    R.string.shortcut_limits_short, R.string.shortcut_limits_long
                ),
            )
            ShortcutManagerCompat.setDynamicShortcuts(appContext, shortcuts)
        }.onFailure { android.util.Log.w("ShortcutsHelper", "Could not update shortcuts", it) }
    }
}