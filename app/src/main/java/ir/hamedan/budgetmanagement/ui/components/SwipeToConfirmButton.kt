package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeToConfirmButton(
    text: String,
    isPersian: Boolean,
    resetTrigger: Any,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
) {
    val layoutDirection = if (isPersian) LayoutDirection.Ltr else LocalLayoutDirection.current

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        SwipeToConfirmButtonContent(
            text = text,
            isPersian = isPersian,
            resetTrigger = resetTrigger,
            onConfirm = onConfirm,
            modifier = modifier,
            height = height,
        )
    }
}

@Composable
private fun SwipeToConfirmButtonContent(
    text: String,
    isPersian: Boolean,
    resetTrigger: Any,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
) {
    val density = LocalDensity.current
    val handleSize = 46.dp
    val handlePadding = 4.dp
    val handleSizePx = with(density) { handleSize.toPx() }
    val handlePaddingPx = with(density) { handlePadding.toPx() }
    val hapticStepPx = with(density) { 14.dp.toPx() }

    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.surfaceContainerHigh
    val progressColor = scheme.primary.copy(alpha = 0.28f)
    val handleColor = scheme.secondaryContainer
    val handleIconColor = scheme.onSecondaryContainer
    val hintColor = scheme.onSurfaceVariant

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    var confirmed by remember { mutableStateOf(false) }
    var thresholdReached by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticOffset by remember { mutableFloatStateOf(0f) }

    val maxOffset = (trackWidthPx - handleSizePx - handlePaddingPx * 2).coerceAtLeast(0f)
    val confirmThreshold = 0.85f
    val progress = (offsetX.value / maxOffset.coerceAtLeast(1f)).coerceIn(0f, 1f)

    fun resetHandle() {
        confirmed = false
        thresholdReached = false
        isDragging = false
        lastHapticOffset = 0f
        scope.launch { offsetX.snapTo(0f) }
    }

    LaunchedEffect(resetTrigger) {
        resetHandle()
    }

    LaunchedEffect(isDragging, thresholdReached) {
        if (isDragging && thresholdReached) {
            while (isActive) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                delay(90)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )
    val hintOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(with(density) { (trackWidthPx * progress).toDp() })
                    .background(progressColor)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = hintColor,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = hintAlpha * (1f - progress)
                    translationX = hintOffset
                }
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (handlePaddingPx + offsetX.value).roundToInt(),
                        0
                    )
                }
                .padding(vertical = handlePadding)
                .size(handleSize)
                .clip(RoundedCornerShape(14.dp))
                .background(handleColor)
                .pointerInput(maxOffset) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            lastHapticOffset = offsetX.value
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                if (offsetX.value >= maxOffset * confirmThreshold && maxOffset > 0f) {
                                    offsetX.animateTo(maxOffset, tween(150))
                                    if (!confirmed) {
                                        confirmed = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConfirm()
                                    }
                                    offsetX.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
                                    confirmed = false
                                } else {
                                    offsetX.animateTo(0f, tween(250))
                                }
                                thresholdReached = false
                                lastHapticOffset = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch { offsetX.animateTo(0f, tween(250)) }
                            thresholdReached = false
                            lastHapticOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newValue = (offsetX.value + dragAmount).coerceIn(0f, maxOffset)
                            scope.launch { offsetX.snapTo(newValue) }

                            val reachedNow = maxOffset > 0f && newValue >= maxOffset * confirmThreshold
                            if (reachedNow && !thresholdReached) {
                                thresholdReached = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (!reachedNow && thresholdReached) {
                                thresholdReached = false
                            }

                            if (abs(newValue - lastHapticOffset) >= hapticStepPx) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticOffset = newValue
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = handleIconColor
            )
        }
    }
}