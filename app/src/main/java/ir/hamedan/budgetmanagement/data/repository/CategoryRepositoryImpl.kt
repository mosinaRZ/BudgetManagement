package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetLimitRepository: BudgetLimitRepository
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insert(category)
    }

    override suspend fun updateCategory(category: CategoryEntity, newTitle: String, newEmoji: String) {
        val updatedCategory = category.copy(
            title = newTitle,
            iconEmoji = newEmoji
        )
        if (category.title != newTitle) {
            transactionDao.reassignCategoryForTransactions(
                oldCategoryTitle = category.title,
                newCategoryTitle = newTitle
            )
            // هم‌گام‌سازی محدودیت‌های بودجه با عنوان جدید دسته‌بندی
            budgetLimitRepository.reassignCategoryForLimits(
                oldCategoryTitle = category.title,
                newCategoryTitle = newTitle
            )
        }
        categoryDao.update(updatedCategory)
    }

    override fun getCategoriesByExpenseStatus(isExpense: Boolean): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByExpenseStatus(isExpense)
    }

    override suspend fun getTransactionCount(categoryTitle: String): Int {
        return transactionDao.getTransactionCountForCategory(categoryTitle)
    }

    override suspend fun getBudgetLimitCount(categoryTitle: String): Int {
        return budgetLimitRepository.getLimitCountForCategory(categoryTitle)
    }

    override suspend fun deleteCategoryWithReassignment(category: CategoryEntity): Int {
        val defaultKey = "UNCATEGORIZED"
        val affectedCount = transactionDao.getTransactionCountForCategory(category.title)

        val uncategorized = categoryDao.getCategoryByTitle(defaultKey)
        if (uncategorized == null) {
            categoryDao.insert(
                CategoryEntity(
                    title = defaultKey,
                    iconEmoji = "📦",
                    isExpense = category.isExpense
                )
            )
        }

        if (affectedCount > 0) {
            transactionDao.reassignCategoryForTransactions(
                oldCategoryTitle = category.title,
                newCategoryTitle = defaultKey
            )
        }

        // حذف محدودیت‌های بودجه متصل به این دسته‌بندی، چون بعد از حذف دسته دیگر معنایی ندارند
        budgetLimitRepository.deleteLimitsByCategory(category.title)

        categoryDao.delete(category)
        return affectedCount
    }
}