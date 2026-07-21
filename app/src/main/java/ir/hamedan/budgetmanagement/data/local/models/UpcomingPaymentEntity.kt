package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "upcoming_payments")
data class UpcomingPaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",     // مثلاً: اجاره خانه / قسط وام
    val amount: Double = 0.0,
    val dueDate: Long = 0L,     // تاریخ سررسید
    val isPaid: Boolean = false // وضعیت پرداخت
)