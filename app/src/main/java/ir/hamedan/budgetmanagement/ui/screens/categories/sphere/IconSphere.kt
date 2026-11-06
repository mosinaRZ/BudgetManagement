package ir.hamedan.budgetmanagement.ui.screens.categories.sphere

import android.graphics.Bitmap
import android.graphics.Paint
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** رشته‌های دسترس‌پذیری کره (فارسی/انگلیسی را صفحه‌ی میزبان می‌دهد). */
internal class IconSphereLabels(
    val description: String,
    val next: String,
    val previous: String
)

private const val ATLAS_PX = 128
private const val ICON_SIZE_FACTOR = 0.19f      // اندازه‌ی آیکون در عمق صفر نسبت به شعاع
private const val SIZE_MARGIN = 1.22f           // بیشینه‌ی گستردگی کره + نصف آیکون + حلقه‌ی انتخاب
private const val DRAG_GAIN = 1.15f             // حساسیت چرخش نسبت به حرکت انگشت
private const val FRICTION = 2.6f               // کندشدن نمایی چرخش بعد از رها کردن انگشت
private const val MIN_FLING_SPEED = 0.6f        // rad/s؛ کمتر از این رها کردن «ایستا» حساب می‌شود
private const val MAX_SPIN = 12f                // rad/s
private const val MIN_SPIN = 0.04f              // rad/s؛ زیر این مقدار چرخش متوقف می‌شود
private const val AUTO_SPEED = 0.42f            // rad/s؛ چرخش خودکارِ مورب وقتی چیزی انتخاب نشده
private const val FOCUS_MS = 520f
private val AUTO_AXIS = Vec3(0.5f, 1f, 0.28f).normalized()

/**
 * وضعیت چرخشِ کره. چرخش فقط یک کواترنیون است؛ هر بار تغییر کند فقط رسم دوباره انجام می‌شود،
 * نه recomposition (چون فقط داخل بلوک رسم خوانده می‌شود).
 */
@Stable
internal class IconSphereState(initialRotation: Quat) {

    var rotation by mutableStateOf(initialRotation)
        private set

    var isDragging by mutableStateOf(false)
        private set

    /** با هر رها کردنِ انگشت زیاد می‌شود تا حلقه‌ی فریم برای ادامه‌ی اینرسی بیدار شود. */
    var spinTick by mutableIntStateOf(0)
        private set

    var spinAxis: Vec3 = Vec3(0f, 1f, 0f)
        private set
    var spinSpeed: Float = 0f
        private set

    fun beginDrag() {
        isDragging = true
        spinSpeed = 0f
    }

    /** انگشت (dx, dy) پیکسل حرکت کرده: کره حول محور عمود بر حرکت می‌چرخد تا سطحش زیر انگشت بماند. */
    fun dragBy(dx: Float, dy: Float, radiusPx: Float) {
        val distance = hypot(dx, dy)
        if (distance < 1e-3f) return
        val angle = distance / radiusPx * DRAG_GAIN
        rotation = (Quat.fromAxisAngle(Vec3(dy, dx, 0f), angle) * rotation).normalized()
    }

    fun endDrag(velocityX: Float, velocityY: Float, radiusPx: Float) {
        isDragging = false
        val speed = hypot(velocityX, velocityY) / radiusPx * DRAG_GAIN
        if (speed > MIN_FLING_SPEED) {
            spinAxis = Vec3(velocityY, velocityX, 0f).normalized()
            spinSpeed = min(speed, MAX_SPIN)
        }
        spinTick++
    }

    fun cancelDrag() {
        isDragging = false
        spinTick++
    }

    fun stepInertia(dt: Float) {
        rotation = (Quat.fromAxisAngle(spinAxis, spinSpeed * dt) * rotation).normalized()
        spinSpeed *= exp(-FRICTION * dt)
        if (spinSpeed < MIN_SPIN) spinSpeed = 0f
    }

    fun stepAuto(dt: Float) {
        rotation = (Quat.fromAxisAngle(AUTO_AXIS, AUTO_SPEED * dt) * rotation).normalized()
    }

    fun stopSpin() {
        spinSpeed = 0f
    }

    fun rotateTo(value: Quat) {
        rotation = value
    }
}

private data class FocusRequest(val index: Int, val id: Int)

/**
 * کره‌ی سه‌بعدیِ آیکون‌ها.
 *
 *  - آیکون‌ها با الگوریتم فیبوناچی روی کره پخش شده‌اند و با پرسپکتیو رسم می‌شوند؛ هر چه به
 *    بیننده نزدیک‌تر، درشت‌تر و پررنگ‌تر، و هر چه دورتر، ریزتر و کم‌رنگ‌تر.
 *  - تا وقتی چیزی انتخاب نشده ([selected] == null) کره به‌آرامی حول یک محور مورب می‌چرخد.
 *  - کاربر با کشیدن در هر جهتی (افقی، عمودی، مورب یا هر زاویه‌ای) آن را می‌چرخاند و با رها کردنِ
 *    سریع، چرخش به‌صورت اینرسی ادامه می‌یابد.
 *  - لمس یک آیکونِ رو به بیننده آن را انتخاب می‌کند و کره آن را نرم به مرکز جلو می‌آورد.
 */
@Composable
internal fun IconSphere(
    icons: List<String>,
    selected: String?,
    accent: Color,
    labels: IconSphereLabels,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()

    val points = remember(icons) { SphereLayout.fibonacci(icons.size) }
    val projector = remember(points) { SphereProjector(points) }
    val selectedIndex = remember(icons, selected) { icons.indexOf(selected).takeIf { it >= 0 } }
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val latestOnSelect by rememberUpdatedState(onSelect)

    val state = remember {
        IconSphereState(
            Quat.fromAxisAngle(Vec3(1f, 0f, 0f), 0.5f) * Quat.fromAxisAngle(Vec3(0f, 0f, 1f), -0.45f)
        )
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var focusRequest by remember {
        mutableStateOf(selectedIndex?.let { FocusRequest(it, 0) })
    }

    val atlas by produceState(emptyMap<String, ImageBitmap>(), icons) {
        value = withContext(Dispatchers.Default) {
            icons.associateWith { renderEmojiBitmap(it, ATLAS_PX) }
        }
    }
    val appear by animateFloatAsState(
        targetValue = if (atlas.isNotEmpty()) 1f else 0f,
        animationSpec = tween(380),
        label = "sphereAppear"
    )

    val autoRotate = selected == null && !reduceMotion

    // حلقه‌ی فریم فقط وقتی کار می‌کند که چیزی برای حرکت‌دادن باشد (اینرسی یا چرخش خودکار)؛
    // در حالت ایستا هیچ فریمی درخواست نمی‌شود و باتری هدر نمی‌رود.
    LaunchedEffect(autoRotate, state.isDragging, state.spinTick) {
        if (state.isDragging) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive) {
            val spinning = state.spinSpeed > 0f
            if (!spinning && !autoRotate) break
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            if (spinning) state.stepInertia(dt) else state.stepAuto(dt)
        }
    }

    // آمدنِ آیکونِ انتخاب‌شده به مرکزِ جلو با کوتاه‌ترین چرخش.
    LaunchedEffect(focusRequest) {
        val request = focusRequest ?: return@LaunchedEffect
        val target = points.getOrNull(request.index) ?: return@LaunchedEffect
        state.stopSpin()
        val start = state.rotation
        val delta = Quat.fromTo(start.rotate(target), Vec3.Front)
        if (reduceMotion) {
            state.rotateTo((delta * start).normalized())
            return@LaunchedEffect
        }
        val begin = withFrameNanos { it }
        while (isActive && !state.isDragging) {
            val now = withFrameNanos { it }
            val t = ((now - begin) / (FOCUS_MS * 1_000_000f)).coerceIn(0f, 1f)
            state.rotateTo((delta.scaledBy(FastOutSlowInEasing.transform(t)) * start).normalized())
            if (t >= 1f) break
        }
    }

    val velocityTracker = remember { VelocityTracker() }
    val density = LocalDensity.current
    val hitSlop = with(density) { 6.dp.toPx() }
    val ringWidth = with(density) { 1.5.dp.toPx() }
    val outlineWidth = with(density) { 1.dp.toPx() }

    fun radiusPx(size: IntSize): Float =
        min(size.width, size.height) / 2f / SIZE_MARGIN

    fun select(index: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        latestOnSelect(icons[index])
        focusRequest = FocusRequest(index, (focusRequest?.id ?: 0) + 1)
    }

    val onSurface = scheme.onSurface

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .semantics {
                contentDescription = labels.description
                customActions = listOf(
                    CustomAccessibilityAction(labels.next) {
                        if (icons.isNotEmpty()) select(((latestSelectedIndex ?: -1) + 1).mod(icons.size))
                        true
                    },
                    CustomAccessibilityAction(labels.previous) {
                        if (icons.isNotEmpty()) select(((latestSelectedIndex ?: 0) - 1).mod(icons.size))
                        true
                    }
                )
            }
            .pointerInput(projector) {
                detectTapGestures { offset ->
                    val size = canvasSize
                    if (size == IntSize.Zero) return@detectTapGestures
                    val radius = radiusPx(size)
                    projector.project(state.rotation, size.width / 2f, size.height / 2f, radius)
                    val hit = projector.hitTest(
                        x = offset.x,
                        y = offset.y,
                        baseSize = radius * ICON_SIZE_FACTOR,
                        slop = hitSlop
                    )
                    if (hit >= 0) select(hit)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        state.beginDrag()
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val size = canvasSize
                        if (size != IntSize.Zero) state.dragBy(drag.x, drag.y, radiusPx(size))
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity()
                        val size = canvasSize
                        if (size != IntSize.Zero) {
                            state.endDrag(velocity.x, velocity.y, radiusPx(size))
                        } else {
                            state.cancelDrag()
                        }
                    },
                    onDragCancel = { state.cancelDrag() }
                )
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = min(size.width, size.height) / 2f / SIZE_MARGIN
        val baseSize = radius * ICON_SIZE_FACTOR
        val center = Offset(centerX, centerY)

        // هاله و کنتور کره.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.06f), Color.Transparent),
                center = center,
                radius = radius * 1.2f
            ),
            radius = radius * 1.2f,
            center = center
        )
        drawCircle(
            color = onSurface.copy(alpha = 0.07f),
            radius = radius,
            center = center,
            style = Stroke(width = outlineWidth)
        )

        projector.project(state.rotation, centerX, centerY, radius)
        val images = atlas
        for (k in 0 until projector.count) {
            val i = projector.order[k]
            val image = images[icons[i]] ?: continue
            val z = projector.depth[i]
            val alpha = SphereDepth.alpha(z) * appear
            val isSelected = i == selectedIndex
            val iconSize = baseSize * projector.scale[i] * if (isSelected) 1.2f else 1f
            val x = projector.screenX[i]
            val y = projector.screenY[i]

            if (isSelected) {
                val spot = Offset(x, y)
                drawCircle(accent.copy(alpha = 0.20f * alpha), radius = iconSize * 0.68f, center = spot)
                drawCircle(
                    accent.copy(alpha = 0.95f * alpha),
                    radius = iconSize * 0.68f,
                    center = spot,
                    style = Stroke(width = ringWidth)
                )
            }
            val s = iconSize / image.width
            withTransform({
                translate(left = x - iconSize / 2f, top = y - iconSize / 2f)
                scale(scaleX = s, scaleY = s, pivot = Offset.Zero)
            }) {
                drawImage(image = image, alpha = alpha)
            }
        }
    }
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * هر ایموجی یک‌بار به بیت‌مپ رسم می‌شود و بعد در هر فریم فقط با شفافیت/مقیاس مختلف روی Canvas
 * کشیده می‌شود. این‌طور شفافیتِ ایموجی‌های رنگی روی همه‌ی دستگاه‌ها دقیق است و ۱۲۰ متنِ ایموجی
 * در هر فریم دوباره چیده نمی‌شود.
 */
private fun renderEmojiBitmap(emoji: String, sizePx: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx * 0.80f
        textAlign = Paint.Align.CENTER
    }
    val metrics = paint.fontMetrics
    canvas.drawText(emoji, sizePx / 2f, sizePx / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
    return bitmap.asImageBitmap()
}