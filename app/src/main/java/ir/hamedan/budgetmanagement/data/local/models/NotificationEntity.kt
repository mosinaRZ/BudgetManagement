package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["isRead"]),
        Index(value = ["timestamp"]),
        Index(value = ["tag"]),
        Index(value = ["updatedAt"])
    ]
)
data class NotificationEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val type: String = "SYSTEM",

    val titleFa: String = "",

    val titleEn: String = "",

    val descFa: String = "",

    val descEn: String = "",

    /**
     * Notification/event time.
     */
    val timestamp: Long = System.currentTimeMillis(),

    val isRead: Boolean = false,

    val tag: String = "",

    /**
     * Record creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last modification time.
     */
    val updatedAt: Long = System.currentTimeMillis()
)