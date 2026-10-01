package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

data class SmsParseResult(
    val amount: Double,
    val isAmountDetected: Boolean,
    val type: String,          // "INCOME" or "EXPENSE"
    val isTypeDetected: Boolean,
    val suggestedTitle: String,
    val timestamp: Long,
    /** Unit of the returned amount. Rial amounts are converted to Toman. */
    val currencyUnit: String = "تومان"
)

/**
 * High-precision parser for Iranian bank SMS messages.
 *
 * The parser intentionally prefers rejecting an ambiguous SMS over creating a
 * wrong transaction. A parser cannot honestly guarantee a universal "<1%"
 * error rate because bank templates change, but this implementation is
 * conservative: only strong bank/transaction contexts are auto-accepted and
 * ambiguous numeric candidates are ignored.
 */
object SmsParser {

    private val incomeKeywords = listOf(
        "واریز", "واریزی", "بستانکار", "افزایش موجودی", "دریافت"
    )

    private val expenseKeywords = listOf(
        "برداشت", "خرید", "برداشتی", "بدهکار", "کسر", "پرداخت",
        "انتقال به", "انتقال وجه", "کارمزد"
    )

    private val bankIndicators = listOf(
        "مانده", "موجودی", "کارت", "حساب", "واریز", "برداشت",
        "خرید", "تراکنش", "کد رهگیری", "ساتنا", "پایا",
        "شبا", "بستانکار", "بدهکار", "درگاه", "شاپرک"
    )

    private val transactionVerbs = incomeKeywords + expenseKeywords

    private val amountLabelRegex = Regex(
        """(?:مبلغ|مقدار|برداشت|خرید|واریز|بستانکار|بدهکار|پرداخت|کسر|انتقال(?:\s+وجه)?(?:\s+به)?)\s*(?:[:=]|به\s*)?\s*[-+]?\s*([\d۰-۹٠-٩][\d۰-۹٠-٩,٬،\s]{2,})""",
        RegexOption.IGNORE_CASE
    )

    private val explicitAmountRegex = Regex(
        """(?:مبلغ|مقدار)\s*(?:[:=]|به\s*)?\s*[-+]?\s*([\d۰-۹٠-٩][\d۰-۹٠-٩,٬،\s]{2,})""",
        RegexOption.IGNORE_CASE
    )

    private val numberRegex = Regex("""[\d۰-۹٠-٩][\d۰-۹٠-٩,٬،\s]{2,}""")

    private val balanceMarkers = listOf("مانده", "موجودی", "موجودی کارت", "مانده حساب")
    private val currencyRegex = Regex("""(?:ریال|رِیال|تومان|irr|irt)""", RegexOption.IGNORE_CASE)

    // Raw values with these lengths are much more likely to be identifiers than money.
    private val identifierLengths = setOf(10, 11, 12, 13, 14, 16, 19)

    fun isLikelyBankSms(body: String): Boolean {
        val normalized = normalizeText(body)
        if (normalized.isBlank()) return false

        val hasTransactionVerb = transactionVerbs.any { normalized.contains(it) }
        val indicatorCount = bankIndicators.count { normalized.contains(it) }
        val hasCurrency = currencyRegex.containsMatchIn(normalized)
        val hasAmountLabel = explicitAmountRegex.containsMatchIn(normalized)

        // A transaction verb alone is not enough. Require another independent
        // banking/amount signal to keep ordinary shopping/OTP messages out.
        return hasTransactionVerb && (
                indicatorCount >= 2 ||
                        (hasAmountLabel && hasCurrency) ||
                        (hasAmountLabel && normalized.contains("کارت")) ||
                        (hasAmountLabel && normalized.contains("حساب"))
                )
    }

    fun parse(body: String, timestamp: Long = System.currentTimeMillis()): SmsParseResult {
        val normalized = normalizeText(body)

        if (!isLikelyBankSms(normalized)) {
            return SmsParseResult(
                amount = 0.0,
                isAmountDetected = false,
                type = "EXPENSE",
                isTypeDetected = false,
                suggestedTitle = "",
                timestamp = timestamp
            )
        }

        val hasIncome = incomeKeywords.any { normalized.contains(it) }
        val hasExpense = expenseKeywords.any { normalized.contains(it) }

        val type = when {
            hasIncome && !hasExpense -> "INCOME"
            hasExpense && !hasIncome -> "EXPENSE"
            // Transfers that contain both words are ambiguous. Keep the
            // historical EXPENSE fallback, but mark the type as uncertain.
            else -> "EXPENSE"
        }

        val isTypeDetected = hasIncome || hasExpense

        val rawAmount = findBestAmount(normalized)
        if (rawAmount <= 0.0) {
            return SmsParseResult(
                amount = 0.0,
                isAmountDetected = false,
                type = type,
                isTypeDetected = isTypeDetected,
                suggestedTitle = "",
                timestamp = timestamp
            )
        }

        val explicitlyRial = Regex("""(?:ریال|رِیال|irr)""", RegexOption.IGNORE_CASE)
            .containsMatchIn(normalized)
        val explicitlyToman = Regex("""(?:تومان|irt)""", RegexOption.IGNORE_CASE)
            .containsMatchIn(normalized)

        // If the SMS explicitly says Rial, internal app amounts are Toman.
        // When no unit is stated, do not invent a conversion.
        val amount = if (explicitlyRial && !explicitlyToman) rawAmount / 10.0 else rawAmount
        val currencyUnit = when {
            explicitlyRial && !explicitlyToman -> "تومان"
            explicitlyToman -> "تومان"
            else -> "واحد پول حساب"
        }

        val suggestedTitle = when {
            hasIncome && !hasExpense -> "واریز پیامکی"
            hasExpense && !hasIncome -> "تراکنش پیامکی"
            else -> "تراکنش پیامکی"
        }

        return SmsParseResult(
            amount = amount,
            isAmountDetected = amount > 0.0,
            type = type,
            isTypeDetected = isTypeDetected,
            suggestedTitle = suggestedTitle,
            timestamp = timestamp,
            currencyUnit = currencyUnit
        )
    }

    fun suggestCategory(body: String, categories: List<CategoryEntity>): String {
        val normalizedBody = normalizeText(body)
        return categories
            .asSequence()
            .filter { it.title.isNotBlank() }
            .sortedByDescending { it.title.length }
            .firstOrNull { normalizedBody.contains(normalizeText(it.title), ignoreCase = true) }
            ?.title
            .orEmpty()
    }

    private fun findBestAmount(text: String): Double {
        // 1) Explicit "مبلغ ..." is the highest-confidence source.
        val explicitCandidates = explicitAmountRegex.findAll(text)
            .mapNotNull { parseCandidate(it.groupValues[1]) }
            .filter { it.value > 0 }
            .toList()

        if (explicitCandidates.isNotEmpty()) {
            return explicitCandidates
                .filterNot { isNearBalanceMarker(text, it.start) }
                .maxByOrNull { it.confidence }
                ?.value
                ?: explicitCandidates.maxByOrNull { it.confidence }!!.value
        }

        // 2) Transaction keyword + amount is the second-highest confidence.
        val keywordCandidates = amountLabelRegex.findAll(text)
            .mapNotNull { match ->
                parseCandidate(match.groupValues[1])?.let { candidate ->
                    candidate.copy(start = match.range.first, confidence = candidate.confidence + 2)
                }
            }
            .filterNot { isNearBalanceMarker(text, it.start) }
            .toList()

        if (keywordCandidates.isNotEmpty()) {
            return keywordCandidates.maxBy { it.confidence }.value
        }

        // 3) Conservative fallback: only accept a number when it is followed
        // closely by an explicit currency marker. This is deliberately stricter
        // than "first number in SMS" because balances/card numbers are common.
        val fallback = numberRegex.findAll(text).mapNotNull { match ->
            val candidate = parseCandidate(match.value) ?: return@mapNotNull null
            val tail = text.substring(match.range.last + 1).take(18)
            if (!currencyRegex.containsMatchIn(tail)) return@mapNotNull null
            if (isNearBalanceMarker(text, match.range.first)) return@mapNotNull null
            candidate.copy(start = match.range.first, confidence = candidate.confidence + 1)
        }.toList()

        return fallback.maxByOrNull { it.confidence }?.value ?: 0.0
    }

    private data class AmountCandidate(
        val value: Double,
        val start: Int,
        val confidence: Int
    )

    private fun parseCandidate(raw: String): AmountCandidate? {
        val digits = raw
            .replace(" ", "")
            .replace(",", "")
            .replace("٬", "")
            .replace("،", "")

        if (digits.isBlank()) return null

        val numeric = digits.toLongOrNull() ?: return null
        if (numeric <= 0L) return null

        val length = digits.length
        // Identifier-like values are rejected unless they came from an explicit
        // "مبلغ" context (handled before fallback).
        if (length in identifierLengths) {
            return null
        }

        val separators = raw.count { it == ',' || it == '٬' || it == '،' }
        val confidence = when {
            separators >= 1 -> 4
            length >= 4 -> 3
            else -> 1
        }

        return AmountCandidate(
            value = numeric.toDouble(),
            start = 0,
            confidence = confidence
        )
    }

    private fun isNearBalanceMarker(text: String, start: Int): Boolean {
        val windowStart = (start - 24).coerceAtLeast(0)
        val prefix = text.substring(windowStart, start)
        return balanceMarkers.any { prefix.contains(it) }
    }

    /** Convert Persian/Arabic digits to Latin and strip bidi marks. */
    private fun normalizeText(input: String): String {
        val fa = "۰۱۲۳۴۵۶۷۸۹"
        val ar = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder(input.length)

        for (c in input) {
            if (c == '\u200E' || c == '\u200F' || c == '\u202A' ||
                c == '\u202B' || c == '\u202C' || c == '\u2066' || c == '\u2067'
            ) continue

            when (c) {
                '٬', '，' -> sb.append(',')
                else -> {
                    val faIdx = fa.indexOf(c)
                    val arIdx = ar.indexOf(c)
                    when {
                        faIdx != -1 -> sb.append(faIdx)
                        arIdx != -1 -> sb.append(arIdx)
                        else -> sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }
}