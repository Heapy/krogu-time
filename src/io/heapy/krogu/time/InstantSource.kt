package io.heapy.krogu.time

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A source of instants on the time-line. */
public interface InstantSource {
    /** Returns the current instant supplied by this source. */
    public fun instant(): Instant

    /** Returns the current millisecond instant measured from the Java epoch. */
    public fun millis(): Long = instant().toEpochMilli()

    /** Combines this source with [zone] to create a clock. */
    public fun withZone(zone: ZoneId): Clock = SourceClock(this, zone)

    public companion object {
        /** Obtains the best available system instant source. */
        @JvmStatic
        public fun system(): InstantSource = SystemInstantSource

        /** Obtains a source that truncates [baseSource] to [tickDuration]. */
        @JvmStatic
        public fun tick(baseSource: InstantSource, tickDuration: Duration): InstantSource =
            Clock.tick(baseSource.withZone(ZoneOffset.UTC), tickDuration)

        /** Obtains a source that always returns [fixedInstant]. */
        @JvmStatic
        public fun fixed(fixedInstant: Instant): InstantSource =
            Clock.fixed(fixedInstant, ZoneOffset.UTC)

        /** Obtains a source that adds [offsetDuration] to [baseSource]. */
        @JvmStatic
        public fun offset(
            baseSource: InstantSource,
            offsetDuration: Duration,
        ): InstantSource = Clock.offset(
            baseSource.withZone(ZoneOffset.UTC),
            offsetDuration,
        )
    }
}

private object SystemInstantSource : InstantSource {
    override fun instant(): Instant = Clock.systemUTC().instant()

    override fun millis(): Long = Clock.systemUTC().millis()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun toString(): String = "SystemInstantSource"
}

private class SourceClock(
    private val baseSource: InstantSource,
    override val zone: ZoneId,
) : Clock() {
    override fun withZone(zone: ZoneId): Clock =
        if (zone == this.zone) this else SourceClock(baseSource, zone)

    override fun millis(): Long = baseSource.millis()

    override fun instant(): Instant = baseSource.instant()

    override fun equals(other: Any?): Boolean =
        this === other || other is SourceClock && baseSource == other.baseSource && zone == other.zone

    override fun hashCode(): Int = baseSource.hashCode() xor zone.hashCode()

    override fun toString(): String = "SourceClock[$baseSource,$zone]"
}
