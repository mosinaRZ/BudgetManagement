package ir.hamedan.budgetmanagement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ir.hamedan.budgetmanagement.data.local.dao.CategoryDao
import ir.hamedan.budgetmanagement.data.local.dao.TransactionDao
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
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
        UpcomingPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    // TODO: وقتی بقیه‌ی Repository ها (User, UserSettings, SavingGoal, BudgetLimit, UpcomingPayment)
    // رو فرستادی، DAO های مربوطه ساخته و اینجا اضافه می‌شن:
    // abstract fun userDao(): UserDao
    // abstract fun userSettingsDao(): UserSettingsDao
    // abstract fun savingGoalDao(): SavingGoalDao
    // abstract fun budgetLimitDao(): BudgetLimitDao
    // abstract fun upcomingPaymentDao(): UpcomingPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_management_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}