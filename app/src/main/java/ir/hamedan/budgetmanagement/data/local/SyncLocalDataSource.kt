package ir.hamedan.budgetmanagement.data.local

import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity

interface SyncLocalDataSource {
    suspend fun <T> transaction(block: suspend () -> T): T

    /** Execute a business mutation and atomically record its sync metadata. */
    suspend fun <T> mutate(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        block: suspend () -> T
    ): T

    /** Execute a deletion and atomically create its tombstone. */
    suspend fun <T> delete(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        block: suspend () -> T
    ): T

    /** Must be called from an existing Room transaction. */
    suspend fun recordMutationInTransaction(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        isDeleted: Boolean = false
    )

    suspend fun getMetadata(entityType: String, entityId: String): SyncMetadataEntity?
    suspend fun saveMetadata(metadata: SyncMetadataEntity)
    suspend fun markDeleted(entityType: String, entityId: String)
    suspend fun getPendingTombstones(): List<SyncMetadataEntity>
    suspend fun cleanupSyncedTombstones(serverRevision: Long)

    /** Clear all account-scoped local data after a completed logout/account switch. */
    suspend fun clearAccountData()
}