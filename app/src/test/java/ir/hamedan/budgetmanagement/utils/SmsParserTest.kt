package ir.hamedan.budgetmanagement.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression suite for [SmsParser]. Every time a real bank SMS is mis-detected
 * (or a promo slips through), add it here as a new case, then fix the parser.
 * Place under app/src/test/java/ir/hamedan/budgetmanagement/utils/
 */
class SmsParserTest {

    private fun accepts(body: String, amountToman: Double, type: String, sender: String? = null) {
        val r = SmsParser.parse(body, 0L, sender)
        assertTrue("should be accepted: $body (reason=${r.rejectReason})", r.isAmountDetected)
        assertEquals(amountToman, r.amount, 0.001)
        assertEquals(type, r.type)
        assertTrue(r.isTypeDetected)
    }

    private fun rejects(body: String, sender: String? = null) {
        val r = SmsParser.parse(body, 0L, sender)
        assertFalse("should be rejected: $body", r.isAmountDetected)
        assertFalse(SmsParser.isLikelyBankSms(body, sender))
    }

    // ------------------------------ real transactions ------------------------------

    @Test fun tejaratWithdraw() = accepts(
        "*بانک تجارت* حساب: ******** برداشت: 720,000 ریال از طريق: شتاب مانده: 31,401,793 ریال 1403/09/29 23:05",
        72000.0, "EXPENSE"
    )

    @Test fun signedSuffixWithdraw() = accepts(
        "بانک ملت\nبرداشت:500,000-\nمانده:1,200,000\n1403/05/01_12:30", 50000.0, "EXPENSE"
    )

    @Test fun signedPrefixDeposit() = accepts(
        "بانک ملی\nحساب 0123456789\n+1,000,000\nمانده: 3,500,000\n1403/06/01 10:15", 100000.0, "INCOME"
    )

    @Test fun persianDigitsDeposit() = accepts(
        "واریز ۲,۵۰۰,۰۰۰ ریال به کارت ****۱۲۳۴\nمانده ۵,۰۰۰,۰۰۰ ریال\nبانک پاسارگاد", 250000.0, "INCOME"
    )

    @Test fun labelOnPreviousLine() = accepts(
        "بانک سپه\nکارت ****1234\nخرید\n250,000 ریال\nمانده 3,000,000", 25000.0, "EXPENSE"
    )

    @Test fun tomanUnitIsKept() = accepts(
        "بانک سامان\nبرداشت 45,000 تومان\nمانده 1,200,000 تومان", 45000.0, "EXPENSE"
    )

    @Test fun amountBeforeVerb() = accepts(
        "بانک رفاه\n2,000,000 ریال به حساب شما واریز شد\nمانده: 4,000,000 ریال", 200000.0, "INCOME"
    )

    @Test fun balanceBeforeAmount() = accepts(
        "بانک صادرات\nمانده: 2,000,000\nبرداشت: 500,000\n1403/05/01", 50000.0, "EXPENSE"
    )

    @Test fun trackingCodeIsNotAmount() = accepts(
        "بانک آینده\nمبلغ 350,000 ریال برداشت شد\nکد رهگیری 123456789012\nمانده 1,000,000 ریال", 35000.0, "EXPENSE"
    )

    @Test fun arabicLettersAndBidiMarks() = accepts(
        "\u200Fبانك تجارت\nبرداشت: 720,000 ريال\nمانده: 31,401,793 ريال\n1403/09/29", 72000.0, "EXPENSE"
    )

    @Test fun genuineSmsWithLinkIsKeptWhenFullyStructured() = accepts(
        "بانک ملت\nبرداشت:500,000-\nمانده:1,200,000\njozeiat: bank.ir/x", 50000.0, "EXPENSE", sender = "30001234"
    )

    @Test fun loanInstallmentDeduction() = accepts(
        "بانک ملی\nکسر قسط وام 3,500,000 ریال\nمانده 8,000,000 ریال\n1403/05/01", 350000.0, "EXPENSE"
    )

    @Test fun interestDeposit() = accepts(
        "بانک سامان\nواریز سود 1,250,000 ریال\nمانده 100,000,000 ریال", 125000.0, "INCOME"
    )

    // ------------------------------ Blu (neo-bank) ------------------------------

    @Test fun bluTransferOut() = accepts(
        "بلو\nانتقال پل\nمحمدسینا عزیز، 25,000,000 ریال از حساب شما پرید.\nموجودی: 66,144,957 ریال\n۲۱:۴۱\n۱۴۰۵.۰۷.۱۵",
        2500000.0, "EXPENSE", sender = "Blu"
    )

    @Test fun bluDepositWithoutSpaceBeforeUnit() = accepts(
        "بلو\nواریز پول\nمحمدسینا عزیز، 60,000,000ریال به حساب شما نشست.\nموجودی: 91,152,957 ریال\n۱۴:۲۰\n۱۴۰۵.۰۷.۱۵",
        6000000.0, "INCOME", sender = "Blu"
    )

    @Test fun bluWithoutHeaderLines() = accepts(
        "علی عزیز، ۲۵٬۰۰۰٬۰۰۰ ریال از حساب شما پرید. موجودی: ۶۶٬۱۴۴٬۹۵۷ ریال",
        2500000.0, "EXPENSE"
    )

    @Test fun bluDepositOnlyBySenderEvidence() = accepts(
        "علی عزیز، 60,000,000ریال به حساب شما نشست.", 6000000.0, "INCOME", sender = "BluBank"
    )

    // ------------------------------ must be ignored ------------------------------

    @Test fun lotteryPromo() = rejects(
        "مشتری گرامی؛ برای شرکت در قرعه کشی بانک ملت نسبت به افزایش موجودی حساب قرض الحسنه خود تا 31 مردادماه به مبلغ حداقل 1,000,000 ریال اقدام فرمایید. هر یک میلیون ریال در هر روز یک امتیاز"
    )

    @Test fun discountPromoWithLink() = rejects(
        "بانک ملی: با خرید از فروشگاه ما تا 30% تخفیف ویژه! همین حالا کلیک کنید https://shop.example.ir/x لغو 11"
    )

    @Test fun loanOfferPromo() = rejects(
        "بانک ملت: وام 100,000,000 تومانی با شرایط ویژه برای شما آماده است. مراجعه به شعبه. لغو 11"
    )

    @Test fun otpMessage() = rejects("رمز پویا: 123456 برای خرید 500,000 ریال از فروشگاه X. بانک ملت")

    @Test fun confirmationIdMessage() = rejects(
        "بانک ملی (هشدار)؛ شناسه تایید برداشت از حساب شما: 98765 مبلغ 500,000 به حساب 6037991234567890"
    )

    @Test fun installmentReminder() = rejects(
        "سر رسید قسط بانکی صادر شد. برای پرداخت قسط ماهانه وام مسکن به مبلغ 3,500,000 ریال طی دو روز آتی اقدام کنید. باتشکر بانک ملّی"
    )

    @Test fun failedTransaction() = rejects(
        "تراکنش ناموفق بود. برداشت 500,000 ریال از کارت ****1234 انجام نشد. بانک ملت"
    )

    @Test fun insufficientFunds() = rejects("بانک ملت: موجودی کافی نیست. برداشت 500,000 ریال از کارت ****1234")

    @Test fun balanceInquiryOnly() = rejects("بانک ملی\nمانده حساب شما 5,000,000 ریال است\n1403/05/01")

    @Test fun friendMessage() = rejects("سلام واریز کردم 500,000 تومان به حسابت", sender = "+989121234567")

    @Test fun friendMessageWithBankWords() = rejects(
        "برداشت 500,000 ریال از حساب بانک ملت مانده 2,000,000", sender = "09121234567"
    )

    @Test fun nonBankPayment() = rejects("پرداخت 150,000 تومان با موفقیت انجام شد. از خرید شما متشکریم")

    @Test fun ordinaryTextWithNumber() = rejects("سلام، فردا ساعت 5 جلسه داریم. هزینه 200,000 تومان")

    @Test fun cardActivation() = rejects("کارت 6037991234567890 شما فعال شد. بانک ملی")

    @Test fun telecomPromo() = rejects("همراه گرامی، با شارژ 100,000 تومانی هدیه بگیرید. جشنواره ویژه. لغو 11")

    @Test fun bluStyleWordingFromPersonalNumber() = rejects(
        "علی عزیز، 60,000,000ریال به حساب شما نشست.", sender = "09121234567"
    )
}