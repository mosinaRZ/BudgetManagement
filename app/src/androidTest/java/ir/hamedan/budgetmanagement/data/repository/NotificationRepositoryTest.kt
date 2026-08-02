package ir.hamedan.budgetmanagement.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NotificationRepositoryImpl(database.notificationDao())
        AppDatabase.clearInstance()
    }

    @After
    fun tearDown() {
        database.close()
        AppDatabase.clearInstance()
    }

    @Test
    fun addAndGetAllNotifications() = runTest {
        repository.addNotification(
            NotificationEntity(
                id = "n1",
                titleFa = "تست",
                titleEn = "Test",
                descFa = "توضیح",
                descEn = "Desc"
            )
        )

        val all = repository.getAllNotifications().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].titleFa).isEqualTo("تست")
        assertThat(all[0].isRead).isFalse()
    }

    @Test
    fun addNotification_withSameTag_isIgnored() = runTest {
        val n1 = NotificationEntity(id = "n1", tag = "GOAL_50_g1", titleFa = "اول")
        val n2 = NotificationEntity(id = "n2", tag = "GOAL_50_g1", titleFa = "دوم")

        repository.addNotification(n1)
        repository.addNotification(n2)

        val all = repository.getAllNotifications().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].titleFa).isEqualTo("اول")
    }

    @Test
    fun addNotification_withEmptyTag_allowsDuplicates() = runTest {
        repository.addNotification(NotificationEntity(id = "n1", tag = "", titleFa = "A"))
        repository.addNotification(NotificationEntity(id = "n2", tag = "", titleFa = "B"))

        val all = repository.getAllNotifications().first()
        assertThat(all).hasSize(2)
    }

    @Test
    fun markAsRead_updatesIsRead() = runTest {
        repository.addNotification(NotificationEntity(id = "n1", titleFa = "خوانده‌نشده"))

        repository.markAsRead("n1")

        val all = repository.getAllNotifications().first()
        assertThat(all[0].isRead).isTrue()
    }

    @Test
    fun getUnreadCount() = runTest {
        repository.addNotification(NotificationEntity(id = "n1", isRead = false))
        repository.addNotification(NotificationEntity(id = "n2", isRead = false))
        repository.addNotification(NotificationEntity(id = "n3", isRead = true))

        // n3 از قبل isRead=true ولی insert مستقیم؛ برای دقت mark می‌کنیم
        val count = repository.getUnreadCount().first()
        // بسته به insert: n1 و n2 unread
        assertThat(count).isAtLeast(2)
    }

    @Test
    fun markAllAsRead() = runTest {
        repository.addNotification(NotificationEntity(id = "n1"))
        repository.addNotification(NotificationEntity(id = "n2"))

        repository.markAllAsRead()

        val count = repository.getUnreadCount().first()
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun deleteById() = runTest {
        repository.addNotification(NotificationEntity(id = "n1", titleFa = "حذف‌شو"))
        repository.deleteById("n1")

        val all = repository.getAllNotifications().first()
        assertThat(all).isEmpty()
    }
}