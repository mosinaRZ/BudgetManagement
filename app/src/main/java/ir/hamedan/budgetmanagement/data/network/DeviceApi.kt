package ir.hamedan.budgetmanagement.data.network

import org.json.JSONObject

class DeviceApi(private val client: AuthenticatedApiClient) {
    fun list(): List<Device> {
        val response = client.request("GET", "/api/v1/devices")
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorCode(), response.errorMessage("دریافت دستگاه‌ها ناموفق بود."))
        }
        val array = response.json().optJSONArray("devices") ?: return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    Device(
                        id = item.getString("id"),
                        name = item.text("name"),
                        model = item.text("model"),
                        platform = item.text("platform"),
                        osVersion = item.text("os_version"),
                        appVersion = item.text("app_version"),
                        lastIp = item.text("last_ip"),
                        lastSeenAt = item.optLong("last_seen_at", 0L) * 1000L,
                        createdAt = item.optLong("created_at", 0L) * 1000L
                    )
                )
            }
        }
    }

    fun revoke(deviceId: String) {
        require(deviceId.isNotBlank()) { "Device id cannot be blank." }
        val response = client.request("DELETE", "/api/v1/devices/${deviceId.urlEncode()}")
        if (response.statusCode !in 200..299 && response.statusCode != 204) {
            throw ApiException(response.statusCode, response.errorCode(), response.errorMessage("لغو دستگاه ناموفق بود."))
        }
    }

    /**
     * Everything except [id] is display-only and may be blank (older installs, or clients that
     * never sent a description). [lastIp] is already masked by the server (e.g. "203.0.113.*").
     */
    data class Device(
        val id: String,
        val name: String = "",
        val model: String = "",
        val platform: String = "",
        val osVersion: String = "",
        val appVersion: String = "",
        val lastIp: String = "",
        val lastSeenAt: Long,
        val createdAt: Long
    )

    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key).trim()

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
}