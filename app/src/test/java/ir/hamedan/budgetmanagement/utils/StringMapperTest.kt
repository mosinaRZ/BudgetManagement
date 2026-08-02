package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Maps DB category keys → localized display labels.
 * Unknown keys must pass through unchanged (custom user categories).
 */
class StringMapperTest {

    // -------------------------------------------------------------------------
    // Known keys — Persian
    // -------------------------------------------------------------------------

    @Test
    fun food_persian() {
        assertThat(StringMapper.getCategoryName("FOOD", isPersian = true))
            .isEqualTo("خوراکی و رستوران")
    }

    @Test
    fun transport_persian() {
        assertThat(StringMapper.getCategoryName("TRANSPORT", isPersian = true))
            .isEqualTo("حمل و نقل")
    }

    @Test
    fun shopping_persian() {
        assertThat(StringMapper.getCategoryName("SHOPPING", isPersian = true))
            .isEqualTo("خرید")
    }

    @Test
    fun bill_persian() {
        assertThat(StringMapper.getCategoryName("BILL", isPersian = true))
            .isEqualTo("قبوض و اجاره")
    }

    @Test
    fun salary_persian() {
        assertThat(StringMapper.getCategoryName("SALARY", isPersian = true))
            .isEqualTo("حقوق و درآمد")
    }

    @Test
    fun investment_persian() {
        assertThat(StringMapper.getCategoryName("INVESTMENT", isPersian = true))
            .isEqualTo("سرمایه‌گذاری")
    }

    @Test
    fun uncategorized_persian() {
        assertThat(StringMapper.getCategoryName("UNCATEGORIZED", isPersian = true))
            .isEqualTo("دسته‌بندی نشده")
    }

    // -------------------------------------------------------------------------
    // Known keys — English
    // -------------------------------------------------------------------------

    @Test
    fun food_english() {
        assertThat(StringMapper.getCategoryName("FOOD", isPersian = false))
            .isEqualTo("Food & Dining")
    }

    @Test
    fun transport_english() {
        assertThat(StringMapper.getCategoryName("TRANSPORT", isPersian = false))
            .isEqualTo("Transportation")
    }

    @Test
    fun shopping_english() {
        assertThat(StringMapper.getCategoryName("SHOPPING", isPersian = false))
            .isEqualTo("Shopping")
    }

    @Test
    fun bill_english() {
        assertThat(StringMapper.getCategoryName("BILL", isPersian = false))
            .isEqualTo("Bills & Rent")
    }

    @Test
    fun salary_english() {
        assertThat(StringMapper.getCategoryName("SALARY", isPersian = false))
            .isEqualTo("Salary")
    }

    @Test
    fun investment_english() {
        assertThat(StringMapper.getCategoryName("INVESTMENT", isPersian = false))
            .isEqualTo("Investment")
    }

    @Test
    fun uncategorized_english() {
        assertThat(StringMapper.getCategoryName("UNCATEGORIZED", isPersian = false))
            .isEqualTo("Uncategorized")
    }

    // -------------------------------------------------------------------------
    // Case-insensitivity (DB might store mixed case)
    // -------------------------------------------------------------------------

    @Test
    fun lowercaseKey_stillMaps() {
        assertThat(StringMapper.getCategoryName("food", isPersian = false))
            .isEqualTo("Food & Dining")
        assertThat(StringMapper.getCategoryName("food", isPersian = true))
            .isEqualTo("خوراکی و رستوران")
    }

    @Test
    fun mixedCaseKey_stillMaps() {
        assertThat(StringMapper.getCategoryName("Food", isPersian = false))
            .isEqualTo("Food & Dining")
        assertThat(StringMapper.getCategoryName("UnCaTeGoRiZeD", isPersian = true))
            .isEqualTo("دسته‌بندی نشده")
    }

    // -------------------------------------------------------------------------
    // Unknown / custom categories — must NOT be mangled
    // -------------------------------------------------------------------------

    @Test
    fun unknownKey_returnedAsIs() {
        assertThat(StringMapper.getCategoryName("ورزش", isPersian = true))
            .isEqualTo("ورزش")
        assertThat(StringMapper.getCategoryName("Gym", isPersian = false))
            .isEqualTo("Gym")
    }

    @Test
    fun emptyKey_returnedAsIs() {
        assertThat(StringMapper.getCategoryName("", isPersian = true)).isEmpty()
        assertThat(StringMapper.getCategoryName("", isPersian = false)).isEmpty()
    }

    @Test
    fun customKey_doesNotAccidentallyMatchSubstring() {
        // "FOODIE" must not become Food & Dining
        assertThat(StringMapper.getCategoryName("FOODIE", isPersian = false))
            .isEqualTo("FOODIE")
    }
}

/**
 * Table-driven coverage: every known key × both locales in one parameterized suite.
 */
@RunWith(Parameterized::class)
class StringMapperParameterizedTest(
    private val key: String,
    private val isPersian: Boolean,
    private val expected: String
) {
    @Test
    fun mapsExpected() {
        assertThat(StringMapper.getCategoryName(key, isPersian)).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} persian={1} → {2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("FOOD", true, "خوراکی و رستوران"),
            arrayOf("FOOD", false, "Food & Dining"),
            arrayOf("TRANSPORT", true, "حمل و نقل"),
            arrayOf("TRANSPORT", false, "Transportation"),
            arrayOf("SHOPPING", true, "خرید"),
            arrayOf("SHOPPING", false, "Shopping"),
            arrayOf("BILL", true, "قبوض و اجاره"),
            arrayOf("BILL", false, "Bills & Rent"),
            arrayOf("SALARY", true, "حقوق و درآمد"),
            arrayOf("SALARY", false, "Salary"),
            arrayOf("INVESTMENT", true, "سرمایه‌گذاری"),
            arrayOf("INVESTMENT", false, "Investment"),
            arrayOf("UNCATEGORIZED", true, "دسته‌بندی نشده"),
            arrayOf("UNCATEGORIZED", false, "Uncategorized"),
        )
    }
}