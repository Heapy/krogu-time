package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeFormatter
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.internal.floorMod
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

/** A time without a date or time-zone in the ISO-8601 calendar system. */
public class LocalTime private constructor(
    public val hour: Int,
    public val minute: Int,
    public val second: Int,
    public val nano: Int,
) : Temporal, TemporalAdjuster, Comparable<LocalTime> {
    override fun isSupported(field: TemporalField?): Boolean =
        if (field is ChronoField) field.isTimeBased else field != null && field.isSupportedBy(this)

    override fun isSupported(unit: TemporalUnit?): Boolean =
        if (unit is ChronoUnit) unit.isTimeBased else unit != null && unit.isSupportedBy(this)

    override fun get(field: TemporalField): Int = when (field) {
        ChronoField.NANO_OF_SECOND -> nano
        ChronoField.NANO_OF_DAY -> throw UnsupportedTemporalTypeException(
            "Invalid field 'NanoOfDay' for get() method, use getLong() instead",
        )
        ChronoField.MICRO_OF_SECOND -> nano / NANOS_PER_MICRO
        ChronoField.MICRO_OF_DAY -> throw UnsupportedTemporalTypeException(
            "Invalid field 'MicroOfDay' for get() method, use getLong() instead",
        )
        ChronoField.MILLI_OF_SECOND -> nano / NANOS_PER_MILLI
        ChronoField.MILLI_OF_DAY -> (toNanoOfDay() / NANOS_PER_MILLI).toInt()
        ChronoField.SECOND_OF_MINUTE -> second
        ChronoField.SECOND_OF_DAY -> toSecondOfDay()
        ChronoField.MINUTE_OF_HOUR -> minute
        ChronoField.MINUTE_OF_DAY -> hour * MINUTES_PER_HOUR + minute
        ChronoField.HOUR_OF_AMPM -> hour % HOURS_PER_AMPM
        ChronoField.CLOCK_HOUR_OF_AMPM -> {
            val hourOfAmPm = hour % HOURS_PER_AMPM
            if (hourOfAmPm == 0) HOURS_PER_AMPM else hourOfAmPm
        }
        ChronoField.HOUR_OF_DAY -> hour
        ChronoField.CLOCK_HOUR_OF_DAY -> if (hour == 0) HOURS_PER_DAY else hour
        ChronoField.AMPM_OF_DAY -> hour / HOURS_PER_AMPM
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> super<Temporal>.get(field)
    }

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

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.localTime()) {
            @Suppress("UNCHECKED_CAST")
            return this as R
        }
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.NANOS as R
        }
        return super<Temporal>.query(query)
    }

    override fun with(adjuster: TemporalAdjuster): LocalTime =
        if (adjuster is LocalTime) adjuster else adjuster.adjustInto(this) as LocalTime

    override fun with(field: TemporalField, newValue: Long): LocalTime {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        field.checkValidValue(newValue)
        return when (field) {
            ChronoField.NANO_OF_SECOND -> withNano(newValue.toInt())
            ChronoField.NANO_OF_DAY -> ofNanoOfDay(newValue)
            ChronoField.MICRO_OF_SECOND -> withNano(newValue.toInt() * NANOS_PER_MICRO)
            ChronoField.MICRO_OF_DAY -> ofNanoOfDay(newValue * NANOS_PER_MICRO)
            ChronoField.MILLI_OF_SECOND -> withNano(newValue.toInt() * NANOS_PER_MILLI)
            ChronoField.MILLI_OF_DAY -> ofNanoOfDay(newValue * NANOS_PER_MILLI)
            ChronoField.SECOND_OF_MINUTE -> withSecond(newValue.toInt())
            ChronoField.SECOND_OF_DAY -> plusSeconds(newValue - toSecondOfDay())
            ChronoField.MINUTE_OF_HOUR -> withMinute(newValue.toInt())
            ChronoField.MINUTE_OF_DAY -> plusMinutes(
                newValue - (hour * MINUTES_PER_HOUR + minute),
            )
            ChronoField.HOUR_OF_AMPM -> plusHours(newValue - hour % HOURS_PER_AMPM)
            ChronoField.CLOCK_HOUR_OF_AMPM -> plusHours(
                (if (newValue == HOURS_PER_AMPM.toLong()) 0 else newValue) -
                    hour % HOURS_PER_AMPM,
            )
            ChronoField.HOUR_OF_DAY -> withHour(newValue.toInt())
            ChronoField.CLOCK_HOUR_OF_DAY -> withHour(
                if (newValue == HOURS_PER_DAY.toLong()) 0 else newValue.toInt(),
            )
            ChronoField.AMPM_OF_DAY -> plusHours(
                (newValue - hour / HOURS_PER_AMPM) * HOURS_PER_AMPM,
            )
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
    }

    /** Returns this time with the hour changed. */
    public fun withHour(hour: Int): LocalTime {
        if (this.hour == hour) return this
        val validHour = ChronoField.HOUR_OF_DAY.checkValidIntValue(hour.toLong())
        return create(validHour, minute, second, nano)
    }

    /** Returns this time with the minute changed. */
    public fun withMinute(minute: Int): LocalTime {
        if (this.minute == minute) return this
        val validMinute = ChronoField.MINUTE_OF_HOUR.checkValidIntValue(minute.toLong())
        return create(hour, validMinute, second, nano)
    }

    /** Returns this time with the second changed. */
    public fun withSecond(second: Int): LocalTime {
        if (this.second == second) return this
        val validSecond = ChronoField.SECOND_OF_MINUTE.checkValidIntValue(second.toLong())
        return create(hour, minute, validSecond, nano)
    }

    /** Returns this time with the nanosecond changed. */
    public fun withNano(nanoOfSecond: Int): LocalTime {
        if (nano == nanoOfSecond) return this
        val validNano = ChronoField.NANO_OF_SECOND.checkValidIntValue(nanoOfSecond.toLong())
        return create(hour, minute, second, validNano)
    }

    /** Truncates this time to the nearest preceding multiple of [unit]. */
    public fun truncatedTo(unit: TemporalUnit): LocalTime {
        if (unit === ChronoUnit.NANOS) return this
        val unitDuration = unit.duration
        if (unitDuration.seconds > SECONDS_PER_DAY) {
            throw UnsupportedTemporalTypeException("Unit is too large to be used for truncation")
        }
        val unitNanos = unitDuration.toNanos()
        if (NANOS_PER_DAY % unitNanos != 0L) {
            throw UnsupportedTemporalTypeException(
                "Unit must divide into a standard day without remainder",
            )
        }
        return ofNanoOfDay(toNanoOfDay() / unitNanos * unitNanos)
    }

    override fun plus(amount: TemporalAmount): LocalTime = amount.addTo(this) as LocalTime

    override fun plus(amountToAdd: Long, unit: TemporalUnit): LocalTime {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        return when (unit) {
            ChronoUnit.NANOS -> plusNanos(amountToAdd)
            ChronoUnit.MICROS -> plusNanos(
                amountToAdd % MICROS_PER_DAY * NANOS_PER_MICRO,
            )
            ChronoUnit.MILLIS -> plusNanos(
                amountToAdd % MILLIS_PER_DAY * NANOS_PER_MILLI,
            )
            ChronoUnit.SECONDS -> plusSeconds(amountToAdd)
            ChronoUnit.MINUTES -> plusMinutes(amountToAdd)
            ChronoUnit.HOURS -> plusHours(amountToAdd)
            ChronoUnit.HALF_DAYS -> plusHours(amountToAdd % 2 * HOURS_PER_AMPM)
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Returns this time with hours added, wrapping at midnight. */
    public fun plusHours(hoursToAdd: Long): LocalTime {
        if (hoursToAdd % HOURS_PER_DAY == 0L) return this
        val newHour = floorMod(hour + hoursToAdd % HOURS_PER_DAY, HOURS_PER_DAY.toLong()).toInt()
        return create(newHour, minute, second, nano)
    }

    /** Returns this time with minutes added, wrapping at midnight. */
    public fun plusMinutes(minutesToAdd: Long): LocalTime {
        if (minutesToAdd % MINUTES_PER_DAY == 0L) return this
        val minuteOfDay = hour * MINUTES_PER_HOUR + minute
        val newMinuteOfDay = floorMod(
            minuteOfDay + minutesToAdd % MINUTES_PER_DAY,
            MINUTES_PER_DAY.toLong(),
        ).toInt()
        if (newMinuteOfDay == minuteOfDay) return this
        return create(
            newMinuteOfDay / MINUTES_PER_HOUR,
            newMinuteOfDay % MINUTES_PER_HOUR,
            second,
            nano,
        )
    }

    /** Returns this time with seconds added, wrapping at midnight. */
    public fun plusSeconds(secondsToAdd: Long): LocalTime {
        if (secondsToAdd % SECONDS_PER_DAY == 0L) return this
        val secondOfDay = toSecondOfDay()
        val newSecondOfDay = floorMod(
            secondOfDay + secondsToAdd % SECONDS_PER_DAY,
            SECONDS_PER_DAY.toLong(),
        ).toInt()
        if (newSecondOfDay == secondOfDay) return this
        return create(
            newSecondOfDay / SECONDS_PER_HOUR,
            newSecondOfDay / SECONDS_PER_MINUTE % MINUTES_PER_HOUR,
            newSecondOfDay % SECONDS_PER_MINUTE,
            nano,
        )
    }

    /** Returns this time with nanoseconds added, wrapping at midnight. */
    public fun plusNanos(nanosToAdd: Long): LocalTime {
        if (nanosToAdd % NANOS_PER_DAY == 0L) return this
        val nanoOfDay = toNanoOfDay()
        val newNanoOfDay = floorMod(
            nanoOfDay + nanosToAdd % NANOS_PER_DAY,
            NANOS_PER_DAY,
        )
        return if (newNanoOfDay == nanoOfDay) this else ofNanoOfDay(newNanoOfDay)
    }

    override fun minus(amount: TemporalAmount): LocalTime = amount.subtractFrom(this) as LocalTime

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): LocalTime =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    /** Returns this time with hours subtracted, wrapping at midnight. */
    public fun minusHours(hoursToSubtract: Long): LocalTime =
        plusHours(-(hoursToSubtract % HOURS_PER_DAY))

    /** Returns this time with minutes subtracted, wrapping at midnight. */
    public fun minusMinutes(minutesToSubtract: Long): LocalTime =
        plusMinutes(-(minutesToSubtract % MINUTES_PER_DAY))

    /** Returns this time with seconds subtracted, wrapping at midnight. */
    public fun minusSeconds(secondsToSubtract: Long): LocalTime =
        plusSeconds(-(secondsToSubtract % SECONDS_PER_DAY))

    /** Returns this time with nanoseconds subtracted, wrapping at midnight. */
    public fun minusNanos(nanosToSubtract: Long): LocalTime =
        plusNanos(-(nanosToSubtract % NANOS_PER_DAY))

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.NANO_OF_DAY, toNanoOfDay())

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        val nanosUntil = end.toNanoOfDay() - toNanoOfDay()
        return when (unit) {
            ChronoUnit.NANOS -> nanosUntil
            ChronoUnit.MICROS -> nanosUntil / NANOS_PER_MICRO
            ChronoUnit.MILLIS -> nanosUntil / NANOS_PER_MILLI
            ChronoUnit.SECONDS -> nanosUntil / NANOS_PER_SECOND
            ChronoUnit.MINUTES -> nanosUntil / NANOS_PER_MINUTE
            ChronoUnit.HOURS -> nanosUntil / NANOS_PER_HOUR
            ChronoUnit.HALF_DAYS -> nanosUntil / (HOURS_PER_AMPM * NANOS_PER_HOUR)
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Combines this time with [date]. */
    public fun atDate(date: LocalDate): LocalDateTime = LocalDateTime.of(date, this)

    /** Combines this time with [offset]. */
    public fun atOffset(offset: ZoneOffset): OffsetTime = OffsetTime.of(this, offset)

    /** Converts this time to the whole second within the day. */
    public fun toSecondOfDay(): Int =
        hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second

    /** Converts this time to the nanosecond within the day. */
    public fun toNanoOfDay(): Long =
        hour * NANOS_PER_HOUR +
            minute * NANOS_PER_MINUTE +
            second * NANOS_PER_SECOND +
            nano

    /** Combines this time with [date] and [offset] as a Unix epoch-second value. */
    public fun toEpochSecond(date: LocalDate, offset: ZoneOffset): Long =
        date.toEpochDay() * SECONDS_PER_DAY + toSecondOfDay() - offset.totalSeconds

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

    /** Formats this time using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    private fun StringBuilder.appendTwoDigits(value: Int) {
        if (value < 10) append('0')
        append(value)
    }

    public companion object {
        private const val HOURS_PER_DAY: Int = 24
        private const val HOURS_PER_AMPM: Int = 12
        private const val MINUTES_PER_HOUR: Int = 60
        private const val MINUTES_PER_DAY: Int = 24 * MINUTES_PER_HOUR
        private const val SECONDS_PER_MINUTE: Int = 60
        private const val SECONDS_PER_HOUR: Int = 3_600
        private const val SECONDS_PER_DAY: Int = 24 * SECONDS_PER_HOUR
        private const val NANOS_PER_MICRO: Int = 1_000
        private const val NANOS_PER_MILLI: Int = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
        private const val NANOS_PER_HOUR: Long = 3_600 * NANOS_PER_SECOND
        private const val NANOS_PER_DAY: Long = 24 * NANOS_PER_HOUR
        private const val MICROS_PER_DAY: Long = NANOS_PER_DAY / NANOS_PER_MICRO
        private const val MILLIS_PER_DAY: Long = NANOS_PER_DAY / NANOS_PER_MILLI

        public val MIN: LocalTime = LocalTime(0, 0, 0, 0)
        public val MAX: LocalTime = LocalTime(23, 59, 59, 999_999_999)
        public val MIDNIGHT: LocalTime = MIN
        public val NOON: LocalTime = LocalTime(12, 0, 0, 0)

        /** Obtains the current time using the system clock in the default time-zone. */
        public fun now(): LocalTime = now(Clock.systemDefaultZone())

        /** Obtains the current time using the system clock in [zone]. */
        public fun now(zone: ZoneId): LocalTime = now(Clock.system(zone))

        /**
         * Obtains the current time from [clock]. Derived directly from the clock's
         * instant and zone offset without constructing a date, so it stays valid at
         * the extreme ends of the instant range (matching Java's `LocalTime.now`).
         */
        public fun now(clock: Clock): LocalTime = ofInstant(clock.instant(), clock.zone)

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

        /** Obtains the local time at [instant] in [zone]. */
        public fun ofInstant(instant: Instant, zone: ZoneId): LocalTime {
            val offset = zone.rules.getOffset(instant)
            val localSecond = instant.epochSecond + offset.totalSeconds
            val secondOfDay = floorMod(localSecond, SECONDS_PER_DAY.toLong())
            return ofNanoOfDay(secondOfDay * NANOS_PER_SECOND + instant.nano)
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

        /** Parses a time using the strict ISO local-time format. */
        public fun parse(text: CharSequence): LocalTime {
            val input = text.toString()
            if (!hasTwoDigits(input, 0)) throw parseFailure(input, 0)
            if (input.length <= 2 || input[2] != ':') throw parseFailure(input, 2)
            if (!hasTwoDigits(input, 3)) throw parseFailure(input, 3)

            val hour = parseTwoDigits(input, 0)
            val minute = parseTwoDigits(input, 3)
            if (input.length == 5) return createParsed(input, hour, minute, 0, 0)
            if (input[5] != ':') throw parseFailure(input, 5)
            if (!hasTwoDigits(input, 6)) throw parseFailure(input, 5)

            val second = parseTwoDigits(input, 6)
            if (input.length == 8) return createParsed(input, hour, minute, second, 0)
            if (input[8] != '.') throw parseFailure(input, 8)

            var index = 9
            var digits = 0
            var nano = 0
            while (index < input.length && digits < 9 && input[index].isAsciiDigit()) {
                nano = nano * 10 + (input[index] - '0')
                index++
                digits++
            }
            if (index != input.length) throw parseFailure(input, index)
            repeat(9 - digits) { nano *= 10 }
            return createParsed(input, hour, minute, second, nano)
        }

        /** Parses a time from [text] using [formatter]. */
        public fun parse(text: CharSequence, formatter: DateTimeFormatter): LocalTime =
            from(formatter.parse(text))

        private fun createParsed(
            input: String,
            hour: Int,
            minute: Int,
            second: Int,
            nano: Int,
        ): LocalTime = try {
            of(hour, minute, second, nano)
        } catch (exception: DateTimeException) {
            throw DateTimeParseException(
                "Text cannot be parsed to a LocalTime",
                input,
                0,
                exception,
            )
        }

        private fun hasTwoDigits(input: String, index: Int): Boolean =
            index + 1 < input.length &&
                input[index].isAsciiDigit() &&
                input[index + 1].isAsciiDigit()

        private fun parseTwoDigits(input: String, index: Int): Int =
            (input[index] - '0') * 10 + (input[index + 1] - '0')

        private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a LocalTime", input, errorIndex)

        private fun create(hour: Int, minute: Int, second: Int, nano: Int): LocalTime =
            when {
                minute == 0 && second == 0 && nano == 0 && hour == 0 -> MIDNIGHT
                minute == 0 && second == 0 && nano == 0 && hour == 12 -> NOON
                else -> LocalTime(hour, minute, second, nano)
            }
    }
}
