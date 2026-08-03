package ir.hamedan.budgetmanagement.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.hamedan.budgetmanagement.data.preferences.AppUsagePreferences
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class InactivityReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val thresholds = listOf(3, 7, 15, 30)

    override suspend fun doWork(): Result {
        val lastOpen = AppUsagePreferences.getLastOpen(applicationContext)
        if (lastOpen == 0L) return Result.success()

        val daysInactive = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - lastOpen
        ).toInt()

        val lastNotified = AppUsagePreferences.getLastNotifiedDays(applicationContext)

        // فقط بالاترین thresholdی که رسیده و قبلاً نوتیف ندادیم
        val targetThreshold = thresholds
            .filter { it <= daysInactive && it > lastNotified }
            .maxOrNull()

        if (targetThreshold != null) {
            val (titleFa, titleEn, descFa, descEn) = when (targetThreshold) {
                3 -> Quadruple(
                    "تراکنش جدیدی نداری؟",
                    "No new transactions?",
                    "۳ روزه چیزی ثبت نکردی. یادت نره هزینه‌ها و درآمدهات رو وارد کنی.",
                    "It's been 3 days. Don't forget to record your expenses and income."
                )
                7 -> Quadruple(
                    "فراموش کردی تراکنش‌هات رو ثبت کنی؟",
                    "Forgot to log your transactions?",
                    "یک هفته از آخرین استفاده گذشته. برای مدیریت بهتر بودجه، تراکنش‌هات رو ثبت کن.",
                    "A week has passed. Log your transactions for better budget management."
                )
                15 -> Quadruple(
                    "۱۵ روزه خبری ازت نیست!",
                    "15 days of silence!",
                    "نیم‌ماهه برنامه رو باز نکردی. بودجه‌ات نیاز به توجه داره.",
                    "It's been 15 days. Your budget needs attention."
                )
                else -> Quadruple( // 30
                    "یک ماهه غایبی!",
                    "Missing for a month!",
                    "۳۰ روزه از برنامه استفاده نکردی. برگرد و وضعیت مالیت رو چک کن.",
                    "It's been 30 days. Come back and check your financial status."
                )
            }

            NotificationHelper.send(
                context = applicationContext,
                type = "WARNING",
                titleFa = titleFa,
                titleEn = titleEn,
                descFa = descFa,
                descEn = descEn,
                tag = "INACTIVITY_$targetThreshold"
            )

            AppUsagePreferences.setLastNotifiedDays(applicationContext, targetThreshold)
        }

        return Result.success()
    }
}

// کمک‌کننده ساده
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)