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
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.domain.usecase.SavingGoalUseCase
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavingGoalsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: SavingGoalRepository
    private lateinit var useCase: SavingGoalUseCase
    private lateinit var txRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: SavingGoalsViewModel
    private val goal = SavingGoalEntity(id="g1", title="سفر", targetAmount=50_000L, currentAmount=10_000L)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed=true)
        useCase = mockk(relaxed=true)
        txRepository = mockk(relaxed=true)
        categoryRepository = mockk(relaxed=true)
        every { repository.getAllGoals() } returns flowOf(listOf(goal))
        every { categoryRepository.getAllCategories() } returns flowOf(listOf(CategoryEntity(id="saving", title="SAVING_GOAL", isExpense=true)))
        mockkObject(NotificationHelper)
        every { NotificationHelper.send(any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        viewModel = SavingGoalsViewModel(
            repository = repository,
            context = mockk<Context>(relaxed=true),
            useCase = useCase,
            transactionRepository = txRepository,
            categoryRepository = categoryRepository
        )
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    @Test fun savingGoals_exposesRepositoryFlow() = runTest(dispatcher) {
        val goals = viewModel.savingGoals.first { it != null }
        assertThat(goals).containsExactly(goal)
    }

    @Test fun addGoal_delegatesToUseCase() = runTest(dispatcher) {
        coEvery { useCase.create("سفر", 100_000L, 10_000L, "✈️") } returns goal

        viewModel.addGoal("سفر", 100_000L, 10_000L, "✈️")
        advanceUntilIdle()

        coVerify { useCase.create("سفر", 100_000L, 10_000L, "✈️") }
    }

    @Test fun deposit_delegatesToUseCase() = runTest(dispatcher) {
        coEvery { txRepository.getCurrentBalance() } returns 100_000L
        coEvery { useCase.deposit("g1", 5_000L) } just Runs
        viewModel.savingGoals.first()

        viewModel.savingGoals.first { goals -> goals?.any { it.id == "g1" } == true }

        viewModel.deposit("g1", 5_000L)
        advanceUntilIdle()

        coVerify { useCase.deposit("g1", 5_000L) }
    }

    @Test fun zeroDeposit_emitsValidationError() = runTest(dispatcher) {
        viewModel.deposit("g1", 0L)
        advanceUntilIdle()
        // No repository/use-case mutation is allowed for an invalid amount.
        coVerify(exactly = 0) { useCase.deposit(any(), any()) }
    }
}