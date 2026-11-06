package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import org.junit.Test

class CategoryNameRulesTest {

    private fun category(id: String, title: String) = CategoryEntity(id = id, title = title)

    private val existing = listOf(
        category("1", "ورزش"),
        category("2", "Pets"),
        category("3", "FOOD"),
        category("4", "SHOPPING")
    )

    @Test
    fun `empty candidate is never a duplicate (emptiness has its own error)`() {
        assertThat(CategoryNameRules.isDuplicate("  ", existing, null, true)).isFalse()
    }

    @Test
    fun `comparison ignores case, extra spaces and zero width joiners`() {
        assertThat(CategoryNameRules.isDuplicate("  pets ", existing, null, false)).isTrue()
        assertThat(CategoryNameRules.isDuplicate("PETS", existing, null, false)).isTrue()
        val withZwnj = listOf(category("9", "کمک‌هزینه"))
        assertThat(CategoryNameRules.isDuplicate("کمک هزینه", withZwnj, null, true)).isFalse() // فاصله ≠ نیم‌فاصله
        assertThat(CategoryNameRules.isDuplicate("کمکهزینه", withZwnj, null, true)).isTrue()
    }

    @Test
    fun `arabic yeh and kaf are treated like persian ones`() {
        val persian = listOf(category("9", "کیف پول"))
        assertThat(CategoryNameRules.isDuplicate("كيف پول", persian, null, true)).isTrue()
    }

    @Test
    fun `editing a category does not collide with itself`() {
        assertThat(CategoryNameRules.isDuplicate("ورزش", existing, editingId = "1", isPersian = true)).isFalse()
        assertThat(CategoryNameRules.isDuplicate("ورزش", existing, editingId = "2", isPersian = true)).isTrue()
    }

    @Test
    fun `display name of a built-in category counts as taken`() {
        assertThat(CategoryNameRules.isDuplicate("خرید", existing, null, isPersian = true)).isTrue()
        assertThat(CategoryNameRules.isDuplicate("Shopping", existing, null, isPersian = false)).isTrue()
        // در انگلیسی نام «خرید» به‌خودیِ خود تکراری نیست
        assertThat(CategoryNameRules.isDuplicate("خرید", existing, null, isPersian = false)).isFalse()
    }

    @Test
    fun `reserved built-in keys are always rejected`() {
        assertThat(CategoryNameRules.isDuplicate("salary", emptyList(), null, true)).isTrue()
        assertThat(CategoryNameRules.isDuplicate("Saving_Goal", emptyList(), null, false)).isTrue()
    }

    @Test
    fun `a fresh name is accepted`() {
        assertThat(CategoryNameRules.isDuplicate("دانشگاه", existing, null, true)).isFalse()
    }
}