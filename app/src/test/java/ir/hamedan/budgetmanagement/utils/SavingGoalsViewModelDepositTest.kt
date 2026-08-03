package ir.hamedan.budgetmanagement.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.goals.SavingGoalsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SavingGoalsViewModelDepositTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var goalRepository: SavingGoalRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var context: Context
    private lateinit var viewModel: SavingGoalsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        goalRepository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)

        coEvery { goalRepository.getAllGoals() } returns flowOf(emptyList())

        // جلوگیری از ClassCastException
        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        viewModel = SavingGoalsViewModel(goalRepository, transactionRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `deposit fails when balance is insufficient`() = runTest {
        coEvery { transactionRepository.getCurrentBalance() } returns 100.0

        viewModel.deposit("goal-1", 500.0)

        // صبر واقعی برای اتمام کار روی Dispatchers.IO
        withContext(Dispatchers.Default) {
            delay(500)
        }

        coVerify(exactly = 0) { goalRepository.depositToGoal(any(), any()) }
        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `deposit succeeds when balance is sufficient`() = runTest {
        coEvery { transactionRepository.getCurrentBalance() } returns 1000.0
        coEvery { goalRepository.getAllGoals() } returns flowOf(
            listOf(SavingGoalEntity(id = "goal-1", title = "Test", targetAmount = 5000.0))
        )

        viewModel.deposit("goal-1", 300.0)
        advanceUntilIdle()

        coVerify(exactly = 1) { goalRepository.depositToGoal("goal-1", 300.0) }
        coVerify(exactly = 1) { transactionRepository.insertTransaction(any()) }
    }
}