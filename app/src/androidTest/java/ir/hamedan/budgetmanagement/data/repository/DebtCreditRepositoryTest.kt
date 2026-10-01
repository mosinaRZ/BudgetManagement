package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.testing.FakeSyncLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DebtCreditRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: DebtCreditRepository
    private lateinit var sync: FakeSyncLocalDataSource

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        sync = FakeSyncLocalDataSource()
        repository = DebtCreditRepositoryImpl(db.debtCreditDao(), db.debtPaymentDao(), sync)
    }

    @After fun tearDown() = db.close()

    @Test fun paymentDelta_isRecordedAsLedgerOperation() = runTest {
        repository.insertOrUpdate(DebtCreditEntity(id="d1", type="DEBT", personName="Ali", totalAmount=10_000L))
        repository.insertOrUpdate(DebtCreditEntity(id="d1", type="DEBT", personName="Ali", totalAmount=10_000L, paidAmount=3_000L))

        assertThat(repository.getById("d1")!!.paidAmount).isEqualTo(3_000L)
        assertThat(db.debtPaymentDao().getByDebtCreditId("d1")).hasSize(1)
        assertThat(db.debtPaymentDao().getByDebtCreditId("d1").single().deltaAmount).isEqualTo(3_000L)
    }

    @Test fun delete_createsTombstoneAndRemovesPayments() = runTest {
        repository.insertOrUpdate(DebtCreditEntity(id="d1", type="DEBT", personName="Ali", totalAmount=10_000L, paidAmount=1_000L))
        repository.deleteById("d1")

        assertThat(repository.getAllDebtCredits().first()).isEmpty()
        assertThat(db.debtPaymentDao().getByDebtCreditId("d1")).isEmpty()
        assertThat(sync.mutations.any { it.entityId == "d1" && it.isDeleted }).isTrue()
    }
}