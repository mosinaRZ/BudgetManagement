package ir.hamedan.budgetmanagement.ui.screens.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * A lightweight Dynamic-Island-inspired hint for the interactive analytics bars.
 * It deliberately uses only Compose primitives so it remains dependency-free.
 */
@Composable
fun AnalyticsInteractionHint(
    isPersian: Boolean,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var expanded by remember(visible) { mutableStateOf(false) }
    var showMessage by remember(visible) { mutableStateOf(false) }

    val width by animateDpAsState(
        targetValue = if (expanded) 332.dp else 58.dp,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "analyticsHintWidth"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) 62.dp else 30.dp,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "analyticsHintHeight"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (showMessage) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "analyticsHintContentAlpha"
    )

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        delay(90)
        expanded = true
        delay(520)
        showMessage = true
        delay(4200)
        showMessage = false
        delay(180)
        expanded = false
        delay(520)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .shadow(
                    elevation = if (expanded) 16.dp else 7.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.91f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (isPersian) {
                            "برای دیدن جزئیات، روی ستون یک بازه بزنید؛ برای بازگشت دوباره همان ستون را لمس کنید"
                        } else {
                            "Tap a bar to explore a period; tap it again to return"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = contentAlpha }
                    )
                }
            }
        }
    }
}