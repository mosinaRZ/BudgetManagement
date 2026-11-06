package ir.hamedan.budgetmanagement.ui.screens.goal

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.ui.screens.goals.GOAL_ICON_OPTIONS
import ir.hamedan.budgetmanagement.ui.screens.goals.emojiKey
import ir.hamedan.budgetmanagement.ui.screens.goals.sameEmoji
import org.junit.Test

/** کاتالوگ آیکون‌های قلک و مقایسه‌ی ایموجی‌ها (ریاضی و چرخش کره در SphereMathTest تست می‌شود). */
class GoalIconSphereTest {

    @Test fun catalogHasNoDuplicateEmojis() {
        val keys = GOAL_ICON_OPTIONS.map { it.emoji.emojiKey() }
        assertThat(keys.toSet()).hasSize(keys.size)
    }

    @Test fun emojiComparisonIgnoresVariationSelector() {
        assertThat(sameEmoji("✈️", "✈")).isTrue()
        assertThat(sameEmoji("🚗", "🏠")).isFalse()
        assertThat(sameEmoji("", "")).isFalse()
        assertThat(sameEmoji(null, "🚗")).isFalse()
    }
}