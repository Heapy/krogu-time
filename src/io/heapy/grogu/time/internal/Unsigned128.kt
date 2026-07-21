package io.heapy.grogu.time.internal

/** A minimal unsigned 128-bit value used for exact duration arithmetic. */
internal data class Unsigned128(
    val high: ULong,
    val low: ULong,
) : Comparable<Unsigned128> {
    val isZero: Boolean
        get() = high == 0uL && low == 0uL

    override fun compareTo(other: Unsigned128): Int {
        val highComparison = high.compareTo(other.high)
        return if (highComparison != 0) highComparison else low.compareTo(other.low)
    }

    fun dividedBy(divisor: Unsigned128): Unsigned128Division {
        if (divisor.isZero) throw ArithmeticException("Division by zero")

        var quotientHigh = 0uL
        var quotientLow = 0uL
        var remainder = ZERO
        for (bitIndex in 127 downTo 0) {
            val overflow = remainder.high and TOP_BIT != 0uL
            remainder = remainder.shiftLeft(bitAt(bitIndex))
            if (overflow || remainder >= divisor) {
                remainder -= divisor
                if (bitIndex >= 64) {
                    quotientHigh = quotientHigh or (1uL shl (bitIndex - 64))
                } else {
                    quotientLow = quotientLow or (1uL shl bitIndex)
                }
            }
        }
        return Unsigned128Division(Unsigned128(quotientHigh, quotientLow), remainder)
    }

    private fun bitAt(bitIndex: Int): ULong =
        if (bitIndex >= 64) {
            high shr (bitIndex - 64) and 1uL
        } else {
            low shr bitIndex and 1uL
        }

    private fun shiftLeft(nextBit: ULong): Unsigned128 = Unsigned128(
        high = high shl 1 or (low shr 63),
        low = low shl 1 or nextBit,
    )

    private operator fun minus(other: Unsigned128): Unsigned128 {
        val resultLow = low - other.low
        val borrow = if (low < other.low) 1uL else 0uL
        return Unsigned128(high - other.high - borrow, resultLow)
    }

    internal companion object {
        val ZERO: Unsigned128 = Unsigned128(0uL, 0uL)

        private const val TOP_BIT: ULong = 0x8000_0000_0000_0000uL
    }
}

internal data class Unsigned128Division(
    val quotient: Unsigned128,
    val remainder: Unsigned128,
)

/** Returns the magnitude of a signed [Long], including [Long.MIN_VALUE]. */
internal fun unsignedMagnitude(value: Long): ULong =
    if (value >= 0) value.toULong() else (-(value + 1)).toULong() + 1uL

/** Multiplies [value] by a 32-bit [multiplier] and adds a 32-bit [addend]. */
internal fun unsignedMultiplyAdd(
    value: ULong,
    multiplier: UInt,
    addend: UInt,
): Unsigned128 {
    val factor = multiplier.toULong()
    val lowerProduct = (value and UINT_MASK) * factor
    val upperProduct = (value shr 32) * factor

    var low = lowerProduct + (upperProduct shl 32)
    var high = upperProduct shr 32
    if (low < lowerProduct) high++

    val adjustedLow = low + addend.toULong()
    if (adjustedLow < low) high++
    low = adjustedLow
    return Unsigned128(high, low)
}

private const val UINT_MASK: ULong = 0xffff_ffffuL
