package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.BudgetLimitDao
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

class BudgetLimitRepository(private val budgetLimitDao: BudgetLimitDao) {

    fun getAllLimits(): Flow<List<BudgetLimitEntity>> = budgetLimitDao.getAllLimits()

    suspend fun saveLimit(limit: BudgetLimitEntity) {
        budgetLimitDao.insertOrUpdate(limit)
    }

    suspend fun deleteLimit(id: String) {
        budgetLimitDao.deleteById(id)
    }
}