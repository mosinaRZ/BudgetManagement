package ir.hamedan.budgetmanagement.data.backup

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import ir.hamedan.budgetmanagement.utils.CategoryRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Encrypted, password-protected export and import of the user's data, to any file the user picks
 * (Storage Access Framework).
 *
 * Why it exists: the local database is encrypted with a key that lives in the Android Keystore.
 * Guest data is stored only on the device, so if that key is ever lost (factory reset, OEM
 * Keystore fault, restored/moved device) the database can no longer be opened and nothing else
 * holds the data. A backup file is protected by a password instead of the Keystore, so it
 * survives exactly that situation and can be restored on the same or another phone.
 *
 * Import never deletes anything. Records are matched by id: a record that does not exist is
 * added, an existing one is replaced only when the backup's copy is newer (`updatedAt`), and
 * ledger entries (saving-goal operations, debt payments) are immutable, so they are only added.
 * Everything imported is queued for synchronisation like any local edit.
 */
class LocalBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val syncLocalDataSource: SyncLocalDataSource
) {
    /**
     * [records] = items in the file. [added]/[updated]/[unchanged] describe what happened to them on
     * import (all 0 for export). [mergedCategories] = categories that already existed under another
     * id (e.g. the built-in Piggy Bank) and were matched instead of duplicated.
     */
    data class Summary(
        val records: Int,
        val added: Int = 0,
        val updated: Int = 0,
        val unchanged: Int = 0,
        val mergedCategories: Int = 0
    )

    /** @throws IOException the destination cannot be written. */
    suspend fun export(destination: Uri, password: CharArray): Summary = withContext(Dispatchers.IO) {
        val content = syncLocalDataSource.transaction { readAll() }
        val encrypted = BackupCrypto.encrypt(BackupCodec.encode(content, System.currentTimeMillis()), password)
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: throw IOException("Cannot open the backup destination")
        output.use {
            it.write(encrypted)
            it.flush()
        }
        Summary(content.totalRecords)
    }

    /**
     * Result of [exportToAppFolder]: where the file went, so the UI can tell the user exactly where
     * to find it.
     */
    data class SavedBackup(val uri: Uri, val fileName: String, val summary: Summary)

    /**
     * Saves the encrypted backup into the app's own, separate folder
     * (`Documents/`[FOLDER_NAME]`/`[BACKUPS_NAME]) without asking the user to pick a location.
     *
     * Android 10+: written through MediaStore (no storage permission needed, the folder is created
     * automatically). Android 8-9: written to the same public folder, which needs the legacy
     * WRITE_EXTERNAL_STORAGE permission (see [needsLegacyStoragePermission]).
     *
     * @throws IOException the file cannot be written.
     */
    suspend fun exportToAppFolder(password: CharArray): SavedBackup = withContext(Dispatchers.IO) {
        val content = syncLocalDataSource.transaction { readAll() }
        val now = System.currentTimeMillis()
        val encrypted = BackupCrypto.encrypt(BackupCodec.encode(content, now), password)
        val fileName = newFileName(now)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeWithMediaStore(fileName, encrypted)
        } else {
            writeLegacy(fileName, encrypted)
        }
        SavedBackup(uri, fileName, Summary(content.totalRecords))
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun writeWithMediaStore(fileName: String, bytes: ByteArray): Uri {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Cannot create the backup file")
        try {
            resolver.openOutputStream(uri, "wt")?.use {
                it.write(bytes)
                it.flush()
            } ?: throw IOException("Cannot open the backup file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            // Never leave a half-written (and therefore unreadable) backup behind.
            runCatching { resolver.delete(uri, null, null) }
            throw if (e is IOException) e else IOException("Cannot write the backup file", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun writeLegacy(fileName: String, bytes: ByteArray): Uri {
        val dir = File(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME),
            BACKUPS_NAME
        )
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) throw IOException("Cannot create the backup folder")
        val file = File(dir, fileName)
        try {
            file.outputStream().use {
                it.write(bytes)
                it.flush()
            }
        } catch (e: IOException) {
            runCatching { file.delete() }
            throw e
        }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(MIME_TYPE), null)
        return Uri.fromFile(file)
    }

    /**
     * @throws BackupFormatException not a backup file this version can read.
     * @throws BackupPasswordException wrong password, or the file was damaged.
     * @throws IOException the file cannot be read.
     */
    suspend fun import(source: Uri, password: CharArray): Summary = withContext(Dispatchers.IO) {
        val encrypted = context.contentResolver.openInputStream(source)?.use { readBounded(it) }
            ?: throw IOException("Cannot open the backup file")
        val content = BackupCodec.decode(BackupCrypto.decrypt(encrypted, password))
        syncLocalDataSource.transaction { applyBackup(content) }
    }

    private suspend fun readAll() = BackupContent(
        categories = database.categoryDao().getAllSnapshot(),
        transactions = database.transactionDao().getAllSnapshot(),
        budgetLimits = database.budgetLimitDao().getAllSnapshot(),
        debtCredits = database.debtCreditDao().getAllSnapshot(),
        debtPayments = database.debtPaymentDao().getAllSnapshot(),
        savingGoals = database.savingGoalDao().getAllGoalsSnapshot(),
        savingGoalOperations = database.savingGoalOperationDao().getAllSnapshot()
    )

    private class Counters {
        var added = 0
        var updated = 0
        var unchanged = 0
        var merged = 0
    }

    /**
     * Runs inside one Room transaction: either everything is imported or nothing is.
     *
     * Categories are matched by identity, not only by id: the built-in categories (Piggy Bank,
     * Food, ...) get different ids on different installs, so a backup's "Piggy Bank" must resolve
     * to the one already on this phone. Every reference (transactions, budget limits) is rewritten
     * to the resolved id, so nothing is duplicated and nothing is left pointing at a missing row.
     */
    private suspend fun applyBackup(content: BackupContent): Summary {
        val counters = Counters()

        // Heal duplicates that earlier imports/syncs may already have created on this device.
        healDuplicateCategories()

        val known = database.categoryDao().getAllSnapshot().toMutableList()
        val idMap = HashMap<String, String>()

        // Parents first, so references are valid at every point of the transaction.
        content.categories.forEach { backup ->
            val sameId = known.indexOfFirst { it.id == backup.id }
            if (sameId >= 0) {
                val current = known[sameId]
                if (backup.updatedAt > current.updatedAt) {
                    val merged = backup.copy(isSystem = backup.isSystem || current.isSystem)
                    database.categoryDao().insert(merged)
                    known[sameId] = merged
                    record(SyncEntityType.CATEGORY, merged.id, merged.updatedAt)
                    counters.updated++
                } else {
                    counters.unchanged++
                }
                idMap[backup.id] = backup.id
                return@forEach
            }
            val twin = known.firstOrNull { CategoryRules.isSameIdentity(it, backup) }
            if (twin != null) {
                // Same category under another id: reuse the local one instead of adding a duplicate.
                idMap[backup.id] = twin.id
                counters.merged++
                return@forEach
            }
            database.categoryDao().insert(backup)
            known += backup
            idMap[backup.id] = backup.id
            record(SyncEntityType.CATEGORY, backup.id, backup.updatedAt)
            counters.added++
        }
        fun resolve(categoryId: String): String = idMap[categoryId] ?: categoryId

        content.savingGoals.forEach { backup ->
            val current = database.savingGoalDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.savingGoalDao().insertGoal(backup)
                record(SyncEntityType.SAVING_GOAL, backup.id, backup.updatedAt)
                if (current == null) counters.added++ else counters.updated++
            } else counters.unchanged++
        }
        content.debtCredits.forEach { backup ->
            val current = database.debtCreditDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.debtCreditDao().insertOrUpdate(backup)
                record(SyncEntityType.DEBT_CREDIT, backup.id, backup.updatedAt)
                if (current == null) counters.added++ else counters.updated++
            } else counters.unchanged++
        }
        content.transactions.forEach { original ->
            val backup = original.copy(categoryId = resolve(original.categoryId))
            val current = database.transactionDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.transactionDao().insertTransaction(backup)
                record(SyncEntityType.TRANSACTION, backup.id, backup.updatedAt)
                if (current == null) counters.added++ else counters.updated++
            } else counters.unchanged++
        }
        content.budgetLimits.forEach { original ->
            val backup = original.copy(categoryId = resolve(original.categoryId))
            val current = database.budgetLimitDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.budgetLimitDao().insertOrUpdate(backup)
                record(SyncEntityType.BUDGET_LIMIT, backup.id, backup.updatedAt)
                if (current == null) counters.added++ else counters.updated++
            } else counters.unchanged++
        }
        // Ledger entries are immutable: insert-if-missing (the DAOs ignore existing ids).
        content.savingGoalOperations.forEach { backup ->
            if (database.savingGoalOperationDao().insert(backup) != IGNORED_ROW) {
                record(SyncEntityType.SAVING_GOAL_OPERATION, backup.id, backup.updatedAt)
                counters.added++
            } else counters.unchanged++
        }
        content.debtPayments.forEach { backup ->
            if (database.debtPaymentDao().insert(backup) != IGNORED_ROW) {
                record(SyncEntityType.DEBT_PAYMENT, backup.id, backup.updatedAt)
                counters.added++
            } else counters.unchanged++
        }

        return Summary(
            records = content.totalRecords,
            added = counters.added,
            updated = counters.updated,
            unchanged = counters.unchanged,
            mergedCategories = counters.merged
        )
    }

    /**
     * Collapses categories that are the same category stored more than once (same identity, see
     * [CategoryRules.isSameIdentity]) into one canonical row: transactions and budget limits are
     * moved to it, the extras are deleted and tombstoned for sync. Only built-in categories are
     * healed automatically; user categories with the same name are left alone because the user
     * may have created them on purpose.
     */
    private suspend fun healDuplicateCategories() {
        val now = System.currentTimeMillis()
        val groups = database.categoryDao().getAllSnapshot()
            .filter { CategoryRules.isBuiltInKey(it.title) }
            .groupBy { it.title.trim().uppercase() }
            .values
            .filter { it.size > 1 }
        groups.forEach { group ->
            val canonical = CategoryRules.pickCanonical(group)
            group.filter { it.id != canonical.id }.forEach { duplicate ->
                database.transactionDao().getByCategoryId(duplicate.id).forEach { tx ->
                    record(SyncEntityType.TRANSACTION, tx.id, now)
                }
                database.transactionDao().reassignCategoryForTransactions(duplicate.id, canonical.id, now)

                database.budgetLimitDao().getByCategoryId(duplicate.id).forEach { limit ->
                    record(SyncEntityType.BUDGET_LIMIT, limit.id, now)
                }
                database.budgetLimitDao().reassignCategoryForLimits(duplicate.id, canonical.id, now)

                database.categoryDao().delete(duplicate)
                syncLocalDataSource.recordMutationInTransaction(
                    entityType = SyncEntityType.CATEGORY,
                    entityId = duplicate.id,
                    updatedAt = now,
                    isDeleted = true
                )
            }
            if (!canonical.isSystem && canonical.title.trim().uppercase() in SYSTEM_TITLES) {
                val fixed = canonical.copy(isSystem = true, updatedAt = now)
                database.categoryDao().insert(fixed)
                record(SyncEntityType.CATEGORY, fixed.id, now)
            }
        }
    }

    private suspend fun record(entityType: String, id: String, updatedAt: Long) {
        syncLocalDataSource.recordMutationInTransaction(entityType, id, updatedAt, isDeleted = false)
    }

    private fun readBounded(input: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_BACKUP_BYTES) throw BackupFormatException("Backup file is too large")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    companion object {
        /** The app's own top-level folder inside Documents. */
        const val FOLDER_NAME = "Cidna"
        const val BACKUPS_NAME = "Backups"
        const val FILE_EXTENSION = "cidnabackup"

        /** Shown to the user, e.g. in the "saved to ..." message. */
        const val DISPLAY_PATH = "Documents/$FOLDER_NAME/$BACKUPS_NAME"
        private const val RELATIVE_PATH = "Documents/$FOLDER_NAME/$BACKUPS_NAME/"
        private const val MIME_TYPE = "application/octet-stream"

        /** True when saving to the shared Documents folder needs the runtime WRITE_EXTERNAL_STORAGE permission. */
        fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        /**
         * Where the system file picker should open when restoring, so the user lands directly in the
         * app's backup folder. Only a hint: the picker ignores it if the folder does not exist.
         */
        fun backupFolderPickerHint(): Uri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Documents/$FOLDER_NAME/$BACKUPS_NAME"
        )

        internal fun newFileName(timeMillis: Long): String {
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date(timeMillis))
            return "cidna-backup-$stamp.$FILE_EXTENSION"
        }

        /** Room's insert returns -1 when OnConflictStrategy.IGNORE skipped the row. */
        private const val IGNORED_ROW = -1L
        private const val MAX_BACKUP_BYTES = 128L * 1024 * 1024
        private val SYSTEM_TITLES = setOf("DEBT_CREDIT_PAYABLE", "DEBT_CREDIT_RECEIVABLE", "SAVING_GOAL", "UNCATEGORIZED")
    }
}