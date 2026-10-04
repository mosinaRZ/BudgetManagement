package ir.hamedan.budgetmanagement.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.ui.navigation.MainTabRoute
import kotlinx.coroutines.launch

data class BottomNavItem(
    val route: MainTabRoute,
    val icon: ImageVector,
    val labelFa: String,
    val labelEn: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = MainTabRoute.Home,
        icon = Icons.Default.Home,
        labelFa = "خانه",
        labelEn = "Home"
    ),
    BottomNavItem(
        route = MainTabRoute.Transactions,
        icon = Icons.Default.CompareArrows,
        labelFa = "تراکنش‌ها",
        labelEn = "Transactions"
    ),
    BottomNavItem(
        route = MainTabRoute.Analytics,
        icon = Icons.Default.BarChart,
        labelFa = "آمار",
        labelEn = "Analytics"
    ),
    BottomNavItem(
        route = MainTabRoute.Settings,
        icon = Icons.Default.Settings,
        labelFa = "تنظیمات",
        labelEn = "Settings"
    )
)

// ═════════════════════════════════════════════════════════════════════════════
//  Backdrop capture
//
//  Usage:
//      val backdrop = rememberGlassBackdrop()
//      Box {
//          NavHost(Modifier.glassSource(backdrop).background(bg), …)   // glassSource BEFORE background
//          CapsuleBottomNavigation(…, backdrop = backdrop,
//              modifier = Modifier.align(Alignment.BottomCenter))     // sibling, never a child
//      }
// ═════════════════════════════════════════════════════════════════════════════

@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/** Marks the content that should appear (refracted) behind the glass bar. */
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

// ═════════════════════════════════════════════════════════════════════════════
//  AGSL (API 33+)
//
//  Physically based glass:
//   • The rim of every shape is a convex "bezel" (squircle height profile).
//   • For each pixel in the bezel we compute the surface slope, build the normal and
//     refract a vertical view ray with Snell's law (n = 1.5). The ray then travels
//     through `thickness` of glass, which gives the lateral shift of what you see.
//     → huge bending right at the rim, fading to nothing towards the flat middle.
//   • The normal comes from the rounded-rect SDF, so corners bend along the diagonal
//     and are refracted/lit the most.
//   • A directional light (top-left) produces a thin crisp rim reflection + a soft
//     glow, and the opposite side gets a weaker bounce light and a subtle shade.
// ═════════════════════════════════════════════════════════════════════════════

private const val GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float4 lens;
uniform float barRadius;
uniform float lensRadius;
uniform float barBezel;
uniform float barThickness;
uniform float lensBezel;
uniform float lensThickness;
uniform float lensZoom;
uniform float lensAmount;
uniform float dispersion;
uniform float specStrength;
uniform float shadeStrength;
uniform float rimThin;
uniform float rimSoft;

float sdRoundRect(float2 p, float2 hs, float r) {
    float2 q = abs(p) - hs + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float2 sdNormal(float2 p, float2 hs, float r) {
    float e = 0.75;
    float2 g = float2(
        sdRoundRect(p + float2(e, 0.0), hs, r) - sdRoundRect(p - float2(e, 0.0), hs, r),
        sdRoundRect(p + float2(0.0, e), hs, r) - sdRoundRect(p - float2(0.0, e), hs, r)
    );
    float l = length(g);
    return l > 0.0001 ? g / l : float2(0.0);
}

// convex squircle: x = 0 at the rim, 1 at the inner end of the bezel
float bezelHeight(float x) {
    float u = 1.0 - x;
    float u2 = u * u;
    return pow(max(1.0 - u2 * u2, 0.0), 0.25);
}

// inward lateral shift (px) of the ray that is seen at `depth` px inside the rim
float bezelShift(float depth, float bezel, float thickness) {
    if (depth <= 0.0 || depth >= bezel) { return 0.0; }
    float x = depth / bezel;
    float h = 0.02;
    float x0 = max(x - h, 0.0);
    float x1 = min(x + h, 1.0);
    float slope = (bezelHeight(x1) - bezelHeight(x0)) / (x1 - x0);
    float2 N = normalize(float2(-slope, 1.0));
    float c = N.y;
    float k = max(1.0 - 0.4444 * (1.0 - c * c), 0.0);          // eta^2, eta = 1 / 1.5
    float2 T = 0.6667 * float2(0.0, -1.0) + (0.6667 * c - sqrt(k)) * N;
    return (T.x / max(-T.y, 0.05)) * thickness;
}

// reflection of the key light on the rim; strongest where the normal faces the light (corners!)
float rimLight(float depth, float2 n, float2 L) {
    if (depth < 0.0) { return 0.0; }
    float d = dot(n, L);
    float lobe = pow(max(d, 0.0), 2.0) + 0.45 * pow(max(-d, 0.0), 2.0);
    float thin = 1.0 - smoothstep(0.0, rimThin, depth);
    float soft = 1.0 - smoothstep(0.0, rimSoft, depth);
    return lobe * (thin * 0.95 + soft * 0.22);
}

// darkening on the side facing away from the light → gives the glass thickness
float rimShade(float depth, float2 n, float2 L) {
    if (depth < 0.0) { return 0.0; }
    float away = max(-dot(n, L), 0.0);
    return away * (1.0 - smoothstep(0.0, rimSoft * 1.6, depth));
}

half4 main(float2 fc) {
    float2 L = float2(-0.6, -0.8);

    // bar
    float2 hsBar = size * 0.5;
    float2 pB = fc - hsBar;
    float depthB = -sdRoundRect(pB, hsBar, barRadius);
    float2 nB = sdNormal(pB, hsBar, barRadius);
    float2 shift = -nB * bezelShift(depthB, barBezel, barThickness);

    // droplet lens
    float2 lc  = float2(lens.x + lens.z, lens.y + lens.w) * 0.5;
    float2 hsL = float2(lens.z - lens.x, lens.w - lens.y) * 0.5;
    float2 pL  = fc - lc;
    float depthL = -sdRoundRect(pL, hsL, lensRadius);
    float2 nL = sdNormal(pL, hsL, lensRadius);
    float inLens = clamp(depthL, 0.0, 1.0) * lensAmount;
    float2 lensShift = (-nL * bezelShift(depthL, lensBezel, lensThickness) - pL * lensZoom) * inLens;

    float2 total = shift + lensShift;

    half4 r = content.eval(fc + total * (1.0 + dispersion));
    half4 g = content.eval(fc + total);
    half4 b = content.eval(fc + total * (1.0 - dispersion));
    half3 col = half3(r.r, g.g, b.b);

    float shade = rimShade(depthB, nB, L) * 0.6 + rimShade(depthL, nL, L) * inLens;
    float spec  = rimLight(depthB, nB, L) * 0.85 + rimLight(depthL, nL, L) * inLens;

    col = col * (1.0 - shadeStrength * clamp(shade, 0.0, 1.0));
    col = col + half3(spec * specStrength);
    return half4(clamp(col, half3(0.0), half3(1.0)), g.a);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlassShader {
    private val shader = RuntimeShader(GLASS_AGSL)

    fun createEffect(
        barSize: Size,
        lensLeft: Float, lensTop: Float, lensRight: Float, lensBottom: Float,
        barRadius: Float, lensRadius: Float,
        barBezel: Float, barThickness: Float,
        lensBezel: Float, lensThickness: Float,
        lensZoom: Float, lensAmount: Float,
        dispersion: Float,
        specStrength: Float, shadeStrength: Float,
        rimThin: Float, rimSoft: Float,
        blurPx: Float
    ): RenderEffect {
        shader.setFloatUniform("size", barSize.width, barSize.height)
        shader.setFloatUniform("lens", lensLeft, lensTop, lensRight, lensBottom)
        shader.setFloatUniform("barRadius", barRadius)
        shader.setFloatUniform("lensRadius", lensRadius)
        shader.setFloatUniform("barBezel", barBezel)
        shader.setFloatUniform("barThickness", barThickness)
        shader.setFloatUniform("lensBezel", lensBezel)
        shader.setFloatUniform("lensThickness", lensThickness)
        shader.setFloatUniform("lensZoom", lensZoom)
        shader.setFloatUniform("lensAmount", lensAmount)
        shader.setFloatUniform("dispersion", dispersion)
        shader.setFloatUniform("specStrength", specStrength)
        shader.setFloatUniform("shadeStrength", shadeStrength)
        shader.setFloatUniform("rimThin", rimThin)
        shader.setFloatUniform("rimSoft", rimSoft)

        val refraction = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
        val effect = if (blurPx > 0.5f) {
            AndroidRenderEffect.createChainEffect(
                refraction,
                AndroidRenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
            )
        } else refraction
        return effect.asComposeRenderEffect()
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Bar
// ═════════════════════════════════════════════════════════════════════════════

/** Uniform gap between the bar's outer edge and the lens → lens stays concentric with the bar. */
private val LensInset = 4.dp

@Composable
fun CapsuleBottomNavigation(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    /** Frost amount. Lower = crisper refraction. */
    blurRadius: Dp = 1.5.dp
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }
    val shape = RoundedCornerShape(percent = 50)

    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f
    val primary = colors.primary
    val canvasColor = colors.background
    val hasBackdrop = backdrop != null

    // Veil uses the theme surface (not white) so the bar keeps its contrast instead of washing out.
    val veilTop = if (hasBackdrop) colors.surface.copy(alpha = if (isDark) 0.46f else 0.38f)
    else colors.surfaceVariant.copy(alpha = 0.80f)
    val veilBottom = if (hasBackdrop) colors.surface.copy(alpha = if (isDark) 0.34f else 0.26f)
    else colors.surfaceVariant.copy(alpha = 0.72f)

    val selectedIndex = bottomNavItems.indexOfFirst { item ->
        currentRoute?.contains(item.route::class.simpleName ?: "") == true
    }

    // Leading edge snaps ahead, trailing edge lags → the drop stretches, then relaxes.
    val initial = selectedIndex.coerceAtLeast(0).toFloat()
    val startEdge = remember { Animatable(initial) }
    val endEdge = remember { Animatable(initial) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        val target = selectedIndex.toFloat()
        val forward = target > startEdge.value
        val lead = spring<Float>(dampingRatio = 0.70f, stiffness = 560f)
        val trail = spring<Float>(dampingRatio = 0.82f, stiffness = 200f)
        launch { startEdge.animateTo(target, if (forward) trail else lead) }
        launch { endEdge.animateTo(target, if (forward) lead else trail) }
    }
    val lensAlpha by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        label = "lensAlpha"
    )

    val barLayer = rememberGraphicsLayer()
    var barOrigin by remember { mutableStateOf(Offset.Zero) }
    val glassShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GlassShader() else null
    }
    val shaderActive = hasBackdrop && glassShader != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.12f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.18f)
            )
            .clip(shape)
            .onGloballyPositioned { barOrigin = it.positionInRoot() }
            .drawBehind {
                val barRadius = size.height / 2f
                val inset = LensInset.toPx()

                // ── Lens geometry (RTL aware, clamped to the bar's inner wall) ──
                val count = bottomNavItems.size
                val itemW = (size.width - inset * 2f) / count
                val rtl = layoutDirection == LayoutDirection.Rtl
                val s = startEdge.value
                val e = endEdge.value + 1f
                val rawLeft = if (rtl) size.width - inset - e * itemW else inset + s * itemW
                val rawRight = if (rtl) size.width - inset - s * itemW else inset + e * itemW
                val lensLeft = rawLeft.coerceAtLeast(inset)
                val lensRight = rawRight.coerceAtMost(size.width - inset).coerceAtLeast(lensLeft + 1f)
                val lensTop = inset
                val lensBottom = size.height - inset
                val lensRadius = (barRadius - inset).coerceAtLeast(0f)
                val stretch = (endEdge.value - startEdge.value).coerceIn(0f, 1.5f)

                // ── 1) Backdrop through the refraction shader ──
                if (backdrop != null) {
                    val delta = barOrigin - backdrop.origin
                    barLayer.record {
                        drawRect(canvasColor)
                        translate(left = -delta.x, top = -delta.y) { drawLayer(backdrop.layer) }
                    }
                    val blurPx = blurRadius.toPx()
                    if (glassShader != null) {
                        barLayer.renderEffect = glassShader.createEffect(
                            barSize = size,
                            lensLeft = lensLeft, lensTop = lensTop,
                            lensRight = lensRight, lensBottom = lensBottom,
                            barRadius = barRadius, lensRadius = lensRadius,
                            barBezel = 15.dp.toPx(),
                            barThickness = 13.dp.toPx(),
                            lensBezel = 20.dp.toPx(),
                            lensThickness = 24.dp.toPx() * (1f + stretch * 0.5f),
                            lensZoom = 0.06f + stretch * 0.03f,
                            lensAmount = lensAlpha,
                            dispersion = 0.03f,
                            specStrength = if (isDark) 0.80f else 0.60f,
                            shadeStrength = if (isDark) 0.40f else 0.22f,
                            rimThin = 1.6.dp.toPx(),
                            rimSoft = 7.dp.toPx(),
                            blurPx = blurPx
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        barLayer.renderEffect = BlurEffect(blurPx * 3f, blurPx * 3f, TileMode.Clamp)
                    }
                    drawLayer(barLayer)
                }

                // ── 2) Veil (kept thin enough not to hide the refraction, strong enough not to look faded) ──
                drawRect(brush = Brush.verticalGradient(listOf(veilTop, veilBottom)))
                if (!shaderActive) {
                    // fallback sheen – with the shader the reflections come from the rim light instead
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.26f),
                                Color.Transparent
                            ),
                            endY = size.height * 0.5f
                        ),
                        size = Size(size.width, size.height * 0.5f)
                    )
                }

                // ── 3) Lens body ──
                if (lensAlpha > 0.01f) {
                    drawLensGlass(
                        left = lensLeft, top = lensTop, right = lensRight, bottom = lensBottom,
                        radius = lensRadius, primary = primary, isDark = isDark,
                        alpha = lensAlpha, hasRefraction = shaderActive
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.40f else 0.80f),
                        Color.White.copy(alpha = if (isDark) 0.05f else 0.16f),
                        Color.White.copy(alpha = if (isDark) 0.20f else 0.45f)
                    )
                ),
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LensInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                NavigationBarItemCustom(
                    item = item,
                    isSelected = index == selectedIndex,
                    isPersian = isPersian,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (index != selectedIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onItemSelected(item)
                    }
                )
            }
        }
    }
}

/**
 * Tint + soft shading of the lens. Every fill is clipped to the lens outline and drawn as a
 * plain rect – drawing sub-rects with rounded corners is what produced the two blobs at the
 * bottom corners before.
 */
private fun DrawScope.drawLensGlass(
    left: Float, top: Float, right: Float, bottom: Float,
    radius: Float,
    primary: Color,
    isDark: Boolean,
    alpha: Float,
    hasRefraction: Boolean
) {
    val w = right - left
    val h = bottom - top
    val topLeft = Offset(left, top)
    val lensPath = Path().apply {
        addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(radius)))
    }

    clipPath(lensPath) {
        // brand tint
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primary.copy(alpha = (if (hasRefraction) 0.14f else 0.26f) * alpha),
                    primary.copy(alpha = (if (hasRefraction) 0.05f else 0.10f) * alpha)
                ),
                startY = top, endY = bottom
            ),
            topLeft = topLeft, size = Size(w, h)
        )
        // upper sheen
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (if (isDark) 0.12f else 0.30f) * alpha),
                    Color.Transparent
                ),
                startY = top, endY = top + h * 0.5f
            ),
            topLeft = topLeft, size = Size(w, h * 0.5f)
        )
        // lower shade for volume
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = (if (isDark) 0.14f else 0.05f) * alpha)
                ),
                startY = top + h * 0.5f, endY = bottom
            ),
            topLeft = Offset(left, top + h * 0.5f), size = Size(w, h * 0.5f)
        )
    }

    // hairline definition; the real reflection comes from the shader when available
    val rimScale = if (hasRefraction) 0.45f else 1f
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (if (isDark) 0.60f else 0.95f) * alpha * rimScale),
                Color.White.copy(alpha = 0.05f * alpha * rimScale),
                Color.White.copy(alpha = (if (isDark) 0.30f else 0.65f) * alpha * rimScale)
            ),
            start = topLeft,
            end = Offset(right, bottom)
        ),
        topLeft = topLeft, size = Size(w, h), cornerRadius = CornerRadius(radius),
        style = Stroke(width = 1.dp.toPx())
    )
}

@Composable
private fun NavigationBarItemCustom(
    item: BottomNavItem,
    isSelected: Boolean,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Stronger unselected contrast (was 0.60) so items no longer look washed out.
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.82f),
        label = "itemColor"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.86f
            isSelected -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "itemScale"
    )
    val iconLift by animateFloatAsState(
        targetValue = if (isSelected) -1.5f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "iconLift"
    )

    val itemLabel = if (isPersian) item.labelFa else item.labelEn

    Box(
        modifier = modifier
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = itemLabel,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { translationY = iconLift.dp.toPx() }
            )
            Text(
                text = itemLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}