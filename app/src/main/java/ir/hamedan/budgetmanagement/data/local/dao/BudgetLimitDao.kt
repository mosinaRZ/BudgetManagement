package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {

    @Query(
        """
        SELECT *
        FROM budget_limits
        ORDER BY startDate DESC
        """
    )
    fun getAllLimits(): Flow<List<BudgetLimitEntity>>

    @Query(
        """
        SELECT *
        FROM budget_limits
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): BudgetLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(limit: BudgetLimitEntity)

    @Delete
    suspend fun delete(limit: BudgetLimitEntity): Int

    @Query(
        """
        DELETE FROM budget_limits
        WHERE id = :id
        """
    )
    suspend fun deleteById(id: String): Int

    @Query("""
        SELECT *
        FROM budget_limits
        WHERE categoryId = :categoryId
    """)
    suspend fun getByCategoryId(categoryId: String): List<BudgetLimitEntity>

    /**
     * Number of budget limits attached to a category.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM budget_limits
        WHERE categoryId = :categoryId
        """
    )
    suspend fun getLimitCountForCategory(categoryId: String): Int

    /**
     * Reassign budgets from one category to another.
     *
     * Normally this should only be necessary for an explicit
     * category reassignment operation.
     */
    @Query(
        """
        UPDATE budget_limits
        SET categoryId = :newCategoryId,
            updatedAt = :updatedAt
        WHERE categoryId = :oldCategoryId
        """
    )
    suspend fun reassignCategoryForLimits(
        oldCategoryId: String,
        newCategoryId: String,
        updatedAt: Long
    ): Int

    /**
     * Delete all budgets associated with a category.
     *
     * The repository/sync layer will later be responsible
     * for creating tombstones for these deleted entities.
     */
    @Query(
        """
        DELETE FROM budget_limits
        WHERE categoryId = :categoryId
        """
    )
    suspend fun deleteByCategoryId(categoryId: String): Int

    /**
     * Enable/disable a budget limit.
     */
    @Query(
        """
        UPDATE budget_limits
        SET isActive = :isActive,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateActiveState(
        id: String,
        isActive: Boolean,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM budget_limits")
    suspend fun clearAll(): Int
}