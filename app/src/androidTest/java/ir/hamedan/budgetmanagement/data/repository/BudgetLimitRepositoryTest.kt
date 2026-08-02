package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetLimitRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetLimitRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetLimitRepositoryImpl(database.budgetLimitDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun saveAndGetAllLimits() = runTest {
        val limit1 = BudgetLimitEntity(
            categoryName = "FOOD",
            maxLimit = 5_000_000.0,
            isActive = true
        )
        val limit2 = BudgetLimitEntity(
            categoryName = "TRANSPORT",
            maxLimit = 2_000_000.0,
            isActive = true
        )

        repository.saveLimit(limit1)
        repository.saveLimit(limit2)

        val all = repository.getAllLimits().first()
        assertThat(all).hasSize(2)
        assertThat(all.map { it.categoryName }).containsExactly("FOOD", "TRANSPORT")
    }

    @Test
    fun saveLimit_withSameId_replacesExisting() = runTest {
        val original = BudgetLimitEntity(
            id = 1L,
            categoryName = "FOOD",
            maxLimit = 3_000_000.0
        )
        repository.saveLimit(original)

        val updated = BudgetLimitEntity(
            id = 1L,
            categoryName = "FOOD",
            maxLimit = 7_000_000.0,
            isActive = false
        )
        repository.saveLimit(updated)

        val all = repository.getAllLimits().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].maxLimit).isEqualTo(7_000_000.0)
        assertThat(all[0].isActive).isFalse()
    }

    @Test
    fun deleteLimit() = runTest {
        val limit = BudgetLimitEntity(
            categoryName = "FOOD",
            maxLimit = 4_000_000.0
        )
        repository.saveLimit(limit)

        val inserted = repository.getAllLimits().first().first()
        repository.deleteLimit(inserted.id)

        val remaining = repository.getAllLimits().first()
        assertThat(remaining).isEmpty()
    }

    @Test
    fun saveMultipleLimits_forDifferentCategories() = runTest {
        repository.saveLimit(BudgetLimitEntity(categoryName = "FOOD", maxLimit = 1_000_000.0))
        repository.saveLimit(BudgetLimitEntity(categoryName = "ENTERTAINMENT", maxLimit = 2_000_000.0))
        repository.saveLimit(BudgetLimitEntity(categoryName = "SHOPPING", maxLimit = 3_000_000.0))

        val all = repository.getAllLimits().first()
        assertThat(all).hasSize(3)
        assertThat(all.map { it.categoryName }).containsExactly("FOOD", "ENTERTAINMENT", "SHOPPING")
    }
}