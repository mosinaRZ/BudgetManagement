package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(

    @PrimaryKey
    val id: Int = 1,

    /**
     * Last globally processed server revision.
     */
    val lastServerRevision: Long = 0L,

    /**
     * Current device identifier.
     */
    val deviceId: String = "",

    /** Account currently bound to this local database. Null means no authenticated account. */
    val accountUserId: String? = null,

    /** Sync-state schema version for future migrations. */
    val syncSchemaVersion: Int = 1,

    /**
     * Last successful synchronization time.
     */
    val lastSuccessfulSyncAt: Long = 0L,

    /**
     * Whether a sync operation is currently pending.
     */
    val syncRequired: Boolean = false
)