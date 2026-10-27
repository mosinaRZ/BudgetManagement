package ir.hamedan.budgetmanagement.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.backup.BackupCrypto
import ir.hamedan.budgetmanagement.data.backup.BackupFormatException
import ir.hamedan.budgetmanagement.data.backup.BackupPasswordException
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.components.showInfo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Save backup" / "Restore backup" buttons with their file pickers and password dialogs.
 * Everything runs through [ir.hamedan.budgetmanagement.data.backup.LocalBackupManager].
 */
@Composable
fun LocalBackupButtons(
    snackbarHostState: SnackbarHostState,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember(context) { (context.applicationContext as BudgetApp).container.localBackupManager }
    val scope = rememberCoroutineScope()

    var askExportPassword by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri == null || password == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val chars = password.toCharArray()
            try {
                val summary = manager.export(uri, chars)
                snackbarHostState.showInfo(
                    if (isPersian) "پشتیبان ذخیره شد (${summary.records} مورد). رمز آن را جایی امن نگه دارید؛ بدون رمز قابل بازیابی نیست."
                    else "Backup saved (${summary.records} items). Keep its password safe; the backup cannot be opened without it."
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LocalBackup", "Export failed", e)
                snackbarHostState.showError(
                    if (isPersian) "ذخیره پشتیبان ناموفق بود. دوباره تلاش کنید."
                    else "Saving the backup failed. Please try again."
                )
            } finally {
                chars.fill('\u0000')
                busy = false
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackupInfoNote(
            text = if (isPersian) "پشتیبان با رمز شما رمزنگاری می‌شود و بدون وابستگی به کلید گوشی باز می‌شود."
            else "The backup is encrypted with your password and does not depend on this phone's security key."
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsActionButton(
                title = if (isPersian) "ذخیره پشتیبان" else "Save backup",
                icon = Icons.Default.Save,
                enabled = !busy,
                loading = busy,
                modifier = Modifier.weight(1f)
            ) { askExportPassword = true }
            SettingsActionButton(
                title = if (isPersian) "بازیابی پشتیبان" else "Restore backup",
                icon = Icons.Default.Download,
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { openLauncher.launch(arrayOf("*/*")) }
        }
    }

    if (askExportPassword) {
        BackupPasswordDialog(
            isPersian = isPersian,
            title = if (isPersian) "رمز پشتیبان" else "Backup password",
            confirmNeeded = true,
            onDismiss = { askExportPassword = false },
            onConfirm = { password ->
                askExportPassword = false
                pendingExportPassword = password
                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                createLauncher.launch("cidna-backup-$stamp.cidnabackup")
            }
        )
    }

    pendingImportUri?.let { uri ->
        BackupPasswordDialog(
            isPersian = isPersian,
            title = if (isPersian) "رمز فایل پشتیبان" else "Password of the backup file",
            confirmNeeded = false,
            onDismiss = { pendingImportUri = null },
            onConfirm = { password ->
                pendingImportUri = null
                busy = true
                scope.launch {
                    val chars = password.toCharArray()
                    try {
                        val summary = manager.import(uri, chars)
                        snackbarHostState.showInfo(
                            if (isPersian) "پشتیبان بازیابی شد (${summary.records} مورد). موارد موجود فقط وقتی جایگزین می‌شوند که نسخه‌ی پشتیبان جدیدتر باشد."
                            else "Backup restored (${summary.records} items). Existing items are replaced only when the backup's copy is newer."
                        )
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: BackupPasswordException) {
                        snackbarHostState.showError(
                            if (isPersian) "رمز اشتباه است یا فایل آسیب دیده است." else "Wrong password, or the file is damaged."
                        )
                    } catch (e: BackupFormatException) {
                        snackbarHostState.showError(
                            if (isPersian) "این فایل، پشتیبان معتبر این برنامه نیست." else "This is not a valid backup file for this app."
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LocalBackup", "Import failed", e)
                        snackbarHostState.showError(
                            if (isPersian) "بازیابی پشتیبان ناموفق بود. چیزی تغییر نکرد." else "Restoring the backup failed. Nothing was changed."
                        )
                    } finally {
                        chars.fill('\u0000')
                        busy = false
                    }
                }
            }
        )
    }
}

@Composable
private fun BackupPasswordDialog(
    isPersian: Boolean,
    title: String,
    confirmNeeded: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val min = BackupCrypto.MIN_PASSWORD_LENGTH
    val longEnough = password.length >= min
    val matches = !confirmNeeded || password == repeat
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (confirmNeeded) {
                    Text(
                        if (isPersian) "این رمز را فراموش نکنید؛ بدون آن پشتیبان قابل بازیابی نیست (حداقل $min کاراکتر)."
                        else "Do not forget this password: the backup cannot be opened without it (at least $min characters)."
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text(if (isPersian) "رمز" else "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (confirmNeeded) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it },
                        singleLine = true,
                        label = { Text(if (isPersian) "تکرار رمز" else "Repeat password") },
                        isError = repeat.isNotEmpty() && !matches,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = longEnough && matches) {
                Text(if (isPersian) "ادامه" else "Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

/** Small bold heading + one-line hint used to separate groups inside a settings card. */
@Composable
internal fun SettingsGroupLabel(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun BackupInfoNote(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/** Shared action button for the export and backup groups, so both look identical. */
@Composable
internal fun SettingsActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled || loading) 1f else 0.5f)
            .clip(shape)
            .background(primary.copy(alpha = 0.05f))
            .border(BorderStroke(1.dp, primary.copy(alpha = 0.15f)), shape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(color = primary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Icon(imageVector = icon, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = primary)
    }
}