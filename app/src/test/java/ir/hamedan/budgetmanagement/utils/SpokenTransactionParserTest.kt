package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.utils.SpokenTransactionParser.SpokenUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpokenTransactionParserTest {

    @Test
    fun digitsWithScaleWord() {
        val r = SpokenTransactionParser.parse("ناهار ۲۵۰ هزار تومان")
        assertEquals(250_000L, r.amount)
        assertEquals(SpokenUnit.TOMAN, r.unit)
        assertEquals("ناهار", r.title)
        assertNull(r.isExpense)
    }

    @Test
    fun spelledOutNumber() {
        val r = SpokenTransactionParser.parse("ناهار دویست و پنجاه هزار تومان")
        assertEquals(250_000L, r.amount)
        assertEquals("ناهار", r.title)
    }

    @Test
    fun halfMillion_andIncomeKeyword() {
        val r = SpokenTransactionParser.parse("حقوق دو و نیم میلیون تومان")
        assertEquals(2_500_000L, r.amount)
        assertEquals(false, r.isExpense)
        assertEquals("حقوق", r.title)
    }

    @Test
    fun expenseKeyword_andFillersRemoved() {
        val r = SpokenTransactionParser.parse("خریدم نون و پنیر پنجاه و پنج هزار تومن")
        assertEquals(55_000L, r.amount)
        assertEquals(true, r.isExpense)
        assertEquals("نون و پنیر", r.title)
    }

    @Test
    fun mixedScales_andSeparators() {
        assertEquals(5_200_000L, SpokenTransactionParser.parse("اجاره ۵ میلیون و ۲۰۰ هزار تومان").amount)
        assertEquals(120_000L, SpokenTransactionParser.parse("بنزین ۱۲۰٬۰۰۰ تومان").amount)
        assertEquals(80_000L, SpokenTransactionParser.parse("قهوه 80,000").amount)
        assertEquals(250_000L, SpokenTransactionParser.parse("taxi 250k").amount)
    }

    @Test
    fun noAmount_returnsNullAmount() {
        val r = SpokenTransactionParser.parse("سلام")
        assertNull(r.amount)
        assertEquals("سلام", r.title)
    }

    @Test
    fun displayAmount_convertsBetweenUnits() {
        val toman = SpokenTransactionParser.parse("ناهار ۲۵۰ هزار تومان")
        assertEquals(2_500_000L, SpokenTransactionParser.displayAmount(toman, displayIsRial = true))
        assertEquals(250_000L, SpokenTransactionParser.displayAmount(toman, displayIsRial = false))
        val rial = SpokenTransactionParser.parse("ناهار ۲۵۰ هزار ریال")
        assertEquals(25_000L, SpokenTransactionParser.displayAmount(rial, displayIsRial = false))
    }
}