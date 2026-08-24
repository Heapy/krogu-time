package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.temporal.TemporalAmount

/** A date-based amount whose units are defined by a [Chronology]. */
public interface ChronoPeriod : TemporalAmount {
    /** The chronology that defines this period's units. */
    public val chronology: Chronology

    /** Whether every supported unit has a zero value. */
    public val isZero: Boolean
        get() = units.all { get(it) == 0L }

    /** Whether any supported unit has a negative value. */
    public val isNegative: Boolean
        get() = units.any { get(it) < 0L }

    /** Returns this period with [amountToAdd] added. */
    public fun plus(amountToAdd: TemporalAmount): ChronoPeriod

    /** Returns this period with [amountToSubtract] subtracted. */
    public fun minus(amountToSubtract: TemporalAmount): ChronoPeriod

    /** Returns this period with each supported value multiplied by [scalar]. */
    public fun multipliedBy(scalar: Int): ChronoPeriod

    /** Returns this period with each supported value negated. */
    public fun negated(): ChronoPeriod = multipliedBy(-1)

    /** Returns this period normalized according to its chronology. */
    public fun normalized(): ChronoPeriod

    public companion object {
        /** Calculates the chronology-specific period between two dates. */
        public fun between(
            startDateInclusive: ChronoLocalDate,
            endDateExclusive: ChronoLocalDate,
        ): ChronoPeriod = startDateInclusive.until(endDateExclusive)
    }
}
