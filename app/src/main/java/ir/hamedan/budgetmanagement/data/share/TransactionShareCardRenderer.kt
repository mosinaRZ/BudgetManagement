package ir.hamedan.budgetmanagement.data.share

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import ir.hamedan.budgetmanagement.R
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the "share transaction" card (crypto-exchange PnL style: artwork full-bleed, text pinned
 * to one side, big colored figure, plain label/value lines) with plain Android
 * Canvas so the output is a real, high-resolution bitmap that does not depend on
 * what is currently on screen.
 *
 * Layout is authored in a 1080x1350 design space and rendered at [OUTPUT_SCALE]x.
 * Everything that has a reading direction (header, pill, rows) is mirrored for RTL.
 *
 * Backgrounds: drawable resources named `share_bg_01` .. `share_bg_12`
 * (put them in res/drawable-nodpi, 4:5 portrait, see [BACKGROUND_NAMES_EN]).
 * A missing file silently falls back to a built-in gradient so the feature
 * works before the artwork is added.
 */
object TransactionShareCardRenderer {

    const val BACKGROUND_COUNT = 12
    const val DESIGN_WIDTH = 1080
    const val DESIGN_HEIGHT = 1350
    const val ASPECT_RATIO = 1080f / 1350f
    private const val OUTPUT_SCALE = 1.5f

    /** Genre of each artwork, index-aligned with `share_bg_01` .. `share_bg_12` and [FALLBACK_PALETTES]. */
    private val BACKGROUND_NAMES_EN = arrayOf(
        "Nature", "AI", "Gaming", "Sports", "Ocean", "Cars",
        "Finance", "Anime", "City", "Persian garden", "Space", "Abstract"
    )
    private val BACKGROUND_NAMES_FA = arrayOf(
        "طبیعت", "هوش مصنوعی", "گیم", "ورزش", "اقیانوس", "خودرو",
        "مالی", "انیمه", "شهر", "باغ ایرانی", "فضا", "انتزاعی"
    )

    /** Human-readable genre of a background (used for accessibility labels). */
    fun backgroundLabel(index: Int, isPersian: Boolean): String =
        (if (isPersian) BACKGROUND_NAMES_FA else BACKGROUND_NAMES_EN)[safeIndex(index)]

    private val INCOME_COLOR = Color.parseColor("#34D399")
    private val EXPENSE_COLOR = Color.parseColor("#FB7185")

    /** Fallback gradients (top-left, middle, bottom-right) used when an image is missing. */
    private val FALLBACK_PALETTES = arrayOf(
        intArrayOf(0xFF0B3D2E.toInt(), 0xFF1F7A5C.toInt(), 0xFF0A1F1A.toInt()),
        intArrayOf(0xFF0F172A.toInt(), 0xFF1D4ED8.toInt(), 0xFF0B1020.toInt()),
        intArrayOf(0xFF2E1065.toInt(), 0xFF7C3AED.toInt(), 0xFF1E1B4B.toInt()),
        intArrayOf(0xFF7C2D12.toInt(), 0xFFF97316.toInt(), 0xFF4C0519.toInt()),
        intArrayOf(0xFF042F2E.toInt(), 0xFF0D9488.toInt(), 0xFF082F49.toInt()),
        intArrayOf(0xFF4C0519.toInt(), 0xFFE11D48.toInt(), 0xFF1F0A12.toInt()),
        intArrayOf(0xFF422006.toInt(), 0xFFCA8A04.toInt(), 0xFF1C1917.toInt()),
        intArrayOf(0xFF312E81.toInt(), 0xFFDB2777.toInt(), 0xFF1E1B4B.toInt()),
        intArrayOf(0xFF0F172A.toInt(), 0xFF475569.toInt(), 0xFF020617.toInt()),
        intArrayOf(0xFF14532D.toInt(), 0xFF65A30D.toInt(), 0xFF052E16.toInt()),
        intArrayOf(0xFF083344.toInt(), 0xFF06B6D4.toInt(), 0xFF0C4A6E.toInt()),
        intArrayOf(0xFF500724.toInt(), 0xFFA21CAF.toInt(), 0xFF2E1065.toInt())
    )

    /** Resource id of `share_bg_NN` (NN = index + 1), or 0 when the artwork is not in the project. */
    fun backgroundResId(context: Context, index: Int): Int {
        val name = String.format(Locale.ROOT, "share_bg_%02d", safeIndex(index) + 1)
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    private fun safeIndex(index: Int): Int = ((index % BACKGROUND_COUNT) + BACKGROUND_COUNT) % BACKGROUND_COUNT

    /** Small preview of one background (with the same scrim the card uses). */
    fun renderBackgroundThumbnail(context: Context, index: Int, width: Int, height: Int, rtl: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(width / DESIGN_WIDTH.toFloat(), height / DESIGN_HEIGHT.toFloat())
        drawBackground(canvas, context, index, width, height, rtl)
        return bitmap
    }

    fun render(context: Context, content: TransactionShareContent, backgroundIndex: Int): Bitmap {
        val rtl = content.isPersian
        val fonts = loadFonts(context, rtl)
        val outW = (DESIGN_WIDTH * OUTPUT_SCALE).toInt()
        val outH = (DESIGN_HEIGHT * OUTPUT_SCALE).toInt()
        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(OUTPUT_SCALE, OUTPUT_SCALE)

        val w = DESIGN_WIDTH.toFloat()
        val h = DESIGN_HEIGHT.toFloat()
        val margin = 72f
        val contentW = w - margin * 2
        val accent = if (content.isExpense) EXPENSE_COLOR else INCOME_COLOR
        val dirApp = if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR
        val dirContent = if (rtl) TextDirectionHeuristics.FIRSTSTRONG_RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR

        drawBackground(canvas, context, backgroundIndex, outW, outH, rtl)

        /** X of a block of [width] pinned to the reading-start edge (left in LTR, right in RTL). */
        fun startX(width: Float) = if (rtl) w - margin - width else margin

        // ───────────── Header: logo + app name (exchange-style, on the text side) ─────────────
        val headerTop = 72f
        val tile = 96f
        val tileX = startX(tile)
        val tileRect = RectF(tileX, headerTop, tileX + tile, headerTop + tile)
        canvas.drawRoundRect(tileRect, 28f, 28f, fill(Color.argb(245, 255, 255, 255)))
        decodeLogo(context)?.let { logo ->
            val inset = 18f
            canvas.drawBitmap(
                logo,
                null,
                RectF(tileRect.left + inset, tileRect.top + inset, tileRect.right - inset, tileRect.bottom - inset),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            logo.recycle()
        }
        val appName = localizedAppName(context, rtl)
        val namePaint = textPaint(fonts.bold, 46f, Color.WHITE)
        val nameLayout = tight(appName, namePaint, 420, 1, dirApp)
        val nameX = if (rtl) tileX - 20f - nameLayout.width else tileX + tile + 20f
        drawLayout(canvas, nameLayout, nameX, headerTop + (tile - nameLayout.height) / 2f)
        val headerBottom = headerTop + tile

        // ───────────── Footer: brand line on the text side ─────────────
        val footerLineY = h - 140f
        canvas.drawRect(margin, footerLineY, w - margin, footerLineY + 2f, fill(Color.argb(46, 255, 255, 255)))
        val brandText = if (rtl) "ساخته‌شده با $appName" else "Made with $appName"
        val brandLayout = tight(
            brandText, textPaint(fonts.medium, 34f, Color.argb(200, 255, 255, 255)),
            contentW.toInt(), 1, dirApp
        )
        drawLayout(canvas, brandLayout, startX(brandLayout.width.toFloat()), footerLineY + 34f)

        // ───────────── Hero block (type chip, title, amount, currency, details) ─────────────
        val textMaxW = contentW * 0.82f
        val titlePaint = textPaint(fonts.bold, 54f, Color.WHITE).apply {
            setShadowLayer(10f, 0f, 3f, Color.argb(90, 0, 0, 0))
        }
        val titleLayout = tight(content.title, titlePaint, textMaxW.toInt(), 2, dirContent)

        // Amount shrinks until it fits on one line.
        val amountPaint = textPaint(fonts.bold, 128f, accent).apply {
            setShadowLayer(18f, 0f, 5f, Color.argb(110, 0, 0, 0))
        }
        var amountSize = 128f
        while (amountSize > 56f && amountPaint.apply { textSize = amountSize }.measureText(content.amountText) > contentW) {
            amountSize -= 6f
        }
        amountPaint.textSize = amountSize
        val amountWidth = amountPaint.measureText(content.amountText)
        val amountCapHeight = amountSize * 0.8f   // digits sit at cap height in both fonts

        val currencyPaint = textPaint(fonts.medium, 42f, Color.argb(225, 255, 255, 255))
        val currencyLayout = tight(content.currencyLabel, currencyPaint, contentW.toInt(), 1, dirApp)

        val pillPaint = textPaint(fonts.semiBold, 34f, accent)
        val pillText = tight(content.typeLabel, pillPaint, 360, 1, dirApp)
        val pillH = 68f
        val pillPadH = 28f
        val dotD = 18f
        val dotGap = 14f
        val pillW = pillPadH + dotD + dotGap + pillText.width + pillPadH

        // Details are plain "label  value" lines (like Entry price / Last price on exchange cards).
        val labelPaint = textPaint(fonts.regular, 34f, Color.argb(170, 255, 255, 255))
        val valuePaint = textPaint(fonts.semiBold, 38f, Color.WHITE)
        val notePaint = textPaint(fonts.medium, 36f, Color.argb(240, 255, 255, 255))
        val colGap = 28f
        val rowGap = 26f

        class InfoRow(val label: StaticLayout, val value: StaticLayout, val stacked: Boolean) {
            val height: Float =
                if (stacked) label.height + 8f + value.height else max(label.height, value.height).toFloat()
        }

        fun inlineRow(label: String, value: String, dir: TextDirectionHeuristic): InfoRow {
            val l = tight(label, labelPaint, (contentW * 0.45f).toInt(), 1, dirApp)
            val v = tight(value, valuePaint, (contentW - l.width - colGap).toInt(), 1, dir)
            return InfoRow(l, v, false)
        }

        val rows = ArrayList<InfoRow>(3)
        rows += inlineRow(if (rtl) "دسته‌بندی" else "Category", content.categoryTitle, dirContent)
        rows += inlineRow(if (rtl) "تاریخ و زمان" else "Date & time", content.dateTimeText, dirApp)
        if (content.note.isNotBlank()) {
            rows += InfoRow(
                tight(if (rtl) "یادداشت" else "Note", labelPaint, textMaxW.toInt(), 1, dirApp),
                tight(content.note, notePaint, textMaxW.toInt(), 2, dirContent),
                true
            )
        }
        val rowsH = rows.sumOf { it.height.toDouble() }.toFloat() + rowGap * (rows.size - 1)

        val availableTop = headerBottom + 24f
        val availableBottom = footerLineY - 28f
        val available = availableBottom - availableTop

        fun total(badge: Float, gs: Float): Float =
            max(badge, pillH) + 36f * gs + titleLayout.height + 12f * gs + amountCapHeight +
                    18f * gs + currencyLayout.height + 56f * gs + rowsH

        // Full emoji badge first, then no badge, and tighter gaps only when long content needs the room.
        var badgeD = 0f
        var gapScale = 0.5f
        search@ for (option in floatArrayOf(96f, 0f)) {
            for (gs in floatArrayOf(1f, 0.5f)) {
                if (total(option, gs) <= available) { badgeD = option; gapScale = gs; break@search }
            }
        }
        val totalH = total(badgeD, gapScale)
        val chipH = max(badgeD, pillH)
        var y = availableTop + max(0f, (available - totalH) * 0.4f)

        // Soft accent glow behind the amount (keeps the number readable on any image).
        val glowCy = y + chipH + 36f * gapScale + titleLayout.height + 12f * gapScale + amountCapHeight / 2f
        val glowCx = startX(amountWidth) + amountWidth / 2f
        canvas.drawCircle(
            glowCx, glowCy, 520f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    glowCx, glowCy, 520f,
                    intArrayOf(withAlpha(accent, 64), withAlpha(accent, 0)),
                    floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                )
            }
        )

        // Chip row: [category emoji]  [● Income / Expense]   (mirrored in RTL)
        if (badgeD > 0f) {
            val cx = startX(badgeD) + badgeD / 2f
            val cy = y + chipH / 2f
            canvas.drawCircle(cx, cy, badgeD / 2f, fill(Color.argb(50, 255, 255, 255)))
            canvas.drawCircle(cx, cy, badgeD / 2f, stroke(Color.argb(110, 255, 255, 255), 3f))
            val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = badgeD * 0.52f; textAlign = Paint.Align.CENTER }
            val fm = emojiPaint.fontMetrics
            canvas.drawText(content.categoryEmoji, cx, cy - (fm.ascent + fm.descent) / 2f, emojiPaint)
        }
        val chipOffset = if (badgeD > 0f) badgeD + 20f else 0f
        val pillX = if (rtl) w - margin - chipOffset - pillW else margin + chipOffset
        val pillY = y + (chipH - pillH) / 2f
        val pillRect = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, fill(withAlpha(accent, 48)))
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, stroke(withAlpha(accent, 190), 2.5f))
        val dotCx: Float
        val textX: Float
        if (rtl) {
            dotCx = pillX + pillW - pillPadH - dotD / 2f
            textX = pillX + pillPadH
        } else {
            dotCx = pillX + pillPadH + dotD / 2f
            textX = pillX + pillPadH + dotD + dotGap
        }
        canvas.drawCircle(dotCx, pillY + pillH / 2f, dotD / 2f, fill(accent))
        drawLayout(canvas, pillText, textX, pillY + (pillH - pillText.height) / 2f)
        y += chipH + 36f * gapScale

        drawLayout(canvas, titleLayout, startX(titleLayout.width.toFloat()), y)
        y += titleLayout.height + 12f * gapScale

        amountPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(content.amountText, startX(amountWidth), y + amountCapHeight, amountPaint)
        y += amountCapHeight + 18f * gapScale

        drawLayout(canvas, currencyLayout, startX(currencyLayout.width.toFloat()), y)
        y += currencyLayout.height

        // Short accent bar separating the amount from the details.
        val barW = 120f
        val barY = y + 24f * gapScale
        canvas.drawRoundRect(
            RectF(startX(barW), barY, startX(barW) + barW, barY + 6f), 3f, 3f, fill(withAlpha(accent, 230))
        )
        y += 56f * gapScale

        rows.forEach { row ->
            if (row.stacked) {
                drawLayout(canvas, row.label, startX(row.label.width.toFloat()), y)
                drawLayout(canvas, row.value, startX(row.value.width.toFloat()), y + row.label.height + 8f)
            } else {
                val labelX = startX(row.label.width.toFloat())
                val valueX = if (rtl) labelX - colGap - row.value.width else labelX + row.label.width + colGap
                val lineH = row.height
                drawLayout(canvas, row.label, labelX, y + (lineH - row.label.height) / 2f)
                drawLayout(canvas, row.value, valueX, y + (lineH - row.value.height) / 2f)
            }
            y += row.height + rowGap
        }

        return bitmap
    }

    // ───────────────────────── Background ─────────────────────────

    /**
     * Draws the artwork full-bleed plus an exchange-style scrim: dark on the text side
     * (left in LTR, right in RTL) fading out so the picture stays vivid on the other side.
     * In RTL the artwork is mirrored so its subject always sits opposite the text.
     */
    private fun drawBackground(canvas: Canvas, context: Context, index: Int, decodeW: Int, decodeH: Int, rtl: Boolean) {
        val w = DESIGN_WIDTH.toFloat()
        val h = DESIGN_HEIGHT.toFloat()
        val bmp = decodeSampled(context, backgroundResId(context, index), decodeW, decodeH)
        if (bmp != null) {
            val srcAspect = bmp.width.toFloat() / bmp.height.toFloat()
            val src = if (srcAspect > ASPECT_RATIO) {
                val sw = (bmp.height * ASPECT_RATIO).toInt()
                val left = (bmp.width - sw) / 2
                Rect(left, 0, left + sw, bmp.height)
            } else {
                val sh = (bmp.width / ASPECT_RATIO).toInt()
                val top = (bmp.height - sh) / 2
                Rect(0, top, bmp.width, top + sh)
            }
            canvas.save()
            if (rtl) canvas.scale(-1f, 1f, w / 2f, h / 2f)
            canvas.drawBitmap(bmp, src, RectF(0f, 0f, w, h), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
            canvas.restore()
            bmp.recycle()
        } else {
            val colors = FALLBACK_PALETTES[safeIndex(index)]
            canvas.drawRect(
                0f, 0f, w, h,
                Paint().apply {
                    shader = LinearGradient(0f, 0f, w, h, colors, floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                }
            )
            val hx = if (rtl) w * 0.15f else w * 0.85f
            canvas.drawCircle(
                hx, h * 0.28f, 620f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        hx, h * 0.28f, 620f,
                        intArrayOf(Color.argb(90, 255, 255, 255), Color.argb(0, 255, 255, 255)),
                        floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                    )
                }
            )
        }
        // Side scrim: strong behind the text, transparent over the artwork.
        val x0 = if (rtl) w else 0f
        val x1 = if (rtl) 0f else w
        canvas.drawRect(
            0f, 0f, w, h,
            Paint().apply {
                shader = LinearGradient(
                    x0, 0f, x1, 0f,
                    intArrayOf(Color.argb(238, 0, 0, 0), Color.argb(205, 0, 0, 0), Color.argb(70, 0, 0, 0), Color.argb(0, 0, 0, 0)),
                    floatArrayOf(0f, 0.45f, 0.8f, 1f), Shader.TileMode.CLAMP
                )
            }
        )
        // Vertical scrim: keeps header and footer readable on bright artwork.
        canvas.drawRect(
            0f, 0f, w, h,
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.argb(110, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(200, 0, 0, 0)),
                    floatArrayOf(0f, 0.2f, 0.65f, 1f), Shader.TileMode.CLAMP
                )
            }
        )
    }

    private fun decodeSampled(context: Context, resId: Int, reqW: Int, reqH: Int): Bitmap? {
        if (resId == 0) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
            BitmapFactory.decodeResource(context.resources, resId, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= reqW && bounds.outHeight / (sample * 2) >= reqH) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeResource(context.resources, resId, opts)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun decodeLogo(context: Context): Bitmap? = try {
        context.resources.openRawResource(R.raw.export_logo_mark).use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    // ───────────────────────── Text helpers ─────────────────────────

    private class Fonts(val regular: Typeface, val medium: Typeface, val semiBold: Typeface, val bold: Typeface)

    private fun loadFonts(context: Context, persian: Boolean): Fonts {
        fun font(id: Int, fallback: Typeface): Typeface =
            try { ResourcesCompat.getFont(context, id) } catch (e: Exception) { null } ?: fallback

        return if (persian) {
            Fonts(
                font(R.font.vazirmatn_regular, Typeface.DEFAULT),
                font(R.font.vazirmatn_medium, Typeface.DEFAULT),
                font(R.font.vazirmatn_semibold, Typeface.DEFAULT_BOLD),
                font(R.font.vazirmatn_bold, Typeface.DEFAULT_BOLD)
            )
        } else {
            Fonts(
                font(R.font.inter_18pt_regular, Typeface.DEFAULT),
                font(R.font.inter_18pt_medium, Typeface.DEFAULT),
                font(R.font.inter_18pt_semibold, Typeface.DEFAULT_BOLD),
                font(R.font.inter_18pt_bold, Typeface.DEFAULT_BOLD)
            )
        }
    }

    private fun localizedAppName(context: Context, persian: Boolean): String {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(if (persian) "fa" else "en"))
        return context.createConfigurationContext(config).getString(R.string.app_name)
    }

    private fun textPaint(face: Typeface, size: Float, color: Int) =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = face
            textSize = size
            this.color = color
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun layout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        maxLines: Int,
        dir: TextDirectionHeuristic,
        align: Layout.Alignment
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, width))
            .setAlignment(align)
            .setTextDirection(dir)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

    /** A layout whose width equals its widest line, so it can be placed by edge or centre exactly. */
    private fun tight(
        text: CharSequence,
        paint: TextPaint,
        maxWidth: Int,
        maxLines: Int,
        dir: TextDirectionHeuristic,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        val safeMax = max(1, maxWidth)
        val first = layout(text, paint, safeMax, maxLines, dir, align)
        var widest = 0f
        for (i in 0 until first.lineCount) widest = max(widest, first.getLineWidth(i))
        val tightWidth = min(safeMax, ceil(widest).toInt() + 2)
        return if (tightWidth >= safeMax) first else layout(text, paint, tightWidth, maxLines, dir, align)
    }

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}