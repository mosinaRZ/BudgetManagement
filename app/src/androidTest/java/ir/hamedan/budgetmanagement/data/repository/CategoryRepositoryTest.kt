package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.testing.FakeSyncLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: CategoryRepository
    private lateinit var sync: FakeSyncLocalDataSource

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        sync = FakeSyncLocalDataSource()
        repository = CategoryRepositoryImpl(db.categoryDao(), db.transactionDao(), db.budgetLimitDao(), sync)
    }

    @After fun tearDown() = db.close()

    @Test fun categoryCrud_andCounts_useStableCategoryId() = runTest {
        val food = CategoryEntity(id="food", title="FOOD", isExpense=true)
        repository.insertCategory(food)
        db.transactionDao().insertTransaction(TransactionEntity(id="t1", amount=1_000L, categoryId="food"))
        assertThat(repository.getTransactionCount("food")).isEqualTo(1)
        assertThat(repository.getTransactionTotal("food")).isEqualTo(1_000L)

        repository.updateCategory(food, "خوراک", "🍽️")
        assertThat(repository.getAllCategories().first().single().title).isEqualTo("خوراک")
    }

    @Test fun delete_reassignsTransactionsAndDeletesBudgets() = runTest {
        val food = CategoryEntity(id="food", title="FOOD", isExpense=true)
        repository.insertCategory(food)
        db.transactionDao().insertTransaction(TransactionEntity(id="t1", amount=500L, categoryId="food"))
        db.budgetLimitDao().insertOrUpdate(
            BudgetLimitEntity(id="limit", categoryId="food", maxLimit=5_000L, startDate=0L, endDate=Long.MAX_VALUE)
        )

        assertThat(repository.deleteCategoryWithReassignment(food)).isEqualTo(1)
        assertThat(db.transactionDao().getById("t1")!!.categoryId).isNotEqualTo("food")
        assertThat(db.budgetLimitDao().getById("limit")).isNull()
        assertThat(repository.getAllCategories().first().any { it.title == "UNCATEGORIZED" }).isTrue()
    }
}