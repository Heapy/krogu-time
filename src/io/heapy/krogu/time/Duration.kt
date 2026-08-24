package io.heapy.krogu.time

import io.heapy.krogu.time.format.DateTimeParseException
import io.heapy.krogu.time.internal.addExact
import io.heapy.krogu.time.internal.floorDiv
import io.heapy.krogu.time.internal.floorMod
import io.heapy.krogu.time.internal.multiplyExact
import io.heapy.krogu.time.internal.Unsigned128
import io.heapy.krogu.time.internal.unsignedMagnitude
import io.heapy.krogu.time.internal.unsignedMultiplyAdd
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException

/**
 * A time-based amount stored as seconds and a nanosecond adjustment.
 *
 * The nanosecond component is always normalized to `0..999,999,999`. A
 * negative fractional duration therefore has a negative [seconds] component
 * and a positive [nano] adjustment.
 */
public class Duration private constructor(
    public val seconds: Long,
    public val nano: Int,
) : TemporalAmount, Comparable<Duration> {
    /** The units supported by [get], in descending order. */
    override val units: List<TemporalUnit>
        get() = listOf(ChronoUnit.SECONDS, ChronoUnit.NANOS)

    /** Whether this duration has no length. */
    public val isZero: Boolean
        get() = seconds == 0L && nano == 0

    /** Whether this duration is greater than zero. */
    public val isPositive: Boolean
        get() = seconds > 0 || seconds == 0L && nano > 0

    /** Whether this duration is less than zero. */
    public val isNegative: Boolean
        get() = seconds < 0

    /** Returns a copy with [seconds], retaining the nanosecond component. */
    public fun withSeconds(seconds: Long): Duration = create(seconds, nano)

    /** Returns a copy with [nanoOfSecond], retaining the seconds component. */
    public fun withNanos(nanoOfSecond: Int): Duration {
        if (nanoOfSecond !in 0..<NANOS_PER_SECOND.toInt()) {
            throw DateTimeException(
                "Invalid value for NanoOfSecond (valid values 0 - 999999999): $nanoOfSecond",
            )
        }
        return create(seconds, nanoOfSecond)
    }

    /** Returns this duration with [other] added. */
    public operator fun plus(other: Duration): Duration =
        plusComponents(other.seconds, other.nano.toLong())

    /** Returns this duration with [amountToAdd] of [unit] added. */
    public fun plus(amountToAdd: Long, unit: TemporalUnit): Duration {
        if (unit === ChronoUnit.DAYS) return plusDays(amountToAdd)
        if (unit.isDurationEstimated) {
            throw UnsupportedTemporalTypeException("Unit must not have an estimated duration")
        }
        if (amountToAdd == 0L) return this

        return when (unit) {
            ChronoUnit.NANOS -> plusNanos(amountToAdd)
            ChronoUnit.MICROS -> plusSeconds(
                amountToAdd / MICROS_PER_SECOND,
            ).plusNanos(amountToAdd % MICROS_PER_SECOND * NANOS_PER_MICRO)
            ChronoUnit.MILLIS -> plusMillis(amountToAdd)
            ChronoUnit.SECONDS -> plusSeconds(amountToAdd)
            is ChronoUnit -> plusSeconds(multiplyExact(unit.duration.seconds, amountToAdd))
            else -> plus(unit.duration.multipliedBy(amountToAdd))
        }
    }

    /** Returns this duration with standard 24-hour days added. */
    public fun plusDays(daysToAdd: Long): Duration =
        plusComponents(multiplyExact(daysToAdd, SECONDS_PER_DAY), 0)

    /** Returns this duration with hours added. */
    public fun plusHours(hoursToAdd: Long): Duration =
        plusComponents(multiplyExact(hoursToAdd, SECONDS_PER_HOUR), 0)

    /** Returns this duration with minutes added. */
    public fun plusMinutes(minutesToAdd: Long): Duration =
        plusComponents(multiplyExact(minutesToAdd, SECONDS_PER_MINUTE), 0)

    /** Returns this duration with seconds added. */
    public fun plusSeconds(secondsToAdd: Long): Duration = plusComponents(secondsToAdd, 0)

    /** Returns this duration with milliseconds added. */
    public fun plusMillis(millisToAdd: Long): Duration = plusComponents(
        millisToAdd / MILLIS_PER_SECOND,
        millisToAdd % MILLIS_PER_SECOND * NANOS_PER_MILLI,
    )

    /** Returns this duration with nanoseconds added. */
    public fun plusNanos(nanosToAdd: Long): Duration = plusComponents(0, nanosToAdd)

    /** Returns this duration with [other] subtracted. */
    public operator fun minus(other: Duration): Duration {
        if (other.seconds == Long.MIN_VALUE) {
            return plusComponents(Long.MAX_VALUE, -other.nano.toLong())
                .plusComponents(1, 0)
        }
        return plusComponents(-other.seconds, -other.nano.toLong())
    }

    /** Returns this duration with [amountToSubtract] of [unit] subtracted. */
    public fun minus(amountToSubtract: Long, unit: TemporalUnit): Duration =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    /** Returns this duration with standard 24-hour days subtracted. */
    public fun minusDays(daysToSubtract: Long): Duration =
        if (daysToSubtract == Long.MIN_VALUE) {
            plusDays(Long.MAX_VALUE).plusDays(1)
        } else {
            plusDays(-daysToSubtract)
        }

    /** Returns this duration with hours subtracted. */
    public fun minusHours(hoursToSubtract: Long): Duration =
        if (hoursToSubtract == Long.MIN_VALUE) {
            plusHours(Long.MAX_VALUE).plusHours(1)
        } else {
            plusHours(-hoursToSubtract)
        }

    /** Returns this duration with minutes subtracted. */
    public fun minusMinutes(minutesToSubtract: Long): Duration =
        if (minutesToSubtract == Long.MIN_VALUE) {
            plusMinutes(Long.MAX_VALUE).plusMinutes(1)
        } else {
            plusMinutes(-minutesToSubtract)
        }

    /** Returns this duration with seconds subtracted. */
    public fun minusSeconds(secondsToSubtract: Long): Duration =
        if (secondsToSubtract == Long.MIN_VALUE) {
            plusSeconds(Long.MAX_VALUE).plusSeconds(1)
        } else {
            plusSeconds(-secondsToSubtract)
        }

    /** Returns this duration with milliseconds subtracted. */
    public fun minusMillis(millisToSubtract: Long): Duration =
        if (millisToSubtract == Long.MIN_VALUE) {
            plusMillis(Long.MAX_VALUE).plusMillis(1)
        } else {
            plusMillis(-millisToSubtract)
        }

    /** Returns this duration with nanoseconds subtracted. */
    public fun minusNanos(nanosToSubtract: Long): Duration =
        if (nanosToSubtract == Long.MIN_VALUE) {
            plusNanos(Long.MAX_VALUE).plusNanos(1)
        } else {
            plusNanos(-nanosToSubtract)
        }

    /** Returns this duration with its sign reversed. */
    public fun negated(): Duration = ZERO - this

    /** Returns this duration multiplied by [multiplicand]. */
    public fun multipliedBy(multiplicand: Long): Duration {
        if (multiplicand == 0L) return ZERO
        if (multiplicand == 1L) return this

        val integralSeconds: Long
        val fractionalNanos: Long
        if (seconds < 0 && nano > 0) {
            integralSeconds = seconds + 1
            fractionalNanos = nano.toLong() - NANOS_PER_SECOND
        } else {
            integralSeconds = seconds
            fractionalNanos = nano.toLong()
        }

        val multiplierSeconds = multiplicand / NANOS_PER_SECOND
        val multiplierNanos = multiplicand % NANOS_PER_SECOND
        val wholeFractionSeconds = multiplyExact(fractionalNanos, multiplierSeconds)
        val partialFraction = multiplyExact(fractionalNanos, multiplierNanos)
        val fractionSeconds = addExact(
            wholeFractionSeconds,
            partialFraction / NANOS_PER_SECOND,
        )
        val resultSeconds = addExact(
            multiplyExact(integralSeconds, multiplicand),
            fractionSeconds,
        )
        return ofSeconds(resultSeconds, partialFraction % NANOS_PER_SECOND)
    }

    /** Returns this duration divided by [divisor], truncated to nanosecond precision. */
    public fun dividedBy(divisor: Long): Duration {
        if (divisor == 0L) throw ArithmeticException("Division by zero")
        if (isZero) return ZERO

        val quotientNanos = magnitudeInNanos().dividedBy(
            Unsigned128(0uL, unsignedMagnitude(divisor)),
        ).quotient
        val split = quotientNanos.dividedBy(NANOS_PER_SECOND_128)
        val negative = isNegative xor (divisor < 0)
        return fromMagnitude(
            secondsMagnitude = split.quotient,
            nanoMagnitude = split.remainder.low,
            negative = negative,
        )
    }

    /** Returns the whole-number ratio between this duration and [divisor]. */
    public fun dividedBy(divisor: Duration): Long {
        if (divisor.isZero) throw ArithmeticException("Division by zero")

        val quotient = magnitudeInNanos().dividedBy(divisor.magnitudeInNanos()).quotient
        if (quotient.high != 0uL) throw ArithmeticException("long overflow")
        val negative = isNegative xor divisor.isNegative
        return signedLong(quotient.low, negative)
    }

    /** Returns a duration with the same magnitude and a non-negative sign. */
    public fun abs(): Duration = if (isNegative) negated() else this

    /** Kotlin-named alias for [abs]. */
    public fun absoluteValue(): Duration = abs()

    /** Returns the value of this duration in [unit]. */
    override fun get(unit: TemporalUnit): Long = when (unit) {
        ChronoUnit.SECONDS -> seconds
        ChronoUnit.NANOS -> nano.toLong()
        else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }

    /** Adds this duration to [temporal], applying seconds before nanoseconds. */
    override fun addTo(temporal: Temporal): Temporal {
        var result = temporal
        if (seconds != 0L) result = result.plus(seconds, ChronoUnit.SECONDS)
        if (nano != 0) result = result.plus(nano.toLong(), ChronoUnit.NANOS)
        return result
    }

    /** Subtracts this duration from [temporal], applying seconds before nanoseconds. */
    override fun subtractFrom(temporal: Temporal): Temporal {
        var result = temporal
        if (seconds != 0L) result = result.minus(seconds, ChronoUnit.SECONDS)
        if (nano != 0) result = result.minus(nano.toLong(), ChronoUnit.NANOS)
        return result
    }

    /** Returns the number of whole standard 24-hour days in this duration. */
    public fun toDays(): Long = seconds / SECONDS_PER_DAY

    /** Returns the number of whole hours in this duration. */
    public fun toHours(): Long = seconds / SECONDS_PER_HOUR

    /** Returns the number of whole minutes in this duration. */
    public fun toMinutes(): Long = seconds / SECONDS_PER_MINUTE

    /** Returns the normalized seconds value of this duration. */
    public fun toSeconds(): Long = seconds

    /** Returns the total number of milliseconds, failing if it does not fit in a [Long]. */
    public fun toMillis(): Long = toExactUnits(MILLIS_PER_SECOND, NANOS_PER_MILLI.toLong())

    /** Returns the total number of nanoseconds, failing if it does not fit in a [Long]. */
    public fun toNanos(): Long = toExactUnits(NANOS_PER_SECOND, 1)

    /** Returns the whole standard-day part of this duration. */
    public fun toDaysPart(): Long = toDays()

    /** Returns the hour part within the standard day. */
    public fun toHoursPart(): Int = (toHours() % HOURS_PER_DAY).toInt()

    /** Returns the minute part within the hour. */
    public fun toMinutesPart(): Int = (toMinutes() % MINUTES_PER_HOUR).toInt()

    /** Returns the second part within the minute. */
    public fun toSecondsPart(): Int = (seconds % SECONDS_PER_MINUTE).toInt()

    /** Returns the millisecond part within the normalized second. */
    public fun toMillisPart(): Int = nano / NANOS_PER_MILLI

    /** Returns the nanosecond part within the normalized second. */
    public fun toNanosPart(): Int = nano

    /** Returns this duration truncated toward zero to a multiple of [unit]. */
    public fun truncatedTo(unit: TemporalUnit): Duration {
        if (unit === ChronoUnit.SECONDS && (seconds >= 0 || nano == 0)) {
            return create(seconds, 0)
        }
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
        val nanosWithinDay =
            (seconds % SECONDS_PER_DAY) * NANOS_PER_SECOND + nano
        val truncatedNanos = nanosWithinDay / unitNanos * unitNanos
        return plusNanos(truncatedNanos - nanosWithinDay)
    }

    private fun toExactUnits(unitsPerSecond: Long, nanosPerUnit: Long): Long {
        val wholeSeconds: Long
        val fractionalNanos: Long
        if (seconds < 0) {
            wholeSeconds = seconds + 1
            fractionalNanos = nano.toLong() - NANOS_PER_SECOND
        } else {
            wholeSeconds = seconds
            fractionalNanos = nano.toLong()
        }
        return addExact(
            multiplyExact(wholeSeconds, unitsPerSecond),
            fractionalNanos / nanosPerUnit,
        )
    }

    private fun magnitudeInNanos(): Unsigned128 {
        val wholeSeconds: Long
        val fractionalNanos: UInt
        if (isNegative && nano > 0) {
            wholeSeconds = seconds + 1
            fractionalNanos = (NANOS_PER_SECOND.toInt() - nano).toUInt()
        } else {
            wholeSeconds = seconds
            fractionalNanos = nano.toUInt()
        }
        return unsignedMultiplyAdd(
            value = unsignedMagnitude(wholeSeconds),
            multiplier = NANOS_PER_SECOND.toUInt(),
            addend = fractionalNanos,
        )
    }

    private fun plusComponents(secondsToAdd: Long, nanosToAdd: Long): Duration {
        if (secondsToAdd == 0L && nanosToAdd == 0L) return this

        var resultSeconds = addExact(seconds, secondsToAdd)
        resultSeconds = addExact(resultSeconds, nanosToAdd / NANOS_PER_SECOND)
        val nanoAdjustment = nano.toLong() + nanosToAdd % NANOS_PER_SECOND
        return ofSeconds(resultSeconds, nanoAdjustment)
    }

    override fun compareTo(other: Duration): Int {
        val secondsComparison = seconds.compareTo(other.seconds)
        return if (secondsComparison != 0) secondsComparison else nano.compareTo(other.nano)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Duration && seconds == other.seconds && nano == other.nano

    override fun hashCode(): Int = seconds.hashCode() + 51 * nano

    override fun toString(): String {
        if (isZero) return "PT0S"

        val hasNegativeFraction = seconds < 0 && nano > 0
        val effectiveSeconds = if (hasNegativeFraction) seconds + 1 else seconds
        val hours = effectiveSeconds / SECONDS_PER_HOUR
        val minutes = ((effectiveSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt()
        val remainingSeconds = (effectiveSeconds % SECONDS_PER_MINUTE).toInt()

        return buildString {
            append("PT")
            if (hours != 0L) append(hours).append('H')
            if (minutes != 0) append(minutes).append('M')
            if (remainingSeconds == 0 && nano == 0 && length > 2) return@buildString

            if (hasNegativeFraction && remainingSeconds == 0) {
                append("-0")
            } else {
                append(remainingSeconds)
            }

            if (nano > 0) {
                val fraction = if (hasNegativeFraction) NANOS_PER_SECOND.toInt() - nano else nano
                append('.')
                append(fraction.toString().padStart(9, '0').trimEnd('0'))
            }
            append('S')
        }
    }

    public companion object {
        private const val NANOS_PER_MICRO: Long = 1_000
        private const val NANOS_PER_MILLI: Int = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_DAY: Long = 86_400_000_000_000
        private const val MICROS_PER_SECOND: Long = 1_000_000
        private const val MILLIS_PER_SECOND: Long = 1_000
        private const val MINUTES_PER_HOUR: Long = 60
        private const val HOURS_PER_DAY: Long = 24
        private const val SECONDS_PER_MINUTE: Long = 60
        private const val SECONDS_PER_HOUR: Long = 3_600
        private const val SECONDS_PER_DAY: Long = 86_400
        private val NANOS_PER_SECOND_128: Unsigned128 = Unsigned128(0uL, 1_000_000_000uL)

        /** The canonical zero-length duration. */
        public val ZERO: Duration = Duration(0, 0)

        /** Creates a duration from [amount] measured in [unit]. */
        public fun of(amount: Long, unit: TemporalUnit): Duration = ZERO.plus(amount, unit)

        /** Creates a duration from any temporal amount. */
        public fun from(amount: TemporalAmount): Duration {
            var duration = ZERO
            amount.units.forEach { unit ->
                duration = duration.plus(amount.get(unit), unit)
            }
            return duration
        }

        /** Parses a Java-compatible ISO-8601 duration. */
        public fun parse(text: CharSequence): Duration {
            val input = text.toString()
            val match = DURATION_PATTERN.matchEntire(input)
                ?: throw parseFailure(input)
            val timeSection = match.groups[3]?.value
            if (timeSection?.equals("T", ignoreCase = true) == true) {
                throw parseFailure(input)
            }

            val days = match.groups[2]?.value
            val hours = match.groups[4]?.value
            val minutes = match.groups[5]?.value
            val seconds = match.groups[6]?.value
            if (days == null && hours == null && minutes == null && seconds == null) {
                throw parseFailure(input)
            }

            val daysAsSeconds = parseNumber(input, days, SECONDS_PER_DAY, "days")
            val hoursAsSeconds = parseNumber(input, hours, SECONDS_PER_HOUR, "hours")
            val minutesAsSeconds = parseNumber(input, minutes, SECONDS_PER_MINUTE, "minutes")
            val secondsValue = parseNumber(input, seconds, 1, "seconds")
            val fraction = parseFraction(
                input = input,
                value = match.groups[7]?.value,
                sign = if (seconds?.startsWith('-') == true) -1 else 1,
            )

            try {
                val totalSeconds = addExact(
                    daysAsSeconds,
                    addExact(hoursAsSeconds, addExact(minutesAsSeconds, secondsValue)),
                )
                val duration = ofSeconds(totalSeconds, fraction.toLong())
                return if (match.groups[1]?.value == "-") duration.negated() else duration
            } catch (exception: ArithmeticException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a Duration: overflow",
                    input,
                    0,
                    exception,
                )
            }
        }

        /** Calculates the duration from [startInclusive] to [endExclusive]. */
        public fun between(startInclusive: Temporal, endExclusive: Temporal): Duration {
            var seconds = startInclusive.until(endExclusive, ChronoUnit.SECONDS)
            if (seconds == 0L) {
                return ofNanos(startInclusive.until(endExclusive, ChronoUnit.NANOS))
            }

            val nanos = try {
                endExclusive.getLong(ChronoField.NANO_OF_SECOND) -
                    startInclusive.getLong(ChronoField.NANO_OF_SECOND)
            } catch (_: DateTimeException) {
                0
            }
            if (nanos < 0 && seconds > 0) {
                seconds++
            } else if (nanos > 0 && seconds < 0) {
                seconds--
            }
            return ofSeconds(seconds, nanos)
        }

        /** Creates a duration from standard 24-hour days. */
        public fun ofDays(days: Long): Duration =
            create(multiplyExact(days, SECONDS_PER_DAY), 0)

        /** Creates a duration from standard 60-minute hours. */
        public fun ofHours(hours: Long): Duration =
            create(multiplyExact(hours, SECONDS_PER_HOUR), 0)

        /** Creates a duration from 60-second minutes. */
        public fun ofMinutes(minutes: Long): Duration =
            create(multiplyExact(minutes, SECONDS_PER_MINUTE), 0)

        /** Creates a duration from whole seconds. */
        public fun ofSeconds(seconds: Long): Duration = create(seconds, 0)

        /** Creates a duration from seconds and an arbitrary nanosecond adjustment. */
        public fun ofSeconds(seconds: Long, nanoAdjustment: Long): Duration {
            val normalizedSeconds = addExact(seconds, floorDiv(nanoAdjustment, NANOS_PER_SECOND))
            val normalizedNanos = floorMod(nanoAdjustment, NANOS_PER_SECOND).toInt()
            return create(normalizedSeconds, normalizedNanos)
        }

        /** Creates a duration from milliseconds. */
        public fun ofMillis(millis: Long): Duration {
            val normalizedSeconds = floorDiv(millis, MILLIS_PER_SECOND)
            val millisOfSecond = floorMod(millis, MILLIS_PER_SECOND).toInt()
            return create(normalizedSeconds, millisOfSecond * NANOS_PER_MILLI)
        }

        /** Creates a duration from nanoseconds. */
        public fun ofNanos(nanos: Long): Duration {
            val normalizedSeconds = floorDiv(nanos, NANOS_PER_SECOND)
            val nanosOfSecond = floorMod(nanos, NANOS_PER_SECOND).toInt()
            return create(normalizedSeconds, nanosOfSecond)
        }

        private fun create(seconds: Long, nano: Int): Duration =
            if (seconds == 0L && nano == 0) ZERO else Duration(seconds, nano)

        private fun fromMagnitude(
            secondsMagnitude: Unsigned128,
            nanoMagnitude: ULong,
            negative: Boolean,
        ): Duration {
            if (secondsMagnitude.high != 0uL || nanoMagnitude >= NANOS_PER_SECOND.toULong()) {
                throw ArithmeticException("Exceeds capacity of Duration")
            }
            val wholeSeconds = secondsMagnitude.low
            if (!negative) {
                if (wholeSeconds > Long.MAX_VALUE.toULong()) {
                    throw ArithmeticException("Exceeds capacity of Duration")
                }
                return create(wholeSeconds.toLong(), nanoMagnitude.toInt())
            }
            if (wholeSeconds == 0uL && nanoMagnitude == 0uL) return ZERO
            if (nanoMagnitude == 0uL) {
                val minMagnitude = Long.MAX_VALUE.toULong() + 1uL
                if (wholeSeconds > minMagnitude) {
                    throw ArithmeticException("Exceeds capacity of Duration")
                }
                val signedSeconds = if (wholeSeconds == minMagnitude) {
                    Long.MIN_VALUE
                } else {
                    -wholeSeconds.toLong()
                }
                return create(signedSeconds, 0)
            }
            if (wholeSeconds > Long.MAX_VALUE.toULong()) {
                throw ArithmeticException("Exceeds capacity of Duration")
            }
            return create(
                seconds = -wholeSeconds.toLong() - 1,
                nano = (NANOS_PER_SECOND.toULong() - nanoMagnitude).toInt(),
            )
        }

        private fun signedLong(magnitude: ULong, negative: Boolean): Long {
            if (!negative) {
                if (magnitude > Long.MAX_VALUE.toULong()) {
                    throw ArithmeticException("long overflow")
                }
                return magnitude.toLong()
            }
            val minMagnitude = Long.MAX_VALUE.toULong() + 1uL
            if (magnitude > minMagnitude) throw ArithmeticException("long overflow")
            return if (magnitude == minMagnitude) Long.MIN_VALUE else -magnitude.toLong()
        }

        private fun parseNumber(
            input: String,
            value: String?,
            multiplier: Long,
            description: String,
        ): Long {
            if (value == null) return 0
            try {
                return multiplyExact(value.toLong(), multiplier)
            } catch (exception: NumberFormatException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a Duration: $description",
                    input,
                    0,
                    exception,
                )
            } catch (exception: ArithmeticException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a Duration: $description",
                    input,
                    0,
                    exception,
                )
            }
        }

        private fun parseFraction(input: String, value: String?, sign: Int): Int {
            if (value.isNullOrEmpty()) return 0
            try {
                var fraction = value.toInt()
                repeat(9 - value.length) {
                    fraction *= 10
                }
                return fraction * sign
            } catch (exception: NumberFormatException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a Duration: fraction",
                    input,
                    0,
                    exception,
                )
            } catch (exception: ArithmeticException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a Duration: fraction",
                    input,
                    0,
                    exception,
                )
            }
        }

        private fun parseFailure(input: String): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a Duration", input, 0)

        private val DURATION_PATTERN: Regex = Regex(
            """([-+]?)[Pp](?:([-+]?[0-9]+)[Dd])?([Tt](?:([-+]?[0-9]+)[Hh])?(?:([-+]?[0-9]+)[Mm])?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?[Ss])?)?""",
        )
    }
}
