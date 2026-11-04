package ir.hamedan.budgetmanagement.data.network

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.TimeZone

/**
 * Client for the admin console endpoints (/api/v1/admin/...). Every call is authenticated through
 * [AuthenticatedApiClient]; the server re-checks that the caller is still an active administrator
 * on each request, so a demoted admin gets 403 here even while their token is still valid.
 */
class AdminApi(private val client: AuthenticatedApiClient) {

    // ------------------------------------------------------------------ models

    data class AdminDevice(
        val id: String,
        val name: String,
        val model: String,
        val platform: String,
        val osVersion: String,
        val appVersion: String,
        val lastIp: String,
        val lastSeenAt: Long,
        val createdAt: Long,
        val online: Boolean
    ) {
        val displayName: String get() = name.ifBlank { model }
    }

    data class AdminUser(
        val id: String,
        val phoneNumber: String,
        val phoneVerified: Boolean,
        val firstName: String,
        val lastName: String,
        val gender: String,
        val birthDate: String,
        val role: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isDeleted: Boolean,
        val deletedAt: Long,
        val lockedUntil: Long,
        val failedLoginAttempts: Int,
        val online: Boolean,
        val lastSeenAt: Long,
        val deviceCount: Int,
        val platforms: List<String>,
        val appVersion: String,
        val syncRecords: Long,
        val syncBytes: Long
    ) {
        val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
        val isLocked: Boolean get() = lockedUntil > System.currentTimeMillis()
        val isAdmin: Boolean get() = role == "admin"
    }

    data class UserPage(val users: List<AdminUser>, val total: Long, val limit: Int, val offset: Int, val onlineWindowSec: Long)

    data class UserQuery(
        val search: String = "",
        val role: String = "",
        val status: String = "",
        val presence: String = "",
        val sort: String = "newest",
        val limit: Int = 30,
        val offset: Int = 0
    )

    data class AdminEvent(
        val id: String,
        val type: String,
        val severity: String,
        val userId: String,
        val userLabel: String,
        val actorUserId: String,
        val actorLabel: String,
        val deviceName: String,
        val platform: String,
        val appVersion: String,
        val ip: String,
        val detail: String,
        val createdAt: Long
    )

    data class EventPage(val events: List<AdminEvent>, val total: Long, val limit: Int, val offset: Int)

    data class UserDetail(val user: AdminUser, val devices: List<AdminDevice>, val events: List<AdminEvent>)

    data class EventQuery(
        val severity: String = "",
        val types: Set<String> = emptySet(),
        val userId: String = "",
        val days: Int = 30,
        val limit: Int = 50,
        val offset: Int = 0
    )

    data class Bucket(val key: String, val count: Long)
    data class LoginDay(val day: String, val success: Long, val failed: Long)
    data class SeverityDay(val day: String, val info: Long, val warning: Long, val danger: Long)
    data class SyncDay(val day: String, val records: Long, val bytes: Long)
    data class SignupDay(val day: String, val count: Long)
    data class TopSyncUser(val userId: String, val label: String, val records: Long, val bytes: Long)
    data class SuspiciousSource(val ip: String, val failed: Long, val accounts: Long)

    data class Kpis(
        val totalUsers: Long,
        val activeUsers: Long,
        val deletedUsers: Long,
        val lockedUsers: Long,
        val onlineNow: Long,
        val newUsers: Long,
        val loginsSuccess: Long,
        val loginsFailed: Long,
        val uniqueLoginUsers: Long,
        val dangerEvents: Long,
        val lockedEvents: Long,
        val syncRecords: Long,
        val syncBytes: Long
    )

    data class Analytics(
        val fromMs: Long,
        val toMs: Long,
        val days: Int,
        val kpis: Kpis,
        val riskScore: Int,
        val riskLevel: String,
        val loginsByDay: List<LoginDay>,
        val severityByDay: List<SeverityDay>,
        val signupsByDay: List<SignupDay>,
        val heatmap: List<List<Long>>,
        val eventTypes: List<Bucket>,
        val roles: List<Bucket>,
        val platforms: List<Bucket>,
        val appVersions: List<Bucket>,
        val syncByDay: List<SyncDay>,
        val syncByEntity: List<Bucket>,
        val topSyncUsers: List<TopSyncUser>,
        val suspicious: List<SuspiciousSource>,
        val eventsTruncated: Boolean,
        val eventsConsidered: Int,
        val onlineWindowSec: Long
    )

    data class AnalyticsQuery(
        val days: Int = 30,
        val severity: String = "",
        val types: Set<String> = emptySet(),
        val platform: String = "",
        val userId: String = ""
    )

    // ------------------------------------------------------------------ calls

    fun me() {
        ensure(client.request("GET", "/api/v1/admin/me"), "Admin check failed.")
    }

    fun listUsers(q: UserQuery): UserPage {
        val params = buildString {
            append("limit=${q.limit}&offset=${q.offset}&sort=${q.sort.enc()}")
            if (q.search.isNotBlank()) append("&q=${q.search.trim().enc()}")
            if (q.role.isNotBlank()) append("&role=${q.role.enc()}")
            if (q.status.isNotBlank()) append("&status=${q.status.enc()}")
            if (q.presence.isNotBlank()) append("&presence=${q.presence.enc()}")
        }
        val json = ensure(client.request("GET", "/api/v1/admin/users?$params"), "Loading users failed.")
        return UserPage(
            users = json.optJSONArray("users").mapObjects { parseUser(it) },
            total = json.optLong("total", 0L),
            limit = json.optInt("limit", q.limit),
            offset = json.optInt("offset", q.offset),
            onlineWindowSec = json.optLong("online_window_sec", 180L)
        )
    }

    fun getUser(id: String): UserDetail {
        val json = ensure(client.request("GET", "/api/v1/admin/users/${id.enc()}"), "Loading user failed.")
        return UserDetail(
            user = parseUser(json.getJSONObject("user")),
            devices = json.optJSONArray("devices").mapObjects { parseDevice(it) },
            events = json.optJSONArray("events").mapObjects { parseEvent(it) }
        )
    }

    /** Only the non-null arguments are sent; the server leaves everything else unchanged. */
    fun updateUser(id: String, firstName: String? = null, lastName: String? = null, role: String? = null): AdminUser {
        val body = JSONObject()
        if (firstName != null) body.put("first_name", firstName)
        if (lastName != null) body.put("last_name", lastName)
        if (role != null) body.put("role", role)
        val json = ensure(client.request("PUT", "/api/v1/admin/users/${id.enc()}", body), "Updating user failed.")
        return parseUser(json)
    }

    fun deleteUser(id: String): AdminUser =
        parseUser(ensure(client.request("DELETE", "/api/v1/admin/users/${id.enc()}"), "Deleting user failed."))

    fun restoreUser(id: String): AdminUser =
        parseUser(ensure(client.request("POST", "/api/v1/admin/users/${id.enc()}/restore", JSONObject()), "Restoring user failed."))

    fun unlockUser(id: String): AdminUser =
        parseUser(ensure(client.request("POST", "/api/v1/admin/users/${id.enc()}/unlock", JSONObject()), "Unlocking user failed."))

    fun revokeSessions(id: String): AdminUser =
        parseUser(ensure(client.request("POST", "/api/v1/admin/users/${id.enc()}/revoke-sessions", JSONObject()), "Revoking sessions failed."))

    fun events(q: EventQuery): EventPage {
        val params = buildString {
            append("days=${q.days}&limit=${q.limit}&offset=${q.offset}")
            if (q.severity.isNotBlank()) append("&severity=${q.severity.enc()}")
            if (q.types.isNotEmpty()) append("&type=${q.types.joinToString(",").enc()}")
            if (q.userId.isNotBlank()) append("&user_id=${q.userId.enc()}")
        }
        val json = ensure(client.request("GET", "/api/v1/admin/events?$params"), "Loading events failed.")
        return EventPage(
            events = json.optJSONArray("events").mapObjects { parseEvent(it) },
            total = json.optLong("total", 0L),
            limit = json.optInt("limit", q.limit),
            offset = json.optInt("offset", q.offset)
        )
    }

    fun analytics(q: AnalyticsQuery): Analytics {
        val tzMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
        val params = buildString {
            append("days=${q.days}&tz=$tzMinutes")
            if (q.severity.isNotBlank()) append("&severity=${q.severity.enc()}")
            if (q.types.isNotEmpty()) append("&type=${q.types.joinToString(",").enc()}")
            if (q.platform.isNotBlank()) append("&platform=${q.platform.enc()}")
            if (q.userId.isNotBlank()) append("&user_id=${q.userId.enc()}")
        }
        val json = ensure(client.request("GET", "/api/v1/admin/analytics?$params"), "Loading analytics failed.")
        val k = json.optJSONObject("kpis") ?: JSONObject()
        val risk = json.optJSONObject("risk") ?: JSONObject()
        val heat = json.optJSONArray("heatmap")
        val heatmap = buildList<List<Long>> {
            if (heat != null) for (r in 0 until heat.length()) {
                val row = heat.optJSONArray(r)
                add(buildList<Long> { if (row != null) for (c in 0 until row.length()) add(row.optLong(c, 0L)) })
            }
        }
        return Analytics(
            fromMs = json.optLong("from_ms", 0L),
            toMs = json.optLong("to_ms", 0L),
            days = json.optInt("days", q.days),
            kpis = Kpis(
                totalUsers = k.optLong("total_users"), activeUsers = k.optLong("active_users"),
                deletedUsers = k.optLong("deleted_users"), lockedUsers = k.optLong("locked_users"),
                onlineNow = k.optLong("online_now"), newUsers = k.optLong("new_users"),
                loginsSuccess = k.optLong("logins_success"), loginsFailed = k.optLong("logins_failed"),
                uniqueLoginUsers = k.optLong("unique_login_users"), dangerEvents = k.optLong("danger_events"),
                lockedEvents = k.optLong("locked_events"), syncRecords = k.optLong("sync_records"),
                syncBytes = k.optLong("sync_bytes")
            ),
            riskScore = risk.optInt("score", 0),
            riskLevel = risk.optString("level", "low"),
            loginsByDay = json.optJSONArray("logins_by_day").mapObjects {
                LoginDay(it.text("day"), it.optLong("success"), it.optLong("failed"))
            },
            severityByDay = json.optJSONArray("severity_by_day").mapObjects {
                SeverityDay(it.text("day"), it.optLong("info"), it.optLong("warning"), it.optLong("danger"))
            },
            signupsByDay = json.optJSONArray("signups_by_day").mapObjects { SignupDay(it.text("day"), it.optLong("count")) },
            heatmap = heatmap,
            eventTypes = json.optJSONArray("event_types").mapObjects { parseBucket(it) },
            roles = json.optJSONArray("roles").mapObjects { parseBucket(it) },
            platforms = json.optJSONArray("platforms").mapObjects { parseBucket(it) },
            appVersions = json.optJSONArray("app_versions").mapObjects { parseBucket(it) },
            syncByDay = json.optJSONArray("sync_by_day").mapObjects {
                SyncDay(it.text("day"), it.optLong("records"), it.optLong("bytes"))
            },
            syncByEntity = json.optJSONArray("sync_by_entity").mapObjects { parseBucket(it) },
            topSyncUsers = json.optJSONArray("top_sync_users").mapObjects {
                TopSyncUser(it.text("user_id"), it.text("label"), it.optLong("records"), it.optLong("bytes"))
            },
            suspicious = json.optJSONArray("suspicious_sources").mapObjects {
                SuspiciousSource(it.text("ip"), it.optLong("failed"), it.optLong("accounts"))
            },
            eventsTruncated = json.optBoolean("events_truncated", false),
            eventsConsidered = json.optInt("events_considered", 0),
            onlineWindowSec = json.optLong("online_window_sec", 180L)
        )
    }

    // ------------------------------------------------------------------ parsing

    private fun ensure(response: ApiHttpClient.ApiResponse, fallback: String): JSONObject {
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorCode(), response.errorMessage(fallback))
        }
        return response.json()
    }

    private fun parseBucket(o: JSONObject) = Bucket(o.text("key"), o.optLong("count"))

    private fun parseDevice(o: JSONObject) = AdminDevice(
        id = o.text("id"), name = o.text("name"), model = o.text("model"), platform = o.text("platform"),
        osVersion = o.text("os_version"), appVersion = o.text("app_version"), lastIp = o.text("last_ip"),
        lastSeenAt = o.optLong("last_seen_at_ms", 0L), createdAt = o.optLong("created_at_ms", 0L),
        online = o.optBoolean("online", false)
    )

    private fun parseUser(o: JSONObject): AdminUser {
        val platforms = o.optJSONArray("platforms")
        return AdminUser(
            id = o.text("id"), phoneNumber = o.text("phone_number"), phoneVerified = o.optBoolean("phone_verified", false),
            firstName = o.text("first_name"), lastName = o.text("last_name"), gender = o.text("gender"),
            birthDate = o.text("birth_date"), role = o.text("role").ifBlank { "user" },
            createdAt = o.optLong("created_at_ms", 0L), updatedAt = o.optLong("updated_at_ms", 0L),
            isDeleted = o.optBoolean("is_deleted", false), deletedAt = o.optLong("deleted_at_ms", 0L),
            lockedUntil = o.optLong("locked_until_ms", 0L), failedLoginAttempts = o.optInt("failed_login_attempts", 0),
            online = o.optBoolean("online", false), lastSeenAt = o.optLong("last_seen_at_ms", 0L),
            deviceCount = o.optInt("device_count", 0),
            platforms = buildList<String> { if (platforms != null) for (i in 0 until platforms.length()) add(platforms.optString(i)) },
            appVersion = o.text("app_version"), syncRecords = o.optLong("sync_records", 0L), syncBytes = o.optLong("sync_bytes", 0L)
        )
    }

    private fun parseEvent(o: JSONObject) = AdminEvent(
        id = o.text("id"), type = o.text("type"), severity = o.text("severity"), userId = o.text("user_id"),
        userLabel = o.text("user_label"), actorUserId = o.text("actor_user_id"), actorLabel = o.text("actor_label"),
        deviceName = o.text("device_name"), platform = o.text("platform"), appVersion = o.text("app_version"),
        ip = o.text("ip"), detail = o.text("detail"), createdAt = o.optLong("created_at_ms", 0L)
    )

    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key).trim()

    private fun String.enc(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) optJSONObject(i)?.let { out.add(transform(it)) }
        return out
    }
}