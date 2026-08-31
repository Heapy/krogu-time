package io.heapy.krogu.time

import io.heapy.krogu.time.format.DateTimeFormatter
import io.heapy.krogu.time.format.DateTimeParseException
import io.heapy.krogu.time.internal.addExact
import io.heapy.krogu.time.internal.floorDiv
import io.heapy.krogu.time.internal.floorMod
import io.heapy.krogu.time.internal.multiplyExact
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjuster
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.temporal.TemporalQuery
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * An instantaneous point on the time-line, stored as seconds and nanoseconds
 * from the epoch of 1970-01-01T00:00:00Z.
 */
public class Instant private constructor(
    public val epochSecond: Long,
    public val nano: Int,
) : Temporal, TemporalAdjuster, Comparable<Instant> {
    override fun isSupported(field: TemporalField?): Boolean = when (field) {
        ChronoField.INSTANT_SECONDS,
        ChronoField.NANO_OF_SECOND,
        ChronoField.MICRO_OF_SECOND,
        ChronoField.MILLI_OF_SECOND,
        -> true
        is ChronoField -> false
        else -> field != null && field.isSupportedBy(this)
    }

    override fun isSupported(unit: TemporalUnit?): Boolean =
        if (unit is ChronoUnit) unit <= ChronoUnit.DAYS else unit != null && unit.isSupportedBy(this)

    override fun get(field: TemporalField): Int = when (field) {
        ChronoField.NANO_OF_SECOND -> nano
        ChronoField.MICRO_OF_SECOND -> nano / NANOS_PER_MICRO
        ChronoField.MILLI_OF_SECOND -> nano / NANOS_PER_MILLI
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> range(field).checkValidIntValue(field.getFrom(this), field)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.NANO_OF_SECOND -> nano.toLong()
        ChronoField.MICRO_OF_SECOND -> (nano / NANOS_PER_MICRO).toLong()
        ChronoField.MILLI_OF_SECOND -> (nano / NANOS_PER_MILLI).toLong()
        ChronoField.INSTANT_SECONDS -> epochSecond
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.NANOS as R
        }
        return super<Temporal>.query(query)
    }

    override fun with(adjuster: TemporalAdjuster): Instant =
        if (adjuster is Instant) adjuster else adjuster.adjustInto(this) as Instant

    override fun with(field: TemporalField, newValue: Long): Instant {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        field.checkValidValue(newValue)
        return when (field) {
            ChronoField.NANO_OF_SECOND ->
                if (newValue == nano.toLong()) this else create(epochSecond, newValue.toInt())
            ChronoField.MICRO_OF_SECOND -> {
                val newNano = newValue.toInt() * NANOS_PER_MICRO
                if (newNano == nano) this else create(epochSecond, newNano)
            }
            ChronoField.MILLI_OF_SECOND -> {
                val newNano = newValue.toInt() * NANOS_PER_MILLI
                if (newNano == nano) this else create(epochSecond, newNano)
            }
            ChronoField.INSTANT_SECONDS ->
                if (newValue == epochSecond) this else create(newValue, nano)
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
    }

    /** Truncates this instant to the nearest preceding multiple of [unit]. */
    public fun truncatedTo(unit: TemporalUnit): Instant {
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
        val nanoOfDay = epochSecond % SECONDS_PER_DAY * NANOS_PER_SECOND + nano
        val truncatedNanoOfDay = floorDiv(nanoOfDay, unitNanos) * unitNanos
        return plusNanos(truncatedNanoOfDay - nanoOfDay)
    }

    override fun plus(amount: TemporalAmount): Instant = amount.addTo(this) as Instant

    override fun plus(amountToAdd: Long, unit: TemporalUnit): Instant {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        return when (unit) {
            ChronoUnit.NANOS -> plusNanos(amountToAdd)
            ChronoUnit.MICROS -> plusComponents(
                amountToAdd / MICROS_PER_SECOND,
                amountToAdd % MICROS_PER_SECOND * NANOS_PER_MICRO,
            )
            ChronoUnit.MILLIS -> plusMillis(amountToAdd)
            ChronoUnit.SECONDS -> plusSeconds(amountToAdd)
            ChronoUnit.MINUTES -> plusSeconds(multiplyExact(amountToAdd, SECONDS_PER_MINUTE))
            ChronoUnit.HOURS -> plusSeconds(multiplyExact(amountToAdd, SECONDS_PER_HOUR))
            ChronoUnit.HALF_DAYS -> plusSeconds(multiplyExact(amountToAdd, SECONDS_PER_HALF_DAY))
            ChronoUnit.DAYS -> plusSeconds(multiplyExact(amountToAdd, SECONDS_PER_DAY))
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Returns this instant with [secondsToAdd] seconds added. */
    public fun plusSeconds(secondsToAdd: Long): Instant {
        if (secondsToAdd == 0L) return this
        return create(addExact(epochSecond, secondsToAdd), nano)
    }

    /** Returns this instant with [millisToAdd] milliseconds added. */
    public fun plusMillis(millisToAdd: Long): Instant = plusComponents(
        millisToAdd / MILLIS_PER_SECOND,
        millisToAdd % MILLIS_PER_SECOND * NANOS_PER_MILLI,
    )

    /** Returns this instant with [nanosToAdd] nanoseconds added. */
    public fun plusNanos(nanosToAdd: Long): Instant = plusComponents(0, nanosToAdd)

    override fun minus(amount: TemporalAmount): Instant = amount.subtractFrom(this) as Instant

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): Instant =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    /** Returns this instant with [secondsToSubtract] seconds subtracted. */
    public fun minusSeconds(secondsToSubtract: Long): Instant =
        if (secondsToSubtract == Long.MIN_VALUE) {
            plusSeconds(Long.MAX_VALUE).plusSeconds(1)
        } else {
            plusSeconds(-secondsToSubtract)
        }

    /** Returns this instant with [millisToSubtract] milliseconds subtracted. */
    public fun minusMillis(millisToSubtract: Long): Instant =
        if (millisToSubtract == Long.MIN_VALUE) {
            plusMillis(Long.MAX_VALUE).plusMillis(1)
        } else {
            plusMillis(-millisToSubtract)
        }

    /** Returns this instant with [nanosToSubtract] nanoseconds subtracted. */
    public fun minusNanos(nanosToSubtract: Long): Instant =
        if (nanosToSubtract == Long.MIN_VALUE) {
            plusNanos(Long.MAX_VALUE).plusNanos(1)
        } else {
            plusNanos(-nanosToSubtract)
        }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.INSTANT_SECONDS, epochSecond)
            .with(ChronoField.NANO_OF_SECOND, nano.toLong())

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        return when (unit) {
            ChronoUnit.NANOS -> nanosUntil(end)
            ChronoUnit.MICROS -> microsUntil(end)
            ChronoUnit.MILLIS -> millisUntil(end)
            ChronoUnit.SECONDS -> secondsUntil(end)
            ChronoUnit.MINUTES -> secondsUntil(end) / SECONDS_PER_MINUTE
            ChronoUnit.HOURS -> secondsUntil(end) / SECONDS_PER_HOUR
            ChronoUnit.HALF_DAYS -> secondsUntil(end) / SECONDS_PER_HALF_DAY
            ChronoUnit.DAYS -> secondsUntil(end) / SECONDS_PER_DAY
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Returns the duration from this instant to [endExclusive]. */
    public fun until(endExclusive: Instant): Duration = Duration.ofSeconds(
        endExclusive.epochSecond - epochSecond,
        (endExclusive.nano - nano).toLong(),
    )

    /** Converts this instant to milliseconds from the epoch. */
    public fun toEpochMilli(): Long {
        if (epochSecond < 0 && nano > 0) {
            val millis = multiplyExact(epochSecond + 1, MILLIS_PER_SECOND)
            return addExact(millis, nano / NANOS_PER_MILLI - MILLIS_PER_SECOND)
        }
        return addExact(
            multiplyExact(epochSecond, MILLIS_PER_SECOND),
            (nano / NANOS_PER_MILLI).toLong(),
        )
    }

    /** Combines this instant with [offset]. */
    public fun atOffset(offset: ZoneOffset): OffsetDateTime = OffsetDateTime.ofInstant(this, offset)

    /** Combines this instant with [zone]. */
    public fun atZone(zone: ZoneId): ZonedDateTime = ZonedDateTime.ofInstant(this, zone)

    override fun compareTo(other: Instant): Int {
        val secondsComparison = epochSecond.compareTo(other.epochSecond)
        return if (secondsComparison != 0) secondsComparison else nano.compareTo(other.nano)
    }

    /** Whether this instant occurs after [other]. */
    public fun isAfter(other: Instant): Boolean = compareTo(other) > 0

    /** Whether this instant occurs before [other]. */
    public fun isBefore(other: Instant): Boolean = compareTo(other) < 0

    override fun equals(other: Any?): Boolean =
        this === other || other is Instant && epochSecond == other.epochSecond && nano == other.nano

    override fun hashCode(): Int = epochSecond.hashCode() + 51 * nano

    /** Formats this instant using the ISO-8601 instant representation. */
    override fun toString(): String {
        val epochDay = floorDiv(epochSecond, SECONDS_PER_DAY)
        val secondOfDay = floorMod(epochSecond, SECONDS_PER_DAY).toInt()
        val date = dateFromEpochDay(epochDay)
        val hour = secondOfDay / SECONDS_PER_HOUR.toInt()
        val minute = secondOfDay / SECONDS_PER_MINUTE.toInt() % 60
        val second = secondOfDay % 60
        return buildString {
            appendYear(date.year)
            append('-')
            appendTwoDigits(date.month)
            append('-')
            appendTwoDigits(date.day)
            append('T')
            appendTwoDigits(hour)
            append(':')
            appendTwoDigits(minute)
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
            append('Z')
        }
    }

    /** Formats this instant using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    private fun StringBuilder.appendYear(year: Int) {
        when {
            year in 0..999 -> append(year.toString().padStart(4, '0'))
            year in -999..-1 -> {
                append('-')
                append((-year).toString().padStart(4, '0'))
            }
            year > 9_999 -> {
                append('+')
                append(year)
            }
            else -> append(year)
        }
    }

    private fun StringBuilder.appendTwoDigits(value: Int) {
        if (value < 10) append('0')
        append(value)
    }

    private fun plusComponents(secondsToAdd: Long, nanosToAdd: Long): Instant {
        if (secondsToAdd == 0L && nanosToAdd == 0L) return this
        var newEpochSecond = addExact(epochSecond, secondsToAdd)
        newEpochSecond = addExact(newEpochSecond, nanosToAdd / NANOS_PER_SECOND)
        return ofEpochSecond(newEpochSecond, nano.toLong() + nanosToAdd % NANOS_PER_SECOND)
    }

    private fun nanosUntil(end: Instant): Long = addExact(
        multiplyExact(end.epochSecond - epochSecond, NANOS_PER_SECOND),
        (end.nano - nano).toLong(),
    )

    private fun microsUntil(end: Instant): Long = subsecondUntil(
        end = end,
        unitsPerSecond = MICROS_PER_SECOND,
        nanosPerUnit = NANOS_PER_MICRO,
    )

    private fun millisUntil(end: Instant): Long = subsecondUntil(
        end = end,
        unitsPerSecond = MILLIS_PER_SECOND,
        nanosPerUnit = NANOS_PER_MILLI,
    )

    private fun subsecondUntil(
        end: Instant,
        unitsPerSecond: Long,
        nanosPerUnit: Int,
    ): Long {
        val units = multiplyExact(end.epochSecond - epochSecond, unitsPerSecond)
        val nanosDifference = end.nano - nano
        return when {
            units > 0 && nanosDifference < 0 ->
                units - unitsPerSecond + (nanosDifference + NANOS_PER_SECOND).toInt() / nanosPerUnit
            units < 0 && nanosDifference > 0 ->
                units + unitsPerSecond + (nanosDifference - NANOS_PER_SECOND).toInt() / nanosPerUnit
            else -> addExact(units, (nanosDifference / nanosPerUnit).toLong())
        }
    }

    private fun secondsUntil(end: Instant): Long {
        var secondsDifference = end.epochSecond - epochSecond
        val nanosDifference = end.nano - nano
        if (secondsDifference > 0 && nanosDifference < 0) {
            secondsDifference--
        } else if (secondsDifference < 0 && nanosDifference > 0) {
            secondsDifference++
        }
        return secondsDifference
    }

    public companion object {
        private const val DAYS_PER_CYCLE: Long = 146_097
        private const val DAYS_0000_TO_1970: Long = 719_528
        private const val MAX_PARSE_YEAR: Long = 1_000_000_000
        private const val MIN_SECOND: Long = -31_557_014_167_219_200
        private const val MAX_SECOND: Long = 31_556_889_864_403_199
        private const val NANOS_PER_MICRO: Int = 1_000
        private const val NANOS_PER_MILLI: Int = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val MICROS_PER_SECOND: Long = 1_000_000
        private const val MILLIS_PER_SECOND: Long = 1_000
        private const val SECONDS_PER_MINUTE: Long = 60
        private const val SECONDS_PER_HOUR: Long = 3_600
        private const val SECONDS_PER_HALF_DAY: Long = 43_200
        private const val SECONDS_PER_DAY: Long = 86_400
        private const val NANOS_PER_DAY: Long = SECONDS_PER_DAY * NANOS_PER_SECOND

        @JvmField
        public val EPOCH: Instant = Instant(0, 0)
        @JvmField
        public val MIN: Instant = Instant(MIN_SECOND, 0)
        @JvmField
        public val MAX: Instant = Instant(MAX_SECOND, 999_999_999)

        /** Obtains the current instant from the system UTC clock. */
        @JvmStatic
        public fun now(): Instant = Clock.systemUTC().instant()

        /** Obtains the current instant from [clock]. */
        @JvmStatic
        public fun now(clock: Clock): Instant = clock.instant()

        /** Obtains an instant from seconds since the epoch. */
        @JvmStatic
        public fun ofEpochSecond(epochSecond: Long): Instant = create(epochSecond, 0)

        /** Obtains an instant from seconds and a nanosecond adjustment. */
        @JvmStatic
        public fun ofEpochSecond(epochSecond: Long, nanoAdjustment: Long): Instant = create(
            addExact(epochSecond, floorDiv(nanoAdjustment, NANOS_PER_SECOND)),
            floorMod(nanoAdjustment, NANOS_PER_SECOND).toInt(),
        )

        /** Obtains an instant from milliseconds since the epoch. */
        @JvmStatic
        public fun ofEpochMilli(epochMilli: Long): Instant = create(
            floorDiv(epochMilli, MILLIS_PER_SECOND),
            (floorMod(epochMilli, MILLIS_PER_SECOND) * NANOS_PER_MILLI).toInt(),
        )

        /** Obtains an instant from a temporal accessor. */
        @JvmStatic
        public fun from(temporal: TemporalAccessor): Instant {
            if (temporal is Instant) return temporal
            return try {
                ofEpochSecond(
                    temporal.getLong(ChronoField.INSTANT_SECONDS),
                    temporal.get(ChronoField.NANO_OF_SECOND).toLong(),
                )
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain Instant from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses an instant using the ISO-8601 instant representation. */
        @JvmStatic
        public fun parse(text: CharSequence): Instant {
            val input = text.toString()
            if (input.isEmpty()) throw parseFailure(input, 0)

            var index = 0
            val sign = when (input[0]) {
                '+' -> {
                    index++
                    1
                }
                '-' -> {
                    index++
                    -1
                }
                else -> 1
            }
            val signed = index == 1
            val yearStart = index
            var yearValue = 0L
            while (index < input.length && input[index].isAsciiDigit()) {
                if (index - yearStart < 10) {
                    yearValue = yearValue * 10 + (input[index] - '0')
                }
                index++
            }
            val yearDigits = index - yearStart
            if (yearDigits > 10) throw parseFailure(input, yearStart + 10)
            val validYearWidth = when {
                !signed -> yearDigits == 4
                sign < 0 -> yearDigits in 4..10
                else -> yearDigits in 5..10 && yearValue >= 10_000
            }
            if (
                !validYearWidth ||
                sign < 0 && yearValue == 0L ||
                yearValue > MAX_PARSE_YEAR
            ) {
                throw parseFailure(input, 0)
            }
            if (index >= input.length || input[index] != '-') {
                throw parseFailure(input, index.coerceAtMost(yearStart + 10))
            }

            val monthStart = ++index
            if (!hasTwoDigits(input, monthStart)) throw parseFailure(input, monthStart)
            val month = parseTwoDigits(input, monthStart)
            index += 2
            if (index >= input.length || input[index] != '-') throw parseFailure(input, index)

            val dayStart = ++index
            if (!hasTwoDigits(input, dayStart)) throw parseFailure(input, dayStart)
            val day = parseTwoDigits(input, dayStart)
            index += 2
            if (index >= input.length || input[index] != 'T' && input[index] != 't') {
                throw parseFailure(input, index)
            }

            val hourStart = ++index
            if (!hasTwoDigits(input, hourStart)) throw parseFailure(input, hourStart)
            val hour = parseTwoDigits(input, hourStart)
            index += 2
            if (index >= input.length || input[index] != ':') throw parseFailure(input, index)

            val minuteStart = ++index
            if (!hasTwoDigits(input, minuteStart)) throw parseFailure(input, minuteStart)
            val minute = parseTwoDigits(input, minuteStart)
            index += 2
            if (index >= input.length || input[index] != ':') throw parseFailure(input, index)

            val secondStart = ++index
            if (!hasTwoDigits(input, secondStart)) throw parseFailure(input, secondStart)
            val second = parseTwoDigits(input, secondStart)
            index += 2

            var nano = 0
            if (index < input.length && input[index] == '.') {
                index++
                var digits = 0
                while (index < input.length && digits < 9 && input[index].isAsciiDigit()) {
                    nano = nano * 10 + (input[index] - '0')
                    index++
                    digits++
                }
                if (index < input.length && input[index].isAsciiDigit()) {
                    throw parseFailure(input, index)
                }
                repeat(9 - digits) { nano *= 10 }
            }

            val offsetStart = index
            val offsetSeconds = when {
                index < input.length && (input[index] == 'Z' || input[index] == 'z') -> {
                    index++
                    0
                }
                index < input.length && (input[index] == '+' || input[index] == '-') -> {
                    val offsetSign = if (input[index] == '-') -1 else 1
                    index++
                    if (!hasTwoDigits(input, index)) throw parseFailure(input, offsetStart)
                    val offsetHour = parseTwoDigits(input, index)
                    index += 2
                    if (index >= input.length || input[index] != ':') {
                        throw parseFailure(input, offsetStart)
                    }
                    index++
                    if (!hasTwoDigits(input, index)) throw parseFailure(input, offsetStart)
                    val offsetMinute = parseTwoDigits(input, index)
                    index += 2
                    var offsetSecond = 0
                    if (index < input.length && input[index] == ':') {
                        index++
                        if (!hasTwoDigits(input, index)) throw parseFailure(input, offsetStart)
                        offsetSecond = parseTwoDigits(input, index)
                        index += 2
                    }
                    if (
                        offsetHour > 18 ||
                        offsetMinute > 59 ||
                        offsetSecond > 59 ||
                        offsetHour == 18 && (offsetMinute != 0 || offsetSecond != 0)
                    ) {
                        throw parseFailure(input, 0)
                    }
                    offsetSign * (offsetHour * 3_600 + offsetMinute * 60 + offsetSecond)
                }
                else -> throw parseFailure(input, offsetStart)
            }
            if (index != input.length) throw parseFailure(input, index)

            val year = (if (sign < 0) -yearValue else yearValue).toInt()
            return try {
                if (month !in 1..12) throw DateTimeException("Invalid month")
                val leapYear = Year.isLeap(year.toLong())
                val monthLength = Month.of(month).length(leapYear)
                if (day !in 1..monthLength) throw DateTimeException("Invalid date")
                if (hour !in 0..24 || minute !in 0..59 || second !in 0..60) {
                    throw DateTimeException("Invalid time")
                }
                if (hour == 24 && (minute != 0 || second != 0 || nano != 0)) {
                    throw DateTimeException("Invalid time")
                }
                if (second == 60 && (hour != 23 || minute != 59)) {
                    throw DateTimeException("Invalid leap second")
                }

                val epochDay = toEpochDay(year, month, day)
                val secondOfDay = if (hour == 24) {
                    SECONDS_PER_DAY
                } else {
                    hour * SECONDS_PER_HOUR +
                        minute * SECONDS_PER_MINUTE +
                        minOf(second, 59)
                }
                ofEpochSecond(
                    epochDay * SECONDS_PER_DAY + secondOfDay - offsetSeconds,
                    nano.toLong(),
                )
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to an Instant",
                    input,
                    0,
                    exception,
                )
            }
        }

        /** Parses an instant from [text] using [formatter]. */
        @JvmStatic
        public fun parse(text: CharSequence, formatter: DateTimeFormatter): Instant =
            from(formatter.parse(text))

        private fun toEpochDay(year: Int, month: Int, day: Int): Long {
            val prolepticYear = year.toLong()
            var total = 365L * prolepticYear
            total += if (prolepticYear >= 0) {
                (prolepticYear + 3) / 4 -
                    (prolepticYear + 99) / 100 +
                    (prolepticYear + 399) / 400
            } else {
                -(prolepticYear / -4 - prolepticYear / -100 + prolepticYear / -400)
            }
            total += (367L * month - 362) / 12
            total += day - 1
            if (month > 2) total -= if (Year.isLeap(prolepticYear)) 1 else 2
            return total - DAYS_0000_TO_1970
        }

        private fun dateFromEpochDay(epochDay: Long): InstantDate {
            var zeroDay = epochDay + DAYS_0000_TO_1970 - 60
            var adjust = 0L
            if (zeroDay < 0) {
                val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                adjust = adjustCycles * 400
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
            }
            var yearEstimate = (400 * zeroDay + 591) / DAYS_PER_CYCLE
            var dayEstimate = zeroDay -
                (365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400)
            if (dayEstimate < 0) {
                yearEstimate--
                dayEstimate = zeroDay -
                    (365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400)
            }
            yearEstimate += adjust
            val marchDay = dayEstimate.toInt()
            val marchMonth = (marchDay * 5 + 2) / 153
            val month = (marchMonth + 2) % 12 + 1
            val day = marchDay - (marchMonth * 306 + 5) / 10 + 1
            yearEstimate += (marchMonth / 10).toLong()
            return InstantDate(yearEstimate.toInt(), month, day)
        }

        private fun hasTwoDigits(input: String, index: Int): Boolean =
            index + 1 < input.length &&
                input[index].isAsciiDigit() &&
                input[index + 1].isAsciiDigit()

        private fun parseTwoDigits(input: String, index: Int): Int =
            (input[index] - '0') * 10 + (input[index + 1] - '0')

        private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to an Instant", input, errorIndex)

        private fun create(epochSecond: Long, nano: Int): Instant {
            if (epochSecond !in MIN_SECOND..MAX_SECOND) {
                throw DateTimeException("Instant exceeds minimum or maximum instant")
            }
            return if (epochSecond == 0L && nano == 0) EPOCH else Instant(epochSecond, nano)
        }
    }

    private data class InstantDate(
        val year: Int,
        val month: Int,
        val day: Int,
    )
}
