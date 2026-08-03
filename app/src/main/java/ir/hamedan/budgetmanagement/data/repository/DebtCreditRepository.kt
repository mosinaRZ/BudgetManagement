package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import kotlinx.coroutines.flow.Flow

interface DebtCreditRepository {
    val allDebtCredits: Flow<List<DebtCreditEntity>>
    suspend fun insertOrUpdateDebtCredit(debtCredit: DebtCreditEntity)
    suspend fun deleteDebtCredit(id: String)
    suspend fun toggleSettledStatus(id: String, currentStatus: Boolean)
}