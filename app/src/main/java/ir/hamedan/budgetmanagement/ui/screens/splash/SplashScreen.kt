package ir.hamedan.budgetmanagement.ui.screens.splash

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.delay

// همان رنگ پس‌زمینه‌ی فریم‌های welcoming.json (اندازه‌گیری شده: ‎#125251)
private val SplashBackground = Color(0xFF125251)

// اگر هاله نسبت به انیمیشن زودتر/دیرتر دیده شد، همین مقدار (ثانیه) را تنظیم کن
private const val HALO_SYNC_OFFSET_SEC = 0f

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val currentOnFinished by rememberUpdatedState(onAnimationFinished)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcoming))

    // ورژن برنامه (همان منطق قبلی)
    val versionName = remember(context, isPersian) {
        val rawVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        if (isPersian) {
            rawVersion
                .replace('0', '۰').replace('1', '۱').replace('2', '۲').replace('3', '۳').replace('4', '۴')
                .replace('5', '۵').replace('6', '۶').replace('7', '۷').replace('8', '۸').replace('9', '۹')
        } else rawVersion
    }

    // انیمیشن فقط یک بار پخش می‌شود (هاله هم فقط یک بار اجرا می‌شود)؛ پایان انیمیشن = پایان اسپلش
    val animState = animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    var finished by remember { mutableStateOf(false) }
    fun finishOnce() {
        if (!finished) {
            finished = true
            currentOnFinished()
        }
    }

    LaunchedEffect(composition, animState.isAtEnd) {
        if (composition != null && animState.isAtEnd) finishOnce()
    }
    // اگر به هر دلیل انیمیشن لود نشد، اسپلش برای همیشه نمی‌ماند
    LaunchedEffect(Unit) {
        delay(8000)
        finishOnce()
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            val message = if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground)
    ) {
        // ۱) هاله‌ی تمام‌صفحه؛ زمانش مستقیم از پیشرفت خود انیمیشن Lottie گرفته می‌شود
        Canvas(modifier = Modifier.fillMaxSize()) {
            val durationSec = (composition?.duration ?: 0f) / 1000f
            val timeSec = animState.progress * durationSec + HALO_SYNC_OFFSET_SEC
            val brush = HaloSweep.brush(timeSec, size.width, size.height)
            if (brush != null) drawRect(brush)
        }

        // ۲) خود انیمیشن؛ لبه‌هایش نرم محو می‌شود تا هاله‌ی داخل فریم‌ها با هاله‌ی صفحه بدون درز ترکیب شود
        LottieAnimation(
            composition = composition,
            progress = { animState.progress },
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .featherEdges()
        )

        Text(
            text = if (isPersian) "نسخه $versionName" else "Version $versionName",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        )
    }
}

/** لبه‌های کادر انیمیشن را نرم محو می‌کند (ماسک آلفا). */
private fun Modifier.featherEdges(fraction: Float = 0.14f): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val stops = arrayOf(
            0f to Color.Transparent,
            fraction to Color.Black,
            1f - fraction to Color.Black,
            1f to Color.Transparent
        )
        drawRect(Brush.horizontalGradient(*stops), blendMode = BlendMode.DstIn)
        drawRect(Brush.verticalGradient(*stops), blendMode = BlendMode.DstIn)
    }

/**
 * هاله‌ی نوری که در ثانیه‌های ۲٫۸ تا ۴٫۴ انیمیشن welcoming.json از گوشه‌ی پایین‌چپ
 * به گوشه‌ی بالاراست صفحه حرکت می‌کند.
 *
 * همه‌ی اعداد از تحلیل فریم‌به‌فریم خود welcoming.json (۱۵۰ فریم) درآمده‌اند:
 *  - محور حرکت: u = x/W + (1 − y/H)   (۰ = پایین‌چپ ... ۲ = بالاراست)
 *  - موقعیت لبه‌ی عقب و جلوی هاله در هر لحظه (کلیدفریم‌های TRAIL و LEAD)
 *  - رنگ هاله: سبز نعنایی روشن با آلفای حدود ۳٪ (اختلاف رنگ اندازه‌گیری‌شده‌ی فریم‌ها)
 */
private object HaloSweep {
    private val HALO_COLOR = Color(0xFF6DF7D9)
    private const val PLATEAU_ALPHA = 0.03f     // شدت هاله؛ کم/زیادش کن
    private const val SOFTNESS = 0.25f          // نیمه‌پهنای نرمی لبه‌ها (واحد u)
    private const val U_MIN = -0.3f
    private const val U_MAX = 2.3f

    // (زمان به ثانیه، مقدار)
    private val TRAIL = arrayOf(
        2.80f to -0.05f, 3.00f to -0.05f, 3.10f to 0.08f, 3.23f to 0.15f, 3.30f to 0.21f,
        3.37f to 0.28f, 3.50f to 0.42f, 3.63f to 0.55f, 3.76f to 0.72f, 3.89f to 1.00f,
        4.03f to 1.20f, 4.16f to 1.35f, 4.29f to 1.45f, 4.38f to 1.90f
    )
    private val LEAD = arrayOf(
        2.80f to 1.00f, 3.04f to 1.00f, 3.23f to 1.07f, 3.37f to 1.20f, 3.50f to 1.42f,
        3.63f to 1.55f, 3.76f to 1.65f, 3.89f to 1.72f, 4.03f to 1.78f, 4.16f to 1.82f,
        4.29f to 1.85f, 4.38f to 1.90f
    )
    private val STRENGTH = arrayOf(
        2.77f to 0.00f, 2.84f to 0.70f, 3.00f to 1.00f, 3.60f to 0.87f, 3.90f to 0.76f,
        4.10f to 0.70f, 4.29f to 0.50f, 4.38f to 0.00f
    )

    private fun interp(keys: Array<Pair<Float, Float>>, t: Float): Float {
        if (t <= keys.first().first) return keys.first().second
        if (t >= keys.last().first) return keys.last().second
        for (i in 0 until keys.size - 1) {
            val (t0, v0) = keys[i]
            val (t1, v1) = keys[i + 1]
            if (t in t0..t1) return v0 + (v1 - v0) * ((t - t0) / (t1 - t0))
        }
        return keys.last().second
    }

    fun brush(t: Float, w: Float, h: Float): Brush? {
        if (t < 2.7f || t > 4.5f || w <= 0f || h <= 0f) return null
        val strength = interp(STRENGTH, t)
        if (strength <= 0.001f) return null

        val trail = interp(TRAIL, t)
        val lead = interp(LEAD, t)
        val solid = HALO_COLOR.copy(alpha = PLATEAU_ALPHA * strength)
        val clear = HALO_COLOR.copy(alpha = 0f)   // هم‌رنگ، تا لبه‌ها تیره نشوند

        // گرادیان خطی که t_gradient = (u − U_MIN)/(U_MAX − U_MIN) شود
        val g = Offset(1f / w, -1f / h)
        val g2 = g.x * g.x + g.y * g.y
        val origin = Offset(0f, h)                // u = 0 در گوشه‌ی پایین‌چپ
        val start = origin + g * (U_MIN / g2)
        val end = origin + g * (U_MAX / g2)

        fun pos(u: Float) = ((u - U_MIN) / (U_MAX - U_MIN)).coerceIn(0f, 1f)
        val p1 = pos(trail - SOFTNESS)
        val p2 = maxOf(pos(trail + SOFTNESS), p1)
        val p3 = maxOf(pos(lead - SOFTNESS), p2)
        val p4 = maxOf(pos(lead + SOFTNESS), p3)

        return Brush.linearGradient(
            0f to clear, p1 to clear, p2 to solid, p3 to solid, p4 to clear, 1f to clear,
            start = start, end = end
        )
    }
}