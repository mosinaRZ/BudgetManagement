package ir.hamedan.budgetmanagement.data.local

import android.content.Context
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

            val passphrase = DatabaseKeyProvider.getPassphrase(context)

            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "budget_management.db"
            )
                .openHelperFactory(factory)
                .build()
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