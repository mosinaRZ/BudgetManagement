package ir.hamedan.budgetmanagement.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.graphics.pdf.PdfDocument
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
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

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 28f
    private const val FOOTER_HEIGHT = 30f
    private const val TABLE_HEADER_HEIGHT = 32f
    private const val FIRST_PAGE_TOP = 255f
    private const val OTHER_PAGE_TOP = 72f
    private const val MIN_ROW_HEIGHT = 36f

    private const val BRAND = "#408A71"
    private const val BRAND_LIGHT = "#EAF4F0"
    private const val ALT_ROW = "#F5F8F7"
    private const val HEADER_GRAY = "#4D5754"
    private const val TEXT = "#26332F"
    private const val MUTED = "#6B7773"
    private const val BORDER = "#D4DEDA"
    private const val INCOME = "#2E8B67"
    private const val EXPENSE = "#C65B5B"

    private data class Column(val key: String, val weight: Float, val titleFa: String, val titleEn: String)

    private val logicalColumns = listOf(
        Column("index", 6f, "ردیف", "#"),
        Column("date", 13f, "تاریخ", "Date"),
        Column("title", 17f, "عنوان", "Title"),
        Column("category", 15f, "دسته‌بندی", "Category"),
        Column("type", 11f, "نوع", "Type"),
        Column("amount", 17f, "مبلغ", "Amount"),
        Column("note", 21f, "یادداشت", "Note")
    )

    fun generate(
        context: Context,
        transactions: List<TransactionEntity>,
        stats: ExportStats,
        isPersian: Boolean,
        categoryNames: Map<String, String> = emptyMap()
    ): File {
        val document = PdfDocument()
        val logo = context.resources.openRawResource(R.raw.export_logo_mark).use { BitmapFactory.decodeStream(it) }
        val rows = transactions.map { buildRow(it, stats.currency, isPersian, categoryNames) }
        val pages = paginate(rows, isPersian)

        val ranges = if (pages.isEmpty()) listOf(0 until 0) else pages
        ranges.forEachIndexed { pageIndex, range ->
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var y = MARGIN

            if (pageNumber == 1) {
                y = drawHeader(canvas, logo, isPersian, y)
                y = drawSummary(canvas, stats, isPersian, y)
                y = drawStatementLine(canvas, stats, isPersian, y)
            } else {
                y = drawCompactHeader(canvas, logo, isPersian, y)
            }

            y = drawTableHeader(canvas, isPersian, y)
            val heights = mutableListOf<Float>()
            var rowIndex = range.first
            for (index in range) {
                val row = rows[index]
                val h = max(MIN_ROW_HEIGHT, row.height)
                drawRow(canvas, rowIndex + 1, row, isPersian, y, h, (rowIndex - range.first) % 2 == 1)
                heights += h
                y += h
                rowIndex++
            }
            drawTableBorders(canvas, y - heights.sum(), heights, isPersian)

            if (range.isEmpty()) {
                drawCentered(canvas, if (isPersian) "تراکنشی برای این بازه ثبت نشده است" else "No transactions were recorded in this period", PAGE_WIDTH / 2f, y + 44f, 10f, true, Color.parseColor(BRAND), isPersian)
            }

            drawFooter(canvas, pageNumber, ranges.size, isPersian)
            document.finishPage(page)
        }

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportsDir, "Cidna_Statement_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
        logo.recycleIfNeeded()
        return outFile
    }


    private fun paginate(rows: List<RowLayout>, isPersian: Boolean): List<IntRange> {
        if (rows.isEmpty()) return emptyList()
        val firstAvailable = PAGE_HEIGHT - FIRST_PAGE_TOP - FOOTER_HEIGHT
        val otherAvailable = PAGE_HEIGHT - OTHER_PAGE_TOP - FOOTER_HEIGHT
        val pages = mutableListOf<IntRange>()
        var start = 0
        var cursor = 0
        var available = firstAvailable
        while (cursor < rows.size) {
            var used = 0f
            var end = cursor
            while (end < rows.size) {
                val h = max(MIN_ROW_HEIGHT, rows[end].height)
                if (end > cursor && used + h > available) break
                used += h
                end++
                if (used >= available) break
            }
            if (end == cursor) end++
            pages += start until end
            start = end
            cursor = end
            available = otherAvailable
        }
        return pages
    }

    private data class RowLayout(
        val timestamp: Long,
        val title: String,
        val category: String,
        val type: String,
        val amount: String,
        val note: String,
        val height: Float
    )

    private fun buildRow(
        tx: TransactionEntity,
        currency: String,
        isPersian: Boolean,
        categoryNames: Map<String, String>
    ): RowLayout {
        val title = tx.title.ifBlank { if (isPersian) "بدون عنوان" else "Untitled" }
        val category = categoryNames[tx.categoryId]
            ?: tx.categoryId.ifBlank { if (isPersian) "دسته‌بندی نشده" else "Uncategorized" }
        val type = if (tx.type == "INCOME") if (isPersian) "درآمد" else "Income" else if (isPersian) "هزینه" else "Expense"
        val amount = formatAmount(tx.amount.toDouble(), currency, isPersian)
        val note = tx.note.ifBlank { "—" }
        val widths = widths()
        val noteWidth = widths.last() - 14f
        val noteLayout = buildLayout(note, noteWidth, isPersian, 8.4f, Layout.Alignment.ALIGN_OPPOSITE)
        val titleLayout = buildLayout(title, widths[2] - 10f, isPersian, 8.2f, Layout.Alignment.ALIGN_CENTER)
        val categoryLayout = buildLayout(category, widths[3] - 10f, isPersian, 8.2f, Layout.Alignment.ALIGN_CENTER)
        val height = max(MIN_ROW_HEIGHT, noteLayout.height + 14f)
        return RowLayout(tx.timestamp, title, category, type, amount, note, height)
    }

    private fun buildLayout(text: String, width: Float, rtl: Boolean, size: Float, alignment: Layout.Alignment): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(TEXT)
            textSize = size
            typeface = Typeface.DEFAULT
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(12))
            .setAlignment(alignment)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.16f)
            .setTextDirection(if (rtl) TextDirectionHeuristics.FIRSTSTRONG_RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .build()
    }

    private fun widths(): List<Float> {
        val total = PAGE_WIDTH - MARGIN * 2
        return logicalColumns.map { total * it.weight / 100f }
    }

    private fun drawHeader(canvas: Canvas, logo: Bitmap?, isPersian: Boolean, startY: Float): Float {
        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(BRAND) }
        canvas.drawRoundRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + 76f, 18f, 18f, brand)
        if (logo != null) {
            val size = 48f
            val left = PAGE_WIDTH / 2f - size / 2f
            val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(left - 9f, startY + 7f, left + size + 9f, startY + size + 7f, 16f, 16f, white)
            drawBitmapContain(canvas, logo, RectF(left, startY + 7f, left + size, startY + size + 7f))
        }
        drawCentered(canvas, if (isPersian) "گزارش مالی سیدنا" else "Cidna Financial Statement", PAGE_WIDTH / 2f, startY + 65f, 15f, true, Color.WHITE, isPersian)
        return startY + 92f
    }

    private fun drawCompactHeader(canvas: Canvas, logo: Bitmap?, isPersian: Boolean, startY: Float): Float {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(BRAND); strokeWidth = 1.5f }
        canvas.drawLine(MARGIN, startY + 42f, PAGE_WIDTH - MARGIN, startY + 42f, line)
        if (logo != null) drawBitmapContain(canvas, logo, RectF(PAGE_WIDTH / 2f - 20f, startY, PAGE_WIDTH / 2f + 20f, startY + 38f))
        drawCentered(canvas, if (isPersian) "گزارش مالی سیدنا" else "Cidna Financial Statement", PAGE_WIDTH / 2f, startY + 59f, 10f, true, Color.parseColor(BRAND), isPersian)
        return startY + 70f
    }

    private fun drawSummary(canvas: Canvas, stats: ExportStats, isPersian: Boolean, startY: Float): Float {
        val top = startY
        val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(BRAND_LIGHT) }
        canvas.drawRoundRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + 112f, 14f, 14f, box)
        val leftX = MARGIN + 16f
        val rightX = PAGE_WIDTH - MARGIN - 16f
        val labels = listOf(
            if (isPersian) "مانده از قبل" else "Opening Balance",
            if (isPersian) "جمع واریز" else "Total Income",
            if (isPersian) "جمع برداشت" else "Total Expense",
            if (isPersian) "مانده" else "Balance",
            if (isPersian) "نوع ارز" else "Currency",
            if (isPersian) "تاریخ صدور" else "Issue Date"
        )
        val values = listOf(
            formatAmount(stats.openingBalance, stats.currency, isPersian),
            formatAmount(stats.totalIncome, stats.currency, isPersian),
            formatAmount(stats.totalExpense, stats.currency, isPersian),
            formatAmount(stats.balance, stats.currency, isPersian),
            currencyLabel(stats.currency, isPersian),
            DateUtils.formatTimestamp(stats.issueDateMillis, isPersian)
        )
        labels.forEachIndexed { i, label ->
            val col = i % 2
            val row = i / 2
            val x = if (isPersian) rightX - col * 270f else leftX + col * 270f
            val y = top + 20f + row * 34f
            drawSingleLine(canvas, label, x, y, 7.7f, false, MUTED, isPersian, if (isPersian) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL, 250f)
            drawSingleLine(canvas, values[i], x, y + 12f, 9.3f, true, TEXT, isPersian, if (isPersian) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL, 250f)
        }
        return top + 124f
    }

    private fun drawStatementLine(canvas: Canvas, stats: ExportStats, isPersian: Boolean, startY: Float): Float {
        val from = DateUtils.formatTimestamp(stats.startMillis, isPersian)
        val to = DateUtils.formatTimestamp(stats.endMillis, isPersian)
        drawCentered(canvas, if (isPersian) "صورتحساب از تاریخ $from تا $to" else "Statement from $from to $to", PAGE_WIDTH / 2f, startY + 12f, 9.5f, true, Color.parseColor(BRAND), isPersian)
        return startY + 28f
    }

    private fun drawTableHeader(canvas: Canvas, isPersian: Boolean, startY: Float): Float {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(HEADER_GRAY) }
        canvas.drawRoundRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + TABLE_HEADER_HEIGHT, 7f, 7f, bg)
        val cols = if (isPersian) logicalColumns.reversed() else logicalColumns
        val ws = if (isPersian) widths().reversed() else widths()
        var x = MARGIN
        cols.forEachIndexed { i, col ->
            drawCentered(canvas, if (isPersian) col.titleFa else col.titleEn, x + ws[i] / 2f, startY + 20f, 7.7f, true, Color.WHITE, isPersian)
            x += ws[i]
        }
        return startY + TABLE_HEADER_HEIGHT + 3f
    }

    private fun drawRow(canvas: Canvas, number: Int, row: RowLayout, isPersian: Boolean, y: Float, rowHeight: Float, alternate: Boolean) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(if (alternate) ALT_ROW else "#FFFFFF") }
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, bg)

        val values = mapOf(
            "index" to number.toString(),
            "date" to DateUtils.formatTimestamp(row.timestamp, isPersian),
            "title" to row.title,
            "category" to row.category,
            "type" to row.type,
            "amount" to row.amount,
            "note" to row.note
        )
        val cols = if (isPersian) logicalColumns.reversed() else logicalColumns
        val ws = if (isPersian) widths().reversed() else widths()
        var x = MARGIN
        cols.forEachIndexed { i, col ->
            val value = values[col.key].orEmpty()
            val alignment = if (col.key == "note" && isPersian) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_CENTER
            val size = if (col.key == "note") 8.1f else 8.0f
            val layout = buildLayout(value, ws[i] - 10f, isPersian, size, alignment)
            canvas.save()
            canvas.translate(x + 5f, y + max(6f, (rowHeight - layout.height) / 2f))
            layout.draw(canvas)
            canvas.restore()
            x += ws[i]
        }
    }

    private fun drawTableBorders(canvas: Canvas, top: Float, heights: List<Float>, isPersian: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(BORDER); strokeWidth = 0.7f }
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        var y = top
        canvas.drawLine(left, y, right, y, paint)
        heights.forEach { y += it; canvas.drawLine(left, y, right, y, paint) }
        var x = left
        val ws = if (isPersian) widths().reversed() else widths()
        ws.dropLast(1).forEach { w -> x += w; canvas.drawLine(x, top, x, y, paint) }
    }

    private fun drawFooter(canvas: Canvas, page: Int, total: Int, isPersian: Boolean) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(BORDER); strokeWidth = 0.8f }
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 32f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 32f, line)
        drawCentered(canvas, if (isPersian) "صفحه $page از $total" else "Page $page of $total", PAGE_WIDTH / 2f, PAGE_HEIGHT - 14f, 7.5f, false, Color.parseColor(MUTED), isPersian)
    }

    private fun drawSingleLine(canvas: Canvas, text: String, x: Float, y: Float, size: Float, bold: Boolean, color: String, rtl: Boolean, alignment: Layout.Alignment, width: Float) {
        val layout = buildLayout(text, width, rtl, size, alignment)
        val left = when (alignment) {
            Layout.Alignment.ALIGN_OPPOSITE -> x - width
            Layout.Alignment.ALIGN_CENTER -> x - width / 2f
            else -> x
        }
        canvas.save(); canvas.translate(left, y - size); layout.draw(canvas); canvas.restore()
    }

    private fun drawCentered(canvas: Canvas, text: String, centerX: Float, baseline: Float, size: Float, bold: Boolean, color: Int, rtl: Boolean) {
        val layout = buildLayout(text, PAGE_WIDTH - MARGIN * 2, rtl, size, Layout.Alignment.ALIGN_CENTER)
        canvas.save(); canvas.translate(MARGIN, baseline - layout.height + 2f); layout.draw(canvas); canvas.restore()
    }

    private fun drawBitmapContain(canvas: Canvas, bitmap: Bitmap, dst: RectF) {
        val scale = minOf(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val left = dst.centerX() - w / 2f
        val top = dst.centerY() - h / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
    }

    private fun formatAmount(amount: Double, currencyCode: String, isPersian: Boolean): String {
        val display = if (currencyCode.equals("IRR", true)) amount * 10 else amount
        val locale = if (isPersian) Locale("fa", "IR") else Locale.US
        val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        return "${number.format(display)} ${currencyLabel(currencyCode, isPersian)}"
    }

    private fun currencyLabel(currencyCode: String, isPersian: Boolean): String = if (currencyCode.equals("IRR", true)) {
        if (isPersian) "ریال" else "Rial"
    } else {
        if (isPersian) "تومان" else "Toman"
    }

    private fun Bitmap.recycleIfNeeded() {
        if (!isRecycled) recycle()
    }
}