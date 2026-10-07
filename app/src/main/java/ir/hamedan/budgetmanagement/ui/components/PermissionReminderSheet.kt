package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

/**
 * وضعیت لحظه‌ای یک مجوز برای نمایش در [PermissionReminderSheet].
 *
 * @param permanentlyDenied کاربر قبلاً درخواست سیستمی را رد کرده و اندروید دیگر دیالوگ نشان نمی‌دهد؛
 *        تنها راه فعال‌سازی، تنظیمات برنامه است.
 */
data class PermissionReminderItem(
    val permission: OnboardingPermission,
    val granted: Boolean,
    val permanentlyDenied: Boolean
)

/**
 * یادآوری حرفه‌ایِ مجوزها به‌صورت Bottom Sheet (جایگزین بنر قدیمی بالای صفحه).
 *
 *  - همهٔ مجوزهای مهم یک‌جا و با پیشرفت «x از y فعال» دیده می‌شوند؛ هر مجوز دکمهٔ مستقل خودش را دارد
 *    و بلافاصله بعد از اجازه‌دادن، وضعیتش زنده به «فعال» تغییر می‌کند.
 *  - اگر مجوزی برای همیشه رد شده باشد، دکمه خودکار به «باز کردن تنظیمات» تبدیل می‌شود.
 *  - بستن شیت (کشیدن، لمس بیرون، Back) = «بعداً»؛ فقط دکمهٔ صریح «دیگه نشون نده» یادآوری را متوقف می‌کند.
 *  - وقتی همه فعال شدند، پیام موفقیت کوتاهی نشان داده و خودکار بسته می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionReminderSheet(
    isPersian: Boolean,
    items: List<PermissionReminderItem>,
    onAllow: (OnboardingPermission) -> Unit,
    onLater: () -> Unit,
    onNeverAskAgain: () -> Unit,
    onAllGranted: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val total = items.size
    val grantedCount = items.count { it.granted }
    val allGranted = total > 0 && grantedCount == total
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else grantedCount.toFloat() / total,
        animationSpec = tween(450),
        label = "permissionProgress"
    )

    // همه فعال شد: کمی صبر کن تا کاربر نتیجه را ببیند، بعد ببند.
    LaunchedEffect(allGranted) {
        if (allGranted) {
            delay(1100)
            onAllGranted()
        }
    }

    val numberFormat = NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)

    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------- هدر ----------
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (allGranted) Icons.Rounded.CheckCircle else AppLogoIcon,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    allGranted && isPersian -> "همه‌چیز آماده است"
                    allGranted -> "You're all set"
                    isPersian -> "چند قدم تا تجربهٔ کامل‌تر"
                    else -> "A few steps to the full experience"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    allGranted && isPersian -> "دسترسی‌ها فعال شد. از این‌به‌بعد ثبت تراکنش‌ها سریع‌تر و خودکار است."
                    allGranted -> "Permissions are on. Recording transactions is now faster and automatic."
                    isPersian -> "برنامه بدون این دسترسی‌ها هم کار می‌کند؛ ولی با آن‌ها ثبت تراکنش‌ها سریع‌تر و خودکار می‌شود."
                    else -> "The app works without these, but with them recording transactions becomes faster and automatic."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(18.dp))

            // ---------- نوار پیشرفت ----------
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "دسترسی‌های فعال" else "Permissions enabled",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = if (isPersian) "${numberFormat.format(grantedCount)} از ${numberFormat.format(total)}"
                        else "${numberFormat.format(grantedCount)} of ${numberFormat.format(total)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(colors.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(colors.primary, colors.secondary)))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- کارت‌های مجوز ----------
            Column(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    PermissionReminderCard(
                        isPersian = isPersian,
                        item = item,
                        onAllow = { onAllow(item.permission) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---------- یادداشت حریم خصوصی ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isPersian) "پیامک و صدای شما فقط روی همین دستگاه پردازش می‌شود و هر زمان از تنظیمات گوشی قابل تغییر است."
                    else "Your SMS and voice are processed only on this device, and you can change this anytime in phone settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(18.dp))

            // ---------- اقدام‌های پایین ----------
            if (!allGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onNeverAskAgain,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(
                            text = if (isPersian) "دیگه نشون نده" else "Don't ask again",
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    FilledTonalButton(
                        onClick = onLater,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isPersian) "بعداً یادآوری کن" else "Remind me later",
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionReminderCard(
    isPersian: Boolean,
    item: PermissionReminderItem,
    onAllow: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val permission = item.permission
    val borderColor = if (item.granted) colors.primary.copy(alpha = 0.45f) else colors.outline.copy(alpha = 0.16f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (item.granted) colors.primaryContainer.copy(alpha = 0.28f) else colors.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (item.granted) colors.primary else colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.granted) Icons.Rounded.CheckCircle else permissionIcon(permission.key),
                        contentDescription = null,
                        tint = if (item.granted) colors.onPrimary else colors.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isPersian) permission.titleFa else permission.titleEn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = if (isPersian) permission.reasonFa else permission.reasonEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            when {
                item.granted -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "فعال است" else "Enabled",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                item.permanentlyDenied -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPersian) "این دسترسی قبلاً رد شده؛ برای فعال‌سازی باید از تنظیمات گوشی اجازه بدهید."
                        else "This was denied earlier; you can enable it from your phone settings.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onAllow,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "باز کردن تنظیمات" else "Open settings",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                else -> Button(
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isPersian) "فعال‌سازی" else "Enable",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}