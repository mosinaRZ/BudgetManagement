package ir.hamedan.budgetmanagement.data.export

import android.content.Context
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.utils.DateUtils
import org.dhatim.fastexcel.BorderSide
import org.dhatim.fastexcel.BorderStyle
import org.dhatim.fastexcel.Position
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object XlsxExporter {

    // ───────────────────────── Palette (same as the PDF) ─────────────────────────
    private const val BRAND = "408A71"
    private const val BRAND_DARK = "2F6B57"
    private const val BRAND_LIGHT = "EAF4F0"
    private const val ALT_ROW = "F6F9F8"
    private const val TEXT = "1F2A27"
    private const val MUTED = "6B7773"
    private const val BORDER = "D9E2DE"
    private const val INCOME = "1F8A5F"
    private const val INCOME_BG = "E3F4EC"
    private const val EXPENSE = "C0392B"
    private const val EXPENSE_BG = "FBE8E6"
    private const val WHITE = "FFFFFF"

    private const val LAST_COL = 6

    /** Positive / negative (red) balances. */
    private const val BALANCE_FORMAT = "#,##0;[Red]-#,##0"

    /** Transactions: real sign, so SUM() of the column equals the net change. */
    private const val SIGNED_FORMAT = "+#,##0;-#,##0;0"

    // Column widths (Excel character units): #, date, title, category, type, amount, note
    private val COLUMN_WIDTHS = doubleArrayOf(7.0, 16.0, 30.0, 22.0, 12.0, 20.0, 60.0)

    private const val LINE_HEIGHT = 15.0
    private const val MAX_ROW_HEIGHT = 409.0          // Excel's hard limit
    private const val MAX_CELL_CHARS = 32000          // Excel's limit is 32,767 per cell

    private fun toDisplayAmount(amount: Double, currencyCode: String): Double =
        if (currencyCode.equals("IRR", ignoreCase = true)) amount * 10 else amount

    private fun currencyLabel(currencyCode: String, isPersian: Boolean): String =
        if (currencyCode.equals("IRR", ignoreCase = true)) {
            if (isPersian) "ریال" else "Rial"
        } else {
            if (isPersian) "تومان" else "Toman"
        }

    fun generate(
        context: Context,
        transactions: List<TransactionEntity>,
        stats: ExportStats,
        isPersian: Boolean,
        categoryNames: Map<String, String> = emptyMap()
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportsDir, "Cidna_Statement_${System.currentTimeMillis()}.xlsx")

        FileOutputStream(outFile).use { fos ->
            val workbook = Workbook(fos, "Cidna", "1.0")
            val ws = workbook.newWorksheet(if (isPersian) "صورتحساب" else "Statement")
            buildSheet(ws, transactions, stats, isPersian, categoryNames)
            workbook.finish()
        }

        // Package-level pass: sheet direction (RTL), A4 paper and the real logo image.
        polishWorkbook(context, outFile, isPersian)
        return outFile
    }

    // ───────────────────────── Sheet content ─────────────────────────

    private fun buildSheet(
        ws: Worksheet,
        transactions: List<TransactionEntity>,
        stats: ExportStats,
        isPersian: Boolean,
        categoryNames: Map<String, String>
    ) {
        fun t(fa: String, en: String) = if (isPersian) fa else en
        val currency = stats.currency
        val unit = currencyLabel(currency, isPersian)

        COLUMN_WIDTHS.forEachIndexed { i, w -> ws.width(i, w) }
        ws.hideGridLines()

        var row = 0

        // ── Title band (the logo image is placed over its start side) ──
        ws.value(row, 0, t("گزارش مالی سیدنا", "Cidna Financial Statement"))
        ws.range(row, 0, row, LAST_COL).style()
            .fillColor(WHITE).fontColor(BRAND).fontSize(20).bold()
            .horizontalAlignment("center").verticalAlignment("center")
            .merge().set()
        ws.rowHeight(row, 42.0)
        row++

        ws.value(row, 0, t("صورتحساب و گزارش تراکنش‌ها", "Statement & transaction report"))
        ws.range(row, 0, row, LAST_COL).style()
            .fillColor(WHITE).fontColor(MUTED).fontSize(10)
            .horizontalAlignment("center").verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.MEDIUM).borderColor(BorderSide.BOTTOM, BRAND)
            .merge().set()
        ws.rowHeight(row, 22.0)
        row++

        spacer(ws, row, 10.0); row++

        // ── Summary ──
        caption(ws, row, t("خلاصه حساب", "Account summary")); row++

        val left = listOf(
            SummaryItem(t("مانده از قبل", "Opening Balance"), toDisplayAmount(stats.openingBalance, currency), TEXT, false),
            SummaryItem(t("جمع کل واریز", "Total Income"), toDisplayAmount(stats.totalIncome, currency), INCOME, false),
            SummaryItem(t("جمع کل برداشت", "Total Expense"), toDisplayAmount(stats.totalExpense, currency), EXPENSE, false),
            SummaryItem(t("مانده نهایی", "Closing Balance"), toDisplayAmount(stats.balance, currency), BRAND_DARK, true),
            SummaryItem(t("معدل موجودی", "Average Balance"), toDisplayAmount(stats.averageBalance, currency), TEXT, false)
        )
        val from = DateUtils.formatTimestamp(stats.startMillis, isPersian)
        val to = DateUtils.formatTimestamp(stats.endMillis, isPersian)
        val right = listOf(
            t("بازه گزارش", "Statement period") to t("از $from تا $to", "$from  –  $to"),
            t("نوع ارز", "Currency") to unit,
            t("تاریخ صدور", "Issue date") to DateUtils.formatTimestamp(stats.issueDateMillis, isPersian),
            t("تعداد تراکنش", "Transactions") to transactions.size.toString()
        )

        left.forEachIndexed { i, item ->
            val r = row + i
            // start group: label (A:B) + numeric value (C)
            ws.value(r, 0, item.label)
            ws.range(r, 0, r, 1).style()
                .fillColor(BRAND_LIGHT).fontColor(TEXT).fontSize(10).bold()
                .horizontalAlignment("center").verticalAlignment("center")
                .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).borderColor(BorderSide.BOTTOM, BORDER)
                .merge().set()

            ws.value(r, 2, item.value)
            var valueStyle = ws.style(r, 2)
                .fillColor(if (item.highlight) BRAND_LIGHT else WHITE)
                .fontColor(item.color).fontSize(if (item.highlight) 12 else 11)
                .format(BALANCE_FORMAT)
                .horizontalAlignment("center").verticalAlignment("center")
                .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).borderColor(BorderSide.BOTTOM, BORDER)
            if (item.highlight || item.color != TEXT) valueStyle = valueStyle.bold()
            valueStyle.set()

            // end group: label (D:E) + text value (F:G)
            if (i < right.size) {
                val (label, value) = right[i]
                ws.value(r, 3, label)
                ws.range(r, 3, r, 4).style()
                    .fillColor(BRAND_LIGHT).fontColor(TEXT).fontSize(10).bold()
                    .horizontalAlignment("center").verticalAlignment("center")
                    .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).borderColor(BorderSide.BOTTOM, BORDER)
                    .merge().set()
                ws.value(r, 5, value)
                ws.range(r, 5, r, 6).style()
                    .fillColor(WHITE).fontColor(TEXT).fontSize(10)
                    .horizontalAlignment("center").verticalAlignment("center")
                    .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).borderColor(BorderSide.BOTTOM, BORDER)
                    .merge().set()
            }
            ws.rowHeight(r, 24.0)
        }
        row += left.size

        spacer(ws, row, 12.0); row++

        // ── Transactions table ──
        caption(ws, row, t("تراکنش‌ها", "Transactions")); row++

        val headers = if (isPersian)
            listOf("ردیف", "تاریخ", "عنوان", "دسته‌بندی", "نوع", "مبلغ ($unit)", "یادداشت")
        else
            listOf("#", "Date", "Title", "Category", "Type", "Amount ($unit)", "Note")

        val headerRow = row
        headers.forEachIndexed { col, h ->
            ws.value(row, col, h)
            ws.style(row, col)
                .fillColor(BRAND_DARK).fontColor(WHITE).fontSize(10).bold()
                .horizontalAlignment("center").verticalAlignment("center").wrapText(true)
                .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).borderColor(BorderSide.BOTTOM, BRAND_DARK)
                .set()
        }
        ws.rowHeight(row, 28.0)
        row++

        val firstDataRow = row
        transactions.forEachIndexed { index, tx ->
            val isIncome = tx.type == "INCOME"
            val category = categoryNames[tx.categoryId]
                ?: tx.categoryId.takeUnless { it.isBlank() || it.equals("UNCATEGORIZED", true) }
                ?: t("دسته‌بندی نشده", "Uncategorized")
            val title = tx.title.ifBlank { t("بدون عنوان", "Untitled") }
            val note = tx.note.ifBlank { "—" }.take(MAX_CELL_CHARS)
            val signedAmount = toDisplayAmount(tx.amount.toDouble(), currency).let { if (isIncome) it else -it }
            val fill = if (index % 2 == 0) WHITE else ALT_ROW

            ws.value(row, 0, index + 1)
            ws.value(row, 1, DateUtils.formatTimestamp(tx.timestamp, isPersian))
            ws.value(row, 2, title)
            ws.value(row, 3, category)
            ws.value(row, 4, if (isIncome) t("درآمد", "Income") else t("هزینه", "Expense"))
            ws.value(row, 5, signedAmount)
            ws.value(row, 6, note)

            dataCell(ws, row, 0, fill, MUTED, h = "center")
            dataCell(ws, row, 1, fill, TEXT, h = "center")
            dataCell(ws, row, 2, fill, TEXT, bold = true, wrap = true)
            dataCell(ws, row, 3, fill, TEXT, wrap = true)
            dataCell(ws, row, 4, if (isIncome) INCOME_BG else EXPENSE_BG, if (isIncome) INCOME else EXPENSE, bold = true, h = "center")
            dataCell(ws, row, 5, fill, if (isIncome) INCOME else EXPENSE, bold = true, h = "center", format = SIGNED_FORMAT)
            dataCell(ws, row, 6, fill, MUTED, wrap = true)

            // Explicit height sized to the longest wrapped cell, so every viewer (Excel, LibreOffice,
            // Google Sheets, phone previews) shows the whole text without relying on auto-fit.
            val lines = maxOf(
                estimateLines(title, COLUMN_WIDTHS[2] * 0.85),
                estimateLines(category, COLUMN_WIDTHS[3] * 0.92),
                estimateLines(note, COLUMN_WIDTHS[6] * 0.92)
            )
            ws.rowHeight(row, min(MAX_ROW_HEIGHT, max(24.0, lines * LINE_HEIGHT + 8.0)))
            row++
        }
        val lastRow = row - 1

        if (transactions.isEmpty()) {
            ws.value(row, 0, t("تراکنشی برای این بازه ثبت نشده است", "No transactions were recorded in this period"))
            ws.range(row, 0, row, LAST_COL).style()
                .fillColor(BRAND_LIGHT).fontColor(BRAND_DARK).fontSize(11).bold()
                .horizontalAlignment("center").verticalAlignment("center")
                .merge().set()
            ws.rowHeight(row, 36.0)
            row++
        } else {
            // Filter buttons on the header row.
            ws.setAutoFilter(headerRow, 0, max(lastRow, firstDataRow), LAST_COL)
        }

        spacer(ws, row, 12.0); row++
        ws.value(row, 0, t("تهیه‌شده با سیدنا", "Generated by Cidna"))
        ws.range(row, 0, row, LAST_COL).style()
            .fillColor(WHITE).fontColor(MUTED).fontSize(9).italic()
            .horizontalAlignment("center").verticalAlignment("center")
            .merge().set()

        // ── Print setup: landscape, one page wide, header row repeated on every page ──
        ws.pageOrientation("landscape")
        ws.setFitToPage(true)
        ws.fitToWidth(1.toShort())
        ws.fitToHeight(0.toShort())
        ws.repeatRows(headerRow)
        ws.footer(t("صفحه &P از &N", "Page &P of &N"), Position.CENTER, "Calibri", 9)
    }

    private class SummaryItem(val label: String, val value: Double, val color: String, val highlight: Boolean)

    private fun spacer(ws: Worksheet, row: Int, height: Double) {
        ws.style(row, 0).fillColor(WHITE).set()
        ws.rowHeight(row, height)
    }

    private fun caption(ws: Worksheet, row: Int, text: String) {
        ws.value(row, 0, text)
        ws.range(row, 0, row, LAST_COL).style()
            .fillColor(BRAND).fontColor(WHITE).fontSize(11).bold()
            .horizontalAlignment("center").verticalAlignment("center")
            .merge().set()
        ws.rowHeight(row, 24.0)
    }

    private fun dataCell(
        ws: Worksheet,
        row: Int,
        col: Int,
        fill: String,
        color: String,
        bold: Boolean = false,
        h: String? = null,
        wrap: Boolean = false,
        format: String? = null
    ) {
        var s = ws.style(row, col)
            .fillColor(fill).fontColor(color).fontSize(10)
            .verticalAlignment("center")
            .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN).borderColor(BorderSide.BOTTOM, BORDER)
        if (bold) s = s.bold()
        if (h != null) s = s.horizontalAlignment(h)
        if (wrap) s = s.wrapText(true)
        if (format != null) s = s.format(format)
        s.set()
    }

    /** Conservative estimate of how many lines [text] needs in a column that is [widthChars] wide. */
    private fun estimateLines(text: String, widthChars: Double): Int {
        val perLine = max(1.0, widthChars)
        return text.split('\n').sumOf { paragraph ->
            max(1, ceil(paragraph.length / perLine).toInt())
        }
    }

    // ───────────────────────── Package-level polish ─────────────────────────

    private fun polishWorkbook(context: Context, xlsx: File, isPersian: Boolean) {
        val logoBytes = context.resources.openRawResource(R.raw.export_logo_mark).use { it.readBytes() }
        val temp = File(xlsx.parentFile, xlsx.nameWithoutExtension + "_polished.xlsx")

        try {
            ZipFile(xlsx).use { zip ->
                ZipOutputStream(FileOutputStream(temp)).use { out ->
                    val replaced = setOf(
                        "xl/worksheets/sheet1.xml",
                        "xl/worksheets/_rels/sheet1.xml.rels",
                        "[Content_Types].xml"
                    )
                    zip.entries().asSequence().forEach { entry ->
                        if (entry.name in replaced) return@forEach
                        out.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                        zip.getInputStream(entry).use { it.copyTo(out) }
                        out.closeEntry()
                    }

                    writeEntry(out, "xl/media/export_logo.png", logoBytes)
                    writeEntry(out, "xl/drawings/drawing1.xml", drawingXml(logoBytes))
                    writeEntry(out, "xl/drawings/_rels/drawing1.xml.rels", drawingRelsXml())

                    val sheetXml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"))
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                    writeEntry(out, "xl/worksheets/sheet1.xml", polishSheetXml(sheetXml, isPersian))

                    val relEntry = zip.getEntry("xl/worksheets/_rels/sheet1.xml.rels")
                    val rels = if (relEntry != null) {
                        zip.getInputStream(relEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"></Relationships>"""
                    }
                    writeEntry(out, "xl/worksheets/_rels/sheet1.xml.rels", addSheetDrawingRelationship(rels))

                    val ct = zip.getInputStream(zip.getEntry("[Content_Types].xml"))
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                    writeEntry(out, "[Content_Types].xml", addContentTypes(ct))
                }
            }

            if (!xlsx.delete() || !temp.renameTo(xlsx)) {
                throw IllegalStateException("Unable to finalize XLSX export")
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    /**
     * Only three surgical edits, all schema-safe (fastexcel already writes margins, page setup,
     * header/footer, merges and the auto-filter in the correct order):
     *  1. right-to-left sheet direction for Persian,
     *  2. A4 paper instead of Letter,
     *  3. the <drawing> element that shows the logo (must sit after <headerFooter>).
     */
    private fun polishSheetXml(xml: String, isPersian: Boolean): String {
        var result = xml

        if (isPersian && !result.contains("rightToLeft=")) {
            result = result.replaceFirst("<sheetView ", "<sheetView rightToLeft=\"1\" ")
        }

        result = result.replace(Regex("paperSize=\"\\d+\""), "paperSize=\"9\"")

        if (!result.contains("<drawing ")) {
            val tag = "<drawing r:id=\"rIdLogo\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/>"
            val at = listOf("<legacyDrawing", "<tableParts", "</worksheet>")
                .map { result.indexOf(it) }
                .filter { it >= 0 }
                .minOrNull() ?: result.length
            result = result.substring(0, at) + tag + result.substring(at)
        }
        return result
    }

    private fun addSheetDrawingRelationship(xml: String): String =
        xml.replace(
            "</Relationships>",
            "<Relationship Id=\"rIdLogo\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/></Relationships>"
        )

    private fun addContentTypes(xml: String): String {
        var result = xml
        if (!result.contains("Extension=\"png\"")) {
            result = result.replace("</Types>", "<Default Extension=\"png\" ContentType=\"image/png\"/></Types>")
        }
        if (!result.contains("/xl/drawings/drawing1.xml")) {
            result = result.replace(
                "</Types>",
                "<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/></Types>"
            )
        }
        return result
    }

    private fun writeEntry(out: ZipOutputStream, name: String, bytes: ByteArray) {
        out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry()
    }

    private fun writeEntry(out: ZipOutputStream, name: String, text: String) =
        writeEntry(out, name, text.toByteArray(Charsets.UTF_8))

    /** Reads width/height from a PNG header (null if the bytes are not a PNG). */
    private fun pngSize(b: ByteArray): Pair<Int, Int>? {
        if (b.size < 24 || b[1] != 0x50.toByte() || b[2] != 0x4E.toByte() || b[3] != 0x47.toByte()) return null
        fun be(o: Int) = ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or
                ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
        val w = be(16)
        val h = be(20)
        return if (w > 0 && h > 0) w to h else null
    }

    /** Logo anchored at the start corner (A1) with its real aspect ratio, max 44px on the long side. */
    private fun drawingXml(logoBytes: ByteArray): String {
        val (w, h) = pngSize(logoBytes) ?: (1 to 1)
        val scale = 44.0 / max(w, h)
        val emuPerPx = 9525L
        val cx = (w * scale * emuPerPx).toLong()
        val cy = (h * scale * emuPerPx).toLong()
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<xdr:oneCellAnchor>
<xdr:from><xdr:col>0</xdr:col><xdr:colOff>76200</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>57150</xdr:rowOff></xdr:from>
<xdr:ext cx="$cx" cy="$cy"/>
<xdr:pic>
<xdr:nvPicPr><xdr:cNvPr id="2" name="Cidna Logo"/><xdr:cNvPicPr><a:picLocks noChangeAspect="1"/></xdr:cNvPicPr></xdr:nvPicPr>
<xdr:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>
<xdr:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></xdr:spPr>
</xdr:pic>
<xdr:clientData/>
</xdr:oneCellAnchor>
</xdr:wsDr>"""
    }

    private fun drawingRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/export_logo.png"/>
</Relationships>"""
}