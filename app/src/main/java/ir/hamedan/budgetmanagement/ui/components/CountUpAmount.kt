package ir.hamedan.budgetmanagement.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * حافظه‌ی سراسری (در طول عمر پروسه) آخرین مبلغی که برای هر «کلید» نمایش داده شده است.
 *
 * - اولین باری که یک مبلغ در این اجرای برنامه نمایش داده می‌شود: شمارش از «صفر» تا مقدار نهایی.
 * - دفعات بعد که مقدار عوض می‌شود (تراکنش جدید، ویرایش، حذف و …): شمارش از «مقدار قبلی» تا مقدار جدید.
 * - اگر مقدار تغییر نکرده باشد (مثلاً برگشتن به یک صفحه): بدون انیمیشن و فوراً نمایش داده می‌شود.
 */
object AmountCountMemory {
    private val last = HashMap<String, Long>()

    @Synchronized
    fun get(key: String): Long? = last[key]

    @Synchronized
    fun put(key: String, value: Long) {
        last[key] = value
    }

    /** هنگام خروج از حساب صدا زده شود تا مبلغ‌های کاربر قبلی «مقدار قبلی» کاربر بعدی نشوند. */
    @Synchronized
    fun clear() = last.clear()
}

/** وضعیت انیمیشن یک مبلغ؛ فقط در فاز draw/layout خوانده می‌شود تا recomposition غیرضروری رخ ندهد. */
@Stable
class CountUpState internal constructor(initialValue: Long) {
    /** مقدار فعلی نمایش‌داده‌شده (بین مقدار شروع و مقدار نهایی). */
    var value by mutableLongStateOf(initialValue)
        internal set

    /** پیشرفت (۰..۱) پس از اعمال easing. */
    var progress by mutableFloatStateOf(1f)
        internal set

    /** مقیاس لحظه‌ای برای افکت «ضربان». */
    internal var scale by mutableFloatStateOf(1f)

    /** شفافیت لحظه‌ای. */
    internal var alpha by mutableFloatStateOf(1f)

    /** جابه‌جایی عمودی نرمالایز شده (۱ = حداکثر)، جهت = شمارش رو به بالا یا پایین. */
    internal var lift by mutableFloatStateOf(0f)

    val isRunning: Boolean get() = progress < 1f
}

/**
 * ویژگی‌های OpenType مناسب برای نمایش مبلغ.
 *
 * `tnum` فقط برای ارقام لاتین (Inter) مفید است. در وزیرمتن، `tnum` همهٔ ارقام فارسی را به عرض
 * ثابت ۱۳۴۹ واحد می‌رساند (مثلاً «۱» از ۵۹۱ به ۱۳۴۹)؛ همین باعث فاصلهٔ زیاد بین ارقام می‌شد.
 * برای فارسی از ارقام تناسبی پیش‌فرض فونت استفاده می‌کنیم.
 */
fun amountFontFeatures(isPersian: Boolean): String? = if (isPersian) null else "tnum"

/**
 * بدترین حالت عرض برای اندازه‌گیری: در فونت فارسی عریض‌ترین رقم (۳) جایگزین همهٔ ارقام می‌شود
 * تا هنگام شمارش، هیچ مقدار میانی از عرض اندازه‌گیری‌شده بیشتر نشود.
 */
fun widestAmountText(text: String, isPersian: Boolean): String {
    if (!isPersian) return text
    val sb = StringBuilder(text.length)
    for (ch in text) sb.append(if (ch in '\u06F0'..'\u06F9' || ch in '0'..'9') '\u06F3' else ch)
    return sb.toString()
}

private val CountEasing = CubicBezierEasing(0.22f, 0.85f, 0.2f, 1f)

private fun digitsOf(value: Long): Int = abs(value).toString().length

/** اگر کاربر انیمیشن‌های سیستم را خاموش کرده باشد (Remove animations) false برمی‌گرداند. */
@Composable
internal fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
}

/**
 * مقدار هدف را به‌صورت شمارنده‌ی انیمیشنی برمی‌گرداند.
 *
 * @param target مبلغ نهایی (علامت‌دار؛ فرمت‌دهی با caller است).
 * @param key شناسه‌ی پایدار این مبلغ (مثلاً "home.balance" یا "tx.<id>").
 * @param durationScale ضریب مدت؛ برای ردیف‌های لیست کمتر از ۱ بدهید.
 */
@Composable
fun rememberCountUpAmount(
    target: Long,
    key: String,
    durationScale: Float = 1f
): CountUpState {
    val enabled = animationsEnabled()
    val state = remember(key) { CountUpState(AmountCountMemory.get(key) ?: 0L) }

    LaunchedEffect(key, target, enabled) {
        val from = state.value

        if (!enabled || from == target) {
            state.value = target
            state.progress = 1f
            state.scale = 1f
            state.alpha = 1f
            state.lift = 0f
            AmountCountMemory.put(key, target)
            return@LaunchedEffect
        }

        val fromZero = from == 0L
        val direction = if (target >= from) 1f else -1f
        val delta = target - from
        val digits = max(digitsOf(delta), 1)
        val totalMs = ((600 + 95 * digits) * durationScale).toInt().coerceIn(450, 1700)
        val startAlpha = if (fromZero) 0.45f else 0.72f

        val driver = Animatable(0f)
        driver.animateTo(1f, tween(durationMillis = totalMs, easing = CountEasing)) {
            val p = this.value
            // ۱) خود عدد
            state.progress = p
            state.value = from + (delta.toDouble() * p).roundToLong()
            AmountCountMemory.put(key, state.value)
            // ۲) محو شدن ملایم در ابتدای شمارش
            state.alpha = startAlpha + (1f - startAlpha) * (p / 0.22f).coerceIn(0f, 1f)
            // ۳) بالا آمدن / پایین رفتن عدد در جهت شمارش
            state.lift = direction * (1f - (p / 0.55f).coerceIn(0f, 1f))
            // ۴) ضربان: بزرگ شدن سریع، نگه داشتن، و تمام شدن با فنر
            val swell = (p / 0.12f).coerceIn(0f, 1f)
            state.scale = 1f + 0.045f * swell
        }

        // پایان: عدد دقیقاً روی مقدار نهایی و یک «نشستن» فنری
        state.value = target
        state.progress = 1f
        state.alpha = 1f
        state.lift = 0f
        AmountCountMemory.put(key, target)

        val settle = Animatable(state.scale)
        settle.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) { state.scale = this.value }
        state.scale = 1f
    }

    return state
}

/** افکت بصری شمارش (مقیاس، شفافیت، جابه‌جایی). فقط در فاز draw اجرا می‌شود. */
fun Modifier.countUpEffect(
    state: CountUpState,
    pivot: TransformOrigin = TransformOrigin.Center
): Modifier = graphicsLayer {
    transformOrigin = pivot
    scaleX = state.scale
    scaleY = state.scale
    alpha = state.alpha
    translationY = state.lift * 7.dp.toPx()
}

/**
 * متن مبلغِ شمارنده. `format` مقدار لحظه‌ای را به رشته‌ی نهایی (با جداکننده، علامت و واحد) تبدیل می‌کند.
 */
@Composable
fun CountUpText(
    target: Long,
    key: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    softWrap: Boolean = false,
    overflow: TextOverflow = TextOverflow.Clip,
    durationScale: Float = 1f,
    pivot: TransformOrigin = TransformOrigin.Center,
    format: (Long) -> String
) {
    val state = rememberCountUpAmount(target, key, durationScale)
    val isPersian = isPersianLocale()
    Text(
        text = format(state.value),
        modifier = modifier.countUpEffect(state, pivot),
        style = style.copy(fontFeatureSettings = amountFontFeatures(isPersian)),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow
    )
}

/** کمک‌تابع برای جاهایی که کل رشته‌ی نهایی از قبل ساخته شده و فقط باید عدد وسطش شمرده شود. */
@Composable
fun rememberCountUpValue(target: Long, key: String, durationScale: Float = 1f): Long =
    rememberCountUpAmount(target, key, durationScale).value

/**
 * دو مبلغ هم‌زمان (مثلاً «پس‌انداز / هدف») که هر دو شمرده می‌شوند و در یک متن فرمت می‌شوند.
 * خواندن state فقط داخل همین composable است تا کل صفحه در هر فریم recompose نشود.
 */
@Composable
fun CountUpPairText(
    first: Long,
    firstKey: String,
    second: Long,
    secondKey: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    softWrap: Boolean = false,
    overflow: TextOverflow = TextOverflow.Clip,
    durationScale: Float = 1f,
    pivot: TransformOrigin = TransformOrigin.Center,
    format: (Long, Long) -> String
) {
    val a = rememberCountUpAmount(first, firstKey, durationScale)
    val b = rememberCountUpAmount(second, secondKey, durationScale)
    val isPersian = isPersianLocale()
    Text(
        text = format(a.value, b.value),
        modifier = modifier.countUpEffect(a, pivot),
        style = style.copy(fontFeatureSettings = amountFontFeatures(isPersian)),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow
    )
}