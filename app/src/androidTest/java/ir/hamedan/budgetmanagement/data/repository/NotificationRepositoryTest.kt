package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: NotificationRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        repository = NotificationRepositoryImpl(db.notificationDao())
    }

    @After fun tearDown() = db.close()

    @Test fun insertAndUnreadCount_work() = runTest {
        repository.insert(NotificationEntity(id="n1", titleFa="سلام", tag="welcome"))
        repository.insert(NotificationEntity(id="n2", titleFa="هشدار", tag="warning"))

        assertThat(repository.getAllNotifications().first()).hasSize(2)
        assertThat(repository.getUnreadCount().first()).isEqualTo(2)
        assertThat(repository.countByTag("welcome")).isEqualTo(1)

        repository.markAsRead("n1")
        assertThat(repository.getUnreadCount().first()).isEqualTo(1)
    }

    @Test fun markAllAsRead_andDelete_work() = runTest {
        repository.insert(NotificationEntity(id="n1"))
        repository.insert(NotificationEntity(id="n2"))
        repository.markAllAsRead()
        assertThat(repository.getUnreadCount().first()).isEqualTo(0)

        repository.deleteById("n1")
        assertThat(repository.getAllNotifications().first().map { it.id }).containsExactly("n2")
    }
}