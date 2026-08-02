package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SavingGoalRepository
    private lateinit var context: Context
    private lateinit var viewModel: SavingGoalsViewModel

    private val sampleGoal = SavingGoalEntity(
        id = "g1",
        title = "سفر",
        targetAmount = 50_000_000.0,
        currentAmount = 10_000_000.0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { repository.getAllGoals() } returns flowOf(listOf(sampleGoal))

        // 🔥 خیلی مهم: NotificationHelper را کامل mock می‌کنیم
        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(
                context = any(),
                type = any(),
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = any()
            )
        } just Runs

        viewModel = SavingGoalsViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun deposit_callsDepositToGoal() = runTest {
        coEvery { repository.depositToGoal(any(), any()) } just Runs

        viewModel.deposit("g1", 5_000_000.0)
        advanceUntilIdle()

        coVerify { repository.depositToGoal("g1", 5_000_000.0) }
    }

    @Test
    fun withdraw_callsWithdrawFromGoal() = runTest {
        coEvery { repository.withdrawFromGoal(any(), any()) } just Runs

        viewModel.withdraw("g1", 2_000_000.0)
        advanceUntilIdle()

        coVerify { repository.withdrawFromGoal("g1", 2_000_000.0) }
    }

    @Test
    fun deleteGoal_callsRepositoryDelete() = runTest {
        coEvery { repository.deleteGoal(any()) } just Runs

        viewModel.deleteGoal(sampleGoal)
        advanceUntilIdle()

        coVerify { repository.deleteGoal(sampleGoal) }
    }
}