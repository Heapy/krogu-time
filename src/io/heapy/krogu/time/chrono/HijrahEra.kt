package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** The era of the Hijrah calendar system. */
public enum class HijrahEra : Era {
    AH;

    override val value: Int
        get() = 1

    override fun range(field: TemporalField): ValueRange =
        if (field === ChronoField.ERA) ValueRange.of(1, 1) else super.range(field)

    public companion object {
        /** Obtains the Hijrah era for [hijrahEra]. */
        @JvmStatic
        public fun of(hijrahEra: Int): HijrahEra =
            if (hijrahEra == 1) AH else throw DateTimeException("Invalid era: $hijrahEra")
    }
}
