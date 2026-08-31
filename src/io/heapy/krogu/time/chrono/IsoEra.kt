package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** The eras of the ISO calendar system. */
public enum class IsoEra : Era {
    BCE,
    CE;

    override val value: Int
        get() = ordinal

    public companion object {
        /** Obtains the ISO era for [isoEra]. */
        @JvmStatic
        public fun of(isoEra: Int): IsoEra = when (isoEra) {
            0 -> BCE
            1 -> CE
            else -> throw DateTimeException("Invalid era: $isoEra")
        }
    }
}
