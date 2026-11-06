package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import org.junit.Test

class CategoryTitleSuggestionsTest {

    private fun category(title: String, expense: Boolean = true) =
        CategoryEntity(id = "id-$title", title = title, isExpense = expense)

    @Test
    fun `sports and university are suggested for expenses`() {
        val fa = CategoryTitleSuggestions.forType(isExpense = true, isPersian = true, existing = emptyList())
            .map { it.title(true) }
        assertThat(fa).containsAtLeast("ورزش", "دانشگاه")

        val en = CategoryTitleSuggestions.forType(isExpense = true, isPersian = false, existing = emptyList())
            .map { it.title(false) }
        assertThat(en).containsAtLeast("Sports", "University")
    }

    @Test
    fun `income and expense lists are different`() {
        val income = CategoryTitleSuggestions.forType(false, true, emptyList()).map { it.titleFa }
        assertThat(income).contains("فریلنس")
        assertThat(income).doesNotContain("ورزش")
    }

    @Test
    fun `titles fit the title limit and are unique in both languages`() {
        val all = CategoryTitleSuggestions.expense + CategoryTitleSuggestions.income
        all.forEach {
            assertThat(it.titleFa.length).isAtMost(CategoryNameRules.MAX_TITLE_LENGTH)
            assertThat(it.titleEn.length).isAtMost(CategoryNameRules.MAX_TITLE_LENGTH)
        }
        assertThat(CategoryTitleSuggestions.expense.map { it.titleFa }.toSet())
            .hasSize(CategoryTitleSuggestions.expense.size)
        assertThat(CategoryTitleSuggestions.expense.map { it.titleEn }.toSet())
            .hasSize(CategoryTitleSuggestions.expense.size)
    }

    @Test
    fun `existing categories are not suggested again`() {
        val existing = listOf(category("ورزش"), category("university", expense = false))
        val fa = CategoryTitleSuggestions.forType(true, true, existing).map { it.titleFa }
        assertThat(fa).doesNotContain("ورزش")
        assertThat(fa).contains("دانشگاه")

        val en = CategoryTitleSuggestions.forType(true, false, existing).map { it.titleEn }
        assertThat(en).doesNotContain("University")
    }
}