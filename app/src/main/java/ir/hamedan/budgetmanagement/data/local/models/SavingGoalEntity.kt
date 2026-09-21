package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "saving_goals",
    indices = [
        Index(value = ["updatedAt"])
    ]
)
data class SavingGoalEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val title: String,

    /**
     * Target amount.
     */
    val targetAmount: Long,

    /**
     * Current saved amount.
     *
     * Stored as Long instead of Double.
     */
    val currentAmount: Long = 0L,

    /**
     * Planned monthly contribution.
     */
    val monthlyAmount: Long = 0L,

    val icon: String = "ðŸŽ¯",

    /**
     * Last automatic deposit timestamp.
     *
     * 0 means no automatic deposit has occurred.
     */
    val lastAutoDepositTimestamp: Long = 0L,

    /**
     * Record creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last modification time.
     */
    val updatedAt: Long = System.currentTimeMillis()
)