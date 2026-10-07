package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

data class SmsParseResult(
    val amount: Double,
    val isAmountDetected: Boolean,
    val type: String,          // "INCOME" or "EXPENSE"
    val isTypeDetected: Boolean,
    val suggestedTitle: String,
    val timestamp: Long,
    /** Unit of the returned amount. Rial amounts are always converted to Toman. */
    val currencyUnit: String = "تومان",
    /** 0..100. How strongly the SMS looks like a real bank transaction. */
    val confidence: Int = 0,
    /** Why the SMS was rejected (null when accepted). Useful for logs/tests. */
    val rejectReason: String? = null
)

/**
 * Precision-first parser for Iranian bank transaction SMS messages.
 *
 * Design (based on how Iranian bank SMS and service/promotional lines work):
 *  - Real transaction SMS are structured: bank name, masked account/card,
 *    a transaction word (برداشت/واریز/خرید...), a signed or labelled amount,
 *    a balance ("مانده") and a date/time.
 *  - Promotional/informational SMS (even from the SAME bank sender) lack that
 *    structure and contain marketing words, links, deadlines, OTP codes, etc.
 *
 * Pipeline: normalize -> hard rejects (OTP, failed tx, reminders, personal
 * sender, 2+ promo markers) -> mask noise (dates, card/account numbers, URLs,
 * phones) -> classify every number by its context (amount / balance / ref /
 * fee / limit) -> score -> accept only if structure score is high enough.
 *
 * NOTE: no parser can honestly promise 100%; the rules below are deliberately
 * conservative (a wrong rejection is cheaper than a wrong transaction), and
 * [SmsParserTest] documents the covered formats so new bank formats can be
 * added as regression cases.
 */
object SmsParser {

    /** Iranian banks report Rial. Used only when the SMS states no unit at all. */
    private const val ASSUME_RIAL_WHEN_UNIT_MISSING = true
    private const val MIN_CANDIDATE_SCORE = 5
    private const val MIN_STRUCTURE_SCORE = 6

    // ---------------------------------------------------------------------
    // Hard-reject rules
    // ---------------------------------------------------------------------

    /** A person texting "واریز کردم 500,000" must never become a bank transaction. */
    private val personalSenderRegex = Regex("""^(?:\+?98|0098|0)?9\d{9}\z""")

    private val otpRegex = Regex(
        """رمز\s*(?:پویا|دوم|یکبار|یک بار|ورود|عبور|ایستا)""" +
                """|کد\s*(?:تایید|تاییدیه|فعال\s*سازی|ورود|امنیتی|احراز|یکبار|یک بار)""" +
                """|شناسه\s*(?:تایید|تاییدیه)""" +
                """|(?<![a-z])otp(?![a-z])|(?<![a-z])cvv2?(?![a-z0-9])|(?<![a-z])pin(?![a-z])"""
    )

    /** Failed / cancelled / rejected: no money moved, so it is not a transaction. */
    private val failedRegex = Regex(
        """ناموفق|نا موفق|موفق نبود|انجام نشد|(?<!\p{L})رد شد|(?<!\p{L})رد گردید""" +
                """|لغو شد|لغو گردید|منقضی|کافی نیست|ناکافی|(?<!\p{L})خطا|عدم موفقیت|نشد(?!ه)"""
    )

    /** Due-date reminders / bills: informational, not an executed transaction. */
    private val reminderRegex = Regex(
        """سر ?رسید|مهلت|موعد|معوق|یادآوری|تا تاریخ|قابل پرداخت|صورت ?حساب"""
    )

    private val urlRegex = Regex(
        """https?://|www\.|(?<![a-z0-9])[a-z0-9][a-z0-9-]*\.(?:ir|com|net|org|ai|co|me|app)(?![a-z])|bit\.ly|t\.me|\*\d{2,4}#"""
    )

    /** Marketing / campaign vocabulary. Two or more distinct hits = rejected. */
    private val promoRegexes: List<Regex> = listOf(
        "تخفیف", "جایزه", "قرعه", "برنده", "هدیه", "کمپین", "جشنواره", "حراج",
        "فروش ویژه", "پیشنهاد", "ویژه", "رایگان", "باشگاه", "همین حالا", "فرصت",
        "ثبت نام", "پیش ثبت", "دانلود", "نصب", "اپلیکیشن", "کلیک", "عضویت",
        "بخشنامه", "اطلاعیه", "تبریک", "مناسبت", "دعوت", "شرکت در",
        "حداقل", "حداکثر", "سقف", "چنانچه", "در صورت", "مراجعه", "کد تخفیف"
    ).map { Regex(it) }

    // ---------------------------------------------------------------------
    // Vocabulary
    // ---------------------------------------------------------------------

    private val incomeWordRegex = Regex(
        """واریز|بستانکار|سود|عودت|برگشت|ایداع|سپرده گذاری""" +
                // Colloquial neo-bank wording (e.g. Blu): "به حساب شما نشست"
                """|(?<!\p{L})نشست(?!\p{L})"""
    )
    private val expenseWordRegex = Regex(
        """برداشت|خرید|پرداخت|کسر|بدهکار|کارمزد|قبض|شارژ""" +
                // Colloquial neo-bank wording (e.g. Blu): "از حساب شما پرید"
                """|(?<!\p{L})پرید(?!\p{L})"""
    )
    private val transactionRegex = Regex(
        incomeWordRegex.pattern + "|" + expenseWordRegex.pattern +
                """|انتقال|حواله|ساتنا|پایا|شتاب|پوز|خودپرداز|کارت به کارت|تراکنش""" +
                """|(?:از|به) حساب (?:شما|ت|تون)"""
    )
    private val postVerbRegex = Regex(
        """واریز|برداشت|خرید|پرداخت|کسر|انتقال|بستانکار|بدهکار""" +
                """|(?<!\p{L})پرید(?!\p{L})|(?<!\p{L})نشست(?!\p{L})"""
    )

    private val bankWordRegex = Regex(
        """بانک|(?<![a-z])bank(?![a-z])|شتاب|پایا|ساتنا|پوز|(?<![a-z])pos(?![a-z])|(?<![a-z])atm(?![a-z])""" +
                """|خودپرداز|همراه بانک|اینترنت بانک|کارت به کارت|ملت|تجارت|صادرات|سپه|پاسارگاد|سامان""" +
                """|پارسیان|رفاه|کشاورزی|آینده|سینا|نوین|کارآفرین|قوامین|توسعه|ایران زمین|گردشگری|حکمت|انصار|رسالت|بلو""" +
                """|(?<![a-z])blu(?:\s*bank)?(?![a-z])"""
    )

    /**
     * Alphanumeric sender ids of banks / neo-banks. A message from one of these that also
     * has transaction structure counts as having bank evidence even when the body never
     * repeats the bank name (Blu texts only "<name> عزیز، X ریال از حساب شما پرید").
     */
    private val bankSenderRegex = Regex(
        """blu|melli|mellat|tejarat|saderat|sepah|pasargad|saman|parsian|refah|keshavarzi|ayandeh""" +
                """|karafarin|ghavamin|resalat|ansar|hekmat|iranzamin|postbank|eghtesad|bank|بلو|بانک""",
        RegexOption.IGNORE_CASE
    )

    /** "از حساب شما ..." / "به حساب شما ..." – typical statement phrasing of neo-banks. */
    private val accountPhraseRegex = Regex("""(?:از|به) حساب (?:شما|ت|تون)""")

    private val balanceRegex = Regex("""مانده|موجودی|باقیمانده|بالانس|balance""")
    private val refRegex = Regex(
        """(?<!\p{L})(?:رهگیری|پیگیری|مرجع|سند|سریال|شناسه|ترمینال|پذیرنده|ref|rrn|stan|شماره|کد|حساب|کارت|شبا|سپرده|تلفن|موبایل)"""
    )
    private val feeRegex = Regex("""کارمزد|هزینه|مالیات|ارزش افزوده""")
    private val limitRegex = Regex("""سقف|حداکثر|حداقل|امتیاز|اعتبار""")
    private val amountLabelRegex = Regex(
        """مبلغ|مقدار|وجه|برداشت|واریز|خرید|پرداخت|کسر|بستانکار|بدهکار|انتقال|سود|شارژ|قبض|حواله|عودت|برگشت|ایداع"""
    )

    private val maskedEvidenceRegex = Regex("""[*•]{2,}|x{3,}|(?:حساب|کارت|شبا|سپرده)\s*:?\s*[\d*•#]{3,}""")
    private val dateTimeEvidenceRegex = Regex(
        """(?<!\d)\d{2,4}[/\-]\d{1,2}[/\-]\d{1,2}(?!\d)|(?<!\d)\d{1,2}:\d{2}(?!\d)"""
    )

    private val numberRegex = Regex("""(?<![\d,.])(\d{1,3}(?:,\d{3})+|\d+)(?!\d)""")
    private val unitAfterRegex = Regex("""^[+\-]?[\s:]{0,3}(ریال|تومان|irr|irt|rls|rials?|toman)""")
    private val unitBeforeRegex = Regex("""(ریال|تومان|irr|irt|rials?|toman)[\s:]{0,3}[+\-]?\z""")

    /** Patterns whose digits must never be mistaken for an amount (same length replaced by '#'). */
    private val noisePatterns: List<Regex> = listOf(
        Regex("""https?://\S+|www\.\S+"""),
        Regex("""(?<![a-z0-9])[a-z0-9][a-z0-9.-]*\.(?:ir|com|net|org|ai|co|me|app)(?![a-z])\S*"""),
        Regex("""ir\d{24}"""),
        Regex("""(?<!\d)(?:\+98|0098|0)9\d{9}(?!\d)"""),
        Regex("""\d{0,6}[*•]{2,}\d{0,6}|x{3,}\d{0,6}"""),
        Regex("""(?<!\d)\d{1,4}[/\-.]\d{1,2}[/\-.]\d{1,4}(?!\d)"""),
        Regex("""(?<!\d)\d{1,2}/\d{1,2}(?!\d)"""),
        Regex("""(?<!\d)\d{1,2}:\d{2}(?::\d{2})?(?!\d)"""),
        Regex("""\d+(?:\.\d+)?\s*(?:%|درصد)""")
    )

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * @param sender optional originating address; personal mobile numbers are rejected.
     */
    fun isLikelyBankSms(body: String, sender: String? = null): Boolean =
        analyze(body, sender).accepted

    fun parse(
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        sender: String? = null
    ): SmsParseResult {
        val analysis = analyze(body, sender)
        val candidate = analysis.candidate

        if (!analysis.accepted || candidate == null) {
            return SmsParseResult(
                amount = 0.0,
                isAmountDetected = false,
                type = "EXPENSE",
                isTypeDetected = false,
                suggestedTitle = "",
                timestamp = timestamp,
                confidence = 0,
                rejectReason = analysis.reason
            )
        }

        val text = analysis.normalized

        // ---- direction -------------------------------------------------
        var type = "EXPENSE"
        var isTypeDetected = true
        when {
            candidate.sign == '+' -> type = "INCOME"
            candidate.sign == '-' -> type = "EXPENSE"
            else -> {
                val byLabel = candidate.label?.let { directionOf(it) } ?: 0
                if (byLabel != 0) {
                    type = if (byLabel > 0) "INCOME" else "EXPENSE"
                } else {
                    val hasIncome = incomeWordRegex.containsMatchIn(text)
                    val hasExpense = expenseWordRegex.containsMatchIn(text)
                    when {
                        hasIncome && !hasExpense -> type = "INCOME"
                        hasExpense && !hasIncome -> type = "EXPENSE"
                        else -> isTypeDetected = false // ambiguous: user must confirm
                    }
                }
            }
        }

        // ---- unit (Rial -> Toman) ----------------------------------------
        val hasRialWord = Regex("""ریال|irr|rls|rial""").containsMatchIn(text)
        val hasTomanWord = Regex("""تومان|irt|toman""").containsMatchIn(text)
        val unit = candidate.unit ?: when {
            hasRialWord && !hasTomanWord -> "rial"
            hasTomanWord && !hasRialWord -> "toman"
            ASSUME_RIAL_WHEN_UNIT_MISSING -> "rial"
            else -> "toman"
        }
        val amount = if (unit == "rial") candidate.value / 10.0 else candidate.value.toDouble()

        val suggestedTitle = if (isTypeDetected && type == "INCOME") "واریز پیامکی" else "تراکنش پیامکی"

        return SmsParseResult(
            amount = amount,
            isAmountDetected = amount > 0.0,
            type = type,
            isTypeDetected = isTypeDetected,
            suggestedTitle = suggestedTitle,
            timestamp = timestamp,
            currencyUnit = "تومان",
            confidence = analysis.confidence,
            rejectReason = null
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

    // ---------------------------------------------------------------------
    // Analysis
    // ---------------------------------------------------------------------

    private enum class Ctx { BALANCE, REF, FEE, LIMIT, AMOUNT }

    private data class Label(val ctx: Ctx, val word: String, val end: Int)

    private data class Candidate(
        val value: Long,
        val start: Int,
        val score: Int,
        val sign: Char?,
        val unit: String?,   // "rial" | "toman" | null
        val label: String?,
        val hasLabel: Boolean
    )

    private data class Analysis(
        val accepted: Boolean,
        val reason: String?,
        val candidate: Candidate?,
        val confidence: Int,
        val normalized: String
    )

    private fun analyze(body: String, sender: String?): Analysis {
        val text = normalizeText(body)
        fun reject(reason: String) = Analysis(false, reason, null, 0, text)

        if (text.isBlank()) return reject("blank")
        if (isPersonalSender(sender)) return reject("personal_sender")
        if (otpRegex.containsMatchIn(text)) return reject("otp")
        if (failedRegex.containsMatchIn(text)) return reject("failed_transaction")
        if (reminderRegex.containsMatchIn(text)) return reject("reminder")

        val promoHits = promoRegexes.count { it.containsMatchIn(text) } +
                if (urlRegex.containsMatchIn(text)) 1 else 0
        if (promoHits >= 2) return reject("promotional")

        val masked = maskNoise(text)
        val tokens = numberRegex.findAll(masked).toList()

        var hasBalance = false
        val candidates = ArrayList<Candidate>()

        tokens.forEachIndexed { i, m ->
            val s = m.range.first
            val e = m.range.last + 1
            val raw = m.value
            val digits = raw.replace(",", "")
            val hasSep = raw.contains(',')

            if (digits.length > 15) return@forEachIndexed
            if (!hasSep && digits.length >= 13) return@forEachIndexed
            if (!hasSep && digits.length >= 4 && digits.startsWith("0")) return@forEachIndexed
            val value = digits.toLongOrNull() ?: return@forEachIndexed
            if (value < 10L) return@forEachIndexed

            val prevEnd = if (i > 0) tokens[i - 1].range.last + 1 else 0
            val nextStart = if (i < tokens.lastIndex) tokens[i + 1].range.first else masked.length

            val seg = masked.substring(maxOf(prevEnd, s - 40, 0), s)
            val hit = classify(seg, plainLength = if (hasSep) 0 else digits.length)

            when (hit?.ctx) {
                Ctx.BALANCE -> { hasBalance = true; return@forEachIndexed }
                Ctx.REF, Ctx.LIMIT -> return@forEachIndexed
                else -> Unit
            }

            val postSeg = masked.substring(e, minOf(nextStart, e + 35, masked.length))
            if (hit == null && i == tokens.lastIndex && balanceRegex.containsMatchIn(postSeg.take(20)) &&
                !postVerbRegex.containsMatchIn(postSeg)
            ) {
                hasBalance = true
                return@forEachIndexed
            }

            val sign = signOf(masked, s, e)
            val unit = unitOf(masked, s, e, prevEnd, nextStart)
            val postVerb = if (hit == null) postVerbRegex.find(postSeg)?.value else null
            val hasLabel = hit?.ctx == Ctx.AMOUNT

            // Long plain digit strings (10-12) are usually account/card/ref numbers.
            if (!hasSep && digits.length in 10..12 && !(hasLabel && unit != null)) {
                return@forEachIndexed
            }

            var score = 0
            if (sign != null) score += 5
            when {
                hasLabel -> score += if (hit!!.word in strongLabels) 6 else 5
                hit?.ctx == Ctx.FEE -> score += 3
                postVerb != null -> score += 3
            }
            if (unit != null) score += 3
            score += if (hasSep) 2 else if (digits.length in 4..9) 1 else 0

            if (score >= MIN_CANDIDATE_SCORE) {
                candidates += Candidate(
                    value = value,
                    start = s,
                    score = score,
                    sign = sign,
                    unit = unit,
                    label = hit?.word ?: postVerb,
                    hasLabel = hasLabel
                )
            }
        }

        val sorted = candidates.sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.start })
        val best = sorted.firstOrNull() ?: return reject("no_amount")
        val runnerUp = sorted.getOrNull(1)
        val ambiguous = runnerUp != null && runnerUp.score == best.score &&
                runnerUp.value != best.value && best.sign == null

        val signed = best.sign != null
        if (promoHits == 1 && !(signed && hasBalance)) return reject("promotional")

        val maskedEvidence = maskedEvidenceRegex.containsMatchIn(text)
        val bankWord = bankWordRegex.containsMatchIn(text) || isBankSender(sender)
        val txnWord = transactionRegex.containsMatchIn(text)

        if (!(hasBalance || maskedEvidence || bankWord || signed)) return reject("no_bank_evidence")
        if (!(txnWord || signed)) return reject("no_transaction_word")

        var structure = 0
        if (signed) structure += 3
        if (best.hasLabel) structure += 2
        if (hasBalance) structure += 2
        if (maskedEvidence) structure += 2
        if (dateTimeEvidenceRegex.containsMatchIn(text)) structure += 1
        if (best.unit != null) structure += 1
        if (accountPhraseRegex.containsMatchIn(text)) structure += 1
        if (bankWord) structure += 2
        if (txnWord) structure += 2

        if (structure < MIN_STRUCTURE_SCORE) return reject("weak_structure")

        var confidence = minOf(100, structure * 100 / 12)
        if (ambiguous) confidence -= 15
        return Analysis(true, null, best, confidence.coerceIn(0, 100), text)
    }

    private val strongLabels = setOf("مبلغ", "مقدار", "وجه")

    /** Finds the closest meaningful keyword before a number and decides what the number is. */
    private fun classify(seg: String, plainLength: Int): Label? {
        val hits = ArrayList<Label>()
        fun collect(re: Regex, ctx: Ctx) {
            re.findAll(seg).forEach { hits += Label(ctx, it.value, it.range.last + 1) }
        }
        collect(balanceRegex, Ctx.BALANCE)
        collect(refRegex, Ctx.REF)
        collect(feeRegex, Ctx.FEE)
        collect(limitRegex, Ctx.LIMIT)
        collect(amountLabelRegex, Ctx.AMOUNT)

        for (h in hits.sortedByDescending { it.end }) {
            val gap = seg.length - h.end
            val between = seg.substring(h.end)
            val ok = when (h.ctx) {
                Ctx.BALANCE -> gap <= 25
                Ctx.LIMIT, Ctx.FEE -> gap <= 14
                Ctx.AMOUNT -> gap <= 30
                // "حساب شما 500,000" must not make 500,000 an account number:
                // a ref label only counts when it is directly attached to the number.
                Ctx.REF -> (gap <= 5 && between.none { it.isLetter() }) || (plainLength >= 8 && gap <= 12)
            }
            if (ok) return h
        }
        return null
    }

    private fun signOf(t: String, s: Int, e: Int): Char? {
        fun isSign(c: Char) = c == '+' || c == '-'
        if (s >= 1 && isSign(t[s - 1]) && (s < 2 || !t[s - 2].isLetterOrDigit())) return t[s - 1]
        if (e < t.length && isSign(t[e]) && (e + 1 >= t.length || !t[e + 1].isDigit())) return t[e]
        return null
    }

    private fun unitOf(t: String, s: Int, e: Int, prevEnd: Int, nextStart: Int): String? {
        val after = t.substring(e, minOf(t.length, nextStart, e + 14))
        unitAfterRegex.find(after)?.let { return toUnit(it.groupValues[1]) }
        val before = t.substring(maxOf(s - 12, prevEnd, 0), s)
        unitBeforeRegex.find(before)?.let { return toUnit(it.groupValues[1]) }
        return null
    }

    private fun toUnit(word: String): String =
        if (word == "ریال" || word == "irr" || word == "rls" || word.startsWith("rial")) "rial" else "toman"

    private fun directionOf(word: String): Int = when {
        incomeWordRegex.containsMatchIn(word) -> 1
        expenseWordRegex.containsMatchIn(word) -> -1
        else -> 0
    }

    private fun isBankSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val letters = sender.filter { it.isLetter() }
        return letters.isNotEmpty() && bankSenderRegex.containsMatchIn(letters)
    }

    private fun isPersonalSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        if (sender.any { it.isLetter() }) return false
        val cleaned = sender.filter { it.isDigit() || it == '+' }
        return personalSenderRegex.matches(cleaned)
    }

    private fun maskNoise(text: String): String {
        var s = text
        for (p in noisePatterns) {
            s = p.replace(s) { "#".repeat(it.value.length) }
        }
        return s
    }

    /**
     * Persian/Arabic digits -> Latin, Arabic letter variants -> Persian, strips bidi marks,
     * ZWNJ -> space, unifies separators/minus signs, lowercases.
     */
    private fun normalizeText(input: String): String {
        val fa = "۰۱۲۳۴۵۶۷۸۹"
        val ar = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder(input.length)

        for (c in input) {
            when (c) {
                '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
                '\u2066', '\u2067', '\u2068', '\u2069', '\uFEFF', '\u0640' -> Unit
                '\u200C', '\u00A0', '\t' -> sb.append(' ')
                '٬', '،', '，' -> sb.append(',')
                '٫' -> sb.append('.')
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ۀ', 'ة' -> sb.append('ه')
                'أ', 'إ', 'ٱ' -> sb.append('ا')
                '−', '–', '—', '‐', '‑' -> sb.append('-')
                else -> {
                    val faIdx = fa.indexOf(c)
                    val arIdx = ar.indexOf(c)
                    when {
                        faIdx != -1 -> sb.append(faIdx)
                        arIdx != -1 -> sb.append(arIdx)
                        else -> sb.append(c.lowercaseChar())
                    }
                }
            }
        }
        return sb.toString().replace(Regex(""" {2,}"""), " ").trim()
    }
}