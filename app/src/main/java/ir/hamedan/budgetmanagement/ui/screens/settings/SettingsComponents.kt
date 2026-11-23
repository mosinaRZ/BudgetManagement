package ir.hamedan.budgetmanagement.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// بلوک‌های سازنده‌ی صفحه‌ی تنظیمات
//
// ساختار: کارت پروفایل (Hero) ← گروه‌های موضوعی (هر گروه یک کارت شیشه‌ای با ردیف‌های جداشده با
// خط‌ریز) ← ردیف‌هایی با آیکونِ رنگیِ هر موضوع. گزینه‌های چندحالته (ظاهر، زبان، واحد پول، اعلان)
// با مقدار فعلی در انتهای ردیف نمایش داده می‌شوند و با لمس، شیت انتخاب باز می‌شود.
// ═════════════════════════════════════════════════════════════════════════════

/** رنگ تأکید هر موضوع؛ تیره در تم روشن و روشن در تم تیره تا آیکون همیشه خوانا بماند (≥ ۳:۱ روی کاشی). */
internal enum class SettingsAccent(val light: Color, val dark: Color) {
    TEAL(Color(0xFF1F6B53), Color(0xFF6FD3B0)),
    INDIGO(Color(0xFF4338CA), Color(0xFFA5B4FC)),
    AMBER(Color(0xFFB45309), Color(0xFFFBBF24)),
    ROSE(Color(0xFFBE185D), Color(0xFFF9A8D4)),
    SKY(Color(0xFF0369A1), Color(0xFF7DD3FC)),
    VIOLET(Color(0xFF6D28D9), Color(0xFFC4B5FD)),
    SLATE(Color(0xFF475569), Color(0xFFCBD5E1)),
    DANGER(Color(0xFFB91C1C), Color(0xFFFCA5A5));

    @Composable
    fun color(): Color = if (isLightSurface()) light else dark
}

@Composable
private fun isLightSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

/** رنگ متنِ تأکیدی برند که روی سطح کارت‌ها در هر دو تم از ۴٫۵:۱ کمتر نمی‌شود. */
@Composable
internal fun settingsBrandText(): Color =
    if (isLightSurface()) Color(0xFF1F6B53) else MaterialTheme.colorScheme.primary

/** برای آیکون‌های برند (واتس‌اپ، ایکس و …) که یک رنگِ ثابت در هر دو تم خوانا نیست. */
@Composable
internal fun settingsBrandColor(light: Color, dark: Color): Color = if (isLightSurface()) light else dark

// ───────────────────────── گروه و ردیف ─────────────────────────

/** عنوان کوچک بالای هر گروه. */
@Composable
internal fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(50))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * کارت یک گروه: ردیف‌ها با خط‌ریزِ هم‌ترازِ متن از هم جدا می‌شوند. وقتی جستجو ردیفی را پنهان می‌کند،
 * خط‌ریز هم خودکار حذف می‌شود، چون فهرست ردیف‌ها را همین‌جا می‌چینیم.
 */
@Composable
internal fun SettingsGroup(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    danger: Boolean = false,
    onSheet: Boolean = false
) {
    if (rows.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val borderColor = if (danger) scheme.error.copy(alpha = 0.28f) else scheme.outline.copy(alpha = 0.13f)
    val fill = if (onSheet) {
        // داخل شیتِ شیشه‌ای، کارت گروه کمی روشن‌تر از زمینه‌ی شیت است تا از آن جدا دیده شود.
        Brush.linearGradient(listOf(scheme.surface.copy(alpha = 0.55f), scheme.surface.copy(alpha = 0.45f)))
    } else if (danger) {
        Brush.linearGradient(listOf(scheme.error.copy(alpha = 0.08f), scheme.surfaceVariant.copy(alpha = 0.34f)))
    } else {
        Brush.linearGradient(listOf(scheme.surfaceVariant.copy(alpha = 0.50f), scheme.surfaceVariant.copy(alpha = 0.34f)))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) SettingsSectionHeader(text = title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(fill, shape)
                .border(1.dp, borderColor, shape)
                .clip(shape)
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 1.dp,
                        color = scheme.outline.copy(alpha = 0.10f)
                    )
                }
                row()
            }
        }
        if (footer != null) {
            Text(
                text = footer,
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/** کاشی آیکون: مربعِ گرد با رنگ تأکید موضوع. */
@Composable
internal fun SettingsIconTile(
    icon: ImageVector?,
    @DrawableRes iconRes: Int?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            iconRes != null -> Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
            icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** ردیف پایه: کاشی آیکون، عنوان و توضیح، و یک جای خالی برای انتهای ردیف (مقدار، فلش یا سوئیچ). */
@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
    accent: SettingsAccent = SettingsAccent.TEAL,
    tint: Color? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    role: Role = Role.Button,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val iconTint = tint ?: accent.color()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconTile(icon = icon, iconRes = iconRes, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
internal fun SettingsChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(22.dp)
    )
}

/** مقدار فعلیِ یک گزینه به‌صورت قرص کوچک + فلش؛ لمس ردیف شیت انتخاب را باز می‌کند. */
@Composable
internal fun SettingsValueTrailing(value: String) {
    val brand = settingsBrandText()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = brand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 130.dp)
                .background(brand.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        SettingsChevron()
    }
}

/** ردیف سوئیچ؛ کل ردیف قابل‌لمس است و خودِ سوئیچ فقط نمایشی. */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    accent: SettingsAccent,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accent = accent,
        role = Role.Switch,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onPrimary,
                    checkedTrackColor = scheme.primary,
                    uncheckedThumbColor = scheme.outline,
                    uncheckedTrackColor = scheme.surfaceVariant,
                    uncheckedBorderColor = scheme.outline.copy(alpha = 0.6f)
                )
            )
        }
    )
}

// ───────────────────────── کارت پروفایل ─────────────────────────

/**
 * کارت بالای تنظیمات: آواتار، نام، شماره‌ی پوشیده یا توضیح حالت مهمان، و نوار تکمیل پروفایل.
 * [completionPercent] برای مهمان null است.
 */
@Composable
internal fun SettingsProfileHero(
    isPersian: Boolean,
    isGuest: Boolean,
    name: String,
    subtitle: String,
    initials: String,
    completionPercent: Int?,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val brand = settingsBrandText()
    val nf = remember(isPersian) { NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.20f),
                        scheme.secondary.copy(alpha = 0.10f),
                        scheme.surfaceVariant.copy(alpha = 0.40f)
                    )
                ),
                shape
            )
            .border(1.dp, scheme.primary.copy(alpha = 0.28f), shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // آواتار: حلقه‌ی گرادیانی دور دایره‌ی حروف اول
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .border(2.dp, Brush.sweepGradient(listOf(scheme.primary, scheme.secondary, scheme.primary)), CircleShape)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.primary.copy(alpha = 0.40f)))),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(34.dp).background(scheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isPersian) "مشاهده پروفایل" else "Open profile",
                    tint = brand,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        when {
            isGuest -> HeroPill(
                icon = Icons.Default.Shield,
                text = if (isPersian) "حالت مهمان" else "Guest mode",
                tint = SettingsAccent.AMBER.color()
            )

            completionPercent != null && completionPercent >= 100 -> HeroPill(
                icon = Icons.Default.Check,
                text = if (isPersian) "پروفایل کامل است" else "Profile complete",
                tint = brand
            )

            completionPercent != null -> {
                val progress by animateFloatAsState(
                    targetValue = (completionPercent / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "settingsHeroProgress"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPersian) "تکمیل پروفایل" else "Profile completion",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = nf.format(completionPercent.toLong()) + if (isPersian) "٪" else "%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = brand
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(scheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(scheme.primary.copy(alpha = 0.75f), scheme.primary)))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPill(icon: ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

// ───────────────────────── شیت انتخاب ─────────────────────────

internal data class SettingsOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

/** شیت انتخاب تک‌گزینه‌ای با هدر رنگی؛ [footer] برای لینک‌های تکمیلی زیر گزینه‌هاست. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsOptionSheet(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    @DrawableRes iconRes: Int? = null,
    accent: SettingsAccent,
    options: List<SettingsOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    SettingsFloatingSheet(onDismiss = onDismiss) {
        SettingsSheetHeader(title = title, subtitle = subtitle, icon = icon, iconRes = iconRes, accent = accent)
        options.forEach { option ->
            SettingsChoiceCard(
                title = option.title,
                description = option.description,
                icon = option.icon,
                isSelected = option.id == selectedId,
                onClick = { onSelect(option.id) }
            )
        }
        if (footer != null) footer()
    }
}

/**
 * شیتِ شناورِ شیشه‌ای، دقیقاً هم‌قالب با شیت اعلان‌های صفحه‌ی خانه: زمینه‌ی شفاف، کارت ۳۲dp با
 * ۱۶dp فاصله از کناره‌ها و ۳۶dp از بالا و پایین، اسکریم ۴۰٪ و دستگیره‌ی کوچک بالای کارت.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsFloatingSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 36.dp)
                .background(color = scheme.surfaceVariant.copy(alpha = 0.96f), shape = shape)
                .border(width = 1.dp, color = scheme.outline.copy(alpha = 0.15f), shape = shape)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(scheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/**
 * بدنه‌ی قابل‌پیمایشِ شیت‌های فیلتر: گزینه‌ها تا سقف ۵۵٪ ارتفاع صفحه اسکرول می‌شوند و دکمه‌های
 * [SettingsSheetActions] زیر آن ثابت می‌مانند.
 */
@Composable
internal fun ColumnScope.SettingsSheetScrollBody(content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** دکمه‌های انتهای شیتِ فیلتر؛ هم‌قالب با دکمه‌های دیالوگ‌های برنامه (۴۸dp، گوشه‌ی ۱۶dp). */
@Composable
internal fun SettingsSheetActions(
    cancelText: String,
    applyText: String,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = cancelText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
            onClick = onApply,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = applyText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SettingsSheetHeader(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    @DrawableRes iconRes: Int? = null,
    accent: SettingsAccent
) {
    val tint = accent.color()
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(tint.copy(alpha = 0.14f), CircleShape)
                .border(1.dp, tint.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconRes != null -> Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * کارت «انتخاب تک‌گزینه‌ای». دایرهٔ انتخاب (Radio) در انتهای کارت نشان می‌دهد که این کارت یکی از
 * چند حالت است، نه دکمهٔ ورود به صفحه‌ای دیگر.
 */
@Composable
internal fun SettingsChoiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val brand = settingsBrandText()
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        if (isSelected) scheme.primary.copy(alpha = 0.60f) else scheme.outline.copy(alpha = 0.16f),
        animationSpec = tween(200), label = "choiceBorder"
    )
    val container by animateColorAsState(
        if (isSelected) scheme.primary.copy(alpha = 0.12f) else scheme.surface.copy(alpha = 0.50f),
        animationSpec = tween(200), label = "choiceContainer"
    )
    val chipColor by animateColorAsState(
        if (isSelected) scheme.primary else scheme.onSurface.copy(alpha = 0.08f),
        animationSpec = tween(200), label = "choiceChip"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
        label = "choiceDot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container, shape)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(chipColor, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) brand else scheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) scheme.primary else scheme.outline.copy(alpha = 0.55f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotScale
                    }
                    .background(scheme.primary, CircleShape)
            )
        }
    }
}

/** کارت «ورود به صفحهٔ جدید» با فلشِ جهت‌دار؛ ظاهر متفاوت از کارت‌های انتخاب. */
@Composable
internal fun SettingsNavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val brand = settingsBrandText()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer.copy(alpha = 0.55f), scheme.primaryContainer.copy(alpha = 0.2f))
                ),
                shape
            )
            .border(1.dp, scheme.primary.copy(alpha = 0.24f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(scheme.primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = brand, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.size(30.dp).background(scheme.primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = brand,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** پانویس صفحه: نام و نسخه‌ی برنامه. */
@Composable
internal fun SettingsFooter(isPersian: Boolean, versionName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = if (isPersian) "سیدنا" else "Cidna",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isPersian) "نسخه $versionName" else "Version $versionName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}