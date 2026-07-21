package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.*
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE title = :title LIMIT 1")
    suspend fun getCategoryByTitle(title: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    // تفکیک دسته‌بندی‌ها بر اساس هزینه‌ای (isExpense = true) یا درآمدی (isExpense = false) بودن
    @Query("SELECT * FROM categories WHERE isExpense = :isExpense ORDER BY title ASC")
    fun getCategoriesByExpenseStatus(isExpense: Boolean): Flow<List<CategoryEntity>>
}