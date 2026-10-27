package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Shown once after the local database key was lost and the unreadable database had to be reset
 * (see AppDatabase). Silent data loss is the worst outcome, so the user is told what happened and
 * what can still be done.
 */
@Composable
fun DatabaseResetNoticeDialog(isPersian: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isPersian) "داده‌های محلی قابل خواندن نبود" else "Local data could not be read")
        },
        text = {
            Text(
                if (isPersian) {
                    "کلید امنیتی گوشی برای باز کردن اطلاعات ذخیره‌شده روی این دستگاه از بین رفته است " +
                            "(مثلاً بعد از بازنشانی یا اختلال در سیستم) و برنامه ناچار شد پایگاه داده‌ی محلی را از نو بسازد.\n\n" +
                            "• اگر با حساب کاربری وارد بودید، اطلاعات همگام‌شده پس از ورود دوباره از سرور برمی‌گردد.\n" +
                            "• اطلاعات حالت مهمان فقط روی همین دستگاه بود و به‌طور خودکار قابل بازیابی نیست. " +
                            "اگر قبلاً فایل پشتیبان گرفته‌اید، از «تنظیمات ← دریافت اطلاعات و گزارش‌ها ← بازیابی پشتیبان» استفاده کنید.\n\n" +
                            "برای جلوگیری از این اتفاق، گاهی از «ذخیره پشتیبان» در همان بخش استفاده کنید."
                } else {
                    "The phone's security key that protects the data stored on this device was lost " +
                            "(for example after a reset or a system fault), so the app had to recreate its local database.\n\n" +
                            "• If you were signed in, your synced data returns from the server after you sign in again.\n" +
                            "• Guest-workspace data lived only on this device and cannot be recovered automatically. " +
                            "If you made a backup file earlier, use Settings → Export Data & Reports → Restore backup.\n\n" +
                            "To protect yourself in future, use \"Save backup\" in the same section from time to time."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "متوجه شدم" else "Got it") }
        }
    )
}