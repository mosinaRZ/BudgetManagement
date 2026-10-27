package ir.hamedan.budgetmanagement.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.backup.LocalBackupManager
import ir.hamedan.budgetmanagement.data.backup.BackupFormatException
import ir.hamedan.budgetmanagement.data.backup.BackupPasswordException
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.components.showInfo
import kotlinx.coroutines.launch

/**
 * "Save backup" / "Restore backup" buttons with their password dialogs.
 *
 * Saving needs no file picker: the backup goes straight into the app's own folder
 * (`Documents/Cidna/Backups`, see [LocalBackupManager.DISPLAY_PATH]). Restoring opens the system
 * picker directly inside that folder.
 *
 * Only shown in guest mode: an account's data lives (encrypted) on the server and returns after
 * sign-in, so a local backup would be redundant there.
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
    var busyIsExport by remember { mutableStateOf(true) }
    var restoreResult by remember { mutableStateOf<LocalBackupManager.Summary?>(null) }

    fun runExport(password: String) {
        busyIsExport = true
        busy = true
        scope.launch {
            val chars = password.toCharArray()
            try {
                val saved = manager.exportToAppFolder(chars)
                snackbarHostState.showInfo(
                    message = if (isPersian) "پشتیبان (${saved.summary.records} مورد) در پوشه‌ی ${LocalBackupManager.DISPLAY_PATH} ذخیره شد. رمز آن را جایی امن نگه دارید؛ بدون رمز قابل بازیابی نیست."
                    else "Backup (${saved.summary.records} items) saved to ${LocalBackupManager.DISPLAY_PATH}. Keep its password safe; the backup cannot be opened without it.",
                    duration = SnackbarDuration.Long
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

    // Android 8-9 only: the shared Documents folder needs the legacy storage permission.
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (password == null) return@rememberLauncherForActivityResult
        if (granted) {
            runExport(password)
        } else {
            scope.launch {
                snackbarHostState.showError(
                    if (isPersian) "برای ذخیره پشتیبان در پوشه‌ی برنامه، اجازه‌ی دسترسی به حافظه لازم است."
                    else "Storage permission is needed to save the backup in the app's folder."
                )
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(OpenBackupDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackupInfoNote(
            text = if (isPersian) "پشتیبان با رمز شما رمزنگاری می‌شود و بدون وابستگی به کلید گوشی باز می‌شود. فایل‌ها در پوشه‌ی اختصاصی برنامه ذخیره می‌شوند: ${LocalBackupManager.DISPLAY_PATH}"
            else "The backup is encrypted with your password and does not depend on this phone's security key. Files are saved in the app's own folder: ${LocalBackupManager.DISPLAY_PATH}"
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
            isExport = true,
            onDismiss = { askExportPassword = false },
            onConfirm = { password ->
                askExportPassword = false
                val needsPermission = LocalBackupManager.needsLegacyStoragePermission() &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    pendingExportPassword = password
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    runExport(password)
                }
            }
        )
    }

    pendingImportUri?.let { uri ->
        BackupPasswordDialog(
            isPersian = isPersian,
            isExport = false,
            onDismiss = { pendingImportUri = null },
            onConfirm = { password ->
                pendingImportUri = null
                busyIsExport = false
                busy = true
                scope.launch {
                    val chars = password.toCharArray()
                    try {
                        val summary = manager.import(uri, chars)
                        restoreResult = summary
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

    if (busy) {
        BackupProgressDialog(isPersian = isPersian, isExport = busyIsExport)
    }

    restoreResult?.let { summary ->
        BackupResultDialog(
            isPersian = isPersian,
            summary = summary,
            onDismiss = { restoreResult = null }
        )
    }
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

/**
 * Same as the standard "open a document" picker, but asks the system to start inside the app's own
 * backup folder (`Documents/Cidna/Backups`). The hint is ignored by the system if the folder does
 * not exist yet, in which case the picker simply opens where it normally would.
 */
private class OpenBackupDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, LocalBackupManager.backupFolderPickerHint())
        }
}