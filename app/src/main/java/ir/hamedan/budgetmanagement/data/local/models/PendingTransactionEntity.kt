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
        Index(value = ["status", "timestamp"]),
        Index(value = ["updatedAt"])
    ]
)
data class PendingTransactionEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * Original SMS/message content.
     */
    val rawMessage: String = "",

    val senderAddress: String = "",

    /**
     * Detected amount.
     */
    val amount: Long = 0L,

    val isAmountDetected: Boolean = false,

    /**
     * EXPENSE / INCOME
     */
    val type: String = "EXPENSE",

    val isTypeDetected: Boolean = false,

    val suggestedTitle: String = "",

    /**
     * This is only a suggestion.
     *
     * It is NOT the stable CategoryEntity relation.
     */
    val suggestedCategory: String = "",

    /**
     * Time of the original SMS/transaction event.
     */
    val timestamp: Long = System.currentTimeMillis(),

    val status: String = PendingStatus.PENDING,

    /**
     * Record creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last modification time.
     */
    val updatedAt: Long = System.currentTimeMillis()
)