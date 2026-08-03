package ir.hamedan.budgetmanagement.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import ir.hamedan.budgetmanagement.data.preferences.AppUsagePreferences
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class InactivityReminderWorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        // جلوگیری از ClassCastException
        mockkObject(NotificationHelper)
        every {
            NotificationHelper.send(any(), any(), any(), any(), any(), any(), any())
        } just Runs
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
}