package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun `updateLastOpen stores current timestamp`() {
        val before = System.currentTimeMillis()
        AppUsagePreferences.updateLastOpen(context)
        val stored = AppUsagePreferences.getLastOpen(context)
        val after = System.currentTimeMillis()

        assertThat(stored).isAtLeast(before)
        assertThat(stored).isAtMost(after)
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