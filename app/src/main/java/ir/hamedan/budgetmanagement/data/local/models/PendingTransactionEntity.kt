package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

object PendingStatus {
    const val PENDING = "PENDING"
    const val CONFIRMED = "CONFIRMED"
    const val IGNORED = "IGNORED"
}

@Entity(
    tableName = "pending_transactions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["timestamp"]),
        Index(value = ["status", "timestamp"])
    ]
)
data class PendingTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val rawMessage: String = "",
    val senderAddress: String = "",
    val amount: Double = 0.0,
    val isAmountDetected: Boolean = false,
    val type: String = "EXPENSE",
    val isTypeDetected: Boolean = false,
    val suggestedTitle: String = "",
    val suggestedCategory: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = PendingStatus.PENDING
)