package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.guest.GuestModeManager
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.network.AccountApi
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.data.security.CidnaKdf
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
import ir.hamedan.budgetmanagement.data.security.DeviceInfoProvider
import ir.hamedan.budgetmanagement.data.security.RememberedLoginStore
import ir.hamedan.budgetmanagement.data.security.SyncKeyManager
import ir.hamedan.budgetmanagement.data.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ir.hamedan.budgetmanagement.worker.SyncScheduler

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val syncStateRepository: SyncStateRepository,
    private val sessionStore: AuthSessionStore,
    private val syncKeyManager: SyncKeyManager,
    private val syncEngine: SyncEngine,
    private val syncLocalDataSource: SyncLocalDataSource,
    private val deviceIdentityStore: DeviceIdentityStore,
    private val rememberedLoginStore: RememberedLoginStore,
    private val syncScheduler: SyncScheduler,
    private val accountApi: AccountApi,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val guestModeManager: GuestModeManager
) : AuthRepository {

    override suspend fun requestOtp(destination: String, channel: String, purpose: String): Result<AuthApi.OtpResponse> =
        withContext(Dispatchers.IO) { runCatching { authApi.requestOtp(destination.trim(), channel, purpose) } }

    override suspend fun login(
        identifier: String,
        password: String,
        restoreAccount: Boolean,
        discardGuestData: Boolean
    ): Result<Unit> {
        if (identifier.isBlank()) return Result.failure(IllegalArgumentException("Identifier must not be blank."))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password must not be blank."))
        return withContext(Dispatchers.IO) {
            runCatching {
                // The password never leaves the device: fetch the public KDF salt, derive the
                // master key locally and send only the independent authentication secret.
                val params = authApi.loginParams(identifier.trim())
                requireSupportedKdf(params)
                val masterKey = syncKeyManager.deriveMasterKey(password.toCharArray(), params.kdfSalt)
                try {
                    val response = authApi.login(
                        identifier = identifier.trim(),
                        authSecret = syncKeyManager.authSecret(masterKey),
                        deviceId = deviceIdentityStore.getOrCreate(),
                        deviceInfo = deviceInfoProvider.toJson(),
                        restoreAccount = restoreAccount
                    )
                    // Credentials are verified; only now may the guest workspace be discarded.
                    if (discardGuestData) guestModeManager.deleteWorkspace().getOrThrow()
                    persistSession(response, password, masterKey, identifier = identifier.trim())
                } finally {
                    SyncKeyManager.wipe(masterKey)
                }
                // Otherwise the guest workspace stays on the device and is merged into the account
                // by the normal sync, which runs once the session exists.
                guestModeManager.onAccountSignedIn().getOrThrow()
            }
        }
    }

    override suspend fun deleteAccount(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.isNotEmpty()) { "Password must not be empty." }
            require(sessionStore.isAuthenticated()) { "Not signed in." }
            // Signing out of this device happens inside the API client's lock, right after the
            // server confirmed the deletion (see AccountApi.deleteAccount).
            val kdfSalt = sessionStore.kdfSalt()?.takeIf { it.isNotBlank() } ?: error("Account key salt is missing.")
            val masterKey = syncKeyManager.deriveMasterKey(password.toCharArray(), kdfSalt)
            try {
                accountApi.deleteAccount(syncKeyManager.authSecret(masterKey)) { sessionStore.clear() }
            } finally {
                SyncKeyManager.wipe(masterKey)
            }
            // From here on the account is deleted server-side; local cleanup must not turn that
            // into a reported failure, so each step is attempted independently.
            runCatching { syncLocalDataSource.clearAccountData() }
            runCatching { syncKeyManager.clear() }
            runCatching { rememberedLoginStore.clearLocalReauthentication() }
            runCatching {
                // Unbind the local database from the deleted account so a later sign-in (to this or
                // another account) starts from a clean state instead of treating data as unsynced.
                syncStateRepository.save(syncStateRepository.get().copy(accountUserId = null, syncRequired = false))
            }
            // The cleanup steps above return their own Result values; end the block with Unit so the
            // outer runCatching yields Result<Unit> instead of Result<Result<Unit>>.
            Unit
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
                    authSecret = material.authSecret,
                    deviceId = deviceId,
                    otpChallengeId = input.otpChallengeId,
                    otpCode = input.otpCode,
                    kdfSalt = material.kdfSalt,
                    passwordKeyEnvelope = material.passwordKeyEnvelope,
                    passwordKeyNonce = material.passwordKeyNonce,
                    recoveryKeyHash = material.recoveryKeyHash,
                    recoveryKeyEnvelope = material.recoveryKeyEnvelope,
                    recoveryKeyNonce = material.recoveryKeyNonce,
                    deviceInfo = deviceInfoProvider.toJson()
                )
            )
            try {
                persistSession(response, input.password, material.masterKey, phoneNumber = input.phoneNumber)
            } finally {
                SyncKeyManager.wipe(material.masterKey)
            }
            // The recovery key is returned exactly once so the registration UI can
            // force an explicit secure backup instead of silently persisting it.
            AuthRepository.RegistrationResult(
                recoveryKey = material.recoveryKey,
                recoveryRequired = response.recoveryRequired
            )
        }
    }

    override suspend fun prepareRecovery(challengeId: String, otpCode: String, recoveryKey: String): Result<AuthApi.RecoveryPreparation> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Only a derived proof is sent; the recovery key itself (which can unwrap the
                // data key) stays on the device.
                val recoveryBytes = CidnaKdf.decodeRecoveryKey(recoveryKey) ?: throw SyncKeyManager.InvalidRecoveryKeyException()
                try {
                    authApi.prepareRecovery(challengeId, otpCode, CidnaKdf.recoveryProof(recoveryBytes))
                } finally {
                    SyncKeyManager.wipe(recoveryBytes)
                }
            }
        }

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
                    newAuthSecret = passwordMaterial.authSecret,
                    deviceId = deviceId,
                    kdfSalt = passwordMaterial.kdfSalt,
                    passwordKeyEnvelope = passwordMaterial.passwordKeyEnvelope,
                    passwordKeyNonce = passwordMaterial.passwordKeyNonce,
                    deviceInfo = deviceInfoProvider.toJson()
                )
            )
            syncKeyManager.storeRecoveredDataKey(passwordMaterial.dataKey)
            try {
                persistSession(response, newPassword, passwordMaterial.masterKey, expectedUserId = userId)
            } finally {
                SyncKeyManager.wipe(passwordMaterial.masterKey, passwordMaterial.dataKey)
            }
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(currentPassword.isNotEmpty()) { "Current password must not be empty." }
            require(newPassword.length in 8..256) { "Password must be between 8 and 256 characters." }
            require(sessionStore.isAuthenticated()) { "Not signed in." }
            val kdfSalt = sessionStore.kdfSalt()?.takeIf { it.isNotBlank() } ?: error("Account key salt is missing.")
            val deviceId = sessionStore.deviceId()?.takeIf { it.isNotBlank() } ?: deviceIdentityStore.getOrCreate()
            val currentMasterKey = syncKeyManager.deriveMasterKey(currentPassword.toCharArray(), kdfSalt)
            val currentAuthSecret = try {
                syncKeyManager.authSecret(currentMasterKey)
            } finally {
                SyncKeyManager.wipe(currentMasterKey)
            }
            val material = syncKeyManager.createPasswordChangeMaterial(newPassword.toCharArray(), kdfSalt)
            accountApi.changePassword(
                AccountApi.ChangePasswordInput(
                    currentAuthSecret = currentAuthSecret,
                    newAuthSecret = material.authSecret,
                    deviceId = deviceId,
                    kdfSalt = material.kdfSalt,
                    passwordKeyEnvelope = material.passwordKeyEnvelope,
                    passwordKeyNonce = material.passwordKeyNonce,
                    deviceInfo = deviceInfoProvider.toJson()
                )
            ) { issued ->
                // Runs under the API client's lock: the old refresh token is already revoked,
                // so the new pair must be persisted before any other request can refresh.
                sessionStore.save(
                    accessToken = issued.accessToken,
                    refreshToken = issued.refreshToken,
                    userId = issued.userId,
                    deviceId = deviceId,
                    kdfSalt = issued.kdfSalt,
                    passwordKeyEnvelope = issued.passwordKeyEnvelope,
                    passwordKeyNonce = issued.passwordKeyNonce,
                    recoveryKeyEnvelope = issued.recoveryKeyEnvelope,
                    recoveryKeyNonce = issued.recoveryKeyNonce,
                    role = issued.role ?: sessionStore.role()
                )
            }
            // Local re-authentication must follow the new password; the fingerprint credential is
            // dropped as well so it is re-enrolled deliberately from Settings.
            SyncKeyManager.wipe(material.masterKey, material.dataKey)
            rememberedLoginStore.savePasswordVerifier(newPassword)
            rememberedLoginStore.clearBiometricCredential()
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
            }
            sessionStore.clear()
        }
    }

    override suspend fun prepareAccountSwitch(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionStore.isAuthenticated()) {
                val result = runCatching { syncEngine.sync() }.getOrNull()
                val fullySynced = result is SyncEngine.SyncResult.Success &&
                        !syncStateRepository.get().syncRequired
                // Never end the session while this account still has changes only this device
                // knows about: signing in to another account wipes the local copy.
                if (!fullySynced) return@runCatching false
                logout().getOrThrow()
            }
            // The fingerprint credential and local password verifier belong to the old account.
            true
        }
    }

    override fun isAuthenticated(): Boolean = sessionStore.isAuthenticated()

    private suspend fun persistSession(
        response: AuthApi.LoginResponse,
        password: String,
        masterKey: ByteArray,
        identifier: String? = null,
        phoneNumber: String? = null,
    ) {
        val deviceId = deviceIdentityStore.getOrCreate()
        val previousUserId = sessionStore.userId() ?: syncStateRepository.get().accountUserId
        if (previousUserId != null && previousUserId != response.userId) {
            syncLocalDataSource.clearAccountData()
        }
        syncKeyManager.initializeFromLogin(
            masterKey = masterKey,
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
        val existingUser = userDao.getById(response.userId)
        userDao.insert(
            UserEntity(
                id = response.userId,
                phoneNumber = phoneNumber
                    ?: identifier?.takeIf { !it.contains("@") }
                    ?: existingUser?.phoneNumber.orEmpty(),
                fullName = existingUser?.fullName.orEmpty(),
                firstName = existingUser?.firstName.orEmpty(),
                lastName = existingUser?.lastName.orEmpty(),
                gender = existingUser?.gender ?: "prefer_not_to_say",
                birthDate = existingUser?.birthDate,
                isLoggedIn = true,
                createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        rememberedLoginStore.saveIdentifier((identifier ?: phoneNumber).orEmpty())
        rememberedLoginStore.savePasswordVerifier(password)
        val state = syncStateRepository.get()
        syncStateRepository.save(state.copy(deviceId = deviceId, accountUserId = response.userId, syncRequired = true))
        syncScheduler.enqueueNow()
    }

    private suspend fun persistSession(
        response: AuthApi.RegisterResponse,
        password: String,
        masterKey: ByteArray,
        phoneNumber: String? = null,
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
            ), password, masterKey, phoneNumber = phoneNumber
        )
    }

    private suspend fun persistSession(response: AuthApi.ResetPasswordResponse, password: String, masterKey: ByteArray, expectedUserId: String) {
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
            ), password, masterKey
        )
    }

    /** Refuses KDF parameters this build does not implement (also blocks a downgrade by a rogue server). */
    private fun requireSupportedKdf(params: AuthApi.LoginParams) {
        check(params.kdfVersion == CidnaKdf.VERSION && params.kdfIterations == CidnaKdf.PBKDF2_ITERATIONS) {
            "Unsupported key-derivation parameters; please update the app."
        }
    }
}