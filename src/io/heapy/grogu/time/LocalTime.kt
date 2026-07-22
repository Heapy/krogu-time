package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/** A time without a date or time-zone in the ISO-8601 calendar system. */
public class LocalTime private constructor(
    public val hour: Int,
    public val minute: Int,
    public val second: Int,
    public val nano: Int,
) : TemporalAccessor, Comparable<LocalTime> {
    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) field.isTimeBased else field.isSupportedBy(this)

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.NANO_OF_SECOND -> nano.toLong()
        ChronoField.NANO_OF_DAY -> toNanoOfDay()
        ChronoField.MICRO_OF_SECOND -> (nano / NANOS_PER_MICRO).toLong()
        ChronoField.MICRO_OF_DAY -> toNanoOfDay() / NANOS_PER_MICRO
        ChronoField.MILLI_OF_SECOND -> (nano / NANOS_PER_MILLI).toLong()
        ChronoField.MILLI_OF_DAY -> toNanoOfDay() / NANOS_PER_MILLI
        ChronoField.SECOND_OF_MINUTE -> second.toLong()
        ChronoField.SECOND_OF_DAY -> toSecondOfDay().toLong()
        ChronoField.MINUTE_OF_HOUR -> minute.toLong()
        ChronoField.MINUTE_OF_DAY -> (hour * MINUTES_PER_HOUR + minute).toLong()
        ChronoField.HOUR_OF_AMPM -> (hour % HOURS_PER_AMPM).toLong()
        ChronoField.CLOCK_HOUR_OF_AMPM -> {
            val hourOfAmPm = hour % HOURS_PER_AMPM
            (if (hourOfAmPm == 0) HOURS_PER_AMPM else hourOfAmPm).toLong()
        }
        ChronoField.HOUR_OF_DAY -> hour.toLong()
        ChronoField.CLOCK_HOUR_OF_DAY -> (if (hour == 0) HOURS_PER_DAY else hour).toLong()
        ChronoField.AMPM_OF_DAY -> (hour / HOURS_PER_AMPM).toLong()
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    /** Converts this time to the whole second within the day. */
    public fun toSecondOfDay(): Int =
        hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second

    /** Converts this time to the nanosecond within the day. */
    public fun toNanoOfDay(): Long =
        hour * NANOS_PER_HOUR +
            minute * NANOS_PER_MINUTE +
            second * NANOS_PER_SECOND +
            nano

    override fun compareTo(other: LocalTime): Int {
        val hourComparison = hour.compareTo(other.hour)
        if (hourComparison != 0) return hourComparison
        val minuteComparison = minute.compareTo(other.minute)
        if (minuteComparison != 0) return minuteComparison
        val secondComparison = second.compareTo(other.second)
        return if (secondComparison != 0) secondComparison else nano.compareTo(other.nano)
    }

    /** Whether this time is after [other] within the day. */
    public fun isAfter(other: LocalTime): Boolean = compareTo(other) > 0

    /** Whether this time is before [other] within the day. */
    public fun isBefore(other: LocalTime): Boolean = compareTo(other) < 0

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LocalTime &&
            hour == other.hour &&
            minute == other.minute &&
            second == other.second &&
            nano == other.nano

    override fun hashCode(): Int = toNanoOfDay().hashCode()

    override fun toString(): String = buildString {
        appendTwoDigits(hour)
        append(':')
        appendTwoDigits(minute)
        if (second != 0 || nano != 0) {
            append(':')
            appendTwoDigits(second)
            if (nano != 0) {
                append('.')
                when {
                    nano % NANOS_PER_MILLI == 0 ->
                        append((nano / NANOS_PER_MILLI).toString().padStart(3, '0'))
                    nano % NANOS_PER_MICRO == 0 ->
                        append((nano / NANOS_PER_MICRO).toString().padStart(6, '0'))
                    else -> append(nano.toString().padStart(9, '0'))
                }
            }
        }
    }

    private fun StringBuilder.appendTwoDigits(value: Int) {
        if (value < 10) append('0')
        append(value)
    }

    public companion object {
        private const val HOURS_PER_DAY: Int = 24
        private const val HOURS_PER_AMPM: Int = 12
        private const val MINUTES_PER_HOUR: Int = 60
        private const val SECONDS_PER_MINUTE: Int = 60
        private const val SECONDS_PER_HOUR: Int = 3_600
        private const val NANOS_PER_MICRO: Int = 1_000
        private const val NANOS_PER_MILLI: Int = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
        private const val NANOS_PER_HOUR: Long = 3_600 * NANOS_PER_SECOND

        public val MIN: LocalTime = LocalTime(0, 0, 0, 0)
        public val MAX: LocalTime = LocalTime(23, 59, 59, 999_999_999)
        public val MIDNIGHT: LocalTime = MIN
        public val NOON: LocalTime = LocalTime(12, 0, 0, 0)

        /** Obtains a time from an hour and minute. */
        public fun of(hour: Int, minute: Int): LocalTime = of(hour, minute, 0, 0)

        /** Obtains a time from an hour, minute, and second. */
        public fun of(hour: Int, minute: Int, second: Int): LocalTime =
            of(hour, minute, second, 0)

        /** Obtains a time from hour, minute, second, and nanosecond components. */
        public fun of(hour: Int, minute: Int, second: Int, nanoOfSecond: Int): LocalTime {
            val validHour = ChronoField.HOUR_OF_DAY.checkValidIntValue(hour.toLong())
            val validMinute = ChronoField.MINUTE_OF_HOUR.checkValidIntValue(minute.toLong())
            val validSecond = ChronoField.SECOND_OF_MINUTE.checkValidIntValue(second.toLong())
            val validNano = ChronoField.NANO_OF_SECOND.checkValidIntValue(nanoOfSecond.toLong())
            return create(validHour, validMinute, validSecond, validNano)
        }

        /** Obtains a time from the whole second within the day. */
        public fun ofSecondOfDay(secondOfDay: Long): LocalTime {
            ChronoField.SECOND_OF_DAY.checkValidValue(secondOfDay)
            var remaining = secondOfDay
            val hour = (remaining / SECONDS_PER_HOUR).toInt()
            remaining -= hour * SECONDS_PER_HOUR
            val minute = (remaining / SECONDS_PER_MINUTE).toInt()
            remaining -= minute * SECONDS_PER_MINUTE
            return create(hour, minute, remaining.toInt(), 0)
        }

        /** Obtains a time from the nanosecond within the day. */
        public fun ofNanoOfDay(nanoOfDay: Long): LocalTime {
            ChronoField.NANO_OF_DAY.checkValidValue(nanoOfDay)
            var remaining = nanoOfDay
            val hour = (remaining / NANOS_PER_HOUR).toInt()
            remaining -= hour * NANOS_PER_HOUR
            val minute = (remaining / NANOS_PER_MINUTE).toInt()
            remaining -= minute * NANOS_PER_MINUTE
            val second = (remaining / NANOS_PER_SECOND).toInt()
            remaining -= second * NANOS_PER_SECOND
            return create(hour, minute, second, remaining.toInt())
        }

        /** Obtains a time from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): LocalTime {
            if (temporal is LocalTime) return temporal
            return try {
                ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain LocalTime from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        private fun create(hour: Int, minute: Int, second: Int, nano: Int): LocalTime =
            when {
                minute == 0 && second == 0 && nano == 0 && hour == 0 -> MIDNIGHT
                minute == 0 && second == 0 && nano == 0 && hour == 12 -> NOON
                else -> LocalTime(hour, minute, second, nano)
            }
    }
}
