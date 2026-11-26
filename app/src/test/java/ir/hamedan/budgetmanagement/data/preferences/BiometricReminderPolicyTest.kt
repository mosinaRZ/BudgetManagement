package ir.hamedan.budgetmanagement.data.preferences

import ir.hamedan.budgetmanagement.data.preferences.BiometricReminderTrigger.RETURN_TO_APP
import ir.hamedan.budgetmanagement.data.preferences.BiometricReminderTrigger.SESSION_ENTRY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricReminderPolicyTest {

    private val minute = 60L * 1000

    @Test
    fun brandNewUserIsRemindedAtOnce() {
        // No waiting period and no minimum number of active days.
        assertTrue(BiometricReminderPolicy.shouldRemind(BiometricReminderState(), 0L, SESSION_ENTRY))
    }

    @Test
    fun everySessionEntryIsRemindedNoMatterHowOftenItWasShown() {
        var s = BiometricReminderState()
        var now = 1_000L
        repeat(50) {
            assertTrue(BiometricReminderPolicy.shouldRemind(s, now, SESSION_ENTRY))
            s = BiometricReminderPolicy.markShown(s, now)
            // Even straight after being shown, a new login shows it again.
            assertTrue(BiometricReminderPolicy.shouldRemind(s, now + 1, SESSION_ENTRY))
            now += minute
        }
        assertEquals(50, s.shownCount)
    }

    @Test
    fun returningToTheAppWaitsForTheShortInterval() {
        val shownAt = 10_000L
        val s = BiometricReminderPolicy.markShown(BiometricReminderState(), shownAt)
        val interval = BiometricReminderPolicy.RETURN_INTERVAL_MILLIS
        assertFalse(BiometricReminderPolicy.shouldRemind(s, shownAt + interval - 1, RETURN_TO_APP))
        assertTrue(BiometricReminderPolicy.shouldRemind(s, shownAt + interval, RETURN_TO_APP))
    }

    @Test
    fun returningToTheAppIsDueWhenNeverShown() {
        assertTrue(
            BiometricReminderPolicy.shouldRemind(
                BiometricReminderState(),
                BiometricReminderPolicy.RETURN_INTERVAL_MILLIS,
                RETURN_TO_APP
            )
        )
    }

    @Test
    fun markShownRecordsTimeAndCount() {
        val s = BiometricReminderPolicy.markShown(BiometricReminderState(shownCount = 2), 777L)
        assertEquals(777L, s.lastShownAt)
        assertEquals(3, s.shownCount)
    }

    @Test
    fun intervalIsShorterThanTheOldTwoDayMinimum() {
        // Guards against the reminder drifting back to a "once in a few days" cadence.
        assertTrue(BiometricReminderPolicy.RETURN_INTERVAL_MILLIS <= 60 * minute)
    }
}