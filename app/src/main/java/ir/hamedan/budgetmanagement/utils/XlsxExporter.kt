package ir.hamedan.budgetmanagement.utils

import android.content.Context
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream

object XlsxExporter {

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

    fun generate(
        context: Context,
        transactions: List<TransactionEntity>,
        stats: ExportStats,
        isPersian: Boolean
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "Cidna_Statement_${System.currentTimeMillis()}.xlsx"
        val outFile = File(exportsDir, fileName)

        FileOutputStream(outFile).use { fos ->
            val workbook = Workbook(fos, "Cidna", "1.0")
            val sheetName = if (isPersian) "صورتحساب" else "Statement"
            val ws = workbook.newWorksheet(sheetName)

            var row = 0

            ws.value(row, 0, "Cidna")
            ws.style(row, 0).bold().set()
            row++

            val statLabels = listOf(
                (if (isPersian) "مانده از قبل" else "Opening Balance") to toDisplayAmount(stats.openingBalance, stats.currency),
                (if (isPersian) "جمع کل واریز" else "Total Income") to toDisplayAmount(stats.totalIncome, stats.currency),
                (if (isPersian) "جمع کل برداشت" else "Total Expense") to toDisplayAmount(stats.totalExpense, stats.currency),
                (if (isPersian) "مانده" else "Balance") to toDisplayAmount(stats.balance, stats.currency),
                (if (isPersian) "معدل موجودی" else "Average Balance") to toDisplayAmount(stats.averageBalance, stats.currency)
            )
            statLabels.forEach { (label, value) ->
                ws.value(row, 0, label)
                ws.value(row, 1, value)
                row++
            }
            ws.value(row, 0, if (isPersian) "نوع ارز" else "Currency"); ws.value(row, 1, currencyLabel(stats.currency, isPersian)); row++
            ws.value(row, 0, if (isPersian) "تاریخ صدور" else "Issue Date")
            ws.value(row, 1, DateUtils.formatTimestamp(stats.issueDateMillis, isPersian)); row += 2

            val from = DateUtils.formatTimestamp(stats.startMillis, isPersian)
            val to = DateUtils.formatTimestamp(stats.endMillis, isPersian)
            ws.value(row, 0, if (isPersian) "صورتحساب از $from تا $to" else "Statement from $from to $to")
            ws.style(row, 0).bold().set()
            row += 2

            val headers = if (isPersian)
                listOf("تاریخ", "عنوان", "دسته‌بندی", "نوع", "مبلغ", "یادداشت")
            else
                listOf("Date", "Title", "Category", "Type", "Amount", "Note")

            headers.forEachIndexed { col, h ->
                ws.value(row, col, h)
                ws.style(row, col).bold().fillColor("2E7D32").fontColor("FFFFFF").set()
            }
            row++

            transactions.forEach { tx ->
                ws.value(row, 0, DateUtils.formatTimestamp(tx.timestamp, isPersian))
                ws.value(row, 1, tx.title)
                ws.value(row, 2, tx.category)
                ws.value(row, 3, if (tx.type == "INCOME") (if (isPersian) "درآمد" else "Income") else (if (isPersian) "هزینه" else "Expense"))
                ws.value(row, 4, toDisplayAmount(tx.amount, stats.currency))
                ws.value(row, 5, tx.note)
                row++
            }

            (0..5).forEach { ws.width(it, 18.0) }
            workbook.finish()
        }

        return outFile
    }
}