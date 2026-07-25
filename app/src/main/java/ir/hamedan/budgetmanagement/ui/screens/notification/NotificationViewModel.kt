package ir.hamedan.budgetmanagement.ui.screens.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.utils.AppNotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val context: Context
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = repository.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.addNotification(notification)
            // همزمان push سیستمی هم بفرست (اگه کاربر اجازه داده)
            AppNotificationManager.sendPushIfAllowed(
                context = context,
                titleFa = notification.titleFa,
                titleEn = notification.titleEn,
                bodyFa = notification.descFa,
                bodyEn = notification.descEn
            )
        }
    }

    fun markAsRead(id: String) = viewModelScope.launch { repository.markAsRead(id) }

    fun markAllAsRead() = viewModelScope.launch { repository.markAllAsRead() }

    fun deleteNotification(id: String) = viewModelScope.launch { repository.deleteById(id) }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            return NotificationViewModel(
                NotificationRepository(db.notificationDao()),
                context.applicationContext
            ) as T
        }
    }
}