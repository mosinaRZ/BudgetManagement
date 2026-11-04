package ir.hamedan.budgetmanagement.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricReminderPolicyTest {

    private val day = 24L * 60 * 60 * 1000

    @Test
    fun waitsForMinimumActiveDays() {
        var s = BiometricReminderState()
        s = BiometricReminderPolicy.recordActiveDay(s, 100)
        assertFalse(BiometricReminderPolicy.shouldRemind(s, 0))
        s = BiometricReminderPolicy.recordActiveDay(s, 101)
        assertTrue(BiometricReminderPolicy.shouldRemind(s, 0))
    }

    @Test
    fun intervalsGrowSlowlyAndStopAtMax() {
        var s = BiometricReminderState(activeDays = 5)
        val expected = listOf(2, 3, 5, 7, 7, 7, 7, 7)
        var now = 1_000L
        expected.forEach { days ->
            assertTrue(BiometricReminderPolicy.shouldRemind(s, now))
            s = BiometricReminderPolicy.markShown(s, now)
            assertEquals(now + days * day, s.nextDueAt)
            assertFalse(BiometricReminderPolicy.shouldRemind(s, now + days * day - 1))
            now = s.nextDueAt
        }
        assertFalse(BiometricReminderPolicy.shouldRemind(s, now + 100 * day))
    }

    @Test
    fun stopIsPermanent() {
        val s = BiometricReminderPolicy.stop(BiometricReminderState(activeDays = 5))
        assertFalse(BiometricReminderPolicy.shouldRemind(s, Long.MAX_VALUE))
    }

    @Test
    fun capNextDuePullsInOldLongDelaysOnlyOnce() {
        val now = 10 * day
        val legacy = BiometricReminderState(activeDays = 9, shownCount = 2, nextDueAt = now + 30 * day)
        val capped = BiometricReminderPolicy.capNextDue(legacy, now)
        assertEquals(now + 3 * day, capped.nextDueAt)
        // Idempotent: a later call (time moved on) must not push the date away again.
        assertEquals(capped, BiometricReminderPolicy.capNextDue(capped, now + day))
    }
}