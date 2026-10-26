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

    @Test fun bankMelliLegacySignedAmountFromPhoneLikeSender() = accepts(
        "بانك ملي ايران\nبرداشت:300,000-\nحساب:31003\nمانده:8,280,766\n1113-08:43",
        30000.0, "EXPENSE", sender = "+989180500841"
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

    // Real Blu messages (1405/07). Amounts are Rial -> Toman.
    @Test fun bluRealDeposit() = accepts(
        "بلو\nواریز پول\nمحمدسینا عزیز، 100,000,000ریال به حساب شما نشست.\nموجودی: 242,751,047 ریال\n۱۳:۳۰\n۱۴۰۵.۰۷.۲۵",
        10000000.0, "INCOME", sender = "Blu"
    )

    @Test fun bluRealWithdraw() = accepts(
        "بلو\nبرداشت پول\nمحمدسینا عزیز، 2,923,800ریال از حساب شما پرید.\nموجودی: 142,751,047 ریال\n۲۱:۰۳\n۱۴۰۵.۰۷.۲۳",
        292380.0, "EXPENSE", sender = "Blu"
    )

    @Test fun bluRealMessagesWithoutSenderOrHeader() {
        accepts("واریز پول\nمحمدسینا عزیز، 100,000,000ریال به حساب شما نشست.\nموجودی: 242,751,047 ریال\n۱۳:۳۰\n۱۴۰۵.۰۷.۲۵", 10000000.0, "INCOME")
        accepts("برداشت پول\r\nمحمدسینا عزیز، 2,923,800ریال از حساب شما پرید.\r\nموجودی: 142,751,047 ریال", 292380.0, "EXPENSE")
    }

    /** The dynamic-password SMS only announces a purchase; the real debit arrives as a separate SMS. */
    @Test fun bluDynamicPasswordIsNotATransaction() = rejects(
        "بلو\nبفرمایید رمز پویا\nخرید\nايرانسل\nمبلغ: 2,923,800 ریال\nرمز: 444155 \n21:03", sender = "Blu"
    )

    // Blu writes the customer's first name into the text; names must never influence detection.
    @Test fun bluNameThatIsAPromoWord() = accepts(
        "بلو\nواریز پول\nهدیه عزیز، 100,000,000ریال به حساب شما نشست.\nموجودی: 242,751,047 ریال\n۱۳:۳۰\n۱۴۰۵.۰۷.۲۵",
        10000000.0, "INCOME", sender = "Blu"
    )

    @Test fun bluNameContainingIncomeWordDoesNotFlipWithdrawToIncome() = accepts(
        "بلو\nبرداشت پول\nسودابه عزیز، 2,923,800ریال از حساب شما پرید.\nموجودی: 142,751,047 ریال\n۲۱:۰۳\n۱۴۰۵.۰۷.۲۳",
        292380.0, "EXPENSE", sender = "Blu"
    )

    @Test fun bluNameThatIsAnExpenseWordDoesNotFlipDepositToExpense() = accepts(
        "بلو\nواریز پول\nشارژ عزیز، 100,000,000ریال به حساب شما نشست.\nموجودی: 242,751,047 ریال\n۱۳:۳۰\n۱۴۰۵.۰۷.۲۵",
        10000000.0, "INCOME", sender = "Blu"
    )

    @Test fun bluNameOnSingleLineMessage() = accepts(
        "بلو برداشت پول فرصت عزیز، 2,923,800ریال از حساب شما پرید. موجودی: 142,751,047 ریال",
        292380.0, "EXPENSE", sender = "Blu"
    )

    // Real Blu messages (1405/07-08) as reported by a user.
    @Test fun bluRealWithdrawEightMillion() = accepts(
        "بلو\nبرداشت پول\nمحمدسینا عزیز، 8,805,000ریال از حساب شما پرید.\nموجودی: 44,536,598 ریال\n۱۱:۴۲\n۱۴۰۵.۰۸.۰۱",
        880500.0, "EXPENSE", sender = "Blu"
    )

    @Test fun bluRealWithdrawOverOneBillionRial() = accepts(
        "بلو\nبرداشت پول\nمحمدسینا عزیز، 1,050,000,000ریال از حساب شما پرید.\nموجودی: 67,756,534 ریال\n۱۳:۰۶\n۱۴۰۵.۰۷.۲۸",
        105000000.0, "EXPENSE", sender = "Blu"
    )

    @Test fun bluRealDepositFortyFiveMillion() = accepts(
        "بلو\nواریز پول\nمحمدسینا عزیز، 45,000,000ریال به حساب شما نشست.\nموجودی: 467,756,534 ریال\n۱۷:۲۱\n۱۴۰۵.۰۷.۲۷",
        4500000.0, "INCOME", sender = "Blu"
    )

    @Test fun bluDynamicPasswordWithArabicLettersIsNotATransaction() = rejects(
        "بلو\nبفرمایید رمز پویا\nخرید\nنوآوران پرداخت مجازي ايرانيان\nمبلغ: 20,156,499 ریال\nرمز: 586421 \n11:43",
        sender = "Blu"
    )

    // SMS gateways may inject invisible characters anywhere (inside the amount too).
    // A single invisible character at ANY position must neither reject the SMS nor change the amount.
    @Test fun invisibleCharactersAnywhereDoNotBreakDetection() {
        val cases = listOf(
            Triple(
                "بلو\nبرداشت پول\nمحمدسینا عزیز، 8,805,000ریال از حساب شما پرید.\nموجودی: 44,536,598 ریال\n۱۱:۴۲\n۱۴۰۵.۰۸.۰۱",
                880500.0, "EXPENSE"
            ),
            Triple(
                "بلو\nواریز پول\nمحمدسینا عزیز، 45,000,000ریال به حساب شما نشست.\nموجودی: 467,756,534 ریال\n۱۷:۲۱\n۱۴۰۵.۰۷.۲۷",
                4500000.0, "INCOME"
            )
        )
        val invisibles = listOf(
            '\u200B', '\u200C', '\u200D', '\u200E', '\u200F', '\u2060', '\u00AD',
            '\u061C', '\u202F', '\u2009', '\u2068', '\uFEFF'
        )
        for ((base, amount, type) in cases) {
            for (ch in invisibles) {
                for (i in 0..base.length) {
                    accepts(base.substring(0, i) + ch + base.substring(i), amount, type, sender = "Blu")
                }
            }
        }
    }

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

    @Test fun bluRealWithdrawFromPhoneLikeSenderIsAccepted() = accepts(
        "بلو\nبرداشت پول\nمحمدسینا عزیز، 8,805,000ریال از حساب شما پرید.\nموجودی: 44,536,598 ریال\n۱۱:۴۲\n۱۴۰۵.۰۸.۰۱",
        880500.0, "EXPENSE", sender = "09180500841"
    )

    @Test fun bluRealDepositFromPhoneLikeSenderIsAccepted() = accepts(
        "بلو\nواریز پول\nمحمدسینا عزیز، 45,000,000ریال به حساب شما نشست.\nموجودی: 467,756,534 ریال\n۱۷:۲۱\n۱۴۰۵.۰۷.۲۷",
        4500000.0, "INCOME", sender = "+989180500841"
    )

    @Test fun phoneLikeSenderDoesNotBypassOtpFilter() = rejects(
        "بلو\nبفرمایید رمز پویا\nخرید\nمبلغ: 20,156,499 ریال\nرمز: 586421\n11:43",
        sender = "09180500841"
    )

    @Test fun phoneLikeSenderWithWeakTransferWordingIsStillRejected() = rejects(
        "علی عزیز، 60,000,000ریال به حساب شما نشست.", sender = "09121234567"
    )
}