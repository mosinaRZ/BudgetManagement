package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

data class SmsParseResult(
    val amount: Double,
    val isAmountDetected: Boolean,
    val type: String,          // "INCOME" or "EXPENSE"
    val isTypeDetected: Boolean,
    val suggestedTitle: String,
    val timestamp: Long
)

/**
 * Heuristic parser for Iranian bank SMS messages.
 * Not 100% accurate — results are stored as PendingTransaction for user review.
 */
object SmsParser {

    private val incomeKeywords = listOf(
        "واریز", "واریزی", "بستانکار", "افزایش موجودی"
    )

    private val expenseKeywords = listOf(
        "برداشت", "خرید", "برداشتی", "بدهکار", "کسر", "پرداخت", "انتقال به"
    )

    private val bankSmsIndicators = listOf(
        "مانده", "موجودی", "کارت", "حساب", "واریز", "برداشت",
        "خرید", "تراکنش", "کد رهگیری", "ساتنا", "پایا"
    )

    // Amounts that look like phone numbers (10 digits) or card-like (12 digits) are ignored
    private val ignoredDigitLengths = setOf(10, 12)

    private val keywordAmountRegex = Regex(
        """(?:برداشت|خرید|واریز|بستانکار|بدهکار|مبلغ)\s*[:\s]*[-+]?\s*([\d,]{3,})""",
        RegexOption.IGNORE_CASE
    )

    private val numberRegex = Regex("""[\d,]{3,}""")

    fun isLikelyBankSms(body: String): Boolean {
        val normalized = normalizeText(body)
        val matchCount = bankSmsIndicators.count { normalized.contains(it) }
        // Require at least 2 bank-related keywords to reduce false positives
        return matchCount >= 2
    }

    fun parse(body: String, timestamp: Long = System.currentTimeMillis()): SmsParseResult {
        val normalized = normalizeText(body)

        if (!isLikelyBankSms(normalized)) {
            return SmsParseResult(0.0, false, "EXPENSE", false, "", timestamp)
        }

        val isIncome = incomeKeywords.any { normalized.contains(it) }
        val isExpense = expenseKeywords.any { normalized.contains(it) }

        val type = when {
            isIncome && !isExpense -> "INCOME"
            isExpense && !isIncome -> "EXPENSE"
            else -> "EXPENSE"
        }
        val isTypeDetected = isIncome || isExpense

        var detectedAmount = extractAmountNearKeywords(normalized)

        if (detectedAmount == 0.0) {
            detectedAmount = extractAmountFallback(normalized)
        }

        val isAmountDetected = detectedAmount > 0.0

        // Many banks still show amounts in Rial; convert to Toman when only "ریال" is present
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
        return categories
            .firstOrNull { body.contains(it.title, ignoreCase = true) }
            ?.title
            .orEmpty()
    }

    // --- private helpers ---

    private fun extractAmountNearKeywords(text: String): Double {
        val match = keywordAmountRegex.find(text) ?: return 0.0
        val raw = match.groupValues[1].replace(",", "")
        return raw.toDoubleOrNull() ?: 0.0
    }

    private fun extractAmountFallback(text: String): Double {
        // Prefer the part before balance keywords (موجودی / مانده)
        val transactionPart = text.split(Regex("""موجودی|مانده""")).firstOrNull() ?: text

        return numberRegex.findAll(transactionPart)
            .map { it.value.replace(",", "") }
            .mapNotNull { it.toDoubleOrNull() }
            .filter { amount ->
                amount > 0 && amount.toLong().toString().length !in ignoredDigitLengths
            }
            .firstOrNull() ?: 0.0
    }

    /** Convert Persian/Arabic digits to Latin and strip bidi marks */
    private fun normalizeText(input: String): String {
        val fa = "۰۱۲۳۴۵۶۷۸۹"
        val ar = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder(input.length)

        for (c in input) {
            // Skip common bidi / invisible marks
            if (c == '\u200E' || c == '\u200F' || c == '\u202A' || c == '\u202B' || c == '\u202C') {
                continue
            }
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
}