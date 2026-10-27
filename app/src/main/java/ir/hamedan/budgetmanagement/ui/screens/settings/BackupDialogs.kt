package ir.hamedan.budgetmanagement.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.hamedan.budgetmanagement.data.backup.BackupCrypto
import ir.hamedan.budgetmanagement.data.backup.LocalBackupManager
import java.text.NumberFormat
import java.util.Locale

/**
 * Shared shell of every backup dialog. Uses the same visual language as the rest of the app's
 * dialogs (28dp corners, translucent surface, hairline border, round tinted icon badge), so the
 * backup flow follows the active theme (light/dark/dynamic) instead of looking like a stock
 * Material alert.
 */
@Composable
private fun BackupDialogShell(
    icon: ImageVector,
    accent: Color,
    title: String,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f), shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)
                .clip(shape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, accent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                content()
            }
        }
    }
}

@Composable
private fun DialogNote(text: String, accent: Color, icon: ImageVector) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.15f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun DialogButtons(
    isPersian: Boolean,
    confirmText: String,
    confirmEnabled: Boolean,
    onCancel: (() -> Unit)?,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onCancel != null) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isPersian) "انصراف" else "Cancel",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = confirmText, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private enum class PasswordStrength { EMPTY, WEAK, FAIR, STRONG }

private fun strengthOf(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY
    if (password.length < BackupCrypto.MIN_PASSWORD_LENGTH) return PasswordStrength.WEAK
    var classes = 0
    if (password.any { it.isLowerCase() }) classes++
    if (password.any { it.isUpperCase() }) classes++
    if (password.any { it.isDigit() }) classes++
    if (password.any { !it.isLetterOrDigit() }) classes++
    return when {
        password.length >= 12 && classes >= 3 -> PasswordStrength.STRONG
        password.length >= 10 || classes >= 3 -> PasswordStrength.FAIR
        else -> PasswordStrength.WEAK
    }
}

@Composable
private fun PasswordStrengthBar(password: String, isPersian: Boolean) {
    val strength = strengthOf(password)
    val target = when (strength) {
        PasswordStrength.EMPTY -> 0f
        PasswordStrength.WEAK -> 0.34f
        PasswordStrength.FAIR -> 0.67f
        PasswordStrength.STRONG -> 1f
    }
    val progress by animateFloatAsState(target, label = "passwordStrength")
    val color by animateColorAsState(
        when (strength) {
            PasswordStrength.EMPTY, PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
            PasswordStrength.FAIR -> MaterialTheme.colorScheme.tertiary
            PasswordStrength.STRONG -> MaterialTheme.colorScheme.primary
        },
        label = "passwordStrengthColor"
    )
    val label = when (strength) {
        PasswordStrength.EMPTY -> ""
        PasswordStrength.WEAK -> if (isPersian) "ضعیف" else "Weak"
        PasswordStrength.FAIR -> if (isPersian) "متوسط" else "Fair"
        PasswordStrength.STRONG -> if (isPersian) "قوی" else "Strong"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun BackupPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        isError = isError,
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** Password prompt for saving ([confirmNeeded] = true) or restoring a backup. */
@Composable
internal fun BackupPasswordDialog(
    isPersian: Boolean,
    isExport: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val min = BackupCrypto.MIN_PASSWORD_LENGTH
    val numbers = remember(isPersian) { NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US) }
    val longEnough = password.length >= min
    val matches = !isExport || password == repeat
    val canConfirm = if (isExport) longEnough && matches && repeat.isNotEmpty() else password.isNotEmpty()
    val accent = MaterialTheme.colorScheme.primary

    BackupDialogShell(
        icon = if (isExport) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
        accent = accent,
        title = if (isExport) {
            if (isPersian) "رمز پشتیبان" else "Backup password"
        } else {
            if (isPersian) "رمز فایل پشتیبان" else "Backup file password"
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = if (isExport) {
                if (isPersian) "پشتیبان با این رمز رمزنگاری می‌شود. رمز را در جای امنی نگه دارید."
                else "Your backup will be encrypted with this password. Keep it somewhere safe."
            } else {
                if (isPersian) "رمزی را وارد کنید که هنگام ذخیره‌ی این پشتیبان انتخاب کرده بودید."
                else "Enter the password you chose when this backup was saved."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        BackupPasswordField(
            value = password,
            onValueChange = { password = it },
            label = if (isPersian) "رمز" else "Password",
            supportingText = if (isExport) {
                if (isPersian) "حداقل ${numbers.format(min)} کاراکتر" else "At least $min characters"
            } else null
        )

        if (isExport) {
            PasswordStrengthBar(password = password, isPersian = isPersian)
            BackupPasswordField(
                value = repeat,
                onValueChange = { repeat = it },
                label = if (isPersian) "تکرار رمز" else "Repeat password",
                isError = repeat.isNotEmpty() && !matches,
                supportingText = if (repeat.isNotEmpty() && !matches) {
                    if (isPersian) "رمزها یکسان نیستند" else "Passwords do not match"
                } else null
            )
            DialogNote(
                text = if (isPersian) "بدون این رمز هیچ‌کس، حتی ما، نمی‌تواند پشتیبان را باز کند."
                else "Without this password nobody, not even us, can open the backup.",
                accent = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Rounded.WarningAmber
            )
        } else {
            DialogNote(
                text = if (isPersian) "اطلاعات موجود حذف نمی‌شوند؛ فقط موارد جدید اضافه یا نسخه‌های قدیمی‌تر به‌روز می‌شوند."
                else "Nothing is deleted: new items are added and older copies are updated.",
                accent = accent,
                icon = Icons.Rounded.CheckCircle
            )
        }

        DialogButtons(
            isPersian = isPersian,
            confirmText = if (isExport) {
                if (isPersian) "ادامه" else "Continue"
            } else {
                if (isPersian) "بازیابی" else "Restore"
            },
            confirmEnabled = canConfirm,
            onCancel = onDismiss,
            onConfirm = { onConfirm(password) }
        )
    }
}

/** Shown after a successful restore: what actually happened to the data. */
@Composable
internal fun BackupResultDialog(
    isPersian: Boolean,
    summary: LocalBackupManager.Summary,
    onDismiss: () -> Unit
) {
    val numbers = remember(isPersian) { NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US) }
    val accent = MaterialTheme.colorScheme.primary
    BackupDialogShell(
        icon = Icons.Rounded.CheckCircle,
        accent = accent,
        title = if (isPersian) "بازیابی با موفقیت انجام شد" else "Restore completed",
        onDismiss = onDismiss
    ) {
        Text(
            text = if (isPersian) "${numbers.format(summary.records)} مورد در فایل پشتیبان بررسی شد."
            else "${numbers.format(summary.records)} items in the backup were processed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(accent.copy(alpha = 0.06f))
                .border(1.dp, accent.copy(alpha = 0.12f), shape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ResultRow(if (isPersian) "اضافه‌شده" else "Added", numbers.format(summary.added))
            ResultRow(if (isPersian) "به‌روزرسانی‌شده" else "Updated", numbers.format(summary.updated))
            ResultRow(if (isPersian) "بدون تغییر (موجود یا قدیمی‌تر)" else "Unchanged (already present or older)", numbers.format(summary.unchanged))
            if (summary.mergedCategories > 0) {
                ResultRow(
                    if (isPersian) "دسته‌بندی ادغام‌شده (بدون تکرار)" else "Categories merged (no duplicates)",
                    numbers.format(summary.mergedCategories)
                )
            }
        }

        DialogButtons(
            isPersian = isPersian,
            confirmText = if (isPersian) "متوجه شدم" else "Got it",
            confirmEnabled = true,
            onCancel = null,
            onConfirm = onDismiss
        )
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Non-dismissable progress card while the (deliberately slow) key derivation + I/O runs. */
@Composable
internal fun BackupProgressDialog(isPersian: Boolean, isExport: Boolean) {
    BackupDialogShell(
        icon = Icons.Rounded.Lock,
        accent = MaterialTheme.colorScheme.primary,
        title = if (isExport) {
            if (isPersian) "در حال ذخیره‌ی پشتیبان…" else "Saving backup…"
        } else {
            if (isPersian) "در حال بازیابی پشتیبان…" else "Restoring backup…"
        },
        onDismiss = {},
        dismissible = false
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
        Text(
            text = if (isPersian) "لطفاً تا پایان کار صبر کنید." else "Please wait until it finishes.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}