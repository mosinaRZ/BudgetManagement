package ir.hamedan.budgetmanagement.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val context: Context
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

            val typeTextFa = if (isExpense) "هزینه" else "درآمد"
            val typeTextEn = if (isExpense) "expense" else "income"

            NotificationHelper.send(
                context = context,
                type = "SUCCESS",
                titleFa = "دسته‌بندی جدید ثبت شد",
                titleEn = "New Category Added",
                descFa = "دسته‌بندی «$title» به عنوان $typeTextFa اضافه شد.",
                descEn = "Category \"$title\" was added as $typeTextEn.",
                tag = "CATEGORY_ADD_${title}_${System.currentTimeMillis()}"
            )
        }
    }

    fun updateCategory(category: CategoryEntity, newTitle: String, newEmoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.updateCategory(category, newTitle, newEmoji)

            NotificationHelper.send(
                context = context,
                type = "WARNING",
                titleFa = "ویرایش دسته‌بندی",
                titleEn = "Category Updated",
                descFa = "دسته‌بندی «${category.title}» به «$newTitle» تغییر یافت.",
                descEn = "Category \"${category.title}\" was updated to \"$newTitle\".",
                tag = "CATEGORY_UPDATE_${newTitle}_${System.currentTimeMillis()}"
            )
        }
    }

    suspend fun getTransactionCount(categoryTitle: String): Int {
        return categoryRepository.getTransactionCount(categoryTitle)
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
                return CategoriesViewModel(
                    categoryRepository = categoryRepository,
                    context = context.applicationContext
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}