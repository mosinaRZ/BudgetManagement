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

    // ---------------- یادآوری افزایشی (۴ ← ۸ ← ۱۶ روز) و سقف تعداد ----------------

    private fun setLastShown(key: String, millis: Long) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putLong("perm_reminder_last_shown_$key", millis)
            .commit()
    }

    private val day = 24L * 60 * 60 * 1000L

    @Test
    fun markShownNow_incrementsShownCount() {
        assertThat(PermissionReminderPreferences.shownCount(context, "SMS")).isEqualTo(0)
        PermissionReminderPreferences.markShownNow(context, "SMS")
        PermissionReminderPreferences.markShownNow(context, "SMS")
        assertThat(PermissionReminderPreferences.shownCount(context, "SMS")).isEqualTo(2)
    }

    @Test
    fun intervalFor_growsWithShownCount() {
        assertThat(PermissionReminderPreferences.intervalFor(1)).isEqualTo(4 * day)
        assertThat(PermissionReminderPreferences.intervalFor(2)).isEqualTo(8 * day)
        assertThat(PermissionReminderPreferences.intervalFor(3)).isEqualTo(16 * day)
    }

    @Test
    fun shouldRemindNow_secondInterval_isEightDays() {
        PermissionReminderPreferences.markShownNow(context, "SMS")
        PermissionReminderPreferences.markShownNow(context, "SMS")

        setLastShown("SMS", System.currentTimeMillis() - 7 * day)
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "SMS")).isFalse()

        setLastShown("SMS", System.currentTimeMillis() - 8 * day - 1_000L)
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "SMS")).isTrue()
    }

    @Test
    fun shouldRemindNow_afterMaxAutoReminders_returnsFalse() {
        repeat(PermissionReminderPreferences.MAX_AUTO_REMINDERS) {
            PermissionReminderPreferences.markShownNow(context, "SMS")
        }
        setLastShown("SMS", 0L)
        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "SMS")).isFalse()
    }

    @Test
    fun snooze_firstTime_remindsAfterOneDay() {
        PermissionReminderPreferences.markShownNow(context, "SMS")
        PermissionReminderPreferences.snooze(context, "SMS")

        val lastShown = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong("perm_reminder_last_shown_SMS", 0L)
        val dueIn = lastShown + PermissionReminderPreferences.intervalFor(1) - System.currentTimeMillis()
        assertThat(dueIn).isAtMost(PermissionReminderPreferences.SNOOZE_MILLIS)
        assertThat(dueIn).isGreaterThan(PermissionReminderPreferences.SNOOZE_MILLIS - 60_000L)
    }

    @Test
    fun snooze_afterSecondShow_keepsFullInterval() {
        PermissionReminderPreferences.markShownNow(context, "SMS")
        PermissionReminderPreferences.markShownNow(context, "SMS")
        PermissionReminderPreferences.snooze(context, "SMS")

        assertThat(PermissionReminderPreferences.shouldRemindNow(context, "SMS")).isFalse()
    }

    @Test
    fun wasRequested_roundTrip() {
        assertThat(PermissionReminderPreferences.wasRequested(context, "mic")).isFalse()
        PermissionReminderPreferences.markRequested(context, "mic")
        assertThat(PermissionReminderPreferences.wasRequested(context, "mic")).isTrue()
        assertThat(PermissionReminderPreferences.wasRequested(context, "sms")).isFalse()
    }
}