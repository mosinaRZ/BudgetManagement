package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            if (entries.isEmpty() || entries.all { it.value == 0f }) {
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        items(entries) { entry ->
                            val targetHeight = (entry.value / maxValue) * 130f
                            val animatedHeight by animateFloatAsState(
                                targetValue = targetHeight,
                                animationSpec = tween(durationMillis = 800),
                                label = "bar_height"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(animatedHeight.coerceAtLeast(4f).dp) // حداقل ارتفاع برای مقادیر صفر
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
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}