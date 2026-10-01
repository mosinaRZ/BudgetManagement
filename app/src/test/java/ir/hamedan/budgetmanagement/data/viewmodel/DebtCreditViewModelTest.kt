package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.domain.usecase.DebtCreditUseCase
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.ui.screens.debtCredit.DebtCreditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebtCreditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: DebtCreditRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: DebtCreditUseCase
    private lateinit var context: Context
    private lateinit var viewModel: DebtCreditViewModel

    private val debt = DebtCreditEntity(
        id = "d1", type = "DEBT", personName = "Ali", totalAmount = 100_000L,
        paidAmount = 20_000L, dueDateMillis = System.currentTimeMillis() + 86_400_000L
    )

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        useCase = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { repository.getAllDebtCredits() } returns flowOf(listOf(debt))
        coEvery { transactionRepository.getCurrentBalance() } returns 1_000_000L

        mockkObject(NotificationHelper)
        every { NotificationHelper.send(any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<BalanceWidget>().updateAll(any()) } just Runs

        viewModel = DebtCreditViewModel(
            debtCreditRepository = repository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            context = context,
            ioDispatcher = dispatcher,
            useCase = useCase
        )
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    @Test fun debtCreditList_exposesRepositoryItems() = runTest(dispatcher) {
        assertThat(viewModel.debtCreditList.first()).containsExactly(debt)
    }

    @Test fun saveOrUpdate_delegatesToUseCase() = runTest(dispatcher) {
        coEvery { useCase.saveOrUpdate(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns debt

        viewModel.saveOrUpdate(
            id = "d1", type = "DEBT", personName = "Ali", totalAmount = 100_000.0,
            isMonthly = false, monthlyAmount = 0.0, dueDay = 5,
            oneTimeDueDateMillis = debt.dueDateMillis, note = "note", addToBalance = false
        )
        advanceUntilIdle()

        coVerify { useCase.saveOrUpdate("d1", "DEBT", "Ali", 100_000.0, false, 0.0, 5, debt.dueDateMillis, "note", false) }
        verify { NotificationHelper.send(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test fun deposit_delegatesToUseCaseWhenBalanceIsEnough() = runTest(dispatcher) {
        viewModel.debtCreditList // establish StateFlow; upstream is lazy
        coEvery { useCase.deposit(debt, 30_000L) } just Runs
        viewModel.debtCreditList.first()

        viewModel.deposit("d1", 30_000L)
        advanceUntilIdle()

        coVerify { useCase.deposit(debt, 30_000L) }
    }
}
