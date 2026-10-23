package ir.hamedan.budgetmanagement.ui.screens.profile

import android.content.Context
import ir.hamedan.budgetmanagement.utils.ErrorMessages
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.guest.GuestModeStore
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.repository.ProfileRepository
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** One-shot confirmations the screen turns into a localized snackbar. */
enum class ProfileNotice { PROFILE_SAVED }

data class ProfileUiState(
    val user: UserEntity? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val notice: ProfileNotice? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var observedUserId: String? = null

    /** True while the guest workspace is open (no server profile). */
    val isGuest: Boolean

    init {
        val container = (context.applicationContext as BudgetApp).container
        // Guest mode has no account: its profile is the local guest row, and nothing is requested.
        isGuest = container.guestModeStore.isSessionActive
        val userId = if (isGuest) GuestModeStore.GUEST_USER_ID else container.authSessionStore.userId()
        if (!userId.isNullOrBlank()) {
            observedUserId = userId
            viewModelScope.launch {
                repository.observeLocalProfile(userId).collectLatest { user ->
                    _state.value = _state.value.copy(user = user, isLoading = false)
                }
            }
            refresh()
        } else {
            _state.value = ProfileUiState(
                isLoading = false,
                error = if (isPersian()) "نشست کاربر پیدا نشد." else "Your session could not be found."
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.refresh()
                .onSuccess { _state.value = _state.value.copy(isLoading = false, error = null) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, error = it.userMessage()) }
        }
    }

    fun saveProfile(firstName: String, lastName: String, gender: String, birthDate: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            repository.updateProfile(firstName, lastName, gender, birthDate)
                .onSuccess { _state.value = _state.value.copy(isSaving = false, notice = ProfileNotice.PROFILE_SAVED) }
                .onFailure { _state.value = _state.value.copy(isSaving = false, error = it.userMessage()) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun consumeNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    private fun isPersian(): Boolean = LocaleHelper.getLanguage(context) == "fa"

    /** Never show the raw exception text: it is technical and often in the wrong language. */
    private fun Throwable.userMessage(): String = ErrorMessages.of(this, isPersian())
}