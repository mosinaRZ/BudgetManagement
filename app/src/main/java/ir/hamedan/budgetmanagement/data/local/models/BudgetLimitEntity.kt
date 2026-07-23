package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String,
    val maxLimit: Double,
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // پیش‌فرض ۳۰ روز بعد
)