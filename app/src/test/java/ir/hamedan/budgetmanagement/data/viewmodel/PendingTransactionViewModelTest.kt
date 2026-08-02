package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.home.PendingTransactionViewModel
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
class PendingTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var pendingRepository: PendingTransactionRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var context: Context
    private lateinit var viewModel: PendingTransactionViewModel

    private val samplePending = PendingTransactionEntity(
        id = "pt1",
        rawMessage = "برداشت 1500000",
        amount = 150_000.0,
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
        every { categoryRepository.getCategoriesByExpenseStatus(any()) } returns flowOf(emptyList())

        // 🔥 mock کامل NotificationHelper (خیلی مهم)
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
    fun confirmTransaction_insertsTransactionAndConfirmsPending() = runTest {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs
        coEvery { pendingRepository.confirm(any()) } just Runs

        viewModel.confirmTransaction(
            pending = samplePending,
            title = "خرید",
            amount = 150_000.0,
            category = "FOOD",
            isExpense = true,
            note = "از پیامک"
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(
                match {
                    it.title == "خرید" &&
                            it.amount == 150_000.0 &&
                            it.category == "FOOD" &&
                            it.type == "EXPENSE" &&
                            it.timestamp == samplePending.timestamp
                }
            )
            pendingRepository.confirm("pt1")
        }
    }

    @Test
    fun ignoreTransaction_callsIgnore() = runTest {
        coEvery { pendingRepository.ignore(any()) } just Runs

        viewModel.ignoreTransaction(samplePending)
        advanceUntilIdle()

        coVerify { pendingRepository.ignore("pt1") }
    }
}