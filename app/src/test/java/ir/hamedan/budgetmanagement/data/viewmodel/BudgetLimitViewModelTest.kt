package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitUiModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.ui.screens.budget.endOfDayMillis
import ir.hamedan.budgetmanagement.ui.screens.budget.findOverlappingLimit
import ir.hamedan.budgetmanagement.ui.screens.budget.startOfDayMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BudgetLimitViewModelTest {
    private val now = System.currentTimeMillis()
    private val category = CategoryEntity(id="food", title="FOOD", isExpense=true)

    @Test fun budgetLimits_computeSpentOnlyInsideActivePeriod() = runTest {
        val limits = MutableStateFlow(
            listOf(BudgetLimitEntity(id="limit", categoryId="food", maxLimit=2_000L, startDate=now-1_000L, endDate=now+1_000L))
        )
        val transactions = MutableStateFlow(
            listOf(
                TransactionEntity(id="in", amount=1_500L, categoryId="food", type="EXPENSE", timestamp=now),
                TransactionEntity(id="out", amount=9_000L, categoryId="food", type="EXPENSE", timestamp=now+2_000L)
            )
        )
        val repo = mockk<BudgetLimitRepository>(relaxed=true)
        val catRepo = mockk<CategoryRepository>(relaxed=true)
        val txRepo = mockk<TransactionRepository>(relaxed=true)
        val notificationRepo = mockk<NotificationRepository>(relaxed=true)
        every { repo.getAllLimits() } returns limits
        every { txRepo.getAllTransactions() } returns transactions
        every { catRepo.getCategoriesByExpenseStatus(true) } returns flowOf(listOf(category))

        val vm = BudgetLimitViewModel(
            repo,
            catRepo,
            txRepo,
            notificationRepo,
            mockk<Context>(relaxed = true)
        )
        val result = vm.budgetLimitsWithSpent.first { it.isNotEmpty() }
        assertThat(result).hasSize(1)
        assertThat(result.single().currentSpent).isEqualTo(1_500L.toDouble())
    }

    @Test fun budgetLimitUiModel_marksExpiredByEndDate() {
        val entity = BudgetLimitEntity(id="x", categoryId="food", maxLimit=100L, startDate=0L, endDate=1L)
        val ui = BudgetLimitUiModel(entity, 0.0)
        assertThat(ui.isExpired).isTrue()
        assertThat(ui.isActive).isFalse()
    }

    @Test fun endOfDayMillis_isIdempotent_soResavingALimitDoesNotShiftTheEndDate() {
        val endOfDay = endOfDayMillis(now)
        assertThat(endOfDay).isGreaterThan(now - 1L)
        assertThat(endOfDayMillis(endOfDay)).isEqualTo(endOfDay)
        assertThat(endOfDayMillis(startOfDayMillis(now))).isEqualTo(endOfDay)
    }

    @Test fun findOverlappingLimit_detectsOverlapOnlyForSameCategory_andIgnoresTheLimitBeingEdited() {
        val day = 24L * 60 * 60 * 1000
        val existing = BudgetLimitEntity(id = "a", categoryId = "food", maxLimit = 100L, startDate = 10 * day, endDate = 20 * day)
        val limits = listOf(existing)

        assertThat(findOverlappingLimit(limits, "food", 15 * day, 25 * day)).isEqualTo(existing)
        assertThat(findOverlappingLimit(limits, "food", 20 * day, 30 * day)).isEqualTo(existing) // touching edge overlaps
        assertThat(findOverlappingLimit(limits, "food", 21 * day, 30 * day)).isNull()
        assertThat(findOverlappingLimit(limits, "transport", 15 * day, 25 * day)).isNull()
        assertThat(findOverlappingLimit(limits, "food", 15 * day, 25 * day, excludeId = "a")).isNull()
    }
}