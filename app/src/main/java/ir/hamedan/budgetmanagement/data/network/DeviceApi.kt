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
                        createdAt = item.optLong("created_at", 0L) * 1000L,
                        isPrimary = item.optBoolean("is_primary", false)
                    )
                )
            }
        }
    }

    /**
     * Device activity (new sign-ins, removals) that happened after [afterMillis]. The server only
     * reports things from after this device joined the account and never this device's own actions.
     */
    fun events(afterMillis: Long): EventPage {
        val response = client.request("GET", "/api/v1/devices/events?after=${afterMillis.coerceAtLeast(0L)}")
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorCode(), response.errorMessage("دریافت رویدادهای دستگاه ناموفق بود."))
        }
        val json = response.json()
        val array = json.optJSONArray("events")
        val events = buildList {
            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        DeviceEvent(
                            id = item.getString("id"),
                            type = item.text("type"),
                            deviceId = item.text("device_id"),
                            deviceName = item.text("device_name"),
                            platform = item.text("platform"),
                            actorName = item.text("actor_name"),
                            createdAtMillis = item.optLong("created_at_ms", 0L)
                        )
                    )
                }
            }
        }
        return EventPage(events, json.optLong("cursor_ms", 0L))
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
        val createdAt: Long,
        /**
         * The first device ever registered on the account. It can remove every other device,
         * but no other device can remove it. False when talking to an older server.
         */
        val isPrimary: Boolean = false
    )

    data class DeviceEvent(
        val id: String,
        /** [TYPE_SIGNED_IN] or [TYPE_REMOVED]. */
        val type: String,
        val deviceId: String,
        val deviceName: String,
        val platform: String,
        /** The device that caused the event; for a removal, the device that removed it. */
        val actorName: String,
        val createdAtMillis: Long
    )

    /** [cursorMillis] is 0 when nothing new was examined; keep the previous cursor then. */
    data class EventPage(val events: List<DeviceEvent>, val cursorMillis: Long)

    companion object {
        const val TYPE_SIGNED_IN = "signed_in"
        const val TYPE_REMOVED = "removed"
    }

    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key).trim()

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
}