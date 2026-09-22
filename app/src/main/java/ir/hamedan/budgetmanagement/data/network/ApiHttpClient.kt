package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

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
        } finally {
            connection.disconnect()
        }
    }

    data class ApiResponse(val statusCode: Int, val body: String) {
        fun json(): JSONObject = if (body.isBlank()) JSONObject() else JSONObject(body)

        fun errorMessage(default: String): String = try {
            json().optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: default
        } catch (_: Exception) {
            default
        }
    }
}

class ApiException(val statusCode: Int, override val message: String) : RuntimeException(message)