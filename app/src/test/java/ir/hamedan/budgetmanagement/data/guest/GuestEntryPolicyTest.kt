package ir.hamedan.budgetmanagement.data.guest

import org.junit.Assert.assertEquals
import org.junit.Test

class GuestEntryPolicyTest {

    @Test
    fun freshDeviceStartsGuestWorkspace() {
        assertEquals(GuestEntryDecision.START, GuestEntryPolicy.decide(accountUserId = null, syncRequired = false))
        assertEquals(GuestEntryDecision.START, GuestEntryPolicy.decide(accountUserId = null, syncRequired = true))
    }

    @Test
    fun unsyncedAccountDataBlocksGuestEntry() {
        assertEquals(
            GuestEntryDecision.BLOCKED_UNSYNCED_ACCOUNT,
            GuestEntryPolicy.decide(accountUserId = "user-1", syncRequired = true)
        )
    }

    @Test
    fun fullySyncedLeftoverAccountIsWipedBeforeGuestEntry() {
        assertEquals(
            GuestEntryDecision.START_AFTER_WIPING_SYNCED_ACCOUNT,
            GuestEntryPolicy.decide(accountUserId = "user-1", syncRequired = false)
        )
    }
}