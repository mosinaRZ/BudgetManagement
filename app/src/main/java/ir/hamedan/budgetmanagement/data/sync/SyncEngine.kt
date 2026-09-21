package ir.hamedan.budgetmanagement.data.sync

import android.util.Base64
import androidx.room.withTransaction
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.local.dao.*
import ir.hamedan.budgetmanagement.data.local.models.*
import ir.hamedan.budgetmanagement.data.network.ApiException
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.network.SyncApi
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.data.security.SyncKeyManager
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import ir.hamedan.budgetmanagement.data.time.ApiTime
import java.util.UUID

/**
 * Offline-first synchronization coordinator.
 *
 * P1 decisions:
 * - SyncMetadataEntity.version is the single client-side version authority.
 * - Conflicts use deterministic SERVER_WINS resolution: the authoritative
 *   server record is applied and the local pending version is acknowledged.
 * - Saving-goal/debt balances are derived from immutable operation records,
 *   preventing concurrent devices from overwriting aggregate changes.
 * - Notifications and raw pending SMS transactions are device-local and are
 *   deliberately excluded from cloud sync.
 */
class SyncEngine(
    private val database: AppDatabase,
    private val syncApi: SyncApi,
    private val authApi: AuthApi,
    private val sessionStore: AuthSessionStore,
    private val syncKeyManager: SyncKeyManager
) {
    suspend fun sync(): SyncResult {
        val initialAccess = sessionStore.accessToken() ?: return SyncResult.NotAuthenticated
        ensureAggregateBaselines()

        var access = initialAccess
        var cursor = database.syncStateDao().get()?.lastServerRevision ?: 0L
        var totalApplied = 0
        var totalConflicts = 0

        do {
            val pending = database.syncMetadataDao().getPendingMetadata() + database.syncMetadataDao().getPendingTombstones()
            val uniquePending = pending.distinctBy { it.entityType to it.entityId }
            val batch = uniquePending.take(MAX_CHANGES_PER_REQUEST)
            val changes = batch.map { metadata ->
                val encrypted = if (metadata.isDeleted) {
                    SyncKeyManager.EncryptedPayload(ByteArray(0), ByteArray(0))
                } else {
                    val plaintext = entityJson(metadata.entityType, metadata.entityId)
                        ?: error("Sync metadata points to a missing ${metadata.entityType}:${metadata.entityId}")
                    syncKeyManager.encrypt(plaintext.toString().toByteArray(StandardCharsets.UTF_8))
                }
                SyncApi.SyncChange(
                    entityType = metadata.entityType,
                    entityId = metadata.entityId,
                    ciphertext = encrypted.ciphertext,
                    nonce = encrypted.nonce,
                    version = metadata.version,
                    isDeleted = metadata.isDeleted,
                    updatedAt = metadata.updatedAt
                )
            }

            val requestId = UUID.randomUUID().toString()
            val deviceId = sessionStore.deviceId() ?: database.syncStateDao().get()?.deviceId
            ?: error("Device identity is not initialized.")
            val response = try {
                syncApi.sync(access, requestId, deviceId, cursor, changes)
            } catch (e: ApiException) {
                if (e.statusCode != 401) throw e
                val refresh = sessionStore.refreshToken()
                if (refresh.isNullOrBlank()) {
                    sessionStore.clearSession()
                    syncKeyManager.clear()
                    return SyncResult.ReauthenticationRequired
                }
                val refreshed = try {
                    authApi.refresh(refresh)
                } catch (refreshError: ApiException) {
                    sessionStore.clearSession()
                    syncKeyManager.clear()
                    return SyncResult.ReauthenticationRequired
                }
                sessionStore.save(
                    accessToken = refreshed.accessToken,
                    refreshToken = refreshed.refreshToken,
                    userId = sessionStore.userId().orEmpty(),
                    deviceId = sessionStore.deviceId() ?: deviceId,
                    kdfSalt = sessionStore.kdfSalt().orEmpty(),
                    passwordKeyEnvelope = sessionStore.passwordKeyEnvelope().orEmpty(),
                    passwordKeyNonce = sessionStore.passwordKeyNonce().orEmpty(),
                    recoveryKeyEnvelope = sessionStore.recoveryKeyEnvelope().orEmpty(),
                    recoveryKeyNonce = sessionStore.recoveryKeyNonce().orEmpty()
                )
                access = refreshed.accessToken
                syncApi.sync(access, requestId, deviceId, cursor, changes)
            }

            database.withTransaction {
                response.changes.forEach { applyServerChange(it, forceServerState = false) }

                // P1-1: server-wins is deterministic and terminates the conflict loop.
                response.conflicts.forEach { applyServerChange(it, forceServerState = true) }

                // Operation records can arrive before their parent entity because pull order
                // is by global server revision. Rebuild derived aggregates after the whole
                // batch so arrival order cannot corrupt balances.
                rebuildDerivedAggregates()

                val acknowledged = response.changes.associateBy { it.entityType to it.entityId }
                batch.forEach { metadata ->
                    // Only acknowledge a write when the backend returned the exact
                    // record and its exact serverRevision. Never use the global cursor
                    // as an entity revision.
                    val ack = acknowledged[metadata.entityType to metadata.entityId]
                    val current = database.syncMetadataDao().get(metadata.entityType, metadata.entityId)
                    if (ack != null && current != null && current.version == metadata.version) {
                        database.syncMetadataDao().markSynced(
                            entityType = metadata.entityType,
                            entityId = metadata.entityId,
                            version = metadata.version,
                            serverRevision = ack.serverRevision
                        )
                    }
                }

                val state = database.syncStateDao().get()
                    ?: SyncStateEntity(id = 1, deviceId = deviceId)
                database.syncStateDao().upsert(
                    state.copy(
                        lastServerRevision = response.nextCursor,
                        lastSuccessfulSyncAt = ApiTime.nowMillis(),
                        syncRequired = response.hasMore ||
                                database.syncMetadataDao().getPendingMetadata().isNotEmpty() ||
                                database.syncMetadataDao().getPendingTombstones().isNotEmpty()
                    )
                )
            }

            cursor = response.nextCursor
            database.syncMetadataDao().deleteSyncedTombstones(cursor)
            totalApplied += response.changes.size
            totalConflicts += response.conflicts.size
        } while (response.hasMore)

        return SyncResult.Success(totalApplied, totalConflicts, cursor)
    }

    /** Create a one-time baseline operation for legacy aggregate values. */
    private suspend fun ensureAggregateBaselines() {
        val now = ApiTime.nowMillis()
        database.withTransaction {
            database.savingGoalDao().getAllGoalsSnapshot().forEach { goal ->
                val goalMetadata = database.syncMetadataDao().get(SyncEntityType.SAVING_GOAL, goal.id)
                val neverSynced = goalMetadata == null || goalMetadata.serverRevision == 0L
                if (neverSynced && goal.currentAmount != 0L && database.savingGoalOperationDao().getBaseline(goal.id) == null) {
                    val baseline = SavingGoalOperationEntity(
                        goalId = goal.id,
                        deltaAmount = goal.currentAmount,
                        operationType = "BASELINE",
                        createdAt = now,
                        updatedAt = now
                    )
                    database.savingGoalOperationDao().insert(baseline)
                    recordMutationInTransaction(SyncEntityType.SAVING_GOAL_OPERATION, baseline.id, now, false)
                }
            }
            database.debtCreditDao().getAllSnapshot().forEach { debt ->
                val debtMetadata = database.syncMetadataDao().get(SyncEntityType.DEBT_CREDIT, debt.id)
                val neverSynced = debtMetadata == null || debtMetadata.serverRevision == 0L
                if (neverSynced && (debt.paidAmount != 0L || debt.isSettled) && database.debtPaymentDao().getBaseline(debt.id) == null) {
                    val baseline = DebtPaymentEntity(
                        debtCreditId = debt.id,
                        deltaAmount = debt.paidAmount,
                        isSettled = debt.isSettled,
                        operationType = "BASELINE",
                        createdAt = now,
                        updatedAt = now
                    )
                    database.debtPaymentDao().insert(baseline)
                    recordMutationInTransaction(SyncEntityType.DEBT_PAYMENT, baseline.id, now, false)
                }
            }
        }
    }

    private suspend fun recordMutationInTransaction(entityType: String, entityId: String, updatedAt: Long, isDeleted: Boolean) {
        val metadataDao = database.syncMetadataDao()
        val stateDao = database.syncStateDao()
        val existing = metadataDao.get(entityType, entityId)
        val state = stateDao.get() ?: SyncStateEntity(
            id = 1,
            deviceId = sessionStore.deviceId() ?: error("Device identity is not initialized.")
        ).also { stateDao.upsert(it) }
        metadataDao.upsert(
            SyncMetadataEntity(
                entityType = entityType,
                entityId = entityId,
                version = (existing?.version ?: 0) + 1,
                lastSyncedVersion = existing?.lastSyncedVersion ?: 0,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                deviceId = state.deviceId,
                serverRevision = existing?.serverRevision ?: 0L
            )
        )
        stateDao.markSyncRequired()
    }

    private suspend fun entityJson(entityType: String, id: String): JSONObject? = when (entityType) {
        SyncEntityType.TRANSACTION -> database.transactionDao().getById(id)?.let {
            JSONObject().put("id", it.id).put("title", it.title).put("amount", it.amount)
                .put("categoryId", it.categoryId).put("type", it.type).put("timestamp", it.timestamp)
                .put("note", it.note).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }
        SyncEntityType.CATEGORY -> database.categoryDao().getById(id)?.let {
            JSONObject().put("id", it.id).put("title", it.title).put("iconName", it.iconName)
                .put("iconEmoji", it.iconEmoji).put("isExpense", it.isExpense).put("isSystem", it.isSystem)
                .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }
        SyncEntityType.BUDGET_LIMIT -> database.budgetLimitDao().getById(id)?.let {
            JSONObject().put("id", it.id).put("categoryId", it.categoryId).put("maxLimit", it.maxLimit)
                .put("isActive", it.isActive).put("startDate", it.startDate).put("endDate", it.endDate)
                .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }
        SyncEntityType.DEBT_CREDIT -> database.debtCreditDao().getById(id)?.let {
            // paidAmount/isSettled are ledger-derived and intentionally excluded.
            JSONObject().put("id", it.id).put("type", it.type).put("personName", it.personName)
                .put("totalAmount", it.totalAmount).put("isMonthly", it.isMonthly)
                .put("monthlyAmount", it.monthlyAmount).put("dueDay", it.dueDay).put("dueDateMillis", it.dueDateMillis)
                .put("note", it.note ?: JSONObject.NULL).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }
        SyncEntityType.SAVING_GOAL -> database.savingGoalDao().getById(id)?.let {
            // currentAmount is ledger-derived and intentionally excluded.
            JSONObject().put("id", it.id).put("title", it.title).put("targetAmount", it.targetAmount)
                .put("monthlyAmount", it.monthlyAmount).put("icon", it.icon)
                .put("lastAutoDepositTimestamp", it.lastAutoDepositTimestamp).put("createdAt", it.createdAt)
                .put("updatedAt", it.updatedAt)
        }
        SyncEntityType.SAVING_GOAL_OPERATION -> database.savingGoalOperationDao().getById(id)?.let {
            JSONObject().put("id", it.id).put("goalId", it.goalId).put("deltaAmount", it.deltaAmount)
                .put("operationType", it.operationType).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }
        SyncEntityType.DEBT_PAYMENT -> database.debtPaymentDao().getById(id)?.let {
            JSONObject().put("id", it.id).put("debtCreditId", it.debtCreditId).put("deltaAmount", it.deltaAmount)
                .put("isSettled", it.isSettled).put("operationType", it.operationType)
                .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }
        else -> error("Unsupported sync entity type: $entityType")
    }

    private suspend fun applyServerChange(change: SyncApi.SyncChange, forceServerState: Boolean) {
        val metadata = database.syncMetadataDao().get(change.entityType, change.entityId)
        if (!forceServerState && metadata != null && metadata.version > change.version) return

        if (change.isDeleted) {
            deleteLocalEntity(change.entityType, change.entityId)
            database.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    entityType = change.entityType,
                    entityId = change.entityId,
                    version = change.version,
                    lastSyncedVersion = change.version,
                    updatedAt = change.updatedAt,
                    isDeleted = true,
                    deviceId = metadata?.deviceId.orEmpty(),
                    serverRevision = change.serverRevision
                )
            )
            return
        }

        // Operation records are immutable. A replay must never apply the delta twice.
        when (change.entityType) {
            SyncEntityType.SAVING_GOAL_OPERATION -> applyGoalOperation(change)
            SyncEntityType.DEBT_PAYMENT -> applyDebtPayment(change)
            else -> {
                val json = JSONObject(String(syncKeyManager.decrypt(change.ciphertext, change.nonce), StandardCharsets.UTF_8))
                upsertLocalEntity(change.entityType, json)
            }
        }

        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                entityType = change.entityType,
                entityId = change.entityId,
                version = change.version,
                lastSyncedVersion = change.version,
                updatedAt = change.updatedAt,
                isDeleted = false,
                deviceId = metadata?.deviceId.orEmpty(),
                serverRevision = change.serverRevision
            )
        )
    }

    private suspend fun applyGoalOperation(change: SyncApi.SyncChange) {
        if (database.savingGoalOperationDao().getById(change.entityId) != null) return
        val j = JSONObject(String(syncKeyManager.decrypt(change.ciphertext, change.nonce), StandardCharsets.UTF_8))
        val op = SavingGoalOperationEntity(
            id = j.getString("id"), goalId = j.getString("goalId"), deltaAmount = j.getLong("deltaAmount"),
            operationType = j.optString("operationType", "DELTA"), createdAt = j.getLong("createdAt"), updatedAt = j.getLong("updatedAt")
        )
        database.savingGoalOperationDao().insert(op)
        val goal = database.savingGoalDao().getById(op.goalId) ?: return
        val next = if (op.operationType == "BASELINE") op.deltaAmount.coerceAtLeast(0L)
        else (goal.currentAmount + op.deltaAmount).coerceAtLeast(0L)
        database.savingGoalDao().setCurrentAmount(op.goalId, next, op.updatedAt)
    }

    private suspend fun applyDebtPayment(change: SyncApi.SyncChange) {
        if (database.debtPaymentDao().getById(change.entityId) != null) return
        val j = JSONObject(String(syncKeyManager.decrypt(change.ciphertext, change.nonce), StandardCharsets.UTF_8))
        val payment = DebtPaymentEntity(
            id = j.getString("id"), debtCreditId = j.getString("debtCreditId"), deltaAmount = j.getLong("deltaAmount"),
            isSettled = j.optBoolean("isSettled", false), operationType = j.optString("operationType", "DELTA"),
            createdAt = j.getLong("createdAt"), updatedAt = j.getLong("updatedAt")
        )
        database.debtPaymentDao().insert(payment)
        val debt = database.debtCreditDao().getById(payment.debtCreditId) ?: return
        val next = if (payment.operationType == "BASELINE") payment.deltaAmount.coerceIn(0L, debt.totalAmount)
        else (debt.paidAmount + payment.deltaAmount).coerceIn(0L, debt.totalAmount)
        database.debtCreditDao().setPaidAggregate(payment.debtCreditId, next, payment.isSettled, payment.updatedAt)
    }

    private suspend fun rebuildDerivedAggregates() {
        database.savingGoalDao().getAllGoalsSnapshot().forEach { goal ->
            val operations = database.savingGoalOperationDao().getByGoalId(goal.id)
            val amount = operations.sumOf { it.deltaAmount }.coerceAtLeast(0L)
            database.savingGoalDao().setCurrentAmount(goal.id, amount, goal.updatedAt)
        }

        database.debtCreditDao().getAllSnapshot().forEach { debt ->
            val payments = database.debtPaymentDao().getByDebtCreditId(debt.id)
            val paid = payments.sumOf { it.deltaAmount }.coerceIn(0L, debt.totalAmount)
            val settled = payments.lastOrNull()?.isSettled ?: debt.isSettled
            database.debtCreditDao().setPaidAggregate(debt.id, paid, settled, debt.updatedAt)
        }
    }

    private suspend fun deleteLocalEntity(type: String, id: String) {
        when (type) {
            SyncEntityType.TRANSACTION -> database.transactionDao().deleteTransactionById(id)
            SyncEntityType.CATEGORY -> database.categoryDao().getById(id)?.let { database.categoryDao().delete(it) }
            SyncEntityType.BUDGET_LIMIT -> database.budgetLimitDao().deleteById(id)
            SyncEntityType.DEBT_CREDIT -> database.debtCreditDao().deleteById(id)
            SyncEntityType.SAVING_GOAL -> database.savingGoalDao().getById(id)?.let { database.savingGoalDao().deleteGoal(it) }
            SyncEntityType.SAVING_GOAL_OPERATION -> database.savingGoalOperationDao().deleteById(id)
            SyncEntityType.DEBT_PAYMENT -> database.debtPaymentDao().deleteById(id)
        }
    }

    private suspend fun upsertLocalEntity(type: String, j: JSONObject) {
        when (type) {
            SyncEntityType.TRANSACTION -> database.transactionDao().insertTransaction(
                TransactionEntity(j.getString("id"), j.optString("title"), j.getLong("amount"), j.optString("categoryId"), j.optString("type"), j.getLong("timestamp"), j.optString("note"), j.getLong("createdAt"), j.getLong("updatedAt"))
            )
            SyncEntityType.CATEGORY -> database.categoryDao().insert(
                CategoryEntity(j.getString("id"), j.optString("title"), j.optString("iconName"), j.optString("iconEmoji", "📁"), j.optBoolean("isExpense", true), j.optBoolean("isSystem", false), j.getLong("createdAt"), j.getLong("updatedAt"))
            )
            SyncEntityType.BUDGET_LIMIT -> database.budgetLimitDao().insertOrUpdate(
                BudgetLimitEntity(j.getString("id"), j.getString("categoryId"), j.getLong("maxLimit"), j.optBoolean("isActive", true), j.getLong("startDate"), j.getLong("endDate"), j.getLong("createdAt"), j.getLong("updatedAt"))
            )
            SyncEntityType.DEBT_CREDIT -> {
                val id = j.getString("id")
                val current = database.debtCreditDao().getById(id)
                database.debtCreditDao().insertOrUpdate(
                    DebtCreditEntity(
                        id = id,
                        type = j.getString("type"),
                        personName = j.getString("personName"),
                        totalAmount = j.getLong("totalAmount"),
                        paidAmount = current?.paidAmount ?: 0L,
                        isMonthly = j.optBoolean("isMonthly", false),
                        monthlyAmount = j.optLong("monthlyAmount", 0L),
                        dueDay = j.optInt("dueDay", 1),
                        dueDateMillis = j.optLong("dueDateMillis", 0L),
                        note = if (j.isNull("note")) null else j.optString("note"),
                        isSettled = current?.isSettled ?: false,
                        createdAt = j.getLong("createdAt"),
                        updatedAt = j.getLong("updatedAt")
                    )
                )
            }
            SyncEntityType.SAVING_GOAL -> {
                val id = j.getString("id")
                val current = database.savingGoalDao().getById(id)
                database.savingGoalDao().insertGoal(
                    SavingGoalEntity(
                        id = id,
                        title = j.getString("title"),
                        targetAmount = j.getLong("targetAmount"),
                        currentAmount = current?.currentAmount ?: 0L,
                        monthlyAmount = j.optLong("monthlyAmount", 0L),
                        icon = j.optString("icon", "🎯"),
                        lastAutoDepositTimestamp = j.optLong("lastAutoDepositTimestamp", 0L),
                        createdAt = j.getLong("createdAt"),
                        updatedAt = j.getLong("updatedAt")
                    )
                )
            }
        }
    }

    sealed interface SyncResult {
        object NotAuthenticated : SyncResult
        object ReauthenticationRequired : SyncResult
        data class Success(val applied: Int, val conflicts: Int, val cursor: Long) : SyncResult
    }

    companion object {
        private const val MAX_CHANGES_PER_REQUEST = 100
    }
}