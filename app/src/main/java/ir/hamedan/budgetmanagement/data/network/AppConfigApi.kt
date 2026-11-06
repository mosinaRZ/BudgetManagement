package ir.hamedan.budgetmanagement.data.network

import ir.hamedan.budgetmanagement.data.update.AppPolicySource
import ir.hamedan.budgetmanagement.data.update.AppUpdatePolicy

/** Public (unauthenticated) app configuration; readable before anyone has signed in. */
class AppConfigApi(private val client: ApiHttpClient) : AppPolicySource {

    override fun fetch(): AppUpdatePolicy {
        val response = client.request("GET", "/app-config")
        if (response.statusCode !in 200..299) {
            throw ApiException(response.statusCode, response.errorCode(), "App configuration request failed.")
        }
        return AppUpdatePolicy.fromJson(response.json())
    }
}