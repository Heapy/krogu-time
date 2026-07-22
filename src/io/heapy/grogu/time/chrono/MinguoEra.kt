package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException

/** The eras of the Minguo calendar system. */
public enum class MinguoEra : Era {
    BEFORE_ROC,
    ROC;

    override val value: Int
        get() = ordinal

    public companion object {
        /** Obtains the Minguo era for [minguoEra]. */
        public fun of(minguoEra: Int): MinguoEra = when (minguoEra) {
            0 -> BEFORE_ROC
            1 -> ROC
            else -> throw DateTimeException("Invalid era: $minguoEra")
        }
    }
}
