package ir.hamedan.budgetmanagement.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hamedan.budgetmanagement.ui.components.animationsEnabled
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * لایه‌ی تور معرفی برنامه: پرده‌ی تیره با یک «سوراخ» دورِ بخشِ در حال معرفی، حلقه‌ی ضربان‌دار،
 * انگشت و موج لمس برای نمایش کلیک، و کارت توضیح که همیشه سمتِ خلوت‌تر هدف می‌نشیند.
 *
 * تمام لمس‌های صفحه‌ی زیرین را می‌گیرد تا کاربر وسط تور ناخواسته چیزی را فعال نکند؛
 * فقط دکمه‌های خودِ کارت (قبلی، بعدی، رد کردن) و دکمه‌ی بازگشت سیستم کار می‌کنند.
 */
@Composable
fun TourOverlay(isPersian: Boolean) {
    if (TourController.isRunning) TourOverlayContent(isPersian)
}

private val SpotlightPadding = 8.dp
private val SpotlightRadius = 22.dp

@Composable
private fun TourOverlayContent(isPersian: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val animate = animationsEnabled()

    val step = TourController.currentStep
    val index = TourController.stepIndex
    val total = TourController.steps.size

    BackHandler(enabled = true) {
        if (index > 0) TourController.back()
    }

    var origin by remember { mutableStateOf(Offset.Zero) }
    var cardHeightPx by remember { mutableIntStateOf(0) }
    var lastRect by remember { mutableStateOf<Rect?>(null) }

    // مختصات هدف از پنجره می‌آید؛ نسبت به مبدأ همین لایه سنجیده می‌شود.
    val rawRect = step.target?.let { TourController.targets[it] }
    val localRect: Rect? = rawRect?.translate(-origin.x, -origin.y)
    LaunchedEffect(localRect) { if (localRect != null) lastRect = localRect }

    // اگر به هر دلیل هدف پیدا نشد، تور گیر نمی‌کند: کارت بدون اسپات‌لایت و وسط صفحه نشان داده می‌شود.
    var waited by remember(step.id) { mutableStateOf(false) }
    LaunchedEffect(step.id) {
        waited = false
        delay(1500)
        waited = true
    }
    val cardVisible = step.target == null || localRect != null || waited

    val animatedRect by animateRectAsState(
        targetValue = localRect ?: lastRect ?: Rect.Zero,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
    )
    val spotAlpha by animateFloatAsState(
        targetValue = if (localRect != null) 1f else 0f,
        animationSpec = tween(260),
        label = "tourSpotAlpha"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = tween(220),
        label = "tourCardAlpha"
    )

    val infinite = rememberInfiniteTransition(label = "tourInfinite")
    val pulseAnimated by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "tourPulse"
    )
    val tapAnimated by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing), RepeatMode.Restart),
        label = "tourTap"
    )
    val pulse = if (animate) pulseAnimated else 0.35f
    val tap = if (animate) tapAnimated else 0.5f

    val primary = scheme.primary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            // هر لمسی که به این لایه برسد همین‌جا مصرف می‌شود و به صفحه‌ی زیرین نمی‌رسد.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val heightPx = constraints.maxHeight.toFloat()
        val paddingPx = with(density) { SpotlightPadding.toPx() }
        val radiusPx = with(density) { SpotlightRadius.toPx() }

        // ───────── پرده و سوراخ ─────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.80f))

            if (spotAlpha > 0.01f && animatedRect.width > 0f) {
                // کادر هرگز از لبه‌های صفحه بیرون نمی‌زند (حاشیه‌ی امن ۶dp).
                val edge = 6.dp.toPx()
                val padded = animatedRect.inflate(paddingPx)
                val hole = Rect(
                    left = max(padded.left, edge),
                    top = max(padded.top, edge),
                    right = min(padded.right, size.width - edge),
                    bottom = min(padded.bottom, size.height - edge)
                )
                val corner = CornerRadius(min(radiusPx, min(hole.width, hole.height) / 2f))
                drawRoundRect(
                    color = Color.Black,
                    topLeft = hole.topLeft,
                    size = hole.size,
                    cornerRadius = corner,
                    alpha = spotAlpha,
                    blendMode = BlendMode.Clear
                )
                // حلقه‌ی ثابت دور سوراخ
                drawRoundRect(
                    color = primary.copy(alpha = 0.9f * spotAlpha),
                    topLeft = hole.topLeft,
                    size = hole.size,
                    cornerRadius = corner,
                    style = Stroke(width = 2.dp.toPx())
                )
                // حلقه‌ی ضربان‌دار که به بیرون پخش می‌شود
                val grow = pulse * 16.dp.toPx()
                drawRoundRect(
                    color = primary.copy(alpha = (1f - pulse) * 0.55f * spotAlpha),
                    topLeft = Offset(hole.left - grow, hole.top - grow),
                    size = Size(hole.width + grow * 2f, hole.height + grow * 2f),
                    cornerRadius = CornerRadius(corner.x + grow),
                    style = Stroke(width = 3.dp.toPx())
                )

                // موج لمس
                if (step.gesture == TourGesture.TAP) {
                    val anchor = Offset(
                        animatedRect.left + animatedRect.width * step.handX,
                        animatedRect.top + animatedRect.height * step.handY
                    )
                    val rp = ((tap - 0.38f) / 0.45f).coerceIn(0f, 1f)
                    if (tap > 0.38f && rp < 1f) {
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - rp) * 0.7f * spotAlpha),
                            radius = 8.dp.toPx() + rp * 34.dp.toPx(),
                            center = anchor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = primary.copy(alpha = (1f - rp) * 0.35f * spotAlpha),
                            radius = 8.dp.toPx() + rp * 34.dp.toPx(),
                            center = anchor
                        )
                    }
                }
            }
        }

        // ───────── کارت توضیح ─────────
        val topSafe = WindowInsets.statusBars.getTop(density).toFloat() + with(density) { 12.dp.toPx() }
        val bottomSafe = WindowInsets.navigationBars.getBottom(density).toFloat() + with(density) { 12.dp.toPx() }
        val gapPx = with(density) { 14.dp.toPx() }
        val cardH = if (cardHeightPx > 0) cardHeightPx.toFloat() else with(density) { 230.dp.toPx() }
        val maxY = max(topSafe, heightPx - bottomSafe - cardH)

        val targetY: Float = run {
            val rect = localRect
            if (rect == null) {
                (heightPx - cardH) / 2f
            } else {
                val below = rect.bottom + paddingPx + gapPx
                val above = rect.top - paddingPx - gapPx - cardH
                val spaceBelow = heightPx - bottomSafe - below
                val spaceAbove = above - topSafe + cardH
                when {
                    spaceBelow >= cardH && spaceBelow >= spaceAbove -> below
                    spaceAbove >= cardH -> above
                    spaceBelow >= cardH -> below
                    // هدف بلند است: کارت پایین صفحه می‌نشیند و بخش بالای هدف دیده می‌شود.
                    else -> maxY
                }
            }
        }.coerceIn(topSafe, maxY)

        val cardY by animateFloatAsState(
            targetValue = targetY,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
            label = "tourCardY"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .onSizeChanged { cardHeightPx = it.height }
                .offset { IntOffset(0, cardY.roundToInt()) }
                .graphicsLayer { alpha = cardAlpha }
        ) {
            AnimatedContent(
                targetState = index,
                transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                label = "tourCardContent"
            ) { cardIndex ->
                val cardStep = TourController.steps[cardIndex.coerceIn(0, total - 1)]
                TourCard(
                    step = cardStep,
                    index = cardIndex,
                    total = total,
                    isPersian = isPersian,
                    onBack = { TourController.back() },
                    onNext = { TourController.next(context) },
                    onSkip = { TourController.finish(context) }
                )
            }
        }
    }
}

// ═════════════════════════════ کارت توضیح ═════════════════════════════

@Composable
private fun TourCard(
    step: TourStep,
    index: Int,
    total: Int,
    isPersian: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(28.dp)
    val buttonShape = RoundedCornerShape(16.dp)
    val numberFormat = remember(isPersian) {
        NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }
    val isFirst = index == 0
    val isLast = index == total - 1
    val progress by animateFloatAsState(
        targetValue = (index + 1f) / total,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "tourProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, cardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(cardShape)
            .background(scheme.surface)
            .border(1.dp, scheme.primary.copy(alpha = 0.30f), cardShape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(scheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stepIcon(step),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.section(isPersian),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
                Text(
                    text = step.title(isPersian),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
            }
            Text(
                text = "${numberFormat.format(index + 1)} / ${numberFormat.format(total)}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }

        Text(
            text = step.body(isPersian),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )

        TourLocator(step = step, isPersian = isPersian)

        // نوار پیشرفت کلی تور
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(scheme.onSurface.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary)))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isLast) {
                Text(
                    text = if (isPersian) "رد کردن" else "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onSkip)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))

            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(buttonShape)
                        .background(scheme.surfaceVariant.copy(alpha = 0.8f))
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = if (isPersian) "قبلی" else "Back",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(buttonShape)
                    .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary)))
                    .clickable(role = Role.Button, onClick = onNext)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isLast && isPersian -> "شروع استفاده"
                            isLast -> "Start using Cidna"
                            isFirst && isPersian -> "شروع تور"
                            isFirst -> "Start the tour"
                            isPersian -> "بعدی"
                            else -> "Next"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


// ═════════════════════════════ نقشه‌ی محل امکانات ═════════════════════════════

private enum class DockSlot { HOME, TRANSACTIONS, ANALYTICS, SETTINGS, ADD }

private fun slotOf(screen: TourScreen): DockSlot = when (screen) {
    TourScreen.HOME -> DockSlot.HOME
    TourScreen.TRANSACTIONS -> DockSlot.TRANSACTIONS
    TourScreen.ANALYTICS -> DockSlot.ANALYTICS
    TourScreen.SETTINGS -> DockSlot.SETTINGS
    TourScreen.ADD -> DockSlot.ADD
}

private fun slotIcon(slot: DockSlot): ImageVector = when (slot) {
    DockSlot.HOME -> Icons.Rounded.Home
    DockSlot.TRANSACTIONS -> Icons.Rounded.SwapHoriz
    DockSlot.ANALYTICS -> Icons.Rounded.BarChart
    DockSlot.SETTINGS -> Icons.Rounded.Settings
    DockSlot.ADD -> Icons.Rounded.Add
}

private fun slotLabel(slot: DockSlot, isPersian: Boolean): String = when (slot) {
    DockSlot.HOME -> if (isPersian) "خانه" else "Home"
    DockSlot.TRANSACTIONS -> if (isPersian) "تراکنش‌ها" else "Transactions"
    DockSlot.ANALYTICS -> if (isPersian) "آمار" else "Analytics"
    DockSlot.SETTINGS -> if (isPersian) "تنظیمات" else "Settings"
    DockSlot.ADD -> if (isPersian) "افزودن" else "Add"
}

private fun stepIcon(step: TourStep): ImageVector =
    if (step.target == null) Icons.Rounded.AutoAwesome else slotIcon(slotOf(step.screen))

/**
 * نسخه‌ی کوچکِ نوار پایین برنامه داخل کارت: صفحه‌ای که این امکان در آن قرار دارد پررنگ می‌شود
 * و زیرش نشانی دقیق (کدام صفحه و کدام قسمت) نوشته شده است. در مرحله‌های شروع و پایان همه‌ی صفحه‌ها
 * به‌صورت یک نقشه‌ی کلی روشن‌اند. ترتیب با نوار واقعی برنامه یکی است (دکمه‌ی افزودن در فارسی ابتدای نوار).
 */
@Composable
private fun TourLocator(step: TourStep, isPersian: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val overview = step.target == null
    val active = if (overview) null else slotOf(step.screen)
    val tabs = listOf(DockSlot.HOME, DockSlot.TRANSACTIONS, DockSlot.ANALYTICS, DockSlot.SETTINGS)
    val ordered = if (isPersian) listOf(DockSlot.ADD) + tabs else tabs + DockSlot.ADD

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(scheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ordered.forEach { slot ->
                val isActive = active == slot
                val background by animateColorAsState(
                    targetValue = when {
                        isActive -> scheme.primary
                        overview -> scheme.primary.copy(alpha = 0.12f)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(300),
                    label = "tourDockBg"
                )
                val content by animateColorAsState(
                    targetValue = when {
                        isActive -> scheme.onPrimary
                        overview -> scheme.primary
                        else -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                    animationSpec = tween(300),
                    label = "tourDockFg"
                )
                Column(
                    modifier = Modifier
                        .weight(if (slot == DockSlot.ADD) 0.85f else 1f)
                        .clip(CircleShape)
                        .background(background)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = slotIcon(slot),
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = slotLabel(slot, isPersian),
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = content
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = step.location(isPersian),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary
            )
        }
    }
}