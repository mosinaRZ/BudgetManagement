package ir.hamedan.budgetmanagement.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInWordsTest {

    @Test
    fun persian_zeroAndNegative_areEmpty() {
        assertEquals("", AmountInWords.persian(0))
        assertEquals("", AmountInWords.persian(-5))
    }

    @Test
    fun persian_commonAmounts() {
        assertEquals("بیست و یک", AmountInWords.persian(21))
        assertEquals("هشتاد هزار", AmountInWords.persian(80_000))
        assertEquals("صد و پنج هزار", AmountInWords.persian(105_000))
        assertEquals("دو میلیون و پانصد هزار", AmountInWords.persian(2_500_000))
        assertEquals("یک میلیون و دویست و پنجاه هزار", AmountInWords.persian(1_250_000))
    }

    @Test
    fun englishCompact_formatsScale() {
        assertEquals("", AmountInWords.englishCompact(999))
        assertEquals("2.5 million", AmountInWords.englishCompact(2_500_000))
        assertEquals("80 thousand", AmountInWords.englishCompact(80_000))
    }
}