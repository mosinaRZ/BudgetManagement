package ir.hamedan.budgetmanagement.utils

/**
 * متن‌های سیستمی تراکنش‌ها (واریز/برداشت قلک، واریز ماهانه خودکار، بدهی و طلب و ...).
 *
 * این متن‌ها هنگام ساخت تراکنش در دیتابیس ذخیره می‌شوند و ممکن است به زبانِ همان لحظه باشند.
 * برای اینکه با تغییر زبان برنامه (مثل دسته‌بندی‌ها در [StringMapper]) همیشه به زبان فعلی
 * نمایش داده شوند، هنگام «نمایش» عنوان/یادداشت را تشخیص می‌دهیم و به زبان جاری برمی‌گردانیم.
 * متن‌های ساخته‌شده توسط کاربر دست‌نخورده می‌مانند. نام قلک/شخص (بعد از «:») هم ترجمه نمی‌شود.
 */
object SystemTransactionText {

    private data class PrefixRule(val fa: String, val en: String)
    private data class ExactRule(val fa: String, val en: String)

    // عنوان‌هایی که با یک پیشوند ثابت شروع می‌شوند و بعدش نام قلک/شخص می‌آید.
    // ترتیب مهم است: پیشوندهای طولانی‌تر/اختصاصی‌تر اول بیایند.
    private val titlePrefixes = listOf(
        PrefixRule("واریز خودکار ماهانه به قلک: ", "Auto Monthly Deposit to: "),
        PrefixRule("واریز به قلک: ", "Deposit to saving goal: "),
        PrefixRule("برداشت از قلک: ", "Withdrawal from saving goal: "),
        PrefixRule("انتقال موجودی قلک هنگام حذف: ", "Saving goal balance transferred on delete: "),
        PrefixRule("اصلاح/برداشت از پرداخت بدهی: ", "Correction/withdrawal of debt payment: "),
        PrefixRule("اصلاح/برداشت از دریافت طلب: ", "Correction/withdrawal of receivable payment: "),
        PrefixRule("دریافت وام/بدهی: ", "Loan/debt received: "),
        PrefixRule("پرداخت وام/طلب: ", "Loan/credit paid out: "),
        PrefixRule("پرداخت بدهی به: ", "Debt payment to: "),
        PrefixRule("دریافت طلب از: ", "Receivable collected from: ")
    )

    private val notePrefixes = listOf(
        PrefixRule("ثبت اولیه ", "Initial entry ")
    )

    private val noteExact = listOf(
        ExactRule("واریز دستی به قلک", "Manual saving goal deposit"),
        ExactRule("برداشت دستی از قلک", "Manual saving goal withdrawal"),
        ExactRule("انتقال موجودی قلک به تراز کل هنگام حذف", "Saving goal balance transferred to total balance on deletion"),
        ExactRule("واریز خودکار ماهانه", "Automatic monthly deposit"),
        ExactRule("ثبت و اصلاح واریزی بدهی و طلب", "Debt and receivable payment entry/correction"),
        ExactRule("تسویه از طریق یادآوری سررسید", "Settled via due-date reminder")
    )

    fun localizeTitle(title: String, isPersian: Boolean): String =
        localizePrefix(title, titlePrefixes, isPersian)

    fun localizeNote(note: String?, isPersian: Boolean): String? {
        if (note.isNullOrBlank()) return note
        val trimmed = note.trim()
        noteExact.firstOrNull { it.fa == trimmed || it.en.equals(trimmed, ignoreCase = true) }
            ?.let { return if (isPersian) it.fa else it.en }
        return localizePrefix(note, notePrefixes, isPersian)
    }

    private fun localizePrefix(text: String, rules: List<PrefixRule>, isPersian: Boolean): String {
        if (text.isEmpty()) return text
        val trimmed = text.trimStart()
        for (rule in rules) {
            val matched = when {
                trimmed.startsWith(rule.fa) -> rule.fa
                trimmed.startsWith(rule.en, ignoreCase = true) -> rule.en
                else -> null
            } ?: continue
            val rest = trimmed.substring(matched.length)
            return (if (isPersian) rule.fa else rule.en) + rest
        }
        return text
    }
}