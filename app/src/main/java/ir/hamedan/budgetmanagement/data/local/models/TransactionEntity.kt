package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"]),
        Index(value = ["timestamp", "type"]),
        Index(value = ["updatedAt"])
    ]
)
data class TransactionEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val title: String = "",

    /**
     * مبلغ بر حسب واحد پایه پولی.
     * برای جلوگیری از خطای Double در محاسبات مالی از Long استفاده می‌کنیم.
     */
    val amount: Long = 0L,

    /**
     * شناسه پایدار Category.
     * دیگر نام دسته‌بندی داخل Transaction ذخیره نمی‌شود.
     */
    val categoryId: String = "",

    val type: String = "EXPENSE",

    /**
     * زمان وقوع واقعی تراکنش.
     */
    val timestamp: Long = System.currentTimeMillis(),

    val note: String = "",

    /**
     * زمان ایجاد رکورد.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * آخرین زمان تغییر رکورد.
     * این با timestamp تراکنش متفاوت است.
     */
    val updatedAt: Long = System.currentTimeMillis()
)