package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "upcoming_payments")
data class UpcomingPaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double = 0.0,
    val dueDate: Long = 0L,     // تاریخ دقیق سررسید بعدی (بر حسب میلی‌ثانیه)
    val dueDay: Int = 1,        // روز سررسید در ماه (مثلاً ۲۶)
    val isPaid: Boolean = false
)