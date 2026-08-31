package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Locale
import io.heapy.krogu.time.chrono.Chronology
import io.heapy.krogu.time.chrono.IsoChronology
import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.internal.addExact
import io.heapy.krogu.time.internal.multiplyExact
import io.heapy.krogu.time.internal.subtractExact
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** Fields and units specific to the ISO-8601 calendar system. */
public object IsoFields {
    /** The day within the quarter, from 1 to 90, 91, or 92. */
    @JvmField
    public val DAY_OF_QUARTER: TemporalField = Field.DAY_OF_QUARTER

    /** The quarter within the year, from 1 to 4. */
    @JvmField
    public val QUARTER_OF_YEAR: TemporalField = Field.QUARTER_OF_YEAR

    /** The week within the ISO week-based year, from 1 to 52 or 53. */
    @JvmField
    public val WEEK_OF_WEEK_BASED_YEAR: TemporalField = Field.WEEK_OF_WEEK_BASED_YEAR

    /** The ISO week-based year. */
    @JvmField
    public val WEEK_BASED_YEAR: TemporalField = Field.WEEK_BASED_YEAR

    /** A unit representing the addition of ISO week-based years. */
    @JvmField
    public val WEEK_BASED_YEARS: TemporalUnit = Unit.WEEK_BASED_YEARS

    /** A unit representing the addition of quarters. */
    @JvmField
    public val QUARTER_YEARS: TemporalUnit = Unit.QUARTER_YEARS

    private enum class Field(
        private val displayName: String,
    ) : TemporalField {
        DAY_OF_QUARTER("DayOfQuarter"),
        QUARTER_OF_YEAR("QuarterOfYear"),
        WEEK_OF_WEEK_BASED_YEAR("WeekOfWeekBasedYear"),
        WEEK_BASED_YEAR("WeekBasedYear"),
        ;

        override val baseUnit: TemporalUnit
            get() = when (this) {
                DAY_OF_QUARTER -> ChronoUnit.DAYS
                QUARTER_OF_YEAR -> Unit.QUARTER_YEARS
                WEEK_OF_WEEK_BASED_YEAR -> ChronoUnit.WEEKS
                WEEK_BASED_YEAR -> Unit.WEEK_BASED_YEARS
            }

        override val rangeUnit: TemporalUnit
            get() = when (this) {
                DAY_OF_QUARTER -> Unit.QUARTER_YEARS
                QUARTER_OF_YEAR -> ChronoUnit.YEARS
                WEEK_OF_WEEK_BASED_YEAR -> Unit.WEEK_BASED_YEARS
                WEEK_BASED_YEAR -> ChronoUnit.FOREVER
            }

        override val range: ValueRange
            get() = when (this) {
                DAY_OF_QUARTER -> ValueRange.of(1, 90, 92)
                QUARTER_OF_YEAR -> ValueRange.of(1, 4)
                WEEK_OF_WEEK_BASED_YEAR -> ValueRange.of(1, 52, 53)
                WEEK_BASED_YEAR -> ChronoField.YEAR.range
            }

        override val isDateBased: Boolean
            get() = true

        override val isTimeBased: Boolean
            get() = false

        override fun getDisplayName(locale: Locale): String =
            if (this == WEEK_OF_WEEK_BASED_YEAR) {
                localizedFieldDisplayName(locale.toLanguageTag(), "week") ?: displayName
            } else {
                displayName
            }

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = when (this) {
            DAY_OF_QUARTER ->
                temporal.isSupported(ChronoField.DAY_OF_YEAR) &&
                    temporal.isSupported(ChronoField.MONTH_OF_YEAR) &&
                    temporal.isSupported(ChronoField.YEAR) &&
                    isIso(temporal)
            QUARTER_OF_YEAR ->
                temporal.isSupported(ChronoField.MONTH_OF_YEAR) && isIso(temporal)
            WEEK_OF_WEEK_BASED_YEAR,
            WEEK_BASED_YEAR,
            -> temporal.isSupported(ChronoField.EPOCH_DAY) && isIso(temporal)
        }

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange {
            checkSupported(temporal)
            return when (this) {
                DAY_OF_QUARTER -> when (temporal.getLong(QUARTER_OF_YEAR)) {
                    1L -> if (IsoChronology.isLeapYear(temporal.getLong(ChronoField.YEAR))) {
                        ValueRange.of(1, 91)
                    } else {
                        ValueRange.of(1, 90)
                    }
                    2L -> ValueRange.of(1, 91)
                    3L, 4L -> ValueRange.of(1, 92)
                    else -> range
                }
                WEEK_OF_WEEK_BASED_YEAR -> weekRange(LocalDate.from(temporal))
                WEEK_BASED_YEAR -> {
                    val chronologyRange = Chronology.from(temporal).range(ChronoField.YEAR)
                    ValueRange.of(
                        maxOf(range.minimum, chronologyRange.minimum),
                        minOf(range.maximum, chronologyRange.maximum),
                    )
                }
                QUARTER_OF_YEAR -> range
            }
        }

        override fun getFrom(temporal: TemporalAccessor): Long {
            checkSupported(temporal)
            return when (this) {
                DAY_OF_QUARTER -> {
                    val dayOfYear = temporal.get(ChronoField.DAY_OF_YEAR)
                    val month = temporal.get(ChronoField.MONTH_OF_YEAR)
                    val year = temporal.getLong(ChronoField.YEAR)
                    val leapOffset = if (IsoChronology.isLeapYear(year)) 4 else 0
                    (dayOfYear - QUARTER_DAYS[(month - 1) / 3 + leapOffset]).toLong()
                }
                QUARTER_OF_YEAR -> (temporal.getLong(ChronoField.MONTH_OF_YEAR) + 2) / 3
                WEEK_OF_WEEK_BASED_YEAR -> week(LocalDate.from(temporal)).toLong()
                WEEK_BASED_YEAR -> weekBasedYear(LocalDate.from(temporal)).toLong()
            }
        }

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
            @Suppress("UNCHECKED_CAST")
            return when (this) {
                DAY_OF_QUARTER -> {
                    val currentValue = getFrom(temporal)
                    range.checkValidValue(newValue, this)
                    temporal.with(
                        ChronoField.DAY_OF_YEAR,
                        temporal.getLong(ChronoField.DAY_OF_YEAR) + newValue - currentValue,
                    ) as R
                }
                QUARTER_OF_YEAR -> {
                    val currentValue = getFrom(temporal)
                    range.checkValidValue(newValue, this)
                    temporal.with(
                        ChronoField.MONTH_OF_YEAR,
                        temporal.getLong(ChronoField.MONTH_OF_YEAR) + (newValue - currentValue) * 3,
                    ) as R
                }
                WEEK_OF_WEEK_BASED_YEAR -> {
                    range.checkValidValue(newValue, this)
                    temporal.plus(subtractExact(newValue, getFrom(temporal)), ChronoUnit.WEEKS) as R
                }
                WEEK_BASED_YEAR -> {
                    checkSupported(temporal)
                    val newWeekBasedYear = range.checkValidIntValue(newValue, this)
                    val date = LocalDate.from(temporal)
                    val dayOfWeek = date.dayOfWeek.value
                    var week = week(date)
                    if (week == 53 && weekRange(newWeekBasedYear) == 52) {
                        week = 52
                    }
                    val weekOne = LocalDate.of(newWeekBasedYear, 1, 4)
                    val days = dayOfWeek - weekOne.dayOfWeek.value + (week - 1) * 7
                    temporal.with(weekOne.plusDays(days.toLong())) as R
                }
            }
        }

        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor? = when (this) {
            DAY_OF_QUARTER -> resolveDayOfQuarter(fieldValues, partialTemporal, resolverStyle)
            WEEK_OF_WEEK_BASED_YEAR -> resolveWeekOfWeekBasedYear(
                fieldValues,
                partialTemporal,
                resolverStyle,
            )
            QUARTER_OF_YEAR,
            WEEK_BASED_YEAR,
            -> null
        }

        private fun resolveDayOfQuarter(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): LocalDate? {
            val yearValue = fieldValues[ChronoField.YEAR] ?: return null
            val quarterValue = fieldValues[QUARTER_OF_YEAR] ?: return null
            var dayOfQuarter = fieldValues[DAY_OF_QUARTER] ?: return null
            ensureIso(partialTemporal)
            val year = ChronoField.YEAR.checkValidIntValue(yearValue)
            val date = if (resolverStyle == ResolverStyle.LENIENT) {
                dayOfQuarter = subtractExact(dayOfQuarter, 1L)
                LocalDate.of(year, 1, 1).plusMonths(
                    multiplyExact(subtractExact(quarterValue, 1L), 3L),
                )
            } else {
                val quarter = QUARTER_OF_YEAR.range.checkValidIntValue(
                    quarterValue,
                    QUARTER_OF_YEAR,
                )
                val firstDay = LocalDate.of(year, (quarter - 1) * 3 + 1, 1)
                if (dayOfQuarter < 1L || dayOfQuarter > 90L) {
                    if (resolverStyle == ResolverStyle.STRICT) {
                        DAY_OF_QUARTER.rangeRefinedBy(firstDay)
                            .checkValidValue(dayOfQuarter, DAY_OF_QUARTER)
                    } else {
                        DAY_OF_QUARTER.range.checkValidValue(dayOfQuarter, DAY_OF_QUARTER)
                    }
                }
                dayOfQuarter--
                firstDay
            }
            fieldValues.remove(DAY_OF_QUARTER)
            fieldValues.remove(ChronoField.YEAR)
            fieldValues.remove(QUARTER_OF_YEAR)
            return date.plusDays(dayOfQuarter)
        }

        private fun resolveWeekOfWeekBasedYear(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): LocalDate? {
            val weekBasedYearValue = fieldValues[WEEK_BASED_YEAR] ?: return null
            val dayOfWeekValue = fieldValues[ChronoField.DAY_OF_WEEK] ?: return null
            val weekValue = fieldValues[WEEK_OF_WEEK_BASED_YEAR] ?: return null
            ensureIso(partialTemporal)
            val weekBasedYear = WEEK_BASED_YEAR.range.checkValidIntValue(
                weekBasedYearValue,
                WEEK_BASED_YEAR,
            )
            var date = LocalDate.of(weekBasedYear, 1, 4)
            date = if (resolverStyle == ResolverStyle.LENIENT) {
                var dayOfWeek = dayOfWeekValue
                if (dayOfWeek > 7L) {
                    date = date.plusWeeks((dayOfWeek - 1L) / 7L)
                    dayOfWeek = (dayOfWeek - 1L) % 7L + 1L
                } else if (dayOfWeek < 1L) {
                    date = date.plusWeeks(subtractExact(dayOfWeek, 7L) / 7L)
                    dayOfWeek = (dayOfWeek + 6L) % 7L + 1L
                }
                date.plusWeeks(subtractExact(weekValue, 1L))
                    .with(ChronoField.DAY_OF_WEEK, dayOfWeek)
            } else {
                val dayOfWeek = ChronoField.DAY_OF_WEEK.checkValidIntValue(dayOfWeekValue)
                if (weekValue < 1L || weekValue > 52L) {
                    if (resolverStyle == ResolverStyle.STRICT) {
                        weekRange(date).checkValidValue(weekValue, WEEK_OF_WEEK_BASED_YEAR)
                    } else {
                        WEEK_OF_WEEK_BASED_YEAR.range.checkValidValue(
                            weekValue,
                            WEEK_OF_WEEK_BASED_YEAR,
                        )
                    }
                }
                date.plusWeeks(weekValue - 1L)
                    .with(ChronoField.DAY_OF_WEEK, dayOfWeek.toLong())
            }
            fieldValues.remove(WEEK_OF_WEEK_BASED_YEAR)
            fieldValues.remove(WEEK_BASED_YEAR)
            fieldValues.remove(ChronoField.DAY_OF_WEEK)
            return date
        }

        private fun ensureIso(partialTemporal: TemporalAccessor) {
            if (Chronology.from(partialTemporal) != IsoChronology) {
                throw DateTimeException("Resolve requires IsoChronology")
            }
        }

        private fun checkSupported(temporal: TemporalAccessor) {
            if (!isSupportedBy(temporal)) {
                throw UnsupportedTemporalTypeException("Unsupported field: $displayName")
            }
        }

        override fun toString(): String = displayName
    }

    private enum class Unit(
        private val displayName: String,
        override val duration: Duration,
    ) : TemporalUnit {
        WEEK_BASED_YEARS("WeekBasedYears", Duration.ofSeconds(31_556_952)),
        QUARTER_YEARS("QuarterYears", Duration.ofSeconds(31_556_952 / 4)),
        ;

        override val isDurationEstimated: Boolean
            get() = true

        override val isDateBased: Boolean
            get() = true

        override val isTimeBased: Boolean
            get() = false

        override fun isSupportedBy(temporal: Temporal): Boolean =
            temporal.isSupported(ChronoField.EPOCH_DAY) && isIso(temporal)

        override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
            @Suppress("UNCHECKED_CAST")
            return when (this) {
                WEEK_BASED_YEARS -> temporal.with(
                    Field.WEEK_BASED_YEAR,
                    addExact(temporal.get(Field.WEEK_BASED_YEAR).toLong(), amount),
                ) as R
                QUARTER_YEARS -> temporal
                    .plus(amount / 4, ChronoUnit.YEARS)
                    .plus(amount % 4 * 3, ChronoUnit.MONTHS) as R
            }
        }

        override fun between(
            temporal1Inclusive: Temporal,
            temporal2Exclusive: Temporal,
        ): Long {
            if (temporal1Inclusive::class != temporal2Exclusive::class) {
                return temporal1Inclusive.until(temporal2Exclusive, this)
            }
            return when (this) {
                WEEK_BASED_YEARS -> subtractExact(
                    temporal2Exclusive.getLong(Field.WEEK_BASED_YEAR),
                    temporal1Inclusive.getLong(Field.WEEK_BASED_YEAR),
                )
                QUARTER_YEARS ->
                    temporal1Inclusive.until(temporal2Exclusive, ChronoUnit.MONTHS) / 3
            }
        }

        override fun toString(): String = displayName
    }

    private fun isIso(temporal: TemporalAccessor): Boolean =
        Chronology.from(temporal).isIsoBased

    private fun weekRange(date: LocalDate): ValueRange =
        ValueRange.of(1, weekRange(weekBasedYear(date)).toLong())

    private fun weekRange(weekBasedYear: Int): Int {
        val first = LocalDate.of(weekBasedYear, 1, 1)
        return if (
            first.dayOfWeek == DayOfWeek.THURSDAY ||
            first.dayOfWeek == DayOfWeek.WEDNESDAY && first.isLeapYear
        ) {
            53
        } else {
            52
        }
    }

    private fun week(date: LocalDate): Int {
        val dayOfWeekZeroBased = date.dayOfWeek.ordinal
        val dayOfYearZeroBased = date.dayOfYear - 1
        val thursdayDayOfYear = dayOfYearZeroBased + (3 - dayOfWeekZeroBased)
        val alignedWeek = thursdayDayOfYear / 7
        val firstThursdayDayOfYear = thursdayDayOfYear - alignedWeek * 7
        var firstMondayDayOfYear = firstThursdayDayOfYear - 3
        if (firstMondayDayOfYear < -3) {
            firstMondayDayOfYear += 7
        }
        if (dayOfYearZeroBased < firstMondayDayOfYear) {
            return weekRange(date.withDayOfYear(180).minusYears(1)).maximum.toInt()
        }
        var week = (dayOfYearZeroBased - firstMondayDayOfYear) / 7 + 1
        if (
            week == 53 &&
            firstMondayDayOfYear != -3 &&
            (firstMondayDayOfYear != -2 || !date.isLeapYear)
        ) {
            week = 1
        }
        return week
    }

    private fun weekBasedYear(date: LocalDate): Int {
        var year = date.year
        var dayOfYear = date.dayOfYear
        if (dayOfYear <= 3) {
            if (dayOfYear - date.dayOfWeek.ordinal < -2) {
                year--
            }
        } else if (dayOfYear >= 363) {
            dayOfYear -= 363 + if (date.isLeapYear) 1 else 0
            if (dayOfYear - date.dayOfWeek.ordinal >= 0) {
                year++
            }
        }
        return year
    }

    private val QUARTER_DAYS: IntArray = intArrayOf(0, 90, 181, 273, 0, 91, 182, 274)
}
