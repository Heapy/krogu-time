package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.ValueRange

/** A calendar system used to organize and identify dates. */
public interface Chronology : Comparable<Chronology> {
    /** The unique chronology identifier. */
    public val id: String

    /** The CLDR calendar-system identifier, or `null` when none is defined. */
    public val calendarType: String?

    /** Whether this chronology has the same fundamental date structure as ISO. */
    public val isIsoBased: Boolean
        get() = false

    /** Returns whether [prolepticYear] is a leap year in this chronology. */
    public fun isLeapYear(prolepticYear: Long): Boolean

    /** Combines [era] and [yearOfEra] into a proleptic year. */
    public fun prolepticYear(era: Era, yearOfEra: Int): Int

    /** Obtains this chronology's era for [eraValue]. */
    public fun eraOf(eraValue: Int): Era

    /** Returns the eras supported by this chronology. */
    public fun eras(): List<Era>

    /** Returns this chronology's range for [field]. */
    public fun range(field: ChronoField): ValueRange

    override fun compareTo(other: Chronology): Int = id.compareTo(other.id)

    public companion object {
        /** Obtains a chronology from [temporal], defaulting to ISO when none is queried. */
        public fun from(temporal: TemporalAccessor): Chronology =
            temporal.query(TemporalQueries.chronology()) ?: IsoChronology

        /** Obtains an available chronology by its ID or calendar type. */
        public fun of(id: String): Chronology = when (id) {
            IsoChronology.id, IsoChronology.calendarType -> IsoChronology
            else -> throw DateTimeException("Unknown chronology: $id")
        }

        /** Returns the chronologies currently available to this library. */
        public fun getAvailableChronologies(): Set<Chronology> = setOf(IsoChronology)
    }
}
