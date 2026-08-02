package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavingGoalRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SavingGoalRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SavingGoalRepositoryImpl(database.savingGoalDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun insertAndGetAllGoals() = runTest {
        val goal1 = SavingGoalEntity(
            id = "g1",
            title = "سفر شمال",
            targetAmount = 50_000_000.0,
            currentAmount = 10_000_000.0,
            icon = "✈️"
        )
        val goal2 = SavingGoalEntity(
            id = "g2",
            title = "لپ‌تاپ",
            targetAmount = 80_000_000.0,
            currentAmount = 0.0,
            icon = "💻"
        )

        repository.insertGoal(goal1)
        repository.insertGoal(goal2)

        val all = repository.getAllGoals().first()
        assertThat(all).hasSize(2)
        assertThat(all.map { it.id }).containsExactly("g1", "g2")
    }

    @Test
    fun updateGoal() = runTest {
        val goal = SavingGoalEntity(
            id = "g1",
            title = "سفر",
            targetAmount = 30_000_000.0,
            currentAmount = 5_000_000.0
        )
        repository.insertGoal(goal)

        val updated = goal.copy(title = "سفر کیش", targetAmount = 40_000_000.0)
        repository.updateGoal(updated)

        val result = repository.getAllGoals().first().first()
        assertThat(result.title).isEqualTo("سفر کیش")
        assertThat(result.targetAmount).isEqualTo(40_000_000.0)
    }

    @Test
    fun deleteGoal() = runTest {
        val goal = SavingGoalEntity(id = "g1", title = "هدف تست", targetAmount = 10_000.0)
        repository.insertGoal(goal)

        repository.deleteGoal(goal)

        val remaining = repository.getAllGoals().first()
        assertThat(remaining).isEmpty()
    }

    @Test
    fun depositToGoal_increasesCurrentAmount() = runTest {
        val goal = SavingGoalEntity(
            id = "g1",
            title = "هدف",
            targetAmount = 100_000.0,
            currentAmount = 20_000.0
        )
        repository.insertGoal(goal)

        repository.depositToGoal("g1", 15_000.0)

        val result = repository.getAllGoals().first().first()
        assertThat(result.currentAmount).isEqualTo(35_000.0)
    }

    @Test
    fun withdrawFromGoal_decreasesCurrentAmount() = runTest {
        val goal = SavingGoalEntity(
            id = "g1",
            title = "هدف",
            targetAmount = 100_000.0,
            currentAmount = 50_000.0
        )
        repository.insertGoal(goal)

        repository.withdrawFromGoal("g1", 20_000.0)

        val result = repository.getAllGoals().first().first()
        assertThat(result.currentAmount).isEqualTo(30_000.0)
    }

    @Test
    fun withdrawFromGoal_doesNotGoBelowZero() = runTest {
        val goal = SavingGoalEntity(
            id = "g1",
            title = "هدف",
            targetAmount = 100_000.0,
            currentAmount = 10_000.0
        )
        repository.insertGoal(goal)

        repository.withdrawFromGoal("g1", 50_000.0)

        val result = repository.getAllGoals().first().first()
        assertThat(result.currentAmount).isEqualTo(0.0)
    }
}