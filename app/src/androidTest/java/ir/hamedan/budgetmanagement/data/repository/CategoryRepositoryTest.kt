package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var budgetLimitRepository: BudgetLimitRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // مقداردهی BudgetLimitRepository با استفاده از DAOهای دیتابیس داخلی
        budgetLimitRepository = BudgetLimitRepositoryImpl(database.budgetLimitDao())

        repository = CategoryRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            budgetLimitRepository = budgetLimitRepository
        )
        transactionRepository = TransactionRepositoryImpl(database.transactionDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun insertAndGetAllCategories() = runTest {
        repository.insertCategory(CategoryEntity(title = "FOOD", iconEmoji = "🏷️", isExpense = true))
        repository.insertCategory(CategoryEntity(title = "SALARY", iconEmoji = "💰", isExpense = false))

        val all = repository.getAllCategories().first()
        assertThat(all).hasSize(2)
        assertThat(all.map { it.title }).containsExactly("FOOD", "SALARY")
    }

    @Test
    fun getCategoriesByExpenseStatus() = runTest {
        repository.insertCategory(CategoryEntity(title = "FOOD", isExpense = true))
        repository.insertCategory(CategoryEntity(title = "TRANSPORT", isExpense = true))
        repository.insertCategory(CategoryEntity(title = "SALARY", isExpense = false))

        val expenses = repository.getCategoriesByExpenseStatus(true).first()
        val incomes = repository.getCategoriesByExpenseStatus(false).first()

        assertThat(expenses).hasSize(2)
        assertThat(incomes).hasSize(1)
        assertThat(incomes[0].title).isEqualTo("SALARY")
    }

    @Test
    fun updateCategory_alsoReassignsTransactions() = runTest {
        val old = CategoryEntity(title = "OLD_FOOD", iconEmoji = "🏷️", isExpense = true)
        repository.insertCategory(old)

        transactionRepository.insertTransaction(
            TransactionEntity(id = "t1", category = "OLD_FOOD", amount = 100.0, type = "EXPENSE")
        )
        transactionRepository.insertTransaction(
            TransactionEntity(id = "t2", category = "OLD_FOOD", amount = 200.0, type = "EXPENSE")
        )

        val inserted = repository.getAllCategories().first().first { it.title == "OLD_FOOD" }
        repository.updateCategory(inserted, newTitle = "FOOD", newEmoji = "🍏")

        val categories = repository.getAllCategories().first()
        assertThat(categories).hasSize(1)
        assertThat(categories[0].title).isEqualTo("FOOD")
        assertThat(categories[0].iconEmoji).isEqualTo("🍏")

        val txs = transactionRepository.getAllTransactions().first()
        assertThat(txs.all { it.category == "FOOD" }).isTrue()
    }

    @Test
    fun deleteCategoryWithReassignment_movesTransactionsToUncategorized() = runTest {
        val food = CategoryEntity(title = "FOOD", iconEmoji = "🏷️", isExpense = true)
        repository.insertCategory(food)

        transactionRepository.insertTransaction(
            TransactionEntity(id = "t1", category = "FOOD", amount = 150.0, type = "EXPENSE")
        )
        transactionRepository.insertTransaction(
            TransactionEntity(id = "t2", category = "FOOD", amount = 80.0, type = "EXPENSE")
        )

        val inserted = repository.getAllCategories().first().first { it.title == "FOOD" }
        val affected = repository.deleteCategoryWithReassignment(inserted)

        assertThat(affected).isEqualTo(2)

        val remainingCategories = repository.getAllCategories().first()
        assertThat(remainingCategories.any { it.title == "FOOD" }).isFalse()
        assertThat(remainingCategories.any { it.title == "UNCATEGORIZED" }).isTrue()

        val txs = transactionRepository.getAllTransactions().first()
        assertThat(txs.all { it.category == "UNCATEGORIZED" }).isTrue()
    }

    @Test
    fun getTransactionCount() = runTest {
        repository.insertCategory(CategoryEntity(title = "FOOD", isExpense = true))

        transactionRepository.insertTransaction(
            TransactionEntity(id = "1", category = "FOOD", amount = 10.0, type = "EXPENSE")
        )
        transactionRepository.insertTransaction(
            TransactionEntity(id = "2", category = "FOOD", amount = 20.0, type = "EXPENSE")
        )

        assertThat(repository.getTransactionCount("FOOD")).isEqualTo(2)
    }
}