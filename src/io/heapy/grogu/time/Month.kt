package io.heapy.grogu.time

/**
 * A month of the year in the ISO-8601 calendar system, from January to December.
 */
public enum class Month {
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
    }
}
