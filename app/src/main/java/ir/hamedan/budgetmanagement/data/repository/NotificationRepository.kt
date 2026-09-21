package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun getAllNotifications(): Flow<List<NotificationEntity>>

    fun getUnreadCount(): Flow<Int>

    suspend fun insert(notification: NotificationEntity)
    suspend fun countByTag(tag: String): Int
    suspend fun markAsRead(id: String)

    suspend fun markAllAsRead()

    suspend fun deleteById(id: String)
}