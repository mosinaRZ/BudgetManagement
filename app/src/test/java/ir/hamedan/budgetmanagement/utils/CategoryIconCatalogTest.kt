package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryIconCatalogTest {

    @Test
    fun `catalog has enough unique icons for the sphere`() {
        val icons = CategoryIconCatalog.icons
        assertThat(icons.size).isAtLeast(100)
        assertThat(icons.toSet()).hasSize(icons.size)
        assertThat(icons.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `default category icon of the old dialog is still available`() {
        assertThat(CategoryIconCatalog.icons).contains("🏷️")
    }

    @Test
    fun `withExtra appends only icons that are missing`() {
        assertThat(CategoryIconCatalog.withExtra(null)).isSameInstanceAs(CategoryIconCatalog.icons)
        assertThat(CategoryIconCatalog.withExtra("")).isSameInstanceAs(CategoryIconCatalog.icons)
        assertThat(CategoryIconCatalog.withExtra("🍕")).isSameInstanceAs(CategoryIconCatalog.icons)

        val extended = CategoryIconCatalog.withExtra("🦄")
        assertThat(extended).hasSize(CategoryIconCatalog.icons.size + 1)
        assertThat(extended.last()).isEqualTo("🦄")
    }

    @Test
    fun `every suggestion icon can be shown on the sphere`() {
        (CategoryTitleSuggestions.expense + CategoryTitleSuggestions.income).forEach {
            assertThat(CategoryIconCatalog.icons).contains(it.emoji)
        }
    }
}