package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.DebtCreditDao
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import kotlinx.coroutines.flow.Flow

class DebtCreditRepositoryImpl(
    private val dao: DebtCreditDao
) : DebtCreditRepository {

    override val allDebtCredits: Flow<List<DebtCreditEntity>> = dao.getAllDebtCredits()

    override suspend fun insertOrUpdateDebtCredit(debtCredit: DebtCreditEntity) {
        dao.insertOrUpdate(debtCredit)
    }

    override suspend fun deleteDebtCredit(id: String) {
        dao.deleteById(id)
    }

    override suspend fun toggleSettledStatus(id: String, currentStatus: Boolean) {
        dao.updateSettledStatus(id, !currentStatus)
    }
}