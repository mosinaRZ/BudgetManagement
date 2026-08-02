package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TransactionRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = TransactionRepositoryImpl(database.transactionDao())
        AppDatabase.clearInstance()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun insertAndGetAllTransactions() = runTest {
        val tx1 = TransactionEntity(
            id = "tx-1",
            title = "حقوق",
            amount = 50_000_000.0,
            category = "SALARY",
            type = "INCOME",
            timestamp = 1_700_000_000_000L,
            note = "ماهانه"
        )
        val tx2 = TransactionEntity(
            id = "tx-2",
            title = "خرید مواد غذایی",
            amount = 1_250_000.0,
            category = "FOOD",
            type = "EXPENSE",
            timestamp = 1_700_100_000_000L,
            note = ""
        )

        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        val all = repository.getAllTransactions().first()

        assertThat(all).hasSize(2)
        assertThat(all[0].id).isEqualTo("tx-2")
        assertThat(all[1].id).isEqualTo("tx-1")
        assertThat(all[1].amount).isEqualTo(50_000_000.0)
    }

    @Test
    fun deleteTransactionById_removesOnlyTarget() = runTest {
        val tx1 = TransactionEntity(id = "tx-1", title = "A", amount = 100.0, type = "EXPENSE")
        val tx2 = TransactionEntity(id = "tx-2", title = "B", amount = 200.0, type = "INCOME")

        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        repository.deleteTransactionById("tx-1")

        val remaining = repository.getAllTransactions().first()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].id).isEqualTo("tx-2")
    }

    @Test
    fun getTransactionCountForCategory() = runTest {
        repository.insertTransaction(
            TransactionEntity(id = "1", category = "FOOD", amount = 100.0, type = "EXPENSE")
        )
        repository.insertTransaction(
            TransactionEntity(id = "2", category = "FOOD", amount = 200.0, type = "EXPENSE")
        )
        repository.insertTransaction(
            TransactionEntity(id = "3", category = "TRANSPORT", amount = 50.0, type = "EXPENSE")
        )

        assertThat(repository.getTransactionCountForCategory("FOOD")).isEqualTo(2)
        assertThat(repository.getTransactionCountForCategory("TRANSPORT")).isEqualTo(1)
        assertThat(repository.getTransactionCountForCategory("UNKNOWN")).isEqualTo(0)
    }

    @Test
    fun reassignCategoryForTransactions() = runTest {
        repository.insertTransaction(
            TransactionEntity(id = "1", category = "OLD_FOOD", amount = 100.0, type = "EXPENSE")
        )
        repository.insertTransaction(
            TransactionEntity(id = "2", category = "OLD_FOOD", amount = 200.0, type = "EXPENSE")
        )
        repository.insertTransaction(
            TransactionEntity(id = "3", category = "TRANSPORT", amount = 50.0, type = "EXPENSE")
        )

        repository.reassignCategoryForTransactions("OLD_FOOD", "FOOD")

        val all = repository.getAllTransactions().first()
        assertThat(all.filter { it.category == "FOOD" }).hasSize(2)
        assertThat(all.filter { it.category == "TRANSPORT" }).hasSize(1)
        assertThat(all.none { it.category == "OLD_FOOD" }).isTrue()
    }

    @Test
    fun getTransactionsBetween_filtersByTimestamp() = runTest {
        val t1 = 1_700_000_000_000L
        val t2 = 1_700_100_000_000L
        val t3 = 1_700_200_000_000L

        repository.insertTransaction(TransactionEntity(id = "1", amount = 10.0, type = "EXPENSE", timestamp = t1))
        repository.insertTransaction(TransactionEntity(id = "2", amount = 20.0, type = "INCOME", timestamp = t2))
        repository.insertTransaction(TransactionEntity(id = "3", amount = 30.0, type = "EXPENSE", timestamp = t3))

        val between = repository.getTransactionsBetween(t1, t2)
        assertThat(between).hasSize(2)
        assertThat(between.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun getBalanceBefore_excludesTransactionAtExactBoundary() = runTest {
        // Query: timestamp < :beforeDate  → خود مرز شمرده نمی‌شود
        val boundary = 1_700_100_000_000L

        repository.insertTransaction(
            TransactionEntity(
                id = "at-boundary",
                title = "x",
                amount = 500_000.0,
                type = "INCOME",
                category = "SALARY",
                timestamp = boundary
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                id = "before",
                title = "y",
                amount = 100_000.0,
                type = "INCOME",
                category = "SALARY",
                timestamp = boundary - 1
            )
        )

        assertThat(repository.getBalanceBefore(boundary)).isEqualTo(100_000.0)
    }

    @Test
    fun getBalanceBefore_onlyExpenses_returnsNegative() = runTest {
        val before = 1_700_200_000_000L
        repository.insertTransaction(
            TransactionEntity(
                id = "e1",
                title = "e",
                amount = 40_000.0,
                type = "EXPENSE",
                category = "FOOD",
                timestamp = 1_700_000_000_000L
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                id = "e2",
                title = "e2",
                amount = 10_000.0,
                type = "EXPENSE",
                category = "FOOD",
                timestamp = 1_700_100_000_000L
            )
        )

        assertThat(repository.getBalanceBefore(before)).isEqualTo(-50_000.0)
    }

    @Test
    fun getTransactionsBetween_isInclusiveOnBothEnds() = runTest {
        val start = 1_700_000_000_000L
        val end = 1_700_100_000_000L

        repository.insertTransaction(
            TransactionEntity(id = "s", title = "s", amount = 1.0, type = "EXPENSE", category = "FOOD", timestamp = start)
        )
        repository.insertTransaction(
            TransactionEntity(id = "e", title = "e", amount = 2.0, type = "EXPENSE", category = "FOOD", timestamp = end)
        )
        repository.insertTransaction(
            TransactionEntity(id = "after", title = "a", amount = 3.0, type = "EXPENSE", category = "FOOD", timestamp = end + 1)
        )
        repository.insertTransaction(
            TransactionEntity(id = "before", title = "b", amount = 4.0, type = "EXPENSE", category = "FOOD", timestamp = start - 1)
        )

        val between = repository.getTransactionsBetween(start, end)
        assertThat(between.map { it.id }).containsExactly("s", "e").inOrder()
    }

    @Test
    fun getTransactionsBetween_emptyRange_returnsEmpty() = runTest {
        repository.insertTransaction(
            TransactionEntity(
                id = "1",
                title = "x",
                amount = 10.0,
                type = "INCOME",
                category = "SALARY",
                timestamp = 1_700_000_000_000L
            )
        )
        val result = repository.getTransactionsBetween(1_800_000_000_000L, 1_900_000_000_000L)
        assertThat(result).isEmpty()
    }

    @Test
    fun getBalanceBefore_farInPast_returnsZero() = runTest {
        repository.insertTransaction(
            TransactionEntity(
                id = "1",
                title = "x",
                amount = 999.0,
                type = "INCOME",
                category = "SALARY",
                timestamp = 1_700_000_000_000L
            )
        )
        assertThat(repository.getBalanceBefore(1_000_000_000_000L)).isEqualTo(0.0)
    }

    @Test
    fun getBalanceBefore_calculatesCorrectly() = runTest {
        val before = 1_700_150_000_000L

        repository.insertTransaction(
            TransactionEntity(id = "inc1", amount = 1_000_000.0, type = "INCOME", timestamp = 1_700_000_000_000L)
        )
        repository.insertTransaction(
            TransactionEntity(id = "exp1", amount = 300_000.0, type = "EXPENSE", timestamp = 1_700_100_000_000L)
        )
        repository.insertTransaction(
            TransactionEntity(id = "later", amount = 999_999.0, type = "INCOME", timestamp = 1_700_200_000_000L)
        )

        val balance = repository.getBalanceBefore(before)
        assertThat(balance).isEqualTo(700_000.0)
    }

    @Test
    fun getBalanceBefore_emptyDatabase_returnsZero() = runTest {
        assertThat(repository.getBalanceBefore(System.currentTimeMillis())).isEqualTo(0.0)
    }

    @Test
    fun insertWithSameId_replacesExisting() = runTest {
        repository.insertTransaction(
            TransactionEntity(id = "same-id", title = "Original", amount = 100.0, type = "EXPENSE")
        )
        repository.insertTransaction(
            TransactionEntity(id = "same-id", title = "Updated", amount = 250.0, type = "INCOME")
        )

        val all = repository.getAllTransactions().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].title).isEqualTo("Updated")
        assertThat(all[0].amount).isEqualTo(250.0)
        assertThat(all[0].type).isEqualTo("INCOME")
    }
}