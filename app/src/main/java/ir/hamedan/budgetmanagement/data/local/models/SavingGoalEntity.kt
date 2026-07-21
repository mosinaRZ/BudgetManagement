package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",          // مثلاً: خرید لپ‌تاپ
    val targetAmount: Double = 0.0,  // مبلغ هدف
    val currentAmount: Double = 0.0, // مبلغ ذخیره‌شده تا کنون
    val colorHex: String = "#FF6200EE",
    val targetDate: Long? = null
)