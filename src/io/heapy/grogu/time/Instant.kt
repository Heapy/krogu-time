package io.heapy.grogu.time

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
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/**
 * An instantaneous point on the time-line, stored as seconds and nanoseconds
 * from the epoch of 1970-01-01T00:00:00Z.
 */
public class Instant private constructor(
    public val epochSecond: Long,
    public val nano: Int,
) : Temporal, TemporalAdjuster, Comparable<Instant> {
    override fun isSupported(field: TemporalField): Boolean = when (field) {
        ChronoField.INSTANT_SECONDS,
        ChronoField.NANO_OF_SECOND,
        ChronoField.MICRO_OF_SECOND,
        ChronoField.MILLI_OF_SECOND,
        -> true
        is ChronoField -> false
        else -> field.isSupportedBy(this)
    }

    override fun isSupported(unit: TemporalUnit): Boolean =
        if (unit is ChronoUnit) unit <= ChronoUnit.DAYS else unit.isSupportedBy(this)

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.NANO_OF_SECOND -> nano.toLong()
        ChronoField.MICRO_OF_SECOND -> (nano / NANOS_PER_MICRO).toLong()
        ChronoField.MILLI_OF_SECOND -> (nano / NANOS_PER_MILLI).toLong()
        ChronoField.INSTANT_SECONDS -> epochSecond
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
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

        public val EPOCH: Instant = Instant(0, 0)
        public val MIN: Instant = Instant(MIN_SECOND, 0)
        public val MAX: Instant = Instant(MAX_SECOND, 999_999_999)

        /** Obtains an instant from seconds since the epoch. */
        public fun ofEpochSecond(epochSecond: Long): Instant = create(epochSecond, 0)

        /** Obtains an instant from seconds and a nanosecond adjustment. */
        public fun ofEpochSecond(epochSecond: Long, nanoAdjustment: Long): Instant = create(
            addExact(epochSecond, floorDiv(nanoAdjustment, NANOS_PER_SECOND)),
            floorMod(nanoAdjustment, NANOS_PER_SECOND).toInt(),
        )

        /** Obtains an instant from milliseconds since the epoch. */
        public fun ofEpochMilli(epochMilli: Long): Instant = create(
            floorDiv(epochMilli, MILLIS_PER_SECOND),
            (floorMod(epochMilli, MILLIS_PER_SECOND) * NANOS_PER_MILLI).toInt(),
        )

        /** Obtains an instant from a temporal accessor. */
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

        private fun create(epochSecond: Long, nano: Int): Instant {
            if (epochSecond !in MIN_SECOND..MAX_SECOND) {
                throw DateTimeException("Instant exceeds minimum or maximum instant")
            }
            return if (epochSecond == 0L && nano == 0) EPOCH else Instant(epochSecond, nano)
        }
    }
}
