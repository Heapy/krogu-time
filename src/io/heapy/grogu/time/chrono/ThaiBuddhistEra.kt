package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException

/** The eras of the Thai Buddhist calendar system. */
public enum class ThaiBuddhistEra : Era {
    BEFORE_BE,
    BE;

    override val value: Int
        get() = ordinal

    public companion object {
        /** Obtains the Thai Buddhist era for [thaiBuddhistEra]. */
        public fun of(thaiBuddhistEra: Int): ThaiBuddhistEra = when (thaiBuddhistEra) {
            0 -> BEFORE_BE
            1 -> BE
            else -> throw DateTimeException("Invalid era: $thaiBuddhistEra")
        }
    }
}
