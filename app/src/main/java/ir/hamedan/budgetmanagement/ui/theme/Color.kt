package ir.hamedan.budgetmanagement.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Color Scheme
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0052CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0052CC),
    onPrimaryContainer = Color(0xFFC4D2FF),

    secondary = Color(0xFF5D5F5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE0E0),
    onSecondaryContainer = Color(0xFF616363),

    tertiary = Color(0xFF7B2600),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA33500),
    onTertiaryContainer = Color(0xFFFFC6B2),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    background = Color(0xFFFCF9F8),
    onBackground = Color(0xFF1C1B1B),

    surface = Color(0xFFFCF9F8),
    onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFE5E2E1),
    onSurfaceVariant = Color(0xFF434654),

    outline = Color(0xFF737685),
    outlineVariant = Color(0xFFC3C6D6),

    inverseSurface = Color(0xFF313030),
    inverseOnSurface = Color(0xFFF3F0EF),
    inversePrimary = Color(0xFFB2C5FF),

    surfaceDim = Color(0xFFDCD9D9),
    surfaceBright = Color(0xFFFCF9F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F3F2),
    surfaceContainer = Color(0xFFF0EDEC),
    surfaceContainerHigh = Color(0xFFEBE7E7),
    surfaceContainerHighest = Color(0xFFE5E2E1),
)

// Dark Color Scheme (پیشنهادی بر اساس توضیحات طراحی)
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    onPrimary = Color(0xFF001848),
    primaryContainer = Color(0xFF0040A2),
    onPrimaryContainer = Color(0xFFDAE2FF),

    secondary = Color(0xFFC6C6C7),
    onSecondary = Color(0xFF1A1C1C),
    secondaryContainer = Color(0xFF454747),
    onSecondaryContainer = Color(0xFFE2E2E2),

    tertiary = Color(0xFFFFB59B),
    onTertiary = Color(0xFF380D00),
    tertiaryContainer = Color(0xFF812800),
    onTertiaryContainer = Color(0xFFFFDBCF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE5E2E1),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFE5E2E1),
    surfaceVariant = Color(0xFF434654),
    onSurfaceVariant = Color(0xFFC3C6D6),

    outline = Color(0xFF8D9099),
    outlineVariant = Color(0xFF434654),

    inverseSurface = Color(0xFFE5E2E1),
    inverseOnSurface = Color(0xFF313030),
    inversePrimary = Color(0xFF0052CC),
)