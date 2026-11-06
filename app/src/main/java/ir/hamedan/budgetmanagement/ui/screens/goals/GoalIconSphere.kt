package ir.hamedan.budgetmanagement.ui.screens.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ir.hamedan.budgetmanagement.ui.screens.categories.sphere.IconSphere
import ir.hamedan.budgetmanagement.ui.screens.categories.sphere.IconSphereLabels

// -----------------------------------------------------------------------------
// کاتالوگ آیکون‌های قلک
// -----------------------------------------------------------------------------

@Immutable
data class GoalIconOption(
    val emoji: String,
    val fa: String,
    val en: String
)

/** مقایسه‌ی ایموجی‌ها بدون وابستگی به variation selector (U+FE0F)؛ «✈️» و «✈» یکی حساب می‌شوند. */
internal fun String.emojiKey(): String = replace("\uFE0F", "")

internal fun sameEmoji(a: String?, b: String?): Boolean =
    a != null && b != null && a.isNotBlank() && a.emojiKey() == b.emojiKey()

/**
 * آیکون‌هایی که روی کره قرار می‌گیرند. همه‌ی ایموجی‌های قدیمیِ قلک (🎯 🚗 🏠 💻 📱 ✈️ 💍 🎓 🎮 🎁 🛵 🪙)
 * در این فهرست هست تا قلک‌های ساخته‌شده‌ی قبلی هنگام ویرایش همچنان روی کره پیدا شوند.
 */
val GOAL_ICON_OPTIONS: List<GoalIconOption> = listOf(
    GoalIconOption("🎯", "هدف", "Goal"),
    GoalIconOption("🚗", "خودرو", "Car"),
    GoalIconOption("🏠", "خانه", "Home"),
    GoalIconOption("🏡", "ویلا", "Villa"),
    GoalIconOption("💍", "ازدواج", "Wedding"),
    GoalIconOption("✈️", "سفر", "Travel"),
    GoalIconOption("🧳", "مسافرت", "Luggage"),
    GoalIconOption("🕋", "زیارت", "Pilgrimage"),
    GoalIconOption("🏖️", "تعطیلات", "Vacation"),
    GoalIconOption("🎓", "تحصیل", "Education"),
    GoalIconOption("📚", "کتاب و دوره", "Books & courses"),
    GoalIconOption("💻", "لپ‌تاپ", "Laptop"),
    GoalIconOption("🖥️", "کامپیوتر", "Computer"),
    GoalIconOption("📱", "گوشی", "Phone"),
    GoalIconOption("⌚", "ساعت", "Watch"),
    GoalIconOption("🎧", "هدفون", "Headphones"),
    GoalIconOption("📷", "دوربین", "Camera"),
    GoalIconOption("🎮", "گیمینگ", "Gaming"),
    GoalIconOption("🎸", "موسیقی", "Music"),
    GoalIconOption("🎨", "هنر", "Art"),
    GoalIconOption("🎁", "هدیه", "Gift"),
    GoalIconOption("🎂", "جشن", "Celebration"),
    GoalIconOption("🛵", "موتور", "Scooter"),
    GoalIconOption("🚲", "دوچرخه", "Bicycle"),
    GoalIconOption("⛺", "کمپینگ", "Camping"),
    GoalIconOption("🏋️", "ورزش", "Fitness"),
    GoalIconOption("🛋️", "مبلمان", "Furniture"),
    GoalIconOption("🍽️", "آشپزخانه", "Kitchen"),
    GoalIconOption("🧺", "لوازم خانگی", "Appliances"),
    GoalIconOption("🛠️", "بازسازی", "Renovation"),
    GoalIconOption("👶", "فرزند", "Baby"),
    GoalIconOption("🐶", "حیوان خانگی", "Pet"),
    GoalIconOption("👗", "پوشاک", "Fashion"),
    GoalIconOption("💎", "جواهر", "Jewelry"),
    GoalIconOption("🪙", "طلا و سکه", "Gold & coins"),
    GoalIconOption("💰", "پس‌انداز", "Savings"),
    GoalIconOption("🏦", "صندوق اضطراری", "Emergency fund"),
    GoalIconOption("🏥", "درمان", "Health"),
    GoalIconOption("💼", "کسب‌وکار", "Business"),
    GoalIconOption("🚀", "استارتاپ", "Startup"),
    GoalIconOption("🛡️", "بیمه", "Insurance")
)

internal fun GoalIconOption.label(isPersian: Boolean): String = if (isPersian) fa else en

// -----------------------------------------------------------------------------
// کره‌ی انتخاب آیکون
// -----------------------------------------------------------------------------

/**
 * کره‌ی سه‌بعدیِ انتخاب آیکونِ قلک. دقیقاً همان کره‌ی انتخاب آیکونِ دسته‌بندی ([IconSphere]) است
 * (همان ظاهر، چرخش، اینرسی و رفتار انتخاب)؛ فقط فهرست آیکون‌ها فرق می‌کند.
 */
@Composable
fun GoalIconSphere(
    options: List<GoalIconOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    accent: Color,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val icons = remember(options) { options.map { it.emoji } }
    // «✈️» و «✈» یکی حساب می‌شوند؛ انتخاب به همان ایموجیِ موجود در فهرست نگاشت می‌شود.
    val selectedIcon = remember(options, selected) {
        options.firstOrNull { sameEmoji(it.emoji, selected) }?.emoji
    }
    val labels = remember(isPersian) {
        IconSphereLabels(
            description = if (isPersian) {
                "کره‌ی آیکون‌ها؛ برای چرخاندن بکشید و برای انتخاب لمس کنید"
            } else {
                "Icon sphere: drag to rotate, tap to choose"
            },
            next = if (isPersian) "آیکون بعدی" else "Next icon",
            previous = if (isPersian) "آیکون قبلی" else "Previous icon"
        )
    }

    IconSphere(
        icons = icons,
        selected = selectedIcon,
        accent = accent,
        labels = labels,
        onSelect = onSelect,
        modifier = modifier
    )
}