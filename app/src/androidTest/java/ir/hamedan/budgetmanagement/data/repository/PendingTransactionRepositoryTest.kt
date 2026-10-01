package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.PendingStatus
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PendingTransactionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: PendingTransactionRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        repository = PendingTransactionRepositoryImpl(db.pendingTransactionDao())
    }

    @After fun tearDown() = db.close()

    @Test fun insertAndStatusUpdate_work() = runTest {
        repository.insert(PendingTransactionEntity(id="p1", rawMessage="SMS", amount=150_000L, suggestedTitle="خرید"))
        assertThat(repository.getPendingTransactions().first().single().amount).isEqualTo(150_000L)

        repository.updateStatus("p1", PendingStatus.CONFIRMED)
        assertThat(repository.getPendingTransactions().first()).isEmpty()
    }

    @Test fun duplicateCounter_countsRecentMessages() = runTest {
        val now = System.currentTimeMillis()
        repository.insert(PendingTransactionEntity(id="p1", rawMessage="same", amount=100L, timestamp=now))
        repository.insert(PendingTransactionEntity(id="p2", rawMessage="same", amount=100L, timestamp=now + 30_000L))

        assertThat(repository.countRecentDuplicates("same", now - 1_000L)).isEqualTo(2)
    }
}