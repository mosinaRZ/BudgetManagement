package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.theme.AppDimens
import ir.hamedan.budgetmanagement.ui.theme.navigationBarBottom
import ir.hamedan.budgetmanagement.ui.theme.statusBarTop

/** ارتفاع پیش‌فرض محوشدگی بالا: status bar + تاپ‌بار. محتوای اولیه پایین‌تر از آن شروع می‌شود، پس چیزی محو نمی‌شود. */
@Composable
fun defaultTopFade(): Dp = statusBarTop() + AppDimens.TopBarBlockHeight

/** ارتفاع پیش‌فرض محوشدگی پایین: nav bar + ۴۸dp (زیر داک شناور). */
@Composable
fun defaultBottomFade(): Dp = navigationBarBottom() + 48.dp

/**
 * محوشدگی نرم لبه‌های بالا و پایین محتوای اسکرول‌شونده (مثل اپ‌های مدرن).
 * محتوا هنگام رفتن زیر تاپ‌بار/داک آرام شفاف می‌شود و پس‌زمینه‌ی aurora دیده می‌ماند.
 * پدینگ‌های استاندارد طوری‌اند که در حالت ساکن (ابتدا/انتهای لیست) هیچ بخشی از محتوا محو نمی‌شود.
 */
@Composable
fun Modifier.scrollEdgeFade(
    top: Dp = defaultTopFade(),
    bottom: Dp = defaultBottomFade()
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val topPx = top.toPx().coerceIn(0f, size.height / 2f)
        val bottomPx = bottom.toPx().coerceIn(0f, size.height / 2f)
        if (topPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topPx
                ),
                size = Size(size.width, topPx),
                blendMode = BlendMode.DstIn
            )
        }
        if (bottomPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - bottomPx,
                    endY = size.height
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - bottomPx),
                size = Size(size.width, bottomPx),
                blendMode = BlendMode.DstIn
            )
        }
    }