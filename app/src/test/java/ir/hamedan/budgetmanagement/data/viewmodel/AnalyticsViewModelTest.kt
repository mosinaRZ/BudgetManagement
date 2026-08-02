package ir.hamedan.budgetmanagement.data.viewmodel

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsUiState
import ir.hamedan.budgetmanagement.ui.screens.analytics.AnalyticsViewModel
import ir.hamedan.budgetmanagement.ui.screens.transactions.TimeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: AnalyticsViewModel
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    private fun nowMillis(): Long = System.currentTimeMillis()

    private fun daysAgo(days: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getAllTransactions() } returns transactionsFlow
        viewModel = AnalyticsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** آخرین state غیر-loading را برمی‌گرداند */
    private fun TestScope.collectLastUiState(): AnalyticsUiState {
        val values = mutableListOf<AnalyticsUiState>()
        backgroundScope.launch {
            viewModel.uiState.collect { values.add(it) }
        }
        advanceUntilIdle()

        val result = values.lastOrNull { !it.isLoading } ?: values.lastOrNull()
        requireNotNull(result) { "uiState never emitted. collected=$values" }
        return result
    }

    @Test
    fun emptyDatabase_returnsZeroTotals() = runTest {
        transactionsFlow.value = emptyList()
        viewModel.onTimeFilterChanged(TimeFilter.ALL)

        val state = collectLastUiState()

        assertThat(state.hasAnyTransactionInDb).isFalse()
        assertThat(state.totalIncome).isEqualTo(0.0)
        assertThat(state.totalExpense).isEqualTo(0.0)
        assertThat(state.balance).isEqualTo(0.0)
        assertThat(state.categoryExpenses).isEmpty()
    }

    @Test
    fun balance_isIncomeMinusExpense() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", title = "i", amount = 1000.0, type = "INCOME", category = "SALARY", timestamp = nowMillis()),
            TransactionEntity(id = "2", title = "e", amount = 400.0, type = "EXPENSE", category = "FOOD", timestamp = nowMillis())
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)
        val state = collectLastUiState()
        assertThat(state.totalIncome).isEqualTo(1000.0)
        assertThat(state.totalExpense).isEqualTo(400.0)
        assertThat(state.balance).isEqualTo(600.0)
    }

    @Test
    fun emptyDatabase_trendPoints_isSingleZero() = runTest {
        transactionsFlow.value = emptyList()
        viewModel.onTimeFilterChanged(TimeFilter.ALL)
        val state = collectLastUiState()
        assertThat(state.totalIncome).isEqualTo(0.0)
        assertThat(state.totalExpense).isEqualTo(0.0)
        assertThat(state.balance).isEqualTo(0.0)
        assertThat(state.trendPoints).containsExactly(0f)
    }

    @Test
    fun singleTransaction_trendPoints_duplicatedForChart() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", title = "i", amount = 500.0, type = "INCOME", category = "SALARY", timestamp = 1000L)
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)
        val state = collectLastUiState()
        // calculateTrendPoints: size < 2 → [p, p]
        assertThat(state.trendPoints).containsExactly(500f, 500f).inOrder()
    }

    @Test
    fun categoryPercentage_sumsToAbout100_whenMultipleCategories() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", title = "a", amount = 75.0, type = "EXPENSE", category = "FOOD", timestamp = nowMillis()),
            TransactionEntity(id = "2", title = "b", amount = 25.0, type = "EXPENSE", category = "TRANSPORT", timestamp = nowMillis())
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)
        val state = collectLastUiState()
        val sumPct = state.categoryExpenses.sumOf { it.percentage.toDouble() }
        assertThat(sumPct).isWithin(0.2).of(100.0)
    }

    @Test
    fun monthlyFilter_excludesTransactionsFromPreviousMonth() = runTest {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 15)
        val thisMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, -1)
        val lastMonth = cal.timeInMillis

        transactionsFlow.value = listOf(
            TransactionEntity(id = "old", title = "o", amount = 999.0, type = "EXPENSE", category = "FOOD", timestamp = lastMonth),
            TransactionEntity(id = "new", title = "n", amount = 10.0, type = "EXPENSE", category = "FOOD", timestamp = thisMonth)
        )
        viewModel.onTimeFilterChanged(TimeFilter.MONTHLY)
        val state = collectLastUiState()
        assertThat(state.totalExpense).isEqualTo(10.0)
    }

    @Test
    fun calculatesIncomeExpenseAndBalance_correctly() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 10_000_000.0, type = "INCOME", category = "SALARY", timestamp = nowMillis()),
            TransactionEntity(id = "2", amount = 3_000_000.0, type = "EXPENSE", category = "FOOD", timestamp = nowMillis()),
            TransactionEntity(id = "3", amount = 1_000_000.0, type = "EXPENSE", category = "TRANSPORT", timestamp = nowMillis())
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)

        val state = collectLastUiState()

        assertThat(state.totalIncome).isEqualTo(10_000_000.0)
        assertThat(state.totalExpense).isEqualTo(4_000_000.0)
        assertThat(state.balance).isEqualTo(6_000_000.0)
        assertThat(state.hasAnyTransactionInDb).isTrue()
    }

    @Test
    fun categoryExpenses_groupsAndSortsByAmount() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 5_000.0, type = "EXPENSE", category = "FOOD", timestamp = nowMillis()),
            TransactionEntity(id = "2", amount = 3_000.0, type = "EXPENSE", category = "FOOD", timestamp = nowMillis()),
            TransactionEntity(id = "3", amount = 10_000.0, type = "EXPENSE", category = "SHOPPING", timestamp = nowMillis())
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)

        val state = collectLastUiState()

        assertThat(state.categoryExpenses).hasSize(2)
        assertThat(state.categoryExpenses[0].categoryName).isEqualTo("SHOPPING")
        assertThat(state.categoryExpenses[0].totalAmount).isEqualTo(10_000.0)
        assertThat(state.categoryExpenses[1].categoryName).isEqualTo("FOOD")
        assertThat(state.categoryExpenses[1].totalAmount).isEqualTo(8_000.0)
        assertThat(state.categoryExpenses[1].percentage).isWithin(0.1f).of(44.44f)
    }

    @Test
    fun topExpenses_onlyAboveAverage() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 100.0, type = "EXPENSE", category = "A", timestamp = nowMillis()),
            TransactionEntity(id = "2", amount = 200.0, type = "EXPENSE", category = "B", timestamp = nowMillis()),
            TransactionEntity(id = "3", amount = 300.0, type = "EXPENSE", category = "C", timestamp = nowMillis())
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)

        val state = collectLastUiState()

        assertThat(state.averageExpense).isEqualTo(200.0)
        assertThat(state.topExpenses).hasSize(1)
        assertThat(state.topExpenses[0].amount).isEqualTo(300.0)
    }

    @Test
    fun timeFilter_ALL_includesOldTransactions() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "old", amount = 1_000.0, type = "EXPENSE", category = "FOOD", timestamp = daysAgo(400)),
            TransactionEntity(id = "new", amount = 2_000.0, type = "EXPENSE", category = "FOOD", timestamp = nowMillis())
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)

        val state = collectLastUiState()

        assertThat(state.totalExpense).isEqualTo(3_000.0)
    }

    @Test
    fun trendPoints_reflectRunningBalance() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(id = "1", amount = 1000.0, type = "INCOME", timestamp = 1000L),
            TransactionEntity(id = "2", amount = 300.0, type = "EXPENSE", timestamp = 2000L),
            TransactionEntity(id = "3", amount = 200.0, type = "INCOME", timestamp = 3000L)
        )
        viewModel.onTimeFilterChanged(TimeFilter.ALL)

        val state = collectLastUiState()

        assertThat(state.trendPoints).containsExactly(1000f, 700f, 900f).inOrder()
    }
}