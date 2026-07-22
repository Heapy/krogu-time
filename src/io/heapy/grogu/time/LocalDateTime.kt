package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.ValueRange

/** A date-time without a time-zone in the ISO-8601 calendar system. */
public class LocalDateTime private constructor(
    public val date: LocalDate,
    public val time: LocalTime,
) : TemporalAccessor, Comparable<LocalDateTime> {
    public val year: Int
        get() = date.year

    public val monthValue: Int
        get() = date.monthValue

    public val month: Month
        get() = date.month

    public val dayOfMonth: Int
        get() = date.dayOfMonth

    public val dayOfYear: Int
        get() = date.dayOfYear

    public val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    public val hour: Int
        get() = time.hour

    public val minute: Int
        get() = time.minute

    public val second: Int
        get() = time.second

    public val nano: Int
        get() = time.nano

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) {
            field.isDateBased || field.isTimeBased
        } else {
            field.isSupportedBy(this)
        }

    override fun range(field: TemporalField): ValueRange = when (field) {
        is ChronoField -> if (field.isTimeBased) time.range(field) else date.range(field)
        else -> field.rangeRefinedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        is ChronoField -> if (field.isTimeBased) time.getLong(field) else date.getLong(field)
        else -> field.getFrom(this)
    }

    /** Returns the local-date part. */
    public fun toLocalDate(): LocalDate = date

    /** Returns the local-time part. */
    public fun toLocalTime(): LocalTime = time

    override fun compareTo(other: LocalDateTime): Int {
        val dateComparison = date.compareTo(other.date)
        return if (dateComparison != 0) dateComparison else time.compareTo(other.time)
    }

    /** Whether this date-time is after [other] on the local timeline. */
    public fun isAfter(other: LocalDateTime): Boolean = compareTo(other) > 0

    /** Whether this date-time is before [other] on the local timeline. */
    public fun isBefore(other: LocalDateTime): Boolean = compareTo(other) < 0

    /** Whether this date-time represents the same local date and time as [other]. */
    public fun isEqual(other: LocalDateTime): Boolean = compareTo(other) == 0

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LocalDateTime &&
            date == other.date &&
            time == other.time

    override fun hashCode(): Int = date.hashCode() xor time.hashCode()

    override fun toString(): String = "${date}T$time"

    public companion object {
        public val MIN: LocalDateTime = LocalDateTime(LocalDate.MIN, LocalTime.MIN)
        public val MAX: LocalDateTime = LocalDateTime(LocalDate.MAX, LocalTime.MAX)

        public fun of(
            year: Int,
            month: Month,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
        ): LocalDateTime = of(year, month, dayOfMonth, hour, minute, 0, 0)

        public fun of(
            year: Int,
            month: Month,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
        ): LocalDateTime = of(year, month, dayOfMonth, hour, minute, second, 0)

        public fun of(
            year: Int,
            month: Month,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
        ): LocalDateTime = of(
            LocalDate.of(year, month, dayOfMonth),
            LocalTime.of(hour, minute, second, nanoOfSecond),
        )

        public fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
        ): LocalDateTime = of(year, month, dayOfMonth, hour, minute, 0, 0)

        public fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
        ): LocalDateTime = of(year, month, dayOfMonth, hour, minute, second, 0)

        public fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
        ): LocalDateTime = of(
            LocalDate.of(year, month, dayOfMonth),
            LocalTime.of(hour, minute, second, nanoOfSecond),
        )

        /** Combines an existing local date and local time. */
        public fun of(date: LocalDate, time: LocalTime): LocalDateTime =
            LocalDateTime(date, time)

        /** Obtains a local date-time from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): LocalDateTime {
            if (temporal is LocalDateTime) return temporal
            return try {
                of(LocalDate.from(temporal), LocalTime.from(temporal))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain LocalDateTime from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }
    }
}
