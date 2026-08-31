package io.heapy.krogu.time

import io.heapy.krogu.time.chrono.IsoEra
import io.heapy.krogu.time.chrono.IsoChronology
import io.heapy.krogu.time.chrono.ChronoLocalDate
import io.heapy.krogu.time.format.DateTimeFormatter
import io.heapy.krogu.time.format.DateTimeParseException
import io.heapy.krogu.time.internal.addExact
import io.heapy.krogu.time.internal.floorDiv
import io.heapy.krogu.time.internal.floorMod
import io.heapy.krogu.time.internal.multiplyExact
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjuster
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.temporal.TemporalQuery
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A date without a time-zone in the ISO-8601 calendar system. */
public class LocalDate private constructor(
    public val year: Int,
    public val monthValue: Int,
    public val dayOfMonth: Int,
) : ChronoLocalDate {
    override val chronology: IsoChronology
        get() = IsoChronology

    /** The month of this date. */
    public val month: Month
        get() = Month.of(monthValue)

    /** The one-based day within this date's year. */
    public val dayOfYear: Int
        get() = month.firstDayOfYear(isLeapYear) + dayOfMonth - 1

    /** The ISO day of week. */
    public val dayOfWeek: DayOfWeek
        get() = DayOfWeek.of(floorMod(toEpochDay() + 3, 7).toInt() + 1)

    /** The ISO era of this date. */
    override val era: IsoEra
        get() = if (year >= 1) IsoEra.CE else IsoEra.BCE

    /** Whether this date's year is a leap year. */
    override val isLeapYear: Boolean
        get() = Year.isLeap(year.toLong())

    private val prolepticMonth: Long
        get() = year * 12L + monthValue - 1

    /** Returns the number of days in this date's month. */
    override fun lengthOfMonth(): Int = month.length(isLeapYear)

    /** Returns the number of days in this date's year. */
    override fun lengthOfYear(): Int = if (isLeapYear) 366 else 365

    override fun isSupported(field: TemporalField?): Boolean =
        if (field is ChronoField) field.isDateBased else field != null && field.isSupportedBy(this)

    override fun isSupported(unit: TemporalUnit?): Boolean =
        if (unit is ChronoUnit) unit.isDateBased else unit != null && unit.isSupportedBy(this)

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.DAY_OF_MONTH -> ValueRange.of(1, lengthOfMonth().toLong())
        ChronoField.DAY_OF_YEAR -> ValueRange.of(1, lengthOfYear().toLong())
        ChronoField.ALIGNED_WEEK_OF_MONTH -> ValueRange.of(
            1,
            if (month === Month.FEBRUARY && !isLeapYear) 4 else 5,
        )
        ChronoField.YEAR_OF_ERA -> if (year <= 0) {
            ValueRange.of(1, Year.MAX_VALUE.toLong() + 1)
        } else {
            ValueRange.of(1, Year.MAX_VALUE.toLong())
        }
        else -> super<ChronoLocalDate>.range(field)
    }

    override fun get(field: TemporalField): Int = when (field) {
        ChronoField.DAY_OF_WEEK -> dayOfWeek.value
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH -> (dayOfMonth - 1) % 7 + 1
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR -> (dayOfYear - 1) % 7 + 1
        ChronoField.DAY_OF_MONTH -> dayOfMonth
        ChronoField.DAY_OF_YEAR -> dayOfYear
        ChronoField.EPOCH_DAY -> throw UnsupportedTemporalTypeException(
            "Invalid field 'EpochDay' for get() method, use getLong() instead",
        )
        ChronoField.ALIGNED_WEEK_OF_MONTH -> (dayOfMonth - 1) / 7 + 1
        ChronoField.ALIGNED_WEEK_OF_YEAR -> (dayOfYear - 1) / 7 + 1
        ChronoField.MONTH_OF_YEAR -> monthValue
        ChronoField.PROLEPTIC_MONTH -> throw UnsupportedTemporalTypeException(
            "Invalid field 'ProlepticMonth' for get() method, use getLong() instead",
        )
        ChronoField.YEAR_OF_ERA -> if (year >= 1) year else 1 - year
        ChronoField.YEAR -> year
        ChronoField.ERA -> if (year >= 1) 1 else 0
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> super<ChronoLocalDate>.get(field)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.DAY_OF_WEEK -> dayOfWeek.value.toLong()
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH -> ((dayOfMonth - 1) % 7 + 1).toLong()
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR -> ((dayOfYear - 1) % 7 + 1).toLong()
        ChronoField.DAY_OF_MONTH -> dayOfMonth.toLong()
        ChronoField.DAY_OF_YEAR -> dayOfYear.toLong()
        ChronoField.EPOCH_DAY -> toEpochDay()
        ChronoField.ALIGNED_WEEK_OF_MONTH -> ((dayOfMonth - 1) / 7 + 1).toLong()
        ChronoField.ALIGNED_WEEK_OF_YEAR -> ((dayOfYear - 1) / 7 + 1).toLong()
        ChronoField.MONTH_OF_YEAR -> monthValue.toLong()
        ChronoField.PROLEPTIC_MONTH -> prolepticMonth
        ChronoField.YEAR_OF_ERA -> (if (year >= 1) year else 1 - year).toLong()
        ChronoField.YEAR -> year.toLong()
        ChronoField.ERA -> if (year >= 1) 1 else 0
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.chronology()) {
            @Suppress("UNCHECKED_CAST")
            return IsoChronology as R
        }
        if (query === TemporalQueries.localDate()) {
            @Suppress("UNCHECKED_CAST")
            return this as R
        }
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.DAYS as R
        }
        return super<ChronoLocalDate>.query(query)
    }

    /** Converts this date to the count of days from 1970-01-01. */
    override fun toEpochDay(): Long {
        val prolepticYear = year.toLong()
        val month = monthValue.toLong()
        var total = 365L * prolepticYear
        total += if (prolepticYear >= 0) {
            (prolepticYear + 3) / 4 -
                (prolepticYear + 99) / 100 +
                (prolepticYear + 399) / 400
        } else {
            -(prolepticYear / -4 - prolepticYear / -100 + prolepticYear / -400)
        }
        total += (367 * month - 362) / 12
        total += dayOfMonth - 1
        if (month > 2) total -= if (isLeapYear) 1 else 2
        return total - DAYS_0000_TO_1970
    }

    override fun with(adjuster: TemporalAdjuster): LocalDate =
        adjuster.adjustInto(this) as LocalDate

    override fun with(field: TemporalField, newValue: Long): LocalDate {
        if (field !is ChronoField) return field.adjustInto(this, newValue)
        field.checkValidValue(newValue)
        return when (field) {
            ChronoField.DAY_OF_WEEK -> plusDays(newValue - dayOfWeek.value)
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH -> plusDays(
                newValue - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH),
            )
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR -> plusDays(
                newValue - getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR),
            )
            ChronoField.DAY_OF_MONTH -> withDayOfMonth(newValue.toInt())
            ChronoField.DAY_OF_YEAR -> withDayOfYear(newValue.toInt())
            ChronoField.EPOCH_DAY -> ofEpochDay(newValue)
            ChronoField.ALIGNED_WEEK_OF_MONTH -> plusWeeks(
                newValue - getLong(ChronoField.ALIGNED_WEEK_OF_MONTH),
            )
            ChronoField.ALIGNED_WEEK_OF_YEAR -> plusWeeks(
                newValue - getLong(ChronoField.ALIGNED_WEEK_OF_YEAR),
            )
            ChronoField.MONTH_OF_YEAR -> withMonth(newValue.toInt())
            ChronoField.PROLEPTIC_MONTH -> plusMonths(newValue - prolepticMonth)
            ChronoField.YEAR_OF_ERA -> withYear(
                if (year >= 1) newValue.toInt() else 1 - newValue.toInt(),
            )
            ChronoField.YEAR -> withYear(newValue.toInt())
            ChronoField.ERA -> if (getLong(ChronoField.ERA) == newValue) this else withYear(1 - year)
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
    }

    /** Returns this date with the year changed, resolving an invalid day to month-end. */
    public fun withYear(year: Int): LocalDate {
        if (this.year == year) return this
        ChronoField.YEAR.checkValidValue(year.toLong())
        return resolvePreviousValid(year, monthValue, dayOfMonth)
    }

    /** Returns this date with the month changed, resolving an invalid day to month-end. */
    public fun withMonth(month: Int): LocalDate {
        if (monthValue == month) return this
        ChronoField.MONTH_OF_YEAR.checkValidValue(month.toLong())
        return resolvePreviousValid(year, month, dayOfMonth)
    }

    /** Returns this date with the day-of-month changed. */
    public fun withDayOfMonth(dayOfMonth: Int): LocalDate =
        if (this.dayOfMonth == dayOfMonth) this else of(year, monthValue, dayOfMonth)

    /** Returns this date with the day-of-year changed. */
    public fun withDayOfYear(dayOfYear: Int): LocalDate =
        if (this.dayOfYear == dayOfYear) this else ofYearDay(year, dayOfYear)

    override fun plus(amount: TemporalAmount): LocalDate = amount.addTo(this) as LocalDate

    override fun plus(amountToAdd: Long, unit: TemporalUnit): LocalDate {
        if (unit !is ChronoUnit) return unit.addTo(this, amountToAdd)
        return when (unit) {
            ChronoUnit.DAYS -> plusDays(amountToAdd)
            ChronoUnit.WEEKS -> plusWeeks(amountToAdd)
            ChronoUnit.MONTHS -> plusMonths(amountToAdd)
            ChronoUnit.YEARS -> plusYears(amountToAdd)
            ChronoUnit.DECADES -> plusYears(multiplyExact(amountToAdd, 10))
            ChronoUnit.CENTURIES -> plusYears(multiplyExact(amountToAdd, 100))
            ChronoUnit.MILLENNIA -> plusYears(multiplyExact(amountToAdd, 1_000))
            ChronoUnit.ERAS -> with(
                ChronoField.ERA,
                addExact(getLong(ChronoField.ERA), amountToAdd),
            )
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    /** Returns this date with [yearsToAdd] added. */
    public fun plusYears(yearsToAdd: Long): LocalDate {
        if (yearsToAdd == 0L) return this
        val newYear = ChronoField.YEAR.checkValidIntValue(year.toLong() + yearsToAdd)
        return resolvePreviousValid(newYear, monthValue, dayOfMonth)
    }

    /** Returns this date with [monthsToAdd] added. */
    public fun plusMonths(monthsToAdd: Long): LocalDate {
        if (monthsToAdd == 0L) return this
        val calculatedMonths = prolepticMonth + monthsToAdd
        val newYear = ChronoField.YEAR.checkValidIntValue(floorDiv(calculatedMonths, 12))
        val newMonth = floorMod(calculatedMonths, 12).toInt() + 1
        return resolvePreviousValid(newYear, newMonth, dayOfMonth)
    }

    /** Returns this date with [weeksToAdd] added. */
    public fun plusWeeks(weeksToAdd: Long): LocalDate =
        plusDays(multiplyExact(weeksToAdd, 7))

    /** Returns this date with [daysToAdd] added. */
    public fun plusDays(daysToAdd: Long): LocalDate =
        if (daysToAdd == 0L) this else ofEpochDay(addExact(toEpochDay(), daysToAdd))

    override fun minus(amount: TemporalAmount): LocalDate = amount.subtractFrom(this) as LocalDate

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): LocalDate =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    /** Returns this date with [yearsToSubtract] subtracted. */
    public fun minusYears(yearsToSubtract: Long): LocalDate =
        if (yearsToSubtract == Long.MIN_VALUE) {
            plusYears(Long.MAX_VALUE).plusYears(1)
        } else {
            plusYears(-yearsToSubtract)
        }

    /** Returns this date with [monthsToSubtract] subtracted. */
    public fun minusMonths(monthsToSubtract: Long): LocalDate =
        if (monthsToSubtract == Long.MIN_VALUE) {
            plusMonths(Long.MAX_VALUE).plusMonths(1)
        } else {
            plusMonths(-monthsToSubtract)
        }

    /** Returns this date with [weeksToSubtract] subtracted. */
    public fun minusWeeks(weeksToSubtract: Long): LocalDate =
        if (weeksToSubtract == Long.MIN_VALUE) {
            plusWeeks(Long.MAX_VALUE).plusWeeks(1)
        } else {
            plusWeeks(-weeksToSubtract)
        }

    /** Returns this date with [daysToSubtract] subtracted. */
    public fun minusDays(daysToSubtract: Long): LocalDate =
        if (daysToSubtract == Long.MIN_VALUE) {
            plusDays(Long.MAX_VALUE).plusDays(1)
        } else {
            plusDays(-daysToSubtract)
        }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.EPOCH_DAY, toEpochDay())

    /** Calculates the ISO calendar period until [endDateExclusive]. */
    public fun until(endDateExclusive: LocalDate): Period {
        var totalMonths = endDateExclusive.prolepticMonth - prolepticMonth
        var days = endDateExclusive.dayOfMonth - dayOfMonth
        if (totalMonths > 0 && days < 0) {
            totalMonths--
            val calculatedDate = plusMonths(totalMonths)
            days = (endDateExclusive.toEpochDay() - calculatedDate.toEpochDay()).toInt()
        } else if (totalMonths < 0 && days > 0) {
            totalMonths++
            days -= endDateExclusive.lengthOfMonth()
        }
        return Period.of(
            years = (totalMonths / 12).toInt(),
            months = (totalMonths % 12).toInt(),
            days = days,
        )
    }

    override fun until(endDateExclusive: ChronoLocalDate): Period =
        until(from(endDateExclusive))

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = from(endExclusive)
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

    /**
     * Returns dates from this date, inclusive, to [endExclusive], exclusive,
     * one day at a time.
     */
    public fun datesUntil(endExclusive: LocalDate): Sequence<LocalDate> {
        val end = endExclusive.toEpochDay()
        val start = toEpochDay()
        if (end < start) {
            throw IllegalArgumentException("$endExclusive < $this")
        }
        return (start..<end).asSequence().map(::ofEpochDay)
    }

    /**
     * Returns dates from this date, inclusive, to [endExclusive], exclusive,
     * advancing by [step].
     */
    public fun datesUntil(
        endExclusive: LocalDate,
        step: Period,
    ): Sequence<LocalDate> {
        if (step.isZero) {
            throw IllegalArgumentException("step is zero")
        }
        val end = endExclusive.toEpochDay()
        val start = toEpochDay()
        val until = end - start
        val months = step.toTotalMonths()
        val days = step.days.toLong()
        if (months < 0 && days > 0 || months > 0 && days < 0) {
            throw IllegalArgumentException("period months and days are of opposite sign")
        }
        if (until == 0L) return emptySequence()

        val sign = if (months > 0 || days > 0) 1 else -1
        if ((sign < 0) xor (until < 0)) {
            val relation = if (sign < 0) " > " else " < "
            throw IllegalArgumentException("$endExclusive$relation$this")
        }
        if (months == 0L) {
            val steps = (until - sign) / days
            return (0L..steps).asSequence().map { n ->
                ofEpochDay(start + n * days)
            }
        }

        var steps = until * 1_600 / (months * 48_699 + days * 1_600) + 1
        var addMonths = months * steps
        var addDays = days * steps
        val maxAddMonths = if (months > 0) {
            MAX.prolepticMonth - prolepticMonth
        } else {
            prolepticMonth - MIN.prolepticMonth
        }
        if (
            addMonths * sign > maxAddMonths ||
            (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign
        ) {
            steps--
            addMonths -= months
            addDays -= days
            if (
                addMonths * sign > maxAddMonths ||
                (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign
            ) {
                steps--
            }
        }
        return (0L..steps).asSequence().map { n ->
            plusMonths(months * n).plusDays(days * n)
        }
    }

    private fun monthsUntil(end: LocalDate): Long {
        val packedThis = prolepticMonth * 32 + dayOfMonth
        val packedEnd = end.prolepticMonth * 32 + end.dayOfMonth
        return (packedEnd - packedThis) / 32
    }

    /** Combines this date with [time]. */
    override fun atTime(localTime: LocalTime): LocalDateTime = LocalDateTime.of(this, localTime)

    /** Combines this date with an offset time. */
    public fun atTime(time: OffsetTime): OffsetDateTime = OffsetDateTime.of(this, time.time, time.offset)

    /** Combines this date with an hour and minute. */
    public fun atTime(hour: Int, minute: Int): LocalDateTime =
        atTime(LocalTime.of(hour, minute))

    /** Combines this date with an hour, minute, and second. */
    public fun atTime(hour: Int, minute: Int, second: Int): LocalDateTime =
        atTime(LocalTime.of(hour, minute, second))

    /** Combines this date with a complete local time. */
    public fun atTime(
        hour: Int,
        minute: Int,
        second: Int,
        nanoOfSecond: Int,
    ): LocalDateTime = atTime(LocalTime.of(hour, minute, second, nanoOfSecond))

    /** Returns this date at midnight. */
    public fun atStartOfDay(): LocalDateTime = atTime(LocalTime.MIDNIGHT)

    /** Returns this date at the earliest valid local time in [zone]. */
    public fun atStartOfDay(zone: ZoneId): ZonedDateTime {
        var dateTime = atStartOfDay()
        if (zone !is ZoneOffset) {
            val transition = zone.rules.getTransition(dateTime)
            if (transition?.isGap == true) {
                dateTime = transition.dateTimeAfter
            }
        }
        return ZonedDateTime.of(dateTime, zone)
    }

    /** Combines this date with [time] and [offset] as an epoch-second value. */
    public fun toEpochSecond(time: LocalTime, offset: ZoneOffset): Long =
        toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay() - offset.totalSeconds

    override fun compareTo(other: ChronoLocalDate): Int {
        if (other !is LocalDate) return super<ChronoLocalDate>.compareTo(other)
        val yearComparison = year - other.year
        if (yearComparison != 0) return yearComparison
        val monthComparison = monthValue - other.monthValue
        return if (monthComparison != 0) monthComparison else dayOfMonth - other.dayOfMonth
    }

    /** Whether this date is after [other] on the local timeline. */
    override fun isAfter(other: ChronoLocalDate): Boolean =
        if (other is LocalDate) compareTo(other) > 0 else super<ChronoLocalDate>.isAfter(other)

    /** Whether this date is before [other] on the local timeline. */
    override fun isBefore(other: ChronoLocalDate): Boolean =
        if (other is LocalDate) compareTo(other) < 0 else super<ChronoLocalDate>.isBefore(other)

    /** Whether this date represents the same local date as [other]. */
    override fun isEqual(other: ChronoLocalDate): Boolean =
        if (other is LocalDate) compareTo(other) == 0 else super<ChronoLocalDate>.isEqual(other)

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LocalDate &&
            year == other.year &&
            monthValue == other.monthValue &&
            dayOfMonth == other.dayOfMonth

    override fun hashCode(): Int {
        val yearAndMonth = year and -2048 xor (year shl 11) xor (monthValue shl 6)
        return yearAndMonth xor dayOfMonth
    }

    override fun toString(): String {
        val yearText = when {
            year in 0..999 -> year.toString().padStart(4, '0')
            year in -999..-1 -> "-" + (-year).toString().padStart(4, '0')
            year > 9_999 -> "+$year"
            else -> year.toString()
        }
        return buildString {
            append(yearText)
            append('-')
            append(monthValue.toString().padStart(2, '0'))
            append('-')
            append(dayOfMonth.toString().padStart(2, '0'))
        }
    }

    /** Formats this date using [formatter]. */
    override fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    public companion object {
        private const val DAYS_PER_CYCLE: Long = 146_097
        private const val DAYS_0000_TO_1970: Long = 719_528
        private const val SECONDS_PER_DAY: Long = 86_400

        @JvmField
        public val MIN: LocalDate = LocalDate(Year.MIN_VALUE, 1, 1)
        @JvmField
        public val MAX: LocalDate = LocalDate(Year.MAX_VALUE, 12, 31)
        @JvmField
        public val EPOCH: LocalDate = LocalDate(1970, 1, 1)

        /** Obtains the current date using the system clock in the default time-zone. */
        @JvmStatic
        public fun now(): LocalDate = now(Clock.systemDefaultZone())

        /** Obtains the current date using the system clock in [zone]. */
        @JvmStatic
        public fun now(zone: ZoneId): LocalDate = now(Clock.system(zone))

        /** Obtains the current date from [clock]. */
        @JvmStatic
        public fun now(clock: Clock): LocalDate = LocalDateTime.now(clock).date

        private fun resolvePreviousValid(year: Int, month: Int, day: Int): LocalDate {
            val resolvedDay = minOf(day, Month.of(month).length(Year.isLeap(year.toLong())))
            return LocalDate(year, month, resolvedDay)
        }

        /** Obtains a date from an ISO year, month, and day. */
        @JvmStatic
        public fun of(year: Int, month: Month, dayOfMonth: Int): LocalDate =
            of(year, month.value, dayOfMonth)

        /** Obtains a date from an ISO year, month number, and day. */
        @JvmStatic
        public fun of(year: Int, month: Int, dayOfMonth: Int): LocalDate {
            val validYear = ChronoField.YEAR.checkValidIntValue(year.toLong())
            val validMonth = ChronoField.MONTH_OF_YEAR.checkValidIntValue(month.toLong())
            ChronoField.DAY_OF_MONTH.checkValidIntValue(dayOfMonth.toLong())
            val monthValue = Month.of(validMonth)
            if (dayOfMonth > monthValue.length(Year.isLeap(validYear.toLong()))) {
                throw DateTimeException("Invalid date '$monthValue $dayOfMonth'")
            }
            return LocalDate(validYear, validMonth, dayOfMonth)
        }

        /** Obtains a date from an ISO year and one-based day-of-year. */
        @JvmStatic
        public fun ofYearDay(year: Int, dayOfYear: Int): LocalDate {
            val validYear = ChronoField.YEAR.checkValidIntValue(year.toLong())
            ChronoField.DAY_OF_YEAR.checkValidIntValue(dayOfYear.toLong())
            val leap = Year.isLeap(validYear.toLong())
            if (dayOfYear == 366 && !leap) {
                throw DateTimeException("Invalid date 'DayOfYear 366' as '$year' is not a leap year")
            }
            var remaining = dayOfYear
            Month.entries.forEach { month ->
                val length = month.length(leap)
                if (remaining <= length) return LocalDate(validYear, month.value, remaining)
                remaining -= length
            }
            error("Validated day-of-year could not be resolved")
        }

        /** Obtains the local date at [instant] in [zone]. */
        @JvmStatic
        public fun ofInstant(instant: Instant, zone: ZoneId): LocalDate {
            val offset = zone.rules.getOffset(instant)
            val localSecond = instant.epochSecond + offset.totalSeconds
            return ofEpochDay(floorDiv(localSecond, SECONDS_PER_DAY))
        }

        /** Obtains a date from the count of days since 1970-01-01. */
        @JvmStatic
        public fun ofEpochDay(epochDay: Long): LocalDate {
            ChronoField.EPOCH_DAY.checkValidValue(epochDay)
            var zeroDay = epochDay + DAYS_0000_TO_1970 - 60
            var adjust = 0L
            if (zeroDay < 0) {
                val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                adjust = adjustCycles * 400
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
            }
            var yearEstimate = (400 * zeroDay + 591) / DAYS_PER_CYCLE
            var dayEstimate = zeroDay -
                (365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400)
            if (dayEstimate < 0) {
                yearEstimate--
                dayEstimate = zeroDay -
                    (365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400)
            }
            yearEstimate += adjust
            val marchDay = dayEstimate.toInt()
            val marchMonth = (marchDay * 5 + 2) / 153
            val month = (marchMonth + 2) % 12 + 1
            val day = marchDay - (marchMonth * 306 + 5) / 10 + 1
            yearEstimate += (marchMonth / 10).toLong()
            return LocalDate(
                ChronoField.YEAR.checkValidIntValue(yearEstimate),
                month,
                day,
            )
        }

        /** Obtains a date from a temporal accessor. */
        @JvmStatic
        public fun from(temporal: TemporalAccessor): LocalDate {
            if (temporal is LocalDate) return temporal
            return try {
                ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain LocalDate from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }

        /** Parses a date using the strict ISO local-date format. */
        @JvmStatic
        public fun parse(text: CharSequence): LocalDate {
            val input = text.toString()
            if (input.isEmpty()) throw parseFailure(input, 0)

            var index = 0
            val sign = when (input[0]) {
                '+' -> {
                    index++
                    1
                }
                '-' -> {
                    index++
                    -1
                }
                else -> 1
            }
            val signed = index == 1
            val yearStart = index
            var yearValue = 0L
            while (index < input.length && input[index].isAsciiDigit()) {
                if (index - yearStart < 10) {
                    yearValue = yearValue * 10 + (input[index] - '0')
                }
                index++
            }
            val yearDigits = index - yearStart
            if (yearDigits > 10) throw parseFailure(input, yearStart + 10)
            val validYearWidth = when {
                !signed -> yearDigits == 4
                sign < 0 -> yearDigits in 4..10
                else -> yearDigits in 5..10
            }
            if (!validYearWidth || sign < 0 && yearValue == 0L) {
                throw parseFailure(input, 0)
            }
            if (index >= input.length || input[index] != '-') {
                throw parseFailure(input, index.coerceAtMost(yearStart + 10))
            }

            val monthStart = ++index
            if (!hasTwoDigits(input, monthStart)) throw parseFailure(input, monthStart)
            val month = parseTwoDigits(input, monthStart)
            index += 2
            if (index >= input.length || input[index] != '-') throw parseFailure(input, index)

            val dayStart = ++index
            if (!hasTwoDigits(input, dayStart)) throw parseFailure(input, dayStart)
            val day = parseTwoDigits(input, dayStart)
            index += 2
            if (index != input.length) throw parseFailure(input, index)

            val year = if (sign < 0) -yearValue else yearValue
            return try {
                of(ChronoField.YEAR.checkValidIntValue(year), month, day)
            } catch (exception: DateTimeException) {
                throw DateTimeParseException(
                    "Text cannot be parsed to a LocalDate",
                    input,
                    0,
                    exception,
                )
            }
        }

        /** Parses a date from [text] using [formatter]. */
        @JvmStatic
        public fun parse(text: CharSequence, formatter: DateTimeFormatter): LocalDate =
            from(formatter.parse(text))

        private fun hasTwoDigits(input: String, index: Int): Boolean =
            index + 1 < input.length &&
                input[index].isAsciiDigit() &&
                input[index + 1].isAsciiDigit()

        private fun parseTwoDigits(input: String, index: Int): Int =
            (input[index] - '0') * 10 + (input[index + 1] - '0')

        private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

        private fun parseFailure(input: String, errorIndex: Int): DateTimeParseException =
            DateTimeParseException("Text cannot be parsed to a LocalDate", input, errorIndex)
    }
}
