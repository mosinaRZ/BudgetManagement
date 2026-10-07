package ir.hamedan.budgetmanagement.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// ---------- محتوای معرفی (مرحله ۱ تا ۳) ----------

private data class OnboardingFeature(
    val icon: ImageVector,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String
)

/** امکانات اصلی برنامه؛ هر مورد یک قابلیت واقعی و فعال است. */
private val coreFeatures = listOf(
    OnboardingFeature(
        Icons.Rounded.Sms,
        "ثبت خودکار از پیامک بانکی", "Auto-capture from bank SMS",
        "تراکنش را از پیامک بانک شناسایی می‌کند و برای تأیید شما آماده می‌سازد.",
        "Detects the transaction in your bank SMS and prepares it for you to confirm."
    ),
    OnboardingFeature(
        Icons.Rounded.Mic,
        "ثبت با صدا", "Voice entry",
        "کافی است بگویید؛ تراکنش از روی گفتار شما ثبت می‌شود.",
        "Just speak; the transaction is recorded from what you say."
    ),
    OnboardingFeature(
        Icons.Rounded.Speed,
        "سقف بودجه", "Budget limits",
        "برای هر دسته سقف تعیین کنید و در ۵۰٪، ۸۰٪ و ۱۰۰٪ هشدار بگیرید.",
        "Set a limit per category and get alerts at 50%, 80% and 100%."
    ),
    OnboardingFeature(
        Icons.Rounded.Savings,
        "اهداف پس‌انداز", "Saving goals",
        "مبلغ ماهانه تعیین کنید؛ واریز خودکار و پیشرفت هدف را دنبال کنید.",
        "Choose a monthly amount and follow automatic deposits and progress."
    ),
    OnboardingFeature(
        Icons.Rounded.AccountBalance,
        "بدهی و طلب", "Debts & credits",
        "سررسیدها را با یادآوری ۱، ۳ یا ۷ روز قبل از موعد مدیریت کنید.",
        "Manage due dates with reminders 1, 3 or 7 days ahead."
    ),
    OnboardingFeature(
        Icons.Rounded.BarChart,
        "گزارش و خروجی", "Reports & export",
        "نمودار درآمد و هزینه ببینید و خروجی PDF یا Excel بگیرید.",
        "See income and expense charts and export to PDF or Excel."
    ),
    OnboardingFeature(
        Icons.Rounded.Share,
        "اشتراک‌گذاری تراکنش", "Share a transaction",
        "از هر تراکنش یک تصویر زیبا بسازید و ذخیره یا ارسال کنید.",
        "Turn any transaction into a beautiful image to save or send."
    ),
    OnboardingFeature(
        Icons.Rounded.Widgets,
        "ویجت و میان‌بر", "Widget & shortcut",
        "موجودی را از صفحه اصلی ببینید و تراکنش جدید را با یک میان‌بر ثبت کنید.",
        "See your balance on the home screen and add a transaction from a shortcut."
    )
)

/** امنیت، همگام‌سازی و مدیریت دستگاه‌ها. */
private val trustFeatures = listOf(
    OnboardingFeature(
        Icons.Rounded.Sync,
        "همگام‌سازی رمزنگاری‌شده", "Encrypted sync",
        "اطلاعات مالی شما پیش از ارسال رمزنگاری می‌شود و روی چند دستگاه هماهنگ می‌ماند.",
        "Your financial data is encrypted before it leaves the device and stays in sync across devices."
    ),
    OnboardingFeature(
        Icons.Rounded.Fingerprint,
        "قفل برنامه و ورود بیومتریک", "App lock & biometrics",
        "با اثر انگشت وارد شوید؛ برنامه پس از مدتی دور بودن، خودکار قفل می‌شود.",
        "Sign in with your fingerprint; the app locks itself after a while in the background."
    ),
    OnboardingFeature(
        Icons.Rounded.Devices,
        "دستگاه اصلی و هشدار ورود", "Primary device & sign-in alerts",
        "اولین دستگاه شما «دستگاه اصلی» است و فقط خودش می‌تواند دستگاه‌های دیگر را حذف کند. با ورود یا حذف هر دستگاه، بلافاصله هشدار می‌گیرید.",
        "Your first device is the primary one and the only one that can remove other devices. You're alerted whenever a device signs in or is removed."
    )
)

// ---------- محتوای مجوزها (آخرین مرحله) ----------

data class OnboardingPermission(
    val key: String, // شناسهٔ پایدار (برای ذخیره‌سازی در سیستم یادآوری)، مستقل از لیست رشته‌های مجوز
    val permissions: List<String>, // یک یا چند رشته Manifest.permission که با هم درخواست می‌شوند
    val emoji: String,
    val titleFa: String,
    val titleEn: String,
    val reasonFa: String,
    val reasonEn: String,
    /**
     * آیا این مجوز مستحق یادآوریِ دوره‌ای است؟ مجوزهایی که فقط هنگام استفادهٔ آگاهانه از یک قابلیت لازم‌اند
     * (مثل میکروفون) مزاحم کاربر نمی‌شوند و همان لحظه درخواست می‌شوند.
     */
    val remindable: Boolean = true
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
            reasonEn = "To record a transaction via voice-to-text, only when you choose that option.",
            remindable = false
        )
    )
}

/**
 * راهنمای اولین ورود: چهار مرحلهٔ تمام‌صفحه (خوش‌آمدگویی، امکانات، امنیت، دسترسی‌ها).
 * با کلیک بیرون یا دکمه Back بسته نمی‌شود؛ فقط با دکمه‌های داخلی خودش پیمایش/بسته می‌شود.
 *
 * @param isPermissionGranted باید وضعیت لحظه‌ای مجوز را برگرداند (مثلاً از طریق ContextCompat.checkSelfPermission)
 * @param onRequestPermissions فراخوانی launcher سیستم برای درخواست مجوزها
 * @param onFinish وقتی کاربر «شروع کنید» را می‌زند (فلگ تکمیل را همین‌جا ذخیره کن)
 */
@Composable
fun OnboardingDialog(
    isPersian: Boolean,
    isPermissionGranted: (String) -> Boolean,
    onRequestPermissions: (List<String>) -> Unit,
    onFinish: () -> Unit,
    isPermissionBlocked: (OnboardingPermission) -> Boolean = { false }
) {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pageCount - 1
    val colors = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = { /* عمداً خالی؛ با کلیک بیرون بسته نمی‌شود */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // بک‌پرس را خودمان مدیریت می‌کنیم: مرحلهٔ قبل؛ در مرحلهٔ اول نادیده گرفته می‌شود تا دیالوگ بسته نشود.
        BackHandler(enabled = true) {
            if (pagerState.currentPage > 0) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.primaryContainer.copy(alpha = 0.45f), colors.background)
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {

                    // نوار بالا: شمارندهٔ مرحله + رد کردن
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant
                        )
                        if (!isLast) {
                            TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pageCount - 1) } }) {
                                Text(if (isPersian) "رد کردن" else "Skip")
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> WelcomePage(isPersian)
                            1 -> FeaturesPage(
                                isPersian = isPersian,
                                icon = AppLogoIcon,
                                titleFa = "هر آنچه برای مدیریت پولتان لازم است",
                                titleEn = "Everything you need to manage your money",
                                features = coreFeatures
                            )
                            2 -> FeaturesPage(
                                isPersian = isPersian,
                                icon = Icons.Rounded.Security,
                                titleFa = "امن، همگام و تحت کنترل شما",
                                titleEn = "Secure, in sync and under your control",
                                features = trustFeatures
                            )
                            else -> PermissionsPage(
                                isPersian = isPersian,
                                isPermissionGranted = isPermissionGranted,
                                isPermissionBlocked = isPermissionBlocked,
                                onRequestPermissions = onRequestPermissions
                            )
                        }
                    }

                    // پایین: نشانگر مرحله + دکمه‌ها
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(pageCount) { index ->
                                val selected = pagerState.currentPage == index
                                val dotWidth by animateDpAsState(if (selected) 26.dp else 8.dp, label = "onboardingDot")
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(dotWidth)
                                        .clip(CircleShape)
                                        .background(if (selected) colors.primary else colors.outlineVariant)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (pagerState.currentPage > 0) {
                                TextButton(
                                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                                ) { Text(if (isPersian) "قبلی" else "Back") }
                            }
                            Button(
                                onClick = {
                                    if (isLast) onFinish()
                                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    text = when {
                                        isLast && isPersian -> "شروع کنید"
                                        isLast -> "Let's start"
                                        isPersian -> "ادامه"
                                        else -> "Continue"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- مرحلهٔ ۱: خوش‌آمدگویی ----------

@Composable
private fun WelcomePage(isPersian: Boolean) {
    val colors = MaterialTheme.colorScheme
    val float by rememberInfiniteTransition(label = "heroFloat").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroFloatValue"
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(12.dp))

        // نشان اصلی: دو حلقهٔ محو و یک دایرهٔ گرادیانی که به آرامی بالا و پایین می‌رود
        Box(modifier = Modifier.size(220.dp).offset(y = float.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.07f)))
            Box(Modifier.size(168.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.13f)))
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppLogoIcon,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isPersian) "به سیدنا خوش آمدید" else "Welcome to Cidna",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colors.onSurface
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (isPersian)
                "دستیار شما برای ثبت، پیگیری و کنترل پول؛ ساده، امن و همیشه همراه شما."
            else
                "Your companion to record, track and control your money; simple, secure and always with you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            HighlightChip(Icons.Rounded.Sms, if (isPersian) "ثبت خودکار از پیامک" else "Auto-capture from SMS")
            HighlightChip(Icons.Rounded.Sync, if (isPersian) "همگام‌سازی رمزنگاری‌شده" else "Encrypted sync")
            HighlightChip(Icons.Rounded.Translate, if (isPersian) "فارسی و English" else "Persian & English")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HighlightChip(icon: ImageVector, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.primaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onPrimaryContainer
        )
    }
}

// ---------- مرحله‌های ۲ و ۳: فهرست قابلیت‌ها ----------

@Composable
private fun FeaturesPage(
    isPersian: Boolean,
    icon: ImageVector,
    titleFa: String,
    titleEn: String,
    features: List<OnboardingFeature>
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (isPersian) titleFa else titleEn,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { feature ->
                FeatureCard(
                    icon = feature.icon,
                    title = if (isPersian) feature.titleFa else feature.titleEn,
                    description = if (isPersian) feature.descFa else feature.descEn
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = colors.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.14f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- مرحلهٔ ۴: دسترسی‌ها ----------

internal fun permissionIcon(key: String): ImageVector = when (key) {
    "sms" -> Icons.Rounded.Sms
    "notifications" -> Icons.Rounded.Notifications
    "mic" -> Icons.Rounded.Mic
    else -> Icons.Rounded.Security
}

@Composable
private fun PermissionsPage(
    isPersian: Boolean,
    isPermissionGranted: (String) -> Boolean,
    isPermissionBlocked: (OnboardingPermission) -> Boolean,
    onRequestPermissions: (List<String>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val permissions = remember { onboardingPermissions(Build.VERSION.SDK_INT) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (isPersian) "دسترسی‌های برنامه" else "App permissions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isPersian)
                "این دسترسی‌ها اختیاری‌اند و هر زمان از تنظیمات گوشی قابل تغییرند. اطلاعات پیامک و صدای شما از دستگاه خارج نمی‌شود."
            else
                "These are optional and can be changed anytime in your phone settings. Your SMS and voice never leave the device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            permissions.forEach { perm ->
                val granted = perm.permissions.all { isPermissionGranted(it) }
                PermissionCard(
                    icon = permissionIcon(perm.key),
                    title = if (isPersian) perm.titleFa else perm.titleEn,
                    reason = if (isPersian) perm.reasonFa else perm.reasonEn,
                    granted = granted,
                    grantedLabel = if (isPersian) "فعال" else "Granted",
                    allowLabel = when {
                        isPermissionBlocked(perm) -> if (isPersian) "باز کردن تنظیمات" else "Open settings"
                        else -> if (isPersian) "اجازه دادن" else "Allow"
                    },
                    onAllow = { onRequestPermissions(perm.permissions) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    reason: String,
    granted: Boolean,
    grantedLabel: String,
    allowLabel: String,
    onAllow: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, (if (granted) colors.primary else colors.outline).copy(alpha = if (granted) 0.4f else 0.14f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(text = reason, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            if (granted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = grantedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(allowLabel, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}