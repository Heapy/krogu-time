package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.format.DateTimeFormatter
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange

/** A month and day without a year or time-zone in the ISO-8601 calendar system. */
public class MonthDay private constructor(
    public val monthValue: Int,
    public val dayOfMonth: Int,
) : TemporalAccessor, TemporalAdjuster, Comparable<MonthDay> {
    /** The month of this month-day. */
    public val month: Month
        get() = Month.of(monthValue)

    override fun isSupported(field: TemporalField?): Boolean =
        if (field is ChronoField) {
            field === ChronoField.MONTH_OF_YEAR || field === ChronoField.DAY_OF_MONTH
        } else {
            field != null && field.isSupportedBy(this)
        }

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.MONTH_OF_YEAR -> field.range
        ChronoField.DAY_OF_MONTH -> ValueRange.of(
            1,
            month.minLength().toLong(),
            month.maxLength().toLong(),
        )
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.rangeRefinedBy(this)
    }

    override fun get(field: TemporalField): Int =
        range(field).checkValidIntValue(getLong(field), field)

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.MONTH_OF_YEAR -> monthValue.toLong()
        ChronoField.DAY_OF_MONTH -> dayOfMonth.toLong()
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.chronology()) {
            @Suppress("UNCHECKED_CAST")
            return IsoChronology as R
        }
        return super<TemporalAccessor>.query(query)
    }

    /** Returns this month-day with [month] changed, clamping to month-end if necessary. */
    public fun with(month: Month): MonthDay = withMonth(month.value)

    /** Returns this month-day with the month changed, clamping to month-end if necessary. */
    public fun withMonth(month: Int): MonthDay {
        val newMonth = Month.of(month)
        if (monthValue == month) return this
        return MonthDay(month, minOf(dayOfMonth, newMonth.maxLength()))
    }

    /** Returns this month-day with the day-of-month changed. */
    public fun withDayOfMonth(dayOfMonth: Int): MonthDay =
        if (this.dayOfMonth == dayOfMonth) this else of(monthValue, dayOfMonth)

    /** Returns whether this month-day is valid in [year]. */
    public fun isValidYear(year: Int): Boolean =
        dayOfMonth != 29 || month !== Month.FEBRUARY || Year.isLeap(year.toLong())

    override fun adjustInto(temporal: Temporal): Temporal {
        if (Chronology.from(temporal) != IsoChronology) {
            throw DateTimeException("Adjustment only supported on ISO date-time")
        }
        val adjusted = temporal.with(ChronoField.MONTH_OF_YEAR, monthValue.toLong())
        val resolvedDay = minOf(
            adjusted.range(ChronoField.DAY_OF_MONTH).maximum,
            dayOfMonth.toLong(),
        )
        return adjusted.with(ChronoField.DAY_OF_MONTH, resolvedDay)
    }

    /** Combines this month-day with [year], resolving February 29 to February 28 if needed. */
    public fun atYear(year: Int): LocalDate = LocalDate.of(
        year,
        monthValue,
        if (isValidYear(year)) dayOfMonth else 28,
    )

    /** Formats this month-day using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    override fun compareTo(other: MonthDay): Int {
        val monthComparison = monthValue - other.monthValue
        return if (monthComparison != 0) monthComparison else dayOfMonth - other.dayOfMonth
    }

    /** Whether this month-day is after [other]. */
    public fun isAfter(other: MonthDay): Boolean = compareTo(other) > 0

    /** Whether this month-day is before [other]. */
    public fun isBefore(other: MonthDay): Boolean = compareTo(other) < 0

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is MonthDay && monthValue == other.monthValue && dayOfMonth == other.dayOfMonth

    override fun hashCode(): Int = (monthValue shl 6) + dayOfMonth

    override fun toString(): String = buildString {
        append("--")
        append(monthValue.toString().padStart(2, '0'))
        append('-')
        append(dayOfMonth.toString().padStart(2, '0'))
    }

    public companion object {
        /** Obtains the current month-day using the system clock in the default time-zone. */
        public fun now(): MonthDay = now(Clock.systemDefaultZone())

        /** Obtains the current month-day using the system clock in [zone]. */
        public fun now(zone: ZoneId): MonthDay = now(Clock.system(zone))

        /** Obtains the current month-day from [clock]. */
        public fun now(clock: Clock): MonthDay {
            val date = LocalDate.now(clock)
            return of(date.monthValue, date.dayOfMonth)
        }

        /** Obtains a month-day from a month and day. */
        public fun of(month: Month, dayOfMonth: Int): MonthDay = of(month.value, dayOfMonth)

        /** Obtains a month-day from a month number and day. */
        public fun of(month: Int, dayOfMonth: Int): MonthDay {
            val resolvedMonth = Month.of(month)
            ChronoField.DAY_OF_MONTH.checkValidIntValue(dayOfMonth.toLong())
            if (dayOfMonth > resolvedMonth.maxLength()) {
                throw DateTimeException(
                    "Illegal value for DayOfMonth field, value $dayOfMonth is not valid " +
                        "for month $resolvedMonth",
                )
            }
            return MonthDay(month, dayOfMonth)
        }

        /** Obtains a month-day from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): MonthDay {
            if (temporal is MonthDay) return temporal
            return try {
                val isoTemporal = if (Chronology.from(temporal) == IsoChronology) {
                    temporal
                } else {
                    LocalDate.from(temporal)
                }
                of(
                    isoTemporal.get(ChronoField.MONTH_OF_YEAR),
                    isoTemporal.get(ChronoField.DAY_OF_MONTH),
                )
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain MonthDay from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses a month-day using the strict ISO month-day format. */
        public fun parse(text: CharSequence): MonthDay {
            val input = text.toString()
            if (input.length < 2 || input[0] != '-' || input[1] != '-') {
                throw parseFailure(input, 0)
            }
            if (!hasTwoDigits(input, 2)) throw parseFailure(input, 2)
            if (input.length <= 4 || input[4] != '-') throw parseFailure(input, 4)
            if (!hasTwoDigits(input, 5)) throw parseFailure(input, 5)
            if (input.length != 7) throw parseFailure(input, 7)

            val month = parseTwoDigits(input, 2)
            val day = parseTwoDigits(input, 5)
            return try {
                of(month, day)
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a MonthDay",
                    input,
                    0,
                    exception,
                )
            }
        }

        /** Parses a month-day from [text] using [formatter]. */
        public fun parse(text: CharSequence, formatter: DateTimeFormatter): MonthDay =
            formatter.parse(text, TemporalQuery(::from))

        private fun hasTwoDigits(input: String, index: Int): Boolean =
            index + 1 < input.length &&
                input[index].isAsciiDigit() &&
                input[index + 1].isAsciiDigit()

        private fun parseTwoDigits(input: String, index: Int): Int =
            (input[index] - '0') * 10 + (input[index + 1] - '0')

        private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a MonthDay", input, errorIndex)
    }
}
