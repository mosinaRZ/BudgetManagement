package ir.hamedan.budgetmanagement.data.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyContractTest {
    @Test fun fromInput_roundsHalfUp() {
        assertThat(MoneyContract.fromInput(12.49)).isEqualTo(12L)
        assertThat(MoneyContract.fromInput(12.50)).isEqualTo(13L)
    }

    @Test fun inputToStorage_convertsRialToToman() {
        assertThat(MoneyContract.inputToStorage(1_000.0, MoneyContract.DISPLAY_RIAL)).isEqualTo(100L)
        assertThat(MoneyContract.displayFromStorage(100L, MoneyContract.DISPLAY_RIAL)).isEqualTo(1_000L)
    }

    @Test fun fromRial_roundsHalfUp() {
        assertThat(MoneyContract.fromRial(15L)).isEqualTo(2L)
        assertThat(MoneyContract.fromRial(14L)).isEqualTo(1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromInput_rejectsNegative() {
        MoneyContract.fromInput(-1.0)
    }

    @Test(expected = ArithmeticException::class)
    fun toRial_rejectsOverflow() {
        MoneyContract.toRial(Long.MAX_VALUE)
    }
}