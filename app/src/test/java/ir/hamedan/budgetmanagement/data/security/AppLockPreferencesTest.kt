package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class AppLockPreferencesTest {
    private lateinit var preferences: AppLockPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("budget_app_lock", Context.MODE_PRIVATE).edit().clear().commit()
        preferences = AppLockPreferences(context)
    }

    @Test fun locksAfterTwoMinutes() {
        preferences.markBackgrounded(1_000L)
        assertThat(preferences.shouldLockNow(120_999L)).isFalse()
        assertThat(preferences.shouldLockNow(121_000L)).isTrue()
    }

    @Test fun clearLockRemovesBackgroundLockState() {
        preferences.markBackgrounded(1_000L)
        preferences.markLockRequired()
        preferences.clearLock()

        assertThat(preferences.shouldLockNow(999_999L)).isFalse()
    }
    @Test fun timeSpentInForegroundDoesNotAccumulateTowardLockTimeout() {
        preferences.markBackgrounded(1_000L)
        assertThat(preferences.shouldLockNow(30_000L)).isFalse()

        // The app resumes before the timeout; its previous background interval is discarded.
        preferences.markForegrounded()
        assertThat(preferences.shouldLockNow(500_000L)).isFalse()
    }

    @Test fun expiredBackgroundIntervalStillRequiresLockAfterResume() {
        preferences.markBackgrounded(1_000L)
        if (preferences.shouldLockNow(121_000L)) {
            preferences.markLockRequired()
        }
        preferences.markForegrounded()

        assertThat(preferences.shouldLockNow(500_000L)).isTrue()
    }

}