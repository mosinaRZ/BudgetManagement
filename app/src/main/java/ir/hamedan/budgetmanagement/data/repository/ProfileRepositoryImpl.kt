package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.network.ProfileApi
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProfileRepositoryImpl(
    private val profileApi: ProfileApi,
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val sessionStore: AuthSessionStore
) : ProfileRepository {

    override fun observeLocalProfile(userId: String): Flow<UserEntity?> = userDao.observeUserById(userId)

    override suspend fun refresh(): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val profile = profileApi.getProfile()
            require(sessionStore.userId() == profile.userId) { "Profile does not belong to the signed-in account." }
            val current = userDao.getById(profile.userId)
            val entity = profile.toEntity(current)
            userDao.insert(entity)
            entity
        }
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        gender: String,
        birthDate: String?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val profile = profileApi.updateProfile(
                ProfileApi.UpdateProfileInput(firstName.trim(), lastName.trim(), gender, birthDate)
            )
            val current = userDao.getById(profile.userId)
            val entity = profile.toEntity(current)
            userDao.insert(entity)
            entity
        }
    }

    override suspend fun requestEmailOtp(email: String): Result<AuthApi.OtpResponse> =
        withContext(Dispatchers.IO) {
            runCatching { authApi.requestOtp(email.trim(), "email", "EMAIL_VERIFICATION") }
        }

    override suspend fun updateEmail(email: String, challengeId: String, code: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val profile = profileApi.updateEmail(
                    ProfileApi.UpdateEmailInput(email.trim(), challengeId, code)
                )
                val current = userDao.getById(profile.userId)
                val entity = profile.toEntity(current)
                userDao.insert(entity)
                entity
            }
        }

    private fun ProfileApi.Profile.toEntity(current: UserEntity?): UserEntity {
        return UserEntity(
            id = userId,
            phoneNumber = phoneNumber.ifBlank { current?.phoneNumber.orEmpty() },
            fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" "),
            firstName = firstName,
            lastName = lastName,
            gender = gender,
            birthDate = birthDate,
            email = email,
            emailVerified = emailVerified,
            isLoggedIn = true,
            createdAt = current?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}