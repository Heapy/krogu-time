package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.ValueRange

/** The Islamic Umm al-Qura calendar system bundled by Java 21. */
public object HijrahChronology : AbstractChronology() {
    override val id: String = "Hijrah-umalqura"
    override val calendarType: String = "islamic-umalqura"

    private val epochMonthStartDays: IntArray =
        IntArray(HIJRAH_UMM_AL_QURA_MONTH_BITS.length + 1).also { starts ->
            var epochDay = HIJRAH_UMM_AL_QURA_MIN_EPOCH_DAY
            HIJRAH_UMM_AL_QURA_MONTH_BITS.forEachIndexed { index, monthBit ->
                starts[index] = epochDay
                epochDay += when (monthBit) {
                    '0' -> 29
                    '1' -> 30
                    else -> error("Invalid bundled Hijrah month bit: $monthBit")
                }
            }
            starts[HIJRAH_UMM_AL_QURA_MONTH_BITS.length] = epochDay
            check(epochDay == HIJRAH_UMM_AL_QURA_MAX_EPOCH_DAY) {
                "Bundled Hijrah data has an invalid end epoch day"
            }
        }

    override fun date(
        era: Era,
        yearOfEra: Int,
        month: Int,
        dayOfMonth: Int,
    ): HijrahDate = date(prolepticYear(era, yearOfEra), month, dayOfMonth)

    override fun date(prolepticYear: Int, month: Int, dayOfMonth: Int): HijrahDate =
        HijrahDate.of(prolepticYear, month, dayOfMonth)

    override fun dateYearDay(
        era: Era,
        yearOfEra: Int,
        dayOfYear: Int,
    ): HijrahDate = dateYearDay(prolepticYear(era, yearOfEra), dayOfYear)

    override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): HijrahDate {
        val firstDay = HijrahDate.of(prolepticYear, 1, 1)
        if (dayOfYear > firstDay.lengthOfYear()) {
            throw DateTimeException("Invalid dayOfYear: $dayOfYear")
        }
        return firstDay.plusDays(dayOfYear.toLong() - 1)
    }

    override fun dateEpochDay(epochDay: Long): HijrahDate = HijrahDate.fromEpochDay(epochDay)

    override fun date(temporal: TemporalAccessor): HijrahDate =
        if (temporal is HijrahDate) temporal else dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY))

    override fun dateNow(): HijrahDate = dateNow(Clock.systemDefaultZone())

    override fun dateNow(zone: ZoneId): HijrahDate = dateNow(Clock.system(zone))

    override fun dateNow(clock: Clock): HijrahDate = HijrahDate.now(clock)

    @Suppress("UNCHECKED_CAST")
    override fun localDateTime(temporal: TemporalAccessor): ChronoLocalDateTime<HijrahDate> =
        super.localDateTime(temporal) as ChronoLocalDateTime<HijrahDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(temporal: TemporalAccessor): ChronoZonedDateTime<HijrahDate> =
        super.zonedDateTime(temporal) as ChronoZonedDateTime<HijrahDate>

    @Suppress("UNCHECKED_CAST")
    override fun zonedDateTime(
        instant: Instant,
        zone: ZoneId,
    ): ChronoZonedDateTime<HijrahDate> =
        ChronoZonedDateTimeImpl.ofInstant(this, instant, zone) as ChronoZonedDateTime<HijrahDate>

    override fun isLeapYear(prolepticYear: Long): Boolean =
        prolepticYear in HIJRAH_UMM_AL_QURA_MIN_YEAR.toLong()..HIJRAH_UMM_AL_QURA_MAX_YEAR.toLong() &&
            yearLength(prolepticYear.toInt()) > 354

    override fun prolepticYear(era: Era, yearOfEra: Int): Int {
        if (era !is HijrahEra) throw ClassCastException("Era must be HijrahEra")
        return yearOfEra
    }

    override fun eraOf(eraValue: Int): HijrahEra = HijrahEra.of(eraValue)

    override fun eras(): List<HijrahEra> = HijrahEra.entries

    override fun range(field: ChronoField): ValueRange = when (field) {
        ChronoField.DAY_OF_MONTH -> ValueRange.of(
            1,
            1,
            HIJRAH_UMM_AL_QURA_MIN_MONTH_LENGTH.toLong(),
            HIJRAH_UMM_AL_QURA_MAX_MONTH_LENGTH.toLong(),
        )
        ChronoField.DAY_OF_YEAR -> ValueRange.of(1, HIJRAH_UMM_AL_QURA_MAX_YEAR_LENGTH.toLong())
        ChronoField.ALIGNED_WEEK_OF_MONTH -> ValueRange.of(1, 5)
        ChronoField.YEAR,
        ChronoField.YEAR_OF_ERA,
        -> ValueRange.of(
            HIJRAH_UMM_AL_QURA_MIN_YEAR.toLong(),
            HIJRAH_UMM_AL_QURA_MAX_YEAR.toLong(),
        )
        ChronoField.ERA -> ValueRange.of(1, 1)
        else -> field.range
    }

    override fun period(years: Int, months: Int, days: Int): ChronoPeriod =
        ChronoPeriodImpl(this, years, months, days)

    internal fun checkValidYear(prolepticYear: Long): Int {
        if (prolepticYear !in HIJRAH_UMM_AL_QURA_MIN_YEAR.toLong()..HIJRAH_UMM_AL_QURA_MAX_YEAR.toLong()) {
            throw DateTimeException("Invalid Hijrah year: $prolepticYear")
        }
        return prolepticYear.toInt()
    }

    internal fun dateInfo(epochDay: Long): IntArray {
        // OpenJDK's HijrahDate narrows the epoch day before checking the configured table.
        val epochDayInt = epochDay.toInt()
        if (epochDayInt !in HIJRAH_UMM_AL_QURA_MIN_EPOCH_DAY..<HIJRAH_UMM_AL_QURA_MAX_EPOCH_DAY) {
            throw DateTimeException("Hijrah date out of range")
        }
        val epochMonth = epochMonth(epochDayInt)
        return intArrayOf(
            HIJRAH_UMM_AL_QURA_MIN_YEAR + epochMonth / 12,
            epochMonth % 12 + 1,
            epochDayInt - epochMonthStartDays[epochMonth] + 1,
        )
    }

    internal fun epochDay(prolepticYear: Int, monthOfYear: Int, dayOfMonth: Int): Long {
        checkValidMonth(monthOfYear)
        checkValidYear(prolepticYear.toLong())
        val epochMonth = (prolepticYear - HIJRAH_UMM_AL_QURA_MIN_YEAR) * 12 + monthOfYear - 1
        val monthLength = monthLengthAt(epochMonth)
        if (dayOfMonth !in 1..monthLength) {
            throw DateTimeException("Invalid Hijrah day of month: $dayOfMonth")
        }
        return (epochMonthStartDays[epochMonth] + dayOfMonth - 1).toLong()
    }

    internal fun dayOfYear(prolepticYear: Int, monthOfYear: Int): Int {
        checkValidMonth(monthOfYear)
        val firstMonth = firstEpochMonth(prolepticYear)
        return epochMonthStartDays[firstMonth + monthOfYear - 1] - epochMonthStartDays[firstMonth]
    }

    internal fun monthLength(prolepticYear: Int, monthOfYear: Int): Int {
        checkValidMonth(monthOfYear)
        return monthLengthAt(firstEpochMonth(prolepticYear) + monthOfYear - 1)
    }

    internal fun yearLength(prolepticYear: Int): Int {
        val firstMonth = firstEpochMonth(prolepticYear)
        return epochMonthStartDays[firstMonth + 12] - epochMonthStartDays[firstMonth]
    }

    private fun checkValidMonth(monthOfYear: Int) {
        if (monthOfYear !in 1..12) throw DateTimeException("Invalid Hijrah month: $monthOfYear")
    }

    private fun firstEpochMonth(prolepticYear: Int): Int {
        checkValidYear(prolepticYear.toLong())
        return (prolepticYear - HIJRAH_UMM_AL_QURA_MIN_YEAR) * 12
    }

    private fun monthLengthAt(epochMonth: Int): Int =
        epochMonthStartDays[epochMonth + 1] - epochMonthStartDays[epochMonth]

    private fun epochMonth(epochDay: Int): Int {
        var low = 0
        var high = epochMonthStartDays.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            val comparison = epochMonthStartDays[middle].compareTo(epochDay)
            when {
                comparison < 0 -> low = middle + 1
                comparison > 0 -> high = middle - 1
                else -> return middle
            }
        }
        return low - 1
    }

    override fun toString(): String = id
}
