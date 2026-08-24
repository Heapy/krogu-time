package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.ValueRange

/** The proleptic Thai Buddhist calendar system. */
public object ThaiBuddhistChronology : AbstractChronology() {
    internal const val YEARS_DIFFERENCE: Int = 543

    override val id: String = "ThaiBuddhist"
    override val calendarType: String = "buddhist"
    override val isIsoBased: Boolean = true

    override fun date(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
    ): ThaiBuddhistDate = date(prolepticYear(era, yearOfEra), month, dayOfMonth)

    override fun date(prolepticYear: Int, month: Int, dayOfMonth: Int): ThaiBuddhistDate =
        ThaiBuddhistDate.of(prolepticYear, month, dayOfMonth)

    override fun dateYearDay(
        era: Era,
        yearOfEra: Int,
        dayOfYear: Int,
    ): ThaiBuddhistDate = dateYearDay(prolepticYear(era, yearOfEra), dayOfYear)

    override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): ThaiBuddhistDate =
        ThaiBuddhistDate.fromIsoDate(LocalDate.ofYearDay(prolepticYear - YEARS_DIFFERENCE, dayOfYear))

    override fun dateEpochDay(epochDay: Long): ThaiBuddhistDate =
        ThaiBuddhistDate.fromIsoDate(LocalDate.ofEpochDay(epochDay))

    override fun date(temporal: TemporalAccessor): ThaiBuddhistDate = ThaiBuddhistDate.from(temporal)

    override fun dateNow(): ThaiBuddhistDate = dateNow(Clock.systemDefaultZone())

    override fun dateNow(zone: ZoneId): ThaiBuddhistDate = dateNow(Clock.system(zone))

    override fun dateNow(clock: Clock): ThaiBuddhistDate = ThaiBuddhistDate.now(clock)

    @Suppress("UNCHECKED_CAST")
    override fun localDateTime(temporal: TemporalAccessor): ChronoLocalDateTime<ThaiBuddhistDate> =
        super.localDateTime(temporal) as ChronoLocalDateTime<ThaiBuddhistDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(temporal: TemporalAccessor): ChronoZonedDateTime<ThaiBuddhistDate> =
        super.zonedDateTime(temporal) as ChronoZonedDateTime<ThaiBuddhistDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(
        instant: Instant,
        zone: ZoneId,
    ): ChronoZonedDateTime<ThaiBuddhistDate> =
        ChronoZonedDateTimeImpl.ofInstant(this, instant, zone) as ChronoZonedDateTime<ThaiBuddhistDate>

    override fun isLeapYear(prolepticYear: Long): Boolean =
        IsoChronology.isLeapYear(prolepticYear - YEARS_DIFFERENCE)

    override fun prolepticYear(era: Era, yearOfEra: Int): Int {
        if (era !is ThaiBuddhistEra) throw ClassCastException("Era must be BuddhistEra")
        return if (era === ThaiBuddhistEra.BE) yearOfEra else 1 - yearOfEra
    }

    override fun eraOf(eraValue: Int): ThaiBuddhistEra = ThaiBuddhistEra.of(eraValue)

    override fun eras(): List<ThaiBuddhistEra> = ThaiBuddhistEra.entries

    override fun range(field: ChronoField): ValueRange = when (field) {
        ChronoField.PROLEPTIC_MONTH -> ValueRange.of(
            field.range.minimum + YEARS_DIFFERENCE * 12L,
            field.range.maximum + YEARS_DIFFERENCE * 12L,
        )
        ChronoField.YEAR_OF_ERA -> ValueRange.of(
            1,
            -(ChronoField.YEAR.range.minimum + YEARS_DIFFERENCE) + 1,
            ChronoField.YEAR.range.maximum + YEARS_DIFFERENCE,
        )
        ChronoField.YEAR -> ValueRange.of(
            field.range.minimum + YEARS_DIFFERENCE,
            field.range.maximum + YEARS_DIFFERENCE,
        )
        else -> field.range
    }

    override fun period(years: Int, months: Int, days: Int): ChronoPeriod =
        ChronoPeriodImpl(this, years, months, days)

    override fun toString(): String = id
}
