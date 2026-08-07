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
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun isDismissedForever_whenNeverSet_returnsFalse() {
        assertThat(PermissionReminderPreferences.isDismissedForever(context, "CAMERA")).isFalse()
    }

    @Test
    fun isDismissedForever_afterDismissForever_returnsTrue() {
        PermissionReminderPreferences.dismissForever(context, "CAMERA")
        assertThat(PermissionReminderPreferences.isDismissedForever(context, "CAMERA")).isTrue()
    }

    @Test
    fun shouldRemindNow_whenNeverSetAndNotDismissed_returnsTrue() {
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "CAMERA")).isTrue()
    }

    @Test
    fun shouldRemindNow_whenDismissedForever_returnsFalse() {
        PermissionReminderPreferences.dismissForever(context, "CAMERA")
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "CAMERA")).isFalse()
    }

    @Test
    fun shouldRemindNow_whenLastShownRecent_returnsFalse() {
        PermissionReminderPreferences.markShownNow(context, "CAMERA")
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "CAMERA")).isFalse()
    }

    @Test
    fun shouldRemindNow_whenLastShownOld_returnsTrue() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putLong("perm_reminder_last_shown_CAMERA", 0L)
            .commit()

        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "CAMERA")).isTrue()
    }

    @Test
    fun markShownNow_updatesLastShown() {
        PermissionReminderPreferences.markShownNow(context, "CAMERA")
        val lastShown = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong("perm_reminder_last_shown_CAMERA", 0L)
        assertThat(lastShown).isGreaterThan(0L)
    }

    @Test
    fun snooze_setsLastShownToRecentValue() {
        PermissionReminderPreferences.snooze(context, "CAMERA")

        val lastShown = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong("perm_reminder_last_shown_CAMERA", 0L)
        assertThat(lastShown).isGreaterThan(0L)
    }

    @Test
    fun REMINDER_INTERVAL_MILLIS_isCorrect() {
        assertThat(PermissionReminderPreferences.REMINDER_INTERVAL_MILLIS).isEqualTo(4L * 24 * 60 * 60 * 1000L)
    }

    @Test
    fun SNOOZE_MILLIS_isCorrect() {
        assertThat(PermissionReminderPreferences.SNOOZE_MILLIS).isEqualTo(1L * 24 * 60 * 60 * 1000L)
    }
}