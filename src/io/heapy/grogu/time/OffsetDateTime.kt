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
import io.heapy.grogu.time.temporal.ValueRange

/** A date-time with a fixed offset from UTC. */
public class OffsetDateTime private constructor(
    public val dateTime: LocalDateTime,
    public val offset: ZoneOffset,
) : Temporal, TemporalAdjuster, Comparable<OffsetDateTime> {
    public val date: LocalDate get() = dateTime.date
    public val time: LocalTime get() = dateTime.time
    public val year: Int get() = dateTime.year
    public val monthValue: Int get() = dateTime.monthValue
    public val month: Month get() = dateTime.month
    public val dayOfMonth: Int get() = dateTime.dayOfMonth
    public val dayOfYear: Int get() = dateTime.dayOfYear
    public val dayOfWeek: DayOfWeek get() = dateTime.dayOfWeek
    public val hour: Int get() = dateTime.hour
    public val minute: Int get() = dateTime.minute
    public val second: Int get() = dateTime.second
    public val nano: Int get() = dateTime.nano

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) true else field.isSupportedBy(this)

    override fun isSupported(unit: TemporalUnit): Boolean =
        if (unit is ChronoUnit) unit !== ChronoUnit.FOREVER else unit.isSupportedBy(this)

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.INSTANT_SECONDS,
        ChronoField.OFFSET_SECONDS,
        -> field.range
        is ChronoField -> dateTime.range(field)
        else -> field.rangeRefinedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.INSTANT_SECONDS -> toEpochSecond()
        ChronoField.OFFSET_SECONDS -> offset.totalSeconds.toLong()
        is ChronoField -> dateTime.getLong(field)
        else -> field.getFrom(this)
    }

    /** Returns a copy at [offset] without changing the local date-time. */
    public fun withOffsetSameLocal(offset: ZoneOffset): OffsetDateTime = with(dateTime, offset)

    /** Returns a copy at [offset] that represents the same instant. */
    public fun withOffsetSameInstant(offset: ZoneOffset): OffsetDateTime {
        if (this.offset == offset) return this
        val difference = offset.totalSeconds - this.offset.totalSeconds
        return OffsetDateTime(dateTime.plusSeconds(difference.toLong()), offset)
    }

    public fun toLocalDateTime(): LocalDateTime = dateTime
    public fun toLocalDate(): LocalDate = date
    public fun toLocalTime(): LocalTime = time

    override fun with(adjuster: TemporalAdjuster): OffsetDateTime = when (adjuster) {
        is LocalDate -> with(LocalDateTime.of(adjuster, time), offset)
        is LocalTime -> with(LocalDateTime.of(date, adjuster), offset)
        is LocalDateTime -> with(adjuster, offset)
        is Instant -> ofInstant(adjuster, offset)
        is ZoneOffset -> with(dateTime, adjuster)
        is OffsetDateTime -> adjuster
        else -> adjuster.adjustInto(this) as OffsetDateTime
    }

    override fun with(field: TemporalField, newValue: Long): OffsetDateTime {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        return when (field) {
            ChronoField.INSTANT_SECONDS ->
                ofInstant(Instant.ofEpochSecond(newValue, nano.toLong()), offset)
            ChronoField.OFFSET_SECONDS -> with(
                dateTime,
                ZoneOffset.ofTotalSeconds(field.checkValidIntValue(newValue)),
            )
            else -> with(dateTime.with(field, newValue), offset)
        }
    }

    public fun withYear(year: Int): OffsetDateTime = with(dateTime.withYear(year), offset)
    public fun withMonth(month: Int): OffsetDateTime = with(dateTime.withMonth(month), offset)
    public fun withDayOfMonth(dayOfMonth: Int): OffsetDateTime =
        with(dateTime.withDayOfMonth(dayOfMonth), offset)
    public fun withDayOfYear(dayOfYear: Int): OffsetDateTime =
        with(dateTime.withDayOfYear(dayOfYear), offset)
    public fun withHour(hour: Int): OffsetDateTime = with(dateTime.withHour(hour), offset)
    public fun withMinute(minute: Int): OffsetDateTime = with(dateTime.withMinute(minute), offset)
    public fun withSecond(second: Int): OffsetDateTime = with(dateTime.withSecond(second), offset)
    public fun withNano(nanoOfSecond: Int): OffsetDateTime =
        with(dateTime.withNano(nanoOfSecond), offset)
    public fun truncatedTo(unit: TemporalUnit): OffsetDateTime =
        with(dateTime.truncatedTo(unit), offset)

    override fun plus(amount: TemporalAmount): OffsetDateTime = amount.addTo(this) as OffsetDateTime

    override fun plus(amountToAdd: Long, unit: TemporalUnit): OffsetDateTime =
        if (unit is ChronoUnit) with(dateTime.plus(amountToAdd, unit), offset) else
            unit.addTo(this, amountToAdd)

    public fun plusYears(years: Long): OffsetDateTime = with(dateTime.plusYears(years), offset)
    public fun plusMonths(months: Long): OffsetDateTime = with(dateTime.plusMonths(months), offset)
    public fun plusWeeks(weeks: Long): OffsetDateTime = with(dateTime.plusWeeks(weeks), offset)
    public fun plusDays(days: Long): OffsetDateTime = with(dateTime.plusDays(days), offset)
    public fun plusHours(hours: Long): OffsetDateTime = with(dateTime.plusHours(hours), offset)
    public fun plusMinutes(minutes: Long): OffsetDateTime = with(dateTime.plusMinutes(minutes), offset)
    public fun plusSeconds(seconds: Long): OffsetDateTime = with(dateTime.plusSeconds(seconds), offset)
    public fun plusNanos(nanos: Long): OffsetDateTime = with(dateTime.plusNanos(nanos), offset)

    override fun minus(amount: TemporalAmount): OffsetDateTime =
        amount.subtractFrom(this) as OffsetDateTime

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): OffsetDateTime =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusYears(years: Long): OffsetDateTime =
        if (years == Long.MIN_VALUE) plusYears(Long.MAX_VALUE).plusYears(1) else plusYears(-years)
    public fun minusMonths(months: Long): OffsetDateTime =
        if (months == Long.MIN_VALUE) plusMonths(Long.MAX_VALUE).plusMonths(1) else plusMonths(-months)
    public fun minusWeeks(weeks: Long): OffsetDateTime =
        if (weeks == Long.MIN_VALUE) plusWeeks(Long.MAX_VALUE).plusWeeks(1) else plusWeeks(-weeks)
    public fun minusDays(days: Long): OffsetDateTime =
        if (days == Long.MIN_VALUE) plusDays(Long.MAX_VALUE).plusDays(1) else plusDays(-days)
    public fun minusHours(hours: Long): OffsetDateTime =
        if (hours == Long.MIN_VALUE) plusHours(Long.MAX_VALUE).plusHours(1) else plusHours(-hours)
    public fun minusMinutes(minutes: Long): OffsetDateTime =
        if (minutes == Long.MIN_VALUE) plusMinutes(Long.MAX_VALUE).plusMinutes(1) else plusMinutes(-minutes)
    public fun minusSeconds(seconds: Long): OffsetDateTime =
        if (seconds == Long.MIN_VALUE) plusSeconds(Long.MAX_VALUE).plusSeconds(1) else plusSeconds(-seconds)
    public fun minusNanos(nanos: Long): OffsetDateTime =
        if (nanos == Long.MIN_VALUE) plusNanos(Long.MAX_VALUE).plusNanos(1) else plusNanos(-nanos)

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.offset()) {
            @Suppress("UNCHECKED_CAST")
            return offset as R
        }
        return super<Temporal>.query(query)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay())
            .with(ChronoField.NANO_OF_DAY, time.toNanoOfDay())
            .with(ChronoField.OFFSET_SECONDS, offset.totalSeconds.toLong())

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        var end = from(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        var start = this
        try {
            end = end.withOffsetSameInstant(offset)
        } catch (_: DateTimeException) {
            start = withOffsetSameInstant(end.offset)
        }
        return start.dateTime.until(end.dateTime, unit)
    }

    public fun toOffsetTime(): OffsetTime = OffsetTime.of(time, offset)

    /** Combines this date-time with [zone] while retaining the instant. */
    public fun atZoneSameInstant(zone: ZoneId): ZonedDateTime =
        ZonedDateTime.ofInstant(dateTime, offset, zone)

    /** Combines this date-time with [zone], retaining the local fields where possible. */
    public fun atZoneSimilarLocal(zone: ZoneId): ZonedDateTime =
        ZonedDateTime.ofLocal(dateTime, zone, offset)

    /** Converts this date-time to a zoned date-time using its offset as the zone ID. */
    public fun toZonedDateTime(): ZonedDateTime = ZonedDateTime.of(dateTime, offset)

    public fun toInstant(): Instant = dateTime.toInstant(offset)
    public fun toEpochSecond(): Long = dateTime.toEpochSecond(offset)

    override fun compareTo(other: OffsetDateTime): Int {
        val instantComparison = compareInstant(other)
        return if (instantComparison != 0) instantComparison else dateTime.compareTo(other.dateTime)
    }

    public fun isAfter(other: OffsetDateTime): Boolean = compareInstant(other) > 0
    public fun isBefore(other: OffsetDateTime): Boolean = compareInstant(other) < 0
    public fun isEqual(other: OffsetDateTime): Boolean = compareInstant(other) == 0

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is OffsetDateTime &&
            dateTime == other.dateTime &&
            offset == other.offset

    override fun hashCode(): Int = dateTime.hashCode() xor offset.hashCode()

    override fun toString(): String = "$dateTime$offset"

    private fun with(dateTime: LocalDateTime, offset: ZoneOffset): OffsetDateTime =
        if (this.dateTime == dateTime && this.offset == offset) this else OffsetDateTime(dateTime, offset)

    private fun compareInstant(other: OffsetDateTime): Int {
        if (offset == other.offset) return dateTime.compareTo(other.dateTime)
        val secondsComparison = toEpochSecond().compareTo(other.toEpochSecond())
        return if (secondsComparison != 0) secondsComparison else nano - other.nano
    }

    public companion object {
        private val TIME_LINE_ORDER: Comparator<OffsetDateTime> =
            Comparator { first, second -> first.compareInstant(second) }

        public val MIN: OffsetDateTime = OffsetDateTime(LocalDateTime.MIN, ZoneOffset.MAX)
        public val MAX: OffsetDateTime = OffsetDateTime(LocalDateTime.MAX, ZoneOffset.MIN)

        public fun of(date: LocalDate, time: LocalTime, offset: ZoneOffset): OffsetDateTime =
            OffsetDateTime(LocalDateTime.of(date, time), offset)

        public fun of(dateTime: LocalDateTime, offset: ZoneOffset): OffsetDateTime =
            OffsetDateTime(dateTime, offset)

        public fun of(
            year: Int,
            month: Month,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            offset: ZoneOffset,
        ): OffsetDateTime = of(
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
            offset,
        )

        public fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            offset: ZoneOffset,
        ): OffsetDateTime = of(
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
            offset,
        )

        /** Obtains an offset date-time representing [instant] at [offset]. */
        public fun ofInstant(instant: Instant, offset: ZoneOffset): OffsetDateTime =
            OffsetDateTime(
                LocalDateTime.ofEpochSecond(instant.epochSecond, instant.nano, offset),
                offset,
            )

        /** Obtains an offset date-time from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): OffsetDateTime {
            if (temporal is OffsetDateTime) return temporal
            return try {
                val offset = ZoneOffset.from(temporal)
                try {
                    of(LocalDateTime.from(temporal), offset)
                } catch (_: DateTimeException) {
                    ofInstant(Instant.from(temporal), offset)
                }
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain OffsetDateTime from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses a date-time using the strict ISO offset-date-time format. */
        public fun parse(text: CharSequence): OffsetDateTime {
            val input = text.toString()
            val separatorIndex = input.indexOfFirst { it == 'T' || it == 't' }
            if (separatorIndex < 0) throw parseFailure(input, input.length)
            val offsetStart = (separatorIndex + 1..<input.length).firstOrNull { index ->
                input[index] == '+' || input[index] == '-' || input[index] == 'Z' || input[index] == 'z'
            } ?: throw parseFailure(input, input.length)

            val dateTime = try {
                LocalDateTime.parse(input.substring(0, offsetStart))
            } catch (exception: DateTimeParseException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to an OffsetDateTime",
                    input,
                    exception.errorIndex,
                    exception,
                )
            }
            val offsetText = input.substring(offsetStart)
            val validOffsetFormat = offsetText.equals("Z", ignoreCase = true) ||
                offsetText.length == 3 ||
                offsetText.length == 6 && offsetText[3] == ':' ||
                offsetText.length == 9 && offsetText[3] == ':' && offsetText[6] == ':'
            if (!validOffsetFormat) throw parseFailure(input, offsetStart)
            val offset = try {
                ZoneOffset.of(if (offsetText == "z") "Z" else offsetText)
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to an OffsetDateTime",
                    input,
                    0,
                    exception,
                )
            }
            return of(dateTime, offset)
        }

        /** Returns a comparator that compares offset date-times only by instant. */
        public fun timeLineOrder(): Comparator<OffsetDateTime> = TIME_LINE_ORDER

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to an OffsetDateTime", input, errorIndex)
    }
}
