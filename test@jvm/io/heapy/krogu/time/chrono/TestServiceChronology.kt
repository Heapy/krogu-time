package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.ValueRange

class TestServiceChronology : AbstractChronology() {
    override val id: String = ID
    override val calendarType: String = CALENDAR_TYPE

    override fun date(
        prolepticYear: Int,
        month: Int,
        dayOfMonth: Int,
    ): ChronoLocalDate = IsoChronology.date(prolepticYear, month, dayOfMonth)

    override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): ChronoLocalDate =
        IsoChronology.dateYearDay(prolepticYear, dayOfYear)

    override fun dateEpochDay(epochDay: Long): ChronoLocalDate =
        IsoChronology.dateEpochDay(epochDay)

    override fun date(temporal: TemporalAccessor): ChronoLocalDate =
        IsoChronology.date(temporal)

    override fun zonedDateTime(instant: Instant, zone: ZoneId): ChronoZonedDateTime<*> =
        IsoChronology.zonedDateTime(instant, zone)

    override fun isLeapYear(prolepticYear: Long): Boolean =
        IsoChronology.isLeapYear(prolepticYear)

    override fun prolepticYear(era: Era, yearOfEra: Int): Int =
        IsoChronology.prolepticYear(era, yearOfEra)

    override fun eraOf(eraValue: Int): Era = IsoChronology.eraOf(eraValue)

    override fun eras(): List<Era> = IsoChronology.eras()

    override fun range(field: ChronoField): ValueRange = IsoChronology.range(field)

    override fun period(years: Int, months: Int, days: Int): ChronoPeriod =
        IsoChronology.period(years, months, days)

    companion object {
        const val ID: String = "ServiceTest"
        const val CALENDAR_TYPE: String = "service-test"
    }
}
