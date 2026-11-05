package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import ir.hamedan.budgetmanagement.data.network.AdminApi

/** Display names and icons for the values the admin API sends. Colours live in AdminTheme.kt. */

internal val AllRoles = listOf("user", "pro", "premium", "admin")

internal val AllEventTypes = listOf(
    "login_success", "login_failed", "account_locked", "registered", "password_changed", "password_reset",
    "account_deleted", "account_restored", "device_removed", "admin_user_edited", "admin_role_changed",
    "admin_user_deleted", "admin_user_restored", "admin_user_unlocked", "admin_sessions_revoked", "admin_access_denied"
)

/** Name shown for a user everywhere in the console (falls back to the phone number). */
internal fun AdminApi.AdminUser.displayLabel(): String = fullName.ifBlank { phoneNumber }

internal fun roleLabel(role: String, isPersian: Boolean): String = when (role) {
    "admin" -> if (isPersian) "ادمین" else "Admin"
    "premium" -> if (isPersian) "پریمیوم" else "Premium"
    "pro" -> if (isPersian) "حرفه‌ای" else "Pro"
    else -> if (isPersian) "کاربر" else "User"
}

internal fun genderLabel(gender: String, isPersian: Boolean): String = when (gender.lowercase()) {
    "male" -> if (isPersian) "مرد" else "Male"
    "female" -> if (isPersian) "زن" else "Female"
    "" -> "—"
    else -> gender
}

internal fun platformLabel(platform: String, isPersian: Boolean): String = when (platform.lowercase()) {
    "android" -> "Android"
    "ios" -> "iOS"
    "web" -> "Web"
    "desktop" -> "Desktop"
    "", "unknown" -> if (isPersian) "نامشخص" else "Unknown"
    else -> platform
}

internal fun platformIcon(platform: String): ImageVector = when (platform.lowercase()) {
    "ios" -> Icons.Default.PhoneIphone
    "android" -> Icons.Default.PhoneAndroid
    else -> Icons.Default.Devices
}

internal fun entityLabel(key: String, isPersian: Boolean): String = when (key) {
    "transaction" -> if (isPersian) "تراکنش" else "Transactions"
    "category" -> if (isPersian) "دسته‌بندی" else "Categories"
    "budget_limit" -> if (isPersian) "سقف بودجه" else "Budget limits"
    "saving_goal" -> if (isPersian) "هدف پس‌انداز" else "Saving goals"
    "saving_goal_operation" -> if (isPersian) "عملیات هدف" else "Goal operations"
    "debt_credit" -> if (isPersian) "بدهی و طلب" else "Debts & credits"
    "debt_payment" -> if (isPersian) "پرداخت بدهی" else "Debt payments"
    "" -> if (isPersian) "نامشخص" else "Unknown"
    else -> key
}

internal fun eventLabel(type: String, isPersian: Boolean): String = when (type) {
    "login_success" -> if (isPersian) "ورود موفق" else "Successful sign-in"
    "login_failed" -> if (isPersian) "ورود ناموفق" else "Failed sign-in"
    "account_locked" -> if (isPersian) "قفل شدن حساب" else "Account locked"
    "registered" -> if (isPersian) "ثبت‌نام" else "Registration"
    "password_changed" -> if (isPersian) "تغییر رمز عبور" else "Password changed"
    "password_reset" -> if (isPersian) "بازنشانی رمز عبور" else "Password reset"
    "account_deleted" -> if (isPersian) "حذف حساب توسط کاربر" else "Account deleted by user"
    "account_restored" -> if (isPersian) "بازیابی حساب" else "Account restored"
    "device_removed" -> if (isPersian) "حذف دستگاه" else "Device removed"
    "admin_user_edited" -> if (isPersian) "ویرایش کاربر توسط ادمین" else "User edited by admin"
    "admin_role_changed" -> if (isPersian) "تغییر نقش کاربر" else "Role changed"
    "admin_user_deleted" -> if (isPersian) "حذف کاربر توسط ادمین" else "User deleted by admin"
    "admin_user_restored" -> if (isPersian) "بازیابی کاربر توسط ادمین" else "User restored by admin"
    "admin_user_unlocked" -> if (isPersian) "رفع قفل حساب" else "Account unlocked"
    "admin_sessions_revoked" -> if (isPersian) "ابطال نشست‌ها" else "Sessions revoked"
    "admin_access_denied" -> if (isPersian) "دسترسی غیرمجاز به پنل ادمین" else "Unauthorised admin access"
    else -> type
}

internal fun eventIcon(type: String): ImageVector = when (type) {
    "login_success" -> Icons.AutoMirrored.Filled.Login
    "login_failed" -> Icons.Default.Block
    "account_locked" -> Icons.Default.Lock
    "registered" -> Icons.Default.PersonAdd
    "password_changed", "password_reset" -> Icons.Default.Key
    "account_deleted", "admin_user_deleted" -> Icons.Default.PersonOff
    "account_restored", "admin_user_restored" -> Icons.Default.Restore
    "device_removed" -> Icons.Default.Devices
    "admin_user_edited" -> Icons.Default.EditNote
    "admin_role_changed" -> Icons.Default.AdminPanelSettings
    "admin_user_unlocked" -> Icons.Default.LockOpen
    "admin_sessions_revoked" -> Icons.Default.DeleteSweep
    "admin_access_denied" -> Icons.Default.Shield
    else -> Icons.Default.Warning
}

internal fun detailLabel(detail: String, isPersian: Boolean): String = when (detail) {
    "" -> ""
    "wrong_password" -> if (isPersian) "رمز عبور اشتباه" else "Wrong password"
    "unknown_identifier" -> if (isPersian) "شماره ناشناس" else "Unknown identifier"
    "account_locked" -> if (isPersian) "حساب قفل بود" else "Account was locked"
    "too_many_failed_logins" -> if (isPersian) "تلاش‌های ناموفق زیاد" else "Too many failed attempts"
    "self_delete", "self_restore" -> if (isPersian) "توسط خود کاربر" else "By the user"
    "soft_delete" -> if (isPersian) "حذف نرم (قابل بازیابی)" else "Soft delete (restorable)"
    "restore" -> if (isPersian) "بازیابی" else "Restore"
    "recovery_key" -> if (isPersian) "با کلید بازیابی" else "With recovery key"
    "first_name" -> if (isPersian) "نام" else "First name"
    "last_name" -> if (isPersian) "نام خانوادگی" else "Last name"
    "first_name,last_name" -> if (isPersian) "نام و نام خانوادگی" else "First & last name"
    "user", "pro", "premium", "admin" -> ""
    else -> if (detail.contains(">")) {
        val (from, to) = detail.split(">", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        "${roleLabel(from, isPersian)} → ${roleLabel(to, isPersian)}"
    } else detail
}

internal fun severityLabel(severity: String, isPersian: Boolean): String = when (severity) {
    "danger" -> if (isPersian) "خطرناک" else "Dangerous"
    "warning" -> if (isPersian) "هشدار" else "Warning"
    "info" -> if (isPersian) "عادی" else "Normal"
    else -> if (isPersian) "همه" else "All"
}

internal fun riskLabel(level: String, isPersian: Boolean): String = when (level) {
    "high" -> if (isPersian) "ریسک بالا" else "High risk"
    "medium" -> if (isPersian) "ریسک متوسط" else "Medium risk"
    else -> if (isPersian) "ریسک پایین" else "Low risk"
}