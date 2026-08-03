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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCategories = categoryRepository.getAllCategories().first()
            val defaultCategories = listOf(
                CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true),
                CategoryEntity(title = "TRANSPORT", iconEmoji = "🚗", isExpense = true),
                CategoryEntity(title = "SHOPPING", iconEmoji = "🛍️", isExpense = true),
                CategoryEntity(title = "BILL", iconEmoji = "📄", isExpense = true),
                CategoryEntity(title = "DEBT_CREDIT_PAYABLE", iconEmoji = "💸", isExpense = true, isSystem = true), // بدهی
                CategoryEntity(title = "SALARY", iconEmoji = "💰", isExpense = false),
                CategoryEntity(title = "INVESTMENT", iconEmoji = "📈", isExpense = false),
                CategoryEntity(title = "DEBT_CREDIT_RECEIVABLE", iconEmoji = "📥", isExpense = false, isSystem = true) // طلب
            )

            defaultCategories.forEach { category ->
                if (currentCategories.none { it.title == category.title }) {
                    categoryRepository.insertCategory(category)
                }
            }
        }
    }

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
}