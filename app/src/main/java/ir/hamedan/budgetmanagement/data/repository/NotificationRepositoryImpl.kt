package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.NotificationDao
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<NotificationEntity>> = dao.getAllNotifications()

    override fun getUnreadCount(): Flow<Int> = dao.getUnreadCount()

    override suspend fun addNotification(notification: NotificationEntity) {
        if (notification.tag.isNotEmpty()) {
            val existing = dao.countByTag(notification.tag)
            if (existing > 0) return
        }
        dao.insert(notification)
    }

    override suspend fun markAsRead(id: String) = dao.markAsRead(id)

    override suspend fun markAllAsRead() = dao.markAllAsRead()

    override suspend fun deleteById(id: String) = dao.deleteById(id)
}
