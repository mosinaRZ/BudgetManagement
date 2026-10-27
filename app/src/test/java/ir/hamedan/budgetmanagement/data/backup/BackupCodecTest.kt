package ir.hamedan.budgetmanagement.data.backup

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.BudgetLimitEntity
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtCreditEntity
import ir.hamedan.budgetmanagement.data.local.models.DebtPaymentEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalOperationEntity
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import android.app.Application
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BackupCodecTest {
    private val content = BackupContent(
        categories = listOf(CategoryEntity("c1", "غذا", "food", "🍕", true, false, 1L, 2L)),
        transactions = listOf(TransactionEntity("t1", "ناهار", 150_000L, "c1", "EXPENSE", 10L, "یادداشت \"x\"\n", 3L, 4L)),
        budgetLimits = listOf(BudgetLimitEntity("b1", "c1", 5_000_000L, true, 1L, 99L, 5L, 6L)),
        debtCredits = listOf(
            DebtCreditEntity("d1", "DEBT", "علی", 1_000L, 250L, true, 100L, 5, 77L, null, false, 7L, 8L),
            DebtCreditEntity("d2", "CREDIT", "Sara", 2_000L, 0L, false, 0L, 1, 0L, "note", true, 9L, 10L)
        ),
        debtPayments = listOf(DebtPaymentEntity("p1", "d1", 250L, false, "DELTA", 11L, 12L)),
        savingGoals = listOf(SavingGoalEntity("g1", "سفر", 9_000L, 1_500L, 100L, "✈️", 13L, 14L, 15L)),
        savingGoalOperations = listOf(SavingGoalOperationEntity("o1", "g1", 1_500L, "BASELINE", 16L, 17L))
    )

    @Test
    fun everyFieldSurvivesARoundTrip() {
        val decoded = BackupCodec.decode(BackupCodec.encode(content, createdAt = 123L))
        assertThat(decoded).isEqualTo(content)
        assertThat(decoded.totalRecords).isEqualTo(8)
    }

    @Test
    fun otherDocumentsAndVersionsAreRejected() {
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("{}".toByteArray()) }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("not json".toByteArray()) }
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode("""{"format":"cidna-backup","version":2}""".toByteArray())
        }
    }

    @Test
    fun aRecordMissingARequiredFieldRejectsTheWholeBackup() {
        val broken = """{"format":"cidna-backup","version":1,"transactions":[{"id":"t1"}]}"""
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(broken.toByteArray()) }
    }
}