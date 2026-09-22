package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
import ir.hamedan.budgetmanagement.data.security.SyncKeyManager
import ir.hamedan.budgetmanagement.data.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val syncStateRepository: SyncStateRepository,
    private val sessionStore: AuthSessionStore,
    private val syncKeyManager: SyncKeyManager,
    private val syncEngine: SyncEngine,
    private val syncLocalDataSource: SyncLocalDataSource,
    private val deviceIdentityStore: DeviceIdentityStore
) : AuthRepository {

    override suspend fun requestOtp(destination: String, channel: String, purpose: String): Result<AuthApi.OtpResponse> =
        withContext(Dispatchers.IO) { runCatching { authApi.requestOtp(destination.trim(), channel, purpose) } }

    override suspend fun login(identifier: String, password: String): Result<Unit> {
        if (identifier.isBlank()) return Result.failure(IllegalArgumentException("Identifier must not be blank."))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password must not be blank."))
        return withContext(Dispatchers.IO) {
            runCatching { persistSession(authApi.login(identifier.trim(), password, deviceIdentityStore.getOrCreate()), password, identifier = identifier.trim()) }
        }
    }

    override suspend fun register(input: AuthRepository.RegistrationInput): Result<AuthRepository.RegistrationResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(input.phoneNumber.isNotBlank()) { "Phone number must not be blank." }
            require(input.password.length in 8..256) { "Password must be between 8 and 256 characters." }
            val deviceId = deviceIdentityStore.getOrCreate()
            val material = syncKeyManager.createRegistrationMaterial(input.password.toCharArray())
            val response = authApi.register(
                AuthApi.RegisterInput(
                    phoneNumber = input.phoneNumber,
                    email = input.email,
                    password = input.password,
                    deviceId = deviceId,
                    otpChallengeId = input.otpChallengeId,
                    otpCode = input.otpCode,
                    emailOtpChallengeId = input.emailOtpChallengeId,
                    emailOtpCode = input.emailOtpCode,
                    kdfSalt = material.kdfSalt,
                    passwordKeyEnvelope = material.passwordKeyEnvelope,
                    passwordKeyNonce = material.passwordKeyNonce,
                    recoveryKeyHash = material.recoveryKeyHash,
                    recoveryKeyEnvelope = material.recoveryKeyEnvelope,
                    recoveryKeyNonce = material.recoveryKeyNonce
                )
            )
            persistSession(response, input.password, phoneNumber = input.phoneNumber, email = input.email)
            // The recovery key is returned exactly once so the registration UI can
            // force an explicit secure backup instead of silently persisting it.
            AuthRepository.RegistrationResult(
                recoveryKey = material.recoveryKey,
                recoveryRequired = response.recoveryRequired
            )
        }
    }

    override suspend fun prepareRecovery(challengeId: String, otpCode: String, recoveryKey: String): Result<AuthApi.RecoveryPreparation> =
        withContext(Dispatchers.IO) { runCatching { authApi.prepareRecovery(challengeId, otpCode, recoveryKey) } }

    override suspend fun resetPassword(
        recoverySessionToken: String,
        newPassword: String,
        recoveryKey: String,
        recoveryKeyEnvelope: String,
        recoveryKeyNonce: String,
        kdfSalt: String,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(newPassword.length in 8..256) { "Password must be between 8 and 256 characters." }
            val deviceId = deviceIdentityStore.getOrCreate()
            val passwordMaterial = syncKeyManager.createPasswordResetMaterial(
                newPassword = newPassword.toCharArray(),
                kdfSaltB64 = kdfSalt,
                recoveryKey = recoveryKey,
                recoveryKeyEnvelopeB64 = recoveryKeyEnvelope,
                recoveryKeyNonceB64 = recoveryKeyNonce
            )
            val response = authApi.resetPassword(
                AuthApi.ResetPasswordInput(
                    recoverySessionToken = recoverySessionToken,
                    newPassword = newPassword,
                    deviceId = deviceId,
                    kdfSalt = passwordMaterial.kdfSalt,
                    passwordKeyEnvelope = passwordMaterial.passwordKeyEnvelope,
                    passwordKeyNonce = passwordMaterial.passwordKeyNonce
                )
            )
            syncKeyManager.storeRecoveredDataKey(passwordMaterial.dataKey)
            persistSession(response, newPassword, expectedUserId = userId)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wasAuthenticated = sessionStore.isAuthenticated()
            var syncSucceeded = true
            if (wasAuthenticated) {
                syncSucceeded = runCatching { syncEngine.sync() }
                    .getOrNull()
                    ?.let { it !is SyncEngine.SyncResult.ReauthenticationRequired && it !is SyncEngine.SyncResult.NotAuthenticated }
                    ?: false
            }

            val refreshToken = sessionStore.refreshToken()
            if (!refreshToken.isNullOrBlank()) {
                runCatching { authApi.logout(refreshToken) }
            }

            if (syncSucceeded) {
                syncLocalDataSource.clearAccountData()
                syncKeyManager.clear()
            } else {
                // Offline/re-authentication failure must not destroy unsynced local data.
                // Clear only credentials; the next login can resume sync for the same account.
                syncStateRepository.markSyncRequired()
                syncKeyManager.clear()
            }
            sessionStore.clear()
        }
    }

    override fun isAuthenticated(): Boolean = sessionStore.isAuthenticated()

    private suspend fun persistSession(
        response: AuthApi.LoginResponse,
        password: String,
        identifier: String? = null,
        phoneNumber: String? = null,
        email: String? = null
    ) {
        val deviceId = deviceIdentityStore.getOrCreate()
        val previousUserId = sessionStore.userId() ?: syncStateRepository.get().accountUserId
        if (previousUserId != null && previousUserId != response.userId) {
            syncLocalDataSource.clearAccountData()
        }
        syncKeyManager.initializeFromLogin(
            password = password.toCharArray(),
            kdfSaltB64 = response.kdfSalt,
            envelopeB64 = response.passwordKeyEnvelope.takeIf { it.isNotBlank() },
            nonceB64 = response.passwordKeyNonce.takeIf { it.isNotBlank() }
        )
        sessionStore.save(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.userId,
            deviceId = deviceId,
            kdfSalt = response.kdfSalt,
            passwordKeyEnvelope = response.passwordKeyEnvelope,
            passwordKeyNonce = response.passwordKeyNonce,
            recoveryKeyEnvelope = response.recoveryKeyEnvelope,
            recoveryKeyNonce = response.recoveryKeyNonce,
            role = response.role
        )
        userDao.clearLoggedInState(System.currentTimeMillis())
        userDao.insert(
            UserEntity(
                id = response.userId,
                phoneNumber = phoneNumber ?: identifier?.takeIf { !it.contains("@") }.orEmpty(),
                email = email ?: identifier?.takeIf { it.contains("@") },
                isLoggedIn = true
            )
        )
        val state = syncStateRepository.get()
        syncStateRepository.save(state.copy(deviceId = deviceId, accountUserId = response.userId, syncRequired = true))
    }

    private suspend fun persistSession(
        response: AuthApi.RegisterResponse,
        password: String,
        phoneNumber: String? = null,
        email: String? = null
    ) {
        persistSession(
            AuthApi.LoginResponse(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                kdfSalt = response.kdfSalt,
                userId = response.userId,
                role = response.role,
                passwordKeyEnvelope = response.passwordKeyEnvelope,
                passwordKeyNonce = response.passwordKeyNonce,
                recoveryKeyEnvelope = response.recoveryKeyEnvelope,
                recoveryKeyNonce = response.recoveryKeyNonce
            ), password, phoneNumber = phoneNumber, email = email
        )
    }

    private suspend fun persistSession(response: AuthApi.ResetPasswordResponse, password: String, expectedUserId: String) {
        require(response.userId == expectedUserId) { "Recovered account does not match the requested account." }
        persistSession(
            AuthApi.LoginResponse(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                kdfSalt = response.kdfSalt,
                userId = response.userId,
                role = response.role,
                passwordKeyEnvelope = response.passwordKeyEnvelope,
                passwordKeyNonce = response.passwordKeyNonce,
                recoveryKeyEnvelope = response.recoveryKeyEnvelope,
                recoveryKeyNonce = response.recoveryKeyNonce
            ), password
        )
    }
}
