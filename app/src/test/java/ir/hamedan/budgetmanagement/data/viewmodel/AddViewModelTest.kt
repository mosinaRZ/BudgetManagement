package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.ui.screens.add.AddViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var viewModel: AddViewModel

    private val food = CategoryEntity(id="food-id", title="FOOD", isExpense=true)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        categoryRepository = mockk(relaxed=true)
        transactionRepository = mockk(relaxed=true)
        every { categoryRepository.getAllCategories() } returns flowOf(listOf(food))
        mockkObject(NotificationHelper)
        every { NotificationHelper.send(any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<BalanceWidget>().updateAll(any()) } just Runs

        viewModel = AddViewModel(transactionRepository, categoryRepository, mockk<Context>(relaxed=true))
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun addTransaction_resolvesCategoryAndConvertsMoneyToStorageUnit() = runTest(dispatcher) {
        viewModel.addTransaction("Lunch", 12_345.0, "FOOD", true, "note")

        coVerify {
            transactionRepository.insertTransaction(
                match {
                    it.title == "Lunch" &&
                            it.amount == 12_345L &&
                            it.categoryId == "food-id" &&
                            it.type == "EXPENSE" &&
                            it.note == "note"
                }
            )
        }
    }

    @Test fun unknownCategory_failsWithoutWritingTransaction() = runTest(dispatcher) {
        every { categoryRepository.getAllCategories() } returns flowOf(listOf(CategoryEntity(id="x", title="OTHER", isExpense=true)))

        viewModel.addTransaction("Lunch", 100.0, "FOOD", true)
        // The coroutine failure is isolated by viewModelScope; verify that no transaction was written.
        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
    }
}
