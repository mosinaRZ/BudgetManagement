package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebtCreditRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DebtCreditRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DebtCreditRepositoryImpl(database.debtCreditDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun insertAndGetAllDebtCredits() = runTest {
        val debt = DebtCreditEntity(
            id = "d1",
            type = "DEBT",
            personName = "مهدی",
            totalAmount = 50000.0,
            paidAmount = 0.0,
            isMonthly = false,
            monthlyAmount = 0.0,
            dueDay = 5,
            dueDateMillis = System.currentTimeMillis() + 86400000L * 30,
            note = "وام شخصی",
            isSettled = false
        )

        repository.insertOrUpdateDebtCredit(debt)

        val all = repository.allDebtCredits.first()
        assertThat(all).hasSize(1)
        assertThat(all[0].personName).isEqualTo("مهدی")
    }

    @Test
    fun deleteDebtCredit_deletes() = runTest {
        val debt = DebtCreditEntity(id = "d1", type = "DEBT", personName = "مهدی", totalAmount = 50000.0)
        repository.insertOrUpdateDebtCredit(debt)

        repository.deleteDebtCredit("d1")

        val all = repository.allDebtCredits.first()
        assertThat(all).isEmpty()
    }

    @Test
    fun toggleSettledStatus_togglesAndUpdatesPaidAmount() = runTest {
        val debt = DebtCreditEntity(id = "d1", type = "DEBT", personName = "مهدی", totalAmount = 50000.0, paidAmount = 0.0, isSettled = false)
        repository.insertOrUpdateDebtCredit(debt)

        repository.toggleSettledStatus("d1", false)

        val updated = repository.allDebtCredits.first().first()
        assertThat(updated.isSettled).isTrue()
        assertThat(updated.paidAmount).isEqualTo(50000.0)
    }

    @Test
    fun getAllDebtCredits_returnsAllItems() = runTest {
        repository.insertOrUpdateDebtCredit(DebtCreditEntity(id = "d1", type = "DEBT", personName = "مهدی", totalAmount = 50000.0))
        repository.insertOrUpdateDebtCredit(DebtCreditEntity(id = "c1", type = "CREDIT", personName = "سارا", totalAmount = 200000.0))

        val all = repository.allDebtCredits.first()
        assertThat(all).hasSize(2)
        assertThat(all.map { it.type }).containsExactly("DEBT", "CREDIT")
    }
}