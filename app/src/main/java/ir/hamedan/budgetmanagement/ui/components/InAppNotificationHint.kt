package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Size and timing of the hint for one notification, derived from how much text it has.
 *
 *  - Display time grows with the amount of text (people need time to read it), within
 *    [MIN_DISPLAY_MS]..[MAX_DISPLAY_MS].
 *  - Short notices use a larger island and larger letters; long notices use a wider island with
 *    smaller letters so they fit in a few more lines instead of being cut off.
 */
internal data class InAppHintLayout(
    val displayMillis: Long,
    val expandedWidth: Dp,
    val expandedHeight: Dp,
    val bodyFontSize: TextUnit,
    val bodyMaxLines: Int
)

internal const val MIN_DISPLAY_MS = 3_200L
internal const val MAX_DISPLAY_MS = 9_000L

/** Text length (title + body) at which the hint reaches its largest/widest size. */
private const val LONG_TEXT_CHARS = 140f
private const val MAX_BODY_LINES = 4
private const val BASE_DISPLAY_MS = 2_400L
private const val MS_PER_CHAR = 45L

private fun lerpFloat(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

internal fun inAppHintLayoutFor(title: String, body: String): InAppHintLayout {
    val cleanTitle = title.trim()
    val cleanBody = body.trim()
    val length = cleanTitle.length + cleanBody.length

    // 0 = very short notice, 1 = long notice.
    val fraction = (length / LONG_TEXT_CHARS).coerceIn(0f, 1f)

    val displayMillis = (BASE_DISPLAY_MS + length * MS_PER_CHAR).coerceIn(MIN_DISPLAY_MS, MAX_DISPLAY_MS)

    val width = lerpFloat(300f, 360f, fraction)
    val fontSp = lerpFloat(14f, 11f, fraction)
    val lineHeightSp = fontSp * 1.35f

    // Rough line estimate for the body: characters that fit on one line at this size.
    val usableWidth = width - 70f // icon, paddings and spacing
    val charsPerLine = (usableWidth / (fontSp * 0.55f)).coerceAtLeast(1f)
    val bodyLines = if (cleanBody.isEmpty()) 1
    else ceil(cleanBody.length / charsPerLine).toInt().coerceIn(1, MAX_BODY_LINES)

    // Title line + gap + body lines + vertical padding.
    val height = (14f + 16f + 2f + bodyLines * lineHeightSp + 10f).coerceAtLeast(62f)

    return InAppHintLayout(
        displayMillis = displayMillis,
        expandedWidth = width.dp,
        expandedHeight = height.dp,
        bodyFontSize = fontSp.sp,
        bodyMaxLines = bodyLines
    )
}

/**
 * In-app notification with the same expanding/collapsing visual language as
 * AnalyticsInteractionHint. It is intentionally rendered above the app
 * content, so a foreground notification never reaches the system shade.
 */
@Composable
fun InAppNotificationHint(
    isPersian: Boolean,
    titleFa: String,
    titleEn: String,
    bodyFa: String,
    bodyEn: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val title = if (isPersian) titleFa else titleEn
    val body = if (isPersian) bodyFa else bodyEn
    val layout = remember(title, body) { inAppHintLayoutFor(title, body) }

    var expanded by remember(visible) { mutableStateOf(false) }
    var showMessage by remember(visible) { mutableStateOf(false) }

    val width by animateDpAsState(
        targetValue = if (expanded) layout.expandedWidth else 58.dp,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "notificationHintWidth"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) layout.expandedHeight else 30.dp,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "notificationHintHeight"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (showMessage) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "notificationHintContentAlpha"
    )

    LaunchedEffect(visible) {
        delay(90)
        expanded = true
        delay(520)
        showMessage = true
        // Time on screen depends on how much there is to read.
        delay(layout.displayMillis)
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
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = contentAlpha }
                    ) {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = layout.bodyFontSize,
                            lineHeight = (layout.bodyFontSize.value * 1.35f).sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            maxLines = layout.bodyMaxLines
                        )
                    }
                }
            }
        }
    }
}