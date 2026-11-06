package ir.hamedan.budgetmanagement.data.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Opens the page where this app can be updated, in the store the app was installed from
 * (Google Play, Cafe Bazaar or Myket). Nothing here closes or dismisses any screen.
 */
object StoreLauncher {

    private class Store(val key: String, val installerPackage: String, val nativeLink: String)

    private val stores = listOf(
        Store("bazaar", "com.farsitel.bazaar", "bazaar://details?id="),
        Store("myket", "ir.mservices.market", "myket://details?id="),
        Store("play", "com.android.vending", "market://details?id=")
    )

    private const val FALLBACK_URL = "https://cidna.ir"

    /** [updateUrls] are the https pages from the server; true when some page was opened. */
    fun open(context: Context, updateUrls: Map<String, String>): Boolean {
        val store = stores.firstOrNull { it.installerPackage == installerOf(context) }

        // 1) The store's own app, straight on this app's page (works without a browser).
        if (store != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(store.nativeLink + context.packageName))
                .setPackage(store.installerPackage)
            if (start(context, intent)) return true
        }
        // 2) The web page for that store, else the general page (unknown installer, e.g. a direct APK).
        val url = store?.let { updateUrls[it.key] } ?: updateUrls["default"] ?: FALLBACK_URL
        if (start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) return true
        // 3) Last resort: any page we were given.
        return updateUrls.values.any { start(context, Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
    }

    private fun installerOf(context: Context): String? = runCatching {
        val packageManager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()

    private fun start(context: Context, intent: Intent): Boolean = try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
