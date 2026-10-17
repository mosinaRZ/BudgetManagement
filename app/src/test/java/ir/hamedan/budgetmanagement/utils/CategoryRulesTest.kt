package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import org.junit.Test

class CategoryRulesTest {

    @Test fun savingGoalIsNotUserSelectable() {
        assertThat(CategoryRules.isUserSelectable("SAVING_GOAL")).isFalse()
        assertThat(CategoryRules.isUserSelectable(" saving_goal ")).isFalse()
    }

    @Test fun otherCategoriesRemainSelectable() {
        listOf("FOOD", "SALARY", "DEBT_CREDIT_PAYABLE", "UNCATEGORIZED", "قلک‌های من")
            .forEach { assertThat(CategoryRules.isUserSelectable(it)).isTrue() }
    }

    @Test fun userSelectableRemovesOnlySavingGoal() {
        val all = listOf(
            CategoryEntity(title = "FOOD"),
            CategoryEntity(title = "SAVING_GOAL", isSystem = true),
            CategoryEntity(title = "SALARY", isExpense = false)
        )
        assertThat(CategoryRules.userSelectable(all).map { it.title }).containsExactly("FOOD", "SALARY").inOrder()
    }
}