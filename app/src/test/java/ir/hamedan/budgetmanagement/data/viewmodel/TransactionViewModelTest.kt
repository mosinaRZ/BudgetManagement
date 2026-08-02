package ir.hamedan.budgetmanagement.data.viewmodel

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.google.common.truth.Truth.assertThat
import io.mockk.*
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
        TransactionEntity(id = "1", title = "ناهار", amount = 100_000.0, category = "FOOD", type = "EXPENSE", timestamp = 1_700_200_000_000L),
        TransactionEntity(id = "2", title = "حقوق", amount = 50_000_000.0, category = "SALARY", type = "INCOME", timestamp = 1_700_100_000_000L),
        TransactionEntity(id = "3", title = "تاکسی", amount = 50_000.0, category = "TRANSPORT", type = "EXPENSE", timestamp = 1_700_000_000_000L),
        TransactionEntity(id = "4", title = "بدون دسته", amount = 20_000.0, category = "UNCATEGORIZED", type = "EXPENSE", timestamp = 1_700_050_000_000L)
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

        viewModel = TransactionViewModel(
            context = context,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            notificationRepository = notificationRepository
        )

        // داده را بعد از ساخت ViewModel ست می‌کنیم
        transactionsFlow.value = sampleTransactions
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        clearAllMocks()
    }

    /**
     * کمک‌کننده: StateFlow را با backgroundScope جمع‌آوری می‌کند
     * تا stateIn واقعاً فعال شود و آخرین مقدار را برمی‌گرداند.
     */
    private fun TestScope.collectLastFiltered(): List<TransactionEntity> {
        val values = mutableListOf<List<TransactionEntity>>()
        backgroundScope.launch {
            viewModel.filteredTransactions.collect { values.add(it) }
        }
        advanceUntilIdle()
        return values.lastOrNull() ?: emptyList()
    }

    @Test
    fun filteredTransactions_returnsAll_whenNoFilterApplied() = runTest {
        val result = collectLastFiltered()
        assertThat(result).hasSize(4)
    }

    @Test
    fun filterByExpense_returnsOnlyExpenseTransactions() = runTest {
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
    fun filterByIncome_returnsOnlyIncomeTransactions() = runTest {
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
    fun searchQuery_filtersByTitle() = runTest {
        viewModel.onSearchQueryChanged("ناهار")

        val result = collectLastFiltered()
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("ناهار")
    }

    @Test
    fun sortByHighestAmount_worksCorrectly() = runTest {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.ALL,
            typeFilter = TransactionTypeFilter.ALL,
            sortOrder = SortOrder.HIGHEST_AMOUNT,
            startDate = null,
            endDate = null
        )

        val result = collectLastFiltered()
        assertThat(result).isNotEmpty()
        assertThat(result[0].amount).isEqualTo(50_000_000.0)
        assertThat(result.last().amount).isEqualTo(20_000.0)
    }

    @Test
    fun deleteTransaction_callsRepositoryDelete() = runTest {
        coEvery { transactionRepository.deleteTransactionById(any()) } just Runs

        // اول collect می‌کنیم تا stateIn فعال شود
        collectLastFiltered()

        viewModel.deleteTransaction("1")
        advanceUntilIdle()

        coVerify { transactionRepository.deleteTransactionById("1") }
    }

    @Test
    fun clearFilter_resetsToDefaultState() = runTest {
        viewModel.applyCustomFilter(
            timeFilter = TimeFilter.MONTHLY,
            typeFilter = TransactionTypeFilter.EXPENSE,
            sortOrder = SortOrder.OLDEST,
            startDate = 100L,
            endDate = 200L
        )
        viewModel.clearFilter()
        advanceUntilIdle()

        val state = viewModel.filterState.value
        assertThat(state.timeFilter).isEqualTo(TimeFilter.ALL)
        assertThat(state.typeFilter).isEqualTo(TransactionTypeFilter.ALL)
        assertThat(state.sortOrder).isEqualTo(SortOrder.NEWEST)
        assertThat(state.isCustomFilterActive).isFalse()
    }
}