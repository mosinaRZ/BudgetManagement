package ir.hamedan.budgetmanagement.utils

import android.content.Context
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    /**
     * ارسال اعلان درون‌برنامه‌ای + (در صورت فعال بودن) اعلان سیستمی
     */
    fun send(
        context: Context,
        type: String = "SYSTEM",
        titleFa: String,
        titleEn: String,
        descFa: String,
        descEn: String,
        tag: String = ""
    ) {
        val db = AppDatabase.getInstance(context.applicationContext)
        val repository = NotificationRepository(db.notificationDao())

        val entity = NotificationEntity(
            type = type,
            titleFa = titleFa,
            titleEn = titleEn,
            descFa = descFa,
            descEn = descEn,
            tag = tag
        )

        CoroutineScope(Dispatchers.IO).launch {
            // اول در دیتابیس ذخیره می‌شود (اعلان درون‌برنامه‌ای)
            repository.addNotification(entity)

            // بعد در صورت مجاز بودن، اعلان سیستمی هم ارسال می‌شود
            AppNotificationManager.sendPushIfAllowed(
                context = context.applicationContext,
                titleFa = titleFa,
                titleEn = titleEn,
                bodyFa = descFa,
                bodyEn = descEn
            )
        }
    }
}