package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

/**
 * Visuals for the app-wide snackbar. [isError] only changes the accent (icon + tint); the shape,
 * elevation, typography and the Undo countdown ring are identical for every message so all screens
 * look and behave the same.
 */
data class AppSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val isError: Boolean = false
) : SnackbarVisuals

/** Shared layout constants so every screen places its snackbar the same way. */
object AppSnackbarDefaults {
    /**
     * Bottom padding that lifts a snackbar above the floating capsule navigation bar + FAB
     * (56dp FAB + 5dp gap + breathing room). Screens that show the main bar must use at least this.
     */
    val AboveBottomBar = 84.dp
}

/**
 * Single entry point for showing a snackbar: a new message always replaces the one currently on
 * screen immediately (instead of queueing behind it), so the user never waits for stale feedback.
 * Dismissing an Undo snackbar this way resolves it as [SnackbarResult.Dismissed], which callers
 * treat as "commit".
 */
suspend fun SnackbarHostState.showReplacing(visuals: SnackbarVisuals): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(visuals)
}

/** Shows a failure message. Errors stay a little longer than confirmations so they can be read. */
suspend fun SnackbarHostState.showError(message: String): SnackbarResult =
    showReplacing(AppSnackbarVisuals(message = message, duration = SnackbarDuration.Long, isError = true))

/** Shows a neutral/confirmation message, optionally with an action (e.g. Undo). */
suspend fun SnackbarHostState.showInfo(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult =
    showReplacing(AppSnackbarVisuals(message = message, actionLabel = actionLabel, duration = duration))

/**
 * The one snackbar host every screen uses. Callers keep their own [modifier] (alignment/padding
 * differs per screen because of bottom bars and FABs); everything else is shared.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    totalSeconds: Int = 5
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        AppSnackbar(snackbarData = data, isPersian = isPersian, totalSeconds = totalSeconds)
    }
}

@Composable
fun AppSnackbar(
    snackbarData: SnackbarData,
    isPersian: Boolean,
    totalSeconds: Int = 5
) {
    val visuals = snackbarData.visuals
    val isError = (visuals as? AppSnackbarVisuals)?.isError == true
    val hasAction = visuals.actionLabel != null
    val countsDown = hasAction || visuals.duration == SnackbarDuration.Indefinite
    val animatedProgress = remember { Animatable(1f) }

    // Undo / indefinite snackbars count down and dismiss themselves; the rest use the host's duration.
    LaunchedEffect(snackbarData) {
        if (countsDown) {
            animatedProgress.snapTo(1f)
            animatedProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = totalSeconds * 1000, easing = LinearEasing)
            )
            snackbarData.dismiss()
        }
    }

    val secondsLeft = ceil(animatedProgress.value * totalSeconds).toInt()
    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }
    val accent = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isError) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(22.dp)
                )
            }
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            visuals.actionLabel?.let { actionLabel ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { snackbarData.performAction() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                        CircularProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier.fillMaxSize(),
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = numberFormatter.format(secondsLeft),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
        }
    }
}