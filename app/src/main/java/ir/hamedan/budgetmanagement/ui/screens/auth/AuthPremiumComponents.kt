package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn

/**
 * Keeps auth content readable on every screen size: full width on phones, a centered column
 * of at most [maxWidth] on tablets, foldables and landscape.
 */
@Composable
fun AuthResponsiveContent(
    maxWidth: Dp = 480.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * One-time-code input drawn as separate cells on top of a single hidden text field, so paste,
 * SMS suggestions, backspace and the numeric keyboard all behave like a normal text field.
 *
 * - Cell size is computed from the available width, so 6 cells fit on any screen.
 * - Cells are always laid out left-to-right (digits keep their order in RTL languages).
 * - Animated active-cell highlight, blinking caret, digit pop-in and a light haptic tick.
 * - Opens the keyboard automatically when it first appears ([autoFocus]).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    length: Int = 6,
    autoFocus: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val digits = value.take(length)
    val complete = digits.length == length
    val activeIndex = digits.length.coerceAtMost(length - 1)

    val caretAlpha = rememberInfiniteTransition(label = "otp_caret").animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "otp_caret_alpha"
    )

    if (autoFocus) {
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface.copy(alpha = 0.7f)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = if (maxWidth < 300.dp) 6.dp else 10.dp
            val cellWidth = ((maxWidth - gap * (length - 1)) / length).coerceAtMost(56.dp)
            val cellHeight = (cellWidth * 1.3f).coerceIn(44.dp, 68.dp)
            // Derived from the cell size (not the font scale) so large system fonts never overflow a cell.
            val fontSize = with(LocalDensity.current) { (cellWidth * 0.52f).toSp() }
            val digitStyle = credentialTextStyle().copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            BasicTextField(
                value = TextFieldValue(text = digits, selection = TextRange(digits.length)),
                onValueChange = { new ->
                    if (new.text.length > digits.length) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onValueChange(new.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = label },
                enabled = enabled,
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    hintLocales = LocaleList(Locale("en"))
                ),
                keyboardActions = keyboardActions,
                decorationBox = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = enabled
                                ) {
                                    runCatching { focusRequester.requestFocus() }
                                    keyboard?.show()
                                },
                            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
                        ) {
                            repeat(length) { index ->
                                OtpCell(
                                    char = digits.getOrNull(index),
                                    isActive = enabled && focused && index == activeIndex,
                                    complete = complete,
                                    width = cellWidth,
                                    height = cellHeight,
                                    textStyle = digitStyle,
                                    caretAlpha = caretAlpha
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OtpCell(
    char: Char?,
    isActive: Boolean,
    complete: Boolean,
    width: Dp,
    height: Dp,
    textStyle: androidx.compose.ui.text.TextStyle,
    caretAlpha: State<Float>
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> colors.primary
            complete -> colors.primary.copy(alpha = 0.6f)
            char != null -> colors.primary.copy(alpha = 0.4f)
            else -> colors.outline.copy(alpha = 0.25f)
        },
        animationSpec = tween(180),
        label = "otp_cell_border"
    )
    val fillColor by animateColorAsState(
        targetValue = when {
            isActive -> colors.primary.copy(alpha = 0.10f)
            char != null -> colors.primary.copy(alpha = 0.06f)
            else -> colors.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(180),
        label = "otp_cell_fill"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 2.dp else 1.dp,
        animationSpec = tween(150),
        label = "otp_cell_border_width"
    )
    val cellScale by animateFloatAsState(
        targetValue = if (isActive) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "otp_cell_scale"
    )
    val pop by animateFloatAsState(
        targetValue = if (char != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "otp_digit_pop"
    )

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .scale(cellScale)
            .background(fillColor, shape)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if (char != null) {
            Text(
                text = char.toString(),
                style = textStyle,
                color = colors.onSurface,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    val s = 0.5f + 0.5f * pop
                    scaleX = s
                    scaleY = s
                    alpha = pop.coerceIn(0f, 1f)
                }
            )
        } else if (isActive) {
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = height * 0.4f)
                    .graphicsLayer { alpha = caretAlpha.value }
                    .background(colors.primary, RoundedCornerShape(1.dp))
            )
        }
    }
}