package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun isLikelyBankSms_requiresAtLeastTwoIndicators() {
        assertFalse(SmsParser.isLikelyBankSms("سلام خوبی؟"))
        assertFalse(SmsParser.isLikelyBankSms("خرید انجام شد"))
        assertTrue(
            SmsParser.isLikelyBankSms(
                "برداشت از کارت 1234 مبلغ 500,000 ریال مانده 1,200,000"
            )
        )
    }

    @Test
    fun parse_detectsExpenseAmountNearKeyword() {
        val body = "برداشت مبلغ 1,500,000 ریال از حساب شما. مانده 10,000,000"
        val result = SmsParser.parse(body, timestamp = 1_700_000_000_000L)

        assertTrue(result.isAmountDetected)
        assertEquals("EXPENSE", result.type)
        assertTrue(result.isTypeDetected)
        assertEquals(150_000.0, result.amount, 0.01)
        assertEquals(1_700_000_000_000L, result.timestamp)
    }

    @Test
    fun parse_detectsIncome() {
        val body = "واریز مبلغ 2,000,000 ریال به حساب. مانده 5,000,000 ریال"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals("INCOME", result.type)
        assertTrue(result.isTypeDetected)
        assertEquals(200_000.0, result.amount, 0.01)
    }

    @Test
    fun parse_ignoresNonBankMessage() {
        val result = SmsParser.parse("کد تایید شما 12345 است")
        assertFalse(result.isAmountDetected)
        assertEquals(0.0, result.amount, 0.0)
    }

    @Test
    fun parse_convertsPersianDigits() {
        val body = "برداشت مبلغ ۱٬۲۵۰٬۰۰۰ ریال از کارت. مانده موجودی ۲٬۰۰۰٬۰۰۰"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals(125_000.0, result.amount, 0.01)
    }

    @Test
    fun suggestCategory_matchesTitleInBody() {
        val categories = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true),
            CategoryEntity(title = "TRANSPORT", iconEmoji = "🚗", isExpense = true)
        )
        val full = "خرید از فروشگاه FOOD مبلغ 100000 ریال مانده 500000 کارت حساب"
        assertEquals("FOOD", SmsParser.suggestCategory(full, categories))
    }

    @Test
    fun suggestCategory_returnsEmptyWhenNoMatch() {
        val categories = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true)
        )
        assertEquals("", SmsParser.suggestCategory("برداشت کارت مانده", categories))
    }
}