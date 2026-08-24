package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.internal.subtractExact
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjusters
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange

/** The Japanese Imperial calendar system supported from Meiji 6 onward. */
public object JapaneseChronology : AbstractChronology() {
    override val id: String = "Japanese"
    override val calendarType: String = "japanese"
    override val isIsoBased: Boolean = true

    override fun date(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
    ): JapaneseDate {
        if (era !is JapaneseEra) throw ClassCastException("Era must be JapaneseEra")
        return JapaneseDate.of(era, yearOfEra, month, dayOfMonth)
    }

    override fun date(prolepticYear: Int, month: Int, dayOfMonth: Int): JapaneseDate =
        JapaneseDate.of(prolepticYear, month, dayOfMonth)

    override fun dateYearDay(
        era: Era,
        yearOfEra: Int,
        dayOfYear: Int,
    ): JapaneseDate {
        if (era !is JapaneseEra) throw ClassCastException("Era must be JapaneseEra")
        return JapaneseDate.ofYearDay(era, yearOfEra, dayOfYear)
    }

    override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): JapaneseDate =
        JapaneseDate.fromIsoDate(LocalDate.ofYearDay(prolepticYear, dayOfYear))

    override fun dateEpochDay(epochDay: Long): JapaneseDate =
        JapaneseDate.fromIsoDate(LocalDate.ofEpochDay(epochDay))

    override fun date(temporal: TemporalAccessor): JapaneseDate = JapaneseDate.from(temporal)

    override fun dateNow(): JapaneseDate = dateNow(Clock.systemDefaultZone())

    override fun dateNow(zone: ZoneId): JapaneseDate = dateNow(Clock.system(zone))

    override fun dateNow(clock: Clock): JapaneseDate = JapaneseDate.now(clock)

    @Suppress("UNCHECKED_CAST")
    override fun localDateTime(temporal: TemporalAccessor): ChronoLocalDateTime<JapaneseDate> =
        super.localDateTime(temporal) as ChronoLocalDateTime<JapaneseDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(temporal: TemporalAccessor): ChronoZonedDateTime<JapaneseDate> =
        super.zonedDateTime(temporal) as ChronoZonedDateTime<JapaneseDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(
        instant: Instant,
        zone: ZoneId,
    ): ChronoZonedDateTime<JapaneseDate> =
        ChronoZonedDateTimeImpl.ofInstant(this, instant, zone) as ChronoZonedDateTime<JapaneseDate>

    override fun isLeapYear(prolepticYear: Long): Boolean =
        IsoChronology.isLeapYear(prolepticYear)

    override fun prolepticYear(era: Era, yearOfEra: Int): Int {
        if (era !is JapaneseEra) throw ClassCastException("Era must be JapaneseEra")
        val gregorianYear = era.since.year.toLong() + yearOfEra - 1
        if (yearOfEra == 1) return gregorianYear.toInt()
        if (yearOfEra <= 0 || gregorianYear !in ISO_MIN_YEAR.toLong()..MAX_YEAR.toLong()) {
            throw DateTimeException("Invalid yearOfEra value")
        }
        val nextEra = JapaneseEra.next(era)
        if (nextEra != null && LocalDate.of(gregorianYear.toInt(), 1, 1) >= nextEra.since) {
            throw DateTimeException("Invalid yearOfEra value")
        }
        return gregorianYear.toInt()
    }

    override fun eraOf(eraValue: Int): JapaneseEra = JapaneseEra.of(eraValue)

    override fun eras(): List<JapaneseEra> = JapaneseEra.values().toList()

    override fun range(field: ChronoField): ValueRange = when (field) {
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH,
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR,
        ChronoField.ALIGNED_WEEK_OF_MONTH,
        ChronoField.ALIGNED_WEEK_OF_YEAR,
        -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        ChronoField.YEAR_OF_ERA -> ValueRange.of(1, 15, 999_997_980)
        ChronoField.DAY_OF_YEAR -> ValueRange.of(1, 7, 366)
        ChronoField.YEAR -> ValueRange.of(MIN_YEAR.toLong(), MAX_YEAR.toLong())
        ChronoField.ERA -> ValueRange.of(JapaneseEra.MEIJI.value.toLong(), JapaneseEra.REIWA.value.toLong())
        else -> field.range
    }

    override fun resolveDate(
        fieldValues: MutableMap<TemporalField, Long>,
        resolverStyle: ResolverStyle,
    ): JapaneseDate? = resolveChronologyDate(
        chronology = this,
        fieldValues = fieldValues,
        resolverStyle = resolverStyle,
        yearOfEraResolver = ::resolveJapaneseYearOfEra,
    ) as JapaneseDate?

    override fun period(years: Int, months: Int, days: Int): ChronoPeriod =
        ChronoPeriodImpl(this, years, months, days)

    override fun toString(): String = id

    private fun resolveJapaneseYearOfEra(
        fieldValues: MutableMap<TemporalField, Long>,
        resolverStyle: ResolverStyle,
    ): ChronoLocalDate? {
        var era = fieldValues[ChronoField.ERA]?.let { eraValue ->
            eraOf(range(ChronoField.ERA).checkValidIntValue(eraValue, ChronoField.ERA))
        }
        val yearOfEraValue = fieldValues[ChronoField.YEAR_OF_ERA]
        val yearOfEra = yearOfEraValue?.let { value ->
            range(ChronoField.YEAR_OF_ERA).checkValidIntValue(value, ChronoField.YEAR_OF_ERA)
        } ?: 0
        if (
            era == null &&
            yearOfEraValue != null &&
            ChronoField.YEAR !in fieldValues &&
            resolverStyle != ResolverStyle.STRICT
        ) {
            era = JapaneseEra.values().last()
        }
        if (yearOfEraValue != null && era != null) {
            if (
                ChronoField.MONTH_OF_YEAR in fieldValues &&
                ChronoField.DAY_OF_MONTH in fieldValues
            ) {
                return resolveJapaneseYmd(era, yearOfEra, fieldValues, resolverStyle)
            }
            if (ChronoField.DAY_OF_YEAR in fieldValues) {
                return resolveJapaneseYearDay(era, yearOfEra, fieldValues, resolverStyle)
            }
        }
        return null
    }

    private fun resolveJapaneseYmd(
        era: JapaneseEra,
        yearOfEra: Int,
        fieldValues: MutableMap<TemporalField, Long>,
        resolverStyle: ResolverStyle,
    ): ChronoLocalDate {
        fieldValues.remove(ChronoField.ERA)
        fieldValues.remove(ChronoField.YEAR_OF_ERA)
        if (resolverStyle == ResolverStyle.LENIENT) {
            val months = subtractExact(requireNotNull(fieldValues.remove(ChronoField.MONTH_OF_YEAR)), 1)
            val days = subtractExact(requireNotNull(fieldValues.remove(ChronoField.DAY_OF_MONTH)), 1)
            return date(prolepticYearLenient(era, yearOfEra), 1, 1)
                .plus(months, ChronoUnit.MONTHS)
                .plus(days, ChronoUnit.DAYS)
        }
        val month = range(ChronoField.MONTH_OF_YEAR).checkValidIntValue(
            requireNotNull(fieldValues.remove(ChronoField.MONTH_OF_YEAR)),
            ChronoField.MONTH_OF_YEAR,
        )
        val day = range(ChronoField.DAY_OF_MONTH).checkValidIntValue(
            requireNotNull(fieldValues.remove(ChronoField.DAY_OF_MONTH)),
            ChronoField.DAY_OF_MONTH,
        )
        if (resolverStyle == ResolverStyle.SMART) {
            if (yearOfEra < 1) throw DateTimeException("Invalid YearOfEra: $yearOfEra")
            val result = try {
                date(prolepticYearLenient(era, yearOfEra), month, day)
            } catch (_: DateTimeException) {
                date(prolepticYearLenient(era, yearOfEra), month, 1)
                    .with(TemporalAdjusters.lastDayOfMonth())
            }
            if (result.era != era && result.get(ChronoField.YEAR_OF_ERA) > 1 && yearOfEra > 1) {
                throw DateTimeException("Invalid YearOfEra for Era: $era $yearOfEra")
            }
            return result
        }
        return date(era, yearOfEra, month, day)
    }

    private fun resolveJapaneseYearDay(
        era: JapaneseEra,
        yearOfEra: Int,
        fieldValues: MutableMap<TemporalField, Long>,
        resolverStyle: ResolverStyle,
    ): ChronoLocalDate {
        fieldValues.remove(ChronoField.ERA)
        fieldValues.remove(ChronoField.YEAR_OF_ERA)
        if (resolverStyle == ResolverStyle.LENIENT) {
            val days = subtractExact(requireNotNull(fieldValues.remove(ChronoField.DAY_OF_YEAR)), 1)
            return dateYearDay(prolepticYearLenient(era, yearOfEra), 1)
                .plus(days, ChronoUnit.DAYS)
        }
        val dayOfYear = range(ChronoField.DAY_OF_YEAR).checkValidIntValue(
            requireNotNull(fieldValues.remove(ChronoField.DAY_OF_YEAR)),
            ChronoField.DAY_OF_YEAR,
        )
        return dateYearDay(era, yearOfEra, dayOfYear)
    }

    private fun prolepticYearLenient(era: JapaneseEra, yearOfEra: Int): Int =
        era.since.year + yearOfEra - 1

    private const val MIN_YEAR: Int = 1_873
    private const val ISO_MIN_YEAR: Int = -999_999_999
    private const val MAX_YEAR: Int = 999_999_999
}
