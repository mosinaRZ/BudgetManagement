package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import kotlinx.coroutines.flow.Flow

interface DebtCreditRepository {

    fun getAllDebtCredits(): Flow<List<DebtCreditEntity>>

    suspend fun insertOrUpdate(
        debtCredit: DebtCreditEntity
    )

    suspend fun update(
        debtCredit: DebtCreditEntity
    )

    suspend fun deleteById(
        id: String
    )

    suspend fun updateSettledStatus(
        id: String,
        isSettled: Boolean,
        paidAmount: Long
    )

    suspend fun getById(
        id: String
    ): DebtCreditEntity?
}