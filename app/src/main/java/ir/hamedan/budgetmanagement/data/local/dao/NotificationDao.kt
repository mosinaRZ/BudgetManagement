package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query(
        """
        SELECT *
        FROM notifications
        ORDER BY timestamp DESC
        """
    )
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("""
        SELECT *
        FROM notifications
    """)
    suspend fun getAllNotificationsSnapshot(): List<NotificationEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM notifications
        WHERE isRead = 0
        """
    )
    fun getUnreadCount(): Flow<Int>

    @Query(
        """
        SELECT *
        FROM notifications
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query(
        """
        UPDATE notifications
        SET isRead = 1,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markAsRead(
        id: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE notifications
        SET isRead = 1,
            updatedAt = :updatedAt
        WHERE isRead = 0
        """
    )
    suspend fun markAllAsRead(
        updatedAt: Long
    ): Int

    @Query(
        """
        DELETE FROM notifications
        WHERE id = :id
        """
    )
    suspend fun deleteById(id: String): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM notifications
        WHERE tag = :tag
        """
    )
    suspend fun countByTag(tag: String): Int

    @Query("DELETE FROM notifications")
    suspend fun clearAll(): Int
}