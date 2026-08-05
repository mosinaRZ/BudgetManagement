package ir.hamedan.budgetmanagement.ui.screens.notification

import android.content.Context
import androidx.lifecycle.ViewModel
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
            val wasInserted = repository.addNotification(notification)
            // اعلان سیستمی فقط وقتی ارسال می‌شود که رکورد جدیدی واقعاً ثبت شده باشد
            if (wasInserted) {
                AppNotificationManager.sendPushIfAllowed(
                    context = context,
                    titleFa = notification.titleFa,
                    titleEn = notification.titleEn,
                    bodyFa = notification.descFa,
                    bodyEn = notification.descEn
                )
            }
        }
    }

    fun markAsRead(id: String) = viewModelScope.launch { repository.markAsRead(id) }

    fun markAllAsRead() = viewModelScope.launch { repository.markAllAsRead() }
}