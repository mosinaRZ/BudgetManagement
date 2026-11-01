package ir.hamedan.budgetmanagement.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.guest.GuestModeStore
import ir.hamedan.budgetmanagement.data.repository.ProfileRepository
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.ui.screens.profile.ProfileCompletion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lightweight, read-only view of the profile completion for the home screen.
 *
 * It only observes the locally stored profile (no network request, unlike the profile screen),
 * so opening the home screen stays cheap. `null` means "show nothing": guest workspace, profile
 * not loaded yet, or the profile is already complete.
 */
class ProfileCompletionViewModel(
    private val repository: ProfileRepository,
    private val authSessionStore: AuthSessionStore,
    private val guestModeStore: GuestModeStore
) : ViewModel() {

    private val _completion = MutableStateFlow<ProfileCompletion?>(null)
    val completion: StateFlow<ProfileCompletion?> = _completion.asStateFlow()

    init {
        // A guest has no account or phone number, so there is no profile to nudge about.
        val userId = authSessionStore.userId()
        if (!guestModeStore.isSessionActive && !userId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.observeLocalProfile(userId).collect { user ->
                    _completion.value = user
                        ?.let(ProfileCompletion::of)
                        ?.takeUnless { it.isComplete }
                }
            }
        }
    }
}