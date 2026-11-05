package ir.hamedan.budgetmanagement.utils

import kotlin.math.roundToLong

/**
 * یک جمله‌ی گفتاری مثل «ناهار دویست و پنجاه هزار تومان» را به مبلغ، عنوان و نوع تراکنش تبدیل می‌کند.
 *
 * - مبلغ: ارقام (فارسی/عربی/انگلیسی) یا عدد حروفی («دو و نیم میلیون»، «۲۵۰ هزار»، «250k»)
 * - واحد: تومان/ریال اگر گفته شده باشد
 * - نوع: از کلیدواژه‌ها (حقوق، دریافت، خریدم، ...)؛ اگر نبود null تا نوع فعلی حفظ شود
 * - عنوان: باقی‌مانده‌ی جمله بدون مبلغ و افعال اضافه
 */
object SpokenTransactionParser {

    enum class SpokenUnit { TOMAN, RIAL, NONE }

    data class Result(
        val amount: Long?,
        val unit: SpokenUnit,
        val title: String,
        val isExpense: Boolean?
    )

    private val ones = mapOf(
        "صفر" to 0, "یک" to 1, "یه" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5,
        "شش" to 6, "شیش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10,
        "یازده" to 11, "دوازده" to 12, "سیزده" to 13, "چهارده" to 14, "پانزده" to 15,
        "پونزده" to 15, "شانزده" to 16, "شونزده" to 16, "هفده" to 17, "هجده" to 18, "نوزده" to 19,
        "بیست" to 20, "سی" to 30, "چهل" to 40, "پنجاه" to 50, "شصت" to 60,
        "هفتاد" to 70, "هشتاد" to 80, "نود" to 90,
        "صد" to 100, "یکصد" to 100, "دویست" to 200, "سیصد" to 300, "چهارصد" to 400,
        "پانصد" to 500, "ششصد" to 600, "هفتصد" to 700, "هشتصد" to 800, "نهصد" to 900
    )

    private val scales = mapOf(
        "هزار" to 1_000.0, "میلیون" to 1_000_000.0, "میلیارد" to 1_000_000_000.0,
        "thousand" to 1_000.0, "million" to 1_000_000.0, "billion" to 1_000_000_000.0
    )

    private val tomanWords = setOf("تومان", "تومن", "تومانی", "toman", "tomans")
    private val rialWords = setOf("ریال", "ریالی", "rial", "rials")

    private val incomeWords = setOf(
        "حقوق", "درآمد", "دریافت", "دریافتی", "واریز", "پاداش", "فروختم", "سود", "عیدی",
        "salary", "income", "received", "earned", "got"
    )
    private val expenseWords = setOf(
        "خریدم", "خرید", "خرج", "پرداخت", "پرداختم", "دادم", "هزینه",
        "spent", "paid", "bought", "cost"
    )

    /** کلمات رابط/فعل که در عنوان نباید بمانند. */
    private val fillers = setOf(
        "خریدم", "خرید", "دادم", "پرداختم", "پرداخت", "کردم", "خرج", "مبلغ", "برای", "به", "رو", "را",
        "دریافت", "دریافتی", "واریز", "درآمد", "هزینه", "یه", "بابت",
        "spent", "paid", "bought", "received", "earned", "got", "for", "on", "a", "the", "cost", "income"
    ) + tomanWords + rialWords

    private enum class Kind { NUM, HALF, SCALE, CONNECTOR, CURRENCY, TEXT }

    private class Token(val text: String, val kind: Kind, val value: Double = 0.0)

    private val thousandsDot = Regex("^\\d{1,3}(\\.\\d{3})+$")
    private val plainNumber = Regex("^\\d+(\\.\\d+)?$")
    private val kNumber = Regex("^(\\d+(\\.\\d+)?)k$")

    private fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            sb.append(
                when (ch) {
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    'ي' -> 'ی'
                    'ك' -> 'ک'
                    '\u200c' -> ' '
                    '٫' -> '.'
                    else -> ch
                }
            )
        }
        // جداکننده‌ی هزارگان بین ارقام حذف می‌شود؛ بقیه‌ی ویرگول‌ها فاصله می‌شوند.
        return sb.toString()
            .lowercase()
            .replace(Regex("(?<=\\d)[,٬،](?=\\d)"), "")
            .replace(Regex("[,٬،؟?!.:;«»\"“”]+(?!\\d)"), " ")
    }

    private fun classify(word: String): Token {
        ones[word]?.let { return Token(word, Kind.NUM, it.toDouble()) }
        scales[word]?.let { return Token(word, Kind.SCALE, it) }
        if (word == "نیم") return Token(word, Kind.HALF, 0.5)
        if (word == "و" || word == "and") return Token(word, Kind.CONNECTOR)
        if (word in tomanWords || word in rialWords) return Token(word, Kind.CURRENCY)
        if (thousandsDot.matches(word)) return Token(word, Kind.NUM, word.replace(".", "").toDouble())
        if (plainNumber.matches(word)) return Token(word, Kind.NUM, word.toDouble())
        kNumber.matchEntire(word)?.let {
            return Token(word, Kind.SCALE, it.groupValues[1].toDouble() * 1_000.0)
        }
        return Token(word, Kind.TEXT)
    }

    private class Run(val start: Int, val end: Int, val value: Double, val unit: SpokenUnit, val marked: Boolean)

    /** مقدار یک دسته‌ی پیوسته از توکن‌های عددی. */
    private fun evaluate(tokens: List<Token>): Double {
        var total = 0.0
        var current = 0.0
        var lastScale = 0.0
        for (t in tokens) {
            when (t.kind) {
                Kind.NUM -> current += t.value
                Kind.HALF -> {
                    if (current == 0.0 && total > 0.0 && lastScale > 1.0) total += lastScale * 0.5
                    else current += 0.5
                }
                Kind.SCALE -> {
                    // «250k» خودش مقدار کامل است؛ «هزار» ضریب عدد قبلی است.
                    if (t.text.endsWith("k") && t.text.length > 1) {
                        total += t.value
                    } else {
                        total += (if (current == 0.0) 1.0 else current) * t.value
                        lastScale = t.value
                    }
                    current = 0.0
                }
                else -> Unit
            }
        }
        return total + current
    }

    fun parse(spoken: String): Result {
        val words = normalize(spoken).split(Regex("\\s+")).filter { it.isNotBlank() }
        val tokens = words.map(::classify)

        fun isNumeric(i: Int) = i < tokens.size && tokens[i].kind.let { it == Kind.NUM || it == Kind.SCALE || it == Kind.HALF }

        // دسته‌های عددیِ پیوسته (با «و» بین اعداد)
        val runs = mutableListOf<Run>()
        var i = 0
        while (i < tokens.size) {
            if (tokens[i].kind != Kind.NUM && tokens[i].kind != Kind.SCALE) {
                i++
                continue
            }
            val start = i
            var end = i
            while (true) {
                val next = end + 1
                when {
                    isNumeric(next) -> end = next
                    next < tokens.size && tokens[next].kind == Kind.CONNECTOR && isNumeric(next + 1) -> end = next + 1
                    else -> break
                }
            }
            val numeric = tokens.subList(start, end + 1).filter { it.kind != Kind.CONNECTOR }
            val value = evaluate(numeric)
            var unit = SpokenUnit.NONE
            var runEnd = end
            if (end + 1 < tokens.size && tokens[end + 1].kind == Kind.CURRENCY) {
                unit = if (tokens[end + 1].text in rialWords) SpokenUnit.RIAL else SpokenUnit.TOMAN
                runEnd = end + 1
            }
            val marked = unit != SpokenUnit.NONE || numeric.any { it.kind == Kind.SCALE }
            runs += Run(start, runEnd, value, unit, marked)
            i = runEnd + 1
        }

        val candidates = runs.filter { it.value > 0.0 }
        val chosen = candidates.lastOrNull { it.marked } ?: candidates.lastOrNull()

        val isExpense = when {
            words.any { it in incomeWords } -> false
            words.any { it in expenseWords } -> true
            else -> null
        }

        val titleWords = words.filterIndexed { index, word ->
            val inRun = chosen != null && index in chosen.start..chosen.end
            // یک «و» تنها که کنار مبلغِ حذف‌شده مانده، یا کلمه‌ی رابط، در عنوان نمی‌ماند.
            !inRun && word !in fillers
        }.let { list ->
            var trimmed = list
            while (trimmed.isNotEmpty() && (trimmed.first() == "و" || trimmed.first() == "and")) trimmed = trimmed.drop(1)
            while (trimmed.isNotEmpty() && (trimmed.last() == "و" || trimmed.last() == "and")) trimmed = trimmed.dropLast(1)
            trimmed
        }

        return Result(
            amount = chosen?.value?.roundToLong(),
            unit = chosen?.unit ?: SpokenUnit.NONE,
            title = titleWords.joinToString(" ").take(40).trim(),
            isExpense = isExpense
        )
    }

    /** مبلغ را به واحد نمایشی کاربر (تومان یا ریال) تبدیل می‌کند؛ اگر مبلغی نبود null. */
    fun displayAmount(result: Result, displayIsRial: Boolean): Long? {
        val amount = result.amount ?: return null
        return when {
            result.unit == SpokenUnit.TOMAN && displayIsRial -> amount * 10
            result.unit == SpokenUnit.RIAL && !displayIsRial -> amount / 10
            else -> amount
        }
    }
}