package ir.hamedan.budgetmanagement.data.backup

import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtPaymentEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalOperationEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Everything a backup carries: the user's data, not notifications, SMS queue or sync state. */
data class BackupContent(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgetLimits: List<BudgetLimitEntity>,
    val debtCredits: List<DebtCreditEntity>,
    val debtPayments: List<DebtPaymentEntity>,
    val savingGoals: List<SavingGoalEntity>,
    val savingGoalOperations: List<SavingGoalOperationEntity>
) {
    val totalRecords: Int
        get() = categories.size + transactions.size + budgetLimits.size + debtCredits.size +
                debtPayments.size + savingGoals.size + savingGoalOperations.size
}

/** JSON mapping of [BackupContent]. Independent of the sync wire format on purpose. */
object BackupCodec {
    const val FORMAT = "cidna-backup"
    const val VERSION = 1

    fun encode(content: BackupContent, createdAt: Long): ByteArray {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAt", createdAt)
            .put("categories", array(content.categories) {
                JSONObject().put("id", it.id).put("title", it.title).put("iconName", it.iconName)
                    .put("iconEmoji", it.iconEmoji).put("isExpense", it.isExpense).put("isSystem", it.isSystem)
                    .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
            })
            .put("transactions", array(content.transactions) {
                JSONObject().put("id", it.id).put("title", it.title).put("amount", it.amount)
                    .put("categoryId", it.categoryId).put("type", it.type).put("timestamp", it.timestamp)
                    .put("note", it.note).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
            })
            .put("budgetLimits", array(content.budgetLimits) {
                JSONObject().put("id", it.id).put("categoryId", it.categoryId).put("maxLimit", it.maxLimit)
                    .put("isActive", it.isActive).put("startDate", it.startDate).put("endDate", it.endDate)
                    .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
            })
            .put("debtCredits", array(content.debtCredits) {
                JSONObject().put("id", it.id).put("type", it.type).put("personName", it.personName)
                    .put("totalAmount", it.totalAmount).put("paidAmount", it.paidAmount)
                    .put("isMonthly", it.isMonthly).put("monthlyAmount", it.monthlyAmount)
                    .put("dueDay", it.dueDay).put("dueDateMillis", it.dueDateMillis)
                    .put("note", it.note ?: JSONObject.NULL).put("isSettled", it.isSettled)
                    .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
            })
            .put("debtPayments", array(content.debtPayments) {
                JSONObject().put("id", it.id).put("debtCreditId", it.debtCreditId)
                    .put("deltaAmount", it.deltaAmount).put("isSettled", it.isSettled)
                    .put("operationType", it.operationType).put("createdAt", it.createdAt)
                    .put("updatedAt", it.updatedAt)
            })
            .put("savingGoals", array(content.savingGoals) {
                JSONObject().put("id", it.id).put("title", it.title).put("targetAmount", it.targetAmount)
                    .put("currentAmount", it.currentAmount).put("monthlyAmount", it.monthlyAmount)
                    .put("icon", it.icon).put("lastAutoDepositTimestamp", it.lastAutoDepositTimestamp)
                    .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
            })
            .put("savingGoalOperations", array(content.savingGoalOperations) {
                JSONObject().put("id", it.id).put("goalId", it.goalId).put("deltaAmount", it.deltaAmount)
                    .put("operationType", it.operationType).put("createdAt", it.createdAt)
                    .put("updatedAt", it.updatedAt)
            })
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    /** @throws BackupFormatException when the content is not a valid backup of a supported version. */
    fun decode(bytes: ByteArray): BackupContent {
        try {
            val root = JSONObject(String(bytes, Charsets.UTF_8))
            if (root.optString("format") != FORMAT) throw BackupFormatException("Not a Cidna backup file")
            val version = root.optInt("version", -1)
            if (version != VERSION) throw BackupFormatException("Unsupported backup version: $version")
            return BackupContent(
                categories = list(root, "categories") {
                    CategoryEntity(
                        id = it.getString("id"), title = it.getString("title"),
                        iconName = it.optString("iconName", ""), iconEmoji = it.optString("iconEmoji", "📁"),
                        isExpense = it.optBoolean("isExpense", true), isSystem = it.optBoolean("isSystem", false),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                },
                transactions = list(root, "transactions") {
                    TransactionEntity(
                        id = it.getString("id"), title = it.optString("title", ""), amount = it.getLong("amount"),
                        categoryId = it.optString("categoryId", ""), type = it.getString("type"),
                        timestamp = it.getLong("timestamp"), note = it.optString("note", ""),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                },
                budgetLimits = list(root, "budgetLimits") {
                    BudgetLimitEntity(
                        id = it.getString("id"), categoryId = it.getString("categoryId"),
                        maxLimit = it.getLong("maxLimit"), isActive = it.optBoolean("isActive", true),
                        startDate = it.getLong("startDate"), endDate = it.getLong("endDate"),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                },
                debtCredits = list(root, "debtCredits") {
                    DebtCreditEntity(
                        id = it.getString("id"), type = it.getString("type"), personName = it.getString("personName"),
                        totalAmount = it.getLong("totalAmount"), paidAmount = it.optLong("paidAmount", 0L),
                        isMonthly = it.optBoolean("isMonthly", false), monthlyAmount = it.optLong("monthlyAmount", 0L),
                        dueDay = it.optInt("dueDay", 1), dueDateMillis = it.optLong("dueDateMillis", 0L),
                        note = if (it.isNull("note")) null else it.optString("note"),
                        isSettled = it.optBoolean("isSettled", false),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                },
                debtPayments = list(root, "debtPayments") {
                    DebtPaymentEntity(
                        id = it.getString("id"), debtCreditId = it.getString("debtCreditId"),
                        deltaAmount = it.getLong("deltaAmount"), isSettled = it.optBoolean("isSettled", false),
                        operationType = it.optString("operationType", "DELTA"),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                },
                savingGoals = list(root, "savingGoals") {
                    SavingGoalEntity(
                        id = it.getString("id"), title = it.getString("title"),
                        targetAmount = it.getLong("targetAmount"), currentAmount = it.optLong("currentAmount", 0L),
                        monthlyAmount = it.optLong("monthlyAmount", 0L), icon = it.optString("icon", "🎯"),
                        lastAutoDepositTimestamp = it.optLong("lastAutoDepositTimestamp", 0L),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                },
                savingGoalOperations = list(root, "savingGoalOperations") {
                    SavingGoalOperationEntity(
                        id = it.getString("id"), goalId = it.getString("goalId"),
                        deltaAmount = it.getLong("deltaAmount"),
                        operationType = it.optString("operationType", "DELTA"),
                        createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt")
                    )
                }
            )
        } catch (e: JSONException) {
            throw BackupFormatException("Backup content is damaged: ${e.message}")
        }
    }

    private fun <T> array(items: List<T>, mapper: (T) -> JSONObject): JSONArray =
        JSONArray().also { array -> items.forEach { array.put(mapper(it)) } }

    private fun <T> list(root: JSONObject, name: String, mapper: (JSONObject) -> T): List<T> {
        val array = root.optJSONArray(name) ?: return emptyList()
        return List(array.length()) { mapper(array.getJSONObject(it)) }
    }
}