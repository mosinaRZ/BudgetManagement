package ir.hamedan.budgetmanagement.di

import android.content.Context
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSourceImpl
import ir.hamedan.budgetmanagement.data.network.ApiHttpClient
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.network.DeviceApi
import ir.hamedan.budgetmanagement.data.network.SyncApi
import ir.hamedan.budgetmanagement.data.repository.AuthRepository
import ir.hamedan.budgetmanagement.data.repository.AuthRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepository
import ir.hamedan.budgetmanagement.data.repository.BudgetLimitRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.CategoryRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.NotificationRepository
import ir.hamedan.budgetmanagement.data.repository.NotificationRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepository
import ir.hamedan.budgetmanagement.data.repository.PendingTransactionRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepository
import ir.hamedan.budgetmanagement.data.repository.SavingGoalRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.SyncMetadataRepository
import ir.hamedan.budgetmanagement.data.repository.SyncMetadataRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.SyncStateRepository
import ir.hamedan.budgetmanagement.data.repository.SyncStateRepositoryImpl
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepositoryImpl
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
import ir.hamedan.budgetmanagement.data.security.SyncKeyManager
import ir.hamedan.budgetmanagement.data.sync.SyncEngine

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: AppDatabase = AppDatabase.getInstance(appContext)

    val syncLocalDataSource: SyncLocalDataSource by lazy { SyncLocalDataSourceImpl(database, deviceIdentityStore) }
    val syncStateRepository: SyncStateRepository by lazy { SyncStateRepositoryImpl(database.syncStateDao(), deviceIdentityStore) }
    val syncMetadataRepository: SyncMetadataRepository by lazy { SyncMetadataRepositoryImpl(database.syncMetadataDao()) }

    private val apiHttpClient: ApiHttpClient by lazy { ApiHttpClient() }
    val authApi: AuthApi by lazy { AuthApi(apiHttpClient) }
    val deviceApi: DeviceApi by lazy { DeviceApi(apiHttpClient) }
    val syncApi: SyncApi by lazy { SyncApi(apiHttpClient) }
    val authSessionStore: AuthSessionStore by lazy { AuthSessionStore(appContext) }
    val deviceIdentityStore: DeviceIdentityStore by lazy { DeviceIdentityStore(appContext) }
    val syncKeyManager: SyncKeyManager by lazy { SyncKeyManager(appContext) }
    val syncEngine: SyncEngine by lazy {
        SyncEngine(database, syncApi, authApi, authSessionStore, syncKeyManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            authApi = authApi,
            userDao = database.userDao(),
            syncStateRepository = syncStateRepository,
            sessionStore = authSessionStore,
            syncKeyManager = syncKeyManager,
            syncEngine = syncEngine,
            syncLocalDataSource = syncLocalDataSource,
            deviceIdentityStore = deviceIdentityStore
        )
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao(), syncLocalDataSource)
    }

    val budgetLimitRepository: BudgetLimitRepository by lazy {
        BudgetLimitRepositoryImpl(database.budgetLimitDao(), syncLocalDataSource)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao(), database.transactionDao(), database.budgetLimitDao(), syncLocalDataSource)
    }

    val savingGoalRepository: SavingGoalRepository by lazy {
        SavingGoalRepositoryImpl(database.savingGoalDao(), database.savingGoalOperationDao(), syncLocalDataSource)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(database.notificationDao())
    }

    val pendingTransactionRepository: PendingTransactionRepository by lazy {
        PendingTransactionRepositoryImpl(database.pendingTransactionDao())
    }

    val debtCreditRepository: DebtCreditRepository by lazy {
        DebtCreditRepositoryImpl(database.debtCreditDao(), database.debtPaymentDao(), syncLocalDataSource)
    }

    fun viewModelFactory(): AppViewModelFactory = AppViewModelFactory(this, appContext)
}