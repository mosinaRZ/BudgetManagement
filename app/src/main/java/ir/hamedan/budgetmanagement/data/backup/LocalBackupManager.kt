package ir.hamedan.budgetmanagement.data.backup

import android.content.Context
import android.net.Uri
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    data class Summary(val records: Int)

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
     * @throws BackupFormatException not a backup file this version can read.
     * @throws BackupPasswordException wrong password, or the file was damaged.
     * @throws IOException the file cannot be read.
     */
    suspend fun import(source: Uri, password: CharArray): Summary = withContext(Dispatchers.IO) {
        val encrypted = context.contentResolver.openInputStream(source)?.use { readBounded(it) }
            ?: throw IOException("Cannot open the backup file")
        val content = BackupCodec.decode(BackupCrypto.decrypt(encrypted, password))
        syncLocalDataSource.transaction { applyBackup(content) }
        Summary(content.totalRecords)
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

    /** Runs inside one Room transaction: either everything is imported or nothing is. */
    private suspend fun applyBackup(content: BackupContent) {
        // Parents first, so references are valid at every point of the transaction.
        content.categories.forEach { backup ->
            val current = database.categoryDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.categoryDao().insert(backup)
                record(SyncEntityType.CATEGORY, backup.id, backup.updatedAt)
            }
        }
        content.savingGoals.forEach { backup ->
            val current = database.savingGoalDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.savingGoalDao().insertGoal(backup)
                record(SyncEntityType.SAVING_GOAL, backup.id, backup.updatedAt)
            }
        }
        content.debtCredits.forEach { backup ->
            val current = database.debtCreditDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.debtCreditDao().insertOrUpdate(backup)
                record(SyncEntityType.DEBT_CREDIT, backup.id, backup.updatedAt)
            }
        }
        content.transactions.forEach { backup ->
            val current = database.transactionDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.transactionDao().insertTransaction(backup)
                record(SyncEntityType.TRANSACTION, backup.id, backup.updatedAt)
            }
        }
        content.budgetLimits.forEach { backup ->
            val current = database.budgetLimitDao().getById(backup.id)
            if (current == null || backup.updatedAt > current.updatedAt) {
                database.budgetLimitDao().insertOrUpdate(backup)
                record(SyncEntityType.BUDGET_LIMIT, backup.id, backup.updatedAt)
            }
        }
        // Ledger entries are immutable: insert-if-missing (the DAOs ignore existing ids).
        content.savingGoalOperations.forEach { backup ->
            if (database.savingGoalOperationDao().insert(backup) != IGNORED_ROW) {
                record(SyncEntityType.SAVING_GOAL_OPERATION, backup.id, backup.updatedAt)
            }
        }
        content.debtPayments.forEach { backup ->
            if (database.debtPaymentDao().insert(backup) != IGNORED_ROW) {
                record(SyncEntityType.DEBT_PAYMENT, backup.id, backup.updatedAt)
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

    private companion object {
        /** Room's insert returns -1 when OnConflictStrategy.IGNORE skipped the row. */
        const val IGNORED_ROW = -1L
        const val MAX_BACKUP_BYTES = 128L * 1024 * 1024
    }
}