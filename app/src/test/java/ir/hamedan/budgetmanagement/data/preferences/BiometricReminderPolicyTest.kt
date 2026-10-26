package ir.hamedan.budgetmanagement.data.preferences

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BiometricReminderPolicyTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    private fun usedOnDays(vararg epochDays: Long): BiometricReminderState =
        epochDays.fold(BiometricReminderState()) { state, d -> BiometricReminderPolicy.recordActiveDay(state, d) }

    @Test
    fun aNewUserIsNeverAskedBeforeUsingTheAppOnSeveralDays() {
        val first = usedOnDays(100)
        assertThat(BiometricReminderPolicy.shouldRemind(first, now)).isFalse()

        val two = usedOnDays(100, 101)
        assertThat(BiometricReminderPolicy.shouldRemind(two, now)).isFalse()

        val three = usedOnDays(100, 101, 105)
        assertThat(three.activeDays).isEqualTo(3)
        assertThat(BiometricReminderPolicy.shouldRemind(three, now)).isTrue()
    }

    @Test
    fun openingTheAppSeveralTimesInOneDayCountsOnce() {
        val state = usedOnDays(100, 100, 100, 100)
        assertThat(state.activeDays).isEqualTo(1)
    }

    @Test
    fun eachReminderPushesTheNextOneFurtherAway() {
        var state = usedOnDays(1, 2, 3)

        state = BiometricReminderPolicy.markShown(state, now)
        assertThat(state.shownCount).isEqualTo(1)
        assertThat(state.nextDueAt).isEqualTo(now + 7 * day)
        assertThat(BiometricReminderPolicy.shouldRemind(state, now + 7 * day - 1)).isFalse()
        assertThat(BiometricReminderPolicy.shouldRemind(state, now + 7 * day)).isTrue()

        state = BiometricReminderPolicy.markShown(state, now + 7 * day)
        assertThat(state.nextDueAt).isEqualTo(now + 7 * day + 14 * day)
    }

    @Test
    fun itStopsAfterTheMaximumNumberOfReminders() {
        var state = usedOnDays(1, 2, 3)
        var time = now
        repeat(BiometricReminderPolicy.MAX_REMINDERS) {
            assertThat(BiometricReminderPolicy.shouldRemind(state, time)).isTrue()
            state = BiometricReminderPolicy.markShown(state, time)
            time += 365 * day
        }
        assertThat(state.shownCount).isEqualTo(BiometricReminderPolicy.MAX_REMINDERS)
        assertThat(BiometricReminderPolicy.shouldRemind(state, time)).isFalse()
    }

    @Test
    fun enablingOrAskingNotToBeRemindedStopsItForGood() {
        val state = BiometricReminderPolicy.stop(usedOnDays(1, 2, 3, 4, 5))
        assertThat(BiometricReminderPolicy.shouldRemind(state, now + 1000 * day)).isFalse()
    }

    @Test
    fun intervalsGrow() {
        assertThat(BiometricReminderPolicy.intervalAfter(1)).isEqualTo(7 * day)
        assertThat(BiometricReminderPolicy.intervalAfter(2)).isEqualTo(14 * day)
        assertThat(BiometricReminderPolicy.intervalAfter(3)).isEqualTo(30 * day)
    }
}