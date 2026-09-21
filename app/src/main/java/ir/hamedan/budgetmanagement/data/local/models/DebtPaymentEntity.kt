package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Immutable payment/adjustment ledger for a debt or receivable. */
@Entity(
    tableName = "debt_payments",
    indices = [Index(value = ["debtCreditId"]), Index(value = ["createdAt"])]
)
data class DebtPaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val debtCreditId: String,
    /** Signed delta from the previous paidAmount. */
    val deltaAmount: Long,
    val isSettled: Boolean,
    /** BASELINE bootstraps legacy paidAmount/isSettled exactly once. */
    val operationType: String = "DELTA",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)