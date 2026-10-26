package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Entry point of guest mode on the sign-in screen. The label names the method the guest set up.
 *
 * A quiet "tonal" secondary action: soft primary-tinted fill, an icon badge at the start and a
 * direction-aware arrow at the end, so it reads as an option next to the filled sign-in button
 * without competing with it.
 */
@Composable
fun GuestEntryButton(
    label: String,
    usesFingerprint: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    val contentAlpha = if (enabled) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .alpha(contentAlpha)
            .clip(shape)
            .background(primary.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, primary.copy(alpha = 0.22f)), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (usesFingerprint) Icons.Default.Fingerprint else Icons.Default.PersonOutline,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = primary.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Asked before an account sign-in on a device that holds a guest workspace. The guest data is
 * either moved into the account (synced like any other local data) or deleted.
 */
@Composable
fun GuestDataChoiceDialog(
    isPersian: Boolean,
    onMoveToAccount: () -> Unit,
    onDeleteGuestData: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (isPersian) "داده‌های حالت مهمان" else "Guest data on this device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isPersian)
                        "روی این دستگاه داده‌هایی از حالت مهمان وجود دارد. می‌خواهید به این حساب منتقل شوند یا حذف شوند؟"
                    else
                        "This device has data from guest mode. Do you want to move it into this account, or delete it?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onMoveToAccount, shape = RoundedCornerShape(12.dp)) {
                Text(if (isPersian) "انتقال به این حساب" else "Move to this account")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onDeleteGuestData) {
                    Text(
                        text = if (isPersian) "حذف داده‌های مهمان" else "Delete guest data",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        }
    )
}

/**
 * Asked when a guest opens the account creation screen. The guest data is not touched here: the
 * choice is applied once the new account has signed in, so a failed or abandoned registration
 * keeps the data. The dialog can only be left through one of its buttons, so tapping outside or
 * pressing back never throws the person out of registration by accident.
 */
@Composable
fun GuestRegisterChoiceDialog(
    isPersian: Boolean,
    onMoveToNewAccount: () -> Unit,
    onDeleteGuestData: () -> Unit,
    onCancel: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(primary.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, primary.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isPersian) "با اطلاعات مهمان چه کنیم؟" else "What about your guest data?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isPersian)
                        "روی این دستگاه اطلاعاتی در حالت مهمان ثبت شده است. انتخاب شما بعد از ساخت حساب و اولین ورود اعمال می‌شود."
                    else
                        "This device holds data recorded in guest mode. Your choice is applied after the account is created and you sign in for the first time.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                GuestChoiceOption(
                    icon = Icons.Default.CloudUpload,
                    accent = primary,
                    title = if (isPersian) "انتقال به حساب جدید" else "Move to the new account",
                    description = if (isPersian)
                        "تراکنش‌ها و تنظیمات شما حفظ می‌شود و با حساب جدید همگام‌سازی می‌شود."
                    else
                        "Your transactions and settings are kept and synced with the new account.",
                    onClick = onMoveToNewAccount
                )
                GuestChoiceOption(
                    icon = Icons.Default.DeleteOutline,
                    accent = error,
                    title = if (isPersian) "شروع از صفر (حذف اطلاعات مهمان)" else "Start fresh (delete guest data)",
                    description = if (isPersian)
                        "اطلاعات مهمان پس از اولین ورود برای همیشه پاک می‌شود و قابل بازگشت نیست."
                    else
                        "Guest data is permanently erased after your first sign-in and cannot be restored.",
                    onClick = onDeleteGuestData
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(
                    text = if (isPersian) "انصراف و بازگشت" else "Cancel and go back",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** A tappable choice card used inside [GuestRegisterChoiceDialog]; the accent colour comes from the theme. */
@Composable
private fun GuestChoiceOption(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.08f), shape)
            .border(1.dp, accent.copy(alpha = 0.3f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}