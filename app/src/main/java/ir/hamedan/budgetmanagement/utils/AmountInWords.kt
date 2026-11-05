package ir.hamedan.budgetmanagement.utils

/**
 * مبلغ را به حروف تبدیل می‌کند تا کاربر هنگام تایپ، اشتباه در تعداد صفرها را همان لحظه ببیند.
 *
 * فارسی: «دو میلیون و پانصد هزار»
 * انگلیسی: نسخه‌ی فشرده مثل «2.5 million»
 */
object AmountInWords {

    private val ones = arrayOf(
        "", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
        "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده"
    )
    private val tens = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val hundreds = arrayOf(
        "", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد"
    )
    private val scales = arrayOf("", "هزار", "میلیون", "میلیارد", "هزار میلیارد")

    /** عدد صفر تا ۹۹۹. */
    private fun threeDigits(n: Int): String {
        val parts = mutableListOf<String>()
        val h = n / 100
        val rest = n % 100
        if (h > 0) parts += hundreds[h]
        if (rest in 1..19) {
            parts += ones[rest]
        } else if (rest >= 20) {
            parts += tens[rest / 10]
            if (rest % 10 > 0) parts += ones[rest % 10]
        }
        return parts.joinToString(" و ")
    }

    /** مبلغ به حروف فارسی؛ برای صفر یا مقدار منفی رشته‌ی خالی برمی‌گرداند. */
    fun persian(value: Long): String {
        if (value <= 0L) return ""
        // گروه‌های سه‌رقمی از کم‌ارزش به پرارزش
        val groups = mutableListOf<Int>()
        var remaining = value
        while (remaining > 0) {
            groups += (remaining % 1000).toInt()
            remaining /= 1000
        }
        if (groups.size > scales.size) return ""
        val parts = mutableListOf<String>()
        for (i in groups.indices.reversed()) {
            val g = groups[i]
            if (g == 0) continue
            val words = threeDigits(g)
            parts += if (scales[i].isEmpty()) words else "$words ${scales[i]}"
        }
        return parts.joinToString(" و ")
    }

    /** نسخه‌ی فشرده‌ی انگلیسی؛ زیر هزار چیزی نشان نمی‌دهد چون خود عدد کافی است. */
    fun englishCompact(value: Long): String {
        if (value < 1_000L) return ""
        val (divisor, name) = when {
            value >= 1_000_000_000L -> 1_000_000_000.0 to "billion"
            value >= 1_000_000L -> 1_000_000.0 to "million"
            else -> 1_000.0 to "thousand"
        }
        val scaled = value / divisor
        val text = if (scaled % 1.0 == 0.0) scaled.toLong().toString() else String.format(java.util.Locale.US, "%.2f", scaled).trimEnd('0').trimEnd('.')
        return "$text $name"
    }
}