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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

object NotificationHelper {

    data class InAppNotification(
        val titleFa: String,
        val titleEn: String,
        val bodyFa: String,
        val bodyEn: String
    )

    // Queue foreground notifications so they survive Activity/Compose startup.
    private val _inAppNotifications = Channel<InAppNotification>(Channel.UNLIMITED)
    val inAppNotifications = _inAppNotifications.receiveAsFlow()

    /**
     * Creates the welcome notification after the user's first successful login.
     * The shown-state is stored per account so logging out/in does not show it again.
     */
    fun sendWelcomeIfNeeded(context: Context) {
        val app = context.applicationContext as BudgetApp
        val userId = app.container.authSessionStore.userId()?.takeIf { it.isNotBlank() } ?: return
        val prefs = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val key = "welcome_shown_$userId"

        if (prefs.getBoolean(key, false)) return

        val repository = app.container.notificationRepository
        val tag = "WELCOME_$userId"
        val entity = NotificationEntity(
            type = "SYSTEM",
            titleFa = "خوش آمدید!",
            titleEn = "Welcome!",
            descFa = "به برنامه مدیریت بودجه سیدنا خوش آمدید. امیدواریم تجربه خوبی داشته باشید.",
            descEn = "Welcome to Cidna Budget Management. We hope you have a great experience.",
            tag = tag
        )

        CoroutineScope(Dispatchers.IO).launch {
            if (repository.countByTag(tag) > 0) {
                prefs.edit().putBoolean(key, true).apply()
                return@launch
            }

            repository.insert(entity)
            prefs.edit().putBoolean(key, true).apply()

            publishDelivery(
                context = context.applicationContext,
                titleFa = entity.titleFa,
                titleEn = entity.titleEn,
                bodyFa = entity.descFa,
                bodyEn = entity.descEn
            )
        }
    }

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

                publishDelivery(
                    context = context.applicationContext,
                    titleFa = titleFa,
                    titleEn = titleEn,
                    bodyFa = descFa,
                    bodyEn = descEn
                )
            }
        }
    }

    private fun publishDelivery(
        context: Context,
        titleFa: String,
        titleEn: String,
        bodyFa: String,
        bodyEn: String
    ) {
        val app = context.applicationContext as? BudgetApp
        if (app?.isAppInForeground == true) {
            _inAppNotifications.trySend(
                InAppNotification(
                    titleFa = titleFa,
                    titleEn = titleEn,
                    bodyFa = bodyFa,
                    bodyEn = bodyEn
                )
            )
        } else {
            AppNotificationManager.sendPushIfAllowed(
                context = context.applicationContext,
                titleFa = titleFa,
                titleEn = titleEn,
                bodyFa = bodyFa,
                bodyEn = bodyEn
            )
        }
    }

}