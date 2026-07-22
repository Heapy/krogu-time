package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.Period
import io.heapy.grogu.time.Year
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.ValueRange

/** The singleton ISO-8601 chronology. */
public object IsoChronology : Chronology {
    override val id: String = "ISO"
    override val calendarType: String = "iso8601"
    override val isIsoBased: Boolean = true

    /** Obtains an ISO date from an era and year-of-era. */
    override fun date(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
    ): LocalDate = date(prolepticYear(era, yearOfEra), month, dayOfMonth)

    /** Obtains an ISO date from its proleptic year, month, and day. */
    override fun date(
        prolepticYear: Int,
        month: Int,
        dayOfMonth: Int,
    ): LocalDate = LocalDate.of(prolepticYear, month, dayOfMonth)

    /** Obtains an ISO date from an era, year-of-era, and day-of-year. */
    override fun dateYearDay(
        era: Era,
        yearOfEra: Int,
        dayOfYear: Int,
    ): LocalDate = dateYearDay(prolepticYear(era, yearOfEra), dayOfYear)

    /** Obtains an ISO date from a proleptic year and day-of-year. */
    override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): LocalDate =
        LocalDate.ofYearDay(prolepticYear, dayOfYear)

    /** Obtains an ISO date from the shared epoch-day count. */
    override fun dateEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** Converts [temporal] to an ISO date. */
    override fun date(temporal: TemporalAccessor): LocalDate = LocalDate.from(temporal)

    /** Obtains the current ISO date using the system clock in [zone]. */
    override fun dateNow(zone: ZoneId): LocalDate = dateNow(Clock.system(zone))

    /** Obtains the current ISO date from [clock]. */
    override fun dateNow(clock: Clock): LocalDate = LocalDate.now(clock)

    /** Converts [temporal] to an ISO local date-time. */
    public fun localDateTime(temporal: TemporalAccessor): LocalDateTime =
        LocalDateTime.from(temporal)

    /** Converts [temporal] to an ISO zoned date-time. */
    public fun zonedDateTime(temporal: TemporalAccessor): ZonedDateTime =
        ZonedDateTime.from(temporal)

    /** Obtains an ISO zoned date-time for [instant] in [zone]. */
    public fun zonedDateTime(instant: Instant, zone: ZoneId): ZonedDateTime =
        ZonedDateTime.ofInstant(instant, zone)

    override fun isLeapYear(prolepticYear: Long): Boolean =
        Year.isLeap(prolepticYear)

    override fun prolepticYear(era: Era, yearOfEra: Int): Int {
        if (era !is IsoEra) throw ClassCastException("Era must be IsoEra")
        return if (era === IsoEra.CE) yearOfEra else 1 - yearOfEra
    }

    override fun eraOf(eraValue: Int): IsoEra = IsoEra.of(eraValue)

    override fun eras(): List<Era> = IsoEra.entries

    override fun range(field: ChronoField): ValueRange = field.range

    /** Obtains an ISO period from independent year, month, and day components. */
    public fun period(years: Int, months: Int, days: Int): Period =
        Period.of(years, months, days)

    /** Converts ISO date-time fields at [zoneOffset] to an epoch-second count. */
    public fun epochSecond(
        prolepticYear: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int,
        zoneOffset: ZoneOffset,
    ): Long = LocalDateTime.of(
        prolepticYear,
        month,
        dayOfMonth,
        hour,
        minute,
        second,
    ).toEpochSecond(zoneOffset)

    /** Converts era-based ISO date-time fields at [zoneOffset] to epoch seconds. */
    public fun epochSecond(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int,
        zoneOffset: ZoneOffset,
    ): Long = epochSecond(
        prolepticYear(era, yearOfEra),
        month,
        dayOfMonth,
        hour,
        minute,
        second,
        zoneOffset,
    )

    override fun toString(): String = id
}
