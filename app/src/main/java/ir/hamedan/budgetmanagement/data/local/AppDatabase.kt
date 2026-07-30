package ir.hamedan.budgetmanagement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.hamedan.budgetmanagement.BuildConfig
import ir.hamedan.budgetmanagement.data.local.dao.BudgetLimitDao
import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.NotificationDao
import ir.hamedan.budgetmanagement.data.local.dao.PendingTransactionDao
import ir.hamedan.budgetmanagement.data.local.dao.SavingGoalDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.dao.UpcomingPaymentDao
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.NotificationEntity
import ir.hamedan.budgetmanagement.data.local.models.PendingTransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import ir.hamedan.budgetmanagement.data.local.models.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        SavingGoalEntity::class,
        BudgetLimitEntity::class,
        UpcomingPaymentEntity::class,
        NotificationEntity::class,
        PendingTransactionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun budgetLimitDao(): BudgetLimitDao
    abstract fun upcomingPaymentDao(): UpcomingPaymentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun pendingTransactionDao(): PendingTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // =========================================================
        //  Migrationها اینجا تعریف می‌شوند
        //  مثال برای نسخه‌های بعدی:
        //
        //  private val MIGRATION_1_2 = object : Migration(1, 2) {
        //      override fun migrate(db: SupportSQLiteDatabase) {
        //          db.execSQL("ALTER TABLE transactions ADD COLUMN new_column TEXT NOT NULL DEFAULT ''")
        //      }
        //  }
        // =========================================================

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "budget_management_db"
            )
                // .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)
                .apply {
                    // فقط در حالت Debug اجازه پاک شدن دیتابیس را بده
                    // در Release اگر Migration نوشته نشود، برنامه کرش می‌کند
                    // تا داده‌های کاربر به اشتباه پاک نشوند.
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    }
                }
                .build()
        }
    }
}