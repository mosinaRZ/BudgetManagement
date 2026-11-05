package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * How long a notification stays on screen. It grows with the amount of text (title + body) so a
 * long message is never gone before it can be read, within [MIN_DISPLAY_MS]..[MAX_DISPLAY_MS].
 * The user can also hold a finger on the card to pause the countdown.
 */
internal const val MIN_DISPLAY_MS = 4_000L
internal const val MAX_DISPLAY_MS = 16_000L
private const val BASE_DISPLAY_MS = 2_500L
private const val MS_PER_CHAR = 60L

/** Time the exit animation needs before the host window can be removed. */
private const val EXIT_ANIMATION_MS = 260L

internal fun inAppDisplayMillisFor(title: String, body: String): Long {
    val length = title.trim().length + body.trim().length
    return (BASE_DISPLAY_MS + length * MS_PER_CHAR).coerceIn(MIN_DISPLAY_MS, MAX_DISPLAY_MS)
}

/** Places the popup horizontally centered, [topPx] below the top edge of the screen. */
private class TopCenterPositionProvider(private val topPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset((windowSize.width - popupContentSize.width) / 2, topPx)
}

/**
 * The single place where in-app notifications are shown, for every screen of the app.
 *
 *  - Rendered in its own [Popup] window, so it always sits above screens, dialogs and bottom
 *    sheets instead of falling behind them, and never changes the layout of what is underneath.
 *  - Anchored to the same spot everywhere: just under the status bar / display cutout.
 *  - A newer [notification] immediately replaces the one on screen (no queueing).
 *  - Dismissed by the "دیدمش" button, or automatically after a time based on its length.
 *
 * Pass `null` when nothing should be shown; call [onDismiss] handling with the id so a stale
 * timer can never close a newer notification.
 */
@Composable
fun InAppNotificationHost(
    isPersian: Boolean,
    notification: NotificationHelper.InAppNotification?,
    onDismiss: (id: Long) -> Unit
) {
    // Keep the last notification composed while the exit animation plays.
    var shown by remember { mutableStateOf<NotificationHelper.InAppNotification?>(null) }
    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(notification) {
        if (notification != null) {
            shown = notification
        } else {
            delay(EXIT_ANIMATION_MS)
            shown = null
        }
    }
    visibleState.targetState = notification != null

    val current = shown ?: return

    val density = LocalDensity.current
    val topInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
    val topPx = topInsets.getTop(density) + with(density) { 8.dp.roundToPx() }
    val cutoutSides = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
    val leftInset = with(density) { cutoutSides.getLeft(density, LayoutDirection.Ltr).toDp() }
    val rightInset = with(density) { cutoutSides.getRight(density, LayoutDirection.Ltr).toDp() }

    val popupPositionProvider = remember(topPx) { TopCenterPositionProvider(topPx) }

    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(
            focusable = false,            // never steals focus / touches from the screen below
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            // The outer padding leaves room for the card's shadow, which a window would clip.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = leftInset, right = rightInset)
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.62f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(tween(200)) + scaleOut(
                        targetScale = 0.84f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = tween(220)
                    )
                ) {
                    // A newer notification cross-fades in quickly over the old one.
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            (fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = tween(160))) togetherWith
                                    fadeOut(tween(90)) using SizeTransform(clip = false) { _, _ -> snap() }
                        },
                        label = "inAppNotificationContent"
                    ) { item ->
                        InAppNotificationCard(
                            isPersian = isPersian,
                            title = if (isPersian) item.titleFa else item.titleEn,
                            body = if (isPersian) item.bodyFa else item.bodyEn,
                            notificationId = item.id,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

private val notificationNumberRegex =
    Regex("[0-9۰-۹٠-٩]+(?:[٬,][0-9۰-۹٠-٩]{3})*")

private fun parseNotificationNumber(value: String): Long? =
    value.filter { it.isDigit() }.map { it.digitToInt() }.joinToString("").toLongOrNull()

private fun formatNotificationNumber(value: Long, sample: String): String {
    val digits = value.toString()
    val separator = sample.firstOrNull { it == ',' || it == '٬' }
    val grouped = if (separator == null) digits else
        digits.reversed().chunked(3).joinToString(separator.toString()).reversed()

    val zero = when {
        sample.any { it in '۰'..'۹' } -> '۰'
        sample.any { it in '٠'..'٩' } -> '٠'
        else -> '0'
    }
    return grouped.map { char ->
        if (char.isDigit()) (zero.code + char.digitToInt()).toChar() else char
    }.joinToString("")
}

@Composable
private fun NotificationBodyText(
    body: String,
    notificationId: Long,
    color: androidx.compose.ui.graphics.Color
) {
    val matches = notificationNumberRegex.findAll(body).toList()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        var cursor = 0
        matches.forEachIndexed { index, match ->
            if (match.range.first > cursor) {
                Text(
                    text = body.substring(cursor, match.range.first),
                    color = color,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
            }

            val sample = match.value
            val amount = parseNotificationNumber(sample)
            if (amount != null) {
                CountUpText(
                    target = amount,
                    key = "in_app_notification.$notificationId.$index",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = color,
                    fontWeight = FontWeight.Medium,
                    softWrap = false,
                    format = { animatedValue ->
                        formatNotificationNumber(animatedValue, sample)
                    }
                )
            } else {
                Text(
                    text = sample,
                    color = color,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            cursor = match.range.last + 1
        }

        if (cursor < body.length) {
            Text(
                text = body.substring(cursor),
                color = color,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun InAppNotificationCard(
    isPersian: Boolean,
    title: String,
    body: String,
    notificationId: Long,
    onDismiss: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val displayMillis = remember(title, body) { inAppDisplayMillisFor(title, body) }

    // Remaining time (1 -> 0). Pressing and holding the card pauses it.
    val progress = remember(notificationId) { Animatable(1f) }
    var pressed by remember(notificationId) { mutableStateOf(false) }
    var dragging by remember(notificationId) { mutableStateOf(false) }
    val paused = pressed || dragging
    val offsetX = remember(notificationId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(notificationId, paused) {
        if (!paused) {
            val remaining = (displayMillis * progress.value).toInt().coerceAtLeast(1)
            progress.animateTo(0f, tween(remaining, easing = LinearEasing))
            onDismiss(notificationId)
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .graphicsLayer {
                // Follows the finger sideways and fades as it leaves, like system notifications.
                translationX = offsetX.value
                alpha = (1f - abs(offsetX.value) / (size.width * 1.1f)).coerceIn(0f, 1f)
            }
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = colors.primary.copy(alpha = 0.22f),
                spotColor = colors.primary.copy(alpha = 0.32f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.surface.copy(alpha = 0.98f),
                        colors.surfaceVariant.copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.50f),
                        colors.onSurface.copy(alpha = 0.08f),
                        colors.primary.copy(alpha = 0.24f)
                    )
                ),
                shape
            )
            .pointerInput(notificationId) {
                detectTapGestures(onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                })
            }
            .pointerInput(notificationId) {
                val dismissDistance = size.width * 0.3f
                val flickSpeed = 14.dp.toPx()
                var lastDelta = 0f
                fun settle() {
                    scope.launch {
                        val fastEnough = abs(lastDelta) > flickSpeed && abs(offsetX.value) > 40.dp.toPx()
                        if (abs(offsetX.value) > dismissDistance || fastEnough) {
                            val direction = if (offsetX.value >= 0f) 1f else -1f
                            offsetX.animateTo(direction * size.width * 1.15f, tween(160))
                            onDismiss(notificationId)
                        } else {
                            offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
                            dragging = false
                        }
                    }
                }
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; lastDelta = 0f },
                    onDragEnd = { settle() },
                    onDragCancel = { settle() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        lastDelta = dragAmount
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                )
            }
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primary, colors.primary.copy(alpha = 0.72f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            color = colors.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                    }
                    if (body.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        // Full text is always reachable: very long messages scroll instead of being cut.
                        Box(
                            modifier = Modifier
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            NotificationBodyText(
                                body = body,
                                notificationId = notificationId,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.13f))
                        .clickable { onDismiss(notificationId) }
                        .semantics { role = Role.Button }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPersian) "دیدمش" else "Got it",
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Thin countdown line along the bottom edge: shows how long the notice will stay.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress.value)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.primary.copy(alpha = 0.85f), colors.primary.copy(alpha = 0.25f))
                    )
                )
        )
    }
}