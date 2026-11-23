package ir.hamedan.budgetmanagement.data.draft

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftMoneyTest {

    @Test
    fun sameUnit_keepsDigits() {
        assertEquals("125000", DraftMoney.convert("125000", "IRT", "IRT"))
    }

    @Test
    fun tomanToRial_multipliesByTen() {
        assertEquals("1250000", DraftMoney.convert("125000", "IRT", "IRR"))
    }

    @Test
    fun rialToToman_dividesByTen() {
        assertEquals("12500", DraftMoney.convert("125000", "IRR", "IRT"))
    }

    @Test
    fun emptyZeroAndGarbage_becomeEmpty() {
        assertEquals("", DraftMoney.convert("", "IRT", "IRR"))
        assertEquals("", DraftMoney.convert("0", "IRT", "IRT"))
        assertEquals("", DraftMoney.convert("abc", "IRT", "IRT"))
        assertEquals("", DraftMoney.convert("5", "IRR", "IRT")) // 0 تومان
    }

    @Test
    fun overflowingAfterConversion_becomesEmpty() {
        assertEquals("", DraftMoney.convert("999999999999", "IRT", "IRR"))
    }

    @Test
    fun jsonHelpers_fallBackToDefaults() {
        val json = buildJsonObject {
            put("s", "x")
            put("b", true)
            put("n", 7)
            put("l", 9_000_000_000L)
        }
        assertEquals("x", json.str("s"))
        assertEquals("", json.str("missing"))
        assertTrue(json.bool("b"))
        assertEquals(7, json.int("n"))
        assertEquals(9_000_000_000L, json.long("l"))
        assertEquals(3, json.int("s", 3)) // نوع نادرست → پیش‌فرض
    }
}