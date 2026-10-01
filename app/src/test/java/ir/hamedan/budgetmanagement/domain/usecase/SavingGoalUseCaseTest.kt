package ir.hamedan.budgetmanagement.domain.usecase

import io.mockk.coVerify
import io.mockk.mockk
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SavingGoalUseCaseTest {
    private val repository = mockk<SavingGoalRepository>(relaxed = true)

    @Test fun create_trimsTitleAndPersistsGoal() = runTest {
        val useCase = SavingGoalUseCase(repository)
        useCase.create("  سفر  ", 1_000L, 100L, "✈️")

        coVerify { repository.insertGoal(match { it.title == "سفر" && it.targetAmount == 1_000L }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_rejectsZeroTarget() = runTest {
        SavingGoalUseCase(repository).create("سفر", 0L, 0L, "✈️")
    }

    @Test(expected = IllegalArgumentException::class)
    fun withdraw_rejectsZero() = runTest {
        SavingGoalUseCase(repository).withdraw("goal", 0L)
    }
}