package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.*
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits")
    fun getAllLimits(): Flow<List<BudgetLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(limit: BudgetLimitEntity)

    @Delete
    suspend fun delete(limit: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE id = :id")
    suspend fun deleteById(id: Long)

    // شمارش محدودیت‌های بودجه متصل به یک عنوان دسته‌بندی
    @Query("SELECT COUNT(*) FROM budget_limits WHERE categoryName = :categoryTitle")
    suspend fun getLimitCountForCategory(categoryTitle: String): Int

    // جابه‌جایی محدودیت‌های بودجه به عنوان دسته‌بندی جدید (هنگام ویرایش عنوان دسته‌بندی)
    @Query("UPDATE budget_limits SET categoryName = :newCategoryTitle WHERE categoryName = :oldCategoryTitle")
    suspend fun reassignCategoryForLimits(oldCategoryTitle: String, newCategoryTitle: String)

    // حذف تمام محدودیت‌های بودجه متصل به یک عنوان دسته‌بندی (هنگام حذف دسته‌بندی)
    @Query("DELETE FROM budget_limits WHERE categoryName = :categoryTitle")
    suspend fun deleteByCategoryName(categoryTitle: String)
}