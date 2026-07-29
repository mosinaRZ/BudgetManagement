package ir.hamedan.budgetmanagement.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

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

    private const val PAGE_WIDTH = 595   // A4 در 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 26f
    private const val HEADER_BLOCK_HEIGHT = 220f // لوگو + استت‌ها + عنوان صورتحساب، فقط صفحه اول
    private const val FOOTER_HEIGHT = 30f
    private const val TABLE_HEADER_HEIGHT = 30f

    fun generate(
        context: Context,
        transactions: List<TransactionEntity>,
        stats: ExportStats,
        isPersian: Boolean
    ): File {
        // --- ۱. محاسبه تعداد صفحات ---
        val usableFirstPage = PAGE_HEIGHT - HEADER_BLOCK_HEIGHT - FOOTER_HEIGHT - TABLE_HEADER_HEIGHT - MARGIN * 2
        val usableOtherPages = PAGE_HEIGHT - FOOTER_HEIGHT - TABLE_HEADER_HEIGHT - MARGIN * 2

        val rowsFirstPage = (usableFirstPage / ROW_HEIGHT).toInt().coerceAtLeast(1)
        val rowsOtherPages = (usableOtherPages / ROW_HEIGHT).toInt().coerceAtLeast(1)

        val totalRows = transactions.size
        val totalPages = if (totalRows <= rowsFirstPage) 1
        else 1 + ceil((totalRows - rowsFirstPage).toDouble() / rowsOtherPages).toInt()

        // --- ۲. رسم واقعی ---
        val document = PdfDocument()

        var rowIndex = 0
        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var y = MARGIN

            if (pageNumber == 1) {
                y = drawHeader(canvas, isPersian, y)
                y = drawStatsBox(canvas, stats, isPersian, y)
                y = drawStatementTitle(canvas, stats, isPersian, y)
            }

            y = drawTableHeader(canvas, isPersian, y)

            val tableStartY = y
            val rowsThisPage = if (pageNumber == 1) rowsFirstPage else rowsOtherPages
            var drawnOnThisPage = 0
            while (rowIndex < totalRows && drawnOnThisPage < rowsThisPage) {
                drawTransactionRow(canvas, rowNumber = rowIndex + 1, tx = transactions[rowIndex], currency = stats.currency, isPersian = isPersian, y = y)
                y += ROW_HEIGHT
                rowIndex++
                drawnOnThisPage++
            }

            // رسم خطوط جدول (افقی و عمودی)
            if (drawnOnThisPage > 0) {
                drawTableBorders(canvas, tableStartY, drawnOnThisPage)
            }

            drawFooter(canvas, pageNumber, totalPages, isPersian)
            document.finishPage(page)
        }

        // --- ۳. ذخیره در cache/exports ---
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "Cidna_Statement_${System.currentTimeMillis()}.pdf"
        val outFile = File(exportsDir, fileName)
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()

        return outFile
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) =
        Paint().apply {
            textSize = size
            isAntiAlias = true
            this.color = color
            textAlign = Paint.Align.LEFT
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    /**
     * وسط‌چین واقعی متن.
     * Paint.Align.CENTER نقطه‌ی X رو مرکز "عرض پیشروی" متن می‌گیره، نه مرکز جوهر واقعیِ حروف؛
     * به همین خاطر با فونت‌های فارسی/بولد معمولاً چشم می‌بینه متن کمی به یک طرف کشیده شده.
     * اینجا با getTextBounds عرض واقعیِ رندرشده رو می‌گیریم و خودمون X دقیق رو حساب می‌کنیم.
     */
    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, y: Float, paint: Paint) {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = centerX - bounds.width() / 2f - bounds.left
        canvas.drawText(text, x, y, paint)
    }

    private fun drawHeader(canvas: Canvas, isPersian: Boolean, startY: Float): Float {
        var y = startY
        val centerX = PAGE_WIDTH / 2f

        drawCenteredText(
            canvas,
            if (isPersian) "سیدنا: مدرن ترین برنامه مدیریت مالی" else "Cidna: The modernest financial manager",
            centerX, y, textPaint(18f, bold = true)
        )
        return y + 24f
    }

    private fun drawStatsBox(canvas: Canvas, stats: ExportStats, isPersian: Boolean, startY: Float): Float {
        var y = startY
        val labelPaint = textPaint(10f, color = Color.DKGRAY)
        val valuePaint = textPaint(11f, bold = true)

        val rows = listOf(
            (if (isPersian) "مانده از قبل" else "Opening Balance") to formatAmount(stats.openingBalance, stats.currency, isPersian),
            (if (isPersian) "جمع کل واریز" else "Total Income") to formatAmount(stats.totalIncome, stats.currency, isPersian),
            (if (isPersian) "جمع کل برداشت" else "Total Expense") to formatAmount(stats.totalExpense, stats.currency, isPersian),
            (if (isPersian) "مانده" else "Balance") to formatAmount(stats.balance, stats.currency, isPersian),
            (if (isPersian) "معدل موجودی" else "Average Balance") to formatAmount(stats.averageBalance, stats.currency, isPersian),
            (if (isPersian) "نوع ارز" else "Currency") to currencyLabel(stats.currency, isPersian),
            (if (isPersian) "تاریخ صدور فایل" else "Issue Date") to DateUtils.formatTimestamp(stats.issueDateMillis, isPersian)
        )

        val colWidth = (PAGE_WIDTH - MARGIN * 2) / 2f
        rows.chunked(2).forEach { pair ->
            pair.forEachIndexed { i, (label, value) ->
                val x = MARGIN + i * colWidth
                canvas.drawText(label, x, y, labelPaint)
                canvas.drawText(value, x, y + 14f, valuePaint)
            }
            y += 34f
        }
        return y + 12f
    }

    private fun drawStatementTitle(canvas: Canvas, stats: ExportStats, isPersian: Boolean, startY: Float): Float {
        val from = DateUtils.formatTimestamp(stats.startMillis, isPersian)
        val to = DateUtils.formatTimestamp(stats.endMillis, isPersian)
        val text = if (isPersian) "صورتحساب از $from تا $to" else "Statement from $from to $to"
        drawCenteredText(canvas, text, PAGE_WIDTH / 2f, startY, textPaint(12f, bold = true))
        return startY + 20f
    }

    // وزن نسبی هر ستون از عرض کل جدول (جمع وزن‌ها = ۱۰۰). ترتیب: ردیف، تاریخ، عنوان، دسته‌بندی، نوع، مبلغ، یادداشت
    private val COLUMN_WEIGHTS = listOf(6, 13, 18, 15, 11, 17, 20)

    private fun columnWidths(): List<Float> {
        val totalWidth = PAGE_WIDTH - MARGIN * 2
        return COLUMN_WEIGHTS.map { totalWidth * it / 100f }
    }

    private fun drawTableHeader(canvas: Canvas, isPersian: Boolean, startY: Float): Float {
        val headerPaint = textPaint(9.5f, bold = true, color = Color.WHITE)
        val bgPaint = Paint().apply { color = Color.parseColor("#2E7D32") } // سبز هماهنگ با آیکون برنامه
        canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + TABLE_HEADER_HEIGHT, bgPaint)

        val cols = if (isPersian)
            listOf("ردیف", "تاریخ", "عنوان", "دسته‌بندی", "نوع", "مبلغ", "یادداشت")
        else
            listOf("#", "Date", "Title", "Category", "Type", "Amount", "Note")

        val widths = columnWidths()
        var x = MARGIN
        cols.forEachIndexed { i, col ->
            val cellCenterX = x + widths[i] / 2f
            drawCenteredText(canvas, col, cellCenterX, startY + 20f, headerPaint)
            x += widths[i]
        }
        return startY + TABLE_HEADER_HEIGHT + 4f
    }

    private fun drawTransactionRow(canvas: Canvas, rowNumber: Int, tx: TransactionEntity, currency: String, isPersian: Boolean, y: Float) {
        val cellPaint = textPaint(9f)

        // برای هر ستون یک محدودیت طول متفاوت می‌ذاریم چون عرض ستون‌ها یکسان نیست
        val cells = listOf(
            rowNumber.toString(),
            DateUtils.formatTimestamp(tx.timestamp, isPersian),
            tx.title.take(16),
            tx.category.take(13),
            if (tx.type == "INCOME") (if (isPersian) "درآمد" else "Income") else (if (isPersian) "هزینه" else "Expense"),
            formatAmount(tx.amount, currency, isPersian),
            tx.note.take(18)
        )

        val widths = columnWidths()
        var x = MARGIN
        cells.forEachIndexed { i, cell ->
            val cellCenterX = x + widths[i] / 2f
            drawCenteredText(canvas, cell, cellCenterX, y + 18f, cellPaint)
            x += widths[i]
        }
    }

    /** رسم خطوط افقی و عمودی جدول برای خوانایی بهتر */
    private fun drawTableBorders(canvas: Canvas, tableStartY: Float, rowCount: Int) {
        val borderPaint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val outerPaint = Paint().apply {
            color = Color.parseColor("#757575")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val tableTop = tableStartY - 4f - TABLE_HEADER_HEIGHT
        val tableBottom = tableStartY + rowCount * ROW_HEIGHT
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN

        // کادر بیرونی جدول
        canvas.drawRect(left, tableTop, right, tableBottom, outerPaint)

        // خطوط افقی بین ردیف‌ها
        for (i in 0..rowCount) {
            val y = tableStartY + i * ROW_HEIGHT
            canvas.drawLine(left, y, right, y, borderPaint)
        }
        // خط بالای هدر (قبلاً با پس‌زمینه سبز پوشیده شده)
        canvas.drawLine(left, tableTop, right, tableTop, outerPaint)

        // خطوط عمودی ستون‌ها
        val widths = columnWidths()
        var x = left
        for (i in 0 until widths.size - 1) {
            x += widths[i]
            canvas.drawLine(x, tableTop, x, tableBottom, borderPaint)
        }
    }

    private fun drawFooter(canvas: Canvas, page: Int, total: Int, isPersian: Boolean) {
        val text = if (isPersian) "صفحه $page از $total" else "Page $page of $total"
        drawCenteredText(canvas, text, PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN / 2, textPaint(9f))
    }

    /**
     * مقادیر همیشه در دیتابیس به تومان ذخیره می‌شن.
     * اگه واحد پولی انتخابی کاربر ریال باشه، مقدار نمایشی باید ضربدر ۱۰ بشه (۱ تومان = ۱۰ ریال).
     */
    private fun toDisplayAmount(amount: Double, currencyCode: String): Double {
        return if (currencyCode.equals("IRR", ignoreCase = true)) amount * 10 else amount
    }

    private fun currencyLabel(currencyCode: String, isPersian: Boolean): String {
        return if (currencyCode.equals("IRR", ignoreCase = true)) {
            if (isPersian) "ریال" else "Rial"
        } else {
            if (isPersian) "تومان" else "Toman"
        }
    }

    private fun formatAmount(amount: Double, currencyCode: String, isPersian: Boolean): String {
        val displayValue = toDisplayAmount(amount, currencyCode)
        return "%,.0f %s".format(displayValue, currencyLabel(currencyCode, isPersian))
    }
}