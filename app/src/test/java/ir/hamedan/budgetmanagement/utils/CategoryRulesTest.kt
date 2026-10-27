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

    @Test fun builtInCategoriesMatchByKeyRegardlessOfIdOrType() {
        val local = CategoryEntity(id = "a", title = "SAVING_GOAL", isExpense = true)
        val fromBackup = CategoryEntity(id = "b", title = "saving_goal", isExpense = false)
        assertThat(CategoryRules.isSameIdentity(local, fromBackup)).isTrue()
    }

    @Test fun userCategoriesMatchByTitleAndType() {
        val a = CategoryEntity(id = "a", title = "Coffee", isExpense = true)
        assertThat(CategoryRules.isSameIdentity(a, CategoryEntity(id = "b", title = " coffee ", isExpense = true))).isTrue()
        assertThat(CategoryRules.isSameIdentity(a, CategoryEntity(id = "c", title = "Coffee", isExpense = false))).isFalse()
        assertThat(CategoryRules.isSameIdentity(a, CategoryEntity(id = "d", title = "FOOD"))).isFalse()
    }

    @Test fun canonicalPrefersStableIdThenOldest() {
        val stable = CategoryRules.stableDefaultId("SAVING_GOAL")
        val old = CategoryEntity(id = "x", title = "SAVING_GOAL", createdAt = 1L)
        val stableCopy = CategoryEntity(id = stable, title = "SAVING_GOAL", createdAt = 99L)
        assertThat(CategoryRules.pickCanonical(listOf(old, stableCopy)).id).isEqualTo(stable)
        val older = CategoryEntity(id = "y", title = "SAVING_GOAL", createdAt = 0L)
        assertThat(CategoryRules.pickCanonical(listOf(old, older)).id).isEqualTo("y")
    }
}