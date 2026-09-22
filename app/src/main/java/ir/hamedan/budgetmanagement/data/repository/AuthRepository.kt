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
    suspend fun logout(): Result<Unit>
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