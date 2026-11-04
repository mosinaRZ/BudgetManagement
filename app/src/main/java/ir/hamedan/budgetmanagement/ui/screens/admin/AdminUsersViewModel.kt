package ir.hamedan.budgetmanagement.ui.screens.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.network.AdminApi
import ir.hamedan.budgetmanagement.data.network.ApiException
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.utils.ErrorMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UsersUiState(
    val users: List<AdminApi.AdminUser> = emptyList(),
    val total: Long = 0L,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    /** The server refused the console (role removed). The screen shows a dedicated "no access" state. */
    val forbidden: Boolean = false,
    val query: AdminApi.UserQuery = AdminApi.UserQuery(),
    val busyIds: Set<String> = emptySet(),
    val onlineWindowSec: Long = 180L
) {
    val canLoadMore: Boolean get() = users.size < total
}

data class DetailUiState(
    val userId: String? = null,
    val isLoading: Boolean = false,
    val detail: AdminApi.UserDetail? = null,
    val error: String? = null
)

/** One-shot messages for the screen's snackbar. [undoToken] is non-null when an Undo is offered. */
sealed interface UsersEvent {
    data class Info(val message: String) : UsersEvent
    data class Failure(val message: String) : UsersEvent
    data class Undoable(val message: String, val undoToken: Long) : UsersEvent
}

@OptIn(FlowPreview::class)
class AdminUsersViewModel(
    private val adminApi: AdminApi,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(UsersUiState())
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    private val _detail = MutableStateFlow(DetailUiState())
    val detail: StateFlow<DetailUiState> = _detail.asStateFlow()

    private val _events = MutableSharedFlow<UsersEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UsersEvent> = _events.asSharedFlow()

    private val searchInput = MutableStateFlow("")
    private var loadJob: Job? = null
    private var undoSeq = 0L
    private val pendingUndo = HashMap<Long, suspend () -> Unit>()

    private val isPersian: Boolean get() = LocaleHelper.getLanguage(context) == "fa"
    private fun tr(fa: String, en: String) = if (isPersian) fa else en

    init {
        reload()
        // Typing in the search box reloads the list after a short pause instead of on every key.
        viewModelScope.launch {
            searchInput.drop(1).debounce(350).distinctUntilChanged().collect { text ->
                _state.update { it.copy(query = it.query.copy(search = text)) }
                reload()
            }
        }
    }

    fun onSearchChanged(text: String) { searchInput.value = text.take(48) }
    fun setRole(role: String) = applyFilter { it.copy(role = role) }
    fun setStatus(status: String) = applyFilter { it.copy(status = status) }
    fun setPresence(presence: String) = applyFilter { it.copy(presence = presence) }
    fun setSort(sort: String) = applyFilter { it.copy(sort = sort) }
    fun clearFilters() {
        searchInput.value = ""
        _state.update { it.copy(query = AdminApi.UserQuery()) }
        reload()
    }

    private fun applyFilter(change: (AdminApi.UserQuery) -> AdminApi.UserQuery) {
        _state.update { it.copy(query = change(it.query)) }
        reload()
    }

    /** Reloads from the first page. With [silent] the current list stays on screen (auto refresh). */
    fun reload(silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _state.update { it.copy(isLoading = true, error = null) }
            // A silent refresh keeps as many rows as are already shown, so scrolling is not disturbed.
            val keep = if (silent) _state.value.users.size.coerceIn(30, 100) else 30
            val query = _state.value.query.copy(offset = 0, limit = keep)
            try {
                val page = withContext(Dispatchers.IO) { adminApi.listUsers(query) }
                _state.update {
                    it.copy(
                        users = page.users, total = page.total, isLoading = false, error = null,
                        forbidden = false, onlineWindowSec = page.onlineWindowSec
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e, silent)
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || !s.canLoadMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val page = withContext(Dispatchers.IO) { adminApi.listUsers(s.query.copy(offset = s.users.size, limit = 30)) }
                _state.update { cur ->
                    val known = cur.users.mapTo(HashSet()) { it.id }
                    cur.copy(users = cur.users + page.users.filter { it.id !in known }, total = page.total, isLoadingMore = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false) }
                _events.tryEmit(UsersEvent.Failure(ErrorMessages.of(e, isPersian)))
            }
        }
    }

    private fun fail(e: Exception, silent: Boolean) {
        val forbidden = (e as? ApiException)?.statusCode == 403
        _state.update {
            it.copy(
                isLoading = false,
                forbidden = forbidden,
                // A failed background refresh must not replace a good list with an error banner.
                error = if (silent && it.users.isNotEmpty()) it.error else ErrorMessages.of(e, isPersian)
            )
        }
    }

    // ------------------------------------------------------------------ detail

    fun openDetail(userId: String) {
        _detail.value = DetailUiState(userId = userId, isLoading = true)
        refreshDetail()
    }

    fun closeDetail() { _detail.value = DetailUiState() }

    private fun refreshDetail() {
        val id = _detail.value.userId ?: return
        viewModelScope.launch {
            try {
                val d = withContext(Dispatchers.IO) { adminApi.getUser(id) }
                if (_detail.value.userId == id) _detail.value = DetailUiState(userId = id, detail = d)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_detail.value.userId == id) _detail.value = DetailUiState(userId = id, error = ErrorMessages.of(e, isPersian))
            }
        }
    }

    // ------------------------------------------------------------------ actions

    private fun nameOf(u: AdminApi.AdminUser) = u.fullName.ifBlank { u.phoneNumber.ifBlank { u.id.takeLast(6) } }

    private fun replace(updated: AdminApi.AdminUser) {
        _state.update { s -> s.copy(users = s.users.map { if (it.id == updated.id) updated else it }) }
        if (_detail.value.userId == updated.id) refreshDetail()
    }

    private fun busy(id: String, on: Boolean) {
        _state.update { it.copy(busyIds = if (on) it.busyIds + id else it.busyIds - id) }
    }

    private fun registerUndo(action: suspend () -> Unit): Long {
        val token = ++undoSeq
        pendingUndo[token] = action
        return token
    }

    /** Runs the reverse operation behind an Undo snackbar. */
    fun undo(token: Long) {
        val action = pendingUndo.remove(token) ?: return
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.tryEmit(UsersEvent.Failure(ErrorMessages.of(e, isPersian)))
            }
        }
    }

    /** The snackbar timed out without Undo: the change stays, the stored reverse action is dropped. */
    fun forgetUndo(token: Long) { pendingUndo.remove(token) }

    private fun perform(userId: String, block: suspend () -> Unit) {
        if (userId in _state.value.busyIds) return
        viewModelScope.launch {
            busy(userId, true)
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if ((e as? ApiException)?.statusCode == 403) _state.update { it.copy(forbidden = true) }
                _events.tryEmit(UsersEvent.Failure(ErrorMessages.of(e, isPersian)))
            } finally {
                busy(userId, false)
            }
        }
    }

    /** Soft delete: the account stays restorable, which is what makes the Undo snackbar safe. */
    fun deleteUser(user: AdminApi.AdminUser) = perform(user.id) {
        val updated = withContext(Dispatchers.IO) { adminApi.deleteUser(user.id) }
        replace(updated)
        val token = registerUndo {
            val restored = withContext(Dispatchers.IO) { adminApi.restoreUser(user.id) }
            replace(restored)
            _events.tryEmit(UsersEvent.Info(tr("«${nameOf(user)}» بازیابی شد.", "\"${nameOf(user)}\" was restored.")))
        }
        _events.tryEmit(UsersEvent.Undoable(tr("«${nameOf(user)}» حذف شد.", "\"${nameOf(user)}\" was deleted."), token))
    }

    fun restoreUser(user: AdminApi.AdminUser) = perform(user.id) {
        val updated = withContext(Dispatchers.IO) { adminApi.restoreUser(user.id) }
        replace(updated)
        _events.tryEmit(UsersEvent.Info(tr("«${nameOf(user)}» بازیابی شد.", "\"${nameOf(user)}\" was restored.")))
    }

    fun unlockUser(user: AdminApi.AdminUser) = perform(user.id) {
        replace(withContext(Dispatchers.IO) { adminApi.unlockUser(user.id) })
        _events.tryEmit(UsersEvent.Info(tr("قفل حساب «${nameOf(user)}» باز شد.", "\"${nameOf(user)}\" was unlocked.")))
    }

    fun revokeSessions(user: AdminApi.AdminUser) = perform(user.id) {
        replace(withContext(Dispatchers.IO) { adminApi.revokeSessions(user.id) })
        _events.tryEmit(UsersEvent.Info(tr("همه نشست‌های «${nameOf(user)}» باطل شد.", "All sessions of \"${nameOf(user)}\" were revoked.")))
    }

    /** Sends only what changed. Undo puts the previous values back. */
    fun saveEdit(user: AdminApi.AdminUser, firstName: String, lastName: String, role: String) = perform(user.id) {
        val first = firstName.trim().takeIf { it != user.firstName }
        val last = lastName.trim().takeIf { it != user.lastName }
        val newRole = role.takeIf { it != user.role }
        if (first == null && last == null && newRole == null) return@perform
        val updated = withContext(Dispatchers.IO) { adminApi.updateUser(user.id, first, last, newRole) }
        replace(updated)
        val token = registerUndo {
            val back = withContext(Dispatchers.IO) {
                adminApi.updateUser(
                    user.id,
                    firstName = if (first != null) user.firstName else null,
                    lastName = if (last != null) user.lastName else null,
                    role = if (newRole != null) user.role else null
                )
            }
            replace(back)
            _events.tryEmit(UsersEvent.Info(tr("تغییرات برگردانده شد.", "Changes were reverted.")))
        }
        _events.tryEmit(UsersEvent.Undoable(tr("«${nameOf(user)}» ویرایش شد.", "\"${nameOf(user)}\" was updated."), token))
    }
}
