package ir.hamedan.budgetmanagement.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.MainActivity
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper

object AppNotificationManager {

    // شناسه‌ی کانال عوض شد (v2) چون NotificationChannel بعد از ساخته‌شدن
    // غیرقابل‌تغییره؛ کاربرانی که نسخه‌ی قبلی رو نصب داشتن با importance
    // قدیمی (DEFAULT) گیر می‌کردن و صرفاً عوض‌کردن این مقدار در کد براشون اثر نمی‌کرد.
    private const val CHANNEL_ID = "budget_channel_v2"
    private const val CHANNEL_NAME = "Budget Notifications"

    // Account-security alerts (new device, device removed) get their own channel so the user can
    // tune them separately in system settings, and so they always interrupt.
    private const val SECURITY_CHANNEL_ID = "budget_security_v1"

    /** Intent extra telling MainActivity which screen a tapped notification should open. */
    const val EXTRA_OPEN_ROUTE = "open_route"
    const val ROUTE_DEVICES = "devices"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // برای نمایش پاپ‌آپ (heads-up) لازم است
            ).apply { description = "Budget management alerts" }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val security = NotificationChannel(
                SECURITY_CHANNEL_ID,
                context.getString(R.string.notification_channel_security),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notification_channel_security_desc) }
            manager.createNotificationChannel(security)
        }
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancelAll()
    }

    fun sendPushIfAllowed(
        context: Context,
        titleFa: String,
        titleEn: String,
        bodyFa: String,
        bodyEn: String,
        security: Boolean = false,
        openRoute: String? = null
    ) {
        val app = context.applicationContext as? BudgetApp

        // Final delivery gate: foreground notifications never reach the
        // Android NotificationManager.
        if (app?.isAppInForeground == true) {
            cancelAll(context.applicationContext)
            return
        }

        // اگر کاربر فقط in-app انتخاب کرده، push نفرست.
        if (NotificationPreferences.getMode(context) == NotificationPreferences.MODE_IN_APP) return

        val isPersian = LocaleHelper.getLanguage(context) == "fa"
        val title = if (isPersian) titleFa else titleEn
        val body = if (isPersian) bodyFa else bodyEn

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }

        // Tapping a notification opens the app (and, for some alerts, a specific screen).
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            openRoute?.let { putExtra(EXTRA_OPEN_ROUTE, it) }
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            (openRoute ?: "").hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, if (security) SECURITY_CHANNEL_ID else CHANNEL_ID)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        if (security) {
            builder
                .setSmallIcon(R.drawable.ic_notification_security)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        } else {
            builder
                .setSmallIcon(R.mipmap.icon)   // آیکون اعلان — ادامه راهنما
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }
        val notification = builder.build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}