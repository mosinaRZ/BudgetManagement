package ir.hamedan.budgetmanagement.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ir.hamedan.budgetmanagement.data.local.dao.BudgetLimitDao
import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.DebtCreditDao
import ir.hamedan.budgetmanagement.data.local.dao.DebtPaymentDao
import ir.hamedan.budgetmanagement.data.local.dao.NotificationDao
import ir.hamedan.budgetmanagement.data.local.dao.PendingTransactionDao
import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalDao
import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalOperationDao
import ir.hamedan.budgetmanagement.data.local.dao.SyncMetadataDao
import ir.hamedan.budgetmanagement.data.local.dao.SyncStateDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtPaymentEntity
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalOperationEntity
import ir.hamedan.budgetmanagement.data.local.models.SyncMetadataEntity
import ir.hamedan.budgetmanagement.data.local.models.SyncStateEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.security.DatabaseKeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        SavingGoalEntity::class,
        BudgetLimitEntity::class,
        NotificationEntity::class,
        PendingTransactionEntity::class,
        DebtCreditEntity::class,
        SavingGoalOperationEntity::class,
        DebtPaymentEntity::class,

        // Sync
        SyncMetadataEntity::class,
        SyncStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    companion object {

        /** Keep in sync with the exclusions in res/xml/backup_rules.xml and data_extraction_rules.xml. */
        const val DATABASE_NAME = "budget_management.db"

        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {

            // SQLCipher native library
            System.loadLibrary("sqlcipher")

            // The passphrase is durably committed (and verified) before it is returned, i.e. before
            // SQLCipher can create the file with it. If no usable key exists (first run, or the key
            // was definitively lost), the hook removes the database that the new key cannot open.
            val passphrase = DatabaseKeyProvider.getPassphrase(context) {
                deleteUnreadableDatabase(context)
            }

            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .build()
        }

        /**
         * Deletes the database files (main, -wal, -shm, -journal). Only called when no key that can
         * open the existing file is available, so the file is unreadable anyway.
         *
         * Be clear about what this means: nothing restores this data automatically.
         *  - Account data is synced to the server, so it returns after the next sign-in/sync
         *    (changes that were never synced are lost).
         *  - Guest-workspace data exists ONLY on this device and is NOT synced anywhere, so it is
         *    gone for good, unless the user kept a backup file (see LocalBackupManager). The UI
         *    tells the user (DatabaseKeyProvider.consumeKeyResetNotice) and offers restoring one.
         *
         * Throws if a file cannot be removed, which aborts the key replacement instead of leaving a
         * key that does not match the file.
         */
        private fun deleteUnreadableDatabase(context: Context) {
            val file = context.getDatabasePath(DATABASE_NAME)
            if (file.exists()) {
                Log.w(TAG, "Deleting database that cannot be opened with the available key")
            }
            // Also removes -wal/-shm/-journal leftovers, even if the main file is already gone.
            context.deleteDatabase(DATABASE_NAME)
            check(!file.exists()) { "Could not delete the unreadable database file" }
        }
    }

    // -------------------------------------------------------------------------
    // Local DAOs
    // -------------------------------------------------------------------------

    abstract fun userDao(): UserDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun savingGoalDao(): SavingGoalDao

    abstract fun budgetLimitDao(): BudgetLimitDao

    abstract fun notificationDao(): NotificationDao

    abstract fun pendingTransactionDao(): PendingTransactionDao

    abstract fun debtCreditDao(): DebtCreditDao

    abstract fun savingGoalOperationDao(): SavingGoalOperationDao

    abstract fun debtPaymentDao(): DebtPaymentDao

    // -------------------------------------------------------------------------
    // Sync DAOs
    // -------------------------------------------------------------------------

    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun syncStateDao(): SyncStateDao
}