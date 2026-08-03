package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val monthlyAmount: Double = 0.0, // مبلغ ماهانه که از بالانس کسر و به قلک اضافه می‌شود
    val icon: String = "🎯"
)