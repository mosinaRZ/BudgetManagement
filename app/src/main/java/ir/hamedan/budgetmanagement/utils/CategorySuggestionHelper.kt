package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity

/**
 * پیشنهاد خودکار دسته‌بندی بر اساس عنوان تراکنش.
 *
 * - از لیست واقعی دسته‌بندی‌های دیتابیس استفاده می‌کند (سیستمی + سفارشی کاربر)
 * - برای دسته‌های سیستمی از کلیدواژه‌های ازپیش‌تعریف‌شده استفاده می‌کند
 * - برای دسته‌های سفارشی، خودِ عنوان دسته‌بندی (و نام نمایشی) را با عنوان تراکنش مقایسه می‌کند
 * - کاربر همچنان می‌تواند دستی دسته‌بندی را عوض کند
 */
object CategorySuggestionHelper {

    /**
     * کلیدواژه‌های کمکی برای دسته‌های سیستمی.
     * کلید = کلمه، مقدار = title دسته‌بندی در دیتابیس (مثلاً "FOOD")
     */
    private val systemKeywordMap: Map<String, String> = mapOf(
        // FOOD
        "غذا" to "FOOD", "خوراک" to "FOOD", "رستوران" to "FOOD", "کافه" to "FOOD",
        "صبحانه" to "FOOD", "ناهار" to "FOOD", "شام" to "FOOD", "پیتزا" to "FOOD",
        "برگر" to "FOOD", "ساندویچ" to "FOOD", "قلیان" to "FOOD", "چای" to "FOOD",
        "قهوه" to "FOOD", "نان" to "FOOD", "میوه" to "FOOD", "سبزی" to "FOOD",
        "سوپرمارکت" to "FOOD", "هایپر" to "FOOD", "خرید خوراکی" to "FOOD",
        "food" to "FOOD", "restaurant" to "FOOD", "cafe" to "FOOD", "coffee" to "FOOD",
        "lunch" to "FOOD", "dinner" to "FOOD", "breakfast" to "FOOD", "pizza" to "FOOD",
        "burger" to "FOOD", "grocery" to "FOOD", "supermarket" to "FOOD",

        // TRANSPORT
        "تاکسی" to "TRANSPORT", "اسنپ" to "TRANSPORT", "تپسی" to "TRANSPORT",
        "اتوبوس" to "TRANSPORT", "مترو" to "TRANSPORT", "بنزین" to "TRANSPORT",
        "سوخت" to "TRANSPORT", "پارکینگ" to "TRANSPORT", "عوارض" to "TRANSPORT",
        "بلیط" to "TRANSPORT", "سفر" to "TRANSPORT", "ماشین" to "TRANSPORT",
        "موتور" to "TRANSPORT", "اسنپ فود" to "FOOD",
        "taxi" to "TRANSPORT", "uber" to "TRANSPORT", "bus" to "TRANSPORT",
        "metro" to "TRANSPORT", "fuel" to "TRANSPORT", "gas" to "TRANSPORT",
        "parking" to "TRANSPORT", "transport" to "TRANSPORT", "ride" to "TRANSPORT",

        // SHOPPING
        "خرید" to "SHOPPING", "لباس" to "SHOPPING", "کفش" to "SHOPPING",
        "موبایل" to "SHOPPING", "لپتاپ" to "SHOPPING", "الکترونیک" to "SHOPPING",
        "دیجی‌کالا" to "SHOPPING", "دیجیکالا" to "SHOPPING", "بازار" to "SHOPPING",
        "مبلمان" to "SHOPPING", "دکوراسیون" to "SHOPPING", "کتاب" to "SHOPPING",
        "shopping" to "SHOPPING", "clothes" to "SHOPPING", "shoes" to "SHOPPING",
        "phone" to "SHOPPING", "laptop" to "SHOPPING", "amazon" to "SHOPPING",
        "online" to "SHOPPING", "store" to "SHOPPING",

        // BILL
        "قبوض" to "BILL", "قبض" to "BILL", "برق" to "BILL", "آب" to "BILL",
        "گاز" to "BILL", "اینترنت" to "BILL", "تلفن" to "BILL",
        "اجاره" to "BILL", "شارژ" to "BILL", "بیمه" to "BILL", "مالیات" to "BILL",
        "bill" to "BILL", "rent" to "BILL", "electricity" to "BILL", "water" to "BILL",
        "internet" to "BILL", "phone bill" to "BILL", "insurance" to "BILL",
        "tax" to "BILL", "utility" to "BILL",

        // SALARY
        "حقوق" to "SALARY", "دستمزد" to "SALARY", "حقوق ماهانه" to "SALARY",
        "حقوقی" to "SALARY", "پرداخت حقوق" to "SALARY",
        "salary" to "SALARY", "wage" to "SALARY", "payroll" to "SALARY",
        "income" to "SALARY", "paycheck" to "SALARY",

        // INVESTMENT
        "سرمایه‌گذاری" to "INVESTMENT", "بورس" to "INVESTMENT", "سهام" to "INVESTMENT",
        "ارز دیجیتال" to "INVESTMENT", "بیت‌کوین" to "INVESTMENT", "طلا" to "INVESTMENT",
        "سپرده" to "INVESTMENT", "سود بانکی" to "INVESTMENT",
        "investment" to "INVESTMENT", "stock" to "INVESTMENT", "crypto" to "INVESTMENT",
        "bitcoin" to "INVESTMENT", "gold" to "INVESTMENT", "deposit" to "INVESTMENT",
        "dividend" to "INVESTMENT",

        // DEBT
        "قسط" to "DEBT_CREDIT_PAYABLE", "وام" to "DEBT_CREDIT_PAYABLE",
        "بدهی" to "DEBT_CREDIT_PAYABLE", "پرداخت قسط" to "DEBT_CREDIT_PAYABLE",
        "loan" to "DEBT_CREDIT_PAYABLE", "debt" to "DEBT_CREDIT_PAYABLE",
        "installment" to "DEBT_CREDIT_PAYABLE",

        // RECEIVABLE
        "طلب" to "DEBT_CREDIT_RECEIVABLE", "وصول" to "DEBT_CREDIT_RECEIVABLE",
        "receivable" to "DEBT_CREDIT_RECEIVABLE"
    )

    /**
     * پیشنهاد بهترین دسته‌بندی از بین لیست واقعی دیتابیس.
     *
     * @param title عنوان تراکنش که کاربر وارد کرده
     * @param availableCategories لیست دسته‌بندی‌هایی که با نوع فعلی (هزینه/درآمد) مطابقت دارند
     * @param isPersian برای گرفتن نام نمایشی دسته‌های سیستمی
     * @return title دسته‌بندی پیشنهادی، یا null
     */
    fun suggestCategory(
        title: String,
        availableCategories: List<CategoryEntity>,
        isPersian: Boolean = true
    ): String? {
        if (title.isBlank() || availableCategories.isEmpty()) return null

        val normalizedTitle = normalize(title)
        val scores = mutableMapOf<String, Int>()

        // ۱) امتیاز از کلیدواژه‌های سیستمی (فقط برای دسته‌هایی که واقعاً در لیست موجودند)
        val availableKeys = availableCategories.map { it.title }.toSet()
        for ((keyword, categoryKey) in systemKeywordMap) {
            if (categoryKey !in availableKeys) continue
            if (normalizedTitle.contains(normalize(keyword))) {
                val current = scores.getOrDefault(categoryKey, 0)
                scores[categoryKey] = current + normalize(keyword).length + 10
            }
        }

        // ۲) امتیاز از خودِ عنوان دسته‌بندی‌ها (سیستمی و سفارشی)
        for (category in availableCategories) {
            val categoryKey = category.title
            val displayName = StringMapper.getCategoryName(categoryKey, isPersian)
            val candidates = listOf(categoryKey, displayName)
                .map { normalize(it) }
                .filter { it.length >= 2 }

            for (candidate in candidates) {
                if (candidate.isBlank()) continue

                // عنوان تراکنش شامل نام دسته باشد
                if (normalizedTitle.contains(candidate)) {
                    val current = scores.getOrDefault(categoryKey, 0)
                    scores[categoryKey] = current + candidate.length + 5
                }
                // یا نام دسته شامل بخش قابل‌توجهی از عنوان باشد (برای عناوین کوتاه)
                else if (candidate.contains(normalizedTitle) && normalizedTitle.length >= 3) {
                    val current = scores.getOrDefault(categoryKey, 0)
                    scores[categoryKey] = current + normalizedTitle.length
                }
            }
        }

        // ۳) تطبیق کلمه به کلمه (مفید برای دسته‌های سفارشی)
        val titleWords = normalizedTitle.split(Regex("\\s+")).filter { it.length >= 2 }
        for (category in availableCategories) {
            val categoryKey = category.title
            val displayName = normalize(StringMapper.getCategoryName(categoryKey, isPersian))
            val categoryWords = displayName.split(Regex("\\s+")).filter { it.length >= 2 }

            for (tWord in titleWords) {
                for (cWord in categoryWords) {
                    if (tWord == cWord || tWord.contains(cWord) || cWord.contains(tWord)) {
                        val current = scores.getOrDefault(categoryKey, 0)
                        scores[categoryKey] = current + minOf(tWord.length, cWord.length)
                    }
                }
            }
        }

        return scores.maxByOrNull { it.value }?.key
    }

    /**
     * نسخه سازگار با امضای قبلی (فقط لیست کلیدها).
     * اگر جایی هنوز با این امضا صدا زده شود، کار می‌کند.
     */
    fun suggestCategory(title: String, availableCategoryKeys: List<String>): String? {
        val fakeList = availableCategoryKeys.map { key ->
            CategoryEntity(title = key, isExpense = true)
        }
        return suggestCategory(title, fakeList, isPersian = true)
    }

    private fun normalize(text: String): String {
        return text.trim()
            .lowercase()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("‌", "") // نیم‌فاصله
            .replace(Regex("\\s+"), " ")
    }
}
