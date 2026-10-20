package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Entry point of guest mode on the sign-in screen. The label names the method the guest set up. */
@Composable
fun GuestEntryButton(
    label: String,
    usesFingerprint: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Icon(
            imageVector = if (usesFingerprint) Icons.Default.Fingerprint else Icons.Default.PersonOutline,
            contentDescription = null,
            modifier = Modifier.height(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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