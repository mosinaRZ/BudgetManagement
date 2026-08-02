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
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.ui.screens.add.AddViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class AddViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var context: Context

    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    private val defaultTitles = setOf(
        "FOOD", "TRANSPORT", "SHOPPING", "BILL", "SALARY", "INVESTMENT"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { categoryRepository.getAllCategories() } returns categoriesFlow
        coEvery { categoryRepository.insertCategory(any()) } returns 1L
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<BalanceWidget>().updateAll(any()) } just Runs

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

    private fun createViewModel(): AddViewModel {
        return AddViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            context = context
        )
    }

    // -------------------------------------------------------------------------
    // Default categories seeding (init block)
    // -------------------------------------------------------------------------

    @Test
    fun init_whenCategoriesEmpty_insertsSixDefaults() = runTest(testDispatcher) {
        categoriesFlow.value = emptyList()

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 6) { categoryRepository.insertCategory(any()) }
        coVerify {
            categoryRepository.insertCategory(match { it.title == "FOOD" && it.isExpense })
            categoryRepository.insertCategory(match { it.title == "TRANSPORT" && it.isExpense })
            categoryRepository.insertCategory(match { it.title == "SHOPPING" && it.isExpense })
            categoryRepository.insertCategory(match { it.title == "BILL" && it.isExpense })
            categoryRepository.insertCategory(match { it.title == "SALARY" && !it.isExpense })
            categoryRepository.insertCategory(match { it.title == "INVESTMENT" && !it.isExpense })
        }
    }

    @Test
    fun init_whenCategoriesAlreadyExist_doesNotInsertDefaults() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true)
        )

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { categoryRepository.insertCategory(any()) }
    }

    // -------------------------------------------------------------------------
    // addTransaction
    // -------------------------------------------------------------------------

    @Test
    fun addTransaction_expense_insertsWithCorrectFields() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true)
        )
        val vm = createViewModel()
        advanceUntilIdle()

        vm.addTransaction(
            title = "ناهار",
            amount = 150_000.0,
            categoryKey = "FOOD",
            isExpense = true,
            note = "رستوران"
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(
                match {
                    it.title == "ناهار" &&
                            it.amount == 150_000.0 &&
                            it.category == "FOOD" &&
                            it.type == "EXPENSE" &&
                            it.note == "رستوران"
                }
            )
        }
    }

    @Test
    fun addTransaction_income_setsTypeIncome() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(
            CategoryEntity(title = "SALARY", iconEmoji = "💰", isExpense = false)
        )
        val vm = createViewModel()
        advanceUntilIdle()

        vm.addTransaction(
            title = "حقوق",
            amount = 50_000_000.0,
            categoryKey = "SALARY",
            isExpense = false
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(
                match { it.type == "INCOME" && it.category == "SALARY" && it.note == "" }
            )
        }
    }

    @Test
    fun addTransaction_sendsSuccessNotification() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true)
        )
        val vm = createViewModel()
        advanceUntilIdle()

        vm.addTransaction(
            title = "تاکسی",
            amount = 50_000.0,
            categoryKey = "TRANSPORT",
            isExpense = true
        )
        advanceUntilIdle()

        verify {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("تاکسی") },
                descEn = any(),
                tag = match { it.startsWith("TRANSACTION_ADDED_") }
            )
        }
    }

    @Test
    fun addTransaction_defaultNoteIsEmptyString() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true)
        )
        val vm = createViewModel()
        advanceUntilIdle()

        vm.addTransaction(
            title = "x",
            amount = 1.0,
            categoryKey = "FOOD",
            isExpense = true
            // note omitted
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(match { it.note == "" })
        }
    }
}