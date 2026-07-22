package io.heapy.grogu.time

import io.heapy.grogu.time.internal.floorMod
import io.heapy.grogu.time.temporal.ChronoField

/** A date without a time-zone in the ISO-8601 calendar system. */
public class LocalDate private constructor(
    public val year: Int,
    public val monthValue: Int,
    public val dayOfMonth: Int,
) : Comparable<LocalDate> {
    /** The month of this date. */
    public val month: Month
        get() = Month.of(monthValue)

    /** The one-based day within this date's year. */
    public val dayOfYear: Int
        get() = month.firstDayOfYear(isLeapYear) + dayOfMonth - 1

    /** The ISO day of week. */
    public val dayOfWeek: DayOfWeek
        get() = DayOfWeek.of(floorMod(toEpochDay() + 3, 7).toInt() + 1)

    /** Whether this date's year is a leap year. */
    public val isLeapYear: Boolean
        get() = Year.isLeap(year.toLong())

    /** Returns the number of days in this date's month. */
    public fun lengthOfMonth(): Int = month.length(isLeapYear)

    /** Returns the number of days in this date's year. */
    public fun lengthOfYear(): Int = if (isLeapYear) 366 else 365

    /** Converts this date to the count of days from 1970-01-01. */
    public fun toEpochDay(): Long {
        val prolepticYear = year.toLong()
        val month = monthValue.toLong()
        var total = 365L * prolepticYear
        total += if (prolepticYear >= 0) {
            (prolepticYear + 3) / 4 -
                (prolepticYear + 99) / 100 +
                (prolepticYear + 399) / 400
        } else {
            -(prolepticYear / -4 - prolepticYear / -100 + prolepticYear / -400)
        }
        total += (367 * month - 362) / 12
        total += dayOfMonth - 1
        if (month > 2) total -= if (isLeapYear) 1 else 2
        return total - DAYS_0000_TO_1970
    }

    override fun compareTo(other: LocalDate): Int {
        val yearComparison = year.compareTo(other.year)
        if (yearComparison != 0) return yearComparison
        val monthComparison = monthValue.compareTo(other.monthValue)
        return if (monthComparison != 0) monthComparison else dayOfMonth.compareTo(other.dayOfMonth)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LocalDate &&
            year == other.year &&
            monthValue == other.monthValue &&
            dayOfMonth == other.dayOfMonth

    override fun hashCode(): Int {
        val yearAndMonth = year and -2048 xor (year shl 11) xor (monthValue shl 6)
        return yearAndMonth xor dayOfMonth
    }

    override fun toString(): String {
        val yearText = when {
            year in 0..999 -> year.toString().padStart(4, '0')
            year in -999..-1 -> "-" + (-year).toString().padStart(4, '0')
            year > 9_999 -> "+$year"
            else -> year.toString()
        }
        return buildString {
            append(yearText)
            append('-')
            append(monthValue.toString().padStart(2, '0'))
            append('-')
            append(dayOfMonth.toString().padStart(2, '0'))
        }
    }

    public companion object {
        private const val DAYS_PER_CYCLE: Long = 146_097
        private const val DAYS_0000_TO_1970: Long = 719_528

        public val MIN: LocalDate = LocalDate(Year.MIN_VALUE, 1, 1)
        public val MAX: LocalDate = LocalDate(Year.MAX_VALUE, 12, 31)
        public val EPOCH: LocalDate = LocalDate(1970, 1, 1)

        /** Obtains a date from an ISO year, month, and day. */
        public fun of(year: Int, month: Month, dayOfMonth: Int): LocalDate =
            of(year, month.value, dayOfMonth)

        /** Obtains a date from an ISO year, month number, and day. */
        public fun of(year: Int, month: Int, dayOfMonth: Int): LocalDate {
            val validYear = ChronoField.YEAR.checkValidIntValue(year.toLong())
            val validMonth = ChronoField.MONTH_OF_YEAR.checkValidIntValue(month.toLong())
            ChronoField.DAY_OF_MONTH.checkValidIntValue(dayOfMonth.toLong())
            val monthValue = Month.of(validMonth)
            if (dayOfMonth > monthValue.length(Year.isLeap(validYear.toLong()))) {
                throw DateTimeException("Invalid date '$monthValue $dayOfMonth'")
            }
            return LocalDate(validYear, validMonth, dayOfMonth)
        }

        /** Obtains a date from an ISO year and one-based day-of-year. */
        public fun ofYearDay(year: Int, dayOfYear: Int): LocalDate {
            val validYear = ChronoField.YEAR.checkValidIntValue(year.toLong())
            ChronoField.DAY_OF_YEAR.checkValidIntValue(dayOfYear.toLong())
            val leap = Year.isLeap(validYear.toLong())
            if (dayOfYear == 366 && !leap) {
                throw DateTimeException("Invalid date 'DayOfYear 366' as '$year' is not a leap year")
            }
            var remaining = dayOfYear
            Month.entries.forEach { month ->
                val length = month.length(leap)
                if (remaining <= length) return LocalDate(validYear, month.value, remaining)
                remaining -= length
            }
            error("Validated day-of-year could not be resolved")
        }

        /** Obtains a date from the count of days since 1970-01-01. */
        public fun ofEpochDay(epochDay: Long): LocalDate {
            ChronoField.EPOCH_DAY.checkValidValue(epochDay)
            var zeroDay = epochDay + DAYS_0000_TO_1970 - 60
            var adjust = 0L
            if (zeroDay < 0) {
                val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                adjust = adjustCycles * 400
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
            }
            var yearEstimate = (400 * zeroDay + 591) / DAYS_PER_CYCLE
            var dayEstimate = zeroDay -
                (365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400)
            if (dayEstimate < 0) {
                yearEstimate--
                dayEstimate = zeroDay -
                    (365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400)
            }
            yearEstimate += adjust
            val marchDay = dayEstimate.toInt()
            val marchMonth = (marchDay * 5 + 2) / 153
            val month = (marchMonth + 2) % 12 + 1
            val day = marchDay - (marchMonth * 306 + 5) / 10 + 1
            yearEstimate += (marchMonth / 10).toLong()
            return LocalDate(
                ChronoField.YEAR.checkValidIntValue(yearEstimate),
                month,
                day,
            )
        }
    }
}
