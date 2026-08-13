package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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

                LaunchedEffect(scrollToIndex, entries.size) {
                    scrollToIndex?.let { index ->
                        if (index in entries.indices) {
                            listState.animateScrollToItem(index)
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
                        itemsIndexed(entries) { _, entry ->
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
                                    .width(36.dp)
                                    .fillMaxHeight()
                            ) {
                                if (entry.value > 0f) {
                                    Text(
                                        text = valueFormatter(entry.value),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(animatedHeight.coerceAtLeast(4f).dp)
                                        .background(
                                            color = if (entry.isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
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
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}