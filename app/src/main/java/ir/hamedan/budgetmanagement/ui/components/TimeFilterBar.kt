package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BarHeight = 46.dp
private val BarShape = RoundedCornerShape(16.dp)
private val SegmentShape = RoundedCornerShape(13.dp)

/**
 * نوار فیلتر زمانی همراه با دکمه‌ی فیلتر پیشرفته؛ مشترک بین «تراکنش‌ها» و «آمار».
 *
 * - چیپ‌ها یک کنترل قطعه‌ای (Segmented) هستند: نشانگر انتخاب با فنر روی قطعه‌ی انتخابی می‌لغزد.
 * - دکمه‌ی فیلتر هم‌ارتفاع نوار و کنار آن است. وقتی فیلتر پیشرفته‌ای فعال باشد رنگ برند می‌گیرد و یک
 *   نقطه‌ی نشان روی آن ظاهر می‌شود. لمس آن با انیمیشن فشرده‌شدن همراه است.
 * - [selectedIndex] = ‎-1 یعنی هیچ چیپی انتخاب نیست (مثلاً بازه‌ی دلخواه فعال است)؛ نشانگر محو می‌شود.
 * - اگر [onClear] داده شود (فیلتر پیشرفته فعال)، به‌جای چیپ‌ها یک قرص «حذف فیلترها» نشان داده می‌شود.
 */
@Composable
fun TimeFilterBar(
    labels: List<String>,
    selectedIndex: Int,
    isPersian: Boolean,
    isFilterActive: Boolean,
    onSelect: (Int) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .height(BarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(colors.surfaceVariant.copy(alpha = 0.5f), BarShape)
                .border(1.dp, colors.outline.copy(alpha = 0.10f), BarShape)
                .padding(4.dp)
        ) {
            if (onClear != null) {
                ClearPill(label = clearLabel.orEmpty(), onClick = onClear)
            } else {
                SegmentedChips(labels = labels, selectedIndex = selectedIndex, onSelect = onSelect)
            }
        }
        FilterButton(active = isFilterActive, isPersian = isPersian, onClick = onFilterClick)
    }
}

@Composable
private fun SegmentedChips(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val brand = if (colors.background.luminance() > 0.5f) Color(0xFF1F6B53) else colors.primary
    val count = labels.size.coerceAtLeast(1)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val segmentWidth = maxWidth / count
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            label = "timeFilterIndicator"
        )
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (selectedIndex >= 0) 1f else 0f,
            animationSpec = tween(220),
            label = "timeFilterIndicatorAlpha"
        )

        // نشانگر لغزنده
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .graphicsLayer { alpha = indicatorAlpha }
                .padding(1.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.22f), colors.primary.copy(alpha = 0.12f))
                    ),
                    SegmentShape
                )
                .border(1.dp, colors.primary.copy(alpha = 0.34f), SegmentShape)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) brand else colors.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "timeFilterText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(SegmentShape)
                        .semantics { this.selected = selected }
                        .clickable(role = Role.Tab) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearPill(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(SegmentShape)
            .background(colors.errorContainer.copy(alpha = 0.9f))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = colors.onErrorContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onErrorContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterButton(active: Boolean, isPersian: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "filterButtonScale"
    )
    val container by animateColorAsState(
        targetValue = if (active) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "filterButtonContainer"
    )
    val tint by animateColorAsState(
        targetValue = if (active) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(250),
        label = "filterButtonTint"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) colors.primary.copy(alpha = 0.55f) else colors.outline.copy(alpha = 0.10f),
        animationSpec = tween(250),
        label = "filterButtonBorder"
    )
    val description = if (isPersian) {
        if (active) "فیلتر پیشرفته (فعال)" else "فیلتر پیشرفته"
    } else {
        if (active) "Advanced filter (active)" else "Advanced filter"
    }

    Box(
        modifier = Modifier
            .size(BarHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(container, BarShape)
            .border(1.dp, borderColor, BarShape)
            .clip(BarShape)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        // نقطه‌ی نشانِ «فیلتر فعال است»
        AnimatedVisibility(
            visible = active,
            modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
            enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(colors.onPrimary, CircleShape)
                    .border(1.5.dp, colors.primary, CircleShape)
            )
        }
    }
}