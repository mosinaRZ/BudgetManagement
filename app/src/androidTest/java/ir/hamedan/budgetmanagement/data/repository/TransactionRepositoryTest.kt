package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.testing.FakeSyncLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class TransactionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var sync: FakeSyncLocalDataSource

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sync = FakeSyncLocalDataSource()
        repository = TransactionRepositoryImpl(db.transactionDao(), sync)
    }

    @After fun tearDown() = db.close()

    @Test fun insertAndQuery_arePersistedAndOrdered() = runTest {
        repository.insertTransaction(TransactionEntity(id="t1", title="Salary", amount=5_000L, categoryId="salary", type="INCOME", timestamp=1_000L))
        repository.insertTransaction(TransactionEntity(id="t2", title="Food", amount=500L, categoryId="food", type="EXPENSE", timestamp=2_000L))

        val all = repository.getAllTransactions().first()
        assertThat(all.map { it.id }).containsExactly("t2", "t1").inOrder()
        assertThat(repository.getCurrentBalance()).isEqualTo(4_500L)
        assertThat(sync.mutations.map { it.entityId }).containsExactly("t1", "t2")
    }

    @Test fun delete_createsTombstone() = runTest {
        repository.insertTransaction(TransactionEntity(id="t1", amount=100L, categoryId="food"))
        repository.deleteTransactionById("t1")

        assertThat(repository.getAllTransactions().first()).isEmpty()
        assertThat(sync.mutations.last().isDeleted).isTrue()
    }

    @Test fun invalidRange_isRejected() = runTest {
        var failed = false
        try { repository.getTransactionsBetween(20L, 10L) } catch (_: IllegalArgumentException) { failed = true }
        assertThat(failed).isTrue()
    }
}