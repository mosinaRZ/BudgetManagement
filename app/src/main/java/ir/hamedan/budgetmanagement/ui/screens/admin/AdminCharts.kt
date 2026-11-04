package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class LineSeries(val name: String, val values: List<Float>, val color: Color)
data class DonutSlice(val label: String, val value: Float, val color: Color)

/** Rounds up to a "nice" axis maximum (at least 4 so the four grid lines are never fractional). */
internal fun niceCeil(v: Float): Float {
    if (v <= 4f) return 4f
    val exp = 10.0.pow(floor(log10(v.toDouble())))
    val f = v / exp
    val nice = listOf(1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0).first { f <= it + 1e-9 }
    return (nice * exp).toFloat()
}

/** Charts always run left-to-right in time, also in Persian, so touch maths and labels stay consistent. */
@Composable
private fun LtrBox(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }
}

private fun DrawScope.drawGrid(
    measurer: TextMeasurer,
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    niceMax: Float,
    gridColor: Color,
    labelColor: Color,
    isPersian: Boolean
) {
    val style = TextStyle(fontSize = 9.sp, color = labelColor)
    for (k in 0..4) {
        val y = top + h * (1f - k / 4f)
        drawLine(gridColor, Offset(left, y), Offset(left + w, y), strokeWidth = 1.dp.toPx())
        val text = fmtCompact((niceMax * k / 4f).toLong(), isPersian)
        val layout = measurer.measure(text, style)
        drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
    }
}

private fun DrawScope.drawXLabels(
    measurer: TextMeasurer,
    labels: List<String>,
    xOf: (Int) -> Float,
    baseY: Float,
    labelColor: Color
) {
    if (labels.isEmpty()) return
    val style = TextStyle(fontSize = 9.sp, color = labelColor)
    val step = max(1, (labels.size - 1) / 4)
    var i = 0
    while (i < labels.size) {
        val layout = measurer.measure(labels[i], style)
        val x = (xOf(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
        drawText(layout, topLeft = Offset(x, baseY))
        i += step
    }
}

private fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    lines: List<Pair<String, Color>>,
    anchorX: Float,
    top: Float,
    background: Color
) {
    val styleFor = { color: Color, bold: Boolean ->
        TextStyle(fontSize = 10.sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
    val layouts = lines.mapIndexed { i, (text, color) -> measurer.measure(text, styleFor(color, i == 0)) }
    val pad = 8.dp.toPx()
    val boxW = (layouts.maxOfOrNull { it.size.width } ?: 0) + pad * 2
    val boxH = layouts.sumOf { it.size.height } + pad * 2
    val x = (anchorX - boxW / 2f).coerceIn(0f, size.width - boxW)
    drawRoundRect(background, Offset(x, top), Size(boxW, boxH.toFloat()), CornerRadius(10.dp.toPx()))
    var y = top + pad
    layouts.forEach { l ->
        drawText(l, topLeft = Offset(x + pad, y))
        y += l.size.height
    }
}

/** Smooth multi-series area chart with a touch tooltip (drag a finger across it to scrub). */
@Composable
fun LineAreaChart(
    labels: List<String>,
    series: List<LineSeries>,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val tooltipBg = MaterialTheme.colorScheme.surfaceVariant
    val tooltipText = MaterialTheme.colorScheme.onSurface
    var selected by remember(labels.size) { mutableIntStateOf(-1) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(series) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    val n = labels.size
    val niceMax = niceCeil(series.maxOfOrNull { it.values.maxOrNull() ?: 0f } ?: 0f)

    LtrBox {
        Column(modifier) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .pointerInput(n) {
                        val left = 34.dp.toPx()
                        val right = 8.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val w = size.width - left - right
                            fun indexAt(x: Float): Int =
                                if (n <= 1) 0 else (((x - left) / w) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            selected = indexAt(down.position.x)
                            var pressed = true
                            while (pressed) {
                                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                                selected = indexAt(change.position.x)
                                pressed = change.pressed
                            }
                        }
                    }
            ) {
                val left = 34.dp.toPx()
                val right = 8.dp.toPx()
                val top = 10.dp.toPx()
                val bottom = 22.dp.toPx()
                val w = size.width - left - right
                val h = size.height - top - bottom
                fun xOf(i: Int) = if (n <= 1) left + w / 2f else left + w * i / (n - 1)
                fun yOf(v: Float) = top + h - (v / niceMax) * h * progress.value

                drawGrid(measurer, left, top, w, h, niceMax, gridColor, labelColor, isPersian)
                drawXLabels(measurer, labels, ::xOf, top + h + 6.dp.toPx(), labelColor)

                series.forEach { s ->
                    val pts = s.values.mapIndexed { i, v -> Offset(xOf(i), yOf(v)) }
                    if (pts.size >= 2) {
                        val line = Path().apply {
                            moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until pts.size) {
                                val p0 = pts[i - 1]
                                val p1 = pts[i]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        val fill = Path().apply {
                            addPath(line)
                            lineTo(pts.last().x, top + h)
                            lineTo(pts.first().x, top + h)
                            close()
                        }
                        drawPath(fill, Brush.verticalGradient(listOf(s.color.copy(alpha = 0.28f), Color.Transparent), startY = top, endY = top + h))
                        drawPath(line, s.color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    } else if (pts.size == 1) {
                        drawCircle(s.color, 4.dp.toPx(), pts[0])
                    }
                }

                val sel = selected
                if (sel in 0 until n) {
                    val sx = xOf(sel)
                    drawLine(labelColor, Offset(sx, top), Offset(sx, top + h), strokeWidth = 1.dp.toPx())
                    series.forEach { s ->
                        val v = s.values.getOrNull(sel) ?: return@forEach
                        drawCircle(s.color, 5.dp.toPx(), Offset(sx, yOf(v)))
                        drawCircle(tooltipBg, 2.dp.toPx(), Offset(sx, yOf(v)))
                    }
                    val lines = buildList<Pair<String, Color>> {
                        add(labels[sel] to tooltipText)
                        series.forEach { s -> add("${s.name}: ${fmtNumber((s.values.getOrNull(sel) ?: 0f).toLong(), isPersian)}" to s.color) }
                    }
                    drawTooltip(measurer, lines, sx, top, tooltipBg)
                }
            }
            if (series.size > 1) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                    series.forEach { s -> LegendDot(s.color, s.name) }
                }
            }
        }
    }
}

/** Per-day stacked columns (e.g. normal / warning / dangerous events). */
@Composable
fun StackedBarChart(
    labels: List<String>,
    stacks: List<List<Float>>,
    names: List<String>,
    colors: List<Color>,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp
) {
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val tooltipBg = MaterialTheme.colorScheme.surfaceVariant
    val tooltipText = MaterialTheme.colorScheme.onSurface
    var selected by remember(labels.size) { mutableIntStateOf(-1) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(stacks) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    val n = labels.size
    val totals = stacks.map { it.sum() }
    val niceMax = niceCeil(totals.maxOrNull() ?: 0f)

    LtrBox {
        Column(modifier) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .pointerInput(n) {
                        val left = 34.dp.toPx()
                        val right = 8.dp.toPx()
                        detectTapGestures { offset ->
                            val w = size.width - left - right
                            if (n > 0) {
                                val slot = w / n
                                val idx = ((offset.x - left) / slot).toInt()
                                selected = if (idx in 0 until n && idx != selected) idx else -1
                            }
                        }
                    }
            ) {
                val left = 34.dp.toPx()
                val right = 8.dp.toPx()
                val top = 10.dp.toPx()
                val bottom = 22.dp.toPx()
                val w = size.width - left - right
                val h = size.height - top - bottom
                val slot = if (n > 0) w / n else w
                fun centerX(i: Int) = left + slot * i + slot / 2f
                drawGrid(measurer, left, top, w, h, niceMax, gridColor, labelColor, isPersian)
                drawXLabels(measurer, labels, ::centerX, top + h + 6.dp.toPx(), labelColor)

                val barW = (slot * 0.62f).coerceAtMost(26.dp.toPx())
                stacks.forEachIndexed { i, parts ->
                    var yBottom = top + h
                    parts.forEachIndexed { k, value ->
                        val segH = (value / niceMax) * h * progress.value
                        if (segH > 0.5f) {
                            val x = centerX(i) - barW / 2f
                            drawRoundRect(
                                colors.getOrElse(k) { Color.Gray }.copy(alpha = if (selected == -1 || selected == i) 1f else 0.35f),
                                Offset(x, yBottom - segH),
                                Size(barW, segH),
                                CornerRadius(2.dp.toPx())
                            )
                            yBottom -= segH
                        }
                    }
                }
                val sel = selected
                if (sel in 0 until n) {
                    val lines = buildList<Pair<String, Color>> {
                        add(labels[sel] to tooltipText)
                        stacks[sel].forEachIndexed { k, v ->
                            add("${names.getOrElse(k) { "" }}: ${fmtNumber(v.toLong(), isPersian)}" to colors.getOrElse(k) { Color.Gray })
                        }
                    }
                    drawTooltip(measurer, lines, centerX(sel), top, tooltipBg)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                names.forEachIndexed { i, name -> LegendDot(colors.getOrElse(i) { Color.Gray }, name) }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

/** Ring chart with a legend that shows value and share for each slice. */
@Composable
fun DonutWithLegend(
    slices: List<DonutSlice>,
    centerTitle: String,
    centerSubtitle: String,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 18.dp.toPx()
                val diameter = min(size.width, size.height) - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (total > 0f) {
                    var start = -90f
                    slices.forEach { s ->
                        val sweep = s.value / total * 360f * progress.value
                        val gap = if (slices.size > 1) 3f else 0f
                        if (sweep > gap) {
                            drawArc(s.color, start + gap / 2f, sweep - gap, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                        }
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(centerSubtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEach { s ->
                val pct = if (total > 0f) (s.value / total * 100f) else 0f
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(s.color, CircleShape))
                    Text(s.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        "${fmtNumber(s.value.toLong(), isPersian)} · ${numberFormat(isPersian).format(pct.roundToInt())}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/** 7 weekdays x 24 hours activity grid. [matrix] rows are Sunday..Saturday, as sent by the server. */
@Composable
fun HeatmapGrid(
    matrix: List<List<Long>>,
    isPersian: Boolean,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val order = if (isPersian) listOf(6, 0, 1, 2, 3, 4, 5) else listOf(0, 1, 2, 3, 4, 5, 6)
    val dayNames = if (isPersian) listOf("یک", "دو", "سه", "چهار", "پنج", "جمعه", "شنبه") else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxV = (matrix.maxOfOrNull { row -> row.maxOrNull() ?: 0L } ?: 0L).coerceAtLeast(1L)
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val cellH = 20.dp

    LtrBox {
        Column(modifier) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(cellH * 7 + 20.dp)
                    .pointerInput(Unit) {
                        val left = 30.dp.toPx()
                        val ch = cellH.toPx()
                        detectTapGestures { offset ->
                            val cw = (size.width - left) / 24f
                            val col = ((offset.x - left) / cw).toInt()
                            val row = (offset.y / ch).toInt()
                            selected = if (col in 0..23 && row in 0..6) row to col else null
                        }
                    }
            ) {
                val left = 30.dp.toPx()
                val cw = (size.width - left) / 24f
                val ch = cellH.toPx()
                val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
                for (r in 0 until 7) {
                    val src = order[r]
                    val name = measurer.measure(dayNames[src], labelStyle)
                    drawText(name, topLeft = Offset(0f, r * ch + (ch - name.size.height) / 2f))
                    for (c in 0 until 24) {
                        val v = matrix.getOrNull(src)?.getOrNull(c) ?: 0L
                        val color = if (v == 0L) empty else accent.copy(alpha = 0.22f + 0.78f * (v.toFloat() / maxV))
                        drawRoundRect(color, Offset(left + c * cw + 1.dp.toPx(), r * ch + 1.dp.toPx()), Size(cw - 2.dp.toPx(), ch - 2.dp.toPx()), CornerRadius(3.dp.toPx()))
                        if (selected == r to c) {
                            drawRoundRect(accent, Offset(left + c * cw, r * ch), Size(cw, ch), CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }
                for (hour in listOf(0, 6, 12, 18, 23)) {
                    val l = measurer.measure("%02d".format(hour), labelStyle)
                    drawText(l, topLeft = Offset(left + hour * cw + (cw - l.size.width) / 2f, ch * 7 + 4.dp.toPx()))
                }
            }
            val sel = selected
            val text = if (sel != null) {
                val src = order[sel.first]
                val v = matrix.getOrNull(src)?.getOrNull(sel.second) ?: 0L
                "${dayNames[src]} ${"%02d".format(sel.second)}:00 — ${fmtNumber(v, isPersian)} $unitLabel"
            } else ""
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().height(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Ranked horizontal bars with animated fill. */
@Composable
fun HorizontalBars(
    items: List<Pair<String, Long>>,
    colors: List<Color>,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    valueText: (Long) -> String = { fmtNumber(it, isPersian) }
) {
    val maxV = (items.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, (label, value) ->
            BarRow(label, valueText(value), value.toFloat() / maxV, colors.getOrElse(i % colors.size) { MaterialTheme.colorScheme.primary })
        }
    }
}

@Composable
private fun BarRow(label: String, valueText: String, fraction: Float, color: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "bar")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
            Text(valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(animated.coerceAtLeast(0.02f)).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

/** Half-circle gauge for the 0-100 security risk score. */
@Composable
fun RiskGauge(score: Int, level: String, levelLabel: String, caption: String, isPersian: Boolean, modifier: Modifier = Modifier) {
    val color = when (level) {
        "high" -> MaterialTheme.colorScheme.error
        "medium" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val animated by animateFloatAsState(score.coerceIn(0, 100) / 100f, tween(1000, easing = FastOutSlowInEasing), label = "risk")
    Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val diameter = min(size.width, (size.height - 4.dp.toPx()) * 2f) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, stroke / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(track, 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)),
                180f, 180f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 2.dp)) {
            Text(numberFormat(isPersian).format(score), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(levelLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}

/** Tiny trend line for KPI cards. */
@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val maxV = max(values.maxOrNull() ?: 1f, 1f)
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, size.height - (v / maxV) * (size.height - 3.dp.toPx()) - 1.5.dp.toPx()) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val cx = (pts[i - 1].x + pts[i].x) / 2f
                cubicTo(cx, pts[i - 1].y, cx, pts[i].y, pts[i].x, pts[i].y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(line, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
