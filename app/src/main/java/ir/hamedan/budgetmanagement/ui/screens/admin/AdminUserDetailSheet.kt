package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.network.AdminApi
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthErrorBanner

/** Everything the admin can see and do for one user. Opens as a bottom sheet from the user list. */
@Composable
internal fun UserDetailSheet(
    state: DetailUiState,
    fallback: AdminApi.AdminUser?,
    isSelf: Boolean,
    busy: Boolean,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onEdit: (AdminApi.AdminUser) -> Unit,
    onUnlock: (AdminApi.AdminUser) -> Unit,
    onRevoke: (AdminApi.AdminUser) -> Unit,
    onDelete: (AdminApi.AdminUser) -> Unit,
    onRestore: (AdminApi.AdminUser) -> Unit,
    onShowAnalytics: (AdminApi.AdminUser) -> Unit
) {
    val user = state.detail?.user ?: fallback
    AdminBottomSheet(onDismiss = onDismiss) {
        if (user == null) {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                if (state.error != null) AuthErrorBanner(message = state.error) else CircularProgressIndicator()
            }
            return@AdminBottomSheet
        }

        DetailHeader(user, busy, isPersian)
        DetailInfoCard(user, isPersian)

        val devices = state.detail?.devices.orEmpty()
        if (devices.isNotEmpty()) {
            DetailSection(if (isPersian) "دستگاه‌ها" else "Devices") {
                devices.forEach { DeviceRow(it, isPersian) }
            }
        }

        val events = state.detail?.events.orEmpty()
        if (events.isNotEmpty()) {
            DetailSection(if (isPersian) "رویدادهای اخیر" else "Recent activity") {
                events.take(8).forEach { AdminEventRow(it, isPersian, compact = true) }
            }
        }

        state.error?.let { AuthErrorBanner(message = it) }

        DetailSection(if (isPersian) "اقدامات" else "Actions") {
            if (!user.isDeleted) {
                ActionButton(Icons.Default.Edit, if (isPersian) "ویرایش" else "Edit", enabled = !busy) { onEdit(user) }
            }
            ActionButton(Icons.Default.Insights, if (isPersian) "نمایش در آنالیز" else "Show in analytics") { onShowAnalytics(user) }
            if (user.isLocked && !user.isDeleted) {
                ActionButton(Icons.Default.LockOpen, if (isPersian) "باز کردن قفل حساب" else "Unlock account", enabled = !busy) { onUnlock(user) }
            }
            if (!isSelf && !user.isDeleted) {
                ActionButton(Icons.AutoMirrored.Filled.Logout, if (isPersian) "ابطال همه نشست‌ها" else "Revoke all sessions", enabled = !busy) { onRevoke(user) }
            }
            if (user.isDeleted) {
                ActionButton(Icons.Default.Restore, if (isPersian) "بازیابی کاربر" else "Restore user", enabled = !busy) { onRestore(user) }
            } else if (!isSelf) {
                ActionButton(Icons.Default.DeleteOutline, if (isPersian) "حذف کاربر" else "Delete user", enabled = !busy, destructive = true) { onDelete(user) }
            }
            if (isSelf) {
                Text(
                    if (isPersian) "این حساب خودِ شماست؛ برای جلوگیری از قفل شدن پنل، حذف و تغییر نقش آن در دسترس نیست."
                    else "This is your own account; deleting it or changing its role is disabled so you can't lock yourself out.",
                    style = MaterialTheme.typography.labelSmall,
                    color = adminMuted()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DetailHeader(user: AdminApi.AdminUser, busy: Boolean, isPersian: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        UserAvatar(user, 60.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                user.fullName.ifBlank { if (isPersian) "بدون نام" else "No name" },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(roleLabel(user.role, isPersian), roleColor(user.role))
                UserStatusPill(user, isPersian)
            }
        }
        if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun DetailInfoCard(user: AdminApi.AdminUser, isPersian: Boolean) {
    AdminCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfoRow(if (isPersian) "شماره" else "Phone", user.phoneNumber.ifBlank { "—" }, ltr = true)
        InfoRow(if (isPersian) "شناسه" else "ID", user.id, ltr = true)
        InfoRow(if (isPersian) "جنسیت" else "Gender", genderLabel(user.gender, isPersian))
        InfoRow(if (isPersian) "تاریخ تولد" else "Birth date", user.birthDate.ifBlank { "—" })
        InfoRow(if (isPersian) "عضویت" else "Joined", dateTime(user.createdAt, isPersian))
        InfoRow(
            if (isPersian) "آخرین فعالیت" else "Last seen",
            if (user.online) (if (isPersian) "اکنون" else "Now") else relativeTime(user.lastSeenAt, isPersian)
        )
        InfoRow(if (isPersian) "نسخه برنامه" else "App version", user.appVersion.ifBlank { "—" }, ltr = true)
        InfoRow(
            if (isPersian) "داده‌های سینک‌شده" else "Synced data",
            "${fmtNumber(user.syncRecords, isPersian)} · ${fmtBytes(user.syncBytes, isPersian)}"
        )
        InfoRow(if (isPersian) "تلاش ناموفق ورود" else "Failed sign-ins", fmtNumber(user.failedLoginAttempts, isPersian))
        if (user.isDeleted) InfoRow(if (isPersian) "زمان حذف" else "Deleted at", dateTime(user.deletedAt, isPersian))
        if (user.isLocked) InfoRow(if (isPersian) "قفل تا" else "Locked until", dateTime(user.lockedUntil, isPersian))
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminSectionLabel(title)
        content()
    }
}

@Composable
private fun DeviceRow(device: AdminApi.AdminDevice, isPersian: Boolean) {
    AdminCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminShapes.Item,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(platformIcon(device.platform), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.displayName.ifBlank { platformLabel(device.platform, isPersian) },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(device.osVersion, device.appVersion.takeIf { it.isNotBlank() }?.let { "v$it" }, device.lastIp)
                        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
                    color = adminMuted(), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (device.online) {
                StatusPill(if (isPersian) "آنلاین" else "Online", adminSuccess())
            } else {
                StatusPill(relativeTime(device.lastSeenAt, isPersian), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, ltr: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.adminCaption, color = adminMuted())
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.adminCaption.copy(textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = AdminShapes.Button,
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.45f else 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
    }
}