package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    // -------------------------------------------------------------------------
    // Bank detection
    // -------------------------------------------------------------------------

    @Test
    fun isLikelyBankSms_requiresAtLeastTwoIndicators() {
        assertFalse(SmsParser.isLikelyBankSms("سلام خوبی؟"))
        assertFalse(SmsParser.isLikelyBankSms("خرید انجام شد")) // only one weak signal
        assertTrue(
            SmsParser.isLikelyBankSms(
                "برداشت از کارت 1234 مبلغ 500,000 ریال مانده 1,200,000"
            )
        )
    }

    @Test
    fun isLikelyBankSms_falseForOtpStyleMessage() {
        assertFalse(SmsParser.isLikelyBankSms("کد تایید ورود شما 847291 است"))
    }

    // -------------------------------------------------------------------------
    // Expense / income + Rial→Toman
    // -------------------------------------------------------------------------

    @Test
    fun parse_detectsExpenseAmountNearKeyword_rialConvertedToToman() {
        val body = "برداشت مبلغ 1,500,000 ریال از حساب شما. مانده 10,000,000"
        val result = SmsParser.parse(body, timestamp = 1_700_000_000_000L)

        assertTrue(result.isAmountDetected)
        assertEquals("EXPENSE", result.type)
        assertTrue(result.isTypeDetected)
        assertEquals(150_000.0, result.amount, 0.01) // /10
        assertEquals(1_700_000_000_000L, result.timestamp)
    }

    @Test
    fun parse_detectsIncome_rialConvertedToToman() {
        val body = "واریز مبلغ 2,000,000 ریال به حساب. مانده 5,000,000 ریال"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals("INCOME", result.type)
        assertEquals(200_000.0, result.amount, 0.01)
    }

    @Test
    fun parse_tomanOnly_doesNotDivideByTen() {
        val body = "برداشت مبلغ 150,000 تومان از کارت. مانده 1,000,000 تومان"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals(150_000.0, result.amount, 0.01)
    }

    @Test
    fun parse_bothRialAndTomanPresent_doesNotDivideByTen() {
        // طبق منطق فعلی: فقط وقتی «ریال» هست و «تومان» نیست تقسیم می‌شود
        val body = "برداشت مبلغ 150,000 ریال معادل تومان از حساب. مانده 500,000"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals(150_000.0, result.amount, 0.01)
    }

    // -------------------------------------------------------------------------
    // Rejection / noise
    // -------------------------------------------------------------------------

    @Test
    fun parse_ignoresNonBankMessage() {
        val result = SmsParser.parse("کد تایید شما 12345 است")
        assertFalse(result.isAmountDetected)
        assertEquals(0.0, result.amount, 0.0)
    }

    @Test
    fun parse_emptyBody_notDetected() {
        val result = SmsParser.parse("")
        assertFalse(result.isAmountDetected)
        assertEquals(0.0, result.amount, 0.0)
    }

    // -------------------------------------------------------------------------
    // Digits / separators
    // -------------------------------------------------------------------------

    @Test
    fun parse_convertsPersianDigitsAndThousandSeparator() {
        val body = "برداشت مبلغ ۱٬۲۵۰٬۰۰۰ ریال از کارت. مانده موجودی ۲٬۰۰۰٬۰۰۰"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals(125_000.0, result.amount, 0.01)
    }

    @Test
    fun parse_prefersAmountNearKeyword_overBalanceNumber() {
        // مبلغ تراکنش کوچک‌تر از مانده است؛ نباید مانده انتخاب شود
        val body = "خرید مبلغ 80,000 ریال از فروشگاه. مانده کارت 5,500,000 ریال حساب"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals(8_000.0, result.amount, 0.01) // 80000/10
    }

    // -------------------------------------------------------------------------
    // Type conflicts & keywords
    // -------------------------------------------------------------------------

    @Test
    fun parse_bothIncomeAndExpenseKeywords_defaultsToExpense() {
        val body = "انتقال به حساب و واریز و برداشت مبلغ 100,000 ریال مانده 200,000 کارت"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals("EXPENSE", result.type)
        assertTrue(result.isTypeDetected)
    }

    @Test
    fun parse_transferKeyword_treatedAsExpense() {
        val body = "انتقال به شماره کارت مبلغ 250,000 ریال. مانده حساب 1,000,000"
        val result = SmsParser.parse(body)

        assertTrue(result.isAmountDetected)
        assertEquals("EXPENSE", result.type)
    }

    // -------------------------------------------------------------------------
    // 10/12 digit filter (fallback path)
    // -------------------------------------------------------------------------

    @Test
    fun parse_fallbackIgnoresTenDigitPhoneLikeNumber() {
        // بدون «مبلغ» کنار کلیدواژه تا fallback درگیر شود؛
        // عدد ۱۰ رقمی شبیه موبایل نباید amount شود
        val body = "برداشت از کارت انجام شد 09121234567 مانده حساب 500000 ریال"
        val result = SmsParser.parse(body)

        // اگر amount تشخیص داده شود باید مانده/مبلغ منطقی باشد نه شماره موبایل
        if (result.isAmountDetected) {
            val digits = result.amount.toLong().toString().replace(".", "")
            // بعد از /10 ممکن است طول عوض شود؛ اصل: برابر کل شماره ۱۰ رقمی خام نباشد
            assertFalse(result.amount == 9121234567.0 || result.amount == 912123456.7)
        }
    }

    // -------------------------------------------------------------------------
    // Category suggestion
    // -------------------------------------------------------------------------

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

    @Test
    fun suggestCategory_isCaseInsensitive() {
        val categories = listOf(
            CategoryEntity(title = "FOOD", iconEmoji = "🍕", isExpense = true)
        )
        assertEquals("FOOD", SmsParser.suggestCategory("paid at food store کارت مانده", categories))
    }
}