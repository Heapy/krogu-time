package io.heapy.grogu.time

import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorMod
import kotlin.time.Clock as KotlinClock

/** A time source that supplies instants using a configured time-zone. */
public abstract class Clock protected constructor() {
    /** The time-zone used when this clock is converted to calendar values. */
    public abstract val zone: ZoneId

    /** Returns an equivalent clock using [zone]. */
    public abstract fun withZone(zone: ZoneId): Clock

    /** Returns the current millisecond instant measured from the Java epoch. */
    public open fun millis(): Long = instant().toEpochMilli()

    /** Returns the current instant supplied by this clock. */
    public abstract fun instant(): Instant

    public companion object {
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
        private val SYSTEM_UTC: Clock = SystemClock(ZoneOffset.UTC)

        /** Obtains the best available system clock in UTC. */
        public fun systemUTC(): Clock = SYSTEM_UTC

        /** Obtains the best available system clock in [zone]. */
        public fun system(zone: ZoneId): Clock =
            if (zone === ZoneOffset.UTC) SYSTEM_UTC else SystemClock(zone)

        /** Obtains a system clock that ticks in whole milliseconds. */
        public fun tickMillis(zone: ZoneId): Clock =
            TickClock(system(zone), NANOS_PER_MILLI)

        /** Obtains a system clock that ticks in whole seconds. */
        public fun tickSeconds(zone: ZoneId): Clock =
            TickClock(system(zone), NANOS_PER_SECOND)

        /** Obtains a system clock that ticks in whole minutes. */
        public fun tickMinutes(zone: ZoneId): Clock =
            TickClock(system(zone), NANOS_PER_MINUTE)

        /** Obtains a clock that truncates [baseClock] to occurrences of [tickDuration]. */
        public fun tick(baseClock: Clock, tickDuration: Duration): Clock {
            require(!tickDuration.isNegative) { "Tick duration must not be negative" }
            val tickNanos = tickDuration.toNanos()
            require(
                tickNanos % NANOS_PER_MILLI == 0L ||
                    NANOS_PER_SECOND % tickNanos == 0L,
            ) { "Invalid tick duration" }
            return if (tickNanos <= 1) baseClock else TickClock(baseClock, tickNanos)
        }

        /** Obtains a clock that always returns [fixedInstant]. */
        public fun fixed(fixedInstant: Instant, zone: ZoneId): Clock =
            FixedClock(fixedInstant, zone)

        /** Obtains a clock that adds [offsetDuration] to [baseClock]. */
        public fun offset(baseClock: Clock, offsetDuration: Duration): Clock =
            if (offsetDuration == Duration.ZERO) baseClock else OffsetClock(baseClock, offsetDuration)
    }
}

private class SystemClock(
    override val zone: ZoneId,
) : Clock() {
    override fun withZone(zone: ZoneId): Clock =
        if (zone == this.zone) this else SystemClock(zone)

    override fun millis(): Long = KotlinClock.System.now().toEpochMilliseconds()

    override fun instant(): Instant {
        val instant = KotlinClock.System.now()
        return Instant.ofEpochSecond(instant.epochSeconds, instant.nanosecondsOfSecond.toLong())
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is SystemClock && zone == other.zone

    override fun hashCode(): Int = zone.hashCode() + 1

    override fun toString(): String = "SystemClock[$zone]"
}

private class FixedClock(
    private val fixedInstant: Instant,
    override val zone: ZoneId,
) : Clock() {
    override fun withZone(zone: ZoneId): Clock =
        if (zone == this.zone) this else FixedClock(fixedInstant, zone)

    override fun millis(): Long = fixedInstant.toEpochMilli()

    override fun instant(): Instant = fixedInstant

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is FixedClock && fixedInstant == other.fixedInstant && zone == other.zone

    override fun hashCode(): Int = fixedInstant.hashCode() xor zone.hashCode()

    override fun toString(): String = "FixedClock[$fixedInstant,$zone]"
}

private class OffsetClock(
    private val baseClock: Clock,
    private val offset: Duration,
) : Clock() {
    override val zone: ZoneId get() = baseClock.zone

    override fun withZone(zone: ZoneId): Clock =
        if (zone == baseClock.zone) this else OffsetClock(baseClock.withZone(zone), offset)

    override fun millis(): Long = addExact(baseClock.millis(), offset.toMillis())

    override fun instant(): Instant = baseClock.instant().plus(offset)

    override fun equals(other: Any?): Boolean =
        this === other || other is OffsetClock && baseClock == other.baseClock && offset == other.offset

    override fun hashCode(): Int = baseClock.hashCode() xor offset.hashCode()

    override fun toString(): String = "OffsetClock[$baseClock,$offset]"
}

private class TickClock(
    private val baseClock: Clock,
    private val tickNanos: Long,
) : Clock() {
    override val zone: ZoneId get() = baseClock.zone

    override fun withZone(zone: ZoneId): Clock =
        if (zone == baseClock.zone) this else TickClock(baseClock.withZone(zone), tickNanos)

    override fun millis(): Long {
        val millis = baseClock.millis()
        val tickMillis = tickNanos / NANOS_PER_MILLI
        return millis - floorMod(millis, tickMillis)
    }

    override fun instant(): Instant {
        if (tickNanos % NANOS_PER_MILLI == 0L) {
            val millis = baseClock.millis()
            val tickMillis = tickNanos / NANOS_PER_MILLI
            return Instant.ofEpochMilli(millis - floorMod(millis, tickMillis))
        }
        val instant = baseClock.instant()
        return instant.minusNanos(floorMod(instant.nano.toLong(), tickNanos))
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is TickClock && tickNanos == other.tickNanos && baseClock == other.baseClock

    override fun hashCode(): Int = baseClock.hashCode() xor tickNanos.hashCode()

    override fun toString(): String = "TickClock[$baseClock,${Duration.ofNanos(tickNanos)}]"

    private companion object {
        const val NANOS_PER_MILLI: Long = 1_000_000
    }
}
