package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorDiv
import io.heapy.grogu.time.internal.floorMod
import io.heapy.grogu.time.internal.multiplyExact
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange

/** A date in the Islamic Umm al-Qura calendar system. */
public class HijrahDate private constructor(
    private val prolepticYear: Int,
    private val monthOfYear: Int,
    private val dayOfMonth: Int,
) : ChronoLocalDate {
    override val chronology: HijrahChronology
        get() = HijrahChronology

    override val era: HijrahEra
        get() = HijrahEra.AH

    override val isLeapYear: Boolean
        get() = chronology.isLeapYear(prolepticYear.toLong())

    private val prolepticMonth: Long
        get() = prolepticYear * 12L + monthOfYear - 1

    private val dayOfYear: Int
        get() = chronology.dayOfYear(prolepticYear, monthOfYear) + dayOfMonth

    override fun lengthOfMonth(): Int = chronology.monthLength(prolepticYear, monthOfYear)

    override fun lengthOfYear(): Int = chronology.yearLength(prolepticYear)

    override fun range(field: TemporalField): ValueRange {
        if (field !is ChronoField) return field.rangeRefinedBy(this)
        if (!isSupported(field)) throw UnsupportedTemporalTypeException("Unsupported field: $field")
        return when (field) {
            ChronoField.DAY_OF_MONTH -> ValueRange.of(1, lengthOfMonth().toLong())
            ChronoField.DAY_OF_YEAR -> ValueRange.of(1, lengthOfYear().toLong())
            ChronoField.ALIGNED_WEEK_OF_MONTH -> ValueRange.of(1, 5)
            else -> chronology.range(field)
        }
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.DAY_OF_WEEK -> floorMod(toEpochDay() + 3, 7) + 1
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH -> ((dayOfMonth - 1) % 7 + 1).toLong()
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR -> ((dayOfYear - 1) % 7 + 1).toLong()
        ChronoField.DAY_OF_MONTH -> dayOfMonth.toLong()
        ChronoField.DAY_OF_YEAR -> dayOfYear.toLong()
        ChronoField.EPOCH_DAY -> toEpochDay()
        ChronoField.ALIGNED_WEEK_OF_MONTH -> ((dayOfMonth - 1) / 7 + 1).toLong()
        ChronoField.ALIGNED_WEEK_OF_YEAR -> ((dayOfYear - 1) / 7 + 1).toLong()
        ChronoField.MONTH_OF_YEAR -> monthOfYear.toLong()
        ChronoField.PROLEPTIC_MONTH -> prolepticMonth
        ChronoField.YEAR_OF_ERA,
        ChronoField.YEAR,
        -> prolepticYear.toLong()
        ChronoField.ERA -> 1
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun with(adjuster: TemporalAdjuster): HijrahDate = when (adjuster) {
        is HijrahDate -> adjuster
        else -> ensureValid(adjuster.adjustInto(this))
    }

    /** Returns this date in [chronology]; the bundled port currently has one Hijrah variant. */
    @Suppress("UNUSED_PARAMETER")
    public fun withVariant(chronology: HijrahChronology): HijrahDate = this

    override fun with(field: TemporalField, newValue: Long): HijrahDate {
        if (field !is ChronoField) return ensureValid(field.adjustInto(this, newValue))
        chronology.range(field).checkValidValue(newValue, field)
        val newIntValue = newValue.toInt()
        return when (field) {
            ChronoField.DAY_OF_WEEK -> plusDays(newValue - getLong(ChronoField.DAY_OF_WEEK))
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH ->
                plusDays(newValue - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH))
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR ->
                plusDays(newValue - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR))
            ChronoField.DAY_OF_MONTH -> resolvePreviousValid(prolepticYear, monthOfYear, newIntValue)
            ChronoField.DAY_OF_YEAR -> plusDays(minOf(newIntValue, lengthOfYear()) - dayOfYear.toLong())
            ChronoField.EPOCH_DAY -> fromEpochDay(newValue)
            ChronoField.ALIGNED_WEEK_OF_MONTH ->
                plusDays((newValue - getLong(ChronoField.ALIGNED_WEEK_OF_MONTH)) * 7)
            ChronoField.ALIGNED_WEEK_OF_YEAR ->
                plusDays((newValue - getLong(ChronoField.ALIGNED_WEEK_OF_YEAR)) * 7)
            ChronoField.MONTH_OF_YEAR -> resolvePreviousValid(prolepticYear, newIntValue, dayOfMonth)
            ChronoField.PROLEPTIC_MONTH -> plusMonths(newValue - prolepticMonth)
            ChronoField.YEAR_OF_ERA -> resolvePreviousValid(
                if (prolepticYear >= 1) newIntValue else 1 - newIntValue,
                monthOfYear,
                dayOfMonth,
            )
            ChronoField.YEAR -> resolvePreviousValid(newIntValue, monthOfYear, dayOfMonth)
            ChronoField.ERA -> resolvePreviousValid(1 - prolepticYear, monthOfYear, dayOfMonth)
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
    }

    override fun plus(amount: TemporalAmount): HijrahDate = ensureValid(amount.addTo(this))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): HijrahDate {
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

    public fun plusYears(yearsToAdd: Long): HijrahDate {
        if (yearsToAdd == 0L) return this
        val newYear = addExact(prolepticYear, yearsToAdd.toInt())
        return resolvePreviousValid(newYear, monthOfYear, dayOfMonth)
    }

    public fun plusMonths(monthsToAdd: Long): HijrahDate {
        if (monthsToAdd == 0L) return this
        val calculatedMonths = prolepticMonth + monthsToAdd
        val newYear = chronology.checkValidYear(floorDiv(calculatedMonths, 12))
        val newMonth = floorMod(calculatedMonths, 12).toInt() + 1
        return resolvePreviousValid(newYear, newMonth, dayOfMonth)
    }

    public fun plusWeeks(weeksToAdd: Long): HijrahDate = plusDays(multiplyExact(weeksToAdd, 7))

    public fun plusDays(daysToAdd: Long): HijrahDate = fromEpochDay(toEpochDay() + daysToAdd)

    override fun minus(amount: TemporalAmount): HijrahDate = ensureValid(amount.subtractFrom(this))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): HijrahDate =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun minusYears(yearsToSubtract: Long): HijrahDate =
        if (yearsToSubtract == Long.MIN_VALUE) {
            plusYears(Long.MAX_VALUE).plusYears(1)
        } else {
            plusYears(-yearsToSubtract)
        }

    public fun minusMonths(monthsToSubtract: Long): HijrahDate =
        if (monthsToSubtract == Long.MIN_VALUE) {
            plusMonths(Long.MAX_VALUE).plusMonths(1)
        } else {
            plusMonths(-monthsToSubtract)
        }

    public fun minusWeeks(weeksToSubtract: Long): HijrahDate =
        if (weeksToSubtract == Long.MIN_VALUE) {
            plusWeeks(Long.MAX_VALUE).plusWeeks(1)
        } else {
            plusWeeks(-weeksToSubtract)
        }

    public fun minusDays(daysToSubtract: Long): HijrahDate =
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
        var totalMonths = (end.prolepticYear - prolepticYear) * 12L + end.monthOfYear - monthOfYear
        var days = end.dayOfMonth - dayOfMonth
        if (totalMonths > 0 && days < 0) {
            totalMonths--
            val calculatedDate = plusMonths(totalMonths)
            days = (end.toEpochDay() - calculatedDate.toEpochDay()).toInt()
        } else if (totalMonths < 0 && days > 0) {
            totalMonths++
            days -= end.lengthOfMonth()
        }
        return chronology.period((totalMonths / 12).toInt(), (totalMonths % 12).toInt(), days)
    }

    override fun atTime(localTime: LocalTime): ChronoLocalDateTime<HijrahDate> =
        ChronoLocalDateTimeImpl.of(this, localTime)

    override fun toEpochDay(): Long = chronology.epochDay(prolepticYear, monthOfYear, dayOfMonth)

    private fun resolvePreviousValid(year: Int, month: Int, day: Int): HijrahDate =
        HijrahDate(year, month, minOf(day, chronology.monthLength(year, month)))

    private fun monthsUntil(end: HijrahDate): Long {
        val packedThis = prolepticMonth * 32 + dayOfMonth
        val packedEnd = end.prolepticMonth * 32 + end.dayOfMonth
        return (packedEnd - packedThis) / 32
    }

    private fun ensureValid(temporal: Temporal): HijrahDate {
        val date = temporal as? HijrahDate
            ?: throw ClassCastException("Temporal is not a HijrahDate: $temporal")
        return date
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is HijrahDate &&
            prolepticYear == other.prolepticYear &&
            monthOfYear == other.monthOfYear &&
            dayOfMonth == other.dayOfMonth

    override fun hashCode(): Int =
        chronology.id.hashCode() xor
            (prolepticYear and -0x800) xor
            ((prolepticYear shl 11) + (monthOfYear shl 6) + dayOfMonth)

    override fun toString(): String = buildString {
        append(chronology)
        append(' ')
        append(era)
        append(' ')
        append(prolepticYear)
        append('-')
        append(monthOfYear.toString().padStart(2, '0'))
        append('-')
        append(dayOfMonth.toString().padStart(2, '0'))
    }

    public companion object {
        /** Obtains the current Hijrah date in the system default time-zone. */
        public fun now(): HijrahDate = now(Clock.systemDefaultZone())

        /** Obtains the current Hijrah date in [zone]. */
        public fun now(zone: ZoneId): HijrahDate = now(Clock.system(zone))

        /** Obtains the current Hijrah date from [clock]. */
        public fun now(clock: Clock): HijrahDate = fromEpochDay(LocalDate.now(clock).toEpochDay())

        /** Obtains a Hijrah date from its proleptic year, month, and day. */
        public fun of(prolepticYear: Int, month: Int, dayOfMonth: Int): HijrahDate {
            HijrahChronology.epochDay(prolepticYear, month, dayOfMonth)
            return HijrahDate(prolepticYear, month, dayOfMonth)
        }

        /** Obtains a Hijrah date from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): HijrahDate = HijrahChronology.date(temporal)

        internal fun fromEpochDay(epochDay: Long): HijrahDate {
            val dateInfo = HijrahChronology.dateInfo(epochDay)
            return HijrahDate(dateInfo[0], dateInfo[1], dateInfo[2])
        }

        private fun addExact(first: Int, second: Int): Int {
            val result = first.toLong() + second
            if (result !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                throw ArithmeticException("integer overflow")
            }
            return result.toInt()
        }
    }
}
