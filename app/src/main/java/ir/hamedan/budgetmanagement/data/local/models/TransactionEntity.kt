package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // شناسه یکتا برای هر تراکنش
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE", // EXPENSE (هزینه) یا INCOME (درآمد)
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)