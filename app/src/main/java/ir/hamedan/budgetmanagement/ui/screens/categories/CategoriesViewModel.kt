package ir.hamedan.budgetmanagement.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(title: String, iconEmoji: String, isExpense: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.insert(
                CategoryEntity(
                    title = title,
                    iconEmoji = iconEmoji,
                    isExpense = isExpense
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity, newTitle: String, newEmoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = category.copy(
                title = newTitle,
                iconEmoji = newEmoji
            )

            // در صورت تغییر عنوان دسته‌بندی، تراکنش‌های قبلی هم بروزرسانی می‌شوند
            if (category.title != newTitle) {
                transactionDao.reassignCategoryForTransactions(
                    oldCategoryTitle = category.title,
                    newCategoryTitle = newTitle
                )
            }

            categoryDao.update(updated)
        }
    }

    suspend fun getTransactionCount(categoryTitle: String): Int {
        return transactionDao.getTransactionCountForCategory(categoryTitle)
    }

    fun deleteCategoryWithReassignment(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultTitle = "دسته‌بندی نشده"

            // ایجاد دسته‌بندی پیش‌فرض در صورت عدم وجود
            var uncategorized = categoryDao.getCategoryByTitle(defaultTitle)
            if (uncategorized == null) {
                categoryDao.insert(
                    CategoryEntity(
                        title = defaultTitle,
                        iconEmoji = "📦",
                        isExpense = category.isExpense
                    )
                )
            }

            // انتقال تراکنش‌ها
            transactionDao.reassignCategoryForTransactions(
                oldCategoryTitle = category.title,
                newCategoryTitle = defaultTitle
            )

            // حذف دسته‌بندی
            categoryDao.delete(category)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
                val db = AppDatabase.getInstance(context)
                return CategoriesViewModel(
                    categoryDao = db.categoryDao(),
                    transactionDao = db.transactionDao()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}