package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "debt_credits",
    indices = [
        Index(value = ["type"]),
        Index(value = ["isSettled"]),
        Index(value = ["dueDateMillis"]),
        Index(value = ["updatedAt"])
    ]
)
data class DebtCreditEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * DEBT / CREDIT
     */
    val type: String,

    val personName: String,

    /**
     * Total debt/credit amount.
     */
    val totalAmount: Long,

    /**
     * Amount already paid.
     */
    val paidAmount: Long = 0L,

    val isMonthly: Boolean = false,

    /**
     * Monthly payment amount.
     */
    val monthlyAmount: Long = 0L,

    val dueDay: Int = 1,

    /**
     * Due date/time in epoch milliseconds.
     *
     * 0 means no specific due date.
     */
    val dueDateMillis: Long = 0L,

    val note: String? = null,

    val isSettled: Boolean = false,

    /**
     * Record creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last modification time.
     */
    val updatedAt: Long = System.currentTimeMillis()
)