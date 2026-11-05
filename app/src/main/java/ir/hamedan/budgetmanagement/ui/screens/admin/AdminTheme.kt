package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Design tokens of the admin console.
 *
 * Everything here is derived from the app theme (CidnaTheme) or from values the rest of the app
 * already uses (24dp frosted cards, 28dp dialogs and sheets, 14dp / 48dp buttons), so the console
 * looks like the budget app instead of a separate product. Screens must not hard-code colours,
 * radii or alphas; add a token here instead.
 */

internal object AdminShapes {
    /** Frosted cards: same radius as AuthCard and the devices screen. */
    val Card = RoundedCornerShape(24.dp)

    /** Rows nested inside a card or sheet (devices, events). */
    val Item = RoundedCornerShape(18.dp)

    /** Text fields and filter buttons. */
    val Field = RoundedCornerShape(16.dp)

    /** Dialog, sheet-footer and action buttons (48dp tall everywhere in the app). */
    val Button = RoundedCornerShape(14.dp)

    val Dialog = RoundedCornerShape(28.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

internal object AdminAlpha {
    /** Secondary text, captions and quiet icons. */
    const val Muted = 0.6f

    /** Values next to a muted label. */
    const val Strong = 0.8f

    /** Background of icon badges and status pills. */
    const val Tint = 0.14f

    /** Frosted card fill (same value as AuthCard / DeviceCard). */
    const val CardFill = 0.4f

    /** Default card outline. */
    const val Hairline = 0.1f
}

// ------------------------------------------------------------------ surfaces

@Composable
internal fun adminCardColor(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AdminAlpha.CardFill)

@Composable
internal fun adminCardBorder(
    color: Color = MaterialTheme.colorScheme.outline,
    alpha: Float = AdminAlpha.Hairline
): BorderStroke = BorderStroke(1.dp, color.copy(alpha = alpha))

@Composable
internal fun adminMuted(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = AdminAlpha.Muted)

// ------------------------------------------------------------------ semantic colours

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

// The three hues below come from the palette the main Analytics screen already uses
// (388E3C / 0288D1 / 6750A4), with lighter siblings for the dark theme.

/** Online / healthy. The theme has no success colour of its own. */
@Composable
internal fun adminSuccess(): Color = if (isDarkSurface()) Color(0xFF81C784) else Color(0xFF388E3C)

@Composable
internal fun adminInfo(): Color = if (isDarkSurface()) Color(0xFF64B5F6) else Color(0xFF0288D1)

@Composable
internal fun adminViolet(): Color = if (isDarkSurface()) Color(0xFFB39DDB) else Color(0xFF6750A4)

@Composable
internal fun severityColor(severity: String): Color = when (severity) {
    "danger" -> MaterialTheme.colorScheme.error
    "warning" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
internal fun roleColor(role: String): Color = when (role) {
    "admin" -> MaterialTheme.colorScheme.error
    "premium" -> MaterialTheme.colorScheme.tertiary
    "pro" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
internal fun riskColor(level: String): Color = when (level) {
    "high" -> MaterialTheme.colorScheme.error
    "medium" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

/** Distinct, theme-friendly colours for categorical charts (donut, bars). */
@Composable
internal fun chartPalette(): List<Color> {
    val dark = isDarkSurface()
    return listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        adminInfo(),
        adminViolet(),
        adminSuccess(),
        MaterialTheme.colorScheme.secondary,
        if (dark) Color(0xFFF48FB1) else Color(0xFFC2185B),
        if (dark) Color(0xFFFFB74D) else Color(0xFFF57C00)
    )
}

// ------------------------------------------------------------------ typography
//
// The app's Typography only defines displayLarge, headlineLarge/Medium, titleMedium, bodyLarge/Medium
// and labelLarge/Small. Every other style (titleSmall, bodySmall, labelMedium...) silently falls back
// to the system font, so Persian text in those styles would not use Vazirmatn. The admin therefore
// sticks to the defined styles and derives the two extra sizes it needs from bodyMedium.

internal val Typography.adminCaption: TextStyle
    get() = bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp)

internal val Typography.adminTitleSmall: TextStyle
    get() = bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)

/** Text drawn on a Canvas does not inherit the theme, so charts take the app font from here. */
internal fun Typography.chartLabel(color: Color, size: TextUnit = 9.sp): TextStyle =
    TextStyle(fontFamily = bodyMedium.fontFamily, fontSize = size, color = color)