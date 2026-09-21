package ir.hamedan.budgetmanagement.data.network

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import ir.hamedan.budgetmanagement.data.time.ApiTime

class SyncApi(private val client: ApiHttpClient) {

    fun sync(
        accessToken: String,
        requestId: String,
        deviceId: String,
        cursor: Long,
        changes: List<SyncChange>
    ): SyncResponse {
        val body = JSONObject()
            .put("requestId", requestId)
            .put("deviceId", deviceId)
            .put("cursor", if (cursor == 0L) "" else cursor.toString())

        val array = JSONArray()
        changes.forEach { change ->
            array.put(
                JSONObject()
                    .put("entityType", change.entityType)
                    .put("entityId", change.entityId)
                    .put("ciphertext", Base64.encodeToString(change.ciphertext, Base64.NO_WRAP))
                    .put("nonce", Base64.encodeToString(change.nonce, Base64.NO_WRAP))
                    .put("version", change.version)
                    .put("isDeleted", change.isDeleted)
                    .put("updatedAt", ApiTime.toApi(change.updatedAt))
            )
        }
        body.put("changes", array)

        val response = client.request("POST", "/api/v1/sync", body, accessToken)
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorMessage("همگام‌سازی ناموفق بود."))
        }

        val json = response.json()
        return SyncResponse(
            changes = parseChanges(json.optJSONArray("changes")),
            conflicts = parseChanges(json.optJSONArray("conflicts")),
            nextCursor = json.optString("nextCursor").toLongOrNull() ?: cursor,
            hasMore = json.optBoolean("hasMore", false)
        )
    }

    private fun parseChanges(array: JSONArray?): List<SyncChange> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    SyncChange(
                        entityType = item.getString("entityType"),
                        entityId = item.getString("entityId"),
                        ciphertext = Base64.decode(item.optString("ciphertext"), Base64.DEFAULT),
                        nonce = Base64.decode(item.optString("nonce"), Base64.DEFAULT),
                        version = item.getInt("version"),
                        isDeleted = item.getBoolean("isDeleted"),
                        updatedAt = ApiTime.fromApi(item.getString("updatedAt")),
                        serverRevision = item.optLong("serverRevision", 0L)
                    )
                )
            }
        }
    }

    data class SyncChange(
        val entityType: String,
        val entityId: String,
        val ciphertext: ByteArray,
        val nonce: ByteArray,
        val version: Int,
        val isDeleted: Boolean,
        val updatedAt: Long,
        val serverRevision: Long = 0L
    )

    data class SyncResponse(
        val changes: List<SyncChange>,
        val conflicts: List<SyncChange>,
        val nextCursor: Long,
        val hasMore: Boolean
    )
}
