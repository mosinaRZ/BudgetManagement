package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String = "SYSTEM",      // SUCCESS / ERROR / REWARD / SYSTEM / WARNING
    val titleFa: String = "",
    val titleEn: String = "",
    val descFa: String = "",
    val descEn: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val tag: String = ""              // برای جلوگیری از اعلان تکراری (مثلاً "BUDGET_LIMIT_50_food")
)