package io.heapy.grogu.time

import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorDiv
import io.heapy.grogu.time.internal.floorMod
import io.heapy.grogu.time.internal.multiplyExact

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
) : Comparable<Duration> {
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
        private const val NANOS_PER_MILLI: Int = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val MILLIS_PER_SECOND: Long = 1_000
        private const val SECONDS_PER_MINUTE: Long = 60
        private const val SECONDS_PER_HOUR: Long = 3_600
        private const val SECONDS_PER_DAY: Long = 86_400

        /** The canonical zero-length duration. */
        public val ZERO: Duration = Duration(0, 0)

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
    }
}
