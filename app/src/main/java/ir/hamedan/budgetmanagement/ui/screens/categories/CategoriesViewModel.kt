package ir.hamedan.budgetmanagement.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // 🔥 تغییر به List<CategoryEntity>? و initialValue = null جهت جلوگیری از فلش زدن کارت CTA
    val categories: StateFlow<List<CategoryEntity>?> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addCategory(title: String, iconEmoji: String, isExpense: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.insertCategory(
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
            categoryRepository.updateCategory(category, newTitle, newEmoji)
        }
    }

    suspend fun getTransactionCount(categoryTitle: String): Int {
        return categoryRepository.getTransactionCount(categoryTitle)
    }

    fun deleteCategoryWithReassignment(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.deleteCategoryWithReassignment(category)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
                val db = AppDatabase.getInstance(context)
                val categoryRepository = CategoryRepository(
                    categoryDao = db.categoryDao(),
                    transactionDao = db.transactionDao()
                )
                return CategoriesViewModel(categoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}