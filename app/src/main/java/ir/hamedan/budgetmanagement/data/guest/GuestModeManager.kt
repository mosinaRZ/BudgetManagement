package ir.hamedan.budgetmanagement.data.guest

import ir.hamedan.budgetmanagement.data.guest.GuestModeStore.Companion.GUEST_USER_ID
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.repository.SyncStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the guest workspace: it lives entirely in the local encrypted database and never calls the
 * backend. Its local rows are plain data; they are encrypted and pushed only when the user later
 * signs in, which is how guest data becomes part of an account.
 */
class GuestModeManager(
    private val store: GuestModeStore,
    private val userDao: UserDao,
    private val syncStateRepository: SyncStateRepository,
    private val syncLocalDataSource: SyncLocalDataSource
) {
    /** Thrown when guest entry is refused because unsynced account data is on the device. */
    class UnsyncedAccountException : IllegalStateException("unsynced account data on device")

    val isSessionActive: Boolean get() = store.isSessionActive

    fun guestStore(): GuestModeStore = store

    /** Starts (or resumes) the guest workspace and marks the in-memory session as active. */
    suspend fun startSession(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val state = syncStateRepository.get()
            when (GuestEntryPolicy.decide(state.accountUserId, state.syncRequired)) {
                GuestEntryDecision.BLOCKED_UNSYNCED_ACCOUNT -> throw UnsyncedAccountException()
                GuestEntryDecision.START_AFTER_WIPING_SYNCED_ACCOUNT -> {
                    syncLocalDataSource.clearAccountData()
                    syncStateRepository.save(state.copy(accountUserId = null, syncRequired = false))
                }
                GuestEntryDecision.START -> Unit
            }
            ensureProfileRow()
            store.setWorkspace(true)
            store.beginSession()
        }
    }

    fun endSession() {
        store.endSession()
    }

    /** Returns the guest profile, creating the row if it is missing. */
    suspend fun ensureGuestProfile(): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching { ensureProfileRow() }
    }

    suspend fun updateGuestProfile(
        firstName: String,
        lastName: String,
        gender: String,
        birthDate: String?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val current = ensureProfileRow()
            val entity = current.copy(
                fullName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" "),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                gender = gender,
                birthDate = birthDate,
                updatedAt = System.currentTimeMillis()
            )
            userDao.insert(entity)
            entity
        }
    }

    /**
     * Removes everything the guest workspace holds on this device: local data, the profile row,
     * the passcode and the fingerprint unlock. Also ends the guest session.
     */
    suspend fun deleteWorkspace(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            syncLocalDataSource.clearAccountData()
            val state = syncStateRepository.get()
            syncStateRepository.save(state.copy(accountUserId = null, syncRequired = false))
            store.clearAllSecrets()
            store.endSession()
        }
    }

    /**
     * Called after the server accepted an account sign-in. The guest workspace, if it existed, now
     * belongs to that account (its rows are pushed by the normal sync), so the guest-only identity
     * and its local unlock secrets are retired.
     */
    suspend fun onAccountSignedIn(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userDao.deleteById(GUEST_USER_ID)
            store.clearAllSecrets()
            store.endSession()
        }
    }

    private suspend fun ensureProfileRow(): UserEntity {
        userDao.getById(GUEST_USER_ID)?.let { existing ->
            if (existing.isLoggedIn) return existing
            val revived = existing.copy(isLoggedIn = true, updatedAt = System.currentTimeMillis())
            userDao.insert(revived)
            return revived
        }
        val now = System.currentTimeMillis()
        val created = UserEntity(
            id = GUEST_USER_ID,
            phoneNumber = "",
            fullName = "",
            firstName = "",
            lastName = "",
            gender = "prefer_not_to_say",
            birthDate = null,
            isLoggedIn = true,
            createdAt = now,
            updatedAt = now
        )
        userDao.insert(created)
        return created
    }
}