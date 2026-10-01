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
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.home.PendingTransactionViewModel
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
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
class PendingTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pendingRepository: PendingTransactionRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var context: Context
    private lateinit var viewModel: PendingTransactionViewModel

    private val samplePending = PendingTransactionEntity(
        id = "pt1",
        rawMessage = "برداشت مبلغ 150000 تومان",
        senderAddress = "Bank",
        amount = 150_000L,
        isAmountDetected = true,
        type = "EXPENSE",
        isTypeDetected = true,
        suggestedTitle = "تراکنش پیامکی",
        suggestedCategory = "",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        pendingRepository = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { pendingRepository.getPendingTransactions() } returns flowOf(listOf(samplePending))
        every { pendingRepository.getPendingCount() } returns flowOf(1)
        every { categoryRepository.getAllCategories() } returns flowOf(
            listOf(
                CategoryEntity(id = "FOOD", title = "غذا", isExpense = true),
                CategoryEntity(id = "SALARY", title = "حقوق", isExpense = false)
            )
        )
        every { categoryRepository.getCategoriesByExpenseStatus(any()) } returns flowOf(emptyList())

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any(), any())
        } just Runs

        viewModel = PendingTransactionViewModel(
            context = context,
            pendingRepository = pendingRepository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun confirmTransaction_insertsTransactionAndConfirmsPending() = runTest(testDispatcher) {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs
        coEvery { pendingRepository.updateStatus(any(), any()) } just Runs

        viewModel.confirmTransaction(
            pending = samplePending,
            title = "خرید",
            amount = 150_000L,
            category = "FOOD",
            isExpense = true,
            note = "از پیامک"
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(
                match {
                    it.title == "خرید" &&
                            it.amount == 150_000L &&
                            it.categoryId == "FOOD" &&
                            it.type == "EXPENSE" &&
                            it.note == "از پیامک" &&
                            it.timestamp == samplePending.timestamp
                }
            )
            pendingRepository.updateStatus("pt1", "CONFIRMED")
        }
    }

    @Test
    fun confirmTransaction_income_setsTypeIncome() = runTest(testDispatcher) {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs
        coEvery { pendingRepository.updateStatus(any(), any()) } just Runs

        viewModel.confirmTransaction(
            pending = samplePending,
            title = "واریز",
            amount = 2_000_000L,
            category = "SALARY",
            isExpense = false
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(match { it.type == "INCOME" })
            pendingRepository.updateStatus("pt1", "CONFIRMED")
        }
    }

    @Test
    fun confirmTransaction_preservesSmsTimestamp() = runTest(testDispatcher) {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs
        coEvery { pendingRepository.updateStatus(any(), any()) } just Runs

        val smsTime = 1_650_111_222_333L
        val pending = samplePending.copy(timestamp = smsTime)

        viewModel.confirmTransaction(
            pending = pending,
            title = "x",
            amount = 1L,
            category = "FOOD",
            isExpense = true
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(match { it.timestamp == smsTime })
        }
    }

    @Test
    fun confirmTransaction_sendsSuccessNotificationWithPendingIdTag() = runTest(testDispatcher) {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs
        coEvery { pendingRepository.updateStatus(any(), any()) } just Runs

        viewModel.confirmTransaction(
            pending = samplePending,
            title = "خرید",
            amount = 10L,
            category = "FOOD",
            isExpense = true
        )
        advanceUntilIdle()

        verify {
            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.SMS_CONFIRMED,
                type = "SMS",
                titleFa = any(),
                titleEn = any(),
                descFa = any(),
                descEn = any(),
                tag = "SMS_CONFIRMED_pt1"
            )
        }
    }

    @Test
    fun ignoreTransaction_callsIgnoreOnly_doesNotInsertTransaction() = runTest(testDispatcher) {
        coEvery { pendingRepository.updateStatus(any(), any()) } just Runs

        viewModel.ignoreTransaction(samplePending)
        advanceUntilIdle()

        coVerify(exactly = 1) { pendingRepository.updateStatus("pt1", "IGNORED") }
        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
        coVerify(exactly = 0) { pendingRepository.updateStatus(any(), "CONFIRMED") }
    }
}