package ir.hamedan.budgetmanagement.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.preferences.NotificationCategory
import ir.hamedan.budgetmanagement.data.preferences.NotificationPreferences
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.AppHapticType
import ir.hamedan.budgetmanagement.utils.rememberAppHaptics
import java.text.NumberFormat
import java.util.Locale

/**
 * شخصی‌سازی اعلان‌ها.
 *
 * هر دسته در یک کارت واحد نمایش داده می‌شود (سربرگ دسته + ردیف‌های نوع اعلان با جداکننده)؛
 * بالای صفحه هم خلاصهٔ «چند اعلان از کل فعال است» دیده می‌شود. آیکون دسته‌های اصلی همان
 * آیکون‌های اختصاصی برنامه (ic_transaction، ic_category، ic_limit، ic_goal، ic_debtcredit) است.
 */
@Composable
fun NotificationCalibrationScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val haptic = rememberAppHaptics()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        NotificationPreferences.ensureDefaultsInitialized(context)
    }

    var typeStates by remember {
        mutableStateOf(NotificationPreferences.getAllTypeStates(context))
    }

    fun refreshStates() {
        typeStates = NotificationPreferences.getAllTypeStates(context)
    }

    fun setTypeEnabled(type: NotificationType, enabled: Boolean) {
        haptic.perform(AppHapticType.Tick)
        NotificationPreferences.setTypeEnabled(context, type, enabled)
        refreshStates()
    }

    fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        haptic.perform(AppHapticType.Click)
        NotificationPreferences.setCategoryEnabled(context, category, enabled)
        refreshStates()
    }

    val categories = remember {
        NotificationCategory.entries.filter { c -> NotificationType.entries.any { it.category == c } }
    }
    val totalCount = NotificationType.entries.size
    val enabledCount = NotificationType.entries.count { typeStates[it] == true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        AuroraBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            CalibrationTopBar(isPersian = isPersian, onBackClick = {
                haptic.perform(AppHapticType.Click)
                onBackClick()
            })

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "summary") {
                    SummaryCard(
                        enabled = enabledCount,
                        total = totalCount,
                        isPersian = isPersian
                    )
                }

                items(count = categories.size, key = { categories[it].name }) { index ->
                    val category = categories[index]
                    val types = NotificationType.entries.filter { it.category == category }
                    CategoryCard(
                        category = category,
                        types = types,
                        typeStates = typeStates,
                        isPersian = isPersian,
                        onToggleAll = { enabled -> setCategoryEnabled(category, enabled) },
                        onToggleType = { type, enabled -> setTypeEnabled(type, enabled) }
                    )
                }

                item(key = "bottom_inset") {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

// ───────────────────────────── اجزای صفحه ─────────────────────────────

@Composable
private fun CalibrationTopBar(isPersian: Boolean, onBackClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .background(scheme.surfaceVariant.copy(alpha = 0.75f), CircleShape)
                .border(1.dp, scheme.outline.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isPersian) "بازگشت" else "Back",
                tint = scheme.onSurface
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPersian) "شخصی‌سازی اعلان‌ها" else "Customize Notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isPersian) "برای هر رویداد تعیین کنید خبر بگیرید یا نه"
                else "Choose which events you want to hear about",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** خلاصهٔ وضعیت: «۱۲ از ۱۸ اعلان فعال است» + نوار پیشرفت. */
@Composable
private fun SummaryCard(enabled: Int, total: Int, isPersian: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val fraction by animateFloatAsState(
        targetValue = if (total == 0) 0f else enabled.toFloat() / total,
        animationSpec = tween(350),
        label = "summaryProgress"
    )
    val nf = remember(isPersian) {
        NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primaryContainer.copy(alpha = 0.9f),
                        scheme.primaryContainer.copy(alpha = 0.45f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.4f), scheme.primary.copy(alpha = 0.08f))),
                shape
            )
            .padding(20.dp)
    ) {
        Text(
            text = if (isPersian) "${nf.format(enabled)} از ${nf.format(total)} اعلان فعال است"
            else "$enabled of $total notifications on",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isPersian) "تغییرها بلافاصله اعمال می‌شوند."
            else "Changes apply immediately.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(16.dp))
        // نوار پیشرفت سفارشی؛ رنگ‌ها کاملاً از تم
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(scheme.onPrimaryContainer.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: NotificationCategory,
    types: List<NotificationType>,
    typeStates: Map<NotificationType, Boolean>,
    isPersian: Boolean,
    onToggleAll: (Boolean) -> Unit,
    onToggleType: (NotificationType, Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val allEnabled = types.all { typeStates[it] == true }
    val enabledCount = types.count { typeStates[it] == true }
    val nf = remember(isPersian) {
        NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceVariant.copy(alpha = 0.4f), shape)
            .border(1.dp, scheme.outline.copy(alpha = 0.12f), shape)
    ) {
        // سربرگ دسته
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.primary.copy(alpha = 0.07f))
                .toggleable(
                    value = allEnabled,
                    role = Role.Switch,
                    onValueChange = onToggleAll
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipColor by animateColorAsState(
                if (enabledCount > 0) scheme.primary else scheme.onSurface.copy(alpha = 0.10f),
                animationSpec = tween(200), label = "categoryChip"
            )
            val chipTint by animateColorAsState(
                if (enabledCount > 0) scheme.onPrimary else scheme.onSurfaceVariant,
                animationSpec = tween(200), label = "categoryChipTint"
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(chipColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                val icon = categoryIcon(category)
                if (icon.res != null) {
                    Icon(
                        painter = painterResource(id = icon.res),
                        contentDescription = null,
                        tint = chipTint,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (icon.vector != null) {
                    Icon(
                        imageVector = icon.vector,
                        contentDescription = null,
                        tint = chipTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) category.titleFa else category.titleEn,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = if (isPersian) "${nf.format(enabledCount)} از ${nf.format(types.size)} فعال"
                    else "$enabledCount of ${types.size} on",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Text(
                text = if (isPersian) "همه" else "All",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            // سوییچ فقط نمایشگر است؛ خودِ ردیف toggleable است (یک ناحیهٔ لمس، بدون دوباره‌خوانی در TalkBack)
            Switch(
                checked = allEnabled,
                onCheckedChange = null,
                colors = calibrationSwitchColors()
            )
        }

        types.forEach { type ->
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = scheme.outline.copy(alpha = 0.10f)
            )
            TypeRow(
                type = type,
                isPersian = isPersian,
                checked = typeStates[type] == true,
                onCheckedChange = { onToggleType(type, it) }
            )
        }
    }
}

@Composable
private fun TypeRow(
    type: NotificationType,
    isPersian: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = if (isPersian) type.titleFa else type.titleEn,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) scheme.onSurface else scheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isPersian) type.descriptionFa else type.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = calibrationSwitchColors()
        )
    }
}

@Composable
private fun calibrationSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
)

/** آیکون هر دسته: درایبل اختصاصی برنامه در صورت وجود، وگرنه آیکون متریال. */
private data class CategoryIcon(
    @DrawableRes val res: Int? = null,
    val vector: ImageVector? = null
)

private fun categoryIcon(category: NotificationCategory): CategoryIcon = when (category) {
    NotificationCategory.TRANSACTIONS -> CategoryIcon(res = R.drawable.ic_transaction)
    NotificationCategory.CATEGORIES -> CategoryIcon(res = R.drawable.ic_category)
    NotificationCategory.BUDGET -> CategoryIcon(res = R.drawable.ic_limit)
    NotificationCategory.GOALS -> CategoryIcon(res = R.drawable.ic_goal)
    NotificationCategory.DEBT_CREDIT -> CategoryIcon(res = R.drawable.ic_debtcredit)
    NotificationCategory.REMINDERS -> CategoryIcon(vector = Icons.Default.Alarm)
    NotificationCategory.SECURITY -> CategoryIcon(vector = Icons.Default.Security)
    NotificationCategory.SYSTEM -> CategoryIcon(vector = Icons.Default.Settings)
}