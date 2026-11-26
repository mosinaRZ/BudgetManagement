package ir.hamedan.budgetmanagement.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.ui.screens.settings.SettingsAccent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════════════
// راهنمای اولین ورود
//
// چهار مرحله‌ی تمام‌صفحه روی پس‌زمینه‌ی «شفق» برنامه:
//  ۱) خوش‌آمدگویی: لوگو در مرکز و آیکون‌های اصلی برنامه که به‌آرامی دورش می‌چرخند
//  ۲) امکانات: شبکه‌ی کارت‌های شیشه‌ای با همان آیکون‌های صفحه‌ی تنظیمات و بقیه‌ی برنامه
//  ۳) امنیت: فهرست قابلیت‌های امنیتی و همگام‌سازی
//  ۴) دسترسی‌ها
// نوار پیشرفتِ بالا مثل استوری‌ها با کشیدن صفحه پر می‌شود، صفحه‌ها هنگام جابه‌جایی محو و کوچک
// می‌شوند و کارت‌ها پله‌پله وارد می‌شوند. همه‌ی انیمیشن‌های بی‌پایان با «حذف انیمیشن‌ها»ی سیستم خاموش می‌شوند.
// ═════════════════════════════════════════════════════════════════════════════

// ---------- محتوای معرفی ----------

private data class OnboardingFeature(
    val accent: SettingsAccent,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null
)

/** امکانات اصلی برنامه؛ آیکون‌ها همان‌هایی‌اند که در تنظیمات و صفحه‌ی افزودن استفاده شده‌اند. */
private val coreFeatures = listOf(
    OnboardingFeature(
        accent = SettingsAccent.ROSE, icon = Icons.Rounded.MicNone,
        titleFa = "ثبت با صدا", titleEn = "Voice entry",
        descFa = "کافی است بگویید؛ تراکنش از روی گفتار شما ثبت می‌شود.",
        descEn = "Just speak; the transaction is recorded from what you say."
    ),
    OnboardingFeature(
        accent = SettingsAccent.TEAL, iconRes = R.drawable.ic_category,
        titleFa = "دسته‌بندی هوشمند", titleEn = "Smart categories",
        descFa = "با نوشتن عنوان، دسته‌ی مناسب پیشنهاد می‌شود؛ دسته‌ها را هم با آیکون دلخواه خودتان بسازید.",
        descEn = "A fitting category is suggested as you type; create your own with a custom icon."
    ),
    OnboardingFeature(
        accent = SettingsAccent.AMBER, iconRes = R.drawable.ic_limit,
        titleFa = "سقف بودجه", titleEn = "Budget limits",
        descFa = "برای هر دسته سقف تعیین کنید و در ۵۰٪، ۸۰٪ و ۱۰۰٪ هشدار بگیرید.",
        descEn = "Set a limit per category and get alerts at 50%, 80% and 100%."
    ),
    OnboardingFeature(
        accent = SettingsAccent.VIOLET, iconRes = R.drawable.ic_goal,
        titleFa = "اهداف پس‌انداز", titleEn = "Saving goals",
        descFa = "مبلغ ماهانه تعیین کنید؛ واریز خودکار و پیشرفت هدف را دنبال کنید.",
        descEn = "Choose a monthly amount and follow automatic deposits and progress."
    ),
    OnboardingFeature(
        accent = SettingsAccent.INDIGO, iconRes = R.drawable.ic_debtcredit,
        titleFa = "بدهی و طلب", titleEn = "Debts & credits",
        descFa = "سررسیدها را با یادآوری ۱، ۳ یا ۷ روز قبل از موعد مدیریت کنید.",
        descEn = "Manage due dates with reminders 1, 3 or 7 days ahead."
    ),
    OnboardingFeature(
        accent = SettingsAccent.SKY, icon = Icons.Rounded.BarChart,
        titleFa = "گزارش و خروجی", titleEn = "Reports & export",
        descFa = "نمودار درآمد و هزینه ببینید و خروجی PDF یا Excel بگیرید.",
        descEn = "See income and expense charts and export to PDF or Excel."
    ),
    OnboardingFeature(
        accent = SettingsAccent.ROSE, icon = Icons.Rounded.Share,
        titleFa = "اشتراک‌گذاری تراکنش", titleEn = "Share a transaction",
        descFa = "از هر تراکنش یک تصویر زیبا بسازید و ذخیره یا ارسال کنید.",
        descEn = "Turn any transaction into a beautiful image to save or send."
    ),
    OnboardingFeature(
        accent = SettingsAccent.SLATE, iconRes = R.drawable.ic_widget,
        titleFa = "ویجت و میان‌بر", titleEn = "Widget & shortcut",
        descFa = "موجودی را از صفحه اصلی ببینید و تراکنش جدید را با یک میان‌بر ثبت کنید.",
        descEn = "See your balance on the home screen and add a transaction from a shortcut."
    )
)

/** امنیت، همگام‌سازی و مدیریت دستگاه‌ها. */
private val trustFeatures = listOf(
    OnboardingFeature(
        accent = SettingsAccent.SKY, icon = Icons.Rounded.CloudSync,
        titleFa = "همگام‌سازی رمزنگاری‌شده", titleEn = "Encrypted sync",
        descFa = "اطلاعات مالی شما پیش از ارسال رمزنگاری می‌شود و روی چند دستگاه هماهنگ می‌ماند.",
        descEn = "Your financial data is encrypted before it leaves the device and stays in sync across devices."
    ),
    OnboardingFeature(
        accent = SettingsAccent.TEAL, icon = Icons.Rounded.Fingerprint,
        titleFa = "قفل برنامه و ورود بیومتریک", titleEn = "App lock & biometrics",
        descFa = "با اثر انگشت وارد شوید؛ برنامه پس از مدتی دور بودن، خودکار قفل می‌شود.",
        descEn = "Sign in with your fingerprint; the app locks itself after a while in the background."
    ),
    OnboardingFeature(
        accent = SettingsAccent.INDIGO, icon = Icons.Rounded.Devices,
        titleFa = "دستگاه اصلی و هشدار ورود", titleEn = "Primary device & sign-in alerts",
        descFa = "اولین دستگاه شما «دستگاه اصلی» است و فقط خودش می‌تواند دستگاه‌های دیگر را حذف کند. با ورود یا حذف هر دستگاه، بلافاصله هشدار می‌گیرید.",
        descEn = "Your first device is the primary one and the only one that can remove other devices. You're alerted whenever a device signs in or is removed."
    ),
    OnboardingFeature(
        accent = SettingsAccent.VIOLET, icon = Icons.Rounded.VisibilityOff,
        titleFa = "محافظت از محتوای صفحه", titleEn = "Screen content protection",
        descFa = "در تنظیمات می‌توانید اسکرین‌شات را مسدود و اطلاعات را در فهرست برنامه‌های اخیر پنهان کنید.",
        descEn = "In Settings you can block screenshots and hide your data in the Recents list."
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
     * مزاحم کاربر نمی‌شوند و همان لحظه درخواست می‌شوند.
     */
    val remindable: Boolean = true
)

fun onboardingPermissions(sdkInt: Int): List<OnboardingPermission> = buildList {
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

        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackground()

            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                OnboardingTopBar(
                    pagerState = pagerState,
                    pageCount = pageCount,
                    isLast = isLast,
                    isPersian = isPersian,
                    onSkip = { scope.launch { pagerState.animateScrollToPage(pageCount - 1) } }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    val active = pagerState.currentPage == page
                    Box(modifier = Modifier.fillMaxSize().pageTransition(pagerState, page)) {
                        when (page) {
                            0 -> WelcomePage(isPersian = isPersian, active = active)
                            1 -> FeaturesPage(
                                isPersian = isPersian,
                                active = active,
                                eyebrowFa = "امکانات", eyebrowEn = "Features",
                                titleFa = "هر آنچه برای مدیریت پولتان لازم است",
                                titleEn = "Everything you need to manage your money",
                                headerIcon = null,
                                features = coreFeatures,
                                asGrid = true
                            )
                            2 -> FeaturesPage(
                                isPersian = isPersian,
                                active = active,
                                eyebrowFa = "امنیت", eyebrowEn = "Security",
                                titleFa = "امن، همگام و تحت کنترل شما",
                                titleEn = "Secure, in sync and under your control",
                                headerIcon = Icons.Rounded.Security,
                                features = trustFeatures,
                                asGrid = false
                            )
                            else -> PermissionsPage(
                                isPersian = isPersian,
                                active = active,
                                isPermissionGranted = isPermissionGranted,
                                isPermissionBlocked = isPermissionBlocked,
                                onRequestPermissions = onRequestPermissions
                            )
                        }
                    }
                }

                OnboardingBottomBar(
                    isPersian = isPersian,
                    currentPage = pagerState.currentPage,
                    isLast = isLast,
                    onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    onNext = {
                        if (isLast) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                )
            }
        }
    }
}

// ---------- ابزارهای مشترک ----------

/** صفحه هنگام کشیدن کمی محو و کوچک می‌شود تا جابه‌جایی نرم و عمق‌دار دیده شود. */
private fun Modifier.pageTransition(state: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = (state.currentPage - page) + state.currentPageOffsetFraction
    val distance = abs(offset).coerceIn(0f, 1f)
    alpha = 1f - distance * 0.7f
    val scale = 1f - distance * 0.06f
    scaleX = scale
    scaleY = scale
}

/** ورود پله‌ای: وقتی صفحه فعال می‌شود هر مورد با کمی تأخیر نسبت به قبلی از پایین بالا می‌آید و ظاهر می‌شود. */
@Composable
private fun Stagger(
    index: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = if (active) 90 + index * 70 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "onboardingStagger"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 36.dp.toPx()
        }
    ) { content() }
}

/** سطح شیشه‌ایِ هم‌زبان با کارت‌های تنظیمات. */
@Composable
private fun Modifier.glassCard(shape: RoundedCornerShape = RoundedCornerShape(24.dp)): Modifier {
    val colors = MaterialTheme.colorScheme
    return this
        .background(
            Brush.linearGradient(
                listOf(colors.surfaceVariant.copy(alpha = 0.62f), colors.surfaceVariant.copy(alpha = 0.40f))
            ),
            shape
        )
        .border(1.dp, colors.outline.copy(alpha = 0.16f), shape)
        .clip(shape)
}

/** کاشی آیکون با رنگ تأکید؛ هم آیکون برداری و هم آیکون اختصاصی برنامه (drawable) را می‌پذیرد. */
@Composable
private fun OnbIconTile(
    feature: OnboardingFeature,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    OnbIconTile(
        icon = feature.icon,
        iconRes = feature.iconRes,
        tint = feature.accent.color(),
        size = size,
        modifier = modifier
    )
}

@Composable
private fun OnbIconTile(
    icon: ImageVector?,
    iconRes: Int?,
    tint: Color,
    size: Int,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((size * 0.3f).dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .background(tint.copy(alpha = 0.15f), shape)
            .border(1.dp, tint.copy(alpha = 0.26f), shape),
        contentAlignment = Alignment.Center
    ) {
        val iconSize = (size * 0.5f).dp
        when {
            iconRes != null -> Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
            icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun EyebrowPill(text: String) {
    val colors = MaterialTheme.colorScheme
    val tint = SettingsAccent.TEAL.color()
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = tint,
        modifier = Modifier
            .background(tint.copy(alpha = 0.13f), CircleShape)
            .border(1.dp, tint.copy(alpha = 0.26f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

// ---------- نوار بالا و پایین ----------

@Composable
private fun OnboardingTopBar(
    pagerState: PagerState,
    pageCount: Int,
    isLast: Boolean,
    isPersian: Boolean,
    onSkip: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // نوار پیشرفت استوری‌مانند: هر بخش با کشیدن صفحه به‌تدریج پر می‌شود.
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
            repeat(pageCount) { index ->
                val fill = (position - index + 1f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(colors.onSurface.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(colors.primary, colors.secondary)))
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isLast,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row {
                Spacer(Modifier.width(14.dp))
                Text(
                    text = if (isPersian) "رد کردن" else "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, colors.outline.copy(alpha = 0.16f), CircleShape)
                        .clickable(role = Role.Button, onClick = onSkip)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    isPersian: Boolean,
    currentPage: Int,
    isLast: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val buttonShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = currentPage > 0,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(buttonShape)
                    .background(colors.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, colors.outline.copy(alpha = 0.16f), buttonShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = if (isPersian) "قبلی" else "Back",
                    tint = colors.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .shadow(elevation = 10.dp, shape = buttonShape, ambientColor = colors.primary, spotColor = colors.primary)
                .clip(buttonShape)
                .background(Brush.horizontalGradient(listOf(colors.primary, colors.secondary)))
                .clickable(role = Role.Button, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = isLast, label = "onboardingCta") { last ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            last && isPersian -> "شروع کنید"
                            last -> "Let's start"
                            isPersian -> "ادامه"
                            else -> "Continue"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ---------- مرحلهٔ ۱: خوش‌آمدگویی ----------

@Composable
private fun WelcomePage(isPersian: Boolean, active: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Stagger(index = 0, active = active) { OrbitHero() }

        Spacer(Modifier.height(20.dp))
        Stagger(index = 1, active = active) {
            Text(
                text = if (isPersian) "به سیدنا خوش آمدید" else "Welcome to Cidna",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )
        }
        Spacer(Modifier.height(10.dp))
        Stagger(index = 2, active = active) {
            Text(
                text = if (isPersian)
                    "دستیار شما برای ثبت، پیگیری و کنترل پول؛ ساده، امن و همیشه همراه شما."
                else
                    "Your companion to record, track and control your money; simple, secure and always with you.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(22.dp))
        Stagger(index = 3, active = active) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HighlightChip(Icons.Rounded.CloudSync, if (isPersian) "همگام‌سازی رمزنگاری‌شده" else "Encrypted sync", SettingsAccent.SKY)
                HighlightChip(Icons.Rounded.Translate, if (isPersian) "فارسی و English" else "Persian & English", SettingsAccent.VIOLET)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * لوگو در مرکز، موج‌های ملایمِ پخش‌شونده دورش و پنج آیکون اصلی برنامه که روی یک مدارِ نقطه‌چین می‌چرخند.
 * با خاموش‌بودن انیمیشن‌های سیستم، همه ثابت می‌مانند.
 */
@Composable
private fun OrbitHero() {
    val colors = MaterialTheme.colorScheme
    val animate = animationsEnabled()
    val transition = rememberInfiniteTransition(label = "onboardingOrbit")
    val angleAnimated by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(42_000, easing = LinearEasing)),
        label = "orbitAngle"
    )
    val pulseAnimated by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "orbitPulse"
    )
    val angle = if (animate) angleAnimated else 20f
    val pulse = if (animate) pulseAnimated else 0.3f

    val orbiters = listOf(
        Triple(R.drawable.ic_transaction, SettingsAccent.TEAL, 0),
        Triple(R.drawable.ic_category, SettingsAccent.INDIGO, 1),
        Triple(R.drawable.ic_limit, SettingsAccent.AMBER, 2),
        Triple(R.drawable.ic_goal, SettingsAccent.VIOLET, 3),
        Triple(R.drawable.ic_debtcredit, SettingsAccent.ROSE, 4)
    )
    val orbitRadius = 118
    val primary = colors.primary

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // مدار نقطه‌چین
            drawCircle(
                color = primary.copy(alpha = 0.22f),
                radius = orbitRadius.dp.toPx(),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 10.dp.toPx())))
            )
            // دو موجِ پخش‌شونده از لوگو
            repeat(2) { i ->
                val p = (pulse + i * 0.5f) % 1f
                drawCircle(
                    color = primary.copy(alpha = (1f - p) * 0.22f),
                    radius = 56.dp.toPx() + p * 62.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(color = primary.copy(alpha = 0.08f), radius = 74.dp.toPx())
        }

        // هستهٔ لوگو
        Box(
            modifier = Modifier
                .size(112.dp)
                .shadow(elevation = 18.dp, shape = CircleShape, ambientColor = primary, spotColor = primary)
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

        orbiters.forEach { (res, accent, index) ->
            val theta = Math.toRadians((angle + index * (360.0 / orbiters.size)) - 90.0)
            val tint = accent.color()
            val tileShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .offset(x = (orbitRadius * cos(theta)).dp, y = (orbitRadius * sin(theta)).dp)
                    .size(46.dp)
                    .clip(tileShape)
                    .background(colors.surface.copy(alpha = 0.88f))
                    .background(tint.copy(alpha = 0.14f))
                    .border(1.dp, tint.copy(alpha = 0.32f), tileShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(res), contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
private fun HighlightChip(icon: ImageVector, text: String, accent: SettingsAccent) {
    val colors = MaterialTheme.colorScheme
    val tint = accent.color()
    Row(
        modifier = Modifier
            .glassCard(RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
}

// ---------- مرحله‌های ۲ و ۳: فهرست قابلیت‌ها ----------

@Composable
private fun FeaturesPage(
    isPersian: Boolean,
    active: Boolean,
    eyebrowFa: String,
    eyebrowEn: String,
    titleFa: String,
    titleEn: String,
    headerIcon: ImageVector?,
    features: List<OnboardingFeature>,
    asGrid: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        Stagger(index = 0, active = active) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (headerIcon != null) {
                    HeaderOrb(icon = headerIcon)
                    Spacer(Modifier.height(12.dp))
                }
                EyebrowPill(text = if (isPersian) eyebrowFa else eyebrowEn)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isPersian) titleFa else titleEn,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        if (asGrid) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.chunked(2).forEachIndexed { rowIndex, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEachIndexed { colIndex, feature ->
                            Stagger(
                                index = 1 + rowIndex * 2 + colIndex,
                                active = active,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                FeatureTile(feature = feature, isPersian = isPersian)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEachIndexed { index, feature ->
                    Stagger(index = 1 + index, active = active) {
                        FeatureRow(feature = feature, isPersian = isPersian)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** هالهٔ گرادیانیِ نشان صفحه‌ی امنیت. */
@Composable
private fun HeaderOrb(icon: ImageVector) {
    val colors = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = colors.primary, spotColor = colors.primary)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(30.dp))
        }
    }
}

/** کارت عمودیِ شبکه‌ی امکانات: کاشی آیکون، عنوان و توضیح. */
@Composable
private fun FeatureTile(feature: OnboardingFeature, isPersian: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OnbIconTile(feature = feature, size = 46)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = if (isPersian) feature.titleFa else feature.titleEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = if (isPersian) feature.descFa else feature.descEn,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** کارت افقی برای متن‌های بلندتر (صفحه‌ی امنیت). */
@Composable
private fun FeatureRow(feature: OnboardingFeature, isPersian: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnbIconTile(feature = feature, size = 48)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (isPersian) feature.titleFa else feature.titleEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = if (isPersian) feature.descFa else feature.descEn,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

// ---------- مرحلهٔ ۴: دسترسی‌ها ----------

internal fun permissionIcon(key: String): ImageVector = when (key) {
    "notifications" -> Icons.Rounded.Notifications
    else -> Icons.Rounded.Security
}

@Composable
private fun PermissionsPage(
    isPersian: Boolean,
    active: Boolean,
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
        Spacer(Modifier.height(6.dp))
        Stagger(index = 0, active = active) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeaderOrb(icon = Icons.Rounded.Notifications)
                Spacer(Modifier.height(12.dp))
                EyebrowPill(text = if (isPersian) "دسترسی‌ها" else "Permissions")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isPersian) "دسترسی‌های برنامه" else "App permissions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isPersian)
                        "این دسترسی‌ها اختیاری‌اند و هر زمان از تنظیمات گوشی قابل تغییرند."
                    else
                        "These are optional and can be changed anytime in your phone settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        if (permissions.isEmpty()) {
            Text(
                text = if (isPersian) "این نسخه از برنامه به هیچ دسترسی ویژه‌ای نیاز ندارد."
                else "This version of the app doesn't need any special permissions.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            permissions.forEachIndexed { index, perm ->
                val granted = perm.permissions.all { isPermissionGranted(it) }
                Stagger(index = 1 + index, active = active) {
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
        }
        Spacer(Modifier.height(12.dp))
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
    val accent = SettingsAccent.ROSE.color()
    val success = SettingsAccent.TEAL.color()
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(colors.surfaceVariant.copy(alpha = 0.62f), colors.surfaceVariant.copy(alpha = 0.40f))
                ),
                shape
            )
            .border(1.dp, (if (granted) success else colors.outline).copy(alpha = if (granted) 0.45f else 0.16f), shape)
            .clip(shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OnbIconTile(icon = icon, iconRes = null, tint = accent, size = 48)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(success.copy(alpha = 0.13f), RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = grantedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = success
                )
            }
        } else {
            FilledTonalButton(
                onClick = onAllow,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text(allowLabel, fontWeight = FontWeight.SemiBold) }
        }
    }
}