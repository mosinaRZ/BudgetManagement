package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.BudgetLimitDao
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

class BudgetLimitRepositoryImpl(
    private val budgetLimitDao: BudgetLimitDao
) : BudgetLimitRepository {

    override fun getAllLimits(): Flow<List<BudgetLimitEntity>> = budgetLimitDao.getAllLimits()

    override suspend fun saveLimit(limit: BudgetLimitEntity) {
        budgetLimitDao.insertOrUpdate(limit)
    }

    override suspend fun deleteLimit(id: Long) {
        budgetLimitDao.deleteById(id)
    }

    override suspend fun getLimitCountForCategory(categoryTitle: String): Int {
        return budgetLimitDao.getLimitCountForCategory(categoryTitle)
    }

    override suspend fun reassignCategoryForLimits(oldCategoryTitle: String, newCategoryTitle: String) {
        budgetLimitDao.reassignCategoryForLimits(oldCategoryTitle, newCategoryTitle)
    }

    override suspend fun deleteLimitsByCategory(categoryTitle: String) {
        budgetLimitDao.deleteByCategoryName(categoryTitle)
    }
}