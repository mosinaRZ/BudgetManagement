package ir.hamedan.budgetmanagement.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.guest.GuestModeStore
import ir.hamedan.budgetmanagement.data.repository.ProfileRepository
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.ui.screens.profile.ProfileCompletion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lightweight view of the profile completion for the home screen.
 *
 * It observes the locally stored profile, and makes ONE silent refresh per home session. The
 * refresh matters: after a login on a new device / reinstall the local row starts empty (names,
 * birth date), while the server profile may already be complete. Without it the CTA kept showing
 * "complete your profile" until the user happened to open the profile screen.
 *
 * `null` means "show nothing": guest workspace, profile not loaded yet, the profile is already
 * complete, or the profile looks incomplete but the refresh has not settled yet (avoids a
 * flash of the CTA that disappears a second later).
 */
class ProfileCompletionViewModel(
    private val repository: ProfileRepository,
    private val authSessionStore: AuthSessionStore,
    private val guestModeStore: GuestModeStore
) : ViewModel() {

    private val _completion = MutableStateFlow<ProfileCompletion?>(null)
    val completion: StateFlow<ProfileCompletion?> = _completion.asStateFlow()

    /** True once the silent refresh finished, failed, or took too long (offline etc.). */
    private val refreshSettled = MutableStateFlow(false)

    init {
        // A guest has no account or phone number, so there is no profile to nudge about.
        val userId = authSessionStore.userId()
        if (!guestModeStore.isSessionActive && !userId.isNullOrBlank()) {
            viewModelScope.launch {
                runCatching { repository.refresh() }
                refreshSettled.value = true
            }
            // Never keep the CTA hidden for long if the network is slow or down.
            viewModelScope.launch {
                delay(REFRESH_WAIT_MS)
                refreshSettled.value = true
            }
            viewModelScope.launch {
                combine(repository.observeLocalProfile(userId), refreshSettled) { user, settled ->
                    user
                        ?.let(ProfileCompletion::of)
                        ?.takeUnless { it.isComplete || !settled }
                }.collect { _completion.value = it }
            }
        }
    }

    private companion object {
        const val REFRESH_WAIT_MS = 3_000L
    }
}