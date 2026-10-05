package ir.hamedan.budgetmanagement.worker

import android.content.Context
import android.icu.util.IslamicCalendar
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProfileOccasionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BudgetApp
        if (!app.container.authRepository.isAuthenticated()) return Result.success()

        val user = app.container.database.userDao().getLoggedInUser() ?: return Result.success()
        val today = LocalDate.now(ZoneId.systemDefault())
        val dateKey = today.format(DateTimeFormatter.ISO_DATE)

        checkBirthday(user, today, dateKey)
        checkGenderOccasions(user, today, dateKey)
        return Result.success()
    }

    private fun checkBirthday(user: UserEntity, today: LocalDate, dateKey: String) {
        val birth = user.birthDate ?: return
        val parsed = runCatching { LocalDate.parse(birth) }.getOrNull() ?: return
        if (parsed.monthValue != today.monthValue || parsed.dayOfMonth != today.dayOfMonth) return

        val name = user.firstName.ifBlank { user.fullName.ifBlank { "دوست خوبم" } }
        NotificationHelper.send(
            context = applicationContext,
            notificationType = NotificationType.PROFILE_OCCASION,
            type = "PROFILE",
            titleFa = "تولدت مبارک، $name! 🎂",
            titleEn = "Happy birthday, $name! 🎂",
            descFa = "امیدواریم سال جدید زندگی‌ات پر از آرامش، موفقیت و تصمیم‌های مالی خوب باشد.",
            descEn = "Wishing you a year full of peace, success, and great financial decisions.",
            tag = "BIRTHDAY_${user.id}_$dateKey"
        )
    }

    private fun checkGenderOccasions(user: UserEntity, today: LocalDate, dateKey: String) {
        val gender = user.gender
        if (gender != "male" && gender != "female") return

        // International observances:
        // Girl: 11 October (UN International Day of the Girl Child).
        // Boys/men: 19 November (International Men's Day) and 16 May (widely observed
        // as International Boys Day; it is not a UN-designated day).
        if (gender == "female" && today.monthValue == 10 && today.dayOfMonth == 11) {
            sendGender(
                dateKey, user,
                "روز جهانی دختر مبارک! 🌷",
                "Happy International Day of the Girl! 🌷",
                "امروز روز جهانی دختر است؛ برای رویاها، رشد و آینده‌ای که خودت می‌سازی.",
                "Today celebrates girls and the future they build through growth, dreams and confidence."
            )
        }

        if (gender == "male" && today.monthValue == 5 && today.dayOfMonth == 16) {
            sendGender(
                dateKey, user,
                "روز جهانی پسر مبارک! ✨",
                "Happy International Boys Day! ✨",
                "امروز بهانه‌ای است برای جشن گرفتن رشد، رویاها و مسیر منحصربه‌فرد تو.",
                "Today is a chance to celebrate your growth, dreams and unique path."
            )
        }

        if (gender == "female" && today.monthValue == 3 && today.dayOfMonth == 8) {
            sendGender(
                dateKey, user,
                "روز جهانی زن مبارک! 🌸",
                "Happy International Women's Day! 🌸",
                "امروز را به افتخار قدرت، تلاش و دستاوردهای تو جشن می‌گیریم.",
                "Today celebrates your strength, effort and achievements."
            )
        }

        if (gender == "male" && today.monthValue == 11 && today.dayOfMonth == 19) {
            sendGender(
                dateKey, user,
                "روز جهانی مرد مبارک! 💚",
                "Happy International Men's Day! 💚",
                "امروز فرصتی برای قدردانی از رشد، مسئولیت‌پذیری و نقش مثبت توست.",
                "Today is a chance to celebrate growth, responsibility and the positive impact you make."
            )
        }

        val islamic = IslamicCalendar.getInstance()
        islamic.timeInMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val month = islamic.get(IslamicCalendar.MONTH)
        val day = islamic.get(IslamicCalendar.DAY_OF_MONTH)

        // Iran/Islamic occasions:
        // 1 Dhu al-Qadah: birth anniversary of Lady Fatima al-Ma'suma, observed as Girl's Day.
        if (gender == "female" && month == 10 && day == 1) {
            sendGender(
                dateKey, user,
                "روز دختر مبارک! 🌷",
                "Happy Girl's Day! 🌷",
                "روز دختر، بهانه‌ای زیبا برای آرزو کردن بهترین‌ها برای تو.",
                "A beautiful day to celebrate you and wish you the very best."
            )
        }

        // 10 Rajab: birth anniversary of Imam Muhammad al-Jawad, observed in Iran as Boy's Day.
        if (gender == "male" && month == 6 && day == 10) {
            sendGender(
                dateKey, user,
                "روز پسر مبارک! 💚",
                "Happy Boy's Day! 💚",
                "امروز روز پسر است؛ امیدواریم مسیر رشد و آینده‌ات پر از موفقیت باشد.",
                "It's Boy's Day. Wishing you a future filled with growth and success."
            )
        }
    }

    private fun sendGender(
        dateKey: String,
        user: UserEntity,
        titleFa: String,
        titleEn: String,
        bodyFa: String,
        bodyEn: String
    ) {
        NotificationHelper.send(
            context = applicationContext,
            notificationType = NotificationType.PROFILE_OCCASION,
            type = "PROFILE",
            titleFa = titleFa,
            titleEn = titleEn,
            descFa = bodyFa,
            descEn = bodyEn,
            tag = "PROFILE_OCCASION_${user.id}_${dateKey}_${titleEn.hashCode()}"
        )
    }
}