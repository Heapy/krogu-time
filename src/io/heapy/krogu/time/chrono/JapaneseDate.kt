package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.internal.addExact
import io.heapy.krogu.time.internal.multiplyExact
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjuster
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A date in the Japanese Imperial calendar system. */
public class JapaneseDate private constructor(
    private val isoDate: LocalDate,
    override val era: JapaneseEra,
    private val yearOfEra: Int,
) : ChronoLocalDate {
    override val chronology: JapaneseChronology
        get() = JapaneseChronology

    override val isLeapYear: Boolean
        get() = isoDate.isLeapYear

    override fun lengthOfMonth(): Int = isoDate.lengthOfMonth()

    override fun lengthOfYear(): Int =
        (segmentEndEpochExclusive() - segmentStart().toEpochDay()).toInt()

    override fun isSupported(field: TemporalField?): Boolean = when (field) {
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH,
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR,
        ChronoField.ALIGNED_WEEK_OF_MONTH,
        ChronoField.ALIGNED_WEEK_OF_YEAR,
        -> false
        else -> super.isSupported(field)
    }

    override fun range(field: TemporalField): ValueRange {
        if (field !is ChronoField) return field.rangeRefinedBy(this)
        if (!isSupported(field)) throw UnsupportedTemporalTypeException("Unsupported field: $field")
        return when (field) {
            ChronoField.DAY_OF_MONTH -> ValueRange.of(1, lengthOfMonth().toLong())
            ChronoField.DAY_OF_YEAR -> ValueRange.of(1, lengthOfYear().toLong())
            ChronoField.YEAR_OF_ERA -> ValueRange.of(1, maximumYearOfEra().toLong())
            else -> chronology.range(field)
        }
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH,
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR,
        ChronoField.ALIGNED_WEEK_OF_MONTH,
        ChronoField.ALIGNED_WEEK_OF_YEAR,
        -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        ChronoField.YEAR_OF_ERA -> yearOfEra.toLong()
        ChronoField.ERA -> era.value.toLong()
        ChronoField.DAY_OF_YEAR -> isoDate.toEpochDay() - segmentStart().toEpochDay() + 1
        is ChronoField -> isoDate.getLong(field)
        else -> field.getFrom(this)
    }

    override fun with(adjuster: TemporalAdjuster): JapaneseDate = when (adjuster) {
        is JapaneseDate -> adjuster
        else -> ensureValid(adjuster.adjustInto(this))
    }

    override fun with(field: TemporalField, newValue: Long): JapaneseDate {
        if (field !is ChronoField) return ensureValid(field.adjustInto(this, newValue))
        if (getLong(field) == newValue) return this
        return when (field) {
            ChronoField.YEAR_OF_ERA -> {
                val year = chronology.range(field).checkValidIntValue(newValue, field)
                withYear(era, year)
            }
            ChronoField.YEAR -> {
                val year = chronology.range(field).checkValidIntValue(newValue, field)
                withIsoDate(isoDate.withYear(year))
            }
            ChronoField.ERA -> {
                val newEra = chronology.eraOf(chronology.range(field).checkValidIntValue(newValue, field))
                withYear(newEra, yearOfEra)
            }
            else -> withIsoDate(isoDate.with(field, newValue))
        }
    }

    override fun plus(amount: TemporalAmount): JapaneseDate = ensureValid(amount.addTo(this))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): JapaneseDate {
        if (unit !is ChronoUnit) return ensureValid(unit.addTo(this, amountToAdd))
        return when (unit) {
            ChronoUnit.DAYS -> plusDays(amountToAdd)
            ChronoUnit.WEEKS -> plusDays(multiplyExact(amountToAdd, 7))
            ChronoUnit.MONTHS -> plusMonths(amountToAdd)
            ChronoUnit.YEARS -> plusYears(amountToAdd)
            ChronoUnit.DECADES -> plusYears(multiplyExact(amountToAdd, 10))
            ChronoUnit.CENTURIES -> plusYears(multiplyExact(amountToAdd, 100))
            ChronoUnit.MILLENNIA -> plusYears(multiplyExact(amountToAdd, 1_000))
            ChronoUnit.ERAS -> with(ChronoField.ERA, addExact(getLong(ChronoField.ERA), amountToAdd))
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    public fun plusYears(yearsToAdd: Long): JapaneseDate = withIsoDate(isoDate.plusYears(yearsToAdd))

    public fun plusMonths(monthsToAdd: Long): JapaneseDate = withIsoDate(isoDate.plusMonths(monthsToAdd))

    public fun plusWeeks(weeksToAdd: Long): JapaneseDate = plusDays(multiplyExact(weeksToAdd, 7))

    public fun plusDays(daysToAdd: Long): JapaneseDate = withIsoDate(isoDate.plusDays(daysToAdd))

    override fun minus(amount: TemporalAmount): JapaneseDate = ensureValid(amount.subtractFrom(this))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): JapaneseDate =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusYears(yearsToSubtract: Long): JapaneseDate =
        if (yearsToSubtract == Long.MIN_VALUE) plusYears(Long.MAX_VALUE).plusYears(1) else plusYears(-yearsToSubtract)

    public fun minusMonths(monthsToSubtract: Long): JapaneseDate =
        if (monthsToSubtract == Long.MIN_VALUE) {
            plusMonths(Long.MAX_VALUE).plusMonths(1)
        } else {
            plusMonths(-monthsToSubtract)
        }

    public fun minusWeeks(weeksToSubtract: Long): JapaneseDate =
        if (weeksToSubtract == Long.MIN_VALUE) plusWeeks(Long.MAX_VALUE).plusWeeks(1) else plusWeeks(-weeksToSubtract)

    public fun minusDays(daysToSubtract: Long): JapaneseDate =
        if (daysToSubtract == Long.MIN_VALUE) plusDays(Long.MAX_VALUE).plusDays(1) else plusDays(-daysToSubtract)

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = chronology.date(endExclusive)
        if (unit !is ChronoUnit) return unit.between(this, end)
        return when (unit) {
            ChronoUnit.DAYS -> end.toEpochDay() - toEpochDay()
            ChronoUnit.WEEKS -> (end.toEpochDay() - toEpochDay()) / 7
            ChronoUnit.MONTHS -> monthsUntil(end)
            ChronoUnit.YEARS -> monthsUntil(end) / 12
            ChronoUnit.DECADES -> monthsUntil(end) / 120
            ChronoUnit.CENTURIES -> monthsUntil(end) / 1_200
            ChronoUnit.MILLENNIA -> monthsUntil(end) / 12_000
            ChronoUnit.ERAS -> end.getLong(ChronoField.ERA) - getLong(ChronoField.ERA)
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    override fun until(endDateExclusive: ChronoLocalDate): ChronoPeriod {
        val end = chronology.date(endDateExclusive)
        val period = isoDate.until(end.isoDate)
        return chronology.period(period.years, period.months, period.days)
    }

    override fun atTime(localTime: LocalTime): ChronoLocalDateTime<JapaneseDate> =
        ChronoLocalDateTimeImpl.of(this, localTime)

    override fun toEpochDay(): Long = isoDate.toEpochDay()

    private fun segmentStart(): LocalDate =
        if (yearOfEra == 1) era.since else LocalDate.of(isoDate.year, 1, 1)

    private fun segmentEndEpochExclusive(): Long {
        val isoYearEnd = LocalDate.of(isoDate.year, 12, 31).toEpochDay() + 1
        val nextEra = JapaneseEra.next(era)
        return if (nextEra != null && nextEra.since.year == isoDate.year) {
            minOf(isoYearEnd, nextEra.since.toEpochDay())
        } else {
            isoYearEnd
        }
    }

    private fun maximumYearOfEra(): Int {
        val nextEra = JapaneseEra.next(era)
        val endYear: Int
        val endMonth: Int
        val endDay: Int
        if (nextEra != null) {
            val endDate = nextEra.since.minusDays(1)
            endYear = endDate.year
            endMonth = endDate.monthValue
            endDay = endDate.dayOfMonth
        } else {
            endYear = LEGACY_CALENDAR_MAX_YEAR
            endMonth = LEGACY_CALENDAR_MAX_MONTH
            endDay = LEGACY_CALENDAR_MAX_DAY
        }
        var maximum = endYear - era.since.year + 1
        if (isoDate.monthValue > endMonth ||
            isoDate.monthValue == endMonth && isoDate.dayOfMonth > endDay
        ) {
            maximum--
        }
        return maximum
    }

    private fun withYear(newEra: JapaneseEra, newYearOfEra: Int): JapaneseDate =
        withIsoDate(isoDate.withYear(chronology.prolepticYear(newEra, newYearOfEra)))

    private fun monthsUntil(end: JapaneseDate): Long {
        val packedThis = isoDate.getLong(ChronoField.PROLEPTIC_MONTH) * 32 + isoDate.dayOfMonth
        val packedEnd = end.isoDate.getLong(ChronoField.PROLEPTIC_MONTH) * 32 + end.isoDate.dayOfMonth
        return (packedEnd - packedThis) / 32
    }

    private fun withIsoDate(newDate: LocalDate): JapaneseDate =
        if (isoDate == newDate) this else fromIsoDate(newDate)

    private fun ensureValid(temporal: Temporal): JapaneseDate {
        val date = temporal as? JapaneseDate
            ?: throw ClassCastException("Temporal is not a JapaneseDate: $temporal")
        return date
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is JapaneseDate && isoDate == other.isoDate

    override fun hashCode(): Int = chronology.id.hashCode() xor isoDate.hashCode()

    override fun toString(): String = buildString {
        append(chronology)
        append(' ')
        append(era)
        append(' ')
        append(yearOfEra)
        append('-')
        append(isoDate.monthValue.toString().padStart(2, '0'))
        append('-')
        append(isoDate.dayOfMonth.toString().padStart(2, '0'))
    }

    public companion object {
        internal val MEIJI_6_ISO_DATE: LocalDate = LocalDate.of(1873, 1, 1)
        private const val LEGACY_CALENDAR_MAX_YEAR: Int = 292_278_994
        private const val LEGACY_CALENDAR_MAX_MONTH: Int = 8
        private const val LEGACY_CALENDAR_MAX_DAY: Int = 17

        /** Obtains the current Japanese date in the system default time-zone. */
        @JvmStatic
        public fun now(): JapaneseDate = now(Clock.systemDefaultZone())

        /** Obtains the current Japanese date in [zone]. */
        @JvmStatic
        public fun now(zone: ZoneId): JapaneseDate = now(Clock.system(zone))

        /** Obtains the current Japanese date from [clock]. */
        @JvmStatic
        public fun now(clock: Clock): JapaneseDate = fromIsoDate(LocalDate.now(clock))

        /** Obtains a Japanese date from an era, year-of-era, month, and day. */
        @JvmStatic
        public fun of(
            era: JapaneseEra,
            yearOfEra: Int,
            month: Int,
            dayOfMonth: Int,
        ): JapaneseDate {
            val isoDate = LocalDate.of(
                JapaneseChronology.prolepticYear(era, yearOfEra),
                month,
                dayOfMonth,
            )
            val result = fromIsoDate(isoDate)
            if (result.era !== era || result.yearOfEra != yearOfEra) {
                throw DateTimeException("year, month, and day not valid for Era")
            }
            return result
        }

        /** Obtains a Japanese date from an ISO-equivalent proleptic year, month, and day. */
        @JvmStatic
        public fun of(prolepticYear: Int, month: Int, dayOfMonth: Int): JapaneseDate =
            fromIsoDate(LocalDate.of(prolepticYear, month, dayOfMonth))

        /** Obtains a Japanese date from a temporal accessor. */
        @JvmStatic
        public fun from(temporal: TemporalAccessor): JapaneseDate =
            if (temporal is JapaneseDate) temporal else fromIsoDate(LocalDate.from(temporal))

        internal fun ofYearDay(
            era: JapaneseEra,
            yearOfEra: Int,
            dayOfYear: Int,
        ): JapaneseDate {
            val start = if (yearOfEra == 1) {
                era.since
            } else {
                LocalDate.of(JapaneseChronology.prolepticYear(era, yearOfEra), 1, 1)
            }
            val result = fromIsoDate(start.plusDays(dayOfYear.toLong() - 1))
            if (dayOfYear < 1 || result.era !== era || result.yearOfEra != yearOfEra) {
                throw DateTimeException("Invalid parameters")
            }
            return result
        }

        internal fun fromIsoDate(date: LocalDate): JapaneseDate {
            if (date < MEIJI_6_ISO_DATE) {
                throw DateTimeException("JapaneseDate before Meiji 6 is not supported")
            }
            val era = JapaneseEra.from(date)
            return JapaneseDate(date, era, date.year - era.since.year + 1)
        }
    }
}
