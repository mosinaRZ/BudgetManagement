package ir.hamedan.budgetmanagement.data.local

import androidx.room.withTransaction
import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore

class SyncLocalDataSourceImpl(
    private val database: AppDatabase,
    private val deviceIdentityStore: DeviceIdentityStore
) : SyncLocalDataSource {

    override suspend fun <T> transaction(block: suspend () -> T): T =
        database.withTransaction { block() }

    override suspend fun <T> mutate(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        block: suspend () -> T
    ): T = database.withTransaction {
        val result = block()
        recordMutationInTransaction(entityType, entityId, updatedAt, false)
        result
    }

    override suspend fun <T> delete(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        block: suspend () -> T
    ): T = database.withTransaction {
        val result = block()
        recordMutationInTransaction(entityType, entityId, updatedAt, true)
        result
    }

    override suspend fun recordMutationInTransaction(
        entityType: String,
        entityId: String,
        updatedAt: Long,
        isDeleted: Boolean
    ) {
        require(entityType.isNotBlank()) { "Entity type cannot be blank." }
        require(entityId.isNotBlank()) { "Entity id cannot be blank." }

        val metadataDao = database.syncMetadataDao()
        val stateDao = database.syncStateDao()
        val existing = metadataDao.get(entityType, entityId)
        val state = stateDao.get() ?: SyncStateEntity(
            id = 1,
            deviceId = deviceIdentityStore.getOrCreate()
        ).also { stateDao.upsert(it) }

        metadataDao.upsert(
            SyncMetadataEntity(
                entityType = entityType,
                entityId = entityId,
                version = (existing?.version ?: 0) + 1,
                lastSyncedVersion = existing?.lastSyncedVersion ?: 0,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                deviceId = state.deviceId,
                serverRevision = existing?.serverRevision ?: 0L
            )
        )
        stateDao.markSyncRequired()
    }

    override suspend fun getMetadata(entityType: String, entityId: String): SyncMetadataEntity? =
        database.syncMetadataDao().get(entityType, entityId)

    override suspend fun saveMetadata(metadata: SyncMetadataEntity) {
        database.syncMetadataDao().upsert(metadata)
    }

    override suspend fun markDeleted(entityType: String, entityId: String) {
        database.withTransaction {
            recordMutationInTransaction(entityType, entityId, System.currentTimeMillis(), true)
        }
    }

    override suspend fun getPendingTombstones(): List<SyncMetadataEntity> =
        database.syncMetadataDao().getAllTombstones()

    override suspend fun cleanupSyncedTombstones(serverRevision: Long) {
        require(serverRevision >= 0L)
        database.syncMetadataDao().deleteSyncedTombstones(serverRevision)
    }

    override suspend fun clearAccountData() {
        database.withTransaction {
            database.syncMetadataDao().clearAll()
            database.transactionDao().clearAll()
            database.categoryDao().clearAll()
            database.savingGoalDao().clearAll()
            database.budgetLimitDao().clearAll()
            database.debtCreditDao().clearAll()
            database.savingGoalOperationDao().clearAll()
            database.debtPaymentDao().clearAll()
            database.notificationDao().clearAll()
            database.pendingTransactionDao().clearAll()
            database.userDao().clearAll()
            database.syncStateDao().resetCursor()
        }
    }
}