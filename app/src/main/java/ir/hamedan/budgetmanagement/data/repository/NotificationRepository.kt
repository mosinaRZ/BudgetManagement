package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    fun getUnreadCount(): Flow<Int>
    suspend fun addNotification(notification: NotificationEntity): Boolean
    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead()
    suspend fun deleteById(id: String)
}