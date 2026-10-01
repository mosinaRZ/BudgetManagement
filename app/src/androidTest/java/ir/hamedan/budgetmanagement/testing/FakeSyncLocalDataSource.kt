package ir.hamedan.budgetmanagement.testing

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity

class FakeSyncLocalDataSource : SyncLocalDataSource {
    val mutations = mutableListOf<SyncMetadataEntity>()

    override suspend fun <T> transaction(block: suspend () -> T): T = block()

    override suspend fun <T> mutate(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        block: suspend () -> T
    ): T {
        val result = block()
        recordMutationInTransaction(entityType, entityId, updatedAt)
        return result
    }

    override suspend fun <T> delete(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        block: suspend () -> T
    ): T {
        val result = block()
        recordMutationInTransaction(entityType, entityId, updatedAt, true)
        return result
    }

    override suspend fun recordMutationInTransaction(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        isDeleted: Boolean
    ) {
        mutations += SyncMetadataEntity(
            entityType = entityType,
            entityId = entityId,
            version = mutations.count { it.entityType == entityType && it.entityId == entityId } + 1,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            deviceId = "test-device"
        )
    }

    override suspend fun getMetadata(entityType: String, entityId: String): SyncMetadataEntity? =
        mutations.lastOrNull { it.entityType == entityType && it.entityId == entityId }

    override suspend fun saveMetadata(metadata: SyncMetadataEntity) {
        mutations.removeAll { it.entityType == metadata.entityType && it.entityId == metadata.entityId }
        mutations += metadata
    }

    override suspend fun markDeleted(entityType: String, entityId: String) {
        recordMutationInTransaction(entityType, entityId, System.currentTimeMillis(), true)
    }

    override suspend fun getPendingTombstones(): List<SyncMetadataEntity> =
        mutations.filter { it.isDeleted }

    override suspend fun cleanupSyncedTombstones(serverRevision: Long) {
        mutations.removeAll { it.isDeleted && it.serverRevision <= serverRevision }
    }

    override suspend fun clearAccountData() {
        mutations.clear()
    }
}