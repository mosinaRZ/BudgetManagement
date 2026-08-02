package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesViewModel
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
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var context: Context
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())

        viewModel = CategoriesViewModel(categoryRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `addCategory calls insertCategory`() = runTest {
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        viewModel.addCategory(title = "ورزش", iconEmoji = "🏋️", isExpense = true)
        advanceUntilIdle()

        coVerify {
            categoryRepository.insertCategory(
                match {
                    it.title == "ورزش" &&
                            it.iconEmoji == "🏋️" &&
                            it.isExpense
                }
            )
        }
    }

    @Test
    fun `updateCategory calls repository update`() = runTest {
        val category = CategoryEntity(id = "c1", title = "قدیمی", iconEmoji = "📁", isExpense = true)
        coEvery { categoryRepository.updateCategory(any(), any(), any()) } just Runs

        viewModel.updateCategory(category, newTitle = "جدید", newEmoji = "🆕")
        advanceUntilIdle()

        coVerify {
            categoryRepository.updateCategory(category, "جدید", "🆕")
        }
    }
}