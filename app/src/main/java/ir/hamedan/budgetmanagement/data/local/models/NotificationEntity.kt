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
        Index(value = ["tag"])
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
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val tag: String = ""
)