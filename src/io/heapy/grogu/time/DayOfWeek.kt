package io.heapy.grogu.time

/**
 * A day of the week in the ISO-8601 calendar system, from Monday to Sunday.
 *
 * The enum declaration order and [value] both follow ISO-8601: Monday is 1
 * and Sunday is 7.
 */
public enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    /** The ISO-8601 day number, from 1 (Monday) to 7 (Sunday). */
    public val value: Int
        get() = ordinal + 1

    /** Returns this day with [days] added, wrapping around the seven-day week. */
    public fun plus(days: Long): DayOfWeek {
        val amount = (days % DAYS_PER_WEEK).toInt()
        return entries[(ordinal + amount + DAYS_PER_WEEK) % DAYS_PER_WEEK]
    }

    /** Returns this day with [days] subtracted, wrapping around the seven-day week. */
    public fun minus(days: Long): DayOfWeek = plus(-(days % DAYS_PER_WEEK))

    public companion object {
        private const val DAYS_PER_WEEK: Int = 7

        /** Returns the day identified by its ISO-8601 value. */
        public fun of(dayOfWeek: Int): DayOfWeek {
            if (dayOfWeek !in 1..DAYS_PER_WEEK) {
                throw DateTimeException("Invalid value for DayOfWeek: $dayOfWeek")
            }
            return entries[dayOfWeek - 1]
        }
    }
}
