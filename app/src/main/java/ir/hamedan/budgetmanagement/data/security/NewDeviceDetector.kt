package ir.hamedan.budgetmanagement.data.security

import ir.hamedan.budgetmanagement.data.network.DeviceApi

/**
 * Pure decision logic for "a new device signed in to my account" alerts, kept free of Android
 * so it can be unit tested on the JVM.
 *
 * A device is "new" from the point of view of THIS device when it
 *  - is not this device,
 *  - was registered on the account after this device was, and
 *  - has not been alerted about before.
 *
 * Devices registered before this one are never reported: they were already part of the account
 * when this device signed in, so the user knew about them.
 */
object NewDeviceDetector {

    /**
     * The very first check on an install (nothing acknowledged yet) only reports devices from the
     * last day, so updating the app does not flood the user with alerts about old devices.
     */
    const val FIRST_CHECK_WINDOW_MS: Long = 24L * 60L * 60L * 1000L

    data class Result(
        /** Devices to alert about, oldest first. */
        val newDevices: List<DeviceApi.Device>,
        /**
         * Ids to persist as handled. It is exactly the current device list, so a device that is
         * removed and later signs in again is reported again.
         */
        val acknowledgedIds: Set<String>
    )

    /**
     * @param previouslyAcknowledged ids stored by the previous check, or null if there was none.
     */
    fun evaluate(
        devices: List<DeviceApi.Device>,
        currentDeviceId: String,
        previouslyAcknowledged: Set<String>?,
        nowMillis: Long
    ): Result {
        val self = devices.firstOrNull { it.id == currentDeviceId }
        // This device is not in the list (removed, or the list is not about this session):
        // nothing can be said reliably, so keep the stored state untouched.
            ?: return Result(emptyList(), previouslyAcknowledged.orEmpty())

        val known = previouslyAcknowledged.orEmpty()
        val firstCheck = previouslyAcknowledged == null

        val fresh = devices
            .asSequence()
            .filter { it.id != currentDeviceId }
            .filter { it.createdAt > self.createdAt }
            .filter { it.id !in known }
            .filter { !firstCheck || nowMillis - it.createdAt <= FIRST_CHECK_WINDOW_MS }
            .sortedBy { it.createdAt }
            .toList()

        return Result(newDevices = fresh, acknowledgedIds = devices.map { it.id }.toSet())
    }
}