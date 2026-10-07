package ir.hamedan.budgetmanagement.data.security

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.network.DeviceApi
import org.junit.Test

class NewDeviceDetectorTest {

    private val hour = 60L * 60L * 1000L
    private val now = 1_000L * hour

    private fun device(id: String, createdAt: Long) =
        DeviceApi.Device(id = id, name = id, lastSeenAt = createdAt, createdAt = createdAt)

    @Test
    fun reportsDevicesRegisteredAfterThisOne() {
        val devices = listOf(device("me", now - 10 * hour), device("tablet", now - hour))

        val result = NewDeviceDetector.evaluate(devices, "me", setOf("me"), now)

        assertThat(result.newDevices.map { it.id }).containsExactly("tablet")
        assertThat(result.acknowledgedIds).containsExactly("me", "tablet")
    }

    @Test
    fun neverReportsDevicesThatWereAlreadyOnTheAccountBeforeThisOne() {
        val devices = listOf(device("old", now - 50 * hour), device("me", now - 10 * hour))

        val result = NewDeviceDetector.evaluate(devices, "me", setOf("me"), now)

        assertThat(result.newDevices).isEmpty()
    }

    @Test
    fun doesNotReportTheSameDeviceTwice() {
        val devices = listOf(device("me", now - 10 * hour), device("tablet", now - hour))

        val result = NewDeviceDetector.evaluate(devices, "me", setOf("me", "tablet"), now)

        assertThat(result.newDevices).isEmpty()
    }

    @Test
    fun firstCheckOnlyReportsDevicesFromTheLastDay() {
        val devices = listOf(
            device("me", now - 100 * hour),
            device("last-week", now - 72 * hour),
            device("today", now - 2 * hour)
        )

        val result = NewDeviceDetector.evaluate(devices, "me", previouslyAcknowledged = null, nowMillis = now)

        assertThat(result.newDevices.map { it.id }).containsExactly("today")
        // The skipped older device is still acknowledged so it is never reported later.
        assertThat(result.acknowledgedIds).contains("last-week")
    }

    @Test
    fun reportsMultipleNewDevicesOldestFirst() {
        val devices = listOf(
            device("me", now - 10 * hour),
            device("b", now - hour),
            device("a", now - 3 * hour)
        )

        val result = NewDeviceDetector.evaluate(devices, "me", setOf("me"), now)

        assertThat(result.newDevices.map { it.id }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun aRemovedDeviceThatSignsInAgainIsReportedAgain() {
        val me = device("me", now - 10 * hour)

        // Tablet was acknowledged, then removed: the next check prunes it from the stored ids.
        val afterRemoval = NewDeviceDetector.evaluate(listOf(me), "me", setOf("me", "tablet"), now - 2 * hour)
        assertThat(afterRemoval.acknowledgedIds).containsExactly("me")

        val signedInAgain = NewDeviceDetector.evaluate(
            listOf(me, device("tablet", now - hour)), "me", afterRemoval.acknowledgedIds, now
        )
        assertThat(signedInAgain.newDevices.map { it.id }).containsExactly("tablet")
    }

    @Test
    fun leavesStateUntouchedWhenThisDeviceIsNotInTheList() {
        val result = NewDeviceDetector.evaluate(listOf(device("tablet", now)), "me", setOf("x"), now)

        assertThat(result.newDevices).isEmpty()
        assertThat(result.acknowledgedIds).containsExactly("x")
    }
}