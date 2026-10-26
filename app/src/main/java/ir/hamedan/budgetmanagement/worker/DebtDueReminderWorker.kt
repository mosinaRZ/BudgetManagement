package ir.hamedan.budgetmanagement.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.notification.AppNotificationManager
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.data.preferences.AmountPrivacyPreferences
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.preferences.DebtDueReminderState
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.DebtDueReminders
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Reminds the user 7, 3 and 1 day(s) before a debt / receivable is due (one-time items and
 * installment plans alike), even when the app is closed.
 *
 * Runs twice a day (see [BudgetApp]). The rules live in [DebtDueReminders]; the step that was
 * already delivered for each item and due date is remembered in [DebtDueReminderState], so a
 * reminder is sent once, a missed step is caught up on the next run, and nothing is ever
 * repeated.
 */
class DebtDueReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        checkDueDates()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        // A reminder is optional: log it and let the next scheduled run try again.
        Log.e(TAG, "Due-date reminder check failed", e)
        Result.success()
    }

    private suspend fun checkDueDates(): Result {
        val context = applicationContext

        // Nothing is marked as delivered while the user has this reminder turned off.
        if (!NotificationPreferences.isTypeEnabled(context, NotificationType.DEBT_DUE_REMINDER)) {
            return Result.success()
        }

        // Never wake the user at night; the next run (at most 12 hours later) falls inside the window.
        val now = ZonedDateTime.now()
        if (now.hour !in FIRST_ALLOWED_HOUR..LAST_ALLOWED_HOUR) return Result.success()

        val app = context as? BudgetApp ?: return Result.success()
        val items = app.container.debtCreditRepository.getAllDebtCredits().first()
        val today = now.toLocalDate()
        val zone = now.zone

        val showAmounts = !AmountPrivacyPreferences.isHidden(context)
        val liveKeys = HashSet<String>()

        for (item in items) {
            val info = DebtDueReminders.dueInfo(item, today, zone) ?: continue
            val key = DebtDueReminderState.key(item.id, info.dueDate)
            liveKeys += key

            val step = DebtDueReminders.stepFor(info.daysLeft) ?: continue
            if (!DebtDueReminders.shouldSend(step, DebtDueReminderState.lastSentStep(context, key))) continue

            sendReminder(item, info, step, showAmounts, zone)
            DebtDueReminderState.markSent(context, key, step)
        }

        // An empty list can be a momentary read; never wipe the delivery history because of it.
        if (items.isNotEmpty()) DebtDueReminderState.retainOnly(context, liveKeys)
        return Result.success()
    }

    private fun sendReminder(
        item: DebtCreditEntity,
        info: DebtDueReminders.DueInfo,
        step: Int,
        showAmounts: Boolean,
        zone: ZoneId
    ) {
        val context = applicationContext
        val isDebt = item.type == "DEBT"
        val days = info.daysLeft

        val dateMillis = info.dueDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val dateFa = toPersianDigits(DateUtils.formatTimestamp(dateMillis, true))
        val dateEn = DateUtils.formatTimestamp(dateMillis, false)

        val amount = amountText(DebtDueReminders.amountDue(item), isPersian = true) to
                amountText(DebtDueReminders.amountDue(item), isPersian = false)

        val whenFa = when (days) {
            0 -> "امروز"
            1 -> "فردا"
            else -> "${toPersianDigits(days.toString())} روز دیگر"
        }
        val whenEn = when (days) {
            0 -> "today"
            1 -> "tomorrow"
            else -> "in $days days"
        }
        val name = item.personName

        val titleFa = when {
            days == 0 -> if (isDebt) "امروز سررسید بدهی است" else "امروز سررسید طلب است"
            days == 1 -> if (isDebt) "فردا سررسید بدهی است" else "فردا سررسید طلب است"
            else -> if (isDebt) "سررسید بدهی نزدیک است" else "سررسید طلب نزدیک است"
        }
        val titleEn = when {
            days == 0 -> if (isDebt) "Debt due today" else "Receivable due today"
            days == 1 -> if (isDebt) "Debt due tomorrow" else "Receivable due tomorrow"
            else -> if (isDebt) "Debt due soon" else "Receivable due soon"
        }

        val subjectFa = when {
            item.isMonthly && isDebt -> "قسط بدهی به «$name»"
            item.isMonthly -> "قسط دریافتی از «$name»"
            isDebt -> "بدهی به «$name»"
            else -> "طلب شما از «$name»"
        }
        val subjectEn = when {
            item.isMonthly && isDebt -> "Your installment to $name"
            item.isMonthly -> "Your installment from $name"
            isDebt -> "Your debt to $name"
            else -> "What $name owes you"
        }
        val amountLabelFa = if (item.isMonthly) "مبلغ قسط" else "باقی\u200cمانده"
        val amountLabelEn = if (item.isMonthly) "Installment" else "Remaining"

        val descFa = buildString {
            append("$subjectFa $whenFa ($dateFa) سررسید می\u200cشود.")
            if (showAmounts) append(" $amountLabelFa: ${amount.first}.")
        }
        val descEn = buildString {
            append("$subjectEn is due $whenEn ($dateEn).")
            if (showAmounts) append(" $amountLabelEn: ${amount.second}.")
        }

        NotificationHelper.send(
            context = context,
            notificationType = NotificationType.DEBT_DUE_REMINDER,
            type = "WARNING",
            titleFa = titleFa,
            titleEn = titleEn,
            descFa = descFa,
            descEn = descEn,
            // One notification per item, due date and step.
            tag = "DEBT_DUE_${item.id}_${info.dueDate}_$step",
            openRoute = AppNotificationManager.ROUTE_DEBT
        )
    }

    /** Stored amounts are Toman; the display currency may be Rial (x10), exactly like the rest of the app. */
    private fun amountText(tomanAmount: Long, isPersian: Boolean): String {
        val context = applicationContext
        val inRial = CurrencySharedPreferences.getCurrency(context) == "IRR"
        val value = if (inRial) tomanAmount * 10L else tomanAmount
        val formatter = NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
        val unit = if (isPersian) (if (inRial) "ریال" else "تومان") else (if (inRial) "Rial" else "Toman")
        return "${formatter.format(value)} $unit"
    }

    private fun toPersianDigits(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in '0'..'9') '۰' + (c - '0') else c)
    }

    companion object {
        private const val TAG = "DebtDueReminderWorker"

        /** Reminders are only delivered between 08:00 and 21:59 local time. */
        private const val FIRST_ALLOWED_HOUR = 8
        private const val LAST_ALLOWED_HOUR = 21

        const val UNIQUE_WORK_NAME = "debt_due_reminder"
    }
}