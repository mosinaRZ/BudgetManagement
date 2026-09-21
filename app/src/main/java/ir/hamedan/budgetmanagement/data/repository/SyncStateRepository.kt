package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import kotlinx.coroutines.flow.Flow

interface SyncStateRepository {

    suspend fun get(): SyncStateEntity

    fun observe(): Flow<SyncStateEntity?>

    suspend fun save(state: SyncStateEntity)

    suspend fun markSyncSuccessful(
        serverRevision: Long,
        syncTime: Long
    ): Int

    suspend fun markSyncRequired(): Int

    suspend fun getDeviceId(): String
}