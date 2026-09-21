package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query(
        """
        SELECT *
        FROM sync_metadata
        WHERE entityType = :entityType
          AND entityId = :entityId
        LIMIT 1
        """
    )
    suspend fun get(
        entityType: String,
        entityId: String
    ): SyncMetadataEntity?

    @Query(
        """
        SELECT *
        FROM sync_metadata
        WHERE entityType = :entityType
        """
    )
    suspend fun getByEntityType(
        entityType: String
    ): List<SyncMetadataEntity>

    @Query(
        """
        SELECT *
        FROM sync_metadata
        WHERE isDeleted = 0
          AND version > lastSyncedVersion
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getPendingMetadata(): List<SyncMetadataEntity>

    @Query(
        """
        SELECT *
        FROM sync_metadata
        WHERE isDeleted = 1
          AND version > lastSyncedVersion
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getPendingTombstones(): List<SyncMetadataEntity>

    @Query(
        """
        SELECT *
        FROM sync_metadata
        WHERE isDeleted = 1
        """
    )
    suspend fun getAllTombstones(): List<SyncMetadataEntity>

    @Query(
        """
        SELECT *
        FROM sync_metadata
        WHERE isDeleted = 0
        ORDER BY updatedAt ASC
        """
    )
    fun observeActiveMetadata(): Flow<List<SyncMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(
        metadata: List<SyncMetadataEntity>
    )

    @Query(
        """
        UPDATE sync_metadata
        SET version = :version,
            lastSyncedVersion = :lastSyncedVersion,
            updatedAt = :updatedAt,
            isDeleted = :isDeleted,
            deviceId = :deviceId,
            serverRevision = :serverRevision,
            encryptionKeyVersion = :encryptionKeyVersion
        WHERE entityType = :entityType
          AND entityId = :entityId
        """
    )
    suspend fun updateMetadata(
        entityType: String,
        entityId: String,
        version: Int,
        lastSyncedVersion: Int,
        updatedAt: Long,
        isDeleted: Boolean,
        deviceId: String,
        serverRevision: Long,
        encryptionKeyVersion: Int
    ): Int

    @Query(
        """
        UPDATE sync_metadata
        SET isDeleted = 1,
            version = :version,
            lastSyncedVersion = :lastSyncedVersion,
            updatedAt = :updatedAt,
            deviceId = :deviceId
        WHERE entityType = :entityType
          AND entityId = :entityId
        """
    )
    suspend fun markDeleted(
        entityType: String,
        entityId: String,
        version: Int,
        lastSyncedVersion: Int,
        updatedAt: Long,
        deviceId: String
    ): Int

    @Query("""
        UPDATE sync_metadata
        SET lastSyncedVersion = version,
            serverRevision = :serverRevision
        WHERE entityType = :entityType
          AND entityId = :entityId
          AND version = :version
    """)
    suspend fun markSynced(
        entityType: String,
        entityId: String,
        version: Int,
        serverRevision: Long
    ): Int

    @Query(
        """
        DELETE FROM sync_metadata
        WHERE entityType = :entityType
          AND entityId = :entityId
        """
    )
    suspend fun deleteMetadata(
        entityType: String,
        entityId: String
    ): Int

    @Query(
        """
        DELETE FROM sync_metadata
        WHERE isDeleted = 1
          AND serverRevision > 0
          AND serverRevision <= :serverRevision
        """
    )
    suspend fun deleteSyncedTombstones(
        serverRevision: Long
    ): Int

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll(): Int
}