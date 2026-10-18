package ir.hamedan.budgetmanagement.ui.screens.notification

import android.content.Context
import ir.hamedan.budgetmanagement.utils.viewModelErrorHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.notification.AppNotificationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val context: Context
) : ViewModel() {

    // Notifications are secondary UI: a failure here is logged but never interrupts the user.
    private val errorHandler = viewModelErrorHandler(context, "NotificationVM") { }

    val notifications: StateFlow<List<NotificationEntity>> = repository.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addNotification(notification: NotificationEntity) {
        viewModelScope.launch(errorHandler) {
            val alreadyExists = repository.countByTag(notification.tag) > 0

            if (!alreadyExists) {
                repository.insert(notification)

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

    fun markAsRead(id: String) = viewModelScope.launch(errorHandler) { repository.markAsRead(id) }

    fun markAllAsRead() = viewModelScope.launch(errorHandler) { repository.markAllAsRead() }
}