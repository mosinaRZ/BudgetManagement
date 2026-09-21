package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["title"]),
        Index(value = ["isExpense"]),
        Index(value = ["isSystem"])
    ]
)
data class CategoryEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val title: String = "",

    val iconName: String = "",

    val iconEmoji: String = "📁",

    /**
     * true  -> expense category
     * false -> income category
     */
    val isExpense: Boolean = true,

    /**
     * System categories cannot be deleted by the user.
     */
    val isSystem: Boolean = false,

    /**
     * Local creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last modification time.
     *
     * This is different from the business/event timestamp.
     */
    val updatedAt: Long = System.currentTimeMillis()
)