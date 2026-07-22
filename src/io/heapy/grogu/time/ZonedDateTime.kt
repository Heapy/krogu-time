package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.IsoChronology
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

/** A date-time with a time-zone in the ISO-8601 calendar system. */
public class ZonedDateTime private constructor(
    public val dateTime: LocalDateTime,
    public val offset: ZoneOffset,
    public val zone: ZoneId,
) : Temporal, Comparable<ZonedDateTime> {
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

    /** Returns a copy using the earlier offset during a local timeline overlap. */
    public fun withEarlierOffsetAtOverlap(): ZonedDateTime {
        val transition = zone.rules.getTransition(dateTime)
        return if (transition?.isOverlap == true) {
            resolveOffset(transition.offsetBefore)
        } else {
            this
        }
    }

    /** Returns a copy using the later offset during a local timeline overlap. */
    public fun withLaterOffsetAtOverlap(): ZonedDateTime {
        val transition = zone.rules.getTransition(dateTime)
        return if (transition?.isOverlap == true) {
            resolveOffset(transition.offsetAfter)
        } else {
            this
        }
    }

    /** Returns a copy in [zone] retaining the local date-time where possible. */
    public fun withZoneSameLocal(zone: ZoneId): ZonedDateTime =
        if (this.zone == zone) this else ofLocal(dateTime, zone, offset)

    /** Returns a copy in [zone] representing the same instant. */
    public fun withZoneSameInstant(zone: ZoneId): ZonedDateTime =
        if (this.zone == zone) this else ofInstant(toInstant(), zone)

    /** Returns a copy whose zone ID is the current fixed offset. */
    public fun withFixedOffsetZone(): ZonedDateTime =
        if (zone == offset) this else ZonedDateTime(dateTime, offset, offset)

    public fun toLocalDateTime(): LocalDateTime = dateTime
    public fun toLocalDate(): LocalDate = date
    public fun toLocalTime(): LocalTime = time

    override fun with(adjuster: TemporalAdjuster): ZonedDateTime = when (adjuster) {
        is LocalDate -> resolveLocal(LocalDateTime.of(adjuster, time))
        is LocalTime -> resolveLocal(LocalDateTime.of(date, adjuster))
        is LocalDateTime -> resolveLocal(adjuster)
        is OffsetDateTime -> ofLocal(adjuster.dateTime, zone, adjuster.offset)
        is Instant -> ofInstant(adjuster, zone)
        is ZoneOffset -> resolveOffset(adjuster)
        else -> adjuster.adjustInto(this) as ZonedDateTime
    }

    override fun with(field: TemporalField, newValue: Long): ZonedDateTime {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        return when (field) {
            ChronoField.INSTANT_SECONDS ->
                ofInstant(Instant.ofEpochSecond(newValue, nano.toLong()), zone)
            ChronoField.OFFSET_SECONDS ->
                resolveOffset(ZoneOffset.ofTotalSeconds(field.checkValidIntValue(newValue)))
            else -> resolveLocal(dateTime.with(field, newValue))
        }
    }

    public fun withYear(year: Int): ZonedDateTime = resolveLocal(dateTime.withYear(year))
    public fun withMonth(month: Int): ZonedDateTime = resolveLocal(dateTime.withMonth(month))
    public fun withDayOfMonth(dayOfMonth: Int): ZonedDateTime =
        resolveLocal(dateTime.withDayOfMonth(dayOfMonth))
    public fun withDayOfYear(dayOfYear: Int): ZonedDateTime =
        resolveLocal(dateTime.withDayOfYear(dayOfYear))
    public fun withHour(hour: Int): ZonedDateTime = resolveLocal(dateTime.withHour(hour))
    public fun withMinute(minute: Int): ZonedDateTime = resolveLocal(dateTime.withMinute(minute))
    public fun withSecond(second: Int): ZonedDateTime = resolveLocal(dateTime.withSecond(second))
    public fun withNano(nanoOfSecond: Int): ZonedDateTime =
        resolveLocal(dateTime.withNano(nanoOfSecond))
    public fun truncatedTo(unit: TemporalUnit): ZonedDateTime =
        resolveLocal(dateTime.truncatedTo(unit))

    override fun plus(amount: TemporalAmount): ZonedDateTime =
        amount.addTo(this) as ZonedDateTime

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ZonedDateTime {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        val newDateTime = dateTime.plus(amountToAdd, unit)
        return if (unit.isDateBased) resolveLocal(newDateTime) else resolveInstant(newDateTime)
    }

    public fun plusYears(years: Long): ZonedDateTime = resolveLocal(dateTime.plusYears(years))
    public fun plusMonths(months: Long): ZonedDateTime = resolveLocal(dateTime.plusMonths(months))
    public fun plusWeeks(weeks: Long): ZonedDateTime = resolveLocal(dateTime.plusWeeks(weeks))
    public fun plusDays(days: Long): ZonedDateTime = resolveLocal(dateTime.plusDays(days))
    public fun plusHours(hours: Long): ZonedDateTime = resolveInstant(dateTime.plusHours(hours))
    public fun plusMinutes(minutes: Long): ZonedDateTime = resolveInstant(dateTime.plusMinutes(minutes))
    public fun plusSeconds(seconds: Long): ZonedDateTime = resolveInstant(dateTime.plusSeconds(seconds))
    public fun plusNanos(nanos: Long): ZonedDateTime = resolveInstant(dateTime.plusNanos(nanos))

    override fun minus(amount: TemporalAmount): ZonedDateTime =
        amount.subtractFrom(this) as ZonedDateTime

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): ZonedDateTime =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusYears(years: Long): ZonedDateTime =
        if (years == Long.MIN_VALUE) plusYears(Long.MAX_VALUE).plusYears(1) else plusYears(-years)
    public fun minusMonths(months: Long): ZonedDateTime =
        if (months == Long.MIN_VALUE) plusMonths(Long.MAX_VALUE).plusMonths(1) else plusMonths(-months)
    public fun minusWeeks(weeks: Long): ZonedDateTime =
        if (weeks == Long.MIN_VALUE) plusWeeks(Long.MAX_VALUE).plusWeeks(1) else plusWeeks(-weeks)
    public fun minusDays(days: Long): ZonedDateTime =
        if (days == Long.MIN_VALUE) plusDays(Long.MAX_VALUE).plusDays(1) else plusDays(-days)
    public fun minusHours(hours: Long): ZonedDateTime =
        if (hours == Long.MIN_VALUE) plusHours(Long.MAX_VALUE).plusHours(1) else plusHours(-hours)
    public fun minusMinutes(minutes: Long): ZonedDateTime =
        if (minutes == Long.MIN_VALUE) plusMinutes(Long.MAX_VALUE).plusMinutes(1) else plusMinutes(-minutes)
    public fun minusSeconds(seconds: Long): ZonedDateTime =
        if (seconds == Long.MIN_VALUE) plusSeconds(Long.MAX_VALUE).plusSeconds(1) else plusSeconds(-seconds)
    public fun minusNanos(nanos: Long): ZonedDateTime =
        if (nanos == Long.MIN_VALUE) plusNanos(Long.MAX_VALUE).plusNanos(1) else plusNanos(-nanos)

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.chronology()) {
            @Suppress("UNCHECKED_CAST")
            return IsoChronology as R
        }
        if (query === TemporalQueries.localDate()) {
            @Suppress("UNCHECKED_CAST")
            return date as R
        }
        if (query === TemporalQueries.localTime()) {
            @Suppress("UNCHECKED_CAST")
            return time as R
        }
        if (query === TemporalQueries.zoneId()) {
            @Suppress("UNCHECKED_CAST")
            return zone as R
        }
        if (query === TemporalQueries.offset()) {
            @Suppress("UNCHECKED_CAST")
            return offset as R
        }
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.NANOS as R
        }
        return super<Temporal>.query(query)
    }

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        val zonedEnd = end.withZoneSameInstant(zone)
        return if (unit.isDateBased) {
            dateTime.until(zonedEnd.dateTime, unit)
        } else {
            toOffsetDateTime().until(zonedEnd.toOffsetDateTime(), unit)
        }
    }

    public fun toOffsetDateTime(): OffsetDateTime = OffsetDateTime.of(dateTime, offset)
    public fun toInstant(): Instant = dateTime.toInstant(offset)
    public fun toEpochSecond(): Long = dateTime.toEpochSecond(offset)

    override fun compareTo(other: ZonedDateTime): Int {
        val instantComparison = compareInstant(other)
        if (instantComparison != 0) return instantComparison
        val localComparison = dateTime.compareTo(other.dateTime)
        return if (localComparison != 0) localComparison else zone.id.compareTo(other.zone.id)
    }

    public fun isAfter(other: ZonedDateTime): Boolean = compareInstant(other) > 0
    public fun isBefore(other: ZonedDateTime): Boolean = compareInstant(other) < 0
    public fun isEqual(other: ZonedDateTime): Boolean = compareInstant(other) == 0

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ZonedDateTime &&
            dateTime == other.dateTime &&
            offset == other.offset &&
            zone == other.zone

    override fun hashCode(): Int =
        dateTime.hashCode() xor offset.hashCode() xor zone.hashCode().rotateLeft(3)

    override fun toString(): String = buildString {
        append(dateTime)
        append(offset)
        if (zone != offset) {
            append('[')
            append(zone)
            append(']')
        }
    }

    private fun resolveLocal(newDateTime: LocalDateTime): ZonedDateTime =
        ofLocal(newDateTime, zone, offset)

    private fun resolveInstant(newDateTime: LocalDateTime): ZonedDateTime =
        ofInstant(newDateTime, offset, zone)

    private fun resolveOffset(newOffset: ZoneOffset): ZonedDateTime =
        if (newOffset == offset || !zone.rules.isValidOffset(dateTime, newOffset)) {
            this
        } else {
            ZonedDateTime(dateTime, newOffset, zone)
        }

    private fun compareInstant(other: ZonedDateTime): Int {
        val secondsComparison = toEpochSecond().compareTo(other.toEpochSecond())
        return if (secondsComparison != 0) secondsComparison else nano.compareTo(other.nano)
    }

    public companion object {
        private val TIME_LINE_ORDER: Comparator<ZonedDateTime> =
            Comparator { first, second -> first.compareInstant(second) }

        /** Obtains the current zoned date-time using the system clock in [zone]. */
        public fun now(zone: ZoneId): ZonedDateTime = now(Clock.system(zone))

        /** Obtains the current zoned date-time from [clock]. */
        public fun now(clock: Clock): ZonedDateTime = ofInstant(clock.instant(), clock.zone)

        public fun of(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime =
            of(LocalDateTime.of(date, time), zone)

        public fun of(dateTime: LocalDateTime, zone: ZoneId): ZonedDateTime =
            ofLocal(dateTime, zone, null)

        public fun of(
            year: Int,
            month: Month,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            zone: ZoneId,
        ): ZonedDateTime = of(
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
            zone,
        )

        public fun of(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            zone: ZoneId,
        ): ZonedDateTime = of(
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
            zone,
        )

        /** Resolves [dateTime] in [zone], optionally preferring [preferredOffset] in an overlap. */
        public fun ofLocal(
            dateTime: LocalDateTime,
            zone: ZoneId,
            preferredOffset: ZoneOffset?,
        ): ZonedDateTime {
            if (zone is ZoneOffset) return ZonedDateTime(dateTime, zone, zone)

            val rules = zone.rules
            val validOffsets = rules.getValidOffsets(dateTime)
            val offset: ZoneOffset
            val resolvedDateTime: LocalDateTime
            when (validOffsets.size) {
                1 -> {
                    resolvedDateTime = dateTime
                    offset = validOffsets[0]
                }
                0 -> {
                    val transition = checkNotNull(rules.getTransition(dateTime))
                    resolvedDateTime = dateTime.plusSeconds(transition.duration.seconds)
                    offset = transition.offsetAfter
                }
                else -> {
                    resolvedDateTime = dateTime
                    offset = if (preferredOffset != null && preferredOffset in validOffsets) {
                        preferredOffset
                    } else {
                        validOffsets[0]
                    }
                }
            }
            return ZonedDateTime(resolvedDateTime, offset, zone)
        }

        /** Obtains a zoned date-time representing [instant] in [zone]. */
        public fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime {
            val offset = zone.rules.getOffset(instant)
            return ZonedDateTime(
                LocalDateTime.ofEpochSecond(instant.epochSecond, instant.nano, offset),
                offset,
                zone,
            )
        }

        /** Resolves the instant implied by [dateTime] and [offset] into [zone]. */
        public fun ofInstant(
            dateTime: LocalDateTime,
            offset: ZoneOffset,
            zone: ZoneId,
        ): ZonedDateTime = if (zone.rules.isValidOffset(dateTime, offset)) {
            ZonedDateTime(dateTime, offset, zone)
        } else {
            ofInstant(dateTime.toInstant(offset), zone)
        }

        /** Obtains a zoned date-time only when [offset] is valid at [dateTime] in [zone]. */
        public fun ofStrict(
            dateTime: LocalDateTime,
            offset: ZoneOffset,
            zone: ZoneId,
        ): ZonedDateTime {
            val rules = zone.rules
            if (rules.isValidOffset(dateTime, offset)) {
                return ZonedDateTime(dateTime, offset, zone)
            }
            if (rules.getTransition(dateTime)?.isGap == true) {
                throw DateTimeException(
                    "LocalDateTime '$dateTime' does not exist in zone '$zone' due to a gap " +
                        "in the local time-line, typically caused by daylight savings",
                )
            }
            throw DateTimeException(
                "ZoneOffset '$offset' is not valid for LocalDateTime '$dateTime' in zone '$zone'",
            )
        }

        /** Obtains a zoned date-time from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): ZonedDateTime {
            if (temporal is ZonedDateTime) return temporal
            return try {
                val zone = ZoneId.from(temporal)
                if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                    ofInstant(Instant.from(temporal), zone)
                } else {
                    of(LocalDateTime.from(temporal), zone)
                }
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain ZonedDateTime from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses a date-time using the strict ISO zoned-date-time format. */
        public fun parse(text: CharSequence): ZonedDateTime {
            val input = text.toString()
            val bracketStart = input.lastIndexOf('[')
            val hasBracket = bracketStart >= 0 || ']' in input
            if (!hasBracket) {
                return parseOffsetDateTime(input, input).toZonedDateTime()
            }
            if (bracketStart < 0 || !input.endsWith(']') || bracketStart == input.lastIndex) {
                throw parseFailure(input, maxOf(bracketStart, 0))
            }

            val zoneText = input.substring(bracketStart + 1, input.lastIndex)
            if (zoneText.isEmpty()) throw parseFailure(input, bracketStart + 1)
            val offsetDateTime = parseOffsetDateTime(
                input.substring(0, bracketStart),
                input,
            )
            val zone = try {
                ZoneId.of(zoneText)
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a ZonedDateTime",
                    input,
                    bracketStart + 1,
                    exception,
                )
            }
            return ofInstant(offsetDateTime.toInstant(), zone)
        }

        /** Returns a comparator that compares zoned date-times only by instant. */
        public fun timeLineOrder(): Comparator<ZonedDateTime> = TIME_LINE_ORDER

        private fun parseOffsetDateTime(
            offsetText: String,
            completeText: String,
        ): OffsetDateTime = try {
            OffsetDateTime.parse(offsetText)
        } catch (exception: DateTimeParseException) {
            throw DateTimeParseException(
                "Text cannot be parsed to a ZonedDateTime",
                completeText,
                exception.errorIndex,
                exception,
            )
        }

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a ZonedDateTime", input, errorIndex)
    }
}
