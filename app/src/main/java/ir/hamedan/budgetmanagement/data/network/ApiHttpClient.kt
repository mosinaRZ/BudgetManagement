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
        val url = URL(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
        val connection = (url.openConnection() as HttpURLConnection).apply {
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

        return try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
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
        } finally {
            connection.disconnect()
        }
    }

    data class ApiResponse(val statusCode: Int, val body: String) {
        fun json(): JSONObject = if (body.isBlank()) JSONObject() else JSONObject(body)

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
    409 -> "CONFLICT"
    429 -> "RATE_LIMITED"
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
        "NOT_FOUND" -> if (isPersian) "اطلاعات موردنظر پیدا نشد." else "The requested information was not found."
        "CONFLICT" -> if (isPersian) "این عملیات با وضعیت فعلی اطلاعات سازگار نیست." else "The operation conflicts with the current data."
        "RATE_LIMITED" -> if (isPersian) "درخواست‌های زیادی ارسال شده است. کمی بعد دوباره تلاش کنید." else "Too many requests. Please try again later."
        "VALIDATION_ERROR", "BAD_REQUEST" -> message
        "INTERNAL_ERROR" -> if (isPersian) "خطای داخلی سرور رخ داد. لطفاً بعداً دوباره تلاش کنید." else "A server error occurred. Please try again later."
        else -> if (message.isNotBlank()) message else if (isPersian) "عملیات ناموفق بود." else "The operation failed."
    }
}