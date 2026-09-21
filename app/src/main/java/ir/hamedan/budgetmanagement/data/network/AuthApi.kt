package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject

class AuthApi(private val client: ApiHttpClient) {

    fun login(identifier: String, password: String, deviceId: String): LoginResponse {
        val body = JSONObject()
            .put("identifier", identifier)
            .put("password", password)
            .put("device_id", deviceId)

        val response = client.request("POST", "/auth/login", body)
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorMessage("ورود ناموفق بود."))
        }

        val json = response.json()
        return LoginResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            kdfSalt = json.optString("kdf_salt"),
            userId = json.getString("user_id"),
            passwordKeyEnvelope = json.optString("password_key_envelope"),
            passwordKeyNonce = json.optString("password_key_nonce"),
            recoveryKeyEnvelope = json.optString("recovery_key_envelope"),
            recoveryKeyNonce = json.optString("recovery_key_nonce")
        )
    }

    fun refresh(refreshToken: String): RefreshResponse {
        val body = JSONObject().put("refresh_token", refreshToken)
        val response = client.request("POST", "/auth/refresh", body)
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorMessage("نشست منقضی شده است."))
        }
        val json = response.json()
        return RefreshResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token")
        )
    }

    fun logout(refreshToken: String) {
        val response = client.request(
            "POST",
            "/auth/logout",
            JSONObject().put("refresh_token", refreshToken)
        )
        if (response.statusCode !in 200..299 && response.statusCode != 204) {
            throw ApiException(response.statusCode, response.errorMessage("خروج ناموفق بود."))
        }
    }

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val kdfSalt: String,
        val userId: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    data class RefreshResponse(
        val accessToken: String,
        val refreshToken: String
    )

    class ApiException(val statusCode: Int, override val message: String) : RuntimeException(message)
}