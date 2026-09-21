package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.BudgetLimitDao
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import kotlinx.coroutines.flow.Flow

class BudgetLimitRepositoryImpl(
    private val budgetLimitDao: BudgetLimitDao,
    private val syncLocalDataSource: SyncLocalDataSource
) : BudgetLimitRepository {

    override fun getAllLimits(): Flow<List<BudgetLimitEntity>> = budgetLimitDao.getAllLimits()

    override suspend fun saveLimit(limit: BudgetLimitEntity) {
        val now = System.currentTimeMillis()
        val updated = limit.copy(updatedAt = now)
        syncLocalDataSource.mutate(SyncEntityType.BUDGET_LIMIT, updated.id, now) {
            budgetLimitDao.insertOrUpdate(updated)
        }
    }

    override suspend fun deleteLimit(id: String) {
        val now = System.currentTimeMillis()
        syncLocalDataSource.delete(SyncEntityType.BUDGET_LIMIT, id, now) {
            budgetLimitDao.deleteById(id)
        }
    }

    override suspend fun getLimitCountForCategory(categoryId: String): Int =
        budgetLimitDao.getLimitCountForCategory(categoryId)

    override suspend fun deleteLimitsByCategory(categoryId: String) {
        val limits = budgetLimitDao.getByCategoryId(categoryId)
        syncLocalDataSource.transaction {
            limits.forEach { limit ->
                budgetLimitDao.deleteById(limit.id)
                syncLocalDataSource.recordMutationInTransaction(
                    SyncEntityType.BUDGET_LIMIT,
                    limit.id,
                    System.currentTimeMillis(),
                    true
                )
            }
        }
    }
}