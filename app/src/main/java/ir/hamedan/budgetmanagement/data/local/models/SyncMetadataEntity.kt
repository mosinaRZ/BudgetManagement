package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_metadata",
    primaryKeys = ["entityType", "entityId"],
    indices = [
        Index(value = ["entityType"]),
        Index(value = ["entityId"]),
        Index(value = ["serverRevision"]),
        Index(value = ["updatedAt"])
    ]
)
data class SyncMetadataEntity(

    /**
     * One of:
     *
     * TRANSACTION
     * CATEGORY
     * BUDGET_LIMIT
     * DEBT_CREDIT
     * SAVING_GOAL
     * SAVING_GOAL_OPERATION
     * DEBT_PAYMENT
     */
    val entityType: String,

    /**
     * ID of the corresponding business entity.
     */
    val entityId: String,

    /**
     * Canonical client-side sync version. This is the only version counter
     * used by the Android sync layer; business entities do not carry a
     * second sync version.
     */
    val version: Int = 1,

    /** Last local version confirmed by the server. */
    val lastSyncedVersion: Int = 0,

    /**
     * Last modification time known by sync layer.
     *
     * Stored as epoch milliseconds locally.
     */
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * Tombstone flag.
     *
     * true means the business entity has been deleted locally
     * but its deletion must still be synchronized.
     */
    val isDeleted: Boolean = false,

    /**
     * Device that last modified/synchronized this record.
     */
    val deviceId: String = "",

    /**
     * Last server revision received for this entity.
     *
     * 0 means this entity has not received a server revision yet.
     */
    val serverRevision: Long = 0L,

    /** Version of the local payload-encryption contract used for this record. */
    val encryptionKeyVersion: Int = 1
)