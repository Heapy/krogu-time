package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.LocalDate
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** Common date-based adjustment strategies. */
public object TemporalAdjusters {
    /** Creates an adjuster backed by a local-date transformation. */
    @JvmStatic
    public fun ofDateAdjuster(dateBasedAdjuster: (LocalDate) -> LocalDate): TemporalAdjuster =
        TemporalAdjuster { temporal ->
            temporal.with(dateBasedAdjuster(LocalDate.from(temporal)))
        }

    /** Returns an adjuster for the first day of the current month. */
    @JvmStatic
    public fun firstDayOfMonth(): TemporalAdjuster =
        TemporalAdjuster { it.with(ChronoField.DAY_OF_MONTH, 1) }

    /** Returns an adjuster for the last day of the current month. */
    @JvmStatic
    public fun lastDayOfMonth(): TemporalAdjuster = TemporalAdjuster {
        it.with(ChronoField.DAY_OF_MONTH, it.range(ChronoField.DAY_OF_MONTH).maximum)
    }

    /** Returns an adjuster for the first day of the next month. */
    @JvmStatic
    public fun firstDayOfNextMonth(): TemporalAdjuster = TemporalAdjuster {
        it.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS)
    }

    /** Returns an adjuster for the first day of the current year. */
    @JvmStatic
    public fun firstDayOfYear(): TemporalAdjuster =
        TemporalAdjuster { it.with(ChronoField.DAY_OF_YEAR, 1) }

    /** Returns an adjuster for the last day of the current year. */
    @JvmStatic
    public fun lastDayOfYear(): TemporalAdjuster = TemporalAdjuster {
        it.with(ChronoField.DAY_OF_YEAR, it.range(ChronoField.DAY_OF_YEAR).maximum)
    }

    /** Returns an adjuster for the first day of the next year. */
    @JvmStatic
    public fun firstDayOfNextYear(): TemporalAdjuster = TemporalAdjuster {
        it.with(ChronoField.DAY_OF_YEAR, 1).plus(1, ChronoUnit.YEARS)
    }

    /** Returns an adjuster for the first [dayOfWeek] in the current month. */
    @JvmStatic
    public fun firstInMonth(dayOfWeek: DayOfWeek): TemporalAdjuster =
        dayOfWeekInMonth(1, dayOfWeek)

    /** Returns an adjuster for the last [dayOfWeek] in the current month. */
    @JvmStatic
    public fun lastInMonth(dayOfWeek: DayOfWeek): TemporalAdjuster =
        dayOfWeekInMonth(-1, dayOfWeek)

    /** Returns an adjuster for an ordinal occurrence of [dayOfWeek] relative to the month. */
    @JvmStatic
    public fun dayOfWeekInMonth(ordinal: Int, dayOfWeek: DayOfWeek): TemporalAdjuster {
        val targetDay = dayOfWeek.value
        return if (ordinal >= 0) {
            TemporalAdjuster { temporal ->
                val first = temporal.with(ChronoField.DAY_OF_MONTH, 1)
                val currentDay = first.get(ChronoField.DAY_OF_WEEK)
                var days = (targetDay - currentDay + DAYS_PER_WEEK) % DAYS_PER_WEEK
                days += ((ordinal.toLong() - 1) * DAYS_PER_WEEK).toInt()
                first.plus(days.toLong(), ChronoUnit.DAYS)
            }
        } else {
            TemporalAdjuster { temporal ->
                val last = temporal.with(
                    ChronoField.DAY_OF_MONTH,
                    temporal.range(ChronoField.DAY_OF_MONTH).maximum,
                )
                val currentDay = last.get(ChronoField.DAY_OF_WEEK)
                var days = targetDay - currentDay
                if (days > 0) days -= DAYS_PER_WEEK
                days -= (((-ordinal).toLong() - 1) * DAYS_PER_WEEK).toInt()
                last.plus(days.toLong(), ChronoUnit.DAYS)
            }
        }
    }

    /** Returns an adjuster for the next occurrence of [dayOfWeek]. */
    @JvmStatic
    public fun next(dayOfWeek: DayOfWeek): TemporalAdjuster {
        val targetDay = dayOfWeek.value
        return TemporalAdjuster { temporal ->
            val currentDay = temporal.get(ChronoField.DAY_OF_WEEK)
            var days = targetDay - currentDay
            if (days <= 0) days += DAYS_PER_WEEK
            temporal.plus(days.toLong(), ChronoUnit.DAYS)
        }
    }

    /** Returns an adjuster for [dayOfWeek], retaining the date if it already matches. */
    @JvmStatic
    public fun nextOrSame(dayOfWeek: DayOfWeek): TemporalAdjuster {
        val targetDay = dayOfWeek.value
        return TemporalAdjuster { temporal ->
            val currentDay = temporal.get(ChronoField.DAY_OF_WEEK)
            if (currentDay == targetDay) {
                temporal
            } else {
                var days = targetDay - currentDay
                if (days < 0) days += DAYS_PER_WEEK
                temporal.plus(days.toLong(), ChronoUnit.DAYS)
            }
        }
    }

    /** Returns an adjuster for the previous occurrence of [dayOfWeek]. */
    @JvmStatic
    public fun previous(dayOfWeek: DayOfWeek): TemporalAdjuster {
        val targetDay = dayOfWeek.value
        return TemporalAdjuster { temporal ->
            val currentDay = temporal.get(ChronoField.DAY_OF_WEEK)
            var days = currentDay - targetDay
            if (days <= 0) days += DAYS_PER_WEEK
            temporal.minus(days.toLong(), ChronoUnit.DAYS)
        }
    }

    /** Returns an adjuster for [dayOfWeek], retaining the date if it already matches. */
    @JvmStatic
    public fun previousOrSame(dayOfWeek: DayOfWeek): TemporalAdjuster {
        val targetDay = dayOfWeek.value
        return TemporalAdjuster { temporal ->
            val currentDay = temporal.get(ChronoField.DAY_OF_WEEK)
            if (currentDay == targetDay) {
                temporal
            } else {
                var days = currentDay - targetDay
                if (days < 0) days += DAYS_PER_WEEK
                temporal.minus(days.toLong(), ChronoUnit.DAYS)
            }
        }
    }

    private const val DAYS_PER_WEEK: Int = 7
}
