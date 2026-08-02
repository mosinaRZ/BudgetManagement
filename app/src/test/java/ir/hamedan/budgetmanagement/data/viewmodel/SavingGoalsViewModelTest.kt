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
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Mutations use Dispatchers.IO → prefer coVerify/verify with timeout.
 * Progress notifications are emitted inside the goals Flow.map when thresholds are crossed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavingGoalsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: SavingGoalRepository
    private lateinit var context: Context

    private val sampleGoal = SavingGoalEntity(
        id = "g1",
        title = "سفر",
        targetAmount = 50_000_000.0,
        currentAmount = 10_000_000.0, // 20% — below 50/80/100 thresholds
        icon = "✈️"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { repository.getAllGoals() } returns flowOf(listOf(sampleGoal))

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    private fun createViewModel(
        goals: List<SavingGoalEntity> = listOf(sampleGoal)
    ): SavingGoalsViewModel {
        every { repository.getAllGoals() } returns flowOf(goals)
        return SavingGoalsViewModel(repository, context)
    }

    private fun TestScope.collectGoals(vm: SavingGoalsViewModel): List<SavingGoalEntity>? {
        val values = mutableListOf<List<SavingGoalEntity>?>()
        backgroundScope.launch(testDispatcher) {
            vm.savingGoals.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull { it != null } ?: values.lastOrNull()
    }

    // -------------------------------------------------------------------------
    // Observe
    // -------------------------------------------------------------------------

    @Test
    fun savingGoals_emitsRepositoryList() = runTest(testDispatcher) {
        val vm = createViewModel()
        val result = collectGoals(vm)
        assertThat(result).isNotNull()
        assertThat(result).hasSize(1)
        assertThat(result!![0].title).isEqualTo("سفر")
    }

    // -------------------------------------------------------------------------
    // addGoal
    // -------------------------------------------------------------------------

    @Test
    fun addGoal_insertsWithZeroCurrentAmountAndIcon() = runTest(testDispatcher) {
        coEvery { repository.insertGoal(any()) } just Runs
        val vm = createViewModel()

        vm.addGoal(title = "لپتاپ", targetAmount = 80_000_000.0, icon = "💻")

        coVerify(timeout = 3_000) {
            repository.insertGoal(
                match {
                    it.title == "لپتاپ" &&
                            it.targetAmount == 80_000_000.0 &&
                            it.currentAmount == 0.0 &&
                            it.icon == "💻"
                }
            )
        }
    }

    @Test
    fun addGoal_sendsSuccessNotification() = runTest(testDispatcher) {
        coEvery { repository.insertGoal(any()) } just Runs
        val vm = createViewModel()

        vm.addGoal(title = "لپتاپ", targetAmount = 80_000_000.0, icon = "💻")

        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("لپتاپ") },
                descEn = any(),
                tag = match { it.startsWith("GOAL_ADD_لپتاپ_") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // update / delete
    // -------------------------------------------------------------------------

    @Test
    fun updateGoal_callsRepositoryAndSendsWarning() = runTest(testDispatcher) {
        coEvery { repository.updateGoal(any()) } just Runs
        val vm = createViewModel()
        val updated = sampleGoal.copy(title = "سفر شمال", targetAmount = 60_000_000.0)

        vm.updateGoal(updated)

        coVerify(timeout = 3_000) { repository.updateGoal(updated) }
        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("سفر شمال") },
                descEn = any(),
                tag = match { it.startsWith("GOAL_UPDATE_g1_") }
            )
        }
    }

    @Test
    fun deleteGoal_callsRepositoryAndSendsErrorNotification() = runTest(testDispatcher) {
        coEvery { repository.deleteGoal(any()) } just Runs
        val vm = createViewModel()

        vm.deleteGoal(sampleGoal)

        coVerify(timeout = 3_000) { repository.deleteGoal(sampleGoal) }
        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("سفر") },
                descEn = any(),
                tag = match { it.startsWith("GOAL_DELETE_g1_") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // deposit / withdraw
    // -------------------------------------------------------------------------

    @Test
    fun deposit_callsDepositToGoalWithAmount() = runTest(testDispatcher) {
        coEvery { repository.depositToGoal(any(), any()) } just Runs
        val vm = createViewModel()
        // Populate savingGoals.value so title lookup works
        collectGoals(vm)

        vm.deposit("g1", 5_000_000.0)

        coVerify(timeout = 3_000) { repository.depositToGoal("g1", 5_000_000.0) }
    }

    @Test
    fun deposit_sendsSuccessNotificationWithGoalTitle() = runTest(testDispatcher) {
        coEvery { repository.depositToGoal(any(), any()) } just Runs
        val vm = createViewModel()
        collectGoals(vm)

        vm.deposit("g1", 5_000_000.0)

        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("سفر") && it.contains("5000000") || it.contains("5.0E6") || it.contains("5") },
                descEn = any(),
                tag = match { it.startsWith("GOAL_DEPOSIT_g1_") }
            )
        }
    }

    @Test
    fun withdraw_callsWithdrawFromGoal() = runTest(testDispatcher) {
        coEvery { repository.withdrawFromGoal(any(), any()) } just Runs
        val vm = createViewModel()
        collectGoals(vm)

        vm.withdraw("g1", 2_000_000.0)

        coVerify(timeout = 3_000) { repository.withdrawFromGoal("g1", 2_000_000.0) }
    }

    @Test
    fun withdraw_sendsWarningNotification() = runTest(testDispatcher) {
        coEvery { repository.withdrawFromGoal(any(), any()) } just Runs
        val vm = createViewModel()
        collectGoals(vm)

        vm.withdraw("g1", 1_000.0)

        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = match { it.startsWith("GOAL_WITHDRAW_g1_") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Progress threshold notifications (side-effect inside Flow.map)
    // -------------------------------------------------------------------------

    @Test
    fun progressThreshold_at50Percent_sendsWarningWithTag50() = runTest(testDispatcher) {
        val goalAt50 = sampleGoal.copy(
            currentAmount = 25_000_000.0, // 50% of 50M
            targetAmount = 50_000_000.0
        )
        val vm = createViewModel(goals = listOf(goalAt50))
        collectGoals(vm)
        advanceUntilIdle()

        verify(timeout = 3_000, atLeast = 1) {
            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = "GOAL_PROGRESS_50_g1"
            )
        }
    }

    @Test
    fun progressThreshold_at100Percent_sendsSuccessWithTag100() = runTest(testDispatcher) {
        val goalDone = sampleGoal.copy(
            currentAmount = 50_000_000.0,
            targetAmount = 50_000_000.0
        )
        val vm = createViewModel(goals = listOf(goalDone))
        collectGoals(vm)
        advanceUntilIdle()

        verify(timeout = 3_000, atLeast = 1) {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = "GOAL_PROGRESS_100_g1"
            )
        }
    }

    @Test
    fun progressThreshold_below50_doesNotSendProgressTags() = runTest(testDispatcher) {
        // sampleGoal is 20% — only collect, no progress tags expected for 50/80/100
        clearAllMocks(answers = false)
        every { repository.getAllGoals() } returns flowOf(listOf(sampleGoal))
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        val vm = createViewModel(goals = listOf(sampleGoal))
        collectGoals(vm)
        advanceUntilIdle()

        verify(exactly = 0) {
            NotificationHelper.send(
                context = any(),
                type = any(),
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = match { it.startsWith("GOAL_PROGRESS_") }
            )
        }
    }
}