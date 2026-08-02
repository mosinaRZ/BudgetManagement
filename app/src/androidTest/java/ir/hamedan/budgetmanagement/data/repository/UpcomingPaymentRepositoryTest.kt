package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpcomingPaymentRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: UpcomingPaymentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UpcomingPaymentRepositoryImpl(database.upcomingPaymentDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun insertAndGetAllPayments() = runTest {
        val payment1 = UpcomingPaymentEntity(
            id = "p1",
            title = "اجاره خانه",
            amount = 15_000_000.0,
            dueDate = 1_700_000_000_000L,
            dueDay = 1,
            isPaid = false
        )
        val payment2 = UpcomingPaymentEntity(
            id = "p2",
            title = "قسط ماشین",
            amount = 8_000_000.0,
            dueDate = 1_700_100_000_000L,
            dueDay = 15,
            isPaid = false
        )

        repository.insertOrUpdatePayment(payment1)
        repository.insertOrUpdatePayment(payment2)

        val all = repository.allPayments.first()
        assertThat(all).hasSize(2)
        // مرتب‌سازی بر اساس dueDate صعودی
        assertThat(all[0].id).isEqualTo("p1")
        assertThat(all[1].id).isEqualTo("p2")
    }

    @Test
    fun togglePaymentStatus() = runTest {
        val payment = UpcomingPaymentEntity(
            id = "p1",
            title = "قبض برق",
            amount = 500_000.0,
            isPaid = false
        )
        repository.insertOrUpdatePayment(payment)

        repository.togglePaymentStatus("p1", currentStatus = false)

        val result = repository.allPayments.first().first()
        assertThat(result.isPaid).isTrue()

        // دوباره toggle کنیم
        repository.togglePaymentStatus("p1", currentStatus = true)
        val result2 = repository.allPayments.first().first()
        assertThat(result2.isPaid).isFalse()
    }

    @Test
    fun deletePayment() = runTest {
        val payment = UpcomingPaymentEntity(
            id = "p1",
            title = "اینترنت",
            amount = 300_000.0
        )
        repository.insertOrUpdatePayment(payment)

        repository.deletePayment("p1")

        val remaining = repository.allPayments.first()
        assertThat(remaining).isEmpty()
    }

    @Test
    fun insertOrUpdate_withSameId_replaces() = runTest {
        val original = UpcomingPaymentEntity(
            id = "p1",
            title = "اصلی",
            amount = 1_000_000.0,
            isPaid = false
        )
        repository.insertOrUpdatePayment(original)

        val updated = UpcomingPaymentEntity(
            id = "p1",
            title = "به‌روز شده",
            amount = 1_500_000.0,
            isPaid = true
        )
        repository.insertOrUpdatePayment(updated)

        val all = repository.allPayments.first()
        assertThat(all).hasSize(1)
        assertThat(all[0].title).isEqualTo("به‌روز شده")
        assertThat(all[0].amount).isEqualTo(1_500_000.0)
        assertThat(all[0].isPaid).isTrue()
    }
}