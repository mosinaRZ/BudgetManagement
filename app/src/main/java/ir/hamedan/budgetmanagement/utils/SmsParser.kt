package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

data class SmsParseResult(
    val amount: Double,
    val isAmountDetected: Boolean,
    val type: String,          // EXPENSE یا INCOME
    val isTypeDetected: Boolean,
    val suggestedTitle: String,
    val timestamp: Long        // اضافه شدن زمان دریافت پیامک به میلی‌ثانیه
)

object SmsParser {

    private val incomeKeywords = listOf("واریز", "واریزی", "بستانکار", "افزایش موجودی")
    private val expenseKeywords = listOf("برداشت", "خرید", "برداشتی", "بدهکار", "کسر", "پرداخت", "انتقال به")

    private val bankSmsIndicators = listOf(
        "مانده", "موجودی", "کارت", "حساب", "واریز", "برداشت", "خرید", "تراکنش", "کد رهگیری", "ساتنا", "پایا"
    )

    fun isLikelyBankSms(body: String): Boolean {
        val normalized = normalizeText(body)

        // باید حداقل دو نشانگر بانکی داشته باشد تا احتمال پیامک عادی کمتر شود
        val matchCount = bankSmsIndicators.count { normalized.contains(it) }
        return matchCount >= 2
    }

    private fun normalizeText(input: String): String {
        val fa = "۰۱۲۳۴۵۶۷۸۹"
        val ar = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder()

        for (c in input) {
            if (c == '\u200E' || c == '\u200F' || c == '\u202A' || c == '\u202B' || c == '\u202C') continue

            val faIdx = fa.indexOf(c)
            val arIdx = ar.indexOf(c)
            when {
                faIdx != -1 -> sb.append(faIdx)
                arIdx != -1 -> sb.append(arIdx)
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun parse(body: String, timestamp: Long = System.currentTimeMillis()): SmsParseResult {
        val normalized = normalizeText(body)

        if (!isLikelyBankSms(normalized)) {
            return SmsParseResult(0.0, false, "EXPENSE", false, "", timestamp)
        }

        val isIncome = incomeKeywords.any { normalized.contains(it) }
        val isExpense = expenseKeywords.any { normalized.contains(it) }

        val type: String = when {
            isIncome && !isExpense -> "INCOME"
            isExpense && !isIncome -> "EXPENSE"
            else -> "EXPENSE"
        }
        val isTypeDetected = isIncome || isExpense

        var detectedAmount = 0.0

        val keywordAmountRegex = Regex(
            """(?:برداشت|خرید|واریز|بستانکار|بدهکار|مبلغ)\s*[:\s]*[-+]?\s*([\d,]{3,})""",
            RegexOption.IGNORE_CASE
        )
        val keywordMatch = keywordAmountRegex.find(normalized)

        if (keywordMatch != null) {
            val rawNum = keywordMatch.groupValues[1].replace(",", "")
            detectedAmount = rawNum.toDoubleOrNull() ?: 0.0
        }

        if (detectedAmount == 0.0) {
            val parts = normalized.split(Regex("""موجودی|مانده"""))
            val transactionPart = parts.firstOrNull() ?: normalized

            val numberRegex = Regex("""[\d,]{3,}""")
            val candidates = numberRegex.findAll(transactionPart)
                .map { it.value.replace(",", "") }
                .mapNotNull { it.toDoubleOrNull() }
                .filter { it > 0 && it.toLong().toString().length != 10 && it.toLong().toString().length != 12 }
                .toList()

            detectedAmount = candidates.firstOrNull() ?: 0.0
        }

        val isAmountDetected = detectedAmount > 0.0

        if (isAmountDetected && normalized.contains("ریال") && !normalized.contains("تومان")) {
            detectedAmount /= 10.0
        }

        val suggestedTitle = when {
            isIncome -> "واریز پیامکی"
            isExpense -> "تراکنش پیامکی"
            else -> "تراکنش نامشخص"
        }

        return SmsParseResult(
            amount = detectedAmount,
            isAmountDetected = isAmountDetected,
            type = type,
            isTypeDetected = isTypeDetected,
            suggestedTitle = suggestedTitle,
            timestamp = timestamp
        )
    }

    fun suggestCategory(body: String, categories: List<CategoryEntity>): String {
        return categories.firstOrNull { body.contains(it.title, ignoreCase = true) }?.title ?: ""
    }
}