package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.testing.FakeSyncLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SavingGoalRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: SavingGoalRepository
    private lateinit var sync: FakeSyncLocalDataSource

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        sync = FakeSyncLocalDataSource()
        repository = SavingGoalRepositoryImpl(db.savingGoalDao(), db.savingGoalOperationDao(), sync)
    }

    @After fun tearDown() = db.close()

    @Test fun depositAndWithdraw_useImmutableOperations() = runTest {
        repository.insertGoal(SavingGoalEntity(id="g1", title="Trip", targetAmount=10_000L))
        repository.depositToGoal("g1", 3_000L)
        repository.withdrawFromGoal("g1", 500L)

        val goal = repository.getAllGoals().first().single()
        assertThat(goal.currentAmount).isEqualTo(2_500L)
        assertThat(db.savingGoalOperationDao().getByGoalId("g1")).hasSize(2)
    }

    @Test fun withdrawAboveBalance_isRejected() = runTest {
        repository.insertGoal(SavingGoalEntity(id="g1", title="Trip", targetAmount=10_000L))
        var failed = false
        try { repository.withdrawFromGoal("g1", 1L) } catch (_: IllegalArgumentException) { failed = true }
        assertThat(failed).isTrue()
    }
}