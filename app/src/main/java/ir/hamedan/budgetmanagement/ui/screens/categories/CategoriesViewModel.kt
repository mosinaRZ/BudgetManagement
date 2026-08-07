package ir.hamedan.budgetmanagement.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val _transactionCountsMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val transactionCountsMap: StateFlow<Map<String, Int>> = _transactionCountsMap.asStateFlow()

    init {
        observeCategoryTransactionCounts()
    }

    private fun observeCategoryTransactionCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            categories.collectLatest { categoryList ->
                if (categoryList != null) {
                    val countsMap = mutableMapOf<String, Int>()
                    for (category in categoryList) {
                        countsMap[category.title] = categoryRepository.getTransactionCount(category.title)
                    }
                    _transactionCountsMap.value = countsMap
                }
            }
        }
    }

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

    // پاک‌سازی موقت دسته‌بندی برای لایه UI
    fun softDeleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // اگر متد حذف موقت یا تغییر وضعیت داری فراخوانی کن
        }
    }

    // بازگردانی دسته‌بندی در صورت فشردن Undo
    fun restoreCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.insertCategory(category)
        }
    }

    // حذف نهایی دسته‌بندی و انتقال تراکنش‌ها پس از پایان ۵ ثانیه شمارش معکوس
    fun commitDeleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.deleteCategoryWithReassignment(category)

            NotificationHelper.send(
                context = context,
                type = "ERROR",
                titleFa = "حذف دسته بندی",
                titleEn = "Category Deleted",
                descFa = "دسته بندی دسته‌بندی «${category.title}» حذف شد.",
                descEn = "Category ${category.title} was deleted.",
                tag = "Category_DELETE_${category.id}_${System.currentTimeMillis()}"
            )
        }
    }
}