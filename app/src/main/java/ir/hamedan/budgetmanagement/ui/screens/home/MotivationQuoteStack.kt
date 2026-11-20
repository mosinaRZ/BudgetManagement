package ir.hamedan.budgetmanagement.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
//  تنظیمات اندازه‌ی کارت — برای کوچک/بزرگ کردن فقط همین بخش را عوض کنید.
//  (داخل پرانتز، مقدار نسخه‌ی اولِ کارت نوشته شده است.)
// ─────────────────────────────────────────────────────────────────────────────

/** ارتفاع اضافه‌ی کارت روی ارتفاع بلندترین جمله (۱۰۰dp) — مهم‌ترین عامل ارتفاع کارت. */
private val CARD_EXTRA_HEIGHT = 60.dp

/** اندازه‌ی متن (۱۶sp) و فاصله‌ی خطوط (۲۸sp). */
private val TEXT_SIZE = 15.sp
private val TEXT_LINE_HEIGHT = 26.sp

/** فاصله‌ی داخلی کارت (۲۲ / ۱۸ / ۱۶). */
private val HORIZONTAL_PADDING = 20.dp
private val PADDING_TOP = 16.dp
private val PADDING_BOTTOM = 14.dp

/** گردی گوشه‌ها (۲۸dp) و اندازه‌ی علامت نقل‌قول تزئینی (۱۲۰sp). */
private val CARD_CORNER = 24.dp
private val QUOTE_MARK_SIZE = 100.sp

/** فاصله‌ی عمودی کارت‌های زیرین (۱۰dp) و کوچک شدن هر پله (۰٫۰۵۵). */
private val PEEK_STEP = 9.dp
private const val PEEK_SCALE_STEP = 0.05f

/** ضخامت نوار زمان (۳dp). */
private val PROGRESS_HEIGHT = 3.dp

// ─────────────────────────────────────────────────────────────────────────────

/** تعداد کارت‌های ساخته‌شده در پشته (کارت بالا + دو کارت دیده‌شونده + یک کارت نامرئی که هنگام سوایپ محو می‌شود). */
private const val STACK_VISIBLE = 3

/** ضریب مسافت کشیدن (نسبت به عرض) که در آن کارت‌های زیرین کاملاً به جایگاه بعدی می‌رسند. */
private const val REVEAL_DISTANCE = 0.45f

/** آستانه‌ی رها کردن کارت (نسبت به عرض) و سرعت فلینگ (px/s). */
private const val DISMISS_FRACTION = 0.28f
private const val DISMISS_VELOCITY = 1100f

/** مدت ماندن هر کارت: به طول جمله وابسته است. */
private fun dwellMillis(text: String): Int = (2800 + text.length * 65).coerceIn(4500, 12000)

/**
 * پشته‌ی کارت‌های انگیزشی.
 *
 * - انتخاب جمله‌ها تصادفی و بدون تکرار است ([QuoteShuffler]): هر جمله که نمایش داده شد از
 *   لیست باقی‌مانده حذف می‌شود و جمله‌ی بعدی از میان بقیه انتخاب می‌شود؛ پس از نمایش همه‌ی
 *   جمله‌ها، یک دور تازه (باز هم تصادفی) آغاز می‌شود.
 * - با کشیدن به چپ/راست (یا به‌صورت خودکار) کارت بالا کج می‌شود، از کادر خارج می‌شود
 *   و کارت بعدی از زیر آن بالا می‌آید.
 * - زمان ماندن هر کارت متناسب با طول جمله است و هنگام لمس کاربر متوقف می‌شود.
 *
 * پارامتر [numberFormatter] برای سازگاری با فراخوانی‌های فعلی نگه داشته شده است.
 */
@Composable
fun MotivationQuoteStack(
    quotes: List<String>,
    isPersian: Boolean,
    numberFormatter: NumberFormat,
    onSwipeHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (quotes.isEmpty()) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // seed و شمارنده ذخیره می‌شوند؛ با همین دو، دنباله‌ی تصادفی بعد از چرخش صفحه
    // یا برگشت از اسکرول دقیقاً از همان نقطه بازسازی می‌شود.
    val seed = rememberSaveable { Random.nextLong() }
    var counter by rememberSaveable { mutableIntStateOf(0) }

    val shuffler = remember(quotes, seed) {
        QuoteShuffler(size = quotes.size, seed = seed, recentMemory = STACK_VISIBLE + 1)
    }
    // صف ایندکس جمله‌های در حال نمایش: [0] = کارت بالا
    val queue = remember(shuffler) {
        repeat(counter) { shuffler.next() }
        mutableStateListOf<Int>().apply { repeat(STACK_VISIBLE + 1) { add(shuffler.next()) } }
    }

    val offsetX = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    val textStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = TEXT_SIZE,
        fontWeight = FontWeight.Medium,
        lineHeight = TEXT_LINE_HEIGHT,
        textAlign = TextAlign.Center
    )
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat()
        val textWidthPx = with(density) { (maxWidth - HORIZONTAL_PADDING * 2).roundToPx() }

        // ارتفاع کارت از بلندترین جمله‌ی کل لیست محاسبه می‌شود تا هنگام تعویض، صفحه نپرد.
        val tallestTextPx = remember(quotes, textWidthPx, textStyle) {
            quotes.maxOf {
                measurer.measure(
                    text = AnnotatedString(it),
                    style = textStyle,
                    constraints = Constraints(maxWidth = textWidthPx.coerceAtLeast(1))
                ).size.height
            }
        }
        val cardHeight = with(density) { tallestTextPx.toDp() } + CARD_EXTRA_HEIGHT
        // فقط دو پله‌ی کارت‌های زیرین دیده می‌شود (پله‌ی سوم نامرئی است).
        val stackPeek = PEEK_STEP * (STACK_VISIBLE - 1)

        suspend fun flingAway(direction: Float) {
            isAnimating = true
            offsetX.animateTo(
                targetValue = direction * widthPx * 1.2f,
                animationSpec = tween(durationMillis = 360, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
            )
            // این بخش بدون هیچ توقفی تا فریم بعد اجرا می‌شود؛ پس جابه‌جایی بدون پرش است.
            progress.snapTo(0f)
            counter += 1
            queue.removeAt(0)
            queue.add(shuffler.next())
            offsetX.snapTo(0f)
            isAnimating = false
        }

        // تعویض خودکار؛ با لمس کاربر متوقف و از همان نقطه ادامه پیدا می‌کند.
        val dwellMs = dwellMillis(quotes[queue[0]])
        LaunchedEffect(counter, isDragging, isAnimating) {
            if (isDragging || isAnimating) return@LaunchedEffect
            val remaining = (dwellMs * (1f - progress.value)).toInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            isAnimating = true
            scope.launch { flingAway(-1f) }
        }

        val dragState = rememberDraggableState { delta ->
            scope.launch { offsetX.snapTo(offsetX.value + delta) }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + stackPeek)
        ) {
            // از عمیق‌ترین کارت تا کارت بالا رسم می‌شود تا ترتیب روی‌هم‌قرارگیری درست باشد.
            for (depth in STACK_VISIBLE downTo 0) {
                key(counter + depth) {
                    val cardModifier = if (depth == 0) {
                        Modifier.draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            enabled = !isAnimating,
                            onDragStarted = { isDragging = true },
                            onDragStopped = { velocity ->
                                val x = offsetX.value
                                val moved = abs(x) > widthPx * 0.05f
                                val byDistance = abs(x) > widthPx * DISMISS_FRACTION
                                val byVelocity = abs(velocity) > DISMISS_VELOCITY &&
                                        (!moved || sign(velocity) == sign(x))
                                if (byDistance || byVelocity) {
                                    val direction = if (moved) sign(x) else sign(velocity)
                                    onSwipeHaptic()
                                    isAnimating = true
                                    isDragging = false
                                    scope.launch { flingAway(direction) }
                                } else {
                                    isDragging = false
                                    scope.launch {
                                        offsetX.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        Modifier
                    }

                    QuoteCard(
                        text = quotes[queue[depth]],
                        depth = depth,
                        textStyle = textStyle,
                        height = cardHeight,
                        widthPx = widthPx,
                        offsetProvider = { offsetX.value },
                        progressProvider = { progress.value },
                        modifier = cardModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun QuoteCard(
    text: String,
    depth: Int,
    textStyle: TextStyle,
    height: Dp,
    widthPx: Float,
    offsetProvider: () -> Float,
    progressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CARD_CORNER)
    val primary = colors.primary

    // پس‌زمینه‌ی کاملاً مات (تا کارت‌های زیرین از آن دیده نشوند) با رنگ‌آمیزی ملایم برند
    val gradient = Brush.linearGradient(
        listOf(
            lerpColor(colors.surfaceVariant, primary, 0.18f),
            lerpColor(colors.surface, primary, 0.05f)
        )
    )
    val borderBrush = Brush.linearGradient(
        listOf(primary.copy(alpha = 0.55f), colors.outline.copy(alpha = 0.12f), primary.copy(alpha = 0.28f))
    )
    val elevation = when (depth) {
        0 -> 18.dp
        1 -> 10.dp
        2 -> 5.dp
        else -> 0.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                val x = offsetProvider()
                val p = (abs(x) / (widthPx * REVEAL_DISTANCE)).coerceIn(0f, 1f)
                if (depth == 0) {
                    // کارت بالا: کج شدن به سمت کشیدن و خروج از کادر
                    val fraction = (x / widthPx).coerceIn(-1.4f, 1.4f)
                    transformOrigin = TransformOrigin(0.5f, 1.35f)
                    translationX = x
                    translationY = -abs(x) * 0.03f
                    rotationZ = fraction * 16f
                    alpha = (1f - (abs(x) / widthPx - 0.75f) / 0.45f).coerceIn(0f, 1f)
                } else {
                    // کارت‌های زیرین: هم‌زمان با کشیدن، یک پله بالا می‌آیند
                    val d = depth - p
                    val s = 1f - PEEK_SCALE_STEP * d
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleX = s
                    scaleY = s
                    translationY = PEEK_STEP.toPx() * d
                    alpha = (STACK_VISIBLE - d).coerceIn(0f, 1f)
                }
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = primary.copy(alpha = 0.45f)
            )
            .background(gradient, shape)
            .border(1.dp, borderBrush, shape)
            .drawBehind {
                // درخشش ملایم گوشه‌ی بالا
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.88f, 0f),
                        radius = size.width * 0.75f
                    )
                )
            }
            .drawWithContent {
                drawContent()
                // کارت‌های عمیق‌تر کمی محوتر دیده می‌شوند
                if (depth > 0) {
                    val p = (abs(offsetProvider()) / (widthPx * REVEAL_DISTANCE)).coerceIn(0f, 1f)
                    val d = depth - p
                    drawRoundRect(
                        color = colors.surface.copy(alpha = (0.18f * d).coerceIn(0f, 0.6f)),
                        cornerRadius = CornerRadius(CARD_CORNER.toPx())
                    )
                }
            }
    ) {
        // علامت نقل‌قول تزئینی
        Text(
            text = "\u201C",
            fontFamily = FontFamily.Serif,
            fontSize = QUOTE_MARK_SIZE,
            color = primary.copy(alpha = 0.09f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp, y = (-6).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = HORIZONTAL_PADDING,
                    end = HORIZONTAL_PADDING,
                    top = PADDING_TOP,
                    bottom = PADDING_BOTTOM
                )
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = textStyle,
                    color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // نوار باریک زمان باقی‌مانده‌ی کارت
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PROGRESS_HEIGHT)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.10f))
                    .drawBehind {
                        if (depth == 0) {
                            val w = size.width * progressProvider().coerceIn(0f, 1f)
                            val left = if (layoutDirection == LayoutDirection.Rtl) size.width - w else 0f
                            drawRoundRect(
                                color = primary.copy(alpha = 0.85f),
                                topLeft = Offset(left, 0f),
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(size.height / 2f)
                            )
                        }
                    }
            )
        }
    }
}