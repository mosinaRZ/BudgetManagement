package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getAllCategories(): Flow<List<CategoryEntity>>

    suspend fun insertCategory(
        category: CategoryEntity
    )

    suspend fun updateCategory(
        category: CategoryEntity,
        newTitle: String,
        newEmoji: String
    )

    fun getCategoriesByExpenseStatus(
        isExpense: Boolean
    ): Flow<List<CategoryEntity>>

    suspend fun getTransactionCount(
        categoryId: String
    ): Int

    suspend fun getBudgetLimitCount(
        categoryId: String
    ): Int

    suspend fun deleteCategoryWithReassignment(
        category: CategoryEntity
    ): Int
}