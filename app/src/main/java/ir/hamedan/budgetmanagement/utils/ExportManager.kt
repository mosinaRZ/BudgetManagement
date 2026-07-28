package ir.hamedan.budgetmanagement.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import java.io.File

enum class ExportFormat { PDF, XLSX }

object ExportManager {

    suspend fun export(context: Context, format: ExportFormat, period: ExportPeriod, isPersian: Boolean) {        val db = AppDatabase.getInstance(context)
        val repo = TransactionRepository(db.transactionDao())

        val range = ExportPeriodCalculator.resolve(period)
        val transactions = repo.getTransactionsBetween(range.startMillis, range.endMillis)
        val openingBalance = repo.getBalanceBefore(range.startMillis)

        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val balance = openingBalance + income - expense

        // معدل موجودی: میانگین موجودی روزانه در طول بازه
        val averageBalance = calculateAverageBalance(transactions, openingBalance, range.startMillis, range.endMillis)

        val stats = ExportStats(
            openingBalance = openingBalance,
            totalIncome = income,
            totalExpense = expense,
            balance = balance,
            averageBalance = averageBalance,
            currency = CurrencySharedPreferences.getCurrency(context),
            issueDateMillis = System.currentTimeMillis(),
            startMillis = range.startMillis,
            endMillis = range.endMillis
        )

        val file: File = when (format) {
            ExportFormat.PDF -> PdfExporter.generate(context, transactions, stats, isPersian)
            ExportFormat.XLSX -> XlsxExporter.generate(context, transactions, stats, isPersian)
        }

        shareFile(context, file, format)
    }

    private fun calculateAverageBalance(
        transactions: List<TransactionEntity>,
        openingBalance: Double,
        start: Long,
        end: Long
    ): Double {
        if (transactions.isEmpty()) return openingBalance
        var running = openingBalance
        var weightedSum = 0.0
        var lastTimestamp = start
        for (tx in transactions) {
            val days = (tx.timestamp - lastTimestamp) / 86_400_000.0
            weightedSum += running * days
            running += if (tx.type == "INCOME") tx.amount else -tx.amount
            lastTimestamp = tx.timestamp
        }
        weightedSum += running * ((end - lastTimestamp) / 86_400_000.0)
        val totalDays = (end - start) / 86_400_000.0
        return if (totalDays > 0) weightedSum / totalDays else running
    }

    private fun shareFile(context: Context, file: File, format: ExportFormat) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = if (format == ExportFormat.PDF) "application/pdf"
        else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            // اگه هیچ اپی برای نمایش این فرمت روی گوشی نصب نبود، بریم سراغ اشتراک‌گذاری
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}