package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.chrono.ChronoLocalDateTime
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorDiv
import io.heapy.grogu.time.internal.floorMod
import io.heapy.grogu.time.internal.multiplyExact
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
import io.heapy.grogu.time.temporal.ValueRange

/** A date-time without a time-zone in the ISO-8601 calendar system. */
public class LocalDateTime private constructor(
    override val date: LocalDate,
    override val time: LocalTime,
) : ChronoLocalDateTime<LocalDate> {
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

    override fun isSupported(unit: TemporalUnit): Boolean =
        if (unit is ChronoUnit) unit !== ChronoUnit.FOREVER else unit.isSupportedBy(this)

    override fun range(field: TemporalField): ValueRange = when (field) {
        is ChronoField -> if (field.isTimeBased) time.range(field) else date.range(field)
        else -> field.rangeRefinedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        is ChronoField -> if (field.isTimeBased) time.getLong(field) else date.getLong(field)
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any = when (query) {
            TemporalQueries.chronology() -> IsoChronology
            TemporalQueries.localDate() -> date
            TemporalQueries.localTime() -> time
            TemporalQueries.precision() -> ChronoUnit.NANOS
            else -> return super<ChronoLocalDateTime>.query(query)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    /** Returns the local-date part. */
    override fun toLocalDate(): LocalDate = date

    /** Returns the local-time part. */
    override fun toLocalTime(): LocalTime = time

    /** Converts this local date-time to epoch seconds using [offset]. */
    override fun toEpochSecond(offset: ZoneOffset): Long =
        date.toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay() - offset.totalSeconds

    /** Combines this local date-time with [offset] to create an instant. */
    override fun toInstant(offset: ZoneOffset): Instant =
        Instant.ofEpochSecond(toEpochSecond(offset), nano.toLong())

    /** Combines this local date-time with [offset]. */
    public fun atOffset(offset: ZoneOffset): OffsetDateTime = OffsetDateTime.of(this, offset)

    /** Resolves this local date-time in [zone]. */
    public fun atZone(zone: ZoneId): ZonedDateTime = ZonedDateTime.of(this, zone)

    override fun with(adjuster: TemporalAdjuster): LocalDateTime = when (adjuster) {
        is LocalDate -> with(adjuster, time)
        is LocalTime -> with(date, adjuster)
        is LocalDateTime -> adjuster
        else -> adjuster.adjustInto(this) as LocalDateTime
    }

    override fun with(field: TemporalField, newValue: Long): LocalDateTime =
        if (field is ChronoField) {
            if (field.isTimeBased) {
                with(date, time.with(field, newValue))
            } else {
                with(date.with(field, newValue), time)
            }
        } else {
            field.adjustInto(this, newValue)
        }

    public fun withYear(year: Int): LocalDateTime = with(date.withYear(year), time)

    public fun withMonth(month: Int): LocalDateTime = with(date.withMonth(month), time)

    public fun withDayOfMonth(dayOfMonth: Int): LocalDateTime =
        with(date.withDayOfMonth(dayOfMonth), time)

    public fun withDayOfYear(dayOfYear: Int): LocalDateTime =
        with(date.withDayOfYear(dayOfYear), time)

    public fun withHour(hour: Int): LocalDateTime = with(date, time.withHour(hour))

    public fun withMinute(minute: Int): LocalDateTime = with(date, time.withMinute(minute))

    public fun withSecond(second: Int): LocalDateTime = with(date, time.withSecond(second))

    public fun withNano(nanoOfSecond: Int): LocalDateTime = with(date, time.withNano(nanoOfSecond))

    /** Truncates the local-time part to [unit]. */
    public fun truncatedTo(unit: TemporalUnit): LocalDateTime =
        with(date, time.truncatedTo(unit))

    override fun plus(amount: TemporalAmount): LocalDateTime =
        if (amount is Period) {
            with(date.plus(amount), time)
        } else {
            amount.addTo(this) as LocalDateTime
        }

    override fun plus(amountToAdd: Long, unit: TemporalUnit): LocalDateTime {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        return when (unit) {
            ChronoUnit.NANOS -> plusNanos(amountToAdd)
            ChronoUnit.MICROS -> plusDays(amountToAdd / MICROS_PER_DAY)
                .plusNanos(amountToAdd % MICROS_PER_DAY * NANOS_PER_MICRO)
            ChronoUnit.MILLIS -> plusDays(amountToAdd / MILLIS_PER_DAY)
                .plusNanos(amountToAdd % MILLIS_PER_DAY * NANOS_PER_MILLI)
            ChronoUnit.SECONDS -> plusSeconds(amountToAdd)
            ChronoUnit.MINUTES -> plusMinutes(amountToAdd)
            ChronoUnit.HOURS -> plusHours(amountToAdd)
            ChronoUnit.HALF_DAYS -> plusDays(amountToAdd / 256)
                .plusHours(amountToAdd % 256 * 12)
            else -> with(date.plus(amountToAdd, unit), time)
        }
    }

    public fun plusYears(years: Long): LocalDateTime = with(date.plusYears(years), time)

    public fun plusMonths(months: Long): LocalDateTime = with(date.plusMonths(months), time)

    public fun plusWeeks(weeks: Long): LocalDateTime = with(date.plusWeeks(weeks), time)

    public fun plusDays(days: Long): LocalDateTime = with(date.plusDays(days), time)

    public fun plusHours(hours: Long): LocalDateTime =
        plusWithOverflow(date, hours, 0, 0, 0, 1)

    public fun plusMinutes(minutes: Long): LocalDateTime =
        plusWithOverflow(date, 0, minutes, 0, 0, 1)

    public fun plusSeconds(seconds: Long): LocalDateTime =
        plusWithOverflow(date, 0, 0, seconds, 0, 1)

    public fun plusNanos(nanos: Long): LocalDateTime =
        plusWithOverflow(date, 0, 0, 0, nanos, 1)

    override fun minus(amount: TemporalAmount): LocalDateTime =
        if (amount is Period) {
            with(date.minus(amount), time)
        } else {
            amount.subtractFrom(this) as LocalDateTime
        }

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): LocalDateTime =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusYears(years: Long): LocalDateTime =
        if (years == Long.MIN_VALUE) plusYears(Long.MAX_VALUE).plusYears(1) else plusYears(-years)

    public fun minusMonths(months: Long): LocalDateTime =
        if (months == Long.MIN_VALUE) plusMonths(Long.MAX_VALUE).plusMonths(1) else plusMonths(-months)

    public fun minusWeeks(weeks: Long): LocalDateTime =
        if (weeks == Long.MIN_VALUE) plusWeeks(Long.MAX_VALUE).plusWeeks(1) else plusWeeks(-weeks)

    public fun minusDays(days: Long): LocalDateTime =
        if (days == Long.MIN_VALUE) plusDays(Long.MAX_VALUE).plusDays(1) else plusDays(-days)

    public fun minusHours(hours: Long): LocalDateTime =
        plusWithOverflow(date, hours, 0, 0, 0, -1)

    public fun minusMinutes(minutes: Long): LocalDateTime =
        plusWithOverflow(date, 0, minutes, 0, 0, -1)

    public fun minusSeconds(seconds: Long): LocalDateTime =
        plusWithOverflow(date, 0, 0, seconds, 0, -1)

    public fun minusNanos(nanos: Long): LocalDateTime =
        plusWithOverflow(date, 0, 0, 0, nanos, -1)

    private fun plusWithOverflow(
        newDate: LocalDate,
        hours: Long,
        minutes: Long,
        seconds: Long,
        nanos: Long,
        sign: Int,
    ): LocalDateTime {
        if ((hours or minutes or seconds or nanos) == 0L) return with(newDate, time)

        var totalDays =
            nanos / NANOS_PER_DAY +
                seconds / SECONDS_PER_DAY +
                minutes / MINUTES_PER_DAY +
                hours / HOURS_PER_DAY
        totalDays *= sign.toLong()
        var totalNanos =
            nanos % NANOS_PER_DAY +
                seconds % SECONDS_PER_DAY * NANOS_PER_SECOND +
                minutes % MINUTES_PER_DAY * NANOS_PER_MINUTE +
                hours % HOURS_PER_DAY * NANOS_PER_HOUR
        val currentNanoOfDay = time.toNanoOfDay()
        totalNanos = totalNanos * sign + currentNanoOfDay
        totalDays += floorDiv(totalNanos, NANOS_PER_DAY)
        val newNanoOfDay = floorMod(totalNanos, NANOS_PER_DAY)
        val newTime = if (newNanoOfDay == currentNanoOfDay) time else LocalTime.ofNanoOfDay(newNanoOfDay)
        return with(newDate.plusDays(totalDays), newTime)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay())
            .with(ChronoField.NANO_OF_DAY, time.toNanoOfDay())

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        if (unit.isTimeBased) {
            var amount = end.date.toEpochDay() - date.toEpochDay()
            if (amount == 0L) return time.until(end.time, unit)
            var timePart = end.time.toNanoOfDay() - time.toNanoOfDay()
            if (amount > 0) {
                amount--
                timePart += NANOS_PER_DAY
            } else {
                amount++
                timePart -= NANOS_PER_DAY
            }
            when (unit) {
                ChronoUnit.NANOS -> amount = multiplyExact(amount, NANOS_PER_DAY)
                ChronoUnit.MICROS -> {
                    amount = multiplyExact(amount, MICROS_PER_DAY)
                    timePart /= NANOS_PER_MICRO
                }
                ChronoUnit.MILLIS -> {
                    amount = multiplyExact(amount, MILLIS_PER_DAY)
                    timePart /= NANOS_PER_MILLI
                }
                ChronoUnit.SECONDS -> {
                    amount = multiplyExact(amount, SECONDS_PER_DAY)
                    timePart /= NANOS_PER_SECOND
                }
                ChronoUnit.MINUTES -> {
                    amount = multiplyExact(amount, MINUTES_PER_DAY)
                    timePart /= NANOS_PER_MINUTE
                }
                ChronoUnit.HOURS -> {
                    amount = multiplyExact(amount, HOURS_PER_DAY)
                    timePart /= NANOS_PER_HOUR
                }
                ChronoUnit.HALF_DAYS -> {
                    amount = multiplyExact(amount, 2)
                    timePart /= 12 * NANOS_PER_HOUR
                }
                else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
            }
            return addExact(amount, timePart)
        }

        var endDate = end.date
        if (endDate.isAfter(date) && end.time.isBefore(time)) {
            endDate = endDate.minusDays(1)
        } else if (endDate.isBefore(date) && end.time.isAfter(time)) {
            endDate = endDate.plusDays(1)
        }
        return date.until(endDate, unit)
    }

    private fun with(newDate: LocalDate, newTime: LocalTime): LocalDateTime =
        if (date == newDate && time == newTime) this else LocalDateTime(newDate, newTime)

    override fun compareTo(other: ChronoLocalDateTime<*>): Int {
        if (other !is LocalDateTime) return super<ChronoLocalDateTime>.compareTo(other)
        val dateComparison = date.compareTo(other.date)
        return if (dateComparison != 0) dateComparison else time.compareTo(other.time)
    }

    /** Whether this date-time is after [other] on the local timeline. */
    override fun isAfter(other: ChronoLocalDateTime<*>): Boolean =
        if (other is LocalDateTime) compareTo(other) > 0 else super<ChronoLocalDateTime>.isAfter(other)

    /** Whether this date-time is before [other] on the local timeline. */
    override fun isBefore(other: ChronoLocalDateTime<*>): Boolean =
        if (other is LocalDateTime) compareTo(other) < 0 else super<ChronoLocalDateTime>.isBefore(other)

    /** Whether this date-time represents the same local date and time as [other]. */
    override fun isEqual(other: ChronoLocalDateTime<*>): Boolean =
        if (other is LocalDateTime) compareTo(other) == 0 else super<ChronoLocalDateTime>.isEqual(other)

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LocalDateTime &&
            date == other.date &&
            time == other.time

    override fun hashCode(): Int = date.hashCode() xor time.hashCode()

    override fun toString(): String = "${date}T$time"

    public companion object {
        private const val HOURS_PER_DAY: Long = 24
        private const val MINUTES_PER_DAY: Long = 1_440
        private const val SECONDS_PER_DAY: Long = 86_400
        private const val NANOS_PER_MICRO: Long = 1_000
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
        private const val NANOS_PER_HOUR: Long = 3_600 * NANOS_PER_SECOND
        private const val NANOS_PER_DAY: Long = 24 * NANOS_PER_HOUR
        private const val MICROS_PER_DAY: Long = NANOS_PER_DAY / NANOS_PER_MICRO
        private const val MILLIS_PER_DAY: Long = NANOS_PER_DAY / NANOS_PER_MILLI

        public val MIN: LocalDateTime = LocalDateTime(LocalDate.MIN, LocalTime.MIN)
        public val MAX: LocalDateTime = LocalDateTime(LocalDate.MAX, LocalTime.MAX)

        /** Obtains the current local date-time using the system clock in [zone]. */
        public fun now(zone: ZoneId): LocalDateTime = now(Clock.system(zone))

        /** Obtains the current local date-time from [clock]. */
        public fun now(clock: Clock): LocalDateTime {
            val instant = clock.instant()
            val offset = clock.zone.rules.getOffset(instant)
            return ofEpochSecond(instant.epochSecond, instant.nano, offset)
        }

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

        /** Obtains a local date-time from epoch seconds interpreted with [offset]. */
        public fun ofEpochSecond(
            epochSecond: Long,
            nanoOfSecond: Int,
            offset: ZoneOffset,
        ): LocalDateTime {
            val nano = ChronoField.NANO_OF_SECOND.checkValidIntValue(nanoOfSecond.toLong())
            val localSecond = epochSecond + offset.totalSeconds
            val localEpochDay = floorDiv(localSecond, SECONDS_PER_DAY)
            val secondOfDay = floorMod(localSecond, SECONDS_PER_DAY)
            return of(
                LocalDate.ofEpochDay(localEpochDay),
                LocalTime.ofSecondOfDay(secondOfDay).withNano(nano),
            )
        }

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

        /** Parses a date-time using the strict ISO local-date-time format. */
        public fun parse(text: CharSequence): LocalDateTime {
            val input = text.toString()
            val separatorIndex = input.indexOfFirst { it == 'T' || it == 't' }
            if (separatorIndex < 0) {
                try {
                    LocalDate.parse(input)
                } catch (exception: DateTimeParseException) {
                    throw translatedFailure(input, exception, 0)
                }
                throw parseFailure(input, input.length)
            }

            val date = try {
                LocalDate.parse(input.substring(0, separatorIndex))
            } catch (exception: DateTimeParseException) {
                throw translatedFailure(input, exception, 0)
            }
            val timeStart = separatorIndex + 1
            val time = try {
                LocalTime.parse(input.substring(timeStart))
            } catch (exception: DateTimeParseException) {
                throw translatedFailure(input, exception, timeStart)
            }
            return of(date, time)
        }

        private fun translatedFailure(
            input: String,
            exception: DateTimeParseException,
            offset: Int,
        ): DateTimeParseException {
            val errorIndex = if (exception.cause == null) {
                offset + exception.errorIndex
            } else {
                0
            }
            return DateTimeParseException(
                "Text cannot be parsed to a LocalDateTime",
                input,
                errorIndex,
                exception,
            )
        }

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a LocalDateTime", input, errorIndex)
    }
}
