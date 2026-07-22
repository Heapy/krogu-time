package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.ZoneId
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

    /** Obtains a date from an era, year-of-era, month, and day. */
    public fun date(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
    ): ChronoLocalDate = date(prolepticYear(era, yearOfEra), month, dayOfMonth)

    /** Obtains a date from a proleptic year, month, and day. */
    public fun date(
        prolepticYear: Int,
        month: Int,
        dayOfMonth: Int,
    ): ChronoLocalDate

    /** Obtains a date from an era, year-of-era, and day-of-year. */
    public fun dateYearDay(
        era: Era,
        yearOfEra: Int,
        dayOfYear: Int,
    ): ChronoLocalDate = dateYearDay(prolepticYear(era, yearOfEra), dayOfYear)

    /** Obtains a date from a proleptic year and day-of-year. */
    public fun dateYearDay(prolepticYear: Int, dayOfYear: Int): ChronoLocalDate

    /** Obtains a date from the shared epoch-day count. */
    public fun dateEpochDay(epochDay: Long): ChronoLocalDate

    /** Converts [temporal] to a date in this chronology. */
    public fun date(temporal: TemporalAccessor): ChronoLocalDate

    /** Obtains the current date using the system clock in [zone]. */
    public fun dateNow(zone: ZoneId): ChronoLocalDate = dateNow(Clock.system(zone))

    /** Obtains the current date from [clock]. */
    public fun dateNow(clock: Clock): ChronoLocalDate = date(LocalDate.now(clock))

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
