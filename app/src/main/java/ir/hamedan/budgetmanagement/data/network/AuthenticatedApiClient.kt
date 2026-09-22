package ir.hamedan.budgetmanagement.data.network

import ir.hamedan.budgetmanagement.data.security.AuthSessionStore

/**
 * Single access-token refresh boundary for authenticated HTTP calls.
 * AuthApi itself uses the raw client so the refresh request can never recurse.
 */
class AuthenticatedApiClient(
    private val client: ApiHttpClient,
    private val authApi: AuthApi,
    private val sessionStore: AuthSessionStore
) {
    @Synchronized
    fun request(method: String, path: String, body: org.json.JSONObject? = null): ApiHttpClient.ApiResponse {
        val access = sessionStore.accessToken()
            ?: throw ApiException(401, "UNAUTHORIZED", "نشست احراز هویت وجود ندارد.")

        var response = client.request(method, path, body, access)
        if (response.statusCode != 401) return response

        val refresh = sessionStore.refreshToken()
            ?: throw ApiException(401, "UNAUTHORIZED", "نشست احراز هویت منقضی شده است.")

        val refreshed = try {
            authApi.refresh(refresh)
        } catch (e: ApiException) {
            sessionStore.clearSession()
            throw ApiException(401, "UNAUTHORIZED", "نشست احراز هویت منقضی شده است.")
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
}