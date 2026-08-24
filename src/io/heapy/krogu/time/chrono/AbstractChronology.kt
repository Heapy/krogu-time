package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.temporal.TemporalField

/**
 * Recommended base class for calendar systems.
 *
 * It supplies the shared field-resolution and identity behavior used by the
 * built-in chronologies while leaving calendar-specific operations abstract.
 */
public abstract class AbstractChronology protected constructor() : Chronology {
    override fun resolveDate(
        fieldValues: MutableMap<TemporalField, Long>,
        resolverStyle: ResolverStyle,
    ): ChronoLocalDate? = super<Chronology>.resolveDate(fieldValues, resolverStyle)

    override fun compareTo(other: Chronology): Int = id.compareTo(other.id)

    override fun equals(other: Any?): Boolean =
        this === other || other is AbstractChronology && compareTo(other) == 0

    override fun hashCode(): Int = this::class.hashCode() xor id.hashCode()

    override fun toString(): String = id
}
