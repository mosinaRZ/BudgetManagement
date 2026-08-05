package ir.hamedan.budgetmanagement.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val context: Context
) : ViewModel() {
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

    suspend fun getBudgetLimitCount(categoryTitle: String): Int {
        return categoryRepository.getBudgetLimitCount(categoryTitle)
    }
}