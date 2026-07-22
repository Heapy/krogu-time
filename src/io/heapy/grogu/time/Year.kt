package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.format.DateTimeFormatter
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.multiplyExact
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange

/** A year in the ISO-8601 calendar system. */
public class Year private constructor(
    public val value: Int,
) : Temporal, TemporalAdjuster, Comparable<Year> {
    /** Whether this year is a leap year. */
    public val isLeap: Boolean
        get() = isLeap(value.toLong())

    /** The number of days in this year. */
    public val length: Int
        get() = if (isLeap) 366 else 365

    /** Combines this year with a one-based day-of-year. */
    public fun atDay(dayOfYear: Int): LocalDate = LocalDate.ofYearDay(value, dayOfYear)

    /** Returns whether [monthDay] is valid in this year. */
    public fun isValidMonthDay(monthDay: MonthDay?): Boolean =
        monthDay != null && monthDay.isValidYear(value)

    /** Combines this year with [monthDay]. */
    public fun atMonthDay(monthDay: MonthDay): LocalDate = monthDay.atYear(value)

    /** Combines this year with [month]. */
    public fun atMonth(month: Month): YearMonth = YearMonth.of(value, month)

    /** Combines this year with a month number. */
    public fun atMonth(month: Int): YearMonth = YearMonth.of(value, month)

    /** Formats this year using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) {
            field === ChronoField.YEAR ||
                field === ChronoField.YEAR_OF_ERA ||
                field === ChronoField.ERA
        } else {
            field.isSupportedBy(this)
        }

    override fun isSupported(unit: TemporalUnit): Boolean =
        if (unit is ChronoUnit) {
            unit === ChronoUnit.YEARS ||
                unit === ChronoUnit.DECADES ||
                unit === ChronoUnit.CENTURIES ||
                unit === ChronoUnit.MILLENNIA ||
                unit === ChronoUnit.ERAS
        } else {
            unit.isSupportedBy(this)
        }

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.YEAR_OF_ERA -> if (value <= 0) {
            ValueRange.of(1, MAX_VALUE.toLong() + 1)
        } else {
            ValueRange.of(1, MAX_VALUE.toLong())
        }
        else -> super<Temporal>.range(field)
    }

    override fun get(field: TemporalField): Int =
        range(field).checkValidIntValue(getLong(field), field)

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.YEAR_OF_ERA -> (if (value < 1) 1 - value else value).toLong()
        ChronoField.YEAR -> value.toLong()
        ChronoField.ERA -> if (value < 1) 0 else 1
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.chronology()) {
            @Suppress("UNCHECKED_CAST")
            return IsoChronology as R
        }
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.YEARS as R
        }
        return super<Temporal>.query(query)
    }

    override fun with(adjuster: TemporalAdjuster): Year = adjuster.adjustInto(this) as Year

    override fun with(field: TemporalField, newValue: Long): Year {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        field.checkValidValue(newValue)
        return when (field) {
            ChronoField.YEAR_OF_ERA -> of(if (value < 1) 1 - newValue.toInt() else newValue.toInt())
            ChronoField.YEAR -> of(newValue.toInt())
            ChronoField.ERA -> if (getLong(ChronoField.ERA) == newValue) this else of(1 - value)
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
    }

    override fun plus(amount: TemporalAmount): Year = amount.addTo(this) as Year

    override fun plus(amountToAdd: Long, unit: TemporalUnit): Year {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        return when (unit) {
            ChronoUnit.YEARS -> plusYears(amountToAdd)
            ChronoUnit.DECADES -> plusYears(multiplyExact(amountToAdd, 10))
            ChronoUnit.CENTURIES -> plusYears(multiplyExact(amountToAdd, 100))
            ChronoUnit.MILLENNIA -> plusYears(multiplyExact(amountToAdd, 1_000))
            ChronoUnit.ERAS -> with(
                ChronoField.ERA,
                addExact(getLong(ChronoField.ERA), amountToAdd),
            )
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Returns this year with [yearsToAdd] added. */
    public fun plusYears(yearsToAdd: Long): Year =
        if (yearsToAdd == 0L) this else of(
            ChronoField.YEAR.checkValidIntValue(value.toLong() + yearsToAdd),
        )

    override fun minus(amount: TemporalAmount): Year = amount.subtractFrom(this) as Year

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): Year =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    /** Returns this year with [yearsToSubtract] subtracted. */
    public fun minusYears(yearsToSubtract: Long): Year =
        if (yearsToSubtract == Long.MIN_VALUE) {
            plusYears(Long.MAX_VALUE).plusYears(1)
        } else {
            plusYears(-yearsToSubtract)
        }

    override fun adjustInto(temporal: Temporal): Temporal {
        if (Chronology.from(temporal) != IsoChronology) {
            throw DateTimeException("Adjustment only supported on ISO date-time")
        }
        return temporal.with(ChronoField.YEAR, value.toLong())
    }

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        val yearsUntil = end.value.toLong() - value
        if (unit !is ChronoUnit) return unit.between(this, end)
        return when (unit) {
            ChronoUnit.YEARS -> yearsUntil
            ChronoUnit.DECADES -> yearsUntil / 10
            ChronoUnit.CENTURIES -> yearsUntil / 100
            ChronoUnit.MILLENNIA -> yearsUntil / 1_000
            ChronoUnit.ERAS -> end.getLong(ChronoField.ERA) - getLong(ChronoField.ERA)
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    override fun compareTo(other: Year): Int = value.compareTo(other.value)

    /** Whether this year is after [other]. */
    public fun isAfter(other: Year): Boolean = value > other.value

    /** Whether this year is before [other]. */
    public fun isBefore(other: Year): Boolean = value < other.value

    override fun equals(other: Any?): Boolean = this === other || other is Year && value == other.value

    override fun hashCode(): Int = value

    override fun toString(): String = value.toString()

    public companion object {
        public const val MIN_VALUE: Int = -999_999_999
        public const val MAX_VALUE: Int = 999_999_999

        /** Obtains the current year using the system clock in the default time-zone. */
        public fun now(): Year = now(Clock.systemDefaultZone())

        /** Obtains the current year using the system clock in [zone]. */
        public fun now(zone: ZoneId): Year = now(Clock.system(zone))

        /** Obtains the current year from [clock]. */
        public fun now(clock: Clock): Year = of(LocalDate.now(clock).year)

        /** Obtains an ISO year. */
        public fun of(isoYear: Int): Year =
            Year(ChronoField.YEAR.checkValidIntValue(isoYear.toLong()))

        /** Obtains a year from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): Year {
            if (temporal is Year) return temporal
            return try {
                val isoTemporal = if (Chronology.from(temporal) == IsoChronology) {
                    temporal
                } else {
                    LocalDate.from(temporal)
                }
                of(isoTemporal.get(ChronoField.YEAR))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain Year from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses a year using the default ISO year format. */
        public fun parse(text: CharSequence): Year {
            val input = text.toString()
            if (input.isEmpty()) throw parseFailure(input, 0)

            var index = 0
            val sign = when (input[0]) {
                '+' -> {
                    index++
                    1
                }
                '-' -> {
                    index++
                    -1
                }
                else -> 1
            }
            val yearStart = index
            var yearValue = 0L
            while (index < input.length && input[index].isAsciiDigit()) {
                if (index - yearStart < 9) {
                    yearValue = yearValue * 10 + (input[index] - '0')
                }
                index++
            }
            val yearDigits = index - yearStart
            if (yearDigits > 9) throw parseFailure(input, yearStart + 9)
            if (yearDigits == 0) throw parseFailure(input, yearStart)
            if (index != input.length) throw parseFailure(input, index)

            val year = if (sign < 0) -yearValue else yearValue
            return try {
                of(ChronoField.YEAR.checkValidIntValue(year))
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a Year",
                    input,
                    0,
                    exception,
                )
            }
        }

        /** Parses a year from [text] using [formatter]. */
        public fun parse(text: CharSequence, formatter: DateTimeFormatter): Year =
            formatter.parse(text, TemporalQuery(::from))

        /** Returns whether [year] is a leap year in the proleptic Gregorian calendar. */
        public fun isLeap(year: Long): Boolean =
            year and 3L == 0L && (year % 100L != 0L || year % 400L == 0L)

        private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a Year", input, errorIndex)
    }
}
