package ir.hamedan.budgetmanagement.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import ir.hamedan.budgetmanagement.data.preferences.AppUsagePreferences
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
// A plain Application: the real BudgetApp would build the SQLCipher database, and its native
// library is not available on the JVM. Same convention as the other Robolectric tests.
@Config(sdk = [35], application = Application::class)
class InactivityReminderWorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        // جلوگیری از ClassCastException
        mockkObject(NotificationHelper)
        // Workers use the awaiting variant (see NotificationHelper.sendAndAwait).
        coEvery {
            NotificationHelper.sendAndAwait(any(), any(), any(), any(), any(), any(), any(), any())
        } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `returns success when lastOpen is 0`() = runBlocking {
        val worker = TestListenableWorkerBuilder<InactivityReminderWorker>(context).build()
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `notifies at 3 days threshold`() = runBlocking {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_open_timestamp", threeDaysAgo)
            .putInt("last_notified_days", 0)
            .commit()

        val worker = TestListenableWorkerBuilder<InactivityReminderWorker>(context).build()
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(3)
    }

    @Test
    fun `does not notify same threshold twice`() = runBlocking {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_open_timestamp", sevenDaysAgo)
            .putInt("last_notified_days", 7)
            .commit()

        val worker = TestListenableWorkerBuilder<InactivityReminderWorker>(context).build()
        worker.doWork()

        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(7)
    }

    @Test
    fun `picks highest applicable threshold`() = runBlocking {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_open_timestamp", thirtyDaysAgo)
            .putInt("last_notified_days", 0)
            .commit()

        val worker = TestListenableWorkerBuilder<InactivityReminderWorker>(context).build()
        worker.doWork()

        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(30)
    }

    @Test
    fun `does not record the threshold when the notification could not be stored`() = runBlocking {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_open_timestamp", threeDaysAgo)
            .putInt("last_notified_days", 0)
            .commit()
        coEvery {
            NotificationHelper.sendAndAwait(any(), any(), any(), any(), any(), any(), any(), any())
        } throws IllegalStateException("database unavailable")

        val worker = TestListenableWorkerBuilder<InactivityReminderWorker>(context).build()
        val result = worker.doWork()

        // The next daily run must be able to try the same threshold again.
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(0)
    }

    @Test
    fun `notifies again after the user opened the app and became inactive again`() = runBlocking {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_open_timestamp", threeDaysAgo)
            .putInt("last_notified_days", 3)
            .commit()

        // A real user launch starts a new inactivity cycle ...
        AppUsagePreferences.markOpened(context)
        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(0)

        // ... and 3 days later the 3-day reminder is allowed again.
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_open_timestamp", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3))
            .commit()
        val worker = TestListenableWorkerBuilder<InactivityReminderWorker>(context).build()
        worker.doWork()

        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(3)
    }
}