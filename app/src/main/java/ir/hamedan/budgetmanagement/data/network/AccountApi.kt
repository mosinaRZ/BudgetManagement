package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject

/** Authenticated account operations (the caller must already hold a valid session). */
class AccountApi(private val client: AuthenticatedApiClient) {

    /**
     * Changes the account password. The server verifies [ChangePasswordInput.currentAuthSecret],
     * stores the new opaque key envelope, revokes every other session and returns a fresh session
     * for this device.
     *
     * The old refresh token is revoked server-side the moment the request succeeds. To make sure a
     * concurrent sync cannot try to refresh with it (which the backend treats as token reuse and
     * answers by revoking every session), the request AND [onSessionIssued] run while holding the
     * same monitor as [AuthenticatedApiClient.request]. [onSessionIssued] must persist the new tokens.
     */
    fun changePassword(
        input: ChangePasswordInput,
        onSessionIssued: (AuthApi.ResetPasswordResponse) -> Unit
    ) {
        val body = JSONObject()
            .put("current_auth_secret", input.currentAuthSecret)
            .put("new_auth_secret", input.newAuthSecret)
            .put("device_id", input.deviceId)
            .put("kdf_salt", input.kdfSalt)
            .put("password_key_envelope", input.passwordKeyEnvelope)
            .put("password_key_nonce", input.passwordKeyNonce)
        input.deviceInfo?.let { body.put("device_info", it) }

        synchronized(client) {
            val response = client.request("POST", "/api/v1/auth/password/change", body)
            if (response.statusCode !in 200..299) {
                throw ApiException(response.statusCode, response.errorCode(), response.errorMessage("تغییر گذرواژه ناموفق بود."))
            }
            val json = response.json()
            onSessionIssued(
                AuthApi.ResetPasswordResponse(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.getString("refresh_token"),
                    kdfSalt = json.getString("kdf_salt"),
                    userId = json.getString("user_id"),
                    role = json.optString("role").takeIf { it.isNotBlank() },
                    passwordKeyEnvelope = json.getString("password_key_envelope"),
                    passwordKeyNonce = json.getString("password_key_nonce"),
                    recoveryKeyEnvelope = json.optString("recovery_key_envelope"),
                    recoveryKeyNonce = json.optString("recovery_key_nonce")
                )
            )
        }
    }

    /**
     * Soft-deletes the signed-in account (server flag isDeleted=true). The password-derived authentication secret is re-verified
     * by the server. The account stays restorable for a grace period by signing in again.
     *
     * The server revokes every refresh token the moment this succeeds, so [onDeleted] runs while
     * holding the same monitor as [AuthenticatedApiClient.request]: no concurrent request can
     * try to refresh with a dead token (which would be reported as an unexpected session end)
     * before the local session has been dropped.
     */
    fun deleteAccount(authSecret: String, onDeleted: () -> Unit) {
        val body = JSONObject().put("auth_secret", authSecret)
        synchronized(client) {
            val response = client.request("POST", "/api/v1/account/delete", body)
            if (response.statusCode !in 200..299) {
                throw ApiException(response.statusCode, response.errorCode(), response.errorMessage("حذف حساب ناموفق بود."))
            }
            onDeleted()
        }
    }

    data class ChangePasswordInput(
        val currentAuthSecret: String,
        val newAuthSecret: String,
        val deviceId: String,
        val kdfSalt: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val deviceInfo: JSONObject? = null
    )
}