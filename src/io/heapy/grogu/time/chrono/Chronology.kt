package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
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

    /** Converts [temporal] to a local date-time in this chronology. */
    public fun localDateTime(temporal: TemporalAccessor): ChronoLocalDateTime<*> = try {
        date(temporal).atTime(LocalTime.from(temporal))
    } catch (exception: DateTimeException) {
        throw DateTimeException(
            "Unable to obtain ChronoLocalDateTime from TemporalAccessor: $temporal",
            exception,
        )
    }

    /** Converts [temporal] to a zoned date-time in this chronology. */
    public fun zonedDateTime(temporal: TemporalAccessor): ChronoZonedDateTime<*> = try {
        val zone = ZoneId.from(temporal)
        try {
            zonedDateTime(Instant.from(temporal), zone)
        } catch (_: DateTimeException) {
            localDateTime(temporal).atZone(zone)
        }
    } catch (exception: DateTimeException) {
        throw DateTimeException(
            "Unable to obtain ChronoZonedDateTime from TemporalAccessor: $temporal",
            exception,
        )
    }

    /** Obtains a zoned date-time for [instant] in [zone]. */
    public fun zonedDateTime(instant: Instant, zone: ZoneId): ChronoZonedDateTime<*>

    /** Obtains the current date using the system clock in the default time-zone. */
    public fun dateNow(): ChronoLocalDate = dateNow(Clock.systemDefaultZone())

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

    /** Obtains a period defined by this chronology. */
    public fun period(years: Int, months: Int, days: Int): ChronoPeriod

    override fun compareTo(other: Chronology): Int = id.compareTo(other.id)

    public companion object {
        /** Obtains a chronology from [temporal], defaulting to ISO when none is queried. */
        public fun from(temporal: TemporalAccessor): Chronology =
            temporal.query(TemporalQueries.chronology()) ?: IsoChronology

        /** Obtains an available chronology by its ID or calendar type. */
        public fun of(id: String): Chronology = when (id) {
            IsoChronology.id, IsoChronology.calendarType -> IsoChronology
            JapaneseChronology.id, JapaneseChronology.calendarType -> JapaneseChronology
            MinguoChronology.id, MinguoChronology.calendarType -> MinguoChronology
            ThaiBuddhistChronology.id, ThaiBuddhistChronology.calendarType -> ThaiBuddhistChronology
            else -> throw DateTimeException("Unknown chronology: $id")
        }

        /** Returns the chronologies currently available to this library. */
        public fun getAvailableChronologies(): Set<Chronology> =
            setOf(IsoChronology, JapaneseChronology, MinguoChronology, ThaiBuddhistChronology)
    }
}
