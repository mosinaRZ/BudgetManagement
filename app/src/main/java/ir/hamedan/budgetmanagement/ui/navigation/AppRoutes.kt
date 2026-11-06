package ir.hamedan.budgetmanagement.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes
 */
@Serializable
sealed interface AppRoute {

    @Serializable
    data object Splash : AppRoute

    @Serializable
    data class Login(val localUnlockOnly: Boolean = false) : AppRoute

    @Serializable
    data object Register : AppRoute

    @Serializable
    data object PasswordReset : AppRoute

    @Serializable
    data object MainStructure : AppRoute

    /** The administrator's separate console (user management + analytics). Admin accounts only. */
    @Serializable
    data object AdminRoot : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object Devices : AppRoute

    @Serializable
    data class AddScreen(
        val highlightId: String? = null
    ) : AppRoute

    /** جریان تمام‌صفحه‌ی مرحله‌به‌مرحله‌ی ثبت تراکنش (مبلغ ← دسته‌بندی ← مرور). */
    @Serializable
    data class AddTransaction(
        val isExpense: Boolean = true
    ) : AppRoute

    /** ویرایش تمام‌صفحه‌ی یک تراکنش (جایگزین باتم‌شیت قدیمی). */
    @Serializable
    data class EditTransaction(
        val transactionId: String
    ) : AppRoute

    /**
     * جریان تمام‌صفحه‌ی مرحله‌به‌مرحله‌ی ثبت بدهی/طلب (مبلغ ← طرف حساب ← نحوه‌ی تسویه ← مرور).
     * نام مسیر عمداً شامل کلمه‌ی «Debt» نیست تا بررسی مسیرِ یادآور سررسید در MainActivity
     * (route.contains("Debt")) فقط به صفحه‌ی فهرست [Debt] واکنش نشان دهد.
     */
    @Serializable
    data object AddLoanRecord : AppRoute

    /** ویرایش تمام‌صفحه‌ی یک بدهی/طلب (جایگزین دیالوگ قدیمی). */
    @Serializable
    data class EditLoanRecord(
        val recordId: String
    ) : AppRoute

    @Serializable
    data object Categories : AppRoute

    /**
     * ثبت و ویرایش تمام‌صفحه‌ی دسته‌بندی (نام ← آیکون ← مرور).
     * [categoryId] خالی یعنی دسته‌ی جدید با نوع [isExpense]؛ مقدار داشته باشد یعنی ویرایش همان دسته.
     */
    @Serializable
    data class CategoryEditor(
        val categoryId: String? = null,
        val isExpense: Boolean = true
    ) : AppRoute

    @Serializable
    data object Limits : AppRoute

    /** جریان تمام‌صفحه‌ی مرحله‌به‌مرحله‌ی ثبت سقف بودجه (دسته ← مبلغ ← بازه ← مرور). */
    @Serializable
    data object AddBudgetLimit : AppRoute

    /** ویرایش تمام‌صفحه‌ی یک سقف بودجه (جایگزین دیالوگ قدیمی). */
    @Serializable
    data class EditBudgetLimit(
        val limitId: String
    ) : AppRoute

    @Serializable
    data object Upcoming : AppRoute

    @Serializable
    data object Goals : AppRoute

    /**
     * جریان تمام‌صفحه‌ی مرحله‌ای ساخت و ویرایش قلک (عنوان ← آیکون ← مبلغ ← مرور).
     * [goalId] خالی یعنی قلک جدید؛ در غیر این صورت ویرایش همان قلک.
     */
    @Serializable
    data class GoalEditor(
        val goalId: String? = null
    ) : AppRoute

    @Serializable
    data object Debt : AppRoute

    @Serializable
    data object NotificationCalibration : AppRoute
}

/**
 * Routeهای مربوط به Bottom Navigation داخل MainStructure
 */
@Serializable
sealed interface MainTabRoute {

    @Serializable
    data object Home : MainTabRoute

    @Serializable
    data object Transactions : MainTabRoute

    @Serializable
    data object Analytics : MainTabRoute

    @Serializable
    data object Settings : MainTabRoute
}