package ir.hamedan.budgetmanagement.ui.screens.home

/**
 * انتخاب تصادفی «بدون تکرار» (Shuffle Bag).
 *
 * - هر بار یک جمله‌ی تصادفی از میان جمله‌های باقی‌مانده انتخاب و از کیسه حذف می‌شود.
 * - وقتی کیسه خالی شد، دوباره با همه‌ی جمله‌ها پر می‌شود؛ پس هر جمله دقیقاً یک‌بار در هر دور می‌آید.
 * - در دور جدید، تا جایی که جمله‌ی جایگزین وجود دارد، جمله‌هایی که تازه نمایش داده شده‌اند
 *   (یا هنوز در پشته دیده می‌شوند) دوباره انتخاب نمی‌شوند.
 * - با seed یکسان، دنباله‌ی خروجی کاملاً قابل بازسازی است (برای حفظ وضعیت هنگام چرخش صفحه).
 */
internal class QuoteShuffler(
    private val size: Int,
    seed: Long,
    private val recentMemory: Int
) {
    private val random = kotlin.random.Random(seed)
    private val remaining = ArrayList<Int>(size)
    private val recent = ArrayDeque<Int>()

    fun next(): Int {
        if (remaining.isEmpty()) {
            for (i in 0 until size) remaining.add(i)
        }
        val fresh = remaining.filter { it !in recent }
        val pool = if (fresh.isNotEmpty()) fresh else remaining
        val pick = pool[random.nextInt(pool.size)]
        remaining.remove(pick)

        recent.addLast(pick)
        if (recent.size > recentMemory) recent.removeFirst()
        return pick
    }
}