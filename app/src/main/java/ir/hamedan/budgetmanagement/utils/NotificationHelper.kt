package ir.hamedan.budgetmanagement.utils

import android.content.Context
import ir.hamedan.budgetmanagement.BudgetApp
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
        val app = context.applicationContext as BudgetApp
        val repository = app.container.notificationRepository

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
            val wasInserted = repository.addNotification(entity)

            // اعلان سیستمی فقط زمانی ارسال می‌شود که رکورد جدیدی واقعاً ثبت شده باشد؛
            // در غیر این صورت (تگ تکراری) اعلان سیستمی بدون داشتن رکورد متناظر در
            // لیست درون‌برنامه‌ای، به‌طور مکرر ارسال می‌شد (فقط صدا، بدون نمایش در لیست).
            if (wasInserted) {
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
}