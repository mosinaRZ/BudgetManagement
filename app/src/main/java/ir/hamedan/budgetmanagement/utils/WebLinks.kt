package ir.hamedan.budgetmanagement.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/**
 * The legal and security pages live on the official website (https://cidna.ir), in both languages,
 * so the app never carries its own copy of them: what the user reads in the app is always exactly
 * what the website and the store listings show.
 */
object WebLinks {

    const val SITE = "https://cidna.ir"

    /** Website page slugs (the Persian page is `/slug/`, the English one `/en/slug/`). */
    enum class Page(val slug: String) {
        PRIVACY("privacy"),
        SECURITY("security"),
        TERMS("terms"),
        LICENSES("licenses"),
        ACCOUNT_DELETION("account-deletion")
    }

    fun url(page: Page, isPersian: Boolean): String =
        SITE + (if (isPersian) "" else "/en") + "/" + page.slug + "/"

    /**
     * Opens a website page in a Chrome Custom Tab (it looks like part of the app and returns to it
     * with Back). Without a Custom Tabs browser it opens the normal browser instead.
     */
    fun open(context: Context, page: Page, isPersian: Boolean, darkTheme: Boolean) {
        val uri = Uri.parse(url(page, isPersian))
        val colorScheme = if (darkTheme) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(colorScheme)
            .build()
        if (context !is Activity) customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            customTab.launchUrl(context, uri)
        } catch (e: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_VIEW, uri)
            if (context !is Activity) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(fallback)
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    if (isPersian) "برنامه‌ای برای باز کردن این لینک پیدا نشد" else "No app found to open this link",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}