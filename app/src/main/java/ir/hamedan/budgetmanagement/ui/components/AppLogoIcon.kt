package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * نشان (لوگوی) خود برنامه به‌صورت ImageVector قابل‌رنگ‌پذیری (tint).
 *
 * سه ستون با گوشهٔ گرد بزرگ که دقیقاً هندسهٔ آیکون لانچر برنامه
 * (mipmap/icon_foreground و raw/export_logo_mark) را بازتولید می‌کند. به‌جای آیکون‌های
 * کیف پول متریال در همه‌جای برنامه استفاده می‌شود و مثل هر Icon دیگری از رنگ tint می‌گیرد:
 *
 *     Icon(imageVector = AppLogoIcon, contentDescription = null, tint = ...)
 */
val AppLogoIcon: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
    ImageVector.Builder(
        name = "AppLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 516f,
        viewportHeight = 516f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // چپ (کوتاه)، وسط، راست (بلند)؛ مختصات از لوگوی اصلی (۵۰۵×۵۱۶) با ۶ واحد جابه‌جایی افقی برای مرکزچینی
            logoBar(x0 = 42f, y0 = 285f, x1 = 158f, y1 = 479f)
            logoBar(x0 = 201f, y0 = 160f, x1 = 315f, y1 = 479f)
            logoBar(x0 = 358f, y0 = 36f, x1 = 474f, y1 = 398f)
        }
    }.build()
}

private const val BIG_RADIUS = 85f
private const val SMALL_RADIUS = 4f

/** یک ستون: گوشهٔ بالا‌چپ و پایین‌راست گرد بزرگ، دو گوشهٔ دیگر تقریباً تیز. */
private fun PathBuilder.logoBar(x0: Float, y0: Float, x1: Float, y1: Float) {
    val r = BIG_RADIUS
    val s = SMALL_RADIUS
    moveTo(x0 + r, y0)
    horizontalLineTo(x1 - s)
    arcTo(s, s, 0f, false, true, x1, y0 + s)
    verticalLineTo(y1 - r)
    arcTo(r, r, 0f, false, true, x1 - r, y1)
    horizontalLineTo(x0 + s)
    arcTo(s, s, 0f, false, true, x0, y1 - s)
    verticalLineTo(y0 + r)
    arcTo(r, r, 0f, false, true, x0 + r, y0)
    close()
}