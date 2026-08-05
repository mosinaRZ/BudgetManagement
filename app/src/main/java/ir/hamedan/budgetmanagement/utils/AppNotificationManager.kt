package ir.hamedan.budgetmanagement.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences

object AppNotificationManager {

    // شناسه‌ی کانال عوض شد (v2) چون NotificationChannel بعد از ساخته‌شدن
    // غیرقابل‌تغییره؛ کاربرانی که نسخه‌ی قبلی رو نصب داشتن با importance
    // قدیمی (DEFAULT) گیر می‌کردن و صرفاً عوض‌کردن این مقدار در کد براشون اثر نمی‌کرد.
    private const val CHANNEL_ID = "budget_channel_v2"
    private const val CHANNEL_NAME = "Budget Notifications"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // برای نمایش پاپ‌آپ (heads-up) لازم است
            ).apply { description = "Budget management alerts" }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendPushIfAllowed(context: Context, titleFa: String, titleEn: String, bodyFa: String, bodyEn: String) {
        // اگر کاربر فقط in-app انتخاب کرده، push نفرست
        if (NotificationPreferences.getMode(context) == NotificationPreferences.MODE_IN_APP) return

        val isPersian = LocaleHelper.getLanguage(context) == "fa"
        val title = if (isPersian) titleFa else titleEn
        val body = if (isPersian) bodyFa else bodyEn

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notificationicon)   // آیکون اعلان — ادامه راهنما
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}