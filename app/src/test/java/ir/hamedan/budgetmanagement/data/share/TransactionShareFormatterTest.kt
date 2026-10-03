package ir.hamedan.budgetmanagement.data.share

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TransactionShareFormatterTest {

    private val utc: ZoneId = ZoneId.of("UTC")
    private val sampleMillis = ZonedDateTime.of(2026, 10, 3, 14, 30, 0, 0, utc).toInstant().toEpochMilli()

    @Test
    fun formatAmount_expenseInToman_hasMinusSignAndLeftToRightMark() {
        val text = TransactionShareFormatter.formatAmount(1_500_000L, "IRT", isExpense = true, isPersian = false)
        assertThat(text).isEqualTo("\u200E-1,500,000")
    }

    @Test
    fun formatAmount_incomeInRial_convertsFromTomanAndUsesPlusSign() {
        val text = TransactionShareFormatter.formatAmount(150_000L, "IRR", isExpense = false, isPersian = false)
        assertThat(text).isEqualTo("\u200E+1,500,000")
    }

    @Test
    fun currencyLabel_followsLanguageAndUnit() {
        assertThat(TransactionShareFormatter.currencyLabel("IRT", true)).isEqualTo("تومان")
        assertThat(TransactionShareFormatter.currencyLabel("IRR", true)).isEqualTo("ریال")
        assertThat(TransactionShareFormatter.currencyLabel("IRT", false)).isEqualTo("Toman")
        assertThat(TransactionShareFormatter.currencyLabel("IRR", false)).isEqualTo("Rial")
    }

    @Test
    fun typeLabel_followsLanguage() {
        assertThat(TransactionShareFormatter.typeLabel(true, true)).isEqualTo("هزینه")
        assertThat(TransactionShareFormatter.typeLabel(false, true)).isEqualTo("درآمد")
        assertThat(TransactionShareFormatter.typeLabel(true, false)).isEqualTo("Expense")
        assertThat(TransactionShareFormatter.typeLabel(false, false)).isEqualTo("Income")
    }

    @Test
    fun formatDateTime_english_usesGregorianCalendar() {
        assertThat(TransactionShareFormatter.formatDateTime(sampleMillis, false, utc))
            .isEqualTo("Oct 3, 2026 • 14:30")
    }

    @Test
    fun formatDateTime_persian_usesJalaliCalendarAndPersianDigits() {
        assertThat(TransactionShareFormatter.formatDateTime(sampleMillis, true, utc))
            .isEqualTo("۱۱ مهر ۱۴۰۵ • ۱۴:۳۰")
    }

    @Test
    fun toPersianDigits_onlyConvertsAsciiDigits() {
        assertThat(TransactionShareFormatter.toPersianDigits("1404/07/11 ab 09:05"))
            .isEqualTo("۱۴۰۴/۰۷/۱۱ ab ۰۹:۰۵")
    }

    @Test
    fun referenceId_isEightUppercaseAlphanumerics() {
        assertThat(TransactionShareFormatter.referenceId("3f9a-12bc-dd45-0001")).isEqualTo("3F9A12BC")
        assertThat(TransactionShareFormatter.referenceId("")).isEmpty()
    }

    @Test
    fun build_blankTitleFallsBackToCategory_andTrimsNote() {
        val tx = TransactionEntity(
            id = "abcd1234-0000",
            title = "   ",
            amount = 2_000L,
            categoryId = "food",
            type = "INCOME",
            timestamp = sampleMillis,
            note = "  lunch with team  "
        )

        val content = TransactionShareFormatter.build(
            transaction = tx,
            categoryTitle = "Food",
            categoryEmoji = "",
            currencyUnit = "IRT",
            isPersian = false,
            zone = utc
        )

        assertThat(content.title).isEqualTo("Food")
        assertThat(content.note).isEqualTo("lunch with team")
        assertThat(content.categoryEmoji).isEqualTo("📁")
        assertThat(content.isExpense).isFalse()
        assertThat(content.amountText).isEqualTo("\u200E+2,000")
        assertThat(content.referenceId).isEqualTo("ABCD1234")
    }
}