package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity

interface SyncMetadataRepository {

    suspend fun get(
        entityType: String,
        entityId: String
    ): SyncMetadataEntity?

    suspend fun upsert(
        metadata: SyncMetadataEntity
    )

    suspend fun markDeleted(
        entityType: String,
        entityId: String,
        deviceId: String,
        updatedAt: Long
    )

    suspend fun getPendingTombstones(): List<SyncMetadataEntity>

    suspend fun delete(
        entityType: String,
        entityId: String
    )

    suspend fun deleteOlderThan(
        serverRevision: Long
    )
}