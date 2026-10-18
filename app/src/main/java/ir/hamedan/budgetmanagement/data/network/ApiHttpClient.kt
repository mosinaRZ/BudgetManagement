package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.UUID
import javax.net.ssl.SSLException

class ApiHttpClient(
    private val baseUrl: String = BackendConfig.BASE_URL
) {
    fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearerToken: String? = null
    ): ApiResponse {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = BackendConfig.CONNECT_TIMEOUT_MS
                readTimeout = BackendConfig.READ_TIMEOUT_MS
                doInput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Request-ID", UUID.randomUUID().toString())
                if (!bearerToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                }
                if (body != null) doOutput = true
            }
            connection = conn

            if (body != null) {
                conn.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }.orEmpty()

            ApiResponse(status, responseText)
        } catch (e: SocketTimeoutException) {
            throw ApiException(0, "NETWORK_ERROR", "Connection timed out.", e)
        } catch (e: UnknownHostException) {
            throw ApiException(0, "NETWORK_ERROR", "Unable to resolve the server.", e)
        } catch (e: ConnectException) {
            throw ApiException(0, "NETWORK_ERROR", "Unable to connect to the server.", e)
        } catch (e: SSLException) {
            throw ApiException(0, "NETWORK_ERROR", "Secure connection failed.", e)
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK_ERROR", "Network request failed.", e)
        } catch (e: ClassCastException) {
            throw ApiException(0, "NETWORK_ERROR", "Unsupported server address.", e)
        } catch (e: IllegalArgumentException) {
            throw ApiException(0, "NETWORK_ERROR", "Invalid server address.", e)
        } finally {
            connection?.disconnect()
        }
    }

    data class ApiResponse(val statusCode: Int, val body: String) {
        fun json(): JSONObject = try {
            if (body.isBlank()) JSONObject() else JSONObject(body)
        } catch (e: org.json.JSONException) {
            // A proxy or gateway error page (HTML) must not surface as a raw parser exception.
            throw ApiException(statusCode, "BAD_RESPONSE", "Unreadable server response.", e)
        }

        fun errorCode(): String? = try {
            json().optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

        fun errorMessage(default: String): String = try {
            json().optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: default
        } catch (_: Exception) {
            default
        }
    }
}

private fun Int.toErrorCode(): String = when (this) {
    400 -> "BAD_REQUEST"
    401 -> "UNAUTHORIZED"
    403 -> "FORBIDDEN"
    404 -> "NOT_FOUND"
    408 -> "REQUEST_TIMEOUT"
    409 -> "CONFLICT"
    413 -> "PAYLOAD_TOO_LARGE"
    422 -> "VALIDATION_ERROR"
    429 -> "RATE_LIMITED"
    502, 503, 504 -> "SERVICE_UNAVAILABLE"
    in 500..599 -> "INTERNAL_ERROR"
    else -> "UNKNOWN_ERROR"
}

class ApiException(
    val statusCode: Int,
    val code: String? = null,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    fun userMessage(isPersian: Boolean): String = when (code ?: statusCode.toErrorCode()) {
        "NETWORK_ERROR" -> if (isPersian) "اتصال به سرور برقرار نشد. اینترنت و اتصال سرور را بررسی کنید." else "Could not connect to the server. Check your internet connection."
        "UNAUTHORIZED" -> if (isPersian) "نشست شما منقضی شده است. دوباره وارد شوید." else "Your session has expired. Please sign in again."
        "FORBIDDEN" -> if (isPersian) "شما اجازه انجام این عملیات را ندارید." else "You are not allowed to perform this operation."
        "ACCOUNT_DELETED" -> if (isPersian) "این حساب حذف شده است." else "This account has been deleted."
        "INVALID_PASSWORD" -> if (isPersian) "گذرواژه نادرست است." else "The password is incorrect."
        "PRIMARY_DEVICE_PROTECTED" -> if (isPersian) "دستگاه اصلی فقط از خود همان دستگاه قابل حذف است." else "The primary device can only be removed from the primary device itself."
        "DEVICE_REMOVED" -> if (isPersian) "این دستگاه از حساب شما حذف شده است. دوباره وارد شوید." else "This device was removed from your account. Please sign in again."
        "DEVICE_REMOVAL_FORBIDDEN" -> if (isPersian) "فقط دستگاه اصلی می‌تواند دستگاه‌های دیگر را حذف کند." else "Only the primary device can remove other devices."
        "NOT_FOUND" -> if (isPersian) "اطلاعات موردنظر پیدا نشد." else "The requested information was not found."
        "CONFLICT" -> if (isPersian) "این عملیات با وضعیت فعلی اطلاعات سازگار نیست." else "The operation conflicts with the current data."
        "RATE_LIMITED" -> if (isPersian) "درخواست‌های زیادی ارسال شده است. کمی بعد دوباره تلاش کنید." else "Too many requests. Please try again later."
        "REQUEST_TIMEOUT" -> if (isPersian) "پاسخ سرور خیلی طول کشید. دوباره تلاش کنید." else "The server took too long to respond. Please try again."
        "PAYLOAD_TOO_LARGE" -> if (isPersian) "حجم اطلاعات ارسالی بیش از حد مجاز است." else "The data you sent is too large."
        "SERVICE_UNAVAILABLE" -> if (isPersian) "سرور موقتاً در دسترس نیست. کمی بعد دوباره تلاش کنید." else "The server is temporarily unavailable. Please try again shortly."
        "BAD_RESPONSE" -> if (isPersian) "پاسخ سرور قابل‌خواندن نبود. کمی بعد دوباره تلاش کنید." else "The server sent an unreadable response. Please try again shortly."
        "VALIDATION_ERROR", "BAD_REQUEST" -> if (isPersian) "اطلاعات واردشده معتبر نیست؛ ورودی‌ها را بررسی کنید." else "The information you entered is not valid. Please check your input."
        "INTERNAL_ERROR" -> if (isPersian) "خطای داخلی سرور رخ داد. لطفاً بعداً دوباره تلاش کنید." else "A server error occurred. Please try again later."
        else -> if (isPersian) "عملیات ناموفق بود. دوباره تلاش کنید." else "The operation failed. Please try again."
    }
}