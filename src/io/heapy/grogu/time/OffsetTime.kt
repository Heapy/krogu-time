package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeParseException
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

/** A time with a fixed offset from UTC. */
public class OffsetTime private constructor(
    public val time: LocalTime,
    public val offset: ZoneOffset,
) : Temporal, TemporalAdjuster, Comparable<OffsetTime> {
    public val hour: Int get() = time.hour
    public val minute: Int get() = time.minute
    public val second: Int get() = time.second
    public val nano: Int get() = time.nano

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) field.isTimeBased || field === ChronoField.OFFSET_SECONDS else
            field.isSupportedBy(this)

    override fun isSupported(unit: TemporalUnit): Boolean =
        if (unit is ChronoUnit) unit.isTimeBased else unit.isSupportedBy(this)

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.OFFSET_SECONDS -> offset.totalSeconds.toLong()
        is ChronoField -> time.getLong(field)
        else -> field.getFrom(this)
    }

    /** Returns a copy at [offset] without changing the local time. */
    public fun withOffsetSameLocal(offset: ZoneOffset): OffsetTime = with(time, offset)

    /** Returns a copy at [offset] that represents the same point on the daily timeline. */
    public fun withOffsetSameInstant(offset: ZoneOffset): OffsetTime {
        if (this.offset == offset) return this
        val difference = offset.totalSeconds - this.offset.totalSeconds
        return OffsetTime(time.plusSeconds(difference.toLong()), offset)
    }

    public fun toLocalTime(): LocalTime = time

    override fun with(adjuster: TemporalAdjuster): OffsetTime = when (adjuster) {
        is LocalTime -> with(adjuster, offset)
        is ZoneOffset -> with(time, adjuster)
        is OffsetTime -> adjuster
        else -> adjuster.adjustInto(this) as OffsetTime
    }

    override fun with(field: TemporalField, newValue: Long): OffsetTime {
        if (field === ChronoField.OFFSET_SECONDS) {
            return with(time, ZoneOffset.ofTotalSeconds(field.range.checkValidIntValue(newValue, field)))
        }
        return with(time.with(field, newValue), offset)
    }

    public fun withHour(hour: Int): OffsetTime = with(time.withHour(hour), offset)
    public fun withMinute(minute: Int): OffsetTime = with(time.withMinute(minute), offset)
    public fun withSecond(second: Int): OffsetTime = with(time.withSecond(second), offset)
    public fun withNano(nanoOfSecond: Int): OffsetTime = with(time.withNano(nanoOfSecond), offset)
    public fun truncatedTo(unit: TemporalUnit): OffsetTime = with(time.truncatedTo(unit), offset)

    override fun plus(amount: TemporalAmount): OffsetTime = amount.addTo(this) as OffsetTime

    override fun plus(amountToAdd: Long, unit: TemporalUnit): OffsetTime =
        if (unit is ChronoUnit) with(time.plus(amountToAdd, unit), offset) else
            unit.addTo(this, amountToAdd)

    public fun plusHours(hoursToAdd: Long): OffsetTime = with(time.plusHours(hoursToAdd), offset)
    public fun plusMinutes(minutesToAdd: Long): OffsetTime = with(time.plusMinutes(minutesToAdd), offset)
    public fun plusSeconds(secondsToAdd: Long): OffsetTime = with(time.plusSeconds(secondsToAdd), offset)
    public fun plusNanos(nanosToAdd: Long): OffsetTime = with(time.plusNanos(nanosToAdd), offset)

    override fun minus(amount: TemporalAmount): OffsetTime = amount.subtractFrom(this) as OffsetTime

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): OffsetTime =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusHours(hoursToSubtract: Long): OffsetTime =
        with(time.minusHours(hoursToSubtract), offset)

    public fun minusMinutes(minutesToSubtract: Long): OffsetTime =
        with(time.minusMinutes(minutesToSubtract), offset)

    public fun minusSeconds(secondsToSubtract: Long): OffsetTime =
        with(time.minusSeconds(secondsToSubtract), offset)

    public fun minusNanos(nanosToSubtract: Long): OffsetTime =
        with(time.minusNanos(nanosToSubtract), offset)

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.offset()) {
            @Suppress("UNCHECKED_CAST")
            return offset as R
        }
        return super<Temporal>.query(query)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.NANO_OF_DAY, time.toNanoOfDay())
            .with(ChronoField.OFFSET_SECONDS, offset.totalSeconds.toLong())

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        val nanosUntil = end.toEpochNano() - toEpochNano()
        return when (unit) {
            ChronoUnit.NANOS -> nanosUntil
            ChronoUnit.MICROS -> nanosUntil / 1_000
            ChronoUnit.MILLIS -> nanosUntil / 1_000_000
            ChronoUnit.SECONDS -> nanosUntil / NANOS_PER_SECOND
            ChronoUnit.MINUTES -> nanosUntil / NANOS_PER_MINUTE
            ChronoUnit.HOURS -> nanosUntil / NANOS_PER_HOUR
            ChronoUnit.HALF_DAYS -> nanosUntil / NANOS_PER_HALF_DAY
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Converts this offset time on [date] to epoch seconds. */
    public fun toEpochSecond(date: LocalDate): Long =
        date.toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay() - offset.totalSeconds

    /** Combines this offset time with [date]. */
    public fun atDate(date: LocalDate): OffsetDateTime = OffsetDateTime.of(date, time, offset)

    override fun compareTo(other: OffsetTime): Int {
        if (offset == other.offset) return time.compareTo(other.time)
        val instantComparison = toEpochNano().compareTo(other.toEpochNano())
        return if (instantComparison != 0) instantComparison else time.compareTo(other.time)
    }

    public fun isAfter(other: OffsetTime): Boolean = toEpochNano() > other.toEpochNano()
    public fun isBefore(other: OffsetTime): Boolean = toEpochNano() < other.toEpochNano()
    public fun isEqual(other: OffsetTime): Boolean = toEpochNano() == other.toEpochNano()

    override fun equals(other: Any?): Boolean =
        this === other || other is OffsetTime && time == other.time && offset == other.offset

    override fun hashCode(): Int = time.hashCode() xor offset.hashCode()

    override fun toString(): String = "$time$offset"

    private fun with(time: LocalTime, offset: ZoneOffset): OffsetTime =
        if (this.time == time && this.offset == offset) this else OffsetTime(time, offset)

    private fun toEpochNano(): Long =
        time.toNanoOfDay() - offset.totalSeconds * NANOS_PER_SECOND

    public companion object {
        private const val SECONDS_PER_DAY: Long = 86_400
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
        private const val NANOS_PER_HOUR: Long = 60 * NANOS_PER_MINUTE
        private const val NANOS_PER_HALF_DAY: Long = 12 * NANOS_PER_HOUR

        public val MIN: OffsetTime = OffsetTime(LocalTime.MIN, ZoneOffset.MAX)
        public val MAX: OffsetTime = OffsetTime(LocalTime.MAX, ZoneOffset.MIN)

        /** Obtains the current offset time using the system clock in [zone]. */
        public fun now(zone: ZoneId): OffsetTime = now(Clock.system(zone))

        /** Obtains the current offset time from [clock]. */
        public fun now(clock: Clock): OffsetTime = OffsetDateTime.now(clock).toOffsetTime()

        public fun of(time: LocalTime, offset: ZoneOffset): OffsetTime = OffsetTime(time, offset)

        public fun of(
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            offset: ZoneOffset,
        ): OffsetTime = OffsetTime(LocalTime.of(hour, minute, second, nanoOfSecond), offset)

        public fun from(temporal: TemporalAccessor): OffsetTime {
            if (temporal is OffsetTime) return temporal
            return try {
                of(LocalTime.from(temporal), ZoneOffset.from(temporal))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain OffsetTime from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        public fun parse(text: CharSequence): OffsetTime {
            val input = text.toString()
            val offsetStart = input.indexOfFirst { it == '+' || it == '-' || it == 'Z' || it == 'z' }
            if (offsetStart < 0) throw parseFailure(input, input.length)
            val time = try {
                LocalTime.parse(input.substring(0, offsetStart))
            } catch (exception: DateTimeParseException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to an OffsetTime",
                    input,
                    exception.errorIndex,
                    exception,
                )
            }
            val offsetText = input.substring(offsetStart)
            val validOffsetFormat = offsetText.equals("Z", ignoreCase = true) ||
                offsetText.length == 6 && offsetText[3] == ':' ||
                offsetText.length == 9 && offsetText[3] == ':' && offsetText[6] == ':'
            if (!validOffsetFormat) throw parseFailure(input, offsetStart)
            val offset = try {
                ZoneOffset.of(if (offsetText == "z") "Z" else offsetText)
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to an OffsetTime",
                    input,
                    0,
                    exception,
                )
            }
            return of(time, offset)
        }

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to an OffsetTime", input, errorIndex)
    }
}
