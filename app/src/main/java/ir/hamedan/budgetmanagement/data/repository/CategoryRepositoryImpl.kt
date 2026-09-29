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
    override suspend fun getTransactionTotal(categoryId: String): Long = transactionDao.getTransactionTotalForCategory(categoryId)
    override suspend fun getBudgetLimitCount(categoryId: String): Int = budgetLimitDao.getLimitCountForCategory(categoryId)

    /**
     * Category deletion is one Room transaction: category, transaction references,
     * budget limits, and all corresponding sync metadata/tombstones commit together.
     */
    override suspend fun deleteCategoryWithReassignment(category: CategoryEntity): Int {
        require(!category.isSystem) { "System categories cannot be deleted." }
        require(category.title != UNCATEGORIZED_TITLE) { "The uncategorized category cannot be deleted." }

        val now = System.currentTimeMillis()

        return syncLocalDataSource.transaction {
            val transactions = transactionDao.getByCategoryId(category.id)
            val budgets = budgetLimitDao.getByCategoryId(category.id)
            val uncategorized = getOrCreateUncategorizedCategory(category.isExpense, now)

            // Reassign first, then delete the category. Both operations are part of
            // the same Room transaction, so a failure cannot leave dangling references.
            transactionDao.reassignCategoryForTransactions(
                oldCategoryId = category.id,
                newCategoryId = uncategorized.id,
                updatedAt = now
            )

            transactions.forEach { transaction ->
                syncLocalDataSource.recordMutationInTransaction(
                    entityType = SyncEntityType.TRANSACTION,
                    entityId = transaction.id,
                    updatedAt = now
                )
            }

            budgets.forEach { budget ->
                budgetLimitDao.delete(budget)
                syncLocalDataSource.recordMutationInTransaction(
                    entityType = SyncEntityType.BUDGET_LIMIT,
                    entityId = budget.id,
                    updatedAt = now,
                    isDeleted = true
                )
            }

            categoryDao.delete(category)
            syncLocalDataSource.recordMutationInTransaction(
                entityType = SyncEntityType.CATEGORY,
                entityId = category.id,
                updatedAt = now,
                isDeleted = true
            )

            transactions.size
        }
    }

    /**
     * Returns the single system fallback category used when a user category is deleted.
     * Creation happens inside the caller's Room transaction, so concurrent deletions
     * cannot observe a partially-created fallback category.
     */
    private suspend fun getOrCreateUncategorizedCategory(
        isExpense: Boolean,
        now: Long
    ): CategoryEntity {
        return categoryDao.getCategoryByTitle(UNCATEGORIZED_TITLE)
            ?: CategoryEntity(
                title = UNCATEGORIZED_TITLE,
                iconEmoji = "📦",
                isExpense = isExpense,
                isSystem = true,
                createdAt = now,
                updatedAt = now
            ).also { category ->
                categoryDao.insert(category)
                syncLocalDataSource.recordMutationInTransaction(
                    entityType = SyncEntityType.CATEGORY,
                    entityId = category.id,
                    updatedAt = now
                )
            }
    }

    private companion object {
        const val UNCATEGORIZED_TITLE = "UNCATEGORIZED"
    }
}