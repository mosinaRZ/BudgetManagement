package ir.hamedan.budgetmanagement.domain.usecase

import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.money.MoneyContract
import ir.hamedan.budgetmanagement.data.repository.CategoryRepository
import ir.hamedan.budgetmanagement.data.repository.DebtCreditRepository
import ir.hamedan.budgetmanagement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID
import kotlin.math.ceil

class DebtCreditUseCase(
    private val debtCreditRepository: DebtCreditRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun saveOrUpdate(id: String?, type: String, personName: String, totalAmount: Double, isMonthly: Boolean, monthlyAmount: Double, dueDay: Int, oneTimeDueDateMillis: Long, note: String?, addToBalance: Boolean): DebtCreditEntity {
        require(type == "DEBT" || type == "CREDIT") { "Invalid debt/credit type." }
        require(personName.isNotBlank()) { "Person name is required." }
        require(totalAmount > 0.0) { "Total amount must be greater than zero." }
        if (isMonthly) require(monthlyAmount > 0.0) { "Monthly amount must be greater than zero." }
        val existing = id?.let { debtCreditRepository.getById(it) }
        val dueDate = if (isMonthly) calculateExpirationDate(totalAmount, monthlyAmount, dueDay) else oneTimeDueDateMillis
        val category = categoryRepository.getAllCategories().first().firstOrNull { it.title == if (type == "DEBT") "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }
            ?: error("Required debt/credit category is missing.")
        val entity = DebtCreditEntity(
            id = id ?: UUID.randomUUID().toString(), type = type, personName = personName.trim(),
            totalAmount = MoneyContract.fromInput(totalAmount), paidAmount = existing?.paidAmount ?: 0L,
            isMonthly = isMonthly, monthlyAmount = if (isMonthly) MoneyContract.fromInput(monthlyAmount) else 0L,
            dueDay = dueDay, dueDateMillis = dueDate, note = note, isSettled = existing?.isSettled ?: false
        )
        if (!id.isNullOrBlank() || !addToBalance) {
            debtCreditRepository.insertOrUpdate(entity)
            return entity
        }
        debtCreditRepository.insertOrUpdate(entity)
        val isDebt = type == "DEBT"
        transactionRepository.insertTransaction(TransactionEntity(title = if (isDebt) "دریافت وام/بدهی: ${personName.trim()}" else "پرداخت وام/طلب: ${personName.trim()}", amount = entity.totalAmount, categoryId = category.id, type = if (isDebt) "INCOME" else "EXPENSE", note = note ?: "ثبت اولیه ${personName.trim()}"))
        return entity
    }

    suspend fun deposit(item: DebtCreditEntity, amount: Long) {
        require(amount > 0L) { "Payment amount must be greater than zero." }
        val newPaid = (item.paidAmount + amount).coerceAtMost(item.totalAmount)
        debtCreditRepository.insertOrUpdate(item.copy(paidAmount = newPaid, isSettled = newPaid >= item.totalAmount))
        insertPaymentTransaction(item, amount, reverse = false)
    }

    suspend fun withdraw(item: DebtCreditEntity, amount: Long) {
        require(amount > 0L) { "Adjustment amount must be greater than zero." }
        val newPaid = (item.paidAmount - amount).coerceAtLeast(0L)
        debtCreditRepository.insertOrUpdate(item.copy(paidAmount = newPaid, isSettled = newPaid >= item.totalAmount))
        insertPaymentTransaction(item, amount, reverse = true)
    }

    suspend fun toggleSettled(item: DebtCreditEntity, target: Boolean) {
        debtCreditRepository.insertOrUpdate(item.copy(isSettled = target, paidAmount = if (target) item.totalAmount else 0L))
    }

    suspend fun updateDueDate(item: DebtCreditEntity, newDueDateMillis: Long) {
        debtCreditRepository.insertOrUpdate(item.copy(dueDateMillis = newDueDateMillis))
    }

    suspend fun delete(item: DebtCreditEntity) = debtCreditRepository.deleteById(item.id)
    suspend fun restore(item: DebtCreditEntity) = debtCreditRepository.insertOrUpdate(item)

    private suspend fun insertPaymentTransaction(item: DebtCreditEntity, amount: Long, reverse: Boolean) {
        val isDebt = item.type == "DEBT"
        val category = categoryRepository.getAllCategories().first().firstOrNull { it.title == if (isDebt) "DEBT_CREDIT_PAYABLE" else "DEBT_CREDIT_RECEIVABLE" }
            ?: error("Required debt/credit category is missing.")
        val type = when {
            isDebt && !reverse -> "EXPENSE"
            isDebt -> "INCOME"
            !isDebt && !reverse -> "INCOME"
            else -> "EXPENSE"
        }
        transactionRepository.insertTransaction(TransactionEntity(title = if (!reverse) { if (isDebt) "پرداخت بدهی به: ${item.personName}" else "دریافت طلب از: ${item.personName}" } else { if (isDebt) "اصلاح/برداشت از پرداخت بدهی: ${item.personName}" else "اصلاح/برداشت از دریافت طلب: ${item.personName}" }, amount = amount, categoryId = category.id, type = type, note = "ثبت و اصلاح واریزی بدهی و طلب"))
    }

    private fun calculateExpirationDate(totalAmount: Double, monthlyAmount: Double, dueDay: Int): Long {
        val monthsRequired = ceil(totalAmount / monthlyAmount).toInt().coerceAtLeast(1)
        return Calendar.getInstance().apply {
            if (get(Calendar.DAY_OF_MONTH) > dueDay) add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            add(Calendar.MONTH, monthsRequired - 1)
        }.timeInMillis
    }
}