package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val categoryName: String = "",   // دسته‌بندی مربوطه (مثلاً: خوراکی)
    val maxLimit: Double = 0.0,      // سقف مجاز هزینه
    val startDate: Long = 0L,
    val endDate: Long = 0L
)