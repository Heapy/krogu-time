package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.format.LocaleTextField
import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.format.localeTextValues
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/**
 * A month of the year in the ISO-8601 calendar system, from January to December.
 */
public enum class Month : TemporalAccessor, TemporalAdjuster {
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER;

    /** The ISO-8601 month number, from 1 (January) to 12 (December). */
    public val value: Int
        get() = ordinal + 1

    /** Returns this month's localized display name, or its numeric value when unavailable. */
    public fun getDisplayName(style: TextStyle, locale: Locale): String =
        localeTextValues(
            locale.toLanguageTag(),
            IsoChronology.id,
            LocaleTextField.MONTH_OF_YEAR,
            style,
        ).firstOrNull { it.value == value.toLong() }?.text ?: value.toString()

    /** Returns this month with [months] added, wrapping around the year. */
    public fun plus(months: Long): Month {
        val amount = (months % MONTHS_PER_YEAR).toInt()
        return entries[(ordinal + amount + MONTHS_PER_YEAR) % MONTHS_PER_YEAR]
    }

    /** Returns this month with [months] subtracted, wrapping around the year. */
    public fun minus(months: Long): Month = plus(-(months % MONTHS_PER_YEAR))

    /** Returns this month's length for a common or leap year. */
    public fun length(isLeapYear: Boolean): Int = when (this) {
        FEBRUARY -> if (isLeapYear) 29 else 28
        APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30
        else -> 31
    }

    /** Returns the shortest possible length of this month. */
    public fun minLength(): Int = when (this) {
        FEBRUARY -> 28
        APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30
        else -> 31
    }

    /** Returns the longest possible length of this month. */
    public fun maxLength(): Int = when (this) {
        FEBRUARY -> 29
        APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30
        else -> 31
    }

    /** Returns this month's first day-of-year, where January 1 is day 1. */
    public fun firstDayOfYear(isLeapYear: Boolean): Int {
        val leapDay = if (isLeapYear && this >= MARCH) 1 else 0
        return COMMON_YEAR_STARTS[ordinal] + leapDay
    }

    /** Returns the first month of the quarter containing this month. */
    public fun firstMonthOfQuarter(): Month = entries[(ordinal / MONTHS_PER_QUARTER) * MONTHS_PER_QUARTER]

    override fun isSupported(field: TemporalField): Boolean = if (field is ChronoField) {
        field == ChronoField.MONTH_OF_YEAR
    } else {
        field.isSupportedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when {
        field == ChronoField.MONTH_OF_YEAR -> value.toLong()
        field is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.MONTH_OF_YEAR, value.toLong())

    public companion object {
        private const val MONTHS_PER_QUARTER: Int = 3
        private const val MONTHS_PER_YEAR: Int = 12
        private val COMMON_YEAR_STARTS: IntArray =
            intArrayOf(1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)

        /** Returns the month identified by its ISO-8601 value. */
        public fun of(month: Int): Month {
            if (month !in 1..MONTHS_PER_YEAR) {
                throw DateTimeException("Invalid value for MonthOfYear: $month")
            }
            return entries[month - 1]
        }

        /** Obtains an ISO month from [temporal]. */
        public fun from(temporal: TemporalAccessor): Month {
            if (temporal is Month) return temporal
            return try {
                val isoTemporal = if (Chronology.from(temporal) == IsoChronology) {
                    temporal
                } else {
                    LocalDate.from(temporal)
                }
                of(isoTemporal.get(ChronoField.MONTH_OF_YEAR))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain Month from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }
    }
}
