package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "debt_credits")
data class DebtCreditEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // DEBT یا CREDIT
    val personName: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0, // فیلد جدید: مجموع واریزی/برداشتی‌های انجام شده
    val isMonthly: Boolean = false,
    val monthlyAmount: Double = 0.0,
    val dueDay: Int = 1,
    val dueDateMillis: Long = 0L,
    val note: String? = null,
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)