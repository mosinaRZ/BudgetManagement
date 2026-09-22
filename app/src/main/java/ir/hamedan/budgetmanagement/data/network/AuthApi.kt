package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject

/** Versioned client contract for the authentication API. */
class AuthApi(private val client: ApiHttpClient) {

    fun requestOtp(destination: String, channel: String, purpose: String): OtpResponse {
        val body = JSONObject()
            .put("destination", destination.trim())
            .put("channel", channel)
            .put("purpose", purpose)
        val response = client.request("POST", "/auth/otp/request", body)
        ensureSuccess(response, "درخواست کد تأیید ناموفق بود.")
        val json = response.json()
        return OtpResponse(
            challengeId = json.getString("challenge_id"),
            expiresAt = json.getLong("expires_at")
        )
    }

    fun register(input: RegisterInput): RegisterResponse {
        val body = JSONObject()
            .put("phone_number", input.phoneNumber.trim())
            .put("password", input.password)
            .put("device_id", input.deviceId)
            .put("otp_challenge_id", input.otpChallengeId)
            .put("otp_code", input.otpCode)
            .put("kdf_salt", input.kdfSalt)
            .put("password_key_envelope", input.passwordKeyEnvelope)
            .put("password_key_nonce", input.passwordKeyNonce)
            .put("recovery_key_hash", input.recoveryKeyHash)
            .put("recovery_key_envelope", input.recoveryKeyEnvelope)
            .put("recovery_key_nonce", input.recoveryKeyNonce)
        input.email?.takeIf { it.isNotBlank() }?.let { body.put("email", it.trim()) }
        input.emailOtpChallengeId?.takeIf { it.isNotBlank() }?.let { body.put("email_otp_challenge_id", it) }
        input.emailOtpCode?.takeIf { it.isNotBlank() }?.let { body.put("email_otp_code", it) }

        val response = client.request("POST", "/auth/register", body)
        ensureSuccess(response, "ثبت‌نام ناموفق بود.")
        return parseRegisterResponse(response.json())
    }

    fun login(identifier: String, password: String, deviceId: String): LoginResponse {
        val body = JSONObject()
            .put("identifier", identifier)
            .put("password", password)
            .put("device_id", deviceId)

        val response = client.request("POST", "/auth/login", body)
        ensureSuccess(response, "ورود ناموفق بود.")
        return parseLoginResponse(response.json())
    }

    fun refresh(refreshToken: String): RefreshResponse {
        val body = JSONObject().put("refresh_token", refreshToken)
        val response = client.request("POST", "/auth/refresh", body)
        ensureSuccess(response, "نشست منقضی شده است.")
        val json = response.json()
        return RefreshResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            role = json.optString("role").takeIf { it.isNotBlank() }
        )
    }

    fun prepareRecovery(challengeId: String, otpCode: String, recoveryKey: String): RecoveryPreparation {
        val body = JSONObject()
            .put("otp_challenge_id", challengeId)
            .put("otp_code", otpCode)
            .put("recovery_key", recoveryKey)
        val response = client.request("POST", "/auth/recovery/prepare", body)
        ensureSuccess(response, "بازیابی حساب ناموفق بود.")
        val json = response.json()
        return RecoveryPreparation(
            recoverySessionToken = json.getString("recovery_session_token"),
            kdfSalt = json.getString("kdf_salt"),
            userId = json.getString("user_id"),
            recoveryKeyEnvelope = json.getString("recovery_key_envelope"),
            recoveryKeyNonce = json.getString("recovery_key_nonce")
        )
    }

    fun resetPassword(input: ResetPasswordInput): ResetPasswordResponse {
        val body = JSONObject()
            .put("recovery_session_token", input.recoverySessionToken)
            .put("new_password", input.newPassword)
            .put("device_id", input.deviceId)
            .put("kdf_salt", input.kdfSalt)
            .put("password_key_envelope", input.passwordKeyEnvelope)
            .put("password_key_nonce", input.passwordKeyNonce)
        val response = client.request("POST", "/auth/password/reset", body)
        ensureSuccess(response, "تغییر گذرواژه ناموفق بود.")
        val json = response.json()
        return ResetPasswordResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            kdfSalt = json.getString("kdf_salt"),
            userId = json.getString("user_id"),
            role = json.optString("role").takeIf { it.isNotBlank() },
            passwordKeyEnvelope = json.getString("password_key_envelope"),
            passwordKeyNonce = json.getString("password_key_nonce"),
            recoveryKeyEnvelope = json.getString("recovery_key_envelope"),
            recoveryKeyNonce = json.getString("recovery_key_nonce")
        )
    }

    fun logout(refreshToken: String) {
        val response = client.request("POST", "/auth/logout", JSONObject().put("refresh_token", refreshToken))
        if (response.statusCode !in 200..299 && response.statusCode != 204) {
            throw ApiException(response.statusCode, response.errorMessage("خروج ناموفق بود."))
        }
    }

    private fun parseLoginResponse(json: JSONObject) = LoginResponse(
        accessToken = json.getString("access_token"),
        refreshToken = json.getString("refresh_token"),
        kdfSalt = json.getString("kdf_salt"),
        userId = json.getString("user_id"),
        role = json.optString("role").takeIf { it.isNotBlank() },
        passwordKeyEnvelope = json.optString("password_key_envelope"),
        passwordKeyNonce = json.optString("password_key_nonce"),
        recoveryKeyEnvelope = json.optString("recovery_key_envelope"),
        recoveryKeyNonce = json.optString("recovery_key_nonce")
    )

    private fun parseRegisterResponse(json: JSONObject) = RegisterResponse(
        accessToken = json.getString("access_token"),
        refreshToken = json.getString("refresh_token"),
        kdfSalt = json.getString("kdf_salt"),
        userId = json.getString("user_id"),
        recoveryRequired = json.optBoolean("recovery_required", false),
        role = json.optString("role").takeIf { it.isNotBlank() },
        passwordKeyEnvelope = json.optString("password_key_envelope"),
        passwordKeyNonce = json.optString("password_key_nonce"),
        recoveryKeyEnvelope = json.optString("recovery_key_envelope"),
        recoveryKeyNonce = json.optString("recovery_key_nonce")
    )

    private fun ensureSuccess(response: ApiHttpClient.ApiResponse, defaultMessage: String) {
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorMessage(defaultMessage))
        }
    }

    data class OtpResponse(val challengeId: String, val expiresAt: Long)

    data class RegisterInput(
        val phoneNumber: String,
        val email: String?,
        val password: String,
        val deviceId: String,
        val otpChallengeId: String,
        val otpCode: String,
        val emailOtpChallengeId: String?,
        val emailOtpCode: String?,
        val kdfSalt: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val recoveryKeyHash: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val kdfSalt: String,
        val userId: String,
        val role: String?,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    data class RegisterResponse(
        val accessToken: String,
        val refreshToken: String,
        val kdfSalt: String,
        val userId: String,
        val recoveryRequired: Boolean,
        val role: String?,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    data class RefreshResponse(
        val accessToken: String,
        val refreshToken: String,
        val role: String?
    )

    data class RecoveryPreparation(
        val recoverySessionToken: String,
        val kdfSalt: String,
        val userId: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    data class ResetPasswordInput(
        val recoverySessionToken: String,
        val newPassword: String,
        val deviceId: String,
        val kdfSalt: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String
    )

    data class ResetPasswordResponse(
        val accessToken: String,
        val refreshToken: String,
        val kdfSalt: String,
        val userId: String,
        val role: String?,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    class ApiException(val statusCode: Int, override val message: String) : RuntimeException(message)
}