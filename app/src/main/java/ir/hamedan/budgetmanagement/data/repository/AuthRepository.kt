package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.network.AuthApi

interface AuthRepository {
    suspend fun requestOtp(destination: String, channel: String, purpose: String): Result<AuthApi.OtpResponse>
    suspend fun login(identifier: String, password: String): Result<Unit>
    suspend fun register(input: RegistrationInput): Result<RegistrationResult>
    suspend fun prepareRecovery(challengeId: String, otpCode: String, recoveryKey: String): Result<AuthApi.RecoveryPreparation>
    suspend fun resetPassword(
        recoverySessionToken: String,
        newPassword: String,
        recoveryKey: String,
        recoveryKeyEnvelope: String,
        recoveryKeyNonce: String,
        kdfSalt: String,
        userId: String
    ): Result<Unit>
    /** Changes the password of the signed-in account; other sessions are revoked server-side. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun logout(): Result<Unit>

    /**
     * Prepares signing in to a DIFFERENT account on this device.
     *
     * The current account's data is synced first; the session is only ended (and the local copy
     * of that account's data and local-unlock credentials wiped) when that sync fully succeeded.
     * Returns `true` when the device is ready for a new sign-in, `false` when unsynced changes
     * would be lost (offline or sync failure) so the caller must keep the user signed in.
     */
    suspend fun prepareAccountSwitch(): Result<Boolean>
    fun isAuthenticated(): Boolean

    data class RegistrationResult(
        val recoveryKey: String,
        val recoveryRequired: Boolean
    )

    data class RegistrationInput(
        val phoneNumber: String,
        val email: String?,
        val password: String,
        val otpChallengeId: String,
        val otpCode: String,
        val emailOtpChallengeId: String? = null,
        val emailOtpCode: String? = null
    )
}