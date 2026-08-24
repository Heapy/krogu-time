package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.ValueRange

/** The proleptic Minguo calendar system used in Taiwan. */
public object MinguoChronology : AbstractChronology() {
    internal const val YEARS_DIFFERENCE: Int = 1_911

    override val id: String = "Minguo"
    override val calendarType: String = "roc"
    override val isIsoBased: Boolean = true

    override fun date(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
    ): MinguoDate = date(prolepticYear(era, yearOfEra), month, dayOfMonth)

    override fun date(prolepticYear: Int, month: Int, dayOfMonth: Int): MinguoDate =
        MinguoDate.of(prolepticYear, month, dayOfMonth)

    override fun dateYearDay(
        era: Era,
        yearOfEra: Int,
        dayOfYear: Int,
    ): MinguoDate = dateYearDay(prolepticYear(era, yearOfEra), dayOfYear)

    override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): MinguoDate =
        MinguoDate.fromIsoDate(LocalDate.ofYearDay(prolepticYear + YEARS_DIFFERENCE, dayOfYear))

    override fun dateEpochDay(epochDay: Long): MinguoDate =
        MinguoDate.fromIsoDate(LocalDate.ofEpochDay(epochDay))

    override fun date(temporal: TemporalAccessor): MinguoDate = MinguoDate.from(temporal)

    override fun dateNow(): MinguoDate = dateNow(Clock.systemDefaultZone())

    override fun dateNow(zone: ZoneId): MinguoDate = dateNow(Clock.system(zone))

    override fun dateNow(clock: Clock): MinguoDate = MinguoDate.now(clock)

    @Suppress("UNCHECKED_CAST")
    override fun localDateTime(temporal: TemporalAccessor): ChronoLocalDateTime<MinguoDate> =
        super.localDateTime(temporal) as ChronoLocalDateTime<MinguoDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(temporal: TemporalAccessor): ChronoZonedDateTime<MinguoDate> =
        super.zonedDateTime(temporal) as ChronoZonedDateTime<MinguoDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(
        instant: Instant,
        zone: ZoneId,
    ): ChronoZonedDateTime<MinguoDate> =
        ChronoZonedDateTimeImpl.ofInstant(this, instant, zone) as ChronoZonedDateTime<MinguoDate>

    override fun isLeapYear(prolepticYear: Long): Boolean =
        IsoChronology.isLeapYear(prolepticYear + YEARS_DIFFERENCE)

    override fun prolepticYear(era: Era, yearOfEra: Int): Int {
        if (era !is MinguoEra) throw ClassCastException("Era must be MinguoEra")
        return if (era === MinguoEra.ROC) yearOfEra else 1 - yearOfEra
    }

    override fun eraOf(eraValue: Int): MinguoEra = MinguoEra.of(eraValue)

    override fun eras(): List<MinguoEra> = MinguoEra.entries

    override fun range(field: ChronoField): ValueRange = when (field) {
        ChronoField.PROLEPTIC_MONTH -> ValueRange.of(
            field.range.minimum - YEARS_DIFFERENCE * 12L,
            field.range.maximum - YEARS_DIFFERENCE * 12L,
        )
        ChronoField.YEAR_OF_ERA -> ValueRange.of(
            1,
            ChronoField.YEAR.range.maximum - YEARS_DIFFERENCE,
            -ChronoField.YEAR.range.minimum + 1 + YEARS_DIFFERENCE,
        )
        ChronoField.YEAR -> ValueRange.of(
            field.range.minimum - YEARS_DIFFERENCE,
            field.range.maximum - YEARS_DIFFERENCE,
        )
        else -> field.range
    }

    override fun period(years: Int, months: Int, days: Int): ChronoPeriod =
        ChronoPeriodImpl(this, years, months, days)

    override fun toString(): String = id
}
