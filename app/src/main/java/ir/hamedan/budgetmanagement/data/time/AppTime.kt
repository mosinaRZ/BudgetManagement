package ir.hamedan.budgetmanagement.data.time

import java.time.Instant

/** Canonical time contract: Room uses epoch millis; HTTP uses RFC3339/ISO-8601 UTC. */
object ApiTime {
    fun toApi(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

    fun fromApi(value: String): Long = Instant.parse(value).toEpochMilli()

    fun nowMillis(): Long = System.currentTimeMillis()
}