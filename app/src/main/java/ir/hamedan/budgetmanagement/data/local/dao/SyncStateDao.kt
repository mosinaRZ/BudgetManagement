package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    @Query(
        """
        SELECT *
        FROM sync_state
        WHERE id = 1
        LIMIT 1
        """
    )
    suspend fun get(): SyncStateEntity?

    @Query(
        """
        SELECT *
        FROM sync_state
        WHERE id = 1
        LIMIT 1
        """
    )
    fun observe(): Flow<SyncStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query(
        """
        UPDATE sync_state
        SET lastServerRevision = :serverRevision,
            lastSuccessfulSyncAt = :syncTime,
            syncRequired = 0
        WHERE id = 1
        """
    )
    suspend fun markSyncSuccessful(
        serverRevision: Long,
        syncTime: Long
    ): Int

    @Query(
        """
        UPDATE sync_state
        SET syncRequired = 1
        WHERE id = 1
        """
    )
    suspend fun markSyncRequired(): Int

    @Query("""
        UPDATE sync_state
        SET lastServerRevision = 0,
            lastSuccessfulSyncAt = 0,
            accountUserId = NULL,
            syncRequired = 0
        WHERE id = 1
    """)
    suspend fun resetCursor(): Int
}