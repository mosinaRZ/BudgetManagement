package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {

    // دریافت لیست دسته‌بندی‌ها به صورت واکنش‌گرا
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    // افزودن دسته‌بندی جدید
    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insert(category)
    }

    // به‌روزرسانی دسته‌بندی و انتقال تراکنش‌ها در صورت تغییر عنوان
    suspend fun updateCategory(category: CategoryEntity, newTitle: String, newEmoji: String) {
        val updatedCategory = category.copy(
            title = newTitle,
            iconEmoji = newEmoji
        )

        // اگر عنوان دسته‌بندی تغییر کرده باشد، تراکنش‌های قبلی به عنوان جدید منتقل می‌شوند
        if (category.title != newTitle) {
            transactionDao.reassignCategoryForTransactions(
                oldCategoryTitle = category.title,
                newCategoryTitle = newTitle
            )
        }

        categoryDao.update(updatedCategory)
    }

    // دریافت دسته‌بندی‌ها بر اساس هزینه (true) یا درآمد (false)
    fun getCategoriesByExpenseStatus(isExpense: Boolean): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByExpenseStatus(isExpense)
    }

    // دریافت تعداد تراکنش‌های متصل به یک دسته‌بندی
    suspend fun getTransactionCount(categoryTitle: String): Int {
        return transactionDao.getTransactionCountForCategory(categoryTitle)
    }

    // حذف دسته‌بندی و انتقال تراکنش‌ها به کلید انگلیسی "UNCATEGORIZED" در دیتابیس
    suspend fun deleteCategoryWithReassignment(category: CategoryEntity) {
        val defaultKey = "UNCATEGORIZED"

        // ۱. بررسی یا ایجاد دسته‌بندی پیش‌فرض با کلید انگلیسی
        var uncategorized = categoryDao.getCategoryByTitle(defaultKey)
        if (uncategorized == null) {
            categoryDao.insert(
                CategoryEntity(
                    title = defaultKey,
                    iconEmoji = "📦",
                    isExpense = category.isExpense
                )
            )
        }

        // ۲. انتقال تراکنش‌ها به دسته‌بندی جدید با کلید انگلیسی
        transactionDao.reassignCategoryForTransactions(
            oldCategoryTitle = category.title,
            newCategoryTitle = defaultKey
        )

        // ۳. حذف دسته‌بندی اصلی
        categoryDao.delete(category)
    }
}