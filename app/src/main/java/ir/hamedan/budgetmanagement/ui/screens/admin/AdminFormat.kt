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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun numberFormat(isPersian: Boolean, fractionDigits: Int = 0): NumberFormat =
    NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US).apply {
        maximumFractionDigits = fractionDigits
        minimumFractionDigits = 0
    }

internal fun fmtNumber(value: Long, isPersian: Boolean): String = numberFormat(isPersian).format(value)

/** 12 345 -> "12.3K" style short form for chart axes and KPI cards. */
internal fun fmtCompact(value: Long, isPersian: Boolean): String {
    val nf = numberFormat(isPersian, 1)
    return when {
        value >= 1_000_000 -> nf.format(value / 1_000_000.0) + "M"
        value >= 10_000 -> nf.format(value / 1_000.0) + "K"
        else -> fmtNumber(value, isPersian)
    }
}

internal fun fmtBytes(bytes: Long, isPersian: Boolean): String {
    val nf = numberFormat(isPersian, 1)
    return when {
        bytes < 1024 -> nf.format(bytes) + " B"
        bytes < 1024L * 1024 -> nf.format(bytes / 1024.0) + " KB"
        bytes < 1024L * 1024 * 1024 -> nf.format(bytes / (1024.0 * 1024)) + " MB"
        else -> nf.format(bytes / (1024.0 * 1024 * 1024)) + " GB"
    }
}

internal fun relativeTime(millis: Long, isPersian: Boolean, now: Long = System.currentTimeMillis()): String {
    if (millis <= 0) return if (isPersian) "نامشخص" else "Unknown"
    val minutes = ((now - millis).coerceAtLeast(0)) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> if (isPersian) "چند لحظه پیش" else "Just now"
        minutes < 60 -> if (isPersian) "${fmtNumber(minutes, true)} دقیقه پیش" else "$minutes min ago"
        hours < 24 -> if (isPersian) "${fmtNumber(hours, true)} ساعت پیش" else "$hours h ago"
        days < 30 -> if (isPersian) "${fmtNumber(days, true)} روز پیش" else "$days d ago"
        else -> DateUtils.formatTimestamp(millis, isPersian)
    }
}

internal fun dateTime(millis: Long, isPersian: Boolean): String {
    if (millis <= 0) return if (isPersian) "نامشخص" else "Unknown"
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    return "${DateUtils.formatTimestamp(millis, isPersian)} - $time"
}

/** "2026-11-03" -> "11/03" (Gregorian) or "08/12" (Jalali), used on chart axes. */
internal fun dayLabel(day: String, isPersian: Boolean): String = runCatching {
    val date = LocalDate.parse(day)
    if (isPersian) {
        val (_, m, d) = DateUtils.toJalali(date)
        val nf = numberFormat(true).apply { minimumIntegerDigits = 2 }
        "${nf.format(m)}/${nf.format(d)}"
    } else {
        "%02d/%02d".format(date.monthValue, date.dayOfMonth)
    }
}.getOrDefault(day)

internal fun roleLabel(role: String, isPersian: Boolean): String = when (role) {
    "admin" -> if (isPersian) "ادمین" else "Admin"
    "premium" -> if (isPersian) "پریمیوم" else "Premium"
    "pro" -> if (isPersian) "حرفه‌ای" else "Pro"
    else -> if (isPersian) "کاربر" else "User"
}

internal fun platformLabel(platform: String, isPersian: Boolean): String = when (platform.lowercase()) {
    "android" -> "Android"
    "ios" -> "iOS"
    "web" -> "Web"
    "desktop" -> "Desktop"
    "" , "unknown" -> if (isPersian) "نامشخص" else "Unknown"
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
    "self_delete" -> if (isPersian) "توسط خود کاربر" else "By the user"
    "self_restore" -> if (isPersian) "توسط خود کاربر" else "By the user"
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

@Composable
internal fun severityColor(severity: String): Color = when (severity) {
    "danger" -> MaterialTheme.colorScheme.error
    "warning" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

internal fun severityLabel(severity: String, isPersian: Boolean): String = when (severity) {
    "danger" -> if (isPersian) "خطرناک" else "Dangerous"
    "warning" -> if (isPersian) "هشدار" else "Warning"
    "info" -> if (isPersian) "عادی" else "Normal"
    else -> if (isPersian) "همه" else "All"
}

@Composable
internal fun roleColor(role: String): Color = when (role) {
    "admin" -> MaterialTheme.colorScheme.error
    "premium" -> MaterialTheme.colorScheme.tertiary
    "pro" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

/** Distinct, theme-friendly colours for categorical charts (donut, bars). */
@Composable
internal fun chartPalette(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    Color(0xFF4F9DB8),
    Color(0xFF9B7EDB),
    Color(0xFFE0B84C),
    MaterialTheme.colorScheme.secondary,
    Color(0xFFD1708F),
    Color(0xFF7FB069)
)
