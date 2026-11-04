package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.sin

private const val SB_TWO_PI = 2.0 * PI

private fun sbWave(t: Double, period: Double, phase: Double = 0.0): Float =
    sin(SB_TWO_PI * (t / period + phase)).toFloat()

private fun sbBreath(t: Double, period: Double, phase: Double = 0.0): Float =
    sbWave(t, period, phase) * 0.5f + 0.5f

@Composable
fun StatusBarAuroraBackground() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val backgroundColor = MaterialTheme.colorScheme.background
    val teal = lerp(primaryColor, Color(0xFF3FB6C4), 0.5f)
    val mint = lerp(primaryColor, Color(0xFF7DF0B8), 0.5f)
    val violet = lerp(primaryColor, Color(0xFF8E7CF0), 0.45f)

    val enabled = animationsEnabled()
    val clock = rememberAuroraClockSeconds(enabled)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = if (enabled) clock() else 0.0
        val w = size.width
        val oneThirdHeight = size.height / 3f

        // ۱. پر کردن کل صفحه با رنگ پس‌زمینه
        drawRect(color = backgroundColor)

        // شدت کلی شفق: ۰٫۷۸ تا ۱٫۱۸ (نور بالا و پایین می‌شود)
        val intensity = 0.78f + 0.40f * sbBreath(t, 8.0)

        // ۲. شفق اصلی در یک‌سوم بالای صفحه
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = (0.85f * intensity).coerceIn(0f, 1f)),
                    primaryColor.copy(alpha = (0.35f * intensity).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                startY = 0f,
                endY = oneThirdHeight
            ),
            topLeft = Offset.Zero,
            size = Size(width = w, height = oneThirdHeight)
        )

        // ۳. لایه‌ی مکمل با نفس کشیدنِ جداگانه (خلاف فاز لایه‌ی اول) برای عمق بیشتر
        val intensity2 = 0.70f + 0.60f * sbBreath(t, 12.0, 0.5)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = (secondaryColor.alpha * intensity2).coerceIn(0f, 1f)),
                    secondaryColor.copy(alpha = (0.15f * intensity2).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                startY = 0f,
                endY = oneThirdHeight
            ),
            topLeft = Offset.Zero,
            size = Size(width = w, height = oneThirdHeight)
        )

        // ۴. چند پرتوی رنگیِ لغزنده در نوار بالا؛ هر کدام با سرعت، فاز و نفسِ جدا
        clipRect(left = 0f, top = 0f, right = w, bottom = oneThirdHeight) {
            val beams = listOf(
                Triple(teal, 0.40f, doubleArrayOf(21.0, 0.00, 10.0)),
                Triple(mint, 0.34f, doubleArrayOf(26.0, 0.35, 8.0)),
                Triple(violet, 0.24f, doubleArrayOf(32.0, 0.70, 13.0)),
                Triple(primaryColor, 0.30f, doubleArrayOf(18.0, 0.55, 9.0)),
                Triple(teal, 0.26f, doubleArrayOf(37.0, 0.15, 11.0))
            )
            beams.forEachIndexed { index, (beamColor, baseAlpha, cfg) ->
                val period = cfg[0]
                val phase = cfg[1]
                val glow = 0.25f + 0.85f * sbBreath(t, cfg[2], phase)
                val center = Offset(
                    x = w * (0.5f + 0.42f * sbWave(t, period, phase)),
                    y = oneThirdHeight * (0.16f + 0.10f * sbWave(t, 14.0 + index * 2.0, phase + 0.3))
                )
                val radius = w * (0.40f + 0.06f * index)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(beamColor.copy(alpha = (baseAlpha * glow).coerceIn(0f, 1f)), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
    }
}