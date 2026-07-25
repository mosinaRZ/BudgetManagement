package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.NotificationDao
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {

    fun getAllNotifications(): Flow<List<NotificationEntity>> = dao.getAllNotifications()

    fun getUnreadCount(): Flow<Int> = dao.getUnreadCount()

    suspend fun addNotification(notification: NotificationEntity) {
        // اگر tag خالی نیست، بررسی کن تکراری نباشه
        if (notification.tag.isNotEmpty()) {
            val existing = dao.countByTag(notification.tag)
            if (existing > 0) return
        }
        dao.insert(notification)
    }

    suspend fun markAsRead(id: String) = dao.markAsRead(id)

    suspend fun markAllAsRead() = dao.markAllAsRead()

    suspend fun deleteById(id: String) = dao.deleteById(id)
}