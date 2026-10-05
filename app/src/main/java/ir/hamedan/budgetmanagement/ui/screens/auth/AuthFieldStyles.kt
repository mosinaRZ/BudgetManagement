package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Text style for credentials (phone, email, password, codes, keys).
 *
 * These values are always typed with Latin characters and digits, so the glyphs must stay
 * left-to-right. In a right-to-left layout the text is right-aligned so it lines up with the
 * floating label and the leading icon instead of jumping to the opposite edge.
 */
@Composable
fun credentialTextStyle(): TextStyle {
    val rtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
    return LocalTextStyle.current.copy(
        textDirection = TextDirection.Ltr,
        textAlign = if (rtlLayout) TextAlign.Right else TextAlign.Left
    )
}

/** Inline error banner used inside auth cards, placed next to the action that failed. */
@Composable
fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Password strength meter shared by registration and the change-password dialog. */
@Composable
fun PasswordStrengthMeter(password: String, isPersian: Boolean, modifier: Modifier = Modifier) {
    val strength = PasswordPolicy.strength(password)
    if (strength == PasswordPolicy.Strength.EMPTY) return

    val (color, label, fraction) = when (strength) {
        PasswordPolicy.Strength.WEAK -> Triple(Color(0xFFE57373), if (isPersian) "ضعیف" else "Weak", 0.25f)
        PasswordPolicy.Strength.MEDIUM -> Triple(Color(0xFFFFB74D), if (isPersian) "متوسط" else "Medium", 0.5f)
        PasswordPolicy.Strength.GOOD -> Triple(Color(0xFFDCE775), if (isPersian) "خوب" else "Good", 0.75f)
        else -> Triple(Color(0xFF81C784), if (isPersian) "قوی" else "Strong", 1f)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPersian) "قدرت گذرواژه" else "Password strength",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(color, CircleShape)
            )
        }
    }
}