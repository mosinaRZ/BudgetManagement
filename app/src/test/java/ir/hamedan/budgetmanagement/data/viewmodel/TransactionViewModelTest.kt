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
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.components.BalanceWidget
import ir.hamedan.budgetmanagement.ui.screens.transactions.SortOrder
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionTypeFilter
import ir.hamedan.budgetmanagement.ui.screens.transactions.TransactionViewModel
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Unit tests for [TransactionViewModel] filtering / sorting / search / mutations.
 *
 * Important: [runTest] must use the same [testDispatcher] that is set as Main,
 * otherwise viewModelScope work never runs when we call advanceUntilIdle().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var context: Context
    private lateinit var viewModel: TransactionViewModel

    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = "1",
            title = "ناهار",
            amount = 100_000.0,
            category = "FOOD",
            type = "EXPENSE",
            timestamp = 1_700_200_000_000L
        ),
        TransactionEntity(
            id = "2",
            title = "حقوق",
            amount = 50_000_000.0,
            category = "SALARY",
            type = "INCOME",
            timestamp = 1_700_100_000_000L
        ),
        TransactionEntity(
            id = "3",
            title = "تاکسی",
            amount = 50_000.0,
            category = "TRANSPORT",
            type = "EXPENSE",
            timestamp = 1_700_000_000_000L
        ),
        TransactionEntity(
            id = "4",
            title = "بدون دسته",
            amount = 20_000.0,
            category = "UNCATEGORIZED",
            type = "EXPENSE",
            timestamp = 1_700_050_000_000L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { transactionRepository.getAllTransactions() } returns transactionsFlow
        every { categoryRepository.getCategoriesByExpenseStatus(any()) } returns flowOf(emptyList())

        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<BalanceWidget>().updateAll(any()) } just Runs

        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs

        // Seed data BEFORE constructing the ViewModel so the first upstream emission is complete.
        transactionsFlow.value = sampleTransactions

        viewModel = TransactionViewModel(
            context = context,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            notificationRepository = notificationRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    /**
     * Subscribes to [TransactionViewModel.filteredTransactions] on the shared test dispatcher
     * and returns the latest emitted list after all pending tasks complete.
     *
     * Skips the synthetic initial empty value from stateIn when a later non-empty emission exists.
     */
    private fun TestScope.collectLastFiltered(): List<TransactionEntity> {
        val values = mutableListOf<List<TransactionEntity>>()
        backgroundScope.launch(testDispatcher) {
            viewModel.filteredTransactions.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull { it.isNotEmpty() } ?: values.lastOrNull() ?: emptyList()
    }

    // -------------------------------------------------------------------------
    // Baseline / type filters
    // -------------------------------------------------------------------------

    @Test
    fun filteredTransactions_returnsAll_whenNoFilterApplied() = runTest(testDispatcher) {
        val result = collectLastFiltered()
        assertThat(result).hasSize(4)
    }

    @Test
    fun filterByExpense_returnsOnlyExpenseTransactions() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.EXPENSE,
            sortOrder = SortOrder.NEWEST,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).hasSize(3)
        assertThat(result.all { it.type == "EXPENSE" }).isTrue()
    }

    @Test
    fun filterByIncome_returnsOnlyIncomeTransactions() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.INCOME,
            sortOrder = SortOrder.NEWEST,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("حقوق")
    }

    @Test
    fun filterByUncategorized_returnsOnlyUncategorized() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.UNCATEGORIZED,
            sortOrder = SortOrder.NEWEST,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].category).isEqualTo("UNCATEGORIZED")
        assertThat(result[0].id).isEqualTo("4")
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @Test
    fun searchQuery_filtersByTitle() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("ناهار")
        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("ناهار")
    }

    @Test
    fun searchQuery_filtersByCategoryKey_caseInsensitive() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("food")
        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("1")
    }

    @Test
    fun searchQuery_blank_returnsAll() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("ناهار")
        collectLastFiltered()
        viewModel.onSearchQueryChanged("")
        val result = collectLastFiltered()
        assertThat(result).hasSize(4)
    }

    // -------------------------------------------------------------------------
    // Sort
    // -------------------------------------------------------------------------

    @Test
    fun sortByNewest_putsHighestTimestampFirst() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.NEWEST,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).isNotEmpty()
        assertThat(result.first().id).isEqualTo("1") // 1_700_200_000_000
        assertThat(result.last().id).isEqualTo("3")  // 1_700_000_000_000
    }

    @Test
    fun sortByOldest_worksCorrectly() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.OLDEST,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).isNotEmpty()
        assertThat(result.first().timestamp).isLessThan(result.last().timestamp)
        assertThat(result.first().id).isEqualTo("3")
        assertThat(result.last().id).isEqualTo("1")
    }

    @Test
    fun sortByHighestAmount_worksCorrectly() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.HIGHEST_AMOUNT,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).isNotEmpty()
        assertThat(result.first().amount).isEqualTo(50_000_000.0)
        assertThat(result.last().amount).isEqualTo(20_000.0)
    }

    @Test
    fun sortByLowestAmount_worksCorrectly() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.LOWEST_AMOUNT,
            startDate = null,
            endDate = null
        )
        val result = collectLastFiltered()
        assertThat(result).isNotEmpty()
        assertThat(result.first().amount).isEqualTo(20_000.0)
        assertThat(result.last().amount).isEqualTo(50_000_000.0)
        assertThat(result.map { it.amount }).isInOrder()
    }

    // -------------------------------------------------------------------------
    // Custom date range
    // -------------------------------------------------------------------------

    @Test
    fun customDateRange_filtersCorrectly() = runTest(testDispatcher) {
        // Inclusive range: [1_700_050_000_000 .. 1_700_150_000_000]
        // hits: id=4 (1_700_050), id=2 (1_700_100)
        // misses: id=3 (1_700_000), id=1 (1_700_200)
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.NEWEST,
            startDate = 1_700_050_000_000L,
            endDate = 1_700_150_000_000L
        )
        val result = collectLastFiltered()
        assertThat(result.map { it.id }).containsExactly("2", "4")
        assertThat(result.none { it.id == "1" || it.id == "3" }).isTrue()
    }

    @Test
    fun customDateRange_boundaryTimestamps_areInclusive() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.OLDEST,
            startDate = 1_700_050_000_000L,
            endDate = 1_700_050_000_000L
        )
        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("4")
    }

    // -------------------------------------------------------------------------
    // Combined filters
    // -------------------------------------------------------------------------

    @Test
    fun expensePlusSearch_combinesFilters() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.EXPENSE,
            sortOrder = SortOrder.NEWEST,
            startDate = null,
            endDate = null
        )
        viewModel.onSearchQueryChanged("تاکسی")
        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("3")
    }

    @Test
    fun clearFilter_resetsToDefaults() = runTest(testDispatcher) {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.INCOME,
            sortOrder = SortOrder.LOWEST_AMOUNT,
            startDate = 1L,
            endDate = 2L
        )
        collectLastFiltered()
        viewModel.clearFilter()
        viewModel.onSearchQueryChanged("")
        val result = collectLastFiltered()
        assertThat(result).hasSize(4)
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    @Test
    fun deleteTransaction_callsRepositoryDelete() = runTest(testDispatcher) {
        coEvery { transactionRepository.deleteTransactionById(any()) } just Runs
        // Activate stateIn subscription so filteredTransactions.value is populated for title lookup
        collectLastFiltered()

        viewModel.deleteTransaction("1")
        advanceUntilIdle()

        coVerify { transactionRepository.deleteTransactionById("1") }
    }

    @Test
    fun updateTransaction_callsInsertTransaction() = runTest(testDispatcher) {
        coEvery { transactionRepository.insertTransaction(any()) } just Runs
        collectLastFiltered()

        val updated = sampleTransactions[0].copy(title = "ناهار ویرایش‌شده", amount = 200_000.0)
        viewModel.updateTransaction(updated)
        advanceUntilIdle()

        coVerify {
            transactionRepository.insertTransaction(
                match { it.title == "ناهار ویرایش‌شده" && it.amount == 200_000.0 }
            )
        }
    }
}