package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.SyncMetadataDao
import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity

class SyncMetadataRepositoryImpl(
    private val syncMetadataDao: SyncMetadataDao
) : SyncMetadataRepository {

    override suspend fun get(
        entityType: String,
        entityId: String
    ): SyncMetadataEntity? =
        syncMetadataDao.get(
            entityType = entityType,
            entityId = entityId
        )

    override suspend fun upsert(
        metadata: SyncMetadataEntity
    ) {
        syncMetadataDao.upsert(metadata)
    }

    override suspend fun markDeleted(
        entityType: String,
        entityId: String,
        deviceId: String,
        updatedAt: Long
    ) {
        val existing = syncMetadataDao.get(
            entityType = entityType,
            entityId = entityId
        )

        val metadata = SyncMetadataEntity(
            entityType = entityType,
            entityId = entityId,
            version = (existing?.version ?: 0) + 1,
            lastSyncedVersion = existing?.lastSyncedVersion ?: 0,
            updatedAt = updatedAt,
            isDeleted = true,
            deviceId = deviceId,
            serverRevision = existing?.serverRevision ?: 0L
        )

        syncMetadataDao.upsert(metadata)
    }

    override suspend fun getPendingTombstones(): List<SyncMetadataEntity> =
        syncMetadataDao.getAllTombstones()

    override suspend fun delete(
        entityType: String,
        entityId: String
    ) {
        syncMetadataDao.deleteMetadata(
            entityType = entityType,
            entityId = entityId
        )
    }

    override suspend fun deleteOlderThan(
        serverRevision: Long
    ) {
        syncMetadataDao.deleteSyncedTombstones(
            serverRevision = serverRevision
        )
    }
}