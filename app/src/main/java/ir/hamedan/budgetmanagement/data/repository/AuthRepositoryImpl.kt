package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.data.security.SyncKeyManager
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
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

    override suspend fun login(identifier: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val deviceId = deviceIdentityStore.getOrCreate()
                val previousUserId = sessionStore.userId() ?: syncStateRepository.get().accountUserId
                val response = authApi.login(identifier.trim(), password, deviceId)

                // The local database is intentionally single-account. If a different
                // account logs in, wipe the previous account before restoring cloud data.
                if (previousUserId != response.userId) {
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
                    recoveryKeyNonce = response.recoveryKeyNonce
                )

                userDao.clearLoggedInState(System.currentTimeMillis())
                userDao.insert(
                    UserEntity(
                        id = response.userId,
                        phoneNumber = if (identifier.contains("@")) "" else identifier.trim(),
                        email = identifier.takeIf { it.contains("@") },
                        isLoggedIn = true
                    )
                )

                val previousState = syncStateRepository.get()
                syncStateRepository.save(
                    previousState.copy(
                        deviceId = deviceId,
                        accountUserId = response.userId,
                        syncRequired = true
                    )
                )
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Account isolation requires a completed sync before local account data is wiped.
            if (sessionStore.isAuthenticated()) {
                syncEngine.sync()
            }

            val refreshToken = sessionStore.refreshToken()
            if (!refreshToken.isNullOrBlank()) {
                runCatching { authApi.logout(refreshToken) }
            }
            syncLocalDataSource.clearAccountData()
            sessionStore.clear()
            syncKeyManager.clear()
        }
    }

    override fun isAuthenticated(): Boolean = sessionStore.isAuthenticated()
}