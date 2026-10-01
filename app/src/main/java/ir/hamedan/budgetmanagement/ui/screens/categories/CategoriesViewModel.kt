package ir.hamedan.budgetmanagement.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage = _errorMessage

    private val _transactionCountsMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val transactionCountsMap: StateFlow<Map<String, Int>> = _transactionCountsMap.asStateFlow()

    private val _transactionTotalsMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val transactionTotalsMap: StateFlow<Map<String, Long>> = _transactionTotalsMap.asStateFlow()

    // استیت برای نگهداری شناسه (String) دسته‌بندی‌هایی که موقتاً در UI مخفی شده‌اند[cite: 10, 19]
    private val _hiddenCategories = MutableStateFlow<Set<String>>(emptySet())
    val hiddenCategories: StateFlow<Set<String>> = _hiddenCategories.asStateFlow()

    init {
        observeCategoryTransactionCounts()
    }

    private fun observeCategoryTransactionCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            categories.collectLatest { categoryList ->
                if (categoryList != null) {
                    val countsMap = mutableMapOf<String, Int>()
                    val totalsMap = mutableMapOf<String, Long>()
                    for (category in categoryList) {
                        // Transactions are linked by the stable category ID, not the title.
                        countsMap[category.id] = categoryRepository.getTransactionCount(category.id)
                        totalsMap[category.id] = categoryRepository.getTransactionTotal(category.id)
                    }
                    _transactionCountsMap.value = countsMap
                    _transactionTotalsMap.value = totalsMap
                }
            }
        }
    }

    fun addCategory(title: String, iconEmoji: String, isExpense: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedTitle = title.trim()
            if (categories.value.orEmpty().any { it.title.trim().equals(normalizedTitle, ignoreCase = true) }) {
                _errorMessage.emit(if (LocaleHelper.getLanguage(context) == "fa") "این نام دسته‌بندی قبلاً استفاده شده است. نام دیگری انتخاب کنید." else "This category name is already in use. Choose another name.")
                return@launch
            }
            categoryRepository.insertCategory(
                CategoryEntity(
                    title = normalizedTitle,
                    iconEmoji = iconEmoji,
                    isExpense = isExpense
                )
            )

            val typeTextFa = if (isExpense) "هزینه" else "درآمد"
            val typeTextEn = if (isExpense) "expense" else "income"

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.CATEGORY_ADD,
                type = "SUCCESS",
                titleFa = "دسته بندی جدید اضافه شد",
                titleEn = "New Category Added",
                descFa = "دسته‌بندی «$title» به عنوان $typeTextFa اضافه شد.",
                descEn = "Category \"$title\" was added as $typeTextEn.",
                tag = "CATEGORY_ADD_${title}_${System.currentTimeMillis()}"
            )
        }
    }

    fun updateCategory(category: CategoryEntity, newTitle: String, newEmoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedTitle = newTitle.trim()
            if (categories.value.orEmpty().any { it.id != category.id && it.title.trim().equals(normalizedTitle, ignoreCase = true) }) {
                _errorMessage.emit(if (LocaleHelper.getLanguage(context) == "fa") "این نام دسته‌بندی قبلاً استفاده شده است. نام دیگری انتخاب کنید." else "This category name is already in use. Choose another name.")
                return@launch
            }
            categoryRepository.updateCategory(category, normalizedTitle, newEmoji)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.CATEGORY_UPDATE,
                type = "WARNING",
                titleFa = "دسته بندی ویرایش شد",
                titleEn = "Category Updated",
                descFa = "دسته‌بندی «${category.title}» به «$normalizedTitle» تغییر یافت.",
                descEn = "Category \"${category.title}\" was updated to \"$normalizedTitle\".",
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

    fun softDeleteCategory(category: CategoryEntity) {
        _hiddenCategories.value = _hiddenCategories.value + category.id
    }

    fun restoreCategory(category: CategoryEntity) {
        _hiddenCategories.value = _hiddenCategories.value - category.id
    }

    fun commitDeleteCategory(category: CategoryEntity) {
        _hiddenCategories.value = _hiddenCategories.value - category.id
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.deleteCategoryWithReassignment(category)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.CATEGORY_DELETE,
                type = "ERROR",
                titleFa = "دسته بندی حذف شد",
                titleEn = "Category Deleted",
                descFa = "دسته بندی دسته‌بندی «${category.title}» حذف شد.",
                descEn = "Category ${category.title} was deleted.",
                tag = "Category_DELETE_${category.id}_${System.currentTimeMillis()}"
            )
        }
    }
}