package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.ui.screens.settings.SettingsSectionHeader
import ir.hamedan.budgetmanagement.ui.screens.settings.settingsBrandText
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ═════════════════════════════════════════════════════════════════════════════
// اجزای مشترکِ شیت‌های «فیلتر پیشرفته» (تراکنش‌ها و آمار)
//
// زبان بصری: شیتِ شیشه‌ای شناور + کاشی‌های آیکون‌دار با گرادیان برند.
//  • هیچ ایموجی‌ای استفاده نمی‌شود؛ هر گزینه یک کاشی‌ی آیکون (یا عددِ بزرگ) دارد.
//  • گزینه‌ی انتخاب‌شده: کاشیِ پرشده با رنگ برند، حاشیه‌ی درخشان و نشانِ تیکِ فنری.
//  • بازه‌ی تاریخ: یک «کارتِ بازه» با دو سلول (از → تا)، خط زمانی و قرصِ مدتِ بازه.
// ═════════════════════════════════════════════════════════════════════════════

/**
 * یک گزینه‌ی کاشی.
 *  - [icon]: آیکون برداری داخل کاشی؛
 *  - [iconRes]: آیکون از منابع drawable (مثلاً R.drawable.ic_category)، وقتی آیکون برداری مناسب نیست؛
 *  - [badge] / [badgeUnit]: به‌جای آیکون، یک عددِ درشت با واحدِ کوچک (مثلاً «۷ / روز»).
 */
internal data class FilterChoice(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    val badge: String? = null,
    val badgeUnit: String? = null
)

/** سربرگ: کاشی‌ی آیکون + عنوان/زیرعنوان؛ «بازنشانی» در انتها (فقط وقتی چیزی تغییر کرده). */
@Composable
internal fun FilterSheetHeader(
    title: String,
    resetLabel: String,
    showReset: Boolean,
    onReset: () -> Unit,
    subtitle: String? = null,
    /** null = بدون کاشی‌ی آیکون (مثلاً صفحه‌ی آمار). */
    icon: ImageVector? = Icons.Default.Tune
) {
    val scheme = MaterialTheme.colorScheme
    val brand = settingsBrandText()
    val tileShape = RoundedCornerShape(15.dp)

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(tileShape)
                    .background(
                        Brush.linearGradient(
                            listOf(scheme.primary.copy(alpha = 0.26f), scheme.primary.copy(alpha = 0.07f))
                        )
                    )
                    .border(1.dp, scheme.primary.copy(alpha = 0.28f), tileShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = brand, modifier = Modifier.size(22.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        AnimatedVisibility(visible = showReset, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
            Text(
                text = resetLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = brand,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(scheme.primary.copy(alpha = 0.10f))
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

/** شبکه‌ی کاشی‌های تک‌انتخابی (پیش‌فرض دو ستونه). [selectedId] = null یعنی هیچ‌کدام انتخاب نیست. */
@Composable
internal fun FilterChipGrid(
    choices: List<FilterChoice>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    /** false = بدون هیچ آیکون (کاشی‌ها فقط عدد/متن دارند و نشانِ انتخاب یک نقطه است). */
    useIcons: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        choices.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { choice ->
                    FilterChoiceTile(
                        choice = choice,
                        selected = choice.id == selectedId,
                        useIcons = useIcons,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onSelect(choice.id) }
                    )
                }
                // ردیفِ ناقص: جای خالی تا کاشی‌ها هم‌عرض بمانند
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FilterChoiceTile(
    choice: FilterChoice,
    selected: Boolean,
    useIcons: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val brand = settingsBrandText()
    val shape = RoundedCornerShape(20.dp)
    val iconShape = RoundedCornerShape(14.dp)

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "filterTileScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = 0.65f) else scheme.outline.copy(alpha = 0.16f),
        animationSpec = tween(200),
        label = "filterTileBorder"
    )
    val bgStart by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = 0.20f) else scheme.surface.copy(alpha = 0.50f),
        animationSpec = tween(220),
        label = "filterTileBgStart"
    )
    val bgEnd by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = 0.05f) else scheme.surface.copy(alpha = 0.50f),
        animationSpec = tween(220),
        label = "filterTileBgEnd"
    )
    val tileBg by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.primary.copy(alpha = 0.10f),
        animationSpec = tween(220),
        label = "filterTileIconBg"
    )
    val tileContent by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else brand,
        animationSpec = tween(220),
        label = "filterTileIconContent"
    )
    // تیکِ انتخاب با فنر ظاهر می‌شود (بدون AnimatedVisibility تا با scopeهای Row/Box تداخل نکند)
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "filterTileCheck"
    )

    Row(
        modifier = modifier
            .heightIn(min = 64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)), shape)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, shape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // کاشی‌ی آیکون / عدد (اگر گزینه نه آیکون دارد و نه عدد، کاشی اصلاً نمایش داده نمی‌شود و فقط عنوان می‌ماند)
        if (choice.badge != null || choice.icon != null || choice.iconRes != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(iconShape)
                    .background(tileBg),
                contentAlignment = Alignment.Center
            ) {
                when {
                    choice.badge != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = choice.badge,
                            color = tileContent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        if (choice.badgeUnit != null) {
                            Text(
                                text = choice.badgeUnit,
                                color = tileContent.copy(alpha = 0.85f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    choice.icon != null && useIcons -> Icon(
                        imageVector = choice.icon,
                        contentDescription = null,
                        tint = tileContent,
                        modifier = Modifier.size(22.dp)
                    )

                    choice.iconRes != null && useIcons -> Icon(
                        painter = painterResource(id = choice.iconRes),
                        contentDescription = null,
                        tint = tileContent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Text(
            text = choice.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) brand else scheme.onSurface,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // نشانِ انتخاب: حلقه‌ی خالی ← دایره‌ی پر با تیکِ فنری
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, scheme.outline.copy(alpha = if (selected) 0f else 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (useIcons) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.onPrimary))
                }
            }
        }
    }
}

/**
 * «کارتِ بازه»: دو سلولِ «از / تا» با فلشِ اتصال، و وقتی هر دو انتخاب شدند یک خطِ زمانی با قرصِ
 * مدتِ بازه (تعداد روز) زیرش ظاهر می‌شود. تاریخ‌ها با چرخ انتخابِ برنامه (WheelDatePickerDialog) انتخاب می‌شوند.
 *
 * مقدارها شروعِ روزِ محلی (epoch millis) هستند. برای جلوگیری از بازه‌ی نامعتبر:
 *  - با انتخاب «از»، اگر «تا» خالی یا قبل از آن باشد، «تا» خودکار اصلاح می‌شود؛
 *  - با انتخاب «تا»، اگر «از» خالی یا بعد از آن باشد، «از» خودکار اصلاح می‌شود.
 */
@Composable
internal fun FilterDateRangeGroup(
    isPersian: Boolean,
    startMillis: Long?,
    endMillis: Long?,
    onRangeChange: (start: Long?, end: Long?) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    minDate: LocalDate = LocalDate.now().minusYears(30),
    maxDate: LocalDate = LocalDate.now(),
    /** false = بدون آیکون (آیکون تقویم و فلشِ بین دو سلول حذف می‌شوند). */
    useIcons: Boolean = true
) {
    // 0 = هیچ، 1 = انتخاب «از»، 2 = انتخاب «تا»
    var picking by remember { mutableIntStateOf(0) }
    val todayMillis = remember { filterStartOfDay(System.currentTimeMillis()) }
    val today = remember { LocalDate.now() }
    // «از تاریخ» هرگز نمی‌تواند در آینده (فردا و بعد از آن) باشد.
    val fromMax = if (maxDate.isAfter(today)) today else maxDate
    val scheme = MaterialTheme.colorScheme
    val brand = settingsBrandText()
    val cardShape = RoundedCornerShape(24.dp)
    val hasBoth = startMillis != null && endMillis != null

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionHeader(text = title)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(
                    Brush.linearGradient(
                        listOf(scheme.surface.copy(alpha = 0.55f), scheme.surface.copy(alpha = 0.42f))
                    )
                )
                .border(1.dp, scheme.outline.copy(alpha = 0.13f), cardShape)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterDateCell(
                    label = if (isPersian) "از تاریخ" else "From",
                    placeholder = if (isPersian) "انتخاب" else "Select",
                    millis = startMillis,
                    isPersian = isPersian,
                    showIcon = useIcons,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { picking = 1 }
                )

                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (useIcons) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = brand.copy(alpha = if (hasBoth) 0.95f else 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        // بدون آیکون: یک خط‌تیره‌ی کوچک بین «از» و «تا»
                        Box(
                            Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(brand.copy(alpha = if (hasBoth) 0.95f else 0.35f))
                        )
                    }
                }

                FilterDateCell(
                    label = if (isPersian) "تا تاریخ" else "To",
                    placeholder = if (isPersian) "انتخاب" else "Select",
                    millis = endMillis,
                    isPersian = isPersian,
                    showIcon = useIcons,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { picking = 2 }
                )
            }

            AnimatedVisibility(
                visible = hasBoth,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                val days = if (startMillis != null && endMillis != null) filterDaysBetween(startMillis, endMillis) else 0L
                Column {
                    Spacer(Modifier.height(14.dp))
                    FilterRangeTrack(
                        durationText = filterDurationText(days, isPersian),
                        brand = brand
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }

    if (picking == 1) {
        // «از تاریخ»: حداکثر امروز؛ مقدار اولیه هم داخل بازه‌ی مجاز نگه داشته می‌شود.
        val initial = (startMillis ?: endMillis ?: todayMillis).coerceAtMost(filterEndOfDay(fromMax.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()))
        WheelDatePickerDialog(
            isPersian = isPersian,
            title = if (isPersian) "از تاریخ" else "From date",
            initialMillis = initial,
            minDate = minDate,
            maxDate = fromMax,
            onConfirm = { picked ->
                val start = minOf(filterStartOfDay(picked), todayMillis)
                val currentEnd = endMillis?.let(::filterStartOfDay)
                // «تا تاریخ» نباید قبل از «از تاریخ» باشد: اگر خالی یا عقب‌تر بود، اصلاح می‌شود.
                val newEnd = when {
                    currentEnd == null -> maxOf(start, todayMillis)
                    currentEnd < start -> start
                    else -> endMillis
                }
                onRangeChange(start, newEnd)
                picking = 0
            },
            onDismiss = { picking = 0 }
        )
    }

    if (picking == 2) {
        // «تا تاریخ»: چرخ‌ها از «از تاریخ» شروع می‌شوند، پس روزِ قبل از آن اصلاً قابل انتخاب نیست.
        val toMin = startMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            ?.takeIf { it.isAfter(minDate) }
            ?: minDate
        val toMinMillis = toMin.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val initial = (endMillis ?: startMillis ?: todayMillis).coerceAtLeast(toMinMillis)
        WheelDatePickerDialog(
            isPersian = isPersian,
            title = if (isPersian) "تا تاریخ" else "To date",
            initialMillis = initial,
            minDate = toMin,
            maxDate = maxDate,
            onConfirm = { picked ->
                val end = maxOf(filterStartOfDay(picked), toMinMillis)
                val currentStart = startMillis?.let(::filterStartOfDay)
                // اگر «از» خالی بود، همان روز (حداکثر امروز) برایش گذاشته می‌شود.
                val newStart = if (currentStart != null) startMillis else minOf(end, todayMillis)
                onRangeChange(newStart, end)
                picking = 0
            },
            onDismiss = { picking = 0 }
        )
    }
}

/** یک سلولِ تاریخ: برچسب کوچک + مقدار (یا «انتخاب»)؛ پر شدن، حاشیه و زمینه را برند می‌کند. */
@Composable
private fun FilterDateCell(
    label: String,
    placeholder: String,
    millis: Long?,
    isPersian: Boolean,
    showIcon: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val brand = settingsBrandText()
    val shape = RoundedCornerShape(18.dp)
    val filled = millis != null

    val borderColor by animateColorAsState(
        targetValue = if (filled) scheme.primary.copy(alpha = 0.45f) else scheme.outline.copy(alpha = 0.18f),
        animationSpec = tween(200),
        label = "dateCellBorder"
    )
    val container by animateColorAsState(
        targetValue = if (filled) scheme.primary.copy(alpha = 0.10f) else scheme.surface.copy(alpha = 0.45f),
        animationSpec = tween(200),
        label = "dateCellBg"
    )

    Column(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(container, shape)
            .border(1.dp, borderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showIcon) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (filled) brand else scheme.onSurface.copy(alpha = 0.40f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = millis?.let { DateUtils.formatTimestamp(it, isPersian) } ?: placeholder,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
                color = if (filled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** خطِ زمانیِ بازه: نقطه — خط — «قرصِ مدت» — خط — نقطه. */
@Composable
private fun FilterRangeTrack(durationText: String, brand: Color) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(brand))
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(brand.copy(alpha = 0.30f))
        )
        Text(
            text = durationText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = brand,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(scheme.primary.copy(alpha = 0.12f))
                .border(1.dp, scheme.primary.copy(alpha = 0.28f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(brand.copy(alpha = 0.30f))
        )
        Box(Modifier.size(8.dp).clip(CircleShape).background(brand))
    }
}

/**
 * بدنه‌ی شیتِ فیلتر. به‌جای سقفِ ۵۵٪ی بدنه‌ی عمومیِ تنظیمات، تا جایی که شیت جا دارد باز می‌شود
 * (ارتفاع صفحه منهای سربرگ، دکمه‌ها، حاشیه‌های شیت و نوار سیستم) تا با باز شدنِ خطِ زمانیِ بازه
 * نیازی به اسکرول نباشد. فقط در صفحه‌های خیلی کوتاه اسکرول فعال می‌شود.
 */
@Composable
internal fun ColumnScope.FilterSheetBody(content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp - 340).coerceAtLeast(260).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .animateContentSize(tween(220)),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** تعداد روزهای بازه (هر دو سر شامل). */
private fun filterDaysBetween(startMillis: Long, endMillis: Long): Long {
    val zone = ZoneId.systemDefault()
    val s = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
    val e = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
    return (ChronoUnit.DAYS.between(s, e) + 1).coerceAtLeast(1L)
}

private fun filterDurationText(days: Long, isPersian: Boolean): String =
    if (isPersian) {
        "${filterPersianDigits(days.toString())} روز"
    } else {
        if (days == 1L) "1 day" else "$days days"
    }

/** ارقام لاتین را فارسی می‌کند؛ برای عنوان‌ها و نشان‌های عددی. */
internal fun filterPersianDigits(value: String): String {
    val latin = "0123456789"
    val persian = "۰۱۲۳۴۵۶۷۸۹"
    return buildString(value.length) {
        value.forEach { ch ->
            val i = latin.indexOf(ch)
            append(if (i >= 0) persian[i] else ch)
        }
    }
}

/** شروعِ روزِ محلیِ [millis]. */
internal fun filterStartOfDay(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** آخرین میلی‌ثانیه‌ی روزِ محلیِ [millis]؛ برای اینکه تراکنش‌های روزِ پایانی هم داخل بازه بیفتند. */
internal fun filterEndOfDay(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L