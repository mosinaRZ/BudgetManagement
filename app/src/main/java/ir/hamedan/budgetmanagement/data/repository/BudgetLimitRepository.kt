package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

interface BudgetLimitRepository {
    fun getAllLimits(): Flow<List<BudgetLimitEntity>>
    suspend fun saveLimit(limit: BudgetLimitEntity)
    suspend fun deleteLimit(id: Long)
}
