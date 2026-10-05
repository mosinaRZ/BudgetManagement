package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject

/** Authenticated account operations (the caller must already hold a valid session). */
class AccountApi(private val client: AuthenticatedApiClient) {

    /**
     * Changes the account password. The server verifies [ChangePasswordInput.currentPassword],
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
            .put("current_password", input.currentPassword)
            .put("new_password", input.newPassword)
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

    data class ChangePasswordInput(
        val currentPassword: String,
        val newPassword: String,
        val deviceId: String,
        val kdfSalt: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val deviceInfo: JSONObject? = null
    )
}