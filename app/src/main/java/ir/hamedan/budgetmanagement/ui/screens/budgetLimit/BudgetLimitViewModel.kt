package ir.hamedan.budgetmanagement.ui.screens.budget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BudgetLimitUiModel(
    val entity: BudgetLimitEntity,
    val currentSpent: Double,
    val categoryEmoji: String = "💰"
) {
    // انقضا تنها پس از عبور کامل از تاریخ و زمان پایان
    val isExpired: Boolean
        get() = System.currentTimeMillis() > entity.endDate

    val isActive: Boolean
        get() = entity.isActive && !isExpired

    val isSuccessful: Boolean
        get() = currentSpent <= entity.maxLimit.toDouble()
}

class BudgetLimitViewModel(
    private val budgetLimitRepository: BudgetLimitRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
    private val context: Context
) : ViewModel() {

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage

    val currencyUnit: StateFlow<String> = CurrencySharedPreferences.currencyFlow

    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryRepository
        .getCategoriesByExpenseStatus(isExpense = true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgetLimitsWithSpent: StateFlow<List<BudgetLimitUiModel>> = combine(
        budgetLimitRepository.getAllLimits(),
        transactionRepository.getAllTransactions(),
        expenseCategories
    ) { limits, transactions, categories ->
        limits.map { limit ->
            val category = categories.find { it.id == limit.categoryId }
            val spent = if (limit.isActive) {
                transactions
                    .filter {
                        it.type == "EXPENSE" &&
                                it.categoryId == limit.categoryId &&
                                it.timestamp in limit.startDate..limit.endDate
                    }
                    .sumOf { it.amount }
                    .toDouble()
            } else 0.0

            val percentUsed = if (limit.maxLimit > 0L) (spent / limit.maxLimit.toDouble() * 100.0) else 0.0
            listOf(50.0, 80.0, 100.0).forEach { threshold ->
                if (percentUsed >= threshold) {
                    val tag = "BUDGET_${threshold.toInt()}_${limit.id}"
                    viewModelScope.launch {
                        NotificationHelper.send(
                            context = context,
                            notificationType = NotificationType.BUDGET_THRESHOLD,
                            type = if (threshold >= 100.0) "ERROR" else "WARNING",
                            titleFa = "هشدار محدودیت بودجه",
                            titleEn = "Budget Limit Alert",
                            descFa = "دسته «${category?.title.orEmpty()}» به ${threshold.toInt()}٪ سقف رسید.",
                            descEn = "${category?.title.orEmpty()} reached ${threshold.toInt()}% of budget.",
                            tag = tag
                        )
                    }
                }
            }

            BudgetLimitUiModel(
                entity = limit,
                currentSpent = spent,
                categoryEmoji = category?.iconEmoji ?: "💰"
            )
        }.map { it }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveBudgetLimit(
        categoryName: String,
        maxLimit: Double,
        startDate: Long,
        endDate: Long,
        limitId: String? = null
    ) {
        viewModelScope.launch {
            // برای ویرایش، رکورد قبلی باید با شناسه (id) پیدا شود، نه با نام دسته‌بندی؛
            // چون در حالت ویرایش ممکن است دسته‌بندی تغییر کرده باشد و جستجو بر اساس نام
            // باعث می‌شد رکورد قدیمی پیدا نشود و یک رکورد جدید و تکراری ساخته شود.
            val existingLimit = limitId?.let { id ->
                budgetLimitsWithSpent.value.find { it.entity.id == id }?.entity
            }

            // تنظیم تاریخ پایان تا آخرین میلی‌ثانیه همان روز (23:59:59.999)
            val adjustedEndDate = endDate + (24 * 60 * 60 * 1000L - 1)

            val category = expenseCategories.value.firstOrNull { it.title == categoryName }
            if (category == null) {
                _errorMessage.emit(if (LocaleHelper.getLanguage(context) == "fa") "دسته‌بندی انتخاب‌شده معتبر نیست." else "The selected category is not valid.")
                return@launch
            }
            val duplicate = budgetLimitsWithSpent.value.any { existing ->
                existing.entity.id != limitId && existing.entity.categoryId == category.id &&
                        existing.entity.startDate <= adjustedEndDate && existing.entity.endDate >= startDate
            }
            if (duplicate) {
                val isPersian = LocaleHelper.getLanguage(context) == "fa"
                _errorMessage.emit(if (isPersian) "برای این دسته‌بندی یک محدودیت با بازه هم‌پوشان از قبل وجود دارد." else "An overlapping budget limit already exists for this category.")
                return@launch
            }
            val limit = BudgetLimitEntity(
                id = existingLimit?.id ?: java.util.UUID.randomUUID().toString(),
                categoryId = category.id,
                maxLimit = MoneyContract.fromInput(maxLimit),
                isActive = existingLimit?.isActive ?: true,
                startDate = startDate,
                endDate = adjustedEndDate,
                createdAt = existingLimit?.createdAt ?: System.currentTimeMillis()
            )
            budgetLimitRepository.saveLimit(limit)

            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.BUDGET_ADD,
                type = "SUCCESS",
                titleFa = "محدودیت مالی جدید ثبت شد",
                titleEn = "New Budget Limit Added",
                descFa = "محدودیت مالی جدید برای دسته بندی «${categoryName}» ثبت شد.",
                descEn = "New budget limit added for category ${categoryName}.",
                tag = "BUDGET_${categoryName}_${System.currentTimeMillis()}"
            )
        }
    }

    fun renewLimit(id: String, durationDays: Int = 30) {
        viewModelScope.launch {
            val current = budgetLimitsWithSpent.value.find { it.entity.id == id }?.entity ?: return@launch
            val start = System.currentTimeMillis()
            val end = start + durationDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L - 1L
            val renewed = current.copy(isActive = true, startDate = start, endDate = end, updatedAt = start)
            budgetLimitRepository.saveLimit(renewed)
            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.BUDGET_ADD,
                type = "SUCCESS",
                titleFa = "محدودیت مالی تمدید شد",
                titleEn = "Budget Limit Renewed",
                descFa = "محدودیت دسته‌بندی «${expenseCategories.value.firstOrNull { it.id == current.categoryId }?.title.orEmpty()}» برای ۳۰ روز تمدید شد.",
                descEn = "The budget limit was renewed for 30 days.",
                tag = "BUDGET_RENEW_${id}_${System.currentTimeMillis()}"
            )
        }
    }

    fun updateLimitStatus(id: String, isActive: Boolean) {
        viewModelScope.launch {
            val currentItem = budgetLimitsWithSpent.value.find { it.entity.id == id }?.entity
            currentItem?.let {
                val updatedLimit = it.copy(isActive = isActive)
                budgetLimitRepository.saveLimit(updatedLimit)

                val statusFa = if (isActive) "فعال" else "غیرفعال"
                val statusEn = if (isActive) "enabled" else "disabled"

                NotificationHelper.send(
                    context = context,
                    notificationType = NotificationType.BUDGET_STATUS_CHANGE,
                    type = "WARNING",
                    titleFa = "محدودیت مالی به روزرسانی شد",
                    titleEn = "Budget Limit Status Updated",
                    descFa = "محدودیت مالی دسته‌بندی «${expenseCategories.value.firstOrNull { c -> c.id == it.categoryId }?.title.orEmpty()}» $statusFa شد.",
                    descEn = "Budget limit for category ${expenseCategories.value.firstOrNull { c -> c.id == it.categoryId }?.title.orEmpty()} was $statusEn.",
                    tag = "BUDGET_STATUS_${expenseCategories.value.firstOrNull { c -> c.id == it.categoryId }?.title.orEmpty()}_${System.currentTimeMillis()}"
                )
            }
        }
    }

    // حذف اولیه از پایگاه داده (بدون ارسال نوتیفیکیشن)
    fun deleteBudgetLimit(id: String) {
        viewModelScope.launch {
            budgetLimitRepository.deleteLimit(id)
        }
    }

    // بازگردانی آیتم حذف شده در صورت زدن Undo
    fun restoreLimit(entity: BudgetLimitEntity) {
        viewModelScope.launch {
            budgetLimitRepository.saveLimit(entity)
        }
    }

    // ارسال نوتیفیکیشن حذف قطعی (تنها در صورتی که کاربر Undo نکرده باشد)
    fun commitDeleteLimit(entity: BudgetLimitEntity) {
        viewModelScope.launch {
            val mappedCategory = expenseCategories.value.firstOrNull { c -> c.id == entity.categoryId }?.title.orEmpty()
            NotificationHelper.send(
                context = context,
                notificationType = NotificationType.BUDGET_DELETE,
                type = "ERROR",
                titleFa = "محدودیت مالی حذف شد",
                titleEn = "Budget Limit Deleted",
                descFa = "محدودیت مالی دسته‌بندی «$mappedCategory» حذف شد.",
                descEn = "Budget limit for category $mappedCategory was deleted.",
                tag = "BUDGET_DELETE_${entity.id}_${System.currentTimeMillis()}"
            )
        }
    }}