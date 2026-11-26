package ir.hamedan.budgetmanagement.data.preferences

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// A plain Application: the real BudgetApp would build the SQLCipher database, and its native
// library is not available on the JVM. Same convention as the other Robolectric tests.
@Config(sdk = [35], application = Application::class)
class AppUsagePreferencesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // پاک کردن state قبلی
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `markOpened stores current timestamp`() {
        val before = System.currentTimeMillis()
        AppUsagePreferences.markOpened(context)
        val stored = AppUsagePreferences.getLastOpen(context)
        val after = System.currentTimeMillis()

        assertThat(stored).isAtLeast(before)
        assertThat(stored).isAtMost(after)
    }

    @Test
    fun `markOpened resets the notified threshold so reminders can fire again`() {
        AppUsagePreferences.setLastNotifiedDays(context, 15)

        AppUsagePreferences.markOpened(context)

        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(0)
    }

    @Test
    fun `getLastOpen returns 0 when never set`() {
        assertThat(AppUsagePreferences.getLastOpen(context)).isEqualTo(0L)
    }

    @Test
    fun `set and get lastNotifiedDays works`() {
        AppUsagePreferences.setLastNotifiedDays(context, 7)
        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(7)
    }

    @Test
    fun `getLastNotifiedDays defaults to 0`() {
        assertThat(AppUsagePreferences.getLastNotifiedDays(context)).isEqualTo(0)
    }
}