package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.network.ProfileApi
import kotlinx.coroutines.flow.Flow
import ir.hamedan.budgetmanagement.data.local.models.UserEntity

interface ProfileRepository {
    fun observeLocalProfile(userId: String): Flow<UserEntity?>
    suspend fun refresh(): Result<UserEntity>
    suspend fun updateProfile(firstName: String, lastName: String, gender: String, birthDate: String?): Result<UserEntity>
    suspend fun requestEmailOtp(email: String): Result<AuthApi.OtpResponse>
    suspend fun updateEmail(email: String, challengeId: String, code: String): Result<UserEntity>
}