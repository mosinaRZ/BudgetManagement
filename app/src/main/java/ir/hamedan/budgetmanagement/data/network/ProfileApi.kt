package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject

/** Authenticated account-profile contract. */
class ProfileApi(private val client: AuthenticatedApiClient) {

    fun getProfile(): Profile {
        val response = client.request("GET", "/api/v1/account/profile")
        ensureSuccess(response, "دریافت پروفایل ناموفق بود.")
        return parse(response.json())
    }

    fun updateProfile(input: UpdateProfileInput): Profile {
        val body = JSONObject()
            .put("first_name", input.firstName)
            .put("last_name", input.lastName)
            .put("gender", input.gender)
        input.birthDate?.let { body.put("birth_date", it) }
        val response = client.request("PUT", "/api/v1/account/profile", body)
        ensureSuccess(response, "ذخیره پروفایل ناموفق بود.")
        return parse(response.json())
    }

    fun updateEmail(input: UpdateEmailInput): Profile {
        val body = JSONObject()
            .put("email", input.email)
            .put("otp_challenge_id", input.otpChallengeId)
            .put("otp_code", input.otpCode)
        val response = client.request("PUT", "/api/v1/account/email", body)
        ensureSuccess(response, "تأیید ایمیل ناموفق بود.")
        return parse(response.json())
    }

    private fun parse(json: JSONObject) = Profile(
        userId = json.getString("user_id"),
        phoneNumber = json.optString("phone_number"),
        email = json.optString("email").takeIf { it.isNotBlank() },
        emailVerified = json.optBoolean("email_verified", false),
        firstName = json.optString("first_name"),
        lastName = json.optString("last_name"),
        gender = json.optString("gender").ifBlank { "prefer_not_to_say" },
        birthDate = json.optString("birth_date").takeIf { it.isNotBlank() }
    )

    private fun ensureSuccess(response: ApiHttpClient.ApiResponse, message: String) {
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorCode(), response.errorMessage(message))
        }
    }

    data class Profile(
        val userId: String,
        val phoneNumber: String,
        val email: String?,
        val emailVerified: Boolean,
        val firstName: String,
        val lastName: String,
        val gender: String,
        val birthDate: String?
    )

    data class UpdateProfileInput(
        val firstName: String,
        val lastName: String,
        val gender: String,
        val birthDate: String?
    )

    data class UpdateEmailInput(
        val email: String,
        val otpChallengeId: String,
        val otpCode: String
    )
}