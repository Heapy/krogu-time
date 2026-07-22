package io.heapy.grogu.time

import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/** A date-based amount expressed in ISO years, months, and days. */
public class Period private constructor(
    public val years: Int,
    public val months: Int,
    public val days: Int,
) : TemporalAmount {
    /** The supported units in descending significance. */
    override val units: List<TemporalUnit>
        get() = SUPPORTED_UNITS

    /** Whether every component is zero. */
    public val isZero: Boolean
        get() = years == 0 && months == 0 && days == 0

    /** Whether any component is negative. */
    public val isNegative: Boolean
        get() = years < 0 || months < 0 || days < 0

    override fun get(unit: TemporalUnit): Long = when (unit) {
        ChronoUnit.YEARS -> years.toLong()
        ChronoUnit.MONTHS -> months.toLong()
        ChronoUnit.DAYS -> days.toLong()
        else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }

    /** Returns this period with its years component replaced. */
    public fun withYears(years: Int): Period =
        if (years == this.years) this else create(years, months, days)

    /** Returns this period with its months component replaced. */
    public fun withMonths(months: Int): Period =
        if (months == this.months) this else create(years, months, days)

    /** Returns this period with its days component replaced. */
    public fun withDays(days: Int): Period =
        if (days == this.days) this else create(years, months, days)

    /** Returns this period with [amountToAdd] added component by component. */
    public fun plus(amountToAdd: TemporalAmount): Period {
        val period = from(amountToAdd)
        return create(
            toIntExact(years.toLong() + period.years),
            toIntExact(months.toLong() + period.months),
            toIntExact(days.toLong() + period.days),
        )
    }

    /** Returns this period with [yearsToAdd] added to the years component. */
    public fun plusYears(yearsToAdd: Long): Period =
        if (yearsToAdd == 0L) this else create(
            toIntExact(addExact(years.toLong(), yearsToAdd)),
            months,
            days,
        )

    /** Returns this period with [monthsToAdd] added to the months component. */
    public fun plusMonths(monthsToAdd: Long): Period =
        if (monthsToAdd == 0L) this else create(
            years,
            toIntExact(addExact(months.toLong(), monthsToAdd)),
            days,
        )

    /** Returns this period with [daysToAdd] added to the days component. */
    public fun plusDays(daysToAdd: Long): Period =
        if (daysToAdd == 0L) this else create(
            years,
            months,
            toIntExact(addExact(days.toLong(), daysToAdd)),
        )

    /** Returns this period with [amountToSubtract] subtracted component by component. */
    public fun minus(amountToSubtract: TemporalAmount): Period {
        val period = from(amountToSubtract)
        return create(
            toIntExact(years.toLong() - period.years),
            toIntExact(months.toLong() - period.months),
            toIntExact(days.toLong() - period.days),
        )
    }

    /** Returns this period with [yearsToSubtract] subtracted from its years. */
    public fun minusYears(yearsToSubtract: Long): Period =
        if (yearsToSubtract == Long.MIN_VALUE) {
            plusYears(Long.MAX_VALUE).plusYears(1)
        } else {
            plusYears(-yearsToSubtract)
        }

    /** Returns this period with [monthsToSubtract] subtracted from its months. */
    public fun minusMonths(monthsToSubtract: Long): Period =
        if (monthsToSubtract == Long.MIN_VALUE) {
            plusMonths(Long.MAX_VALUE).plusMonths(1)
        } else {
            plusMonths(-monthsToSubtract)
        }

    /** Returns this period with [daysToSubtract] subtracted from its days. */
    public fun minusDays(daysToSubtract: Long): Period =
        if (daysToSubtract == Long.MIN_VALUE) {
            plusDays(Long.MAX_VALUE).plusDays(1)
        } else {
            plusDays(-daysToSubtract)
        }

    /** Returns this period with every component multiplied by [scalar]. */
    public fun multipliedBy(scalar: Int): Period {
        if (isZero || scalar == 1) return this
        return create(
            toIntExact(years.toLong() * scalar),
            toIntExact(months.toLong() * scalar),
            toIntExact(days.toLong() * scalar),
        )
    }

    /** Returns this period with every component negated. */
    public fun negated(): Period = multipliedBy(-1)

    /** Returns this period with its years and months normalized to a 12-month year. */
    public fun normalized(): Period {
        val totalMonths = toTotalMonths()
        val splitYears = totalMonths / 12
        val splitMonths = (totalMonths % 12).toInt()
        if (splitYears == years.toLong() && splitMonths == months) return this
        return create(toIntExact(splitYears), splitMonths, days)
    }

    /** Returns the years and months combined as a total number of months. */
    public fun toTotalMonths(): Long = years * 12L + months

    override fun addTo(temporal: Temporal): Temporal {
        var result = temporal
        if (months == 0) {
            if (years != 0) result = result.plus(years.toLong(), ChronoUnit.YEARS)
        } else {
            val totalMonths = toTotalMonths()
            if (totalMonths != 0L) result = result.plus(totalMonths, ChronoUnit.MONTHS)
        }
        if (days != 0) result = result.plus(days.toLong(), ChronoUnit.DAYS)
        return result
    }

    override fun subtractFrom(temporal: Temporal): Temporal {
        var result = temporal
        if (months == 0) {
            if (years != 0) result = result.minus(years.toLong(), ChronoUnit.YEARS)
        } else {
            val totalMonths = toTotalMonths()
            if (totalMonths != 0L) result = result.minus(totalMonths, ChronoUnit.MONTHS)
        }
        if (days != 0) result = result.minus(days.toLong(), ChronoUnit.DAYS)
        return result
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Period &&
            years == other.years &&
            months == other.months &&
            days == other.days

    override fun hashCode(): Int =
        years + months.rotateLeft(8) + days.rotateLeft(16)

    override fun toString(): String {
        if (isZero) return "P0D"
        return buildString {
            append('P')
            if (years != 0) {
                append(years)
                append('Y')
            }
            if (months != 0) {
                append(months)
                append('M')
            }
            if (days != 0) {
                append(days)
                append('D')
            }
        }
    }

    public companion object {
        private val SUPPORTED_UNITS: List<TemporalUnit> =
            listOf(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS)

        public val ZERO: Period = Period(0, 0, 0)

        /** Creates a period containing only years. */
        public fun ofYears(years: Int): Period = create(years, 0, 0)

        /** Creates a period containing only months. */
        public fun ofMonths(months: Int): Period = create(0, months, 0)

        /** Creates a period containing a whole number of seven-day weeks. */
        public fun ofWeeks(weeks: Int): Period = create(0, 0, toIntExact(weeks.toLong() * 7))

        /** Creates a period containing only days. */
        public fun ofDays(days: Int): Period = create(0, 0, days)

        /** Creates a period containing independent year, month, and day components. */
        public fun of(years: Int, months: Int, days: Int): Period = create(years, months, days)

        /** Converts a temporal amount whose units are years, months, and days. */
        public fun from(amount: TemporalAmount): Period {
            if (amount is Period) return amount
            var years = 0
            var months = 0
            var days = 0
            amount.units.forEach { unit ->
                when (unit) {
                    ChronoUnit.YEARS -> years = toIntExact(amount.get(unit))
                    ChronoUnit.MONTHS -> months = toIntExact(amount.get(unit))
                    ChronoUnit.DAYS -> days = toIntExact(amount.get(unit))
                    else -> throw DateTimeException(
                        "Unit must be Years, Months or Days, but was $unit",
                    )
                }
            }
            return create(years, months, days)
        }

        private fun create(years: Int, months: Int, days: Int): Period =
            if (years == 0 && months == 0 && days == 0) ZERO else Period(years, months, days)

        private fun toIntExact(value: Long): Int {
            val result = value.toInt()
            if (result.toLong() != value) throw ArithmeticException("integer overflow")
            return result
        }
    }
}
