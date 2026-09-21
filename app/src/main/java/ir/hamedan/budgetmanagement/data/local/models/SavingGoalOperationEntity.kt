package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Immutable operation ledger for saving-goal balance changes.
 *
 * The goal's currentAmount is a derived aggregate. Deposit/withdraw operations
 * are synchronized instead of syncing the aggregate on every mutation, which
 * prevents concurrent devices from overwriting each other's changes.
 */
@Entity(
    tableName = "saving_goal_operations",
    indices = [Index(value = ["goalId"]), Index(value = ["createdAt"])]
)
data class SavingGoalOperationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    /** Signed delta: positive = deposit, negative = withdrawal. */
    val deltaAmount: Long,
    /** BASELINE is used to bootstrap legacy aggregate values exactly once. */
    val operationType: String = "DELTA",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)