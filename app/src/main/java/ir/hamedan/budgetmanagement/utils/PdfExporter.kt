package ir.hamedan.budgetmanagement.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class ExportStats(
    val openingBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val averageBalance: Double,
    val currency: String,
    val issueDateMillis: Long,
    val startMillis: Long,
    val endMillis: Long
)

// ───────────────────────── Page geometry (A4 @ 72dpi, units = pt) ─────────────────────────
private const val PAGE_W = 595f
private const val PAGE_H = 842f
private const val MARGIN = 32f
private const val CONTENT_WIDTH = PAGE_W - MARGIN * 2

private const val FOOTER_TOP = PAGE_H - 40f
private const val CONTENT_BOTTOM = FOOTER_TOP - 10f

private const val TABLE_HEADER_HEIGHT = 30f
private const val ROWS_GAP = 4f
private const val MIN_ROW_HEIGHT = 30f
private const val ROW_V_PAD = 8f

// Fixed vertical layout of the first page (header → cards → info strip → table)
private const val FIRST_HEADER_HEIGHT = 84f
private const val CARDS_TOP = 128f
private const val CARDS_HEIGHT = 72f
private const val INFO_TOP = 210f
private const val INFO_HEIGHT = 44f
private const val FIRST_TABLE_TOP = 268f
private const val OTHER_TABLE_TOP = 80f
private const val FIRST_ROWS_TOP = FIRST_TABLE_TOP + TABLE_HEADER_HEIGHT + ROWS_GAP
private const val OTHER_ROWS_TOP = OTHER_TABLE_TOP + TABLE_HEADER_HEIGHT + ROWS_GAP

// ───────────────────────── Colors ─────────────────────────
private val cBrand = Color.parseColor("#408A71")
private val cBrandDark = Color.parseColor("#2F6B57")
private val cBrandLight = Color.parseColor("#EAF4F0")
private val cAltRow = Color.parseColor("#F6F9F8")
private val cText = Color.parseColor("#1F2A27")
private val cMuted = Color.parseColor("#6B7773")
private val cBorder = Color.parseColor("#D9E2DE")
private val cIncome = Color.parseColor("#1F8A5F")
private val cIncomeBg = Color.parseColor("#E3F4EC")
private val cExpense = Color.parseColor("#C0392B")
private val cExpenseBg = Color.parseColor("#FBE8E6")
private val cNeutral = Color.parseColor("#4D5754")
private val cWhiteSoft = Color.argb(215, 255, 255, 255)

private enum class Align { START, END, CENTER }

private enum class Col(
    val weight: Float,
    val align: Align,
    val pad: Float,
    val titleFa: String,
    val titleEn: String
) {
    INDEX(6f, Align.CENTER, 2f, "ردیف", "#"),
    DATE(14f, Align.CENTER, 4f, "تاریخ", "Date"),
    TITLE(18f, Align.START, 8f, "عنوان", "Title"),
    CATEGORY(15f, Align.START, 8f, "دسته‌بندی", "Category"),
    TYPE(9f, Align.CENTER, 4f, "نوع", "Type"),
    AMOUNT(19f, Align.CENTER, 4f, "مبلغ", "Amount"),
    NOTE(19f, Align.START, 8f, "یادداشت", "Note")
}

private class RowLayout(
    val date: StaticLayout,
    val title: StaticLayout,
    val category: StaticLayout,
    val amount: StaticLayout,
    val note: StaticLayout,
    val typeLabel: String,
    val isIncome: Boolean,
    val otherHeight: Float,   // tallest non-note cell (content only, no padding)
    val height: Float
)

/** A slice of a row placed on one page. Normal rows are one fragment; very long notes are split by line. */
private class Fragment(
    val row: Int,
    val fromLine: Int,
    val toLine: Int,
    val isFirst: Boolean,
    val height: Float
)

object PdfExporter {

    /**
     * @param regularTypeface / boldTypeface  optional custom fonts (e.g. Vazirmatn) for the sharpest
     * Persian rendering. When null, the system fonts are used.
     */
    fun generate(
        context: Context,
        transactions: List<TransactionEntity>,
        stats: ExportStats,
        isPersian: Boolean,
        categoryNames: Map<String, String> = emptyMap(),
        regularTypeface: Typeface? = null,
        boldTypeface: Typeface? = null
    ): File {
        val painter = PdfPainter(
            rtl = isPersian,
            regularFace = regularTypeface ?: Typeface.DEFAULT,
            boldFace = boldTypeface ?: Typeface.DEFAULT_BOLD
        )
        val document = PdfDocument()
        val logo: Bitmap? = context.resources.openRawResource(R.raw.export_logo_mark)
            .use { BitmapFactory.decodeStream(it) }

        try {
            val rows = transactions.map { painter.buildRow(it, stats.currency, categoryNames) }
            val pages = paginate(rows)

            pages.forEachIndexed { pageIndex, fragments ->
                val pageNumber = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val tableTop: Float
                if (pageNumber == 1) {
                    painter.drawFirstHeader(canvas, logo, stats.issueDateMillis)
                    painter.drawSummaryCards(canvas, stats)
                    painter.drawInfoStrip(canvas, stats, rows.size)
                    tableTop = FIRST_TABLE_TOP
                } else {
                    painter.drawCompactHeader(canvas, logo)
                    tableTop = OTHER_TABLE_TOP
                }

                painter.drawTableHeader(canvas, tableTop, stats.currency)
                var y = tableTop + TABLE_HEADER_HEIGHT + ROWS_GAP
                for (fragment in fragments) {
                    painter.drawRow(canvas, fragment, rows[fragment.row], y, fragment.row % 2 == 1)
                    y += fragment.height
                }
                if (rows.isEmpty()) painter.drawEmptyState(canvas, y)

                painter.drawFooter(canvas, pageNumber, pages.size)
                document.finishPage(page)
            }

            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportsDir, "Cidna_Statement_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { document.writeTo(it) }
            return outFile
        } finally {
            document.close()
            if (logo != null && !logo.isRecycled) logo.recycle()
        }
    }

    /**
     * Splits rows into pages using the real space under the table header.
     * A row that fits on a page is never cut. A row taller than a whole page (a huge note) is
     * split line-by-line across as many pages as needed, so no text is ever lost.
     * Always returns at least one page (empty statements still get a page).
     */
    private fun paginate(rows: List<RowLayout>): List<List<Fragment>> {
        val pages = mutableListOf<List<Fragment>>()
        var current = mutableListOf<Fragment>()
        var used = 0f
        var available = CONTENT_BOTTOM - FIRST_ROWS_TOP
        val fullCapacity = CONTENT_BOTTOM - OTHER_ROWS_TOP

        fun newPage() {
            pages.add(current)
            current = mutableListOf()
            used = 0f
            available = fullCapacity
        }

        rows.forEachIndexed { i, row ->
            if (row.height <= fullCapacity) {
                if (used > 0f && used + row.height > available) newPage()
                current.add(Fragment(i, 0, row.note.lineCount, true, row.height))
                used += row.height
            } else {
                val note = row.note
                val lines = note.lineCount
                var from = 0
                var first = true
                while (from < lines) {
                    val base = if (first) row.otherHeight else 0f
                    val firstLineH = (note.getLineBottom(from) - note.getLineTop(from)).toFloat()
                    val minNeeded = ROW_V_PAD * 2 + max(base, firstLineH)
                    if (used > 0f && available - used < minNeeded) newPage()
                    val space = available - used
                    var to = from
                    while (to < lines && note.getLineBottom(to) - note.getLineTop(from) <= space - ROW_V_PAD * 2) to++
                    if (to == from) to = from + 1
                    val noteH = (note.getLineBottom(to - 1) - note.getLineTop(from)).toFloat()
                    val h = ROW_V_PAD * 2 + max(base, noteH)
                    current.add(Fragment(i, from, to, first, h))
                    used += h
                    from = to
                    first = false
                    if (from < lines) newPage()
                }
            }
        }
        pages.add(current)
        return pages
    }
}

private class PdfPainter(
    private val rtl: Boolean,
    private val regularFace: Typeface,
    private val boldFace: Typeface
) {
    private val locale: Locale = if (rtl) Locale.forLanguageTag("fa-IR") else Locale.US
    private val moneyFormat: NumberFormat =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
    private val plainFormat: NumberFormat =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0; isGroupingUsed = false }

    private val colWidths: List<Float> = Col.values().map { CONTENT_WIDTH * it.weight / 100f }
    private val colOffsets: List<Float> = run {
        var acc = 0f
        colWidths.map { val o = acc; acc += it; o }
    }

    private fun t(fa: String, en: String) = if (rtl) fa else en

    /** Left x of a box that starts [offset] pt from the reading-start edge (right in Persian). */
    private fun startLeft(offset: Float, width: Float): Float =
        if (rtl) PAGE_W - MARGIN - offset - width else MARGIN + offset

    // ───────────────────────── Text helpers ─────────────────────────

    private fun layout(
        value: String,
        width: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        align: Align = Align.START,
        ltr: Boolean = false,
        maxLines: Int = 1
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) boldFace else regularFace
        }
        val alignment = when (align) {
            Align.START -> Layout.Alignment.ALIGN_NORMAL
            Align.END -> Layout.Alignment.ALIGN_OPPOSITE
            Align.CENTER -> Layout.Alignment.ALIGN_CENTER
        }
        val direction = if (ltr || !rtl) TextDirectionHeuristics.LTR else TextDirectionHeuristics.RTL
        return StaticLayout.Builder.obtain(value, 0, value.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(alignment)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.2f)
            .setTextDirection(direction)
            .apply {
                if (maxLines != Int.MAX_VALUE) {
                    setMaxLines(maxLines)
                    setEllipsize(TextUtils.TruncateAt.END)
                }
            }
            .build()
    }

    private fun draw(canvas: Canvas, layout: StaticLayout, left: Float, top: Float) {
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun text(
        canvas: Canvas,
        value: String,
        left: Float,
        top: Float,
        width: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        align: Align = Align.START,
        ltr: Boolean = false,
        maxLines: Int = 1
    ): StaticLayout {
        val l = layout(value, width, size, color, bold, align, ltr, maxLines)
        draw(canvas, l, left, top)
        return l
    }

    private fun measure(value: String, size: Float, bold: Boolean): Float {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) boldFace else regularFace
        }
        return p.measureText(value)
    }

    /** Largest font size (<= [start]) at which [value] fits on one line. */
    private fun fit(value: String, maxWidth: Float, start: Float, min: Float, bold: Boolean): Float {
        var s = start
        while (s > min) {
            if (measure(value, s, bold) <= maxWidth) return s
            s -= 0.5f
        }
        return min
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = width
        style = Paint.Style.STROKE
    }

    // ───────────────────────── Formatting ─────────────────────────

    private fun unitLabel(currency: String) =
        if (currency.equals("IRR", true)) t("ریال", "Rial") else t("تومان", "Toman")

    private fun formatMoney(value: Double, currency: String, forceSign: Boolean = false): String {
        val display = if (currency.equals("IRR", true)) value * 10 else value
        val body = moneyFormat.format(abs(display))
        return when {
            display < 0 -> "-$body"
            forceSign && display > 0 -> "+$body"
            else -> body
        }
    }

    // ───────────────────────── Row building ─────────────────────────

    private fun colInner(col: Col) = colWidths[col.ordinal] - col.pad * 2

    fun buildRow(tx: TransactionEntity, currency: String, categoryNames: Map<String, String>): RowLayout {
        val isIncome = tx.type == "INCOME"
        val title = tx.title.ifBlank { t("بدون عنوان", "Untitled") }
        val category = categoryNames[tx.categoryId]
            ?: tx.categoryId.takeUnless { it.isBlank() || it.equals("UNCATEGORIZED", true) }
            ?: t("دسته‌بندی نشده", "Uncategorized")
        val note = tx.note.ifBlank { "—" }
        val signed = tx.amount.toDouble().let { if (isIncome) it else -it }
        val amountText = formatMoney(signed, currency, forceSign = true)
        val amountColor = if (isIncome) cIncome else cExpense

        val dateL = layout(
            DateUtils.formatTimestamp(tx.timestamp, rtl), colInner(Col.DATE), 8.5f, cText,
            align = Align.CENTER, maxLines = 2
        )
        val titleL = layout(title, colInner(Col.TITLE), 9.5f, cText, bold = true, maxLines = 10)
        val categoryL = layout(category, colInner(Col.CATEGORY), 9f, cText, maxLines = 10)
        val amountSize = fit(amountText, colInner(Col.AMOUNT), 9.5f, 6.5f, true)
        val amountL = layout(
            amountText, colInner(Col.AMOUNT), amountSize, amountColor,
            bold = true, align = Align.CENTER, ltr = true
        )
        val noteL = layout(note, colInner(Col.NOTE), 8.5f, cMuted, maxLines = Int.MAX_VALUE)

        val otherHeight = listOf(dateL, titleL, categoryL, amountL).maxOf { it.height }.toFloat()
        val content = max(otherHeight, noteL.height.toFloat())
        val height = max(MIN_ROW_HEIGHT, content + ROW_V_PAD * 2)

        return RowLayout(
            date = dateL,
            title = titleL,
            category = categoryL,
            amount = amountL,
            note = noteL,
            typeLabel = if (isIncome) t("درآمد", "Income") else t("هزینه", "Expense"),
            isIncome = isIncome,
            otherHeight = otherHeight,
            height = height
        )
    }

    // ───────────────────────── Page 1 header ─────────────────────────

    fun drawFirstHeader(canvas: Canvas, logo: Bitmap?, issueMillis: Long) {
        val top = MARGIN
        val rect = RectF(MARGIN, top, PAGE_W - MARGIN, top + FIRST_HEADER_HEIGHT)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, cBrandDark, cBrand, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, 16f, 16f, bg)

        val tile = 48f
        val inset = 18f
        val tileLeft = startLeft(inset, tile)
        val tileTop = top + (FIRST_HEADER_HEIGHT - tile) / 2f
        canvas.drawRoundRect(RectF(tileLeft, tileTop, tileLeft + tile, tileTop + tile), 12f, 12f, fill(Color.WHITE))
        if (logo != null) {
            drawBitmapContain(canvas, logo, RectF(tileLeft + 7f, tileTop + 7f, tileLeft + tile - 7f, tileTop + tile - 7f))
        }

        val issueWidth = 124f
        val textOffset = inset + tile + 14f
        val issueOffset = CONTENT_WIDTH - inset - issueWidth
        val textWidth = issueOffset - 14f - textOffset

        // Title + subtitle
        val title = layout(t("گزارش مالی سیدنا", "Cidna Financial Statement"), textWidth, 17f, Color.WHITE, bold = true)
        val subtitle = layout(t("صورتحساب تراکنش‌ها", "Transaction statement"), textWidth, 9f, cWhiteSoft)
        var y = top + (FIRST_HEADER_HEIGHT - (title.height + 4f + subtitle.height)) / 2f
        draw(canvas, title, startLeft(textOffset, textWidth), y)
        y += title.height + 4f
        draw(canvas, subtitle, startLeft(textOffset, textWidth), y)

        // Divider + issue date (opposite side)
        val dividerX = startLeft(issueOffset - 8f, 0f)
        canvas.drawLine(
            dividerX, top + 22f, dividerX, top + FIRST_HEADER_HEIGHT - 22f,
            stroke(Color.argb(90, 255, 255, 255), 1f)
        )
        val issueValue = DateUtils.formatTimestamp(issueMillis, rtl)
        val label = layout(t("تاریخ صدور", "Issue date"), issueWidth, 8.5f, cWhiteSoft)
        val valueSize = fit(issueValue, issueWidth, 10.5f, 7.5f, true)
        val value = layout(issueValue, issueWidth, valueSize, Color.WHITE, bold = true)
        var iy = top + (FIRST_HEADER_HEIGHT - (label.height + 4f + value.height)) / 2f
        draw(canvas, label, startLeft(issueOffset, issueWidth), iy)
        iy += label.height + 4f
        draw(canvas, value, startLeft(issueOffset, issueWidth), iy)
    }

    fun drawCompactHeader(canvas: Canvas, logo: Bitmap?) {
        val top = MARGIN
        val size = 28f
        var textOffset = 0f
        if (logo != null) {
            val left = startLeft(0f, size)
            drawBitmapContain(canvas, logo, RectF(left, top, left + size, top + size))
            textOffset = size + 10f
        }
        val width = 320f
        val l = layout(t("گزارش مالی سیدنا", "Cidna Financial Statement"), width, 12f, cBrand, bold = true)
        draw(canvas, l, startLeft(textOffset, width), top + (size - l.height) / 2f)
        val lineY = top + size + 8f
        canvas.drawLine(MARGIN, lineY, PAGE_W - MARGIN, lineY, stroke(cBrand, 1.2f))
    }

    // ───────────────────────── Summary cards ─────────────────────────

    private class Card(val label: String, val value: String, val accent: Int, val filled: Boolean)

    fun drawSummaryCards(canvas: Canvas, stats: ExportStats) {
        val gap = 8f
        val w = (CONTENT_WIDTH - gap * 3) / 4f
        val unit = unitLabel(stats.currency)
        val cards = listOf(
            Card(t("مانده از قبل", "Opening balance"), formatMoney(stats.openingBalance, stats.currency), cNeutral, false),
            Card(t("جمع واریز", "Total income"), formatMoney(stats.totalIncome, stats.currency), cIncome, false),
            Card(t("جمع برداشت", "Total expense"), formatMoney(stats.totalExpense, stats.currency), cExpense, false),
            Card(t("مانده", "Balance"), formatMoney(stats.balance, stats.currency), cBrand, true)
        )
        cards.forEachIndexed { i, card ->
            val left = startLeft(i * (w + gap), w)
            val rect = RectF(left, CARDS_TOP, left + w, CARDS_TOP + CARDS_HEIGHT)
            if (card.filled) {
                canvas.drawRoundRect(rect, 12f, 12f, fill(cBrand))
            } else {
                canvas.drawRoundRect(rect, 12f, 12f, fill(Color.WHITE))
                canvas.drawRoundRect(rect, 12f, 12f, stroke(cBorder, 0.9f))
                canvas.drawRoundRect(
                    RectF(left + 16f, CARDS_TOP, left + w - 16f, CARDS_TOP + 3.5f), 2f, 2f, fill(card.accent)
                )
            }
            val soft = if (card.filled) cWhiteSoft else cMuted
            val strong = if (card.filled) Color.WHITE else card.accent

            text(canvas, card.label, left + 6f, CARDS_TOP + 14f, w - 12f, 8.5f, soft, align = Align.CENTER)
            val size = fit(card.value, w - 16f, 13.5f, 8f, true)
            text(canvas, card.value, left + 8f, CARDS_TOP + 30f, w - 16f, size, strong, bold = true, align = Align.CENTER, ltr = true)
            text(canvas, unit, left + 6f, CARDS_TOP + 53f, w - 12f, 8f, soft, align = Align.CENTER)
        }
    }

    fun drawInfoStrip(canvas: Canvas, stats: ExportStats, txCount: Int) {
        val rect = RectF(MARGIN, INFO_TOP, PAGE_W - MARGIN, INFO_TOP + INFO_HEIGHT)
        canvas.drawRoundRect(rect, 12f, 12f, fill(cBrandLight))

        val from = DateUtils.formatTimestamp(stats.startMillis, rtl)
        val to = DateUtils.formatTimestamp(stats.endMillis, rtl)
        val items = listOf(
            t("بازه گزارش", "Statement period") to t("از $from تا $to", "$from  –  $to"),
            t("نوع ارز", "Currency") to unitLabel(stats.currency),
            t("تعداد تراکنش", "Transactions") to plainFormat.format(txCount)
        )
        val weights = floatArrayOf(0.44f, 0.28f, 0.28f)
        var offset = 0f
        items.forEachIndexed { i, (label, value) ->
            val w = CONTENT_WIDTH * weights[i]
            val left = startLeft(offset, w)
            text(canvas, label, left + 8f, INFO_TOP + 9f, w - 16f, 8.5f, cMuted, align = Align.CENTER)
            val size = fit(value, w - 16f, 10.5f, 7.5f, true)
            text(canvas, value, left + 8f, INFO_TOP + 24f, w - 16f, size, cText, bold = true, align = Align.CENTER)
            if (i > 0) {
                val x = startLeft(offset, 0f)
                canvas.drawLine(x, INFO_TOP + 10f, x, INFO_TOP + INFO_HEIGHT - 10f, stroke(cBorder, 0.9f))
            }
            offset += w
        }
    }

    // ───────────────────────── Table ─────────────────────────

    fun drawTableHeader(canvas: Canvas, top: Float, currency: String) {
        val rect = RectF(MARGIN, top, PAGE_W - MARGIN, top + TABLE_HEADER_HEIGHT)
        canvas.drawRoundRect(rect, 8f, 8f, fill(cNeutral))

        Col.values().forEach { col ->
            var title = t(col.titleFa, col.titleEn)
            if (col == Col.AMOUNT) title = "$title (${unitLabel(currency)})"
            val width = colInner(col)
            val l = layout(title, width, 8.5f, Color.WHITE, bold = true, align = col.align, maxLines = 2)
            val left = startLeft(colOffsets[col.ordinal], colWidths[col.ordinal]) + col.pad
            draw(canvas, l, left, top + (TABLE_HEADER_HEIGHT - l.height) / 2f)
        }
    }

    fun drawRow(canvas: Canvas, fragment: Fragment, row: RowLayout, top: Float, alternate: Boolean) {
        val h = fragment.height
        if (alternate) canvas.drawRect(MARGIN, top, PAGE_W - MARGIN, top + h, fill(cAltRow))
        canvas.drawLine(MARGIN, top + h, PAGE_W - MARGIN, top + h, stroke(cBorder, 0.6f))

        fun cell(col: Col, l: StaticLayout) {
            val left = startLeft(colOffsets[col.ordinal], colWidths[col.ordinal]) + col.pad
            draw(canvas, l, left, top + (h - l.height) / 2f)
        }

        // Note column: whole note, or only the lines that belong to this fragment.
        val note = row.note
        if (fragment.fromLine == 0 && fragment.toLine == note.lineCount) {
            cell(Col.NOTE, note)
        } else {
            val left = startLeft(colOffsets[Col.NOTE.ordinal], colWidths[Col.NOTE.ordinal]) + Col.NOTE.pad
            val sliceTop = note.getLineTop(fragment.fromLine)
            val sliceH = (note.getLineBottom(fragment.toLine - 1) - sliceTop).toFloat()
            val y = top + ROW_V_PAD
            canvas.save()
            canvas.clipRect(left, y, left + colInner(Col.NOTE), y + sliceH)
            canvas.translate(left, y - sliceTop)
            note.draw(canvas)
            canvas.restore()
        }

        if (!fragment.isFirst) {
            val hint = layout(
                t("ادامه‌ی یادداشت ردیف ${plainFormat.format(fragment.row + 1)}", "Note continued (row ${fragment.row + 1})"),
                colInner(Col.TITLE) + colWidths[Col.DATE.ordinal], 8f, cMuted
            )
            val left = startLeft(colOffsets[Col.DATE.ordinal], colWidths[Col.DATE.ordinal] + colWidths[Col.TITLE.ordinal]) + 4f
            draw(canvas, hint, left, top + ROW_V_PAD)
            return
        }

        Col.values().forEach { col ->
            when (col) {
                Col.INDEX -> cell(
                    col,
                    layout(plainFormat.format(fragment.row + 1), colInner(col), 8.5f, cMuted, align = Align.CENTER)
                )
                Col.DATE -> cell(col, row.date)
                Col.TITLE -> cell(col, row.title)
                Col.CATEGORY -> cell(col, row.category)
                Col.AMOUNT -> cell(col, row.amount)
                Col.TYPE -> drawTypePill(canvas, row, top, h)
                Col.NOTE -> Unit
            }
        }
    }

    private fun drawTypePill(canvas: Canvas, row: RowLayout, top: Float, rowHeight: Float) {
        val col = Col.TYPE
        val colW = colWidths[col.ordinal]
        val textColor = if (row.isIncome) cIncome else cExpense
        val bgColor = if (row.isIncome) cIncomeBg else cExpenseBg
        val pillW = minOf(colW - 8f, measure(row.typeLabel, 8f, true) + 14f)
        val pillH = 16f
        val pillLeft = startLeft(colOffsets[col.ordinal], colW) + (colW - pillW) / 2f
        val pillTop = top + (rowHeight - pillH) / 2f
        canvas.drawRoundRect(RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH), pillH / 2f, pillH / 2f, fill(bgColor))
        val l = layout(row.typeLabel, pillW, 8f, textColor, bold = true, align = Align.CENTER)
        draw(canvas, l, pillLeft, pillTop + (pillH - l.height) / 2f)
    }

    fun drawEmptyState(canvas: Canvas, top: Float) {
        val rect = RectF(MARGIN, top + 12f, PAGE_W - MARGIN, top + 72f)
        canvas.drawRoundRect(rect, 12f, 12f, fill(cBrandLight))
        val l = layout(
            t("تراکنشی برای این بازه ثبت نشده است", "No transactions were recorded in this period"),
            rect.width() - 24f, 10.5f, cBrand, bold = true, align = Align.CENTER
        )
        draw(canvas, l, rect.left + 12f, rect.centerY() - l.height / 2f)
    }

    // ───────────────────────── Footer ─────────────────────────

    fun drawFooter(canvas: Canvas, page: Int, total: Int) {
        canvas.drawLine(MARGIN, FOOTER_TOP, PAGE_W - MARGIN, FOOTER_TOP, stroke(cBorder, 0.8f))
        val y = FOOTER_TOP + 9f
        val w = 220f
        text(canvas, t("تهیه‌شده با سیدنا", "Generated by Cidna"), startLeft(0f, w), y, w, 8.5f, cMuted)
        text(
            canvas,
            t("صفحه ${plainFormat.format(page)} از ${plainFormat.format(total)}", "Page $page of $total"),
            startLeft(CONTENT_WIDTH - w, w), y, w, 8.5f, cMuted, align = Align.END
        )
    }

    private fun drawBitmapContain(canvas: Canvas, bitmap: Bitmap, dst: RectF) {
        val scale = minOf(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val left = dst.centerX() - w / 2f
        val top = dst.centerY() - h / 2f
        canvas.drawBitmap(
            bitmap, null, RectF(left, top, left + w, top + h),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        )
    }
}