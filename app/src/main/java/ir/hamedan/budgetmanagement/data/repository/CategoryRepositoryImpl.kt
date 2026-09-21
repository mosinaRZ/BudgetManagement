package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.BudgetLimitDao
import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetLimitDao: BudgetLimitDao,
    private val syncLocalDataSource: SyncLocalDataSource
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun insertCategory(category: CategoryEntity) {
        val now = System.currentTimeMillis()
        val updated = category.copy(createdAt = if (category.createdAt > 0L) category.createdAt else now, updatedAt = now)
        syncLocalDataSource.mutate(SyncEntityType.CATEGORY, updated.id, now) { categoryDao.insert(updated) }
    }

    override suspend fun updateCategory(category: CategoryEntity, newTitle: String, newEmoji: String) {
        val now = System.currentTimeMillis()
        val updated = category.copy(title = newTitle, iconEmoji = newEmoji, updatedAt = now)
        syncLocalDataSource.mutate(SyncEntityType.CATEGORY, updated.id, now) { categoryDao.update(updated) }
    }

    override fun getCategoriesByExpenseStatus(isExpense: Boolean): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByExpenseStatus(isExpense)

    override suspend fun getTransactionCount(categoryId: String): Int = transactionDao.getTransactionCountForCategory(categoryId)
    override suspend fun getBudgetLimitCount(categoryId: String): Int = budgetLimitDao.getLimitCountForCategory(categoryId)

    /**
     * Category deletion is one Room transaction: category, transaction references,
     * budget limits, and all corresponding sync metadata/tombstones commit together.
     */
    override suspend fun deleteCategoryWithReassignment(category: CategoryEntity): Int {
        val defaultCategoryTitle = "UNCATEGORIZED"
        val now = System.currentTimeMillis()

        return syncLocalDataSource.transaction {
            val transactions = transactionDao.getByCategoryId(category.id)
            val budgets = budgetLimitDao.getByCategoryId(category.id)

            var uncategorized = categoryDao.getCategoryByTitle(defaultCategoryTitle)
            if (uncategorized == null) {
                uncategorized = CategoryEntity(
                    title = defaultCategoryTitle,
                    iconEmoji = "📦",
                    isExpense = category.isExpense,
                    isSystem = true,
                    createdAt = now,
                    updatedAt = now
                )
                categoryDao.insert(uncategorized)
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.CATEGORY, uncategorized.id, now)
            }

            transactionDao.reassignCategoryForTransactions(category.id, uncategorized.id, now)
            transactions.forEach { transaction ->
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.TRANSACTION, transaction.id, now)
            }

            budgets.forEach { budget ->
                budgetLimitDao.delete(budget)
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.BUDGET_LIMIT, budget.id, now, true)
            }

            categoryDao.delete(category)
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.CATEGORY, category.id, now, true)
            transactions.size
        }
    }
}