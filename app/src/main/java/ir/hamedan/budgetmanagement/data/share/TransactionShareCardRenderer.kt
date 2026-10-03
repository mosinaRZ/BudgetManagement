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
 * Draws the "share transaction" card (exchange-style PnL card) with plain Android
 * Canvas so the output is a real, high-resolution bitmap that does not depend on
 * what is currently on screen.
 *
 * Layout is authored in a 1080x1350 design space and rendered at [OUTPUT_SCALE]x.
 * Everything that has a reading direction (header, pill, rows) is mirrored for RTL.
 *
 * Backgrounds: drawable resources named `share_bg_01` .. `share_bg_12`
 * (put them in res/drawable-nodpi). A missing file silently falls back to a
 * built-in gradient so the feature works before the artwork is added.
 */
object TransactionShareCardRenderer {

    const val BACKGROUND_COUNT = 12
    const val DESIGN_WIDTH = 1080
    const val DESIGN_HEIGHT = 1350
    const val ASPECT_RATIO = 1080f / 1350f
    private const val OUTPUT_SCALE = 1.5f

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

    /** Small preview of one background (with the same dark scrim the card uses). */
    fun renderBackgroundThumbnail(context: Context, index: Int, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(width / DESIGN_WIDTH.toFloat(), height / DESIGN_HEIGHT.toFloat())
        drawBackground(canvas, context, index, width, height)
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

        drawBackground(canvas, context, backgroundIndex, outW, outH)

        // ───────────── Header: logo + app name  |  type pill ─────────────
        val headerTop = 72f
        val tile = 96f
        val tileX = if (rtl) w - margin - tile else margin
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

        val pillPaint = textPaint(fonts.semiBold, 34f, accent)
        val pillText = tight(content.typeLabel, pillPaint, 360, 1, dirApp)
        val pillH = 68f
        val pillPadH = 28f
        val dotD = 18f
        val dotGap = 14f
        val pillW = pillPadH + dotD + dotGap + pillText.width + pillPadH
        val pillX = if (rtl) margin else w - margin - pillW
        val pillY = headerTop + (tile - pillH) / 2f
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
        val headerBottom = headerTop + tile

        // ───────────── Footer ─────────────
        val footerLineY = h - 176f
        canvas.drawRect(margin, footerLineY, w - margin, footerLineY + 2f, fill(Color.argb(46, 255, 255, 255)))
        val brandText = if (rtl) "ساخته‌شده با $appName" else "Made with $appName"
        val brandLayout = tight(
            brandText, textPaint(fonts.medium, 32f, Color.argb(220, 255, 255, 255)),
            contentW.toInt(), 1, dirApp, Layout.Alignment.ALIGN_CENTER
        )
        drawLayout(canvas, brandLayout, (w - brandLayout.width) / 2f, footerLineY + 28f)
        if (content.referenceId.isNotBlank()) {
            val refLabel = if (rtl) "شناسه تراکنش" else "Transaction ID"
            val refLayout = tight(
                "$refLabel: ${content.referenceId}",
                textPaint(fonts.regular, 26f, Color.argb(140, 255, 255, 255)),
                contentW.toInt(), 1, dirApp, Layout.Alignment.ALIGN_CENTER
            )
            drawLayout(canvas, refLayout, (w - refLayout.width) / 2f, footerLineY + 28f + brandLayout.height + 6f)
        }

        // ───────────── Hero + details card (measured first, then centered vertically) ─────────────
        val titlePaint = textPaint(fonts.bold, 54f, Color.WHITE).apply {
            setShadowLayer(10f, 0f, 3f, Color.argb(90, 0, 0, 0))
        }
        val titleLayout = tight(content.title, titlePaint, (contentW - 40f).toInt(), 2, dirContent, Layout.Alignment.ALIGN_CENTER)

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
        val currencyLayout = tight(content.currencyLabel, currencyPaint, contentW.toInt(), 1, dirApp, Layout.Alignment.ALIGN_CENTER)

        val cardPadH = 40f
        val innerW = contentW - cardPadH * 2
        val rowPadV = 24f
        val labelPaint = textPaint(fonts.regular, 32f, Color.argb(165, 255, 255, 255))
        val valuePaint = textPaint(fonts.semiBold, 36f, Color.WHITE)
        val notePaint = textPaint(fonts.medium, 34f, Color.argb(240, 255, 255, 255))

        class Row(
            val label: StaticLayout,
            val value: StaticLayout,
            val isNote: Boolean
        ) {
            val height: Float = if (isNote) {
                rowPadV * 2 + label.height + 8f + value.height
            } else {
                rowPadV * 2 + max(label.height, value.height)
            }
        }

        fun simpleRow(label: String, value: String, dir: TextDirectionHeuristic): Row {
            val labelLayout = tight(label, labelPaint, (innerW * 0.45f).toInt(), 1, dirApp)
            val valueMax = (innerW - labelLayout.width - 32f).toInt()
            return Row(labelLayout, tight(value, valuePaint, valueMax, 1, dir), false)
        }

        val rows = ArrayList<Row>(3)
        rows += simpleRow(if (rtl) "دسته‌بندی" else "Category", content.categoryTitle, dirContent)
        rows += simpleRow(if (rtl) "تاریخ و زمان" else "Date & time", content.dateTimeText, dirApp)
        if (content.note.isNotBlank()) {
            rows += Row(
                tight(if (rtl) "یادداشت" else "Note", labelPaint, innerW.toInt(), 1, dirApp),
                tight(content.note, notePaint, innerW.toInt(), 2, dirContent),
                true
            )
        }
        val cardH = rows.sumOf { it.height.toDouble() }.toFloat()

        val availableTop = headerBottom + 24f
        val availableBottom = footerLineY - 28f
        val available = availableBottom - availableTop

        // Try the full emoji badge first, then smaller / no badge when long content needs the room.
        val badgeOptions = floatArrayOf(140f, 112f, 0f)
        var badgeD = badgeOptions.last()
        var gapScale = 1f
        for (option in badgeOptions) {
            val total = heroHeight(option, 1f, titleLayout.height, amountCapHeight, currencyLayout.height, cardH)
            if (total <= available) { badgeD = option; gapScale = 1f; break }
            val squeezed = heroHeight(option, 0.5f, titleLayout.height, amountCapHeight, currencyLayout.height, cardH)
            if (squeezed <= available) { badgeD = option; gapScale = 0.5f; break }
        }
        val total = heroHeight(badgeD, gapScale, titleLayout.height, amountCapHeight, currencyLayout.height, cardH)
        var y = availableTop + max(0f, (available - total) / 2f)

        // Soft accent glow behind the amount (purely decorative, keeps the number readable on any image).
        val glowCy = y + (if (badgeD > 0f) badgeD + 28f * gapScale else 0f) + titleLayout.height + amountCapHeight / 2f
        canvas.drawCircle(
            w / 2f, glowCy, 520f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w / 2f, glowCy, 520f,
                    intArrayOf(withAlpha(accent, 70), withAlpha(accent, 0)),
                    floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                )
            }
        )

        if (badgeD > 0f) {
            val cx = w / 2f
            val cy = y + badgeD / 2f
            canvas.drawCircle(cx, cy, badgeD / 2f, fill(Color.argb(50, 255, 255, 255)))
            canvas.drawCircle(cx, cy, badgeD / 2f, stroke(Color.argb(110, 255, 255, 255), 3f))
            val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = badgeD * 0.52f; textAlign = Paint.Align.CENTER }
            val fm = emojiPaint.fontMetrics
            canvas.drawText(content.categoryEmoji, cx, cy - (fm.ascent + fm.descent) / 2f, emojiPaint)
            y += badgeD + 28f * gapScale
        }

        drawLayout(canvas, titleLayout, (w - titleLayout.width) / 2f, y)
        y += titleLayout.height + 10f * gapScale

        amountPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(content.amountText, (w - amountWidth) / 2f, y + amountCapHeight, amountPaint)
        y += amountCapHeight + 16f * gapScale

        drawLayout(canvas, currencyLayout, (w - currencyLayout.width) / 2f, y)
        y += currencyLayout.height + 48f * gapScale

        // Glass card
        val cardRect = RectF(margin, y, margin + contentW, y + cardH)
        canvas.drawRoundRect(cardRect, 36f, 36f, fill(Color.argb(92, 0, 0, 0)))
        canvas.drawRoundRect(cardRect, 36f, 36f, fill(Color.argb(26, 255, 255, 255)))
        canvas.drawRoundRect(cardRect, 36f, 36f, stroke(Color.argb(64, 255, 255, 255), 2f))

        val innerLeft = margin + cardPadH
        val innerRight = margin + contentW - cardPadH
        var rowY = y
        rows.forEachIndexed { i, row ->
            if (row.isNote) {
                val labelX = if (rtl) innerRight - row.label.width else innerLeft
                drawLayout(canvas, row.label, labelX, rowY + rowPadV)
                val valueX = if (rtl) innerRight - row.value.width else innerLeft
                drawLayout(canvas, row.value, valueX, rowY + rowPadV + row.label.height + 8f)
            } else {
                val labelX = if (rtl) innerRight - row.label.width else innerLeft
                val valueX = if (rtl) innerLeft else innerRight - row.value.width
                val lineH = max(row.label.height, row.value.height).toFloat()
                drawLayout(canvas, row.label, labelX, rowY + rowPadV + (lineH - row.label.height) / 2f)
                drawLayout(canvas, row.value, valueX, rowY + rowPadV + (lineH - row.value.height) / 2f)
            }
            rowY += row.height
            if (i < rows.lastIndex) {
                canvas.drawRect(innerLeft, rowY - 1f, innerRight, rowY + 1f, fill(Color.argb(36, 255, 255, 255)))
            }
        }

        return bitmap
    }

    private fun heroHeight(
        badge: Float, gapScale: Float, titleH: Int, amountH: Float, currencyH: Int, cardH: Float
    ): Float {
        val badgePart = if (badge > 0f) badge + 28f * gapScale else 0f
        return badgePart + titleH + 10f * gapScale + amountH + 16f * gapScale + currencyH + 48f * gapScale + cardH
    }

    // ───────────────────────── Background ─────────────────────────

    /** Draws background + readability scrim into the full [0,0,DESIGN] area of an already scaled canvas. */
    private fun drawBackground(canvas: Canvas, context: Context, index: Int, decodeW: Int, decodeH: Int) {
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
            canvas.drawBitmap(bmp, src, RectF(0f, 0f, w, h), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
            bmp.recycle()
        } else {
            val colors = FALLBACK_PALETTES[safeIndex(index)]
            canvas.drawRect(
                0f, 0f, w, h,
                Paint().apply {
                    shader = LinearGradient(0f, 0f, w, h, colors, floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                }
            )
            canvas.drawCircle(
                w * 0.85f, h * 0.12f, 620f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.85f, h * 0.12f, 620f,
                        intArrayOf(Color.argb(70, 255, 255, 255), Color.argb(0, 255, 255, 255)),
                        floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                    )
                }
            )
        }
        // Dark scrim: guarantees white text stays readable on bright artwork.
        canvas.drawRect(
            0f, 0f, w, h,
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.argb(92, 0, 0, 0), Color.argb(104, 0, 0, 0), Color.argb(186, 0, 0, 0)),
                    floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
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