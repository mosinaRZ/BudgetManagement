package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class BarChartEntry(
    val label: String,
    val value: Float,
    val color: Color,
    val isHighlighted: Boolean = false
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
    listState: LazyListState = rememberLazyListState(),
    valueFormatter: (Float) -> String = { it.toInt().toString() }
) {
    val cardShape = RoundedCornerShape(24.dp)
    val averageColor = MaterialTheme.colorScheme.tertiary
    val barAreaHeight = 160.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), cardShape)
            .clip(cardShape)
            .padding(20.dp)
    ) {
        Column {
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

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text(
                    text = emptyStateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val maxValue = (entries.maxOfOrNull { it.value } ?: 1f)
                    .let { if (it <= 0f) 1f else it } * 1.15f

                val average = entries.map { it.value }.average().toFloat()
                val avgFraction = if (maxValue > 0f) (average / maxValue).coerceIn(0f, 1f) else 0f

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = yAxisLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .rotate(-90f)
                            .padding(end = 4.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barAreaHeight)
                                    .align(Alignment.TopStart)
                            ) {
                                val y = size.height - (avgFraction * size.height)
                                drawLine(
                                    color = averageColor.copy(alpha = 0.85f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            }

                            LazyRow(
                                state = listState,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                items(entries) { entry ->
                                    val heightFraction = (entry.value / maxValue).coerceIn(0f, 1f)
                                    val barHeight = (barAreaHeight * heightFraction)
                                        .let { if (it < 4.dp) 4.dp else it }

                                    Column(
                                        modifier = Modifier.width(44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(barAreaHeight)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                if (entry.value > 0f) {
                                                    Text(
                                                        text = valueFormatter(entry.value),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (entry.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .width(24.dp)
                                                        .height(barHeight)
                                                        .clip(
                                                            RoundedCornerShape(
                                                                topStart = 8.dp,
                                                                topEnd = 8.dp
                                                            )
                                                        )
                                                        .background(
                                                            if (entry.isHighlighted) MaterialTheme.colorScheme.primary else entry.color.copy(alpha = 0.7f)
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = entry.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (entry.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (entry.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = xAxisLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(20.dp, 2.dp)) {
                        drawLine(
                            color = averageColor.copy(alpha = 0.85f),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }
                    Text(
                        text = averageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}