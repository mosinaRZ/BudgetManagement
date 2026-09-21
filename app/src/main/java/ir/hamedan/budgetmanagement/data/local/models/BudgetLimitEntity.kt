package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "budget_limits",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isActive"]),
        Index(value = ["updatedAt"])
    ]
)
data class BudgetLimitEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * Stable reference to CategoryEntity.id.
     *
     * Never store category title/name here.
     */
    val categoryId: String,

    /**
     * Maximum allowed amount.
     *
     * Stored as Long instead of Double.
     */
    val maxLimit: Long,

    val isActive: Boolean = true,

    /**
     * Beginning of budget period.
     */
    val startDate: Long,

    /**
     * End of budget period.
     */
    val endDate: Long,

    /**
     * Record creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last modification time.
     */
    val updatedAt: Long = System.currentTimeMillis()
)