package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
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

/** A date in the proleptic Thai Buddhist calendar system. */
public class ThaiBuddhistDate private constructor(
    private val isoDate: LocalDate,
) : ChronoLocalDate {
    override val chronology: ThaiBuddhistChronology
        get() = ThaiBuddhistChronology

    override val era: ThaiBuddhistEra
        get() = if (prolepticYear >= 1) ThaiBuddhistEra.BE else ThaiBuddhistEra.BEFORE_BE

    override val isLeapYear: Boolean
        get() = isoDate.isLeapYear

    private val prolepticYear: Int
        get() = isoDate.year + ThaiBuddhistChronology.YEARS_DIFFERENCE

    private val prolepticMonth: Long
        get() = prolepticYear * 12L + isoDate.monthValue - 1

    override fun lengthOfMonth(): Int = isoDate.lengthOfMonth()

    override fun lengthOfYear(): Int = isoDate.lengthOfYear()

    override fun range(field: TemporalField): ValueRange {
        if (field !is ChronoField) return field.rangeRefinedBy(this)
        if (!isSupported(field)) throw UnsupportedTemporalTypeException("Unsupported field: $field")
        return when (field) {
            ChronoField.DAY_OF_MONTH,
            ChronoField.DAY_OF_YEAR,
            ChronoField.ALIGNED_WEEK_OF_MONTH,
            -> isoDate.range(field)
            ChronoField.YEAR_OF_ERA -> ValueRange.of(
                1,
                if (prolepticYear <= 0) {
                    -(ChronoField.YEAR.range.minimum + ThaiBuddhistChronology.YEARS_DIFFERENCE) + 1
                } else {
                    ChronoField.YEAR.range.maximum + ThaiBuddhistChronology.YEARS_DIFFERENCE
                },
            )
            else -> chronology.range(field)
        }
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.PROLEPTIC_MONTH -> prolepticMonth
        ChronoField.YEAR_OF_ERA -> if (prolepticYear >= 1) {
            prolepticYear.toLong()
        } else {
            1L - prolepticYear
        }
        ChronoField.YEAR -> prolepticYear.toLong()
        ChronoField.ERA -> if (prolepticYear >= 1) 1 else 0
        is ChronoField -> isoDate.getLong(field)
        else -> field.getFrom(this)
    }

    override fun with(adjuster: TemporalAdjuster): ThaiBuddhistDate = when (adjuster) {
        is ThaiBuddhistDate -> adjuster
        else -> ensureValid(adjuster.adjustInto(this))
    }

    override fun with(field: TemporalField, newValue: Long): ThaiBuddhistDate {
        if (field !is ChronoField) return ensureValid(field.adjustInto(this, newValue))
        if (getLong(field) == newValue) return this
        return when (field) {
            ChronoField.PROLEPTIC_MONTH -> {
                chronology.range(field).checkValidValue(newValue, field)
                plusMonths(newValue - prolepticMonth)
            }
            ChronoField.YEAR_OF_ERA -> {
                val year = chronology.range(field).checkValidIntValue(newValue, field)
                withIsoDate(
                    isoDate.withYear(
                        if (prolepticYear >= 1) {
                            year - ThaiBuddhistChronology.YEARS_DIFFERENCE
                        } else {
                            1 - year - ThaiBuddhistChronology.YEARS_DIFFERENCE
                        },
                    ),
                )
            }
            ChronoField.YEAR -> {
                val year = chronology.range(field).checkValidIntValue(newValue, field)
                withIsoDate(isoDate.withYear(year - ThaiBuddhistChronology.YEARS_DIFFERENCE))
            }
            ChronoField.ERA -> {
                chronology.range(field).checkValidIntValue(newValue, field)
                withIsoDate(
                    isoDate.withYear(1 - prolepticYear - ThaiBuddhistChronology.YEARS_DIFFERENCE),
                )
            }
            else -> withIsoDate(isoDate.with(field, newValue))
        }
    }

    override fun plus(amount: TemporalAmount): ThaiBuddhistDate = ensureValid(amount.addTo(this))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ThaiBuddhistDate {
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

    public fun plusYears(yearsToAdd: Long): ThaiBuddhistDate = withIsoDate(isoDate.plusYears(yearsToAdd))

    public fun plusMonths(monthsToAdd: Long): ThaiBuddhistDate = withIsoDate(isoDate.plusMonths(monthsToAdd))

    public fun plusWeeks(weeksToAdd: Long): ThaiBuddhistDate = plusDays(multiplyExact(weeksToAdd, 7))

    public fun plusDays(daysToAdd: Long): ThaiBuddhistDate = withIsoDate(isoDate.plusDays(daysToAdd))

    override fun minus(amount: TemporalAmount): ThaiBuddhistDate = ensureValid(amount.subtractFrom(this))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): ThaiBuddhistDate =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusYears(yearsToSubtract: Long): ThaiBuddhistDate =
        if (yearsToSubtract == Long.MIN_VALUE) {
            plusYears(Long.MAX_VALUE).plusYears(1)
        } else {
            plusYears(-yearsToSubtract)
        }

    public fun minusMonths(monthsToSubtract: Long): ThaiBuddhistDate =
        if (monthsToSubtract == Long.MIN_VALUE) {
            plusMonths(Long.MAX_VALUE).plusMonths(1)
        } else {
            plusMonths(-monthsToSubtract)
        }

    public fun minusWeeks(weeksToSubtract: Long): ThaiBuddhistDate =
        if (weeksToSubtract == Long.MIN_VALUE) {
            plusWeeks(Long.MAX_VALUE).plusWeeks(1)
        } else {
            plusWeeks(-weeksToSubtract)
        }

    public fun minusDays(daysToSubtract: Long): ThaiBuddhistDate =
        if (daysToSubtract == Long.MIN_VALUE) {
            plusDays(Long.MAX_VALUE).plusDays(1)
        } else {
            plusDays(-daysToSubtract)
        }

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

    override fun atTime(localTime: LocalTime): ChronoLocalDateTime<ThaiBuddhistDate> =
        ChronoLocalDateTimeImpl.of(this, localTime)

    override fun toEpochDay(): Long = isoDate.toEpochDay()

    private fun monthsUntil(end: ThaiBuddhistDate): Long {
        val packedThis = prolepticMonth * 32 + isoDate.dayOfMonth
        val packedEnd = end.prolepticMonth * 32 + end.isoDate.dayOfMonth
        return (packedEnd - packedThis) / 32
    }

    private fun withIsoDate(newDate: LocalDate): ThaiBuddhistDate =
        if (isoDate == newDate) this else ThaiBuddhistDate(newDate)

    private fun ensureValid(temporal: Temporal): ThaiBuddhistDate {
        val date = temporal as? ThaiBuddhistDate
            ?: throw ClassCastException("Temporal is not a ThaiBuddhistDate: $temporal")
        return date
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is ThaiBuddhistDate && isoDate == other.isoDate

    override fun hashCode(): Int = chronology.id.hashCode() xor isoDate.hashCode()

    override fun toString(): String = buildString {
        append(chronology)
        append(' ')
        append(era)
        append(' ')
        append(getLong(ChronoField.YEAR_OF_ERA))
        append('-')
        append(isoDate.monthValue.toString().padStart(2, '0'))
        append('-')
        append(isoDate.dayOfMonth.toString().padStart(2, '0'))
    }

    public companion object {
        /** Obtains the current Thai Buddhist date in the system default time-zone. */
        public fun now(): ThaiBuddhistDate = now(Clock.systemDefaultZone())

        /** Obtains the current Thai Buddhist date in [zone]. */
        public fun now(zone: ZoneId): ThaiBuddhistDate = now(Clock.system(zone))

        /** Obtains the current Thai Buddhist date from [clock]. */
        public fun now(clock: Clock): ThaiBuddhistDate = fromIsoDate(LocalDate.now(clock))

        /** Obtains a Thai Buddhist date from its proleptic year, month, and day. */
        public fun of(prolepticYear: Int, month: Int, dayOfMonth: Int): ThaiBuddhistDate =
            fromIsoDate(
                LocalDate.of(
                    prolepticYear - ThaiBuddhistChronology.YEARS_DIFFERENCE,
                    month,
                    dayOfMonth,
                ),
            )

        /** Obtains a Thai Buddhist date from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): ThaiBuddhistDate =
            if (temporal is ThaiBuddhistDate) temporal else fromIsoDate(LocalDate.from(temporal))

        internal fun fromIsoDate(date: LocalDate): ThaiBuddhistDate = ThaiBuddhistDate(date)
    }
}
