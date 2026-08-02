package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.PendingStatus
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingTransactionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PendingTransactionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PendingTransactionRepositoryImpl(database.pendingTransactionDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun addPending_andGetPendingTransactions() = runTest {
        val pending = PendingTransactionEntity(
            id = "pt1",
            rawMessage = "برداشت مبلغ 1,500,000 ریال",
            amount = 150_000.0,
            isAmountDetected = true,
            type = "EXPENSE",
            isTypeDetected = true,
            suggestedTitle = "تراکنش پیامکی",
            timestamp = System.currentTimeMillis(),
            status = PendingStatus.PENDING
        )

        val added = repository.addPending(pending)
        assertThat(added).isTrue()

        val list = repository.getPendingTransactions().first()
        assertThat(list).hasSize(1)
        assertThat(list[0].id).isEqualTo("pt1")
        assertThat(list[0].amount).isEqualTo(150_000.0)
    }

    @Test
    fun addPending_rejectsDuplicateWithin2Minutes() = runTest {
        val now = System.currentTimeMillis()
        val pending1 = PendingTransactionEntity(
            id = "pt1",
            rawMessage = "برداشت مبلغ 500,000 ریال از کارت",
            amount = 50_000.0,
            timestamp = now,
            status = PendingStatus.PENDING
        )
        val pending2 = PendingTransactionEntity(
            id = "pt2",
            rawMessage = "برداشت مبلغ 500,000 ریال از کارت", // همان پیام
            amount = 50_000.0,
            timestamp = now + 30_000, // ۳۰ ثانیه بعد
            status = PendingStatus.PENDING
        )

        assertThat(repository.addPending(pending1)).isTrue()
        assertThat(repository.addPending(pending2)).isFalse() // باید رد شود

        val list = repository.getPendingTransactions().first()
        assertThat(list).hasSize(1)
    }

    @Test
    fun getPendingCount() = runTest {
        repository.addPending(
            PendingTransactionEntity(id = "1", rawMessage = "msg1", status = PendingStatus.PENDING)
        )
        repository.addPending(
            PendingTransactionEntity(id = "2", rawMessage = "msg2", status = PendingStatus.PENDING)
        )

        val count = repository.getPendingCount().first()
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun confirm_changesStatusAndRemovesFromPendingList() = runTest {
        val pending = PendingTransactionEntity(
            id = "pt1",
            rawMessage = "واریز 2,000,000",
            amount = 200_000.0,
            status = PendingStatus.PENDING
        )
        repository.addPending(pending)

        repository.confirm("pt1")

        val pendingList = repository.getPendingTransactions().first()
        assertThat(pendingList).isEmpty()

        val count = repository.getPendingCount().first()
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun ignore_changesStatusAndRemovesFromPendingList() = runTest {
        val pending = PendingTransactionEntity(
            id = "pt1",
            rawMessage = "پیامک تست",
            status = PendingStatus.PENDING
        )
        repository.addPending(pending)

        repository.ignore("pt1")

        val pendingList = repository.getPendingTransactions().first()
        assertThat(pendingList).isEmpty()
    }

    @Test
    fun delete_removesCompletely() = runTest {
        val pending = PendingTransactionEntity(
            id = "pt1",
            rawMessage = "برای حذف",
            status = PendingStatus.PENDING
        )
        repository.addPending(pending)

        repository.delete("pt1")

        val list = repository.getPendingTransactions().first()
        assertThat(list).isEmpty()
        assertThat(repository.getPendingCount().first()).isEqualTo(0)
    }
}