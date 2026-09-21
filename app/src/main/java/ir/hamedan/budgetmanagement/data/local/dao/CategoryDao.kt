package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("""
        SELECT *
        FROM categories
        ORDER BY title ASC
    """)
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("""
        SELECT *
        FROM categories
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getById(id: String): CategoryEntity?

    @Query("""
        SELECT *
        FROM categories
        WHERE title = :title
        LIMIT 1
    """)
    suspend fun getCategoryByTitle(
        title: String
    ): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(
        category: CategoryEntity
    )

    @Update
    suspend fun update(
        category: CategoryEntity
    ): Int

    @Delete
    suspend fun delete(
        category: CategoryEntity
    )

    @Query("""
        SELECT *
        FROM categories
        WHERE isExpense = :isExpense
        ORDER BY title ASC
    """)
    fun getCategoriesByExpenseStatus(
        isExpense: Boolean
    ): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories")
    suspend fun clearAll(): Int
}