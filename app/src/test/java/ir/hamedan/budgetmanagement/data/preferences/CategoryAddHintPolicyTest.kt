package ir.hamedan.budgetmanagement.data.preferences

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryAddHintPolicyTest {

    @Test
    fun `first hint comes quickly and later ones are spaced out`() {
        assertThat(CategoryAddHintPolicy.delayBeforeNext(0)).isEqualTo(CategoryAddHintPolicy.FIRST_DELAY_MS)
        assertThat(CategoryAddHintPolicy.delayBeforeNext(1)).isEqualTo(CategoryAddHintPolicy.REPEAT_INTERVAL_MS)
        assertThat(CategoryAddHintPolicy.FIRST_DELAY_MS).isLessThan(CategoryAddHintPolicy.REPEAT_INTERVAL_MS)
        // نکته باید قبل از آمدن بعدی جمع شده باشد
        assertThat(CategoryAddHintPolicy.VISIBLE_MS).isLessThan(CategoryAddHintPolicy.REPEAT_INTERVAL_MS)
    }

    @Test
    fun `a new visit shows the hint for a user who has not learned yet`() {
        assertThat(CategoryAddHintPolicy.shouldShow(shownThisVisit = 0, totalShown = 0, fabUses = 0)).isTrue()
    }

    @Test
    fun `hint count per visit is capped`() {
        val max = CategoryAddHintPolicy.MAX_PER_VISIT
        assertThat(CategoryAddHintPolicy.shouldShow(max - 1, 0, 0)).isTrue()
        assertThat(CategoryAddHintPolicy.shouldShow(max, 0, 0)).isFalse()
    }

    @Test
    fun `lifetime count is capped`() {
        val max = CategoryAddHintPolicy.MAX_TOTAL
        assertThat(CategoryAddHintPolicy.shouldShow(0, max - 1, 0)).isTrue()
        assertThat(CategoryAddHintPolicy.shouldShow(0, max, 0)).isFalse()
    }

    @Test
    fun `hint stops once the user has used the button enough times`() {
        val learned = CategoryAddHintPolicy.LEARNED_AFTER_USES
        assertThat(CategoryAddHintPolicy.shouldShow(0, 0, learned - 1)).isTrue()
        assertThat(CategoryAddHintPolicy.shouldShow(0, 0, learned)).isFalse()
    }
}