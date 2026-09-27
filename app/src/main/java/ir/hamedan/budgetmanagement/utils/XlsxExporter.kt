package ir.hamedan.budgetmanagement.utils

import android.content.Context
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object XlsxExporter {

    private const val BRAND = "408A71"
    private const val BRAND_LIGHT = "EAF4F0"
    private const val ALT_ROW = "F5F8F7"
    private const val TEXT = "26332F"
    private const val INCOME = "2E8B67"
    private const val EXPENSE = "C65B5B"

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
        var headerRow = -1
        var lastRow = -1

        FileOutputStream(outFile).use { fos ->
            val workbook = Workbook(fos, "Cidna", "1.0")
            val sheetName = if (isPersian) "صورتحساب" else "Statement"
            val ws = workbook.newWorksheet(sheetName)
            var row = 0

            // Brand header / report title.
            ws.value(row, 0, if (isPersian) "گزارش مالی سیدنا" else "Cidna Financial Statement")
            (0..6).forEach { col -> ws.style(row, col).fillColor(BRAND).set() }
            ws.style(row, 0).bold().fontColor("FFFFFF").set()
            row++
            ws.value(row, 0, if (isPersian) "صورتحساب و گزارش تراکنش‌ها" else "Statement & transaction report")
            (0..6).forEach { col -> ws.style(row, col).fillColor(BRAND_LIGHT).set() }
            ws.style(row, 0).fontColor(BRAND).set()
            row += 2

            val summary = listOf(
                (if (isPersian) "مانده از قبل" else "Opening Balance") to toDisplayAmount(stats.openingBalance, stats.currency),
                (if (isPersian) "جمع کل واریز" else "Total Income") to toDisplayAmount(stats.totalIncome, stats.currency),
                (if (isPersian) "جمع کل برداشت" else "Total Expense") to toDisplayAmount(stats.totalExpense, stats.currency),
                (if (isPersian) "مانده نهایی" else "Closing Balance") to toDisplayAmount(stats.balance, stats.currency),
                (if (isPersian) "معدل موجودی" else "Average Balance") to toDisplayAmount(stats.averageBalance, stats.currency),
                (if (isPersian) "نوع ارز" else "Currency") to currencyLabel(stats.currency, isPersian),
                (if (isPersian) "تاریخ صدور" else "Issue Date") to DateUtils.formatTimestamp(stats.issueDateMillis, isPersian)
            )

            summary.forEach { (label, value) ->
                ws.value(row, 0, label)
                if (value is Double) ws.value(row, 1, value) else ws.value(row, 1, value.toString())
                ws.style(row, 0).bold().fontColor(TEXT).fillColor(BRAND_LIGHT).set()
                ws.style(row, 1).fontColor(TEXT).fillColor("FFFFFF").set()
                row++
            }
            row += 1

            val from = DateUtils.formatTimestamp(stats.startMillis, isPersian)
            val to = DateUtils.formatTimestamp(stats.endMillis, isPersian)
            ws.value(row, 0, if (isPersian) "صورتحساب از تاریخ $from تا $to" else "Statement from $from to $to")
            ws.style(row, 0).bold().fontColor(BRAND).set()
            row += 2

            // Logical order is intentionally RTL-friendly: when the sheet is RTL, column A is the rightmost column.
            val headers = if (isPersian)
                listOf("ردیف", "تاریخ", "عنوان", "دسته‌بندی", "نوع", "مبلغ", "یادداشت")
            else
                listOf("#", "Date", "Title", "Category", "Type", "Amount", "Note")

            headers.forEachIndexed { col, h ->
                ws.value(row, col, h)
                ws.style(row, col).bold().fontColor("FFFFFF").fillColor(BRAND).set()
            }
            headerRow = row
            row++


            transactions.forEachIndexed { index, tx ->
                val category = categoryNames[tx.categoryId]
                    ?: tx.categoryId.ifBlank { if (isPersian) "دسته‌بندی نشده" else "Uncategorized" }
                val amount = toDisplayAmount(tx.amount.toDouble(), stats.currency)
                val typeText = if (tx.type == "INCOME") {
                    if (isPersian) "درآمد" else "Income"
                } else {
                    if (isPersian) "هزینه" else "Expense"
                }

                ws.value(row, 0, index + 1)
                ws.value(row, 1, DateUtils.formatTimestamp(tx.timestamp, isPersian))
                ws.value(row, 2, tx.title.ifBlank { if (isPersian) "بدون عنوان" else "Untitled" })
                ws.value(row, 3, category)
                ws.value(row, 4, typeText)
                ws.value(row, 5, amount)
                ws.value(row, 6, tx.note.ifBlank { "—" })

                val fill = if (index % 2 == 0) "FFFFFF" else ALT_ROW
                (0..6).forEach { col -> ws.style(row, col).fillColor(fill).fontColor(TEXT).set() }
                ws.style(row, 5)
                    .bold()
                    .fontColor(if (tx.type == "INCOME") INCOME else EXPENSE)
                    .set()
                row++
            }
            lastRow = row - 1

            // Deliberately generous widths mirror the clean bank-statement style and keep long notes readable.
            ws.width(0, 9.0)
            ws.width(1, 17.0)
            ws.width(2, 28.0)
            ws.width(3, 24.0)
            ws.width(4, 14.0)
            ws.width(5, 22.0)
            ws.width(6, 68.0)

            workbook.finish()
        }

        // Final OOXML pass: real logo, RTL sheet direction, merged title bands, freeze/filter/print setup.
        polishWorkbook(context, outFile, isPersian, headerRow = headerRow, lastRow = lastRow)
        return outFile
    }

    private fun polishWorkbook(context: Context, xlsx: File, isPersian: Boolean, headerRow: Int, lastRow: Int) {
        val logoBytes = context.resources.openRawResource(R.raw.export_logo_mark).use { it.readBytes() }
        val temp = File(xlsx.parentFile, xlsx.nameWithoutExtension + "_polished.xlsx")

        ZipFile(xlsx).use { zip ->
            ZipOutputStream(FileOutputStream(temp)).use { out ->
                zip.entries().asSequence().forEach { entry ->
                    val replace = entry.name == "xl/worksheets/sheet1.xml" ||
                            entry.name == "xl/worksheets/_rels/sheet1.xml.rels" ||
                            entry.name == "[Content_Types].xml"
                    if (replace) return@forEach
                    out.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                    zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }

                writeEntry(out, "xl/media/export_logo.png", logoBytes)
                writeEntry(out, "xl/drawings/drawing1.xml", drawingXml())
                writeEntry(out, "xl/drawings/_rels/drawing1.xml.rels", drawingRelsXml())

                val sheetXml = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).bufferedReader().readText()
                writeEntry(out, "xl/worksheets/sheet1.xml", polishSheetXml(sheetXml, isPersian, headerRow, lastRow))

                val relEntry = zip.getEntry("xl/worksheets/_rels/sheet1.xml.rels")
                val rels = if (relEntry != null) zip.getInputStream(relEntry).bufferedReader().readText() else {
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"></Relationships>"""
                }
                writeEntry(out, "xl/worksheets/_rels/sheet1.xml.rels", addSheetDrawingRelationship(rels))

                val ct = zip.getInputStream(zip.getEntry("[Content_Types].xml")).bufferedReader().readText()
                writeEntry(out, "[Content_Types].xml", addContentTypes(ct))
            }
        }

        if (!xlsx.delete() || !temp.renameTo(xlsx)) {
            temp.delete()
            throw IllegalStateException("Unable to finalize XLSX export")
        }
    }

    private fun polishSheetXml(xml: String, isPersian: Boolean, headerRow: Int, lastRow: Int): String {
        var result = xml
        if (!result.contains("xmlns:r=")) {
            result = result.replace("<worksheet", "<worksheet xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"")
        }
        val view = """<sheetView workbookViewId="0"${if (isPersian) " rightToLeft=\"1\"" else ""}><pane ySplit="${headerRow + 1}" topLeftCell="A${headerRow + 2}" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft" activeCell="A${headerRow + 2}" sqref="A${headerRow + 2}"/></sheetView>"""
        result = if (result.contains("<sheetViews>")) {
            val withViews = result.replaceFirst(Regex("<sheetViews>.*?</sheetViews>"), "<sheetViews>$view</sheetViews>")
            if (withViews.contains("<sheetPr>")) withViews else withViews.replace("<sheetViews>", "<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr><sheetViews>")
        } else {
            result.replace("<sheetData>", "<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr><sheetViews>$view</sheetViews><sheetData>")
        }
        result = result.replace("</sheetData>", "</sheetData><autoFilter ref=\"A${headerRow + 1}:G${lastRow + 1}\"/>")
        if (!result.contains("<mergeCells")) {
            result = result.replace("</sheetData>", "</sheetData><mergeCells count=2><mergeCell ref=\"A1:G1\"/><mergeCell ref=\"A2:G2\"/></mergeCells>")
        }
        if (!result.contains("r:id=\"rIdLogo\"")) {
            result = result.replace("</sheetData>", "</sheetData><drawing r:id=\"rIdLogo\"/>")
        }
        result = result.replace("<drawing r:id=\"rIdLogo\"/>", "<pageMargins left=\"0.3\" right=\"0.3\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/><pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/><drawing r:id=\"rIdLogo\"/>")
        return result
    }

    private fun addSheetDrawingRelationship(xml: String): String =
        xml.replace("</Relationships>", "<Relationship Id=\"rIdLogo\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/></Relationships>")

    private fun addContentTypes(xml: String): String {
        var result = xml
        if (!result.contains("Extension=\"png\"")) {
            result = result.replace("</Types>", "<Default Extension=\"png\" ContentType=\"image/png\"/></Types>")
        }
        if (!result.contains("/xl/drawings/drawing1.xml")) {
            result = result.replace("</Types>", "<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/></Types>")
        }
        return result
    }

    private fun addDrawingReference(xml: String): String =
        if (xml.contains("<drawing r:id=\"rIdLogo\"")) xml else xml.replace("</worksheet>", "<drawing r:id=\"rIdLogo\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/></worksheet>")

    private fun writeEntry(out: ZipOutputStream, name: String, bytes: ByteArray) {
        out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry()
    }

    private fun writeEntry(out: ZipOutputStream, name: String, text: String) = writeEntry(out, name, text.toByteArray(Charsets.UTF_8))

    private fun drawingXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <xdr:twoCellAnchor editAs="oneCell">
            <xdr:from><xdr:col>3</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>
            <xdr:to><xdr:col>5</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>3</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>
            <xdr:pic>
              <xdr:nvPicPr><xdr:cNvPr id="2" name="Cidna Logo"/><xdr:cNvPicPr/></xdr:nvPicPr>
              <xdr:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>
              <xdr:spPr><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></xdr:spPr>
            </xdr:pic>
            <xdr:clientData/>
          </xdr:twoCellAnchor>
        </xdr:wsDr>
    """.trimIndent()

    private fun drawingRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/export_logo.png"/>
        </Relationships>
    """.trimIndent()
}