package ir.hamedan.budgetmanagement.data.update

import org.json.JSONObject

/** Where the update policy comes from (the server in production, a fake in tests). */
fun interface AppPolicySource {
    fun fetch(): AppUpdatePolicy
}

/**
 * What the server says about app versions (GET /app-config).
 *
 * @property minVersion oldest version that may still be used; older builds are blocked. Null = no limit.
 * @property latestVersion newest published version (shown on the update screen).
 * @property updateUrls store key ("play", "bazaar", "myket", "default") to the https page of the update.
 */
data class AppUpdatePolicy(
    val minVersion: String?,
    val latestVersion: String?,
    val updateUrls: Map<String, String>
) {
    fun toJson(): JSONObject = JSONObject()
        .put("minVersion", minVersion ?: JSONObject.NULL)
        .put("latestVersion", latestVersion ?: JSONObject.NULL)
        .put("updateUrls", JSONObject(updateUrls))

    companion object {
        fun fromJson(json: JSONObject): AppUpdatePolicy {
            val urls = mutableMapOf<String, String>()
            json.optJSONObject("updateUrls")?.let { obj ->
                obj.keys().forEach { key ->
                    val value = obj.optString(key).trim()
                    // Only https links are ever opened from a server-provided value.
                    if (value.startsWith("https://")) urls[key] = value
                }
            }
            return AppUpdatePolicy(
                minVersion = json.optStringOrNull("minVersion"),
                latestVersion = json.optStringOrNull("latestVersion"),
                updateUrls = urls
            )
        }

        private fun JSONObject.optStringOrNull(name: String): String? =
            if (isNull(name)) null else optString(name).trim().ifEmpty { null }
    }
}
