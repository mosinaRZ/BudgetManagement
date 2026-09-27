package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class BarChartEntry(
    val label: String,
    val value: Float,
    val isCurrent: Boolean = false
)

@Composable
fun ColumnBarChartCard(
    title: String,
    subtitle: String,
    entries: List<BarChartEntry>,
    emptyStateText: String,
    averageLabel: String,
    yAxisLabel: String,
    xAxisLabel: String,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null,
    scrollToIndex: Int? = null,
    selectedIndex: Int? = null,
    onEntryClick: ((Int) -> Unit)? = null,
    valueFormatter: (Float) -> String = { it.toLong().toString() }
) {
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            // طراحی شیشه‌ای (Glassmorphic) با آلفای پایین
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .padding(20.dp)
    ) {
        Column {
            // هدر کارت شامل عنوان و اسلات دکمه‌های سوییچ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (actionContent != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    actionContent()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // بدنه نمودار
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyStateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                val maxValue = entries.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
                val listState = rememberLazyListState()
                val density = LocalDensity.current

                LaunchedEffect(scrollToIndex, entries.size) {
                    scrollToIndex?.let { index ->
                        if (index in entries.indices) {
                            // ابتدا از ابتدای نمودار شروع می‌کنیم و سپس با easing
                            // slow → fast → slow به نقطه‌ی هدف می‌رسیم.
                            listState.scrollToItem(0)
                            delay(120)
                            val itemExtentPx = with(density) { 76.dp.toPx() }
                            val currentScrollPx =
                                listState.firstVisibleItemIndex * itemExtentPx + listState.firstVisibleItemScrollOffset
                            val targetScrollPx =
                                (index * itemExtentPx - itemExtentPx * 2f).coerceAtLeast(0f)
                            listState.animateScrollBy(
                                value = targetScrollPx - currentScrollPx,
                                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        itemsIndexed(entries) { index, entry ->
                            val targetHeight = (entry.value / maxValue) * 120f
                            val animatedHeight by animateFloatAsState(
                                targetValue = targetHeight,
                                animationSpec = tween(durationMillis = 800),
                                label = "bar_height"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .width(60.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = onEntryClick != null) { onEntryClick?.invoke(index) }
                                    .background(
                                        if (selectedIndex == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 4.dp)
                            ) {
                                val formattedValue = valueFormatter(entry.value)
                                val amountFontSize = when {
                                    formattedValue.length <= 8 -> 11.sp
                                    formattedValue.length <= 11 -> 9.sp
                                    formattedValue.length <= 14 -> 8.sp
                                    else -> 7.sp
                                }
                                Text(
                                    text = formattedValue,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = amountFontSize),
                                    color = if (selectedIndex == index || entry.isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selectedIndex == index || entry.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 18.dp, max = 30.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(animatedHeight.coerceAtLeast(4f).dp)
                                        .background(
                                            color = when {
                                                selectedIndex == index -> MaterialTheme.colorScheme.primary
                                                entry.isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                                            },
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(56.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}