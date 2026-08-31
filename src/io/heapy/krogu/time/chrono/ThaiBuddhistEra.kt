package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** The eras of the Thai Buddhist calendar system. */
public enum class ThaiBuddhistEra : Era {
    BEFORE_BE,
    BE;

    override val value: Int
        get() = ordinal

    public companion object {
        /** Obtains the Thai Buddhist era for [thaiBuddhistEra]. */
        @JvmStatic
        public fun of(thaiBuddhistEra: Int): ThaiBuddhistEra = when (thaiBuddhistEra) {
            0 -> BEFORE_BE
            1 -> BE
            else -> throw DateTimeException("Invalid era: $thaiBuddhistEra")
        }
    }
}
