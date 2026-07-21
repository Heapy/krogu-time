package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException

/** The eras of the ISO calendar system. */
public enum class IsoEra : Era {
    BCE,
    CE;

    override val value: Int
        get() = ordinal

    public companion object {
        /** Obtains the ISO era for [isoEra]. */
        public fun of(isoEra: Int): IsoEra = when (isoEra) {
            0 -> BCE
            1 -> CE
            else -> throw DateTimeException("Invalid era: $isoEra")
        }
    }
}
