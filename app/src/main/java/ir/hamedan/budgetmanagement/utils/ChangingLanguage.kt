package ir.hamedan.budgetmanagement.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

fun changeAppLanguage(languageCode: String) {
    // languageCode می‌تواند "fa" یا "en" باشد
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    // اعمال تغییر زبان روی کل ساختار اکتیویتی‌های برنامه
    AppCompatDelegate.setApplicationLocales(appLocale)
}