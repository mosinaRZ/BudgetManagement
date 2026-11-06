package ir.hamedan.budgetmanagement.data.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Decides whether this build is too old to be used.
 *
 * The only truth is the installed version compared with the server's minimum version: there is
 * no "the user tapped the link" flag. After a real update Android restarts the app as the new
 * version, the comparison passes and the update screen disappears by itself; if the user comes
 * back without updating, the comparison still fails and the screen stays.
 *
 * The decision is made from the last policy the server sent (kept on disk), so it holds offline
 * too. A device that has never reached the server is not blocked: there is nothing to enforce yet.
 */
class AppUpdateManager(
    private val api: AppPolicySource,
    private val store: AppUpdateStore,
    private val installedVersionName: String,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    /** Why and how to update. */
    data class Requirement(
        val installedVersion: String,
        val minVersion: String?,
        val latestVersion: String?,
        val updateUrls: Map<String, String>
    )

    data class State(val blocked: Requirement? = null, val checking: Boolean = false)

    private val installed: AppVersion? = AppVersion.parse(installedVersionName)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshLock = Mutex()
    private val startupCheckDone = CompletableDeferred<Unit>()
    private var lastAttemptAt = 0L

    private val _state = MutableStateFlow(State(blocked = evaluate()))
    val state: StateFlow<State> = _state.asStateFlow()

    fun isBlocked(): Boolean = _state.value.blocked != null

    /**
     * Waits (bounded) for the first policy request of this launch, so the splash screen does not
     * let the user in before the answer is known. Returns immediately once it is known.
     */
    suspend fun awaitStartupCheck() {
        withTimeoutOrNull(STARTUP_WAIT_MS) { startupCheckDone.await() }
    }

    /**
     * Fetches the policy. Calls are throttled (and skipped while another one runs), so it is safe
     * to call from every foreground tick. A failed request keeps the last known policy.
     */
    suspend fun refresh(force: Boolean = false) {
        if (!refreshLock.tryLock()) return
        try {
            val now = clock()
            val interval = if (isBlocked()) BLOCKED_REFRESH_INTERVAL_MS else REFRESH_INTERVAL_MS
            if (!force && lastAttemptAt != 0L && now - lastAttemptAt < interval) return
            lastAttemptAt = now

            _state.update { it.copy(checking = true) }
            try {
                val policy = withContext(Dispatchers.IO) { api.fetch() }
                store.save(policy)
                val minimum = AppVersion.parse(policy.minVersion)
                val current = installed
                // The server now says this build is fine: forget an earlier 426.
                if (current != null && minimum != null && current >= minimum) store.clearRejected()
                _state.value = State(blocked = evaluate())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Offline or a server problem: keep deciding from the last known policy.
                _state.update { it.copy(checking = false) }
            }
        } finally {
            startupCheckDone.complete(Unit)
            refreshLock.unlock()
        }
    }

    /** The API answered 426: the server itself no longer accepts this build. */
    fun onServerRejectedVersion() {
        if (store.rejectedVersion() == installedVersionName && isBlocked()) return
        store.markRejected(installedVersionName)
        _state.update { it.copy(blocked = evaluate()) }
        // Fetch the store links (and the latest version) for the screen.
        scope.launch { refresh(force = true) }
    }

    private fun evaluate(): Requirement? {
        val policy = store.policy()
        val minimum = AppVersion.parse(policy?.minVersion)
        val current = installed
        val tooOld = current != null && minimum != null && current < minimum
        val rejected = store.rejectedVersion() == installedVersionName
        if (!tooOld && !rejected) return null
        return Requirement(
            installedVersion = installedVersionName,
            minVersion = policy?.minVersion,
            latestVersion = policy?.latestVersion,
            updateUrls = policy?.updateUrls.orEmpty()
        )
    }

    private companion object {
        const val STARTUP_WAIT_MS = 5_000L
        const val REFRESH_INTERVAL_MS = 10 * 60_000L
        const val BLOCKED_REFRESH_INTERVAL_MS = 30_000L
    }
}
