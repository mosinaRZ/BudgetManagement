package ir.hamedan.budgetmanagement.data.notification

import android.content.Context
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NotificationHelper {

    data class InAppNotification(
        val titleFa: String,
        val titleEn: String,
        val bodyFa: String,
        val bodyEn: String,
        // Unique per notification, so two identical messages in a row still count as two events.
        val id: Long = System.nanoTime()
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
            descFa = "به سیدنا خوش آمدید. امیدواریم تجربه خوبی داشته باشید.",
            descEn = "Welcome to Cidna. We hope you have a great experience.",
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
     *
     * Fire-and-forget: returns immediately and does the work on a background scope. Fine for UI
     * code (ViewModels, screens). Do NOT use it from a Worker — the worker would finish before the
     * notification is stored/shown and the process may be killed in between; use [sendAndAwait].
     */
    fun send(
        context: Context,
        notificationType: NotificationType,
        type: String = "SYSTEM",
        titleFa: String,
        titleEn: String,
        descFa: String,
        descEn: String,
        tag: String = "",
        /** Marks account-security alerts: they use the security channel and icon. */
        security: Boolean = false,
        /** Screen a tapped system notification should open (see AppNotificationManager.ROUTE_*). */
        openRoute: String? = null
    ) {
        if (tag != "WELCOME" && !NotificationPreferences.isTypeEnabled(context, notificationType)) return

        CoroutineScope(Dispatchers.IO).launch {
            deliver(context, type, titleFa, titleEn, descFa, descEn, tag, security, openRoute)
        }
    }

    /**
     * Same as [send], but suspends until the notification has been stored (and handed to the
     * system / in-app queue). Workers must use this so that the work is finished before they
     * report a result, and so that they only record an item as "delivered" when it really was.
     *
     * @return `true` if the notification is in the app's notification list when this returns
     *         (newly stored, or already there from an earlier run with the same [tag]);
     *         `false` if the user has turned this notification type off.
     * @throws Exception when storing fails (e.g. database error), so the caller can retry later
     *         instead of treating the item as delivered.
     */
    suspend fun sendAndAwait(
        context: Context,
        notificationType: NotificationType,
        type: String = "SYSTEM",
        titleFa: String,
        titleEn: String,
        descFa: String,
        descEn: String,
        tag: String = "",
        security: Boolean = false,
        openRoute: String? = null
    ): Boolean {
        if (tag != "WELCOME" && !NotificationPreferences.isTypeEnabled(context, notificationType)) return false

        withContext(Dispatchers.IO) {
            deliver(context, type, titleFa, titleEn, descFa, descEn, tag, security, openRoute)
        }
        return true
    }

    private suspend fun deliver(
        context: Context,
        type: String,
        titleFa: String,
        titleEn: String,
        descFa: String,
        descEn: String,
        tag: String,
        security: Boolean,
        openRoute: String?
    ) {
        val app = context.applicationContext as BudgetApp
        val repository = app.container.notificationRepository

        if (repository.countByTag(tag) > 0) return

        repository.insert(
            NotificationEntity(
                type = type,
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = tag
            )
        )

        publishDelivery(
            context = context.applicationContext,
            titleFa = titleFa,
            titleEn = titleEn,
            bodyFa = descFa,
            bodyEn = descEn,
            security = security,
            openRoute = openRoute
        )
    }

    private fun publishDelivery(
        context: Context,
        titleFa: String,
        titleEn: String,
        bodyFa: String,
        bodyEn: String,
        security: Boolean = false,
        openRoute: String? = null
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
                bodyEn = bodyEn,
                security = security,
                openRoute = openRoute
            )
        }
    }

}