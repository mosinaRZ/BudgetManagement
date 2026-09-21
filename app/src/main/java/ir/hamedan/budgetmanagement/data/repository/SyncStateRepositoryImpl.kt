package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.SyncStateDao
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
import kotlinx.coroutines.flow.Flow

class SyncStateRepositoryImpl(
    private val syncStateDao: SyncStateDao,
    private val deviceIdentityStore: DeviceIdentityStore
) : SyncStateRepository {

    override suspend fun get(): SyncStateEntity {
        return syncStateDao.get()
            ?: SyncStateEntity(
                id = 1,
                deviceId = deviceIdentityStore.getOrCreate()
            ).also { state -> syncStateDao.upsert(state) }
    }

    override fun observe(): Flow<SyncStateEntity?> = syncStateDao.observe()

    override suspend fun save(state: SyncStateEntity) {
        require(state.id == 1) { "SyncStateEntity.id must always be 1." }
        require(state.deviceId.isNotBlank()) { "Device ID cannot be blank." }
        require(state.lastServerRevision >= 0L) { "Server revision cannot be negative." }
        syncStateDao.upsert(state)
    }

    override suspend fun markSyncSuccessful(serverRevision: Long, syncTime: Long): Int {
        require(serverRevision >= 0L) { "Server revision cannot be negative." }
        require(syncTime > 0L) { "Sync time must be greater than zero." }
        return syncStateDao.markSyncSuccessful(serverRevision, syncTime)
    }

    override suspend fun markSyncRequired(): Int = syncStateDao.markSyncRequired()

    override suspend fun getDeviceId(): String = get().deviceId
}