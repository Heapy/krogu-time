package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** The eras of the Minguo calendar system. */
public enum class MinguoEra : Era {
    BEFORE_ROC,
    ROC;

    override val value: Int
        get() = ordinal

    public companion object {
        /** Obtains the Minguo era for [minguoEra]. */
        @JvmStatic
        public fun of(minguoEra: Int): MinguoEra = when (minguoEra) {
            0 -> BEFORE_ROC
            1 -> ROC
            else -> throw DateTimeException("Invalid era: $minguoEra")
        }
    }
}
