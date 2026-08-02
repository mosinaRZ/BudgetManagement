package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository
import ir.hamedan.budgetmanagement.ui.screens.upcomings.UpcomingPaymentViewModel
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
class UpcomingPaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: UpcomingPaymentRepository
    private lateinit var context: Context
    private lateinit var viewModel: UpcomingPaymentViewModel

    private val samplePayment = UpcomingPaymentEntity(
        id = "p1",
        title = "اجاره",
        amount = 15_000_000.0,
        dueDate = 1_700_000_000_000L,
        dueDay = 1,
        isPaid = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { repository.allPayments } returns flowOf(listOf(samplePayment))

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just Runs

        viewModel = UpcomingPaymentViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun addOrUpdatePayment_callsInsertOrUpdatePayment() = runTest {
        coEvery { repository.insertOrUpdatePayment(any()) } just Runs

        viewModel.addOrUpdatePayment(samplePayment)
        advanceUntilIdle()

        coVerify { repository.insertOrUpdatePayment(samplePayment) }
    }

    @Test
    fun togglePaymentStatus_updatesStatus() = runTest {
        coEvery { repository.insertOrUpdatePayment(any()) } just Runs

        viewModel.togglePaymentStatus(samplePayment)
        advanceUntilIdle()

        coVerify {
            repository.insertOrUpdatePayment(
                match { it.id == "p1" && it.isPaid }
            )
        }
    }
}