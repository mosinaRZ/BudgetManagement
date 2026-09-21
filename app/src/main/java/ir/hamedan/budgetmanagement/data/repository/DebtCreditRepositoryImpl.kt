package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.DebtCreditDao
import ir.hamedan.budgetmanagement.data.local.dao.DebtPaymentDao
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtPaymentEntity
import ir.hamedan.budgetmanagement.data.sync.SyncEntityType
import kotlinx.coroutines.flow.Flow

class DebtCreditRepositoryImpl(
    private val debtCreditDao: DebtCreditDao,
    private val paymentDao: DebtPaymentDao,
    private val syncLocalDataSource: SyncLocalDataSource
) : DebtCreditRepository {
    override fun getAllDebtCredits(): Flow<List<DebtCreditEntity>> = debtCreditDao.getAllDebtCredits()

    override suspend fun insertOrUpdate(debtCredit: DebtCreditEntity) {
        val now = System.currentTimeMillis()
        val current = debtCreditDao.getById(debtCredit.id)
        val updated = debtCredit.copy(createdAt = if (debtCredit.createdAt > 0L) debtCredit.createdAt else now, updatedAt = now)
        syncLocalDataSource.transaction {
            debtCreditDao.insertOrUpdate(updated)
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_CREDIT, updated.id, now)
            if (current == null && (updated.paidAmount != 0L || updated.isSettled) && paymentDao.getBaseline(updated.id) == null) {
                val baseline = DebtPaymentEntity(
                    debtCreditId = updated.id,
                    deltaAmount = updated.paidAmount,
                    isSettled = updated.isSettled,
                    operationType = "BASELINE",
                    createdAt = now,
                    updatedAt = now
                )
                paymentDao.insert(baseline)
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_PAYMENT, baseline.id, now)
            } else if (current != null && (current.paidAmount != updated.paidAmount || current.isSettled != updated.isSettled)) {
                val payment = DebtPaymentEntity(
                    debtCreditId = updated.id,
                    deltaAmount = updated.paidAmount - current.paidAmount,
                    isSettled = updated.isSettled,
                    operationType = "DELTA",
                    createdAt = now,
                    updatedAt = now
                )
                paymentDao.insert(payment)
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_PAYMENT, payment.id, now)
            }
        }
    }

    override suspend fun update(debtCredit: DebtCreditEntity) {
        val now = System.currentTimeMillis()
        val current = debtCreditDao.getById(debtCredit.id)
        val updated = debtCredit.copy(updatedAt = now)
        syncLocalDataSource.transaction {
            debtCreditDao.update(updated)
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_CREDIT, updated.id, now)
            if (current != null && (current.paidAmount != updated.paidAmount || current.isSettled != updated.isSettled)) {
                val payment = DebtPaymentEntity(
                    debtCreditId = updated.id,
                    deltaAmount = updated.paidAmount - current.paidAmount,
                    isSettled = updated.isSettled,
                    createdAt = now,
                    updatedAt = now
                )
                paymentDao.insert(payment)
                syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_PAYMENT, payment.id, now)
            }
        }
    }

    override suspend fun deleteById(id: String) {
        val now = System.currentTimeMillis()
        syncLocalDataSource.transaction {
            val payments = paymentDao.getByDebtCreditId(id)
            debtCreditDao.deleteById(id)
            payments.forEach { payment ->
                paymentDao.deleteById(payment.id)
                syncLocalDataSource.recordMutationInTransaction(
                    SyncEntityType.DEBT_PAYMENT, payment.id, now, true
                )
            }
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_CREDIT, id, now, true)
        }
    }

    override suspend fun updateSettledStatus(id: String, isSettled: Boolean, paidAmount: Long) {
        require(paidAmount >= 0L) { "Paid amount cannot be negative." }
        val now = System.currentTimeMillis()
        syncLocalDataSource.transaction {
            val current = debtCreditDao.getById(id) ?: error("Debt/credit not found: $id")
            debtCreditDao.setPaidAggregate(id, paidAmount, isSettled, now)
            val payment = DebtPaymentEntity(
                debtCreditId = id,
                deltaAmount = paidAmount - current.paidAmount,
                isSettled = isSettled,
                createdAt = now,
                updatedAt = now
            )
            paymentDao.insert(payment)
            syncLocalDataSource.recordMutationInTransaction(SyncEntityType.DEBT_PAYMENT, payment.id, now)
        }
    }

    override suspend fun getById(id: String): DebtCreditEntity? = debtCreditDao.getById(id)
}