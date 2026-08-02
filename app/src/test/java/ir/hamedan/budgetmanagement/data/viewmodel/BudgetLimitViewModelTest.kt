package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
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
    private val start = now - 7L * 24 * 60 * 60 * 1000
    private val end = now + 7L * 24 * 60 * 60 * 1000

    private val foodCategory = CategoryEntity(
        id = "c1",
        title = "FOOD",
        iconEmoji = "🍕",
        isExpense = true
    )

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
        every { categoryRepository.getCategoriesByExpenseStatus(true) } returns flowOf(listOf(foodCategory))

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        limitsFlow.value = emptyList()
        transactionsFlow.value = emptyList()

        viewModel = BudgetLimitViewModel(
            budgetLimitRepository = budgetLimitRepository,
            categoryRepository = categoryRepository,
            transactionRepository = transactionRepository,
            notificationRepository = notificationRepository,
            context = context
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
        backgroundScope.launch(testDispatcher) {
            viewModel.budgetLimitsWithSpent.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull { it.isNotEmpty() } ?: values.lastOrNull() ?: emptyList()
    }

    // -------------------------------------------------------------------------
    // Spent calculation
    // -------------------------------------------------------------------------

    @Test
    fun calculatesCurrentSpent_onlyMatchingExpenseInDateRange() = runTest(testDispatcher) {
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
            TransactionEntity(id = "1", title = "a", amount = 1_000_000.0, type = "EXPENSE", category = "FOOD", timestamp = now),
            TransactionEntity(id = "2", title = "b", amount = 500_000.0, type = "EXPENSE", category = "FOOD", timestamp = now),
            TransactionEntity(id = "3", title = "c", amount = 9_000_000.0, type = "EXPENSE", category = "FOOD", timestamp = start - 1000),
            TransactionEntity(id = "4", title = "d", amount = 2_000_000.0, type = "EXPENSE", category = "TRANSPORT", timestamp = now),
            TransactionEntity(id = "5", title = "e", amount = 3_000_000.0, type = "INCOME", category = "FOOD", timestamp = now)
        )

        val result = collectLastLimits()
        assertThat(result).hasSize(1)
        assertThat(result[0].currentSpent).isEqualTo(1_500_000.0)
        assertThat(result[0].isSuccessful).isTrue()
        assertThat(result[0].categoryEmoji).isAnyOf("🍕", "💰")    }

    @Test
    fun inactiveLimit_hasZeroSpent() = runTest(testDispatcher) {
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
            TransactionEntity(id = "1", title = "x", amount = 900_000.0, type = "EXPENSE", category = "FOOD", timestamp = now)
        )

        val result = collectLastLimits()
        assertThat(result).hasSize(1)
        assertThat(result[0].currentSpent).isEqualTo(0.0)
    }

    @Test
    fun isSuccessful_false_whenOverLimit() = runTest(testDispatcher) {
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
            TransactionEntity(id = "1", title = "x", amount = 1_500_000.0, type = "EXPENSE", category = "FOOD", timestamp = now)
        )

        val result = collectLastLimits()
        assertThat(result[0].isSuccessful).isFalse()
        assertThat(result[0].currentSpent).isEqualTo(1_500_000.0)
    }

    @Test
    fun overLimit_sendsBudgetProgressNotificationTags() = runTest(testDispatcher) {
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
            TransactionEntity(id = "1", title = "x", amount = 1_000_000.0, type = "EXPENSE", category = "FOOD", timestamp = now)
        )

        collectLastLimits()
        advanceUntilIdle()

        // 100% → tag BUDGET_100_FOOD (and also 50 / 80 because percent >= those too)
        verify(timeout = 3_000, atLeast = 1) {
            NotificationHelper.send(
                context = context,
                type = any(),
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = "BUDGET_100_FOOD"
            )
        }
    }

    // -------------------------------------------------------------------------
    // save / delete / status
    // -------------------------------------------------------------------------

    @Test
    fun saveBudgetLimit_callsRepository_andAdjustsEndDateToEndOfDay() = runTest(testDispatcher) {
        coEvery { budgetLimitRepository.saveLimit(any()) } just Runs
        collectLastLimits() // subscribe so budgetLimitsWithSpent is active

        val rawEnd = end
        viewModel.saveBudgetLimit(
            categoryName = "FOOD",
            maxLimit = 2_000_000.0,
            startDate = start,
            endDate = rawEnd
        )
        advanceUntilIdle()

        val expectedEnd = rawEnd + (24 * 60 * 60 * 1000L - 1)

        coVerify {
            budgetLimitRepository.saveLimit(
                match {
                    it.categoryName == "FOOD" &&
                            it.maxLimit == 2_000_000.0 &&
                            it.startDate == start &&
                            it.endDate == expectedEnd &&
                            it.isActive
                }
            )
        }
    }

    @Test
    fun saveBudgetLimit_reusesExistingId_whenSameCategoryExists() = runTest(testDispatcher) {
        coEvery { budgetLimitRepository.saveLimit(any()) } just Runs
        limitsFlow.value = listOf(
            BudgetLimitEntity(
                id = 99L,
                categoryName = "FOOD",
                maxLimit = 1_000_000.0,
                isActive = true,
                startDate = start,
                endDate = end
            )
        )
        collectLastLimits()

        viewModel.saveBudgetLimit("FOOD", 3_000_000.0, start, end)
        advanceUntilIdle()

        coVerify {
            budgetLimitRepository.saveLimit(match { it.id == 99L && it.maxLimit == 3_000_000.0 })
        }
    }

    @Test
    fun deleteBudgetLimit_callsRepository() = runTest(testDispatcher) {
        coEvery { budgetLimitRepository.deleteLimit(any()) } just Runs
        limitsFlow.value = listOf(
            BudgetLimitEntity(
                id = 42L,
                categoryName = "FOOD",
                maxLimit = 1.0,
                isActive = true,
                startDate = start,
                endDate = end
            )
        )
        collectLastLimits()

        viewModel.deleteBudgetLimit(42L)
        advanceUntilIdle()

        coVerify { budgetLimitRepository.deleteLimit(42L) }
    }

    @Test
    fun updateLimitStatus_savesCopiedEntityWithNewActiveFlag() = runTest(testDispatcher) {
        coEvery { budgetLimitRepository.saveLimit(any()) } just Runs
        limitsFlow.value = listOf(
            BudgetLimitEntity(
                id = 7L,
                categoryName = "FOOD",
                maxLimit = 1_000_000.0,
                isActive = true,
                startDate = start,
                endDate = end
            )
        )
        collectLastLimits()

        viewModel.updateLimitStatus(id = 7L, isActive = false)
        advanceUntilIdle()

        coVerify {
            budgetLimitRepository.saveLimit(
                match { it.id == 7L && !it.isActive && it.categoryName == "FOOD" }
            )
        }
    }
}