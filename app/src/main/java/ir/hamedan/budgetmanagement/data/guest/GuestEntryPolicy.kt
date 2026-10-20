package ir.hamedan.budgetmanagement.data.guest

enum class GuestEntryDecision {
    /** Start the guest workspace on top of whatever local guest data exists. */
    START,

    /**
     * The device still holds a finished account session whose data was fully synced and cleared
     * from the account. Its leftovers must not appear inside the guest workspace, so they are wiped.
     */
    START_AFTER_WIPING_SYNCED_ACCOUNT,

    /**
     * The device holds account data that never reached the server. Entering the guest workspace
     * would hide or mix that data, so the user must sign in to that account and let it sync first.
     */
    BLOCKED_UNSYNCED_ACCOUNT
}

/** Pure decision logic for guest entry, kept free of Android types so it can be unit tested. */
object GuestEntryPolicy {
    fun decide(accountUserId: String?, syncRequired: Boolean): GuestEntryDecision = when {
        accountUserId == null -> GuestEntryDecision.START
        syncRequired -> GuestEntryDecision.BLOCKED_UNSYNCED_ACCOUNT
        else -> GuestEntryDecision.START_AFTER_WIPING_SYNCED_ACCOUNT
    }
}