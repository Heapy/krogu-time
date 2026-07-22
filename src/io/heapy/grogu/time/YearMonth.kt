package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorDiv
import io.heapy.grogu.time.internal.floorMod
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

/** A year and month without a day or time-zone in the ISO-8601 calendar system. */
public class YearMonth private constructor(
    public val year: Int,
    public val monthValue: Int,
) : Temporal, TemporalAdjuster, Comparable<YearMonth> {
    /** The month of this year-month. */
    public val month: Month
        get() = Month.of(monthValue)

    /** Whether this year-month occurs in a leap year. */
    public val isLeapYear: Boolean
        get() = Year.isLeap(year.toLong())

    private val prolepticMonth: Long
        get() = year * 12L + monthValue - 1

    /** Returns whether [dayOfMonth] is valid for this year-month. */
    public fun isValidDay(dayOfMonth: Int): Boolean = dayOfMonth in 1..lengthOfMonth()

    /** Returns the number of days in this month. */
    public fun lengthOfMonth(): Int = month.length(isLeapYear)

    /** Returns the number of days in this year. */
    public fun lengthOfYear(): Int = if (isLeapYear) 366 else 365

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) {
            field === ChronoField.MONTH_OF_YEAR ||
                field === ChronoField.PROLEPTIC_MONTH ||
                field === ChronoField.YEAR_OF_ERA ||
                field === ChronoField.YEAR ||
                field === ChronoField.ERA
        } else {
            field.isSupportedBy(this)
        }

    override fun isSupported(unit: TemporalUnit): Boolean =
        if (unit is ChronoUnit) {
            unit === ChronoUnit.MONTHS ||
                unit === ChronoUnit.YEARS ||
                unit === ChronoUnit.DECADES ||
                unit === ChronoUnit.CENTURIES ||
                unit === ChronoUnit.MILLENNIA ||
                unit === ChronoUnit.ERAS
        } else {
            unit.isSupportedBy(this)
        }

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.YEAR_OF_ERA -> if (year <= 0) {
            ValueRange.of(1, Year.MAX_VALUE.toLong() + 1)
        } else {
            ValueRange.of(1, Year.MAX_VALUE.toLong())
        }
        else -> super<Temporal>.range(field)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.MONTH_OF_YEAR -> monthValue.toLong()
        ChronoField.PROLEPTIC_MONTH -> prolepticMonth
        ChronoField.YEAR_OF_ERA -> (if (year < 1) 1 - year else year).toLong()
        ChronoField.YEAR -> year.toLong()
        ChronoField.ERA -> if (year < 1) 0 else 1
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.MONTHS as R
        }
        return super<Temporal>.query(query)
    }

    override fun with(adjuster: TemporalAdjuster): YearMonth =
        if (adjuster is YearMonth) adjuster else adjuster.adjustInto(this) as YearMonth

    override fun with(field: TemporalField, newValue: Long): YearMonth {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        field.checkValidValue(newValue)
        return when (field) {
            ChronoField.MONTH_OF_YEAR -> withMonth(newValue.toInt())
            ChronoField.PROLEPTIC_MONTH -> plusMonths(newValue - prolepticMonth)
            ChronoField.YEAR_OF_ERA -> withYear(
                if (year < 1) 1 - newValue.toInt() else newValue.toInt(),
            )
            ChronoField.YEAR -> withYear(newValue.toInt())
            ChronoField.ERA -> if (getLong(ChronoField.ERA) == newValue) this else withYear(1 - year)
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
    }

    /** Returns this year-month with the year changed. */
    public fun withYear(year: Int): YearMonth =
        if (this.year == year) this else of(year, monthValue)

    /** Returns this year-month with the month changed. */
    public fun withMonth(month: Int): YearMonth =
        if (monthValue == month) this else of(year, month)

    override fun plus(amount: TemporalAmount): YearMonth = amount.addTo(this) as YearMonth

    override fun plus(amountToAdd: Long, unit: TemporalUnit): YearMonth {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        return when (unit) {
            ChronoUnit.MONTHS -> plusMonths(amountToAdd)
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

    /** Returns this year-month with [yearsToAdd] added. */
    public fun plusYears(yearsToAdd: Long): YearMonth =
        if (yearsToAdd == 0L) this else withYear(
            ChronoField.YEAR.checkValidIntValue(year.toLong() + yearsToAdd),
        )

    /** Returns this year-month with [monthsToAdd] added. */
    public fun plusMonths(monthsToAdd: Long): YearMonth {
        if (monthsToAdd == 0L) return this
        val calculatedMonths = prolepticMonth + monthsToAdd
        val newYear = ChronoField.YEAR.checkValidIntValue(floorDiv(calculatedMonths, 12))
        val newMonth = floorMod(calculatedMonths, 12).toInt() + 1
        return with(newYear, newMonth)
    }

    override fun minus(amount: TemporalAmount): YearMonth = amount.subtractFrom(this) as YearMonth

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): YearMonth =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    /** Returns this year-month with [yearsToSubtract] subtracted. */
    public fun minusYears(yearsToSubtract: Long): YearMonth =
        if (yearsToSubtract == Long.MIN_VALUE) {
            plusYears(Long.MAX_VALUE).plusYears(1)
        } else {
            plusYears(-yearsToSubtract)
        }

    /** Returns this year-month with [monthsToSubtract] subtracted. */
    public fun minusMonths(monthsToSubtract: Long): YearMonth =
        if (monthsToSubtract == Long.MIN_VALUE) {
            plusMonths(Long.MAX_VALUE).plusMonths(1)
        } else {
            plusMonths(-monthsToSubtract)
        }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.PROLEPTIC_MONTH, prolepticMonth)

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        val monthsUntil = end.prolepticMonth - prolepticMonth
        if (unit !is ChronoUnit) return unit.between(this, end)
        return when (unit) {
            ChronoUnit.MONTHS -> monthsUntil
            ChronoUnit.YEARS -> monthsUntil / 12
            ChronoUnit.DECADES -> monthsUntil / 120
            ChronoUnit.CENTURIES -> monthsUntil / 1_200
            ChronoUnit.MILLENNIA -> monthsUntil / 12_000
            ChronoUnit.ERAS -> end.getLong(ChronoField.ERA) - getLong(ChronoField.ERA)
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Combines this year-month with [dayOfMonth]. */
    public fun atDay(dayOfMonth: Int): LocalDate = LocalDate.of(year, monthValue, dayOfMonth)

    /** Returns the final date in this year-month. */
    public fun atEndOfMonth(): LocalDate = LocalDate.of(year, monthValue, lengthOfMonth())

    override fun compareTo(other: YearMonth): Int {
        val yearComparison = year - other.year
        return if (yearComparison != 0) yearComparison else monthValue - other.monthValue
    }

    /** Whether this year-month is after [other]. */
    public fun isAfter(other: YearMonth): Boolean = compareTo(other) > 0

    /** Whether this year-month is before [other]. */
    public fun isBefore(other: YearMonth): Boolean = compareTo(other) < 0

    override fun equals(other: Any?): Boolean =
        this === other || other is YearMonth && year == other.year && monthValue == other.monthValue

    override fun hashCode(): Int = year xor (monthValue shl 27)

    override fun toString(): String {
        val yearText = when {
            year in 0..999 -> year.toString().padStart(4, '0')
            year in -999..-1 -> "-" + (-year).toString().padStart(4, '0')
            else -> year.toString()
        }
        return "$yearText-${monthValue.toString().padStart(2, '0')}"
    }

    private fun with(newYear: Int, newMonth: Int): YearMonth =
        if (year == newYear && monthValue == newMonth) this else YearMonth(newYear, newMonth)

    public companion object {
        /** Obtains the current year-month using the system clock in [zone]. */
        public fun now(zone: ZoneId): YearMonth = now(Clock.system(zone))

        /** Obtains the current year-month from [clock]. */
        public fun now(clock: Clock): YearMonth {
            val date = LocalDate.now(clock)
            return of(date.year, date.monthValue)
        }

        /** Obtains a year-month from a year and month. */
        public fun of(year: Int, month: Month): YearMonth = of(year, month.value)

        /** Obtains a year-month from a year and month number. */
        public fun of(year: Int, month: Int): YearMonth = YearMonth(
            ChronoField.YEAR.checkValidIntValue(year.toLong()),
            ChronoField.MONTH_OF_YEAR.checkValidIntValue(month.toLong()),
        )

        /** Obtains a year-month from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): YearMonth {
            if (temporal is YearMonth) return temporal
            return try {
                of(
                    temporal.get(ChronoField.YEAR),
                    temporal.get(ChronoField.MONTH_OF_YEAR),
                )
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain YearMonth from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses a year-month using the strict ISO year-month format. */
        public fun parse(text: CharSequence): YearMonth {
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
            val signed = index == 1
            val yearStart = index
            var yearValue = 0L
            while (index < input.length && input[index].isAsciiDigit()) {
                if (index - yearStart < 10) {
                    yearValue = yearValue * 10 + (input[index] - '0')
                }
                index++
            }
            val yearDigits = index - yearStart
            if (yearDigits > 10) throw parseFailure(input, yearStart + 10)
            val validYearWidth = when {
                !signed -> yearDigits == 4
                sign < 0 -> yearDigits in 4..10
                else -> yearDigits in 5..10
            }
            if (!validYearWidth || sign < 0 && yearValue == 0L) {
                throw parseFailure(input, 0)
            }
            if (index >= input.length || input[index] != '-') {
                throw parseFailure(input, index.coerceAtMost(yearStart + 10))
            }

            val monthStart = ++index
            if (!hasTwoDigits(input, monthStart)) throw parseFailure(input, monthStart)
            val month = parseTwoDigits(input, monthStart)
            index += 2
            if (index != input.length) throw parseFailure(input, index)

            val year = if (sign < 0) -yearValue else yearValue
            return try {
                of(ChronoField.YEAR.checkValidIntValue(year), month)
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a YearMonth",
                    input,
                    0,
                    exception,
                )
            }
        }

        private fun hasTwoDigits(input: String, index: Int): Boolean =
            index + 1 < input.length &&
                input[index].isAsciiDigit() &&
                input[index + 1].isAsciiDigit()

        private fun parseTwoDigits(input: String, index: Int): Int =
            (input[index] - '0') * 10 + (input[index + 1] - '0')

        private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a YearMonth", input, errorIndex)
    }
}
