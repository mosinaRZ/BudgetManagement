package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitUiModel
import ir.hamedan.budgetmanagement.ui.screens.budget.BudgetLimitViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetLimitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var budgetLimitRepository: BudgetLimitRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var context: Context
    private lateinit var viewModel: BudgetLimitViewModel

    private val limitsFlow = MutableStateFlow<List<BudgetLimitEntity>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    private val now = System.currentTimeMillis()
    private val start = now - 7 * 24 * 60 * 60 * 1000L
    private val end = now + 7 * 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        budgetLimitRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { budgetLimitRepository.getAllLimits() } returns limitsFlow
        every { transactionRepository.getAllTransactions() } returns transactionsFlow
        every { categoryRepository.getCategoriesByExpenseStatus(true) } returns flowOf(
            listOf(CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true))
        )

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        viewModel = BudgetLimitViewModel(
            budgetLimitRepository,
            categoryRepository,
            transactionRepository,
            notificationRepository,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    private fun TestScope.collectLastLimits(): List<BudgetLimitUiModel> {
        val values = mutableListOf<List<BudgetLimitUiModel>>()
        backgroundScope.launch {
            viewModel.budgetLimitsWithSpent.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull() ?: emptyList()
    }

    @Test
    fun calculatesCurrentSpent_onlyMatchingExpenseInDateRange() = runTest {
        limitsFlow.value = listOf(
            BudgetLimitEntity(
                id = 1L,
                categoryName = "FOOD",
                maxLimit = 5_000_000.0,
                isActive = true,
                startDate = start,
                endDate = end
            )
        )
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 1_000_000.0, type = "EXPENSE", category = "FOOD", timestamp = now),
            TransactionEntity(id = "2", amount = 500_000.0, type = "EXPENSE", category = "FOOD", timestamp = now),
            // خارج از بازه
            TransactionEntity(id = "3", amount = 9_000_000.0, type = "EXPENSE", category = "FOOD", timestamp = start - 1000),
            // دسته دیگر
            TransactionEntity(id = "4", amount = 2_000_000.0, type = "EXPENSE", category = "TRANSPORT", timestamp = now),
            // درآمد نباید حساب شود
            TransactionEntity(id = "5", amount = 3_000_000.0, type = "INCOME", category = "FOOD", timestamp = now)
        )

        val result = collectLastLimits()
        assertThat(result).hasSize(1)
        assertThat(result[0].currentSpent).isEqualTo(1_500_000.0)
        assertThat(result[0].isSuccessful).isTrue()
        assertThat(result[0].categoryEmoji).isAnyOf("🍕", "💰")
    }

    @Test
    fun inactiveLimit_hasZeroSpent() = runTest {
        limitsFlow.value = listOf(
            BudgetLimitEntity(
                id = 1L,
                categoryName = "FOOD",
                maxLimit = 1_000_000.0,
                isActive = false,
                startDate = start,
                endDate = end
            )
        )
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 900_000.0, type = "EXPENSE", category = "FOOD", timestamp = now)
        )

        val result = collectLastLimits()
        assertThat(result[0].currentSpent).isEqualTo(0.0)
    }

    @Test
    fun isSuccessful_false_whenOverLimit() = runTest {
        limitsFlow.value = listOf(
            BudgetLimitEntity(
                id = 1L,
                categoryName = "FOOD",
                maxLimit = 1_000_000.0,
                isActive = true,
                startDate = start,
                endDate = end
            )
        )
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 1_500_000.0, type = "EXPENSE", category = "FOOD", timestamp = now)
        )

        val result = collectLastLimits()
        assertThat(result[0].isSuccessful).isFalse()
        assertThat(result[0].currentSpent).isEqualTo(1_500_000.0)
    }

    @Test
    fun saveBudgetLimit_callsRepository() = runTest {
        coEvery { budgetLimitRepository.saveLimit(any()) } just Runs

        viewModel.saveBudgetLimit(
            categoryName = "FOOD",
            maxLimit = 2_000_000.0,
            startDate = start,
            endDate = end
        )
        advanceUntilIdle()

        coVerify {
            budgetLimitRepository.saveLimit(
                match {
                    it.categoryName == "FOOD" &&
                            it.maxLimit == 2_000_000.0
                }
            )
        }
    }

    @Test
    fun deleteBudgetLimit_callsRepository() = runTest {
        coEvery { budgetLimitRepository.deleteLimit(any()) } just Runs

        viewModel.deleteBudgetLimit(42L)
        advanceUntilIdle()

        coVerify { budgetLimitRepository.deleteLimit(42L) }
    }
}