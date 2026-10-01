package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import org.junit.Test

class CategorySuggestionHelperTest {
    private val categories = listOf(
        CategoryEntity(id = "food", title = "FOOD", isExpense = true),
        CategoryEntity(id = "shopping", title = "SHOPPING", isExpense = true),
        CategoryEntity(id = "custom", title = "ورزش", isExpense = true)
    )

    @Test fun suggestsSystemCategoryFromPersianKeyword() {
        assertThat(CategorySuggestionHelper.suggestCategory("خرید پیتزا", categories)).isEqualTo("FOOD")
    }

    @Test fun suggestsCustomCategoryFromDisplayName() {
        assertThat(CategorySuggestionHelper.suggestCategory("کلاس ورزش", categories)).isEqualTo("ورزش")
    }

    @Test fun ignoresUnavailableSystemCategory() {
        val onlyShopping = categories.filter { it.title == "SHOPPING" }
        assertThat(CategorySuggestionHelper.suggestCategory("پیتزا", onlyShopping)).isNull()
    }

    @Test fun blankTitleReturnsNull() {
        assertThat(CategorySuggestionHelper.suggestCategory("   ", categories)).isNull()
    }
}