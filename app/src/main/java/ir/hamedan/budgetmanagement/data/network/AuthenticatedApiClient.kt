package ir.hamedan.budgetmanagement.data.network

import ir.hamedan.budgetmanagement.data.security.AuthSessionStore

/**
 * Single access-token refresh boundary for authenticated HTTP calls.
 * AuthApi itself uses the raw client so the refresh request can never recurse.
 */
class AuthenticatedApiClient(
    private val client: ApiHttpClient,
    private val authApi: AuthApi,
    private val sessionStore: AuthSessionStore,
    /** Called once when the server reports that this device was removed from the account. */
    private val onDeviceRemoved: () -> Unit = {}
) {
    @Synchronized
    fun request(method: String, path: String, body: org.json.JSONObject? = null): ApiHttpClient.ApiResponse {
        val access = sessionStore.accessToken()
            ?: throw ApiException(401, "UNAUTHORIZED", "نشست احراز هویت وجود ندارد.")

        var response = client.request(method, path, body, access)
        if (response.statusCode != 401) return response

        // This device was removed from the account: refreshing cannot help, so end the session
        // right away and let the user know why.
        if (response.errorCode() == DEVICE_REMOVED) failDeviceRemoved()

        val refresh = sessionStore.refreshToken()
            ?: throw ApiException(401, "UNAUTHORIZED", "نشست احراز هویت منقضی شده است.")

        val refreshed = try {
            authApi.refresh(refresh)
        } catch (e: ApiException) {
            // Only definitive refresh-token failures invalidate the local session.
            // Transient 429/5xx/network failures must preserve credentials so the
            // next request/worker can retry without forcing a needless logout.
            if (e.code == DEVICE_REMOVED) failDeviceRemoved(e)
            if (e.statusCode in setOf(400, 401, 403)) {
                sessionStore.clearSession(unexpected = true)
                throw ApiException(401, "UNAUTHORIZED", "نشست احراز هویت منقضی شده است.", e)
            }
            throw e
        }

        sessionStore.save(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            userId = sessionStore.userId().orEmpty(),
            deviceId = sessionStore.deviceId().orEmpty(),
            kdfSalt = sessionStore.kdfSalt().orEmpty(),
            passwordKeyEnvelope = sessionStore.passwordKeyEnvelope().orEmpty(),
            passwordKeyNonce = sessionStore.passwordKeyNonce().orEmpty(),
            recoveryKeyEnvelope = sessionStore.recoveryKeyEnvelope().orEmpty(),
            recoveryKeyNonce = sessionStore.recoveryKeyNonce().orEmpty(),
            role = refreshed.role ?: sessionStore.role()
        )
        return client.request(method, path, body, refreshed.accessToken)
    }

    private fun failDeviceRemoved(cause: Throwable? = null): Nothing {
        sessionStore.clearSession(unexpected = true, deviceRemoved = true)
        runCatching { onDeviceRemoved() }
        throw ApiException(401, DEVICE_REMOVED, "این دستگاه از حساب حذف شده است.", cause)
    }

    private companion object {
        const val DEVICE_REMOVED = "DEVICE_REMOVED"
    }
}