package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.NotificationDao
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

/** Device-local notification store; notifications are intentionally not cloud-synchronized. */
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao
) : NotificationRepository {
    override fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    override fun getUnreadCount(): Flow<Int> = notificationDao.getUnreadCount()

    override suspend fun insert(notification: NotificationEntity) {
        val now = System.currentTimeMillis()

        notificationDao.insert(
            notification.copy(
                createdAt = if (notification.createdAt > 0L) {
                    notification.createdAt
                } else {
                    now
                },
                updatedAt = now
            )
        )
    }

    override suspend fun markAsRead(id: String) {
        notificationDao.markAsRead(id, System.currentTimeMillis())
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead(System.currentTimeMillis())
    }

    override suspend fun deleteById(id: String) {
        notificationDao.deleteById(id)
    }

    override suspend fun countByTag(tag: String): Int =
        notificationDao.countByTag(tag)}