package ir.hamedan.budgetmanagement.data.notification

import android.content.Context
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.network.DeviceApi
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns account device activity into user-facing security alerts:
 *  - another device signed in to the account,
 *  - another device was removed from the account,
 *  - this device itself was removed.
 *
 * There is no push channel, so the first two are learned by asking the server for events this
 * device has not seen yet (called after every successful sync). The third is detected when the
 * server rejects this device's session. Alerts go through [NotificationHelper], so they appear
 * in the in-app notification list and, when the app is in the background, as a system
 * notification that opens the Devices screen.
 */
object DeviceSecurityNotifier {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_CURSOR_PREFIX = "device_event_cursor_"

    /** Blocking network call: run it off the main thread. */
    fun pollAndNotify(context: Context) {
        val app = context.applicationContext as BudgetApp
        val userId = app.container.authSessionStore.userId()?.takeIf { it.isNotBlank() } ?: return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_CURSOR_PREFIX + userId
        val cursor = prefs.getLong(key, 0L)

        val page = app.container.deviceApi.events(cursor)
        page.events.forEach { notifyEvent(context.applicationContext, it) }
        // Advance only after the alerts were handed off; duplicates are impossible anyway because
        // every alert is tagged with its event id.
        if (page.cursorMillis > cursor) {
            prefs.edit().putLong(key, page.cursorMillis).apply()
        }
    }

    /** This device was removed from the account by the primary device. */
    fun notifyThisDeviceRemoved(context: Context) {
        NotificationHelper.send(
            context = context.applicationContext,
            notificationType = NotificationType.DEVICE_REMOVED,
            titleFa = "این دستگاه از حساب حذف شد",
            titleEn = "This device was removed from your account",
            descFa = "دسترسی این دستگاه به حساب شما قطع شد. برای استفاده دوباره، با رمز عبور وارد شوید. اگر این کار را شما انجام نداده‌اید، پس از ورود رمز عبور خود را تغییر دهید.",
            descEn = "This device no longer has access to your account. Sign in with your password to use it again. If you didn't do this, change your password after signing in.",
            tag = "DEVICE_REMOVED_SELF_${System.currentTimeMillis()}",
            security = true
        )
    }

    private fun notifyEvent(context: Context, event: DeviceApi.DeviceEvent) {
        val nameFa = event.deviceName.ifBlank { "دستگاه ناشناس" }
        val nameEn = event.deviceName.ifBlank { "Unknown device" }
        val whenFa = whenPhrase(event.createdAtMillis, isPersian = true)
        val whenEn = whenPhrase(event.createdAtMillis, isPersian = false)

        when (event.type) {
            DeviceApi.TYPE_SIGNED_IN -> NotificationHelper.send(
                context = context,
                notificationType = NotificationType.DEVICE_SIGN_IN,
                titleFa = "ورود از دستگاه جدید",
                titleEn = "New device signed in",
                descFa = "«$nameFa»$whenFa وارد حساب شما شد. اگر این شما نبودید، همین حالا رمز عبور خود را تغییر دهید و دستگاه‌های حساب را بررسی کنید.",
                descEn = "\"$nameEn\" signed in to your account$whenEn. If this wasn't you, change your password right away and review your account devices.",
                tag = "DEVICE_EVENT_${event.id}",
                security = true,
                openRoute = AppNotificationManager.ROUTE_DEVICES
            )

            DeviceApi.TYPE_REMOVED -> {
                val by = event.actorName.takeIf { it.isNotBlank() }
                NotificationHelper.send(
                    context = context,
                    notificationType = NotificationType.DEVICE_REMOVED,
                    titleFa = "دستگاه از حساب حذف شد",
                    titleEn = "Device removed from your account",
                    descFa = if (by != null) "«$nameFa» توسط «$by»$whenFa از حساب شما حذف شد."
                    else "«$nameFa»$whenFa از حساب شما حذف شد.",
                    descEn = if (by != null) "\"$nameEn\" was removed from your account by \"$by\"$whenEn."
                    else "\"$nameEn\" was removed from your account$whenEn.",
                    tag = "DEVICE_EVENT_${event.id}",
                    security = true,
                    openRoute = AppNotificationManager.ROUTE_DEVICES
                )
            }
        }
    }

    /** " در تاریخ 1405/07/15 ساعت 14:32" / " on 2026/10/07 at 14:32", or "" when the time is unknown. */
    private fun whenPhrase(millis: Long, isPersian: Boolean): String {
        if (millis <= 0L) return ""
        val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        val date = DateUtils.formatTimestamp(millis, isPersian)
        return if (isPersian) " در تاریخ $date ساعت $time" else " on $date at $time"
    }
}