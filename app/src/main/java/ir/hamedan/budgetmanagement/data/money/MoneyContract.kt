package ir.hamedan.budgetmanagement.data.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Canonical money contract for the app.
 *
 * Persistent/sync values are Long integer units. The current product stores
 * amounts in Toman; IRR is a display conversion of 1 Toman = 10 Rial.
 * Floating point is allowed only at UI/input boundaries and is converted with
 * BigDecimal so binary floating-point rounding never reaches Room or the API.
 */
object MoneyContract {
    const val STORAGE_UNIT = "IRT"
    const val DISPLAY_RIAL = "IRR"
    const val RIALS_PER_TOMAN = 10L

    fun fromInput(value: Double): Long {
        require(value.isFinite()) { "Money value must be finite." }
        require(value >= 0.0) { "Money value cannot be negative." }
        return BigDecimal.valueOf(value)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun fromInput(value: String): Long {
        val decimal = value.trim().replace(",", "").toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Invalid money value.")
        require(decimal >= BigDecimal.ZERO) { "Money value cannot be negative." }
        return decimal.setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    fun toRial(toman: Long): Long = Math.multiplyExact(toman, RIALS_PER_TOMAN)

    fun inputToStorage(value: Double, currencyUnit: String): Long =
        if (currencyUnit == DISPLAY_RIAL) fromRial(fromInput(value)) else fromInput(value)

    fun displayFromStorage(toman: Long, currencyUnit: String): Long =
        if (currencyUnit == DISPLAY_RIAL) toRial(toman) else toman

    fun fromRial(rial: Long): Long {
        require(rial >= 0L) { "Rial value cannot be negative." }
        return BigDecimal.valueOf(rial)
            .divide(BigDecimal.valueOf(RIALS_PER_TOMAN), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}