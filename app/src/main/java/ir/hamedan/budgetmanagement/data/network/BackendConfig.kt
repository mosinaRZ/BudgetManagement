package ir.hamedan.budgetmanagement.data.network

import ir.hamedan.budgetmanagement.BuildConfig

/** Single source of truth for the backend origin. Production must use HTTPS. */
object BackendConfig {
    val BASE_URL: String = BuildConfig.BASE_URL.trimEnd('/') + "/"
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 30_000
}