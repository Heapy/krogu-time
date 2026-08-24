package io.heapy.krogu.time.zone

import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.Month
import io.heapy.krogu.time.Year
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.TemporalAdjusters

/** A recurring rule that creates a zone-offset transition for a year. */
public class ZoneOffsetTransitionRule private constructor(
    public val month: Month,
    public val dayOfMonthIndicator: Int,
    public val dayOfWeek: DayOfWeek?,
    public val localTime: LocalTime,
    public val isMidnightEndOfDay: Boolean,
    public val timeDefinition: TimeDefinition,
    public val standardOffset: ZoneOffset,
    public val offsetBefore: ZoneOffset,
    public val offsetAfter: ZoneOffset,
) {
    /** Defines the time scale used to express a recurring transition. */
    public enum class TimeDefinition {
        UTC,
        WALL,
        STANDARD;

        /** Converts [dateTime] from this definition to wall time. */
        public fun createDateTime(
            dateTime: LocalDateTime,
            standardOffset: ZoneOffset,
            wallOffset: ZoneOffset,
        ): LocalDateTime = when (this) {
            UTC -> dateTime.plusSeconds(wallOffset.totalSeconds.toLong())
            WALL -> dateTime
            STANDARD -> dateTime.plusSeconds(
                (wallOffset.totalSeconds - standardOffset.totalSeconds).toLong(),
            )
        }
    }

    /** Creates the transition produced by this rule in [year]. */
    public fun createTransition(year: Int): ZoneOffsetTransition {
        var date = if (dayOfMonthIndicator < 0) {
            val day = month.length(Year.isLeap(year.toLong())) + 1 + dayOfMonthIndicator
            LocalDate.of(year, month, day).let { initial ->
                dayOfWeek?.let { initial.with(TemporalAdjusters.previousOrSame(it)) } ?: initial
            }
        } else {
            LocalDate.of(year, month, dayOfMonthIndicator).let { initial ->
                dayOfWeek?.let { initial.with(TemporalAdjusters.nextOrSame(it)) } ?: initial
            }
        }
        if (isMidnightEndOfDay) date = date.plusDays(1)
        val transitionDateTime = timeDefinition.createDateTime(
            LocalDateTime.of(date, localTime),
            standardOffset,
            offsetBefore,
        )
        return ZoneOffsetTransition(transitionDateTime, offsetBefore, offsetAfter)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ZoneOffsetTransitionRule &&
            month == other.month &&
            dayOfMonthIndicator == other.dayOfMonthIndicator &&
            dayOfWeek == other.dayOfWeek &&
            localTime == other.localTime &&
            isMidnightEndOfDay == other.isMidnightEndOfDay &&
            timeDefinition == other.timeDefinition &&
            standardOffset == other.standardOffset &&
            offsetBefore == other.offsetBefore &&
            offsetAfter == other.offsetAfter

    override fun hashCode(): Int {
        var hash = (localTime.toSecondOfDay() + if (isMidnightEndOfDay) 1 else 0) shl 15
        hash += month.ordinal shl 11
        hash += (dayOfMonthIndicator + 32) shl 5
        hash += (dayOfWeek?.ordinal ?: 7) shl 2
        hash += timeDefinition.ordinal
        return hash xor standardOffset.hashCode() xor offsetBefore.hashCode() xor offsetAfter.hashCode()
    }

    override fun toString(): String = buildString {
        append("TransitionRule[")
        append(if (offsetAfter.totalSeconds > offsetBefore.totalSeconds) "Gap " else "Overlap ")
        append(offsetBefore)
        append(" to ")
        append(offsetAfter)
        append(", ")
        when {
            dayOfWeek == null -> {
                append(month.name)
                append(' ')
                append(dayOfMonthIndicator)
            }
            dayOfMonthIndicator == -1 -> {
                append(dayOfWeek.name)
                append(" on or before last day of ")
                append(month.name)
            }
            dayOfMonthIndicator < 0 -> {
                append(dayOfWeek.name)
                append(" on or before last day minus ")
                append(-dayOfMonthIndicator - 1)
                append(" of ")
                append(month.name)
            }
            else -> {
                append(dayOfWeek.name)
                append(" on or after ")
                append(month.name)
                append(' ')
                append(dayOfMonthIndicator)
            }
        }
        append(" at ")
        append(if (isMidnightEndOfDay) "24:00" else localTime.toString())
        append(' ')
        append(timeDefinition)
        append(", standard offset ")
        append(standardOffset)
        append(']')
    }

    public companion object {
        /** Creates a validated recurring transition rule. */
        public fun of(
            month: Month,
            dayOfMonthIndicator: Int,
            dayOfWeek: DayOfWeek?,
            localTime: LocalTime,
            midnightEndOfDay: Boolean,
            timeDefinition: TimeDefinition,
            standardOffset: ZoneOffset,
            offsetBefore: ZoneOffset,
            offsetAfter: ZoneOffset,
        ): ZoneOffsetTransitionRule {
            require(dayOfMonthIndicator in -28..31 && dayOfMonthIndicator != 0) {
                "Day of month indicator must be between -28 and 31 inclusive excluding zero"
            }
            require(!midnightEndOfDay || localTime == LocalTime.MIDNIGHT) {
                "Time must be midnight when end of day flag is true"
            }
            require(localTime.nano == 0) { "Time's nano-of-second must be zero" }
            return ZoneOffsetTransitionRule(
                month,
                dayOfMonthIndicator,
                dayOfWeek,
                localTime,
                midnightEndOfDay,
                timeDefinition,
                standardOffset,
                offsetBefore,
                offsetAfter,
            )
        }
    }
}
