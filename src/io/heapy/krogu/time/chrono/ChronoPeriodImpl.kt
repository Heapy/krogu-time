package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException

internal class ChronoPeriodImpl(
    override val chronology: Chronology,
    private val years: Int,
    private val months: Int,
    private val days: Int,
) : ChronoPeriod {
    override val units: List<TemporalUnit>
        get() = SUPPORTED_UNITS

    override val isZero: Boolean
        get() = years == 0 && months == 0 && days == 0

    override val isNegative: Boolean
        get() = years < 0 || months < 0 || days < 0

    override fun get(unit: TemporalUnit): Long = when (unit) {
        ChronoUnit.YEARS -> years.toLong()
        ChronoUnit.MONTHS -> months.toLong()
        ChronoUnit.DAYS -> days.toLong()
        else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }

    override fun plus(amountToAdd: TemporalAmount): ChronoPeriod {
        val amount = validateAmount(amountToAdd)
        return ChronoPeriodImpl(
            chronology,
            addExact(years, amount.years),
            addExact(months, amount.months),
            addExact(days, amount.days),
        )
    }

    override fun minus(amountToSubtract: TemporalAmount): ChronoPeriod {
        val amount = validateAmount(amountToSubtract)
        return ChronoPeriodImpl(
            chronology,
            subtractExact(years, amount.years),
            subtractExact(months, amount.months),
            subtractExact(days, amount.days),
        )
    }

    override fun multipliedBy(scalar: Int): ChronoPeriod {
        if (isZero || scalar == 1) return this
        return ChronoPeriodImpl(
            chronology,
            multiplyExact(years, scalar),
            multiplyExact(months, scalar),
            multiplyExact(days, scalar),
        )
    }

    override fun normalized(): ChronoPeriod {
        val monthRange = monthRange()
        if (monthRange <= 0) return this
        val totalMonths = years * monthRange + months
        val splitYears = totalMonths / monthRange
        val splitMonths = (totalMonths % monthRange).toInt()
        return if (splitYears == years.toLong() && splitMonths == months) {
            this
        } else {
            ChronoPeriodImpl(chronology, toIntExact(splitYears), splitMonths, days)
        }
    }

    override fun addTo(temporal: Temporal): Temporal = applyTo(temporal, subtract = false)

    override fun subtractFrom(temporal: Temporal): Temporal = applyTo(temporal, subtract = true)

    private fun applyTo(temporal: Temporal, subtract: Boolean): Temporal {
        validateChronology(temporal)
        var result = temporal
        val sign = if (subtract) -1L else 1L
        if (months == 0) {
            if (years != 0) result = result.plus(sign * years, ChronoUnit.YEARS)
        } else {
            val monthRange = monthRange()
            if (monthRange > 0) {
                result = result.plus(sign * (years * monthRange + months), ChronoUnit.MONTHS)
            } else {
                if (years != 0) result = result.plus(sign * years, ChronoUnit.YEARS)
                result = result.plus(sign * months, ChronoUnit.MONTHS)
            }
        }
        if (days != 0) result = result.plus(sign * days, ChronoUnit.DAYS)
        return result
    }

    private fun validateAmount(amount: TemporalAmount): ChronoPeriodImpl {
        if (amount !is ChronoPeriodImpl) {
            throw DateTimeException("Unable to obtain ChronoPeriod from TemporalAmount: $amount")
        }
        if (chronology != amount.chronology) {
            throw ClassCastException(
                "Chronology mismatch, expected: ${chronology.id}, actual: ${amount.chronology.id}",
            )
        }
        return amount
    }

    private fun validateChronology(temporal: Temporal) {
        val actual = temporal.query(TemporalQueries.chronology())
        if (actual != null && chronology != actual) {
            throw DateTimeException(
                "Chronology mismatch, expected: ${chronology.id}, actual: ${actual.id}",
            )
        }
    }

    private fun monthRange(): Long {
        val range = chronology.range(ChronoField.MONTH_OF_YEAR)
        return if (range.isFixed && range.isIntValue) range.maximum - range.minimum + 1 else -1
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ChronoPeriodImpl &&
            chronology == other.chronology &&
            years == other.years &&
            months == other.months &&
            days == other.days

    override fun hashCode(): Int =
        (years + months.rotateLeft(8) + days.rotateLeft(16)) xor chronology.hashCode()

    override fun toString(): String {
        if (isZero) return "$chronology P0D"
        return buildString {
            append(chronology)
            append(" P")
            if (years != 0) append(years).append('Y')
            if (months != 0) append(months).append('M')
            if (days != 0) append(days).append('D')
        }
    }

    private companion object {
        val SUPPORTED_UNITS: List<TemporalUnit> =
            listOf(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS)

        fun addExact(first: Int, second: Int): Int = toIntExact(first.toLong() + second)

        fun subtractExact(first: Int, second: Int): Int = toIntExact(first.toLong() - second)

        fun multiplyExact(first: Int, second: Int): Int = toIntExact(first.toLong() * second)

        fun toIntExact(value: Long): Int {
            val result = value.toInt()
            if (result.toLong() != value) throw ArithmeticException("integer overflow")
            return result
        }
    }
}
