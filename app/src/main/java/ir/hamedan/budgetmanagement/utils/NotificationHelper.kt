package ir.hamedan.budgetmanagement.utils

import android.content.Context
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
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
        notificationType: NotificationType,
        type: String = "SYSTEM",
        titleFa: String,
        titleEn: String,
        descFa: String,
        descEn: String,
        tag: String = ""
    ) {
        if (tag != "WELCOME" && !NotificationPreferences.isTypeEnabled(context, notificationType)) return

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
            val alreadyExists = repository.countByTag(entity.tag) > 0

            if (!alreadyExists) {
                repository.insert(entity)

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