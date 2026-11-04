package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ساعت مشترک شفق.
 *
 * فاز همه‌ی حرکت‌ها از «زمان واقعی» محاسبه می‌شود، نه از لحظه‌ی ساخته‌شدن صفحه؛
 * بنابراین هنگام جابه‌جایی بین صفحه‌ها شفق از جای قبلی خودش ادامه می‌دهد و پرش نمی‌کند.
 * مقدار برگشتی فقط در فاز draw خوانده شود تا recomposition رخ ندهد.
 */
@Composable
internal fun rememberAuroraClockSeconds(enabled: Boolean): () -> Double {
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            androidx.compose.runtime.withFrameNanos { frameNanos = it }
        }
    }
    return {
        // خواندن frameNanos باعث بازرسم در هر فریم می‌شود؛ زمان از ساعت یکپارچه‌ی سیستم گرفته می‌شود.
        @Suppress("UNUSED_VARIABLE") val tick = frameNanos
        System.nanoTime() / 1_000_000_000.0
    }
}

private const val TWO_PI = (2.0 * PI)

/** موج نرمِ −۱..۱ با دوره‌ی [periodSec] ثانیه و فاز [phase] (۰..۱). */
private fun wave(t: Double, periodSec: Double, phase: Double = 0.0): Float =
    sin(TWO_PI * (t / periodSec + phase)).toFloat()

private fun waveCos(t: Double, periodSec: Double, phase: Double = 0.0): Float =
    cos(TWO_PI * (t / periodSec + phase)).toFloat()

/** موج ۰..۱ برای «نفس کشیدن» شدت نور. */
private fun breath(t: Double, periodSec: Double, phase: Double = 0.0): Float =
    wave(t, periodSec, phase) * 0.5f + 0.5f

/** یک لکه/پرده‌ی نور بیضوی با کشیدگی و کجی دلخواه. */
private fun DrawScope.auroraBlob(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    tilt: Float = 0f
) {
    val brush = Brush.radialGradient(
        colors = listOf(color.copy(alpha = alpha.coerceIn(0f, 1f)), Color.Transparent),
        center = center,
        radius = radius
    )
    rotate(degrees = tilt, pivot = center) {
        scale(scaleX = scaleX, scaleY = scaleY, pivot = center) {
            drawCircle(brush = brush, radius = radius, center = center)
        }
    }
}

@Composable
fun AuroraBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val backgroundColor = MaterialTheme.colorScheme.background
    // رگه‌های خنک‌تر برای حس واقعی‌تر شفق، هماهنگ با پالت اپ
    val teal = lerp(primary, Color(0xFF3FB6C4), 0.5f)
    val mint = lerp(primary, Color(0xFF7DF0B8), 0.5f)
    val violet = lerp(primary, Color(0xFF8E7CF0), 0.45f)

    val enabled = animationsEnabled()
    val clock = rememberAuroraClockSeconds(enabled)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // در حالت «انیمیشن خاموش» زمان ثابت می‌ماند و تصویر یک شفق ایستا است.
        val t = if (enabled) clock() else 0.0
        val w = size.width
        val h = size.height

        drawRect(color = backgroundColor)

        // ۱) هاله‌ی اصلی (سبز نعنایی)
        auroraBlob(
            center = Offset(w * (0.40f + 0.16f * wave(t, 23.0)), h * (0.35f + 0.08f * waveCos(t, 17.0))),
            radius = w * 0.8f, color = primary,
            alpha = 0.30f * (0.70f + 0.60f * breath(t, 9.0))
        )

        // ۲) هاله‌ی دوم (مریم‌گلی)، خلاف جهت اولی
        auroraBlob(
            center = Offset(w * (0.60f + 0.18f * waveCos(t, 29.0, 0.2)), h * (0.50f + 0.10f * wave(t, 21.0, 0.4))),
            radius = w * 0.7f, color = secondary,
            alpha = 0.24f * (0.65f + 0.70f * breath(t, 13.0, 0.33))
        )

        // ۳) پرده‌ی عمودی سبز-آبی (بالا)
        auroraBlob(
            center = Offset(w * (0.50f + 0.30f * wave(t, 31.0, 0.6)), h * (0.20f + 0.06f * waveCos(t, 19.0, 0.1))),
            radius = w * 0.55f, color = teal,
            alpha = 0.26f * (0.35f + 0.95f * breath(t, 11.0, 0.7)),
            scaleX = 0.55f, scaleY = 1.7f, tilt = 14f * wave(t, 27.0, 0.15)
        )

        // ۴) پرده‌ی نعنایی روشن سمت چپ، با کجیِ مخالف
        auroraBlob(
            center = Offset(w * (0.18f + 0.14f * waveCos(t, 25.0, 0.3)), h * (0.30f + 0.10f * wave(t, 18.0, 0.55))),
            radius = w * 0.50f, color = mint,
            alpha = 0.22f * (0.30f + 1.00f * breath(t, 8.0, 0.15)),
            scaleX = 0.45f, scaleY = 1.9f, tilt = -18f * wave(t, 22.0, 0.45)
        )

        // ۵) پرده‌ی بنفشِ ملایم سمت راست؛ لبه‌ی شفق را رنگی‌تر می‌کند
        auroraBlob(
            center = Offset(w * (0.82f + 0.12f * wave(t, 27.0, 0.8)), h * (0.26f + 0.08f * waveCos(t, 16.0, 0.65))),
            radius = w * 0.48f, color = violet,
            alpha = 0.16f * (0.25f + 1.05f * breath(t, 14.0, 0.5)),
            scaleX = 0.50f, scaleY = 1.8f, tilt = 20f * waveCos(t, 24.0, 0.2)
        )

        // ۶) پرتوی سبز-آبی در میانه‌ی صفحه که آرام می‌لغزد
        auroraBlob(
            center = Offset(w * (0.55f + 0.30f * waveCos(t, 35.0, 0.1)), h * (0.58f + 0.10f * wave(t, 26.0, 0.9))),
            radius = w * 0.60f, color = teal,
            alpha = 0.15f * (0.30f + 1.00f * breath(t, 12.0, 0.85)),
            scaleX = 1.3f, scaleY = 0.7f, tilt = -10f * wave(t, 30.0, 0.35)
        )

        // ۷) درخشش کم‌نور و تند پایین صفحه (چشمک نرم)
        auroraBlob(
            center = Offset(w * (0.30f + 0.25f * wave(t, 20.0, 0.7)), h * (0.80f + 0.06f * waveCos(t, 15.0, 0.4))),
            radius = w * 0.55f, color = mint,
            alpha = 0.12f * (0.20f + 1.10f * breath(t, 7.0, 0.6)),
            scaleX = 1.2f, scaleY = 0.8f
        )
    }
}