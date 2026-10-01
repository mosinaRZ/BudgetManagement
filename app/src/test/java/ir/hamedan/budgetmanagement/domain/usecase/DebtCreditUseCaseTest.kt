package ir.hamedan.budgetmanagement.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DebtCreditUseCaseTest {
    private val debtRepo = mockk<DebtCreditRepository>(relaxed = true)
    private val txRepo = mockk<TransactionRepository>(relaxed = true)
    private val categoryRepo = mockk<CategoryRepository>(relaxed = true)

    private fun useCase(): DebtCreditUseCase {
        every { categoryRepo.getAllCategories() } returns flowOf(
            listOf(CategoryEntity(id = "debt", title = "DEBT_CREDIT_PAYABLE", isExpense = true))
        )
        return DebtCreditUseCase(debtRepo, txRepo, categoryRepo)
    }

    @Test fun calculateExpirationDate_usesCeilingNumberOfInstallments() {
        val result = useCase().calculateExpirationDate(100.0, 30.0, 15)
        assertThat(result).isGreaterThan(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveOrUpdate_rejectsInvalidDueDay() = runTest {
        useCase().saveOrUpdate(
            id = null, type = "DEBT", personName = "Ali", totalAmount = 100.0,
            isMonthly = true, monthlyAmount = 30.0, dueDay = 0,
            oneTimeDueDateMillis = 0L, note = null, addToBalance = false
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun deposit_rejectsZero() = runTest {
        useCase().deposit(
            ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity(
                type = "DEBT", personName = "Ali", totalAmount = 100L
            ), 0L
        )
    }
}