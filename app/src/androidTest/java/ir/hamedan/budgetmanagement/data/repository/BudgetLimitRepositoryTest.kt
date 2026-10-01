package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.testing.FakeSyncLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class BudgetLimitRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: BudgetLimitRepository
    private lateinit var sync: FakeSyncLocalDataSource

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        sync = FakeSyncLocalDataSource()
        repository = BudgetLimitRepositoryImpl(db.budgetLimitDao(), sync)
    }

    @After fun tearDown() = db.close()

    @Test fun saveAndDelete_persistAndCreateTombstone() = runTest {
        val limit = BudgetLimitEntity(id="b1", categoryId="food", maxLimit=10_000L, startDate=1L, endDate=2L)
        repository.saveLimit(limit)
        val saved = repository.getAllLimits().first().single()
        assertThat(saved.categoryId).isEqualTo("food")
        assertThat(saved.maxLimit).isEqualTo(10_000L)
        assertThat(repository.getLimitCountForCategory("food")).isEqualTo(1)

        repository.deleteLimit("b1")
        assertThat(repository.getAllLimits().first()).isEmpty()
        assertThat(sync.mutations.last().isDeleted).isTrue()
    }
}