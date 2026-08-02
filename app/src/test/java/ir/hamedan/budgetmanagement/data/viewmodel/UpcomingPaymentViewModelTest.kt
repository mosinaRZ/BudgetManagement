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
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.repository.UpcomingPaymentRepository
import ir.hamedan.budgetmanagement.ui.screens.upcomings.UpcomingPaymentViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import ir.hamedan.budgetmanagement.utils.PaymentDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class UpcomingPaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: UpcomingPaymentRepository
    private lateinit var context: Context
    private lateinit var viewModel: UpcomingPaymentViewModel

    private val paymentsFlow = MutableStateFlow<List<UpcomingPaymentEntity>>(emptyList())

    private val samplePayment = UpcomingPaymentEntity(
        id = "p1",
        title = "اجاره",
        amount = 15_000_000.0,
        dueDate = 1_700_000_000_000L,
        dueDay = 1,
        isPaid = false
    )

    private val nextMonthDue = 1_702_675_200_000L // arbitrary fixed next date for mock

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        paymentsFlow.value = listOf(samplePayment)
        every { repository.allPayments } returns paymentsFlow

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        mockkObject(PaymentDateUtils)
        every {
            PaymentDateUtils.getNextMonthDueDate(any(), any())
        } returns nextMonthDue

        viewModel = UpcomingPaymentViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    private fun TestScope.collectPayments(): List<UpcomingPaymentEntity> {
        val values = mutableListOf<List<UpcomingPaymentEntity>>()
        backgroundScope.launch(testDispatcher) {
            viewModel.payments.collect { values.add(it) }
        }
        advanceUntilIdle()
        // force upstream re-emit after subscription is active
        paymentsFlow.value = listOf(samplePayment)
        advanceUntilIdle()
        return values.lastOrNull { it.isNotEmpty() } ?: values.lastOrNull() ?: emptyList()
    }

    // -------------------------------------------------------------------------
    // addOrUpdatePayment
    // -------------------------------------------------------------------------

    @Test
    fun addOrUpdatePayment_newPayment_callsInsertAndSendsSuccess() = runTest(testDispatcher) {
        coEvery { repository.insertOrUpdatePayment(any()) } just Runs
        // empty list → treated as new (not edit)
        paymentsFlow.value = emptyList()
        collectPayments()

        val newPayment = samplePayment.copy(id = "p2", title = "قبض آب")
        viewModel.addOrUpdatePayment(newPayment)
        advanceUntilIdle()

        coVerify { repository.insertOrUpdatePayment(newPayment) }
        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("قبض آب") },
                descEn = any(),
                tag = match { it.startsWith("UPCOMING_SAVE_p2_") }
            )
        }
    }

    @Test
    fun addOrUpdatePayment_existingId_sendsWarningAsEdit() = runTest(testDispatcher) {
        coEvery { repository.insertOrUpdatePayment(any()) } just Runs
        collectPayments() // payments.value contains p1

        val edited = samplePayment.copy(amount = 16_000_000.0)
        viewModel.addOrUpdatePayment(edited)
        advanceUntilIdle()

        coVerify { repository.insertOrUpdatePayment(edited) }
        verify {
            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = match { it.startsWith("UPCOMING_SAVE_p1_") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // togglePaymentStatus
    // -------------------------------------------------------------------------

    @Test
    fun togglePaymentStatus_markPaid_setsIsPaidTrue_andAdvancesDueDate() = runTest(testDispatcher) {
        coEvery { repository.insertOrUpdatePayment(any()) } just Runs
        collectPayments()

        viewModel.togglePaymentStatus(samplePayment)
        advanceUntilIdle()

        coVerify {
            repository.insertOrUpdatePayment(
                match {
                    it.id == "p1" &&
                            it.isPaid &&
                            it.dueDate == nextMonthDue
                }
            )
        }
        verify {
            PaymentDateUtils.getNextMonthDueDate(samplePayment.dueDate, samplePayment.dueDay)
        }
        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = match { it.startsWith("UPCOMING_STATUS_p1_") }
            )
        }
    }

    @Test
    fun togglePaymentStatus_markUnpaid_doesNotChangeDueDate() = runTest(testDispatcher) {
        coEvery { repository.insertOrUpdatePayment(any()) } just Runs
        val paid = samplePayment.copy(isPaid = true, dueDate = nextMonthDue)
        paymentsFlow.value = listOf(paid)
        collectPayments()

        viewModel.togglePaymentStatus(paid)
        advanceUntilIdle()

        coVerify {
            repository.insertOrUpdatePayment(
                match {
                    it.id == "p1" &&
                            !it.isPaid &&
                            it.dueDate == nextMonthDue // unchanged when going unpaid
                }
            )
        }
        verify(exactly = 0) {
            PaymentDateUtils.getNextMonthDueDate(any(), any())
        }
    }

    // -------------------------------------------------------------------------
    // deletePayment
    // -------------------------------------------------------------------------

    @Test
    fun deletePayment_callsRepositoryAndSendsErrorNotification() = runTest(testDispatcher) {
        coEvery { repository.deletePayment(any()) } just Runs
        collectPayments()

        viewModel.deletePayment("p1")
        advanceUntilIdle()

        coVerify { repository.deletePayment("p1") }
        verify {
            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("اجاره") },
                descEn = any(),
                tag = match { it.startsWith("UPCOMING_DELETE_p1_") }
            )
        }
    }
}