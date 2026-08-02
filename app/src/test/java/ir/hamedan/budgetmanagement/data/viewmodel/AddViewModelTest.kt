package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.ui.screens.add.AddViewModel
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
class AddViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var context: Context
    private lateinit var viewModel: AddViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // دسته‌بندی‌ها از قبل وجود داشته باشند تا init چیزی insert نکند
        every { categoryRepository.getAllCategories() } returns flowOf(
            listOf(CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true))
        )
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        // 🔥 مهم: mock کردن side-effectهای اندرویدی
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<BalanceWidget>().updateAll(any()) } just Runs

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

        viewModel = AddViewModel(transactionRepository, categoryRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()          // همه mockها را پاک می‌کند
        clearAllMocks()
    }

    @Test
    fun addTransaction_callsInsertTransactionWithCorrectData() = runTest {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        viewModel.addTransaction(
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
    fun addTransaction_withIsExpenseFalse_setsTypeIncome() = runTest {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs

        viewModel.addTransaction(
            title = "حقوق",
            amount = 50_000_000.0,
            categoryKey = "SALARY",
            isExpense = false
        )
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(
                match { it.type == "INCOME" && it.category == "SALARY" }
            )
        }
    }
}