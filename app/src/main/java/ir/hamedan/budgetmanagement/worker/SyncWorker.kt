package ir.hamedan.budgetmanagement.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.network.ApiException
import ir.hamedan.budgetmanagement.data.sync.SyncEngine

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BudgetApp
        if (!app.container.authRepository.isAuthenticated()) return Result.success()

        return try {
            when (val result = app.container.syncEngine.sync()) {
                SyncEngine.SyncResult.NotAuthenticated,
                SyncEngine.SyncResult.ReauthenticationRequired -> Result.success()
                is SyncEngine.SyncResult.Success -> {
                    if (app.container.syncStateRepository.get().syncRequired) Result.retry()
                    else Result.success()
                }
            }
        } catch (e: ApiException) {
            when {
                e.statusCode == 429 -> Result.retry()
                e.statusCode in 500..599 -> Result.retry()
                e.statusCode == 0 -> Result.retry()
                else -> Result.success()
            }
        } catch (_: java.io.IOException) {
            Result.retry()
        } catch (_: Exception) {
            // Do not create an infinite retry loop for deterministic client/data errors.
            Result.success()
        }
    }
}