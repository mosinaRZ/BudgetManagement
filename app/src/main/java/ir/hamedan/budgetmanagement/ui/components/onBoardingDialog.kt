package ir.hamedan.budgetmanagement.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// ---------- محتوای ویژگی‌ها (صفحه ۱) ----------

private data class OnboardingFeature(
    val emoji: String,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String
)

private val onboardingFeatures = listOf(
    OnboardingFeature(
        "🏦", "ثبت خودکار از پیامک بانکی", "Auto-capture from bank SMS",
        "پیامک‌های بانکی رو می‌خونه و تراکنش رو برات پیشنهاد می‌ده تا فقط تأیید کنی.",
        "Reads bank SMS and suggests the transaction for you to confirm."
    ),
    OnboardingFeature(
        "🎯", "اهداف پس‌انداز", "Saving goals",
        "برای هر هدف مبلغ ماهانه تعیین کن و پیشرفتت رو قدم‌به‌قدم ببین.",
        "Set a monthly amount for each goal and track your progress."
    ),
    OnboardingFeature(
        "🤝", "بدهی و طلب", "Debts & credits",
        "بدهی‌ها و طلب‌هات رو با یادآوری سررسید، منظم مدیریت کن.",
        "Keep track of debts and credits with due-date reminders."
    ),
    OnboardingFeature(
        "📊", "گزارش و خروجی", "Reports & export",
        "نمودار درآمد و هزینه بگیر و خروجی Excel و PDF از تراکنش‌هات داشته باش.",
        "View income/expense charts and export to Excel and PDF."
    ),
    OnboardingFeature(
        "🎙️", "ثبت با صدا", "Voice entry",
        "کافیه حرف بزنی؛ تراکنش با گفتار به متن ثبت می‌شه.",
        "Just speak — your transaction gets recorded via voice-to-text."
    )
)

// ---------- محتوای مجوزها (صفحه ۲) ----------

data class OnboardingPermission(
    val key: String, // شناسهٔ پایدار (برای ذخیره‌سازی در سیستم یادآوری)، مستقل از لیست رشته‌های مجوز
    val permissions: List<String>, // یک یا چند رشته Manifest.permission که با هم درخواست می‌شوند
    val emoji: String,
    val titleFa: String,
    val titleEn: String,
    val reasonFa: String,
    val reasonEn: String
)

fun onboardingPermissions(sdkInt: Int): List<OnboardingPermission> = buildList {
    add(
        OnboardingPermission(
            key = "sms",
            permissions = listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
            emoji = "💬",
            titleFa = "پیامک بانکی",
            titleEn = "Bank SMS",
            reasonFa = "برای تشخیص خودکار تراکنش‌های بانکی از پیامک و ثبت سریع‌تر بدون تایپ دستی.",
            reasonEn = "To automatically detect bank transactions from SMS and log them faster."
        )
    )
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        add(
            OnboardingPermission(
                key = "notifications",
                permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
                emoji = "🔔",
                titleFa = "نوتیفیکیشن",
                titleEn = "Notifications",
                reasonFa = "برای اطلاع از واریز/برداشت، یادآوری سررسید بدهی‌ها و پیشرفت اهداف پس‌انداز.",
                reasonEn = "To notify you about deposits/withdrawals, due dates and saving-goal progress."
            )
        )
    }
    add(
        OnboardingPermission(
            key = "mic",
            permissions = listOf(Manifest.permission.RECORD_AUDIO),
            emoji = "🎙️",
            titleFa = "میکروفون",
            titleEn = "Microphone",
            reasonFa = "برای ثبت تراکنش با گفتار به متن، فقط وقتی خودت این گزینه رو انتخاب کنی.",
            reasonEn = "To record a transaction via voice-to-text, only when you choose that option."
        )
    )
}

/**
 * دیالوگ دوصفحه‌ای اولین ورود.
 * با کلیک بیرون یا دکمه Back بسته نمی‌شود؛ فقط با دکمه‌های داخلی خودش پیمایش/بسته می‌شود.
 *
 * @param isPermissionGranted باید وضعیت لحظه‌ای مجوز را برگرداند (مثلاً از طریق ContextCompat.checkSelfPermission)
 * @param onRequestPermissions فراخوانی launcher سیستم برای درخواست مجوزها
 * @param onFinish وقتی کاربر «شروع کن» را می‌زند (فلگ تکمیل را همین‌جا ذخیره کن)
 */
@Composable
fun OnboardingDialog(
    isPersian: Boolean,
    isPermissionGranted: (String) -> Boolean,
    onRequestPermissions: (List<String>) -> Unit,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { /* عمداً خالی؛ با کلیک بیرون بسته نمی‌شود */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // بک‌پرس را خودمان مدیریت می‌کنیم: صفحه دوم → برگرد به اول، صفحه اول → نادیده بگیر (دیالوگ بسته نشود)
        BackHandler(enabled = true) {
            if (pagerState.currentPage > 0) {
                scope.launch { pagerState.animateScrollToPage(0) }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false, // فقط با دکمه جابه‌جا شود
                    modifier = Modifier.wrapContentHeight()
                ) { page ->
                    if (page == 0) WelcomePage(isPersian = isPersian)
                    else PermissionsPage(
                        isPersian = isPersian,
                        isPermissionGranted = isPermissionGranted,
                        onRequestPermissions = onRequestPermissions
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // نشانگر صفحات
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(0) } }) {
                            Text(if (isPersian) "برگشت" else "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage == 0) {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            } else {
                                onFinish()
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (pagerState.currentPage == 0) {
                                if (isPersian) "بعدی" else "Next"
                            } else {
                                if (isPersian) "متوجه شدم، شروع کن" else "Got it, let's start"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(isPersian: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "👋", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isPersian) "خوش اومدی!" else "Welcome!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isPersian)
                "قبل از شروع، یه نگاه سریع به کارایی می‌ندازیم که برات آماده کردیم."
            else
                "Before we start, here's a quick look at what's ready for you.",
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        onboardingFeatures.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = feature.emoji, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) feature.titleFa else feature.titleEn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) feature.descFa else feature.descEn,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    isPersian: Boolean,
    isPermissionGranted: (String) -> Boolean,
    onRequestPermissions: (List<String>) -> Unit
) {
    val permissions = remember { onboardingPermissions(Build.VERSION.SDK_INT) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "🔐", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isPersian) "دسترسی‌های برنامه" else "App permissions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isPersian)
                "این دسترسی‌ها اختیاری‌ان و هر وقت بخوای از تنظیمات گوشی قابل تغییرن."
            else
                "These are optional and can be changed anytime from your phone settings.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        permissions.forEach { perm ->
            val granted = perm.permissions.all { isPermissionGranted(it) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = perm.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) perm.titleFa else perm.titleEn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) perm.reasonFa else perm.reasonEn,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                if (granted) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(if (isPersian) "فعال" else "Granted", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                } else {
                    AssistChip(
                        onClick = { onRequestPermissions(perm.permissions) },
                        label = { Text(if (isPersian) "اجازه دادن" else "Allow", fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}