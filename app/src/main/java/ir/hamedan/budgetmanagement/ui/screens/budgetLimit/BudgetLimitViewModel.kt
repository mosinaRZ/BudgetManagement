package ir.hamedan.budgetmanagement.ui.screens.budget

import android.content.Context
import ir.hamedan.budgetmanagement.utils.viewModelErrorHandler
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
import ir.hamedan.budgetmanagement.utils.ErrorMessages
import ir.hamedan.budgetmanagement.utils.rethrowIfCancelled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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


private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

private fun millisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

/** آغاز روزی که [millis] در آن قرار دارد (۰۰:۰۰:۰۰.۰۰۰ به وقت محلی). */
internal fun startOfDayMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    millisToLocalDate(millis, zone).atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * پایان روزی که [millis] در آن قرار دارد (۲۳:۵۹:۵۹.۹۹۹).
 * اگر [millis] از قبل پایان همان روز باشد، همان مقدار برمی‌گردد؛ پس ذخیره‌ی دوباره‌ی یک سقف
 * (مثلاً در ویرایش) تاریخ پایان را جابه‌جا نمی‌کند.
 */
internal fun endOfDayMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    millisToLocalDate(millis, zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

/**
 * سقفی از همین دسته که بازه‌اش با [startMillis]..[endMillis] هم‌پوشانی دارد (یا null).
 * [excludeId] برای ویرایش است تا سقف با خودش اشتباه نشود.
 */
internal fun findOverlappingLimit(
    limits: List<BudgetLimitEntity>,
    categoryId: String,
    startMillis: Long,
    endMillis: Long,
    excludeId: String? = null
): BudgetLimitEntity? = limits.firstOrNull { existing ->
    existing.id != excludeId &&
            existing.categoryId == categoryId &&
            existing.startDate <= endMillis &&
            existing.endDate >= startMillis
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

    /** Last line of defence: an unexpected failure becomes a localized message, never a crash. */
    private val errorHandler = viewModelErrorHandler(context, "BudgetLimitVM") { _errorMessage.tryEmit(it) }


    val currencyUnit: StateFlow<String> = CurrencySharedPreferences.currencyFlow

    /**
     * فهرست خام سقف‌ها (بدون محاسبه‌ی مصرف و بدون اعلان). صفحه‌های ثبت/ویرایش برای تشخیص
     * هم‌پوشانی از این استفاده می‌کنند؛ پس نیازی به باز بودن صفحه‌ی فهرست نیست.
     */
    val allLimits: StateFlow<List<BudgetLimitEntity>> = budgetLimitRepository
        .getAllLimits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
                    viewModelScope.launch(errorHandler) {
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

    private val isPersian: Boolean get() = LocaleHelper.getLanguage(context) == "fa"

    /** یک سقف را با شناسه برمی‌گرداند؛ null اگر (مثلاً بین دو صفحه) حذف شده باشد. */
    suspend fun findLimit(id: String): BudgetLimitEntity? =
        budgetLimitRepository.getAllLimits().first().firstOrNull { it.id == id }

    /**
     * مجموع هزینه‌ی یک دسته در بازه‌ی [startMillis]..[endMillis] (به تومانِ ذخیره‌شده).
     * برای نمایش «تاکنون چقدر خرج شده» و پیشنهاد سقف؛ هر خطایی به صفر تبدیل می‌شود چون فقط راهنماست.
     */
    suspend fun spentBetween(categoryId: String, startMillis: Long, endMillis: Long): Long =
        try {
            transactionRepository.getTransactionsBetween(startMillis, endMillis)
                .filter { it.type == "EXPENSE" && it.categoryId == categoryId }
                .sumOf { it.amount }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0L
        }

    /** هزینه‌ی ۳۰ روز اخیرِ یک دسته؛ مبنای «پیشنهاد سقف». */
    suspend fun recentMonthlySpending(categoryId: String): Long {
        val now = System.currentTimeMillis()
        return spentBetween(categoryId, now - 30L * DAY_MILLIS, now)
    }

    /**
     * ذخیره‌ی سقف. [categoryName] هم با شناسه و هم با عنوان دسته پذیرفته می‌شود.
     * [startDate] و [endDate] هر لحظه‌ای از روز شروع/پایان هستند؛ ساعتشان در اینجا نرمال می‌شود
     * (شروع = آغاز روز، پایان = پایان روز). در ویرایش، اگر روزِ شروع/پایان تغییر نکرده باشد
     * مقدار قبلی دست‌نخورده می‌ماند.
     *
     * [onResult] با `true` فقط بعد از ذخیره‌ی واقعی صدا زده می‌شود (برای شیت موفقیت).
     */
    fun saveBudgetLimit(
        categoryName: String,
        maxLimit: Double,
        startDate: Long,
        endDate: Long,
        limitId: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(errorHandler) {
            val persian = isPersian
            val saved = try {
                // از مخزن می‌خوانیم، نه از stateIn؛ این تابع باید وقتی صفحه‌ی فهرست باز نیست هم درست کار کند.
                val limits = budgetLimitRepository.getAllLimits().first()
                val categories = categoryRepository.getCategoriesByExpenseStatus(isExpense = true).first()

                // برای ویرایش، رکورد قبلی باید با شناسه پیدا شود، نه با نام دسته‌بندی؛
                // چون در حالت ویرایش ممکن است دسته‌بندی تغییر کرده باشد.
                val existingLimit = limitId?.let { id -> limits.firstOrNull { it.id == id } }
                if (limitId != null && existingLimit == null) {
                    _errorMessage.emit(if (persian) "این محدودیت دیگر وجود ندارد." else "This budget limit no longer exists.")
                    onResult(false)
                    return@launch
                }

                val category = categories.firstOrNull { it.id == categoryName || it.title == categoryName }
                if (category == null) {
                    _errorMessage.emit(if (persian) "دسته‌بندی انتخاب‌شده معتبر نیست." else "The selected category is not valid.")
                    onResult(false)
                    return@launch
                }

                val resolvedStart = if (existingLimit != null &&
                    millisToLocalDate(existingLimit.startDate) == millisToLocalDate(startDate)
                ) existingLimit.startDate else startOfDayMillis(startDate)
                val resolvedEnd = if (existingLimit != null &&
                    millisToLocalDate(existingLimit.endDate) == millisToLocalDate(endDate)
                ) existingLimit.endDate else endOfDayMillis(endDate)

                if (resolvedEnd < resolvedStart) {
                    _errorMessage.emit(if (persian) "تاریخ پایان نمی‌تواند قبل از تاریخ شروع باشد." else "The end date can't be before the start date.")
                    onResult(false)
                    return@launch
                }

                val overlapping = findOverlappingLimit(limits, category.id, resolvedStart, resolvedEnd, limitId)
                if (overlapping != null) {
                    _errorMessage.emit(if (persian) "برای این دسته‌بندی یک محدودیت با بازه هم‌پوشان از قبل وجود دارد." else "An overlapping budget limit already exists for this category.")
                    onResult(false)
                    return@launch
                }

                val limit = BudgetLimitEntity(
                    id = existingLimit?.id ?: java.util.UUID.randomUUID().toString(),
                    categoryId = category.id,
                    maxLimit = MoneyContract.fromInput(maxLimit),
                    isActive = existingLimit?.isActive ?: true,
                    startDate = resolvedStart,
                    endDate = resolvedEnd,
                    createdAt = existingLimit?.createdAt ?: System.currentTimeMillis()
                )
                budgetLimitRepository.saveLimit(limit)
                category to (existingLimit != null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.emit(ErrorMessages.of(e, persian))
                onResult(false)
                return@launch
            }

            // سقف ذخیره شده است؛ مشکل اعلان نباید یک ذخیره‌ی موفق را ناموفق نشان بدهد.
            onResult(true)
            val (category, isEdit) = saved
            runCatching {
                NotificationHelper.send(
                    context = context,
                    notificationType = NotificationType.BUDGET_ADD,
                    type = "SUCCESS",
                    titleFa = if (isEdit) "محدودیت مالی به‌روزرسانی شد" else "محدودیت مالی جدید ثبت شد",
                    titleEn = if (isEdit) "Budget Limit Updated" else "New Budget Limit Added",
                    descFa = if (isEdit) "محدودیت مالی دسته‌بندی «${category.title}» به‌روزرسانی شد."
                    else "محدودیت مالی جدید برای دسته بندی «${category.title}» ثبت شد.",
                    descEn = if (isEdit) "Budget limit for category ${category.title} was updated."
                    else "New budget limit added for category ${category.title}.",
                    tag = "BUDGET_${category.title}_${System.currentTimeMillis()}"
                )
            }.onFailure { it.rethrowIfCancelled() }
        }
    }

    fun renewLimit(id: String, durationDays: Int = 30) {
        viewModelScope.launch(errorHandler) {
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
        viewModelScope.launch(errorHandler) {
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
        viewModelScope.launch(errorHandler) {
            budgetLimitRepository.deleteLimit(id)
        }
    }

    // بازگردانی آیتم حذف شده در صورت زدن Undo
    fun restoreLimit(entity: BudgetLimitEntity) {
        viewModelScope.launch(errorHandler) {
            budgetLimitRepository.saveLimit(entity)
        }
    }

    // ارسال نوتیفیکیشن حذف قطعی (تنها در صورتی که کاربر Undo نکرده باشد)
    fun commitDeleteLimit(entity: BudgetLimitEntity) {
        viewModelScope.launch(errorHandler) {
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