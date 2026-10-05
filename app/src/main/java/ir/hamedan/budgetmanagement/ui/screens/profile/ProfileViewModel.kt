package ir.hamedan.budgetmanagement.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: UserEntity? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val emailOtpSent: Boolean = false,
    val emailOtpChallengeId: String? = null,
    val emailOtpExpiresAt: Long? = null,
    val emailPending: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var observedUserId: String? = null

    init {
        val userId = (context.applicationContext as BudgetApp).container.authSessionStore.userId()
        if (!userId.isNullOrBlank()) {
            observedUserId = userId
            viewModelScope.launch {
                repository.observeLocalProfile(userId).collectLatest { user ->
                    _state.value = _state.value.copy(user = user, isLoading = false)
                }
            }
            refresh()
        } else {
            _state.value = ProfileUiState(isLoading = false, error = "نشست کاربر پیدا نشد.")
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
                .onSuccess { _state.value = _state.value.copy(isSaving = false) }
                .onFailure { _state.value = _state.value.copy(isSaving = false, error = it.userMessage()) }
        }
    }

    fun requestEmailVerification(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            repository.requestEmailOtp(email)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        emailOtpSent = true,
                        emailOtpChallengeId = it.challengeId,
                        emailOtpExpiresAt = it.expiresAt,
                        emailPending = email.trim()
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(isSaving = false, error = it.userMessage())
                }
        }
    }

    fun verifyEmail(code: String) {
        val challenge = _state.value.emailOtpChallengeId ?: return
        val email = _state.value.emailPending ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            repository.updateEmail(email, challenge, code)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        emailOtpSent = false,
                        emailOtpChallengeId = null,
                        emailOtpExpiresAt = null,
                        emailPending = null
                    )
                }
                .onFailure { _state.value = _state.value.copy(isSaving = false, error = it.userMessage()) }
        }
    }

    fun dismissEmailOtp() {
        _state.value = _state.value.copy(
            emailOtpSent = false,
            emailOtpChallengeId = null,
            emailOtpExpiresAt = null,
            emailPending = null
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "عملیات انجام نشد. دوباره تلاش کنید."
}