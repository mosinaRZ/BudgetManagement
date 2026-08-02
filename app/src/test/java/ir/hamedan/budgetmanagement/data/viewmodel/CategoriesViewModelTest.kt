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
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.ui.screens.categories.CategoriesViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

/**
 * CategoriesViewModel launches mutations on Dispatchers.IO.
 * Use coVerify(timeout = …) / verify(timeout = …) so assertions wait for the IO thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var context: Context
    private lateinit var viewModel: CategoriesViewModel

    private val sampleCategory = CategoryEntity(
        id = "c1",
        title = "FOOD",
        iconEmoji = "🍕",
        isExpense = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        categoryRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { categoryRepository.getAllCategories() } returns flowOf(listOf(sampleCategory))

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        viewModel = CategoriesViewModel(
            categoryRepository = categoryRepository,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    private fun TestScope.collectLastCategories(): List<CategoryEntity>? {
        val values = mutableListOf<List<CategoryEntity>?>()
        backgroundScope.launch(testDispatcher) {
            viewModel.categories.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull { it != null } ?: values.lastOrNull()
    }

    // -------------------------------------------------------------------------
    // Observing categories
    // -------------------------------------------------------------------------

    @Test
    fun categories_emitsRepositoryList() = runTest(testDispatcher) {
        val result = collectLastCategories()
        assertThat(result).isNotNull()
        assertThat(result).hasSize(1)
        assertThat(result!![0].title).isEqualTo("FOOD")
    }

    // -------------------------------------------------------------------------
    // addCategory
    // -------------------------------------------------------------------------

    @Test
    fun addCategory_expense_callsInsertWithCorrectFields() = runTest(testDispatcher) {
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        viewModel.addCategory(title = "ورزش", iconEmoji = "🏋️", isExpense = true)

        coVerify(timeout = 3_000) {
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
    fun addCategory_income_setsIsExpenseFalse() = runTest(testDispatcher) {
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        viewModel.addCategory(title = "پاداش", iconEmoji = "🎁", isExpense = false)

        coVerify(timeout = 3_000) {
            categoryRepository.insertCategory(
                match { it.title == "پاداش" && !it.isExpense }
            )
        }
    }

    @Test
    fun addCategory_sendsSuccessNotification() = runTest(testDispatcher) {
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        viewModel.addCategory(title = "ورزش", iconEmoji = "🏋️", isExpense = true)

        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("ورزش") },
                descEn = match { it.contains("ورزش") || it.contains("expense") },
                tag = match { it.startsWith("CATEGORY_ADD_ورزش_") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // updateCategory
    // -------------------------------------------------------------------------

    @Test
    fun updateCategory_callsRepositoryWithNewTitleAndEmoji() = runTest(testDispatcher) {
        coEvery { categoryRepository.updateCategory(any(), any(), any()) } just Runs

        viewModel.updateCategory(
            category = sampleCategory,
            newTitle = "خوراکی",
            newEmoji = "🍔"
        )

        coVerify(timeout = 3_000) {
            categoryRepository.updateCategory(sampleCategory, "خوراکی", "🍔")
        }
    }

    @Test
    fun updateCategory_sendsWarningNotification() = runTest(testDispatcher) {
        coEvery { categoryRepository.updateCategory(any(), any(), any()) } just Runs

        viewModel.updateCategory(sampleCategory, newTitle = "خوراکی", newEmoji = "🍔")

        verify(timeout = 3_000) {
            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = any(),
                titleEn = any(),
                descFa = match { it.contains("FOOD") && it.contains("خوراکی") },
                descEn = any(),
                tag = match { it.startsWith("CATEGORY_UPDATE_") }
            )
        }
    }

    // -------------------------------------------------------------------------
    // getTransactionCount
    // -------------------------------------------------------------------------

    @Test
    fun getTransactionCount_delegatesToRepository() = runTest(testDispatcher) {
        coEvery { categoryRepository.getTransactionCount("FOOD") } returns 7

        val count = viewModel.getTransactionCount("FOOD")

        assertThat(count).isEqualTo(7)
        coVerify { categoryRepository.getTransactionCount("FOOD") }
    }
}