package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionReminderPreferencesTest {

    private lateinit var context: Context
    private val day = 24L * 60 * 60 * 1000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    private fun clearPrefs() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun isDismissedForever_whenNeverSet_returnsFalse() {
        assertThat(PermissionReminderPreferences.isDismissedForever(context, "CAMERA")).isFalse()
    }

    @Test
    fun dismissForever_stopsReminders() {
        PermissionReminderPreferences.dismissForever(context, "CAMERA")
        assertThat(PermissionReminderPreferences.isDismissedForever(context, "CAMERA")).isTrue()
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "CAMERA")).isFalse()
    }

    @Test
    fun shouldRemindNow_whenNeverShown_returnsTrue() {
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "NOTIFICATIONS")).isTrue()
    }

    @Test
    fun shouldRemindNow_rightAfterShowing_returnsFalse() {
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS")
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "NOTIFICATIONS")).isFalse()
    }

    @Test
    fun shouldRemindNow_afterIntervalPasses_returnsTrue() {
        val now = 1_000_000_000_000L
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", nowMillis = now)
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "NOTIFICATIONS", now + 4 * day - 1)).isFalse()
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "NOTIFICATIONS", now + 4 * day)).isTrue()
    }

    @Test
    fun markShownNow_schedulesFollowingReminderAfterFourDays() {
        val now = 1_000_000_000_000L
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", nowMillis = now)
        assertThat(PermissionReminderPreferences.nextDueAt(context, "NOTIFICATIONS")).isEqualTo(now + 4 * day)
    }

    @Test
    fun intervals_growFromFourToEightToSixteenDays() {
        val now = 1_000_000_000_000L
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", now)
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", now)
        assertThat(PermissionReminderPreferences.nextDueAt(context, "NOTIFICATIONS")).isEqualTo(now + 8 * day)
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", now)
        assertThat(PermissionReminderPreferences.nextDueAt(context, "NOTIFICATIONS")).isEqualTo(now + 16 * day)
    }

    @Test
    fun afterMaxAutoReminders_noMoreAutomaticReminders() {
        repeat(PermissionReminderPreferences.MAX_AUTO_REMINDERS) {
            PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", nowMillis = 0L)
        }
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "NOTIFICATIONS", Long.MAX_VALUE)).isFalse()
    }

    @Test
    fun snooze_afterFirstShow_postponesByOneDay() {
        val now = 1_000_000_000_000L
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", now)
        PermissionReminderPreferences.snooze(context, "NOTIFICATIONS", now)
        assertThat(PermissionReminderPreferences.nextDueAt(context, "NOTIFICATIONS"))
            .isEqualTo(now + PermissionReminderPreferences.SNOOZE_MILLIS)
    }

    @Test
    fun snooze_afterSecondShow_keepsEightDayInterval() {
        val now = 1_000_000_000_000L
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", now)
        PermissionReminderPreferences.markShownNow(context, "NOTIFICATIONS", now)
        PermissionReminderPreferences.snooze(context, "NOTIFICATIONS", now)
        assertThat(PermissionReminderPreferences.nextDueAt(context, "NOTIFICATIONS")).isEqualTo(now + 8 * day)
    }

    @Test
    fun legacyState_isDerivedFromLastShownOnce() {
        val lastShown = 1_000_000_000_000L
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putLong("perm_reminder_last_shown_NOTIFICATIONS", lastShown)
            .putInt("perm_reminder_count_NOTIFICATIONS", 1)
            .commit()
        assertThat(PermissionReminderPreferences.nextDueAt(context, "NOTIFICATIONS")).isEqualTo(lastShown + 4 * day)
    }

    @Test
    fun wasRequested_roundTrip() {
        assertThat(PermissionReminderPreferences.wasRequested(context, "mic")).isFalse()
        PermissionReminderPreferences.markRequested(context, "mic")
        assertThat(PermissionReminderPreferences.wasRequested(context, "mic")).isTrue()
        assertThat(PermissionReminderPreferences.wasRequested(context, "notifications")).isFalse()
    }
}