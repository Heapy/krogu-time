package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Locale
import io.heapy.krogu.time.chrono.ChronoLocalDate
import io.heapy.krogu.time.chrono.Chronology
import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.internal.addExact
import io.heapy.krogu.time.internal.multiplyExact
import io.heapy.krogu.time.internal.subtractExact
import io.heapy.krogu.time.localeWeekRules
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A localized definition of the first day of the week and the minimum number
 * of days required in the first week of a month or year.
 */
public class WeekFields private constructor(
    public val firstDayOfWeek: DayOfWeek,
    public val minimalDaysInFirstWeek: Int,
) {
    /** Day of week numbered from this definition's [firstDayOfWeek]. */
    public val dayOfWeek: TemporalField = ComputedDayOfField(
        displayName = "DayOfWeek",
        weekDefinition = this,
        baseUnit = ChronoUnit.DAYS,
        rangeUnit = ChronoUnit.WEEKS,
        range = DAY_OF_WEEK_RANGE,
    )

    /** Week within the month, including a possible week zero. */
    public val weekOfMonth: TemporalField = ComputedDayOfField(
        displayName = "WeekOfMonth",
        weekDefinition = this,
        baseUnit = ChronoUnit.WEEKS,
        rangeUnit = ChronoUnit.MONTHS,
        range = WEEK_OF_MONTH_RANGE,
    )

    /** Week within the calendar year, including a possible week zero. */
    public val weekOfYear: TemporalField = ComputedDayOfField(
        displayName = "WeekOfYear",
        weekDefinition = this,
        baseUnit = ChronoUnit.WEEKS,
        rangeUnit = ChronoUnit.YEARS,
        range = WEEK_OF_YEAR_RANGE,
    )

    /** Week within the week-based year. */
    public val weekOfWeekBasedYear: TemporalField = ComputedDayOfField(
        displayName = "WeekOfWeekBasedYear",
        weekDefinition = this,
        baseUnit = ChronoUnit.WEEKS,
        rangeUnit = IsoFields.WEEK_BASED_YEARS,
        range = WEEK_OF_WEEK_BASED_YEAR_RANGE,
    )

    /** Year defined by complete localized weeks. */
    public val weekBasedYear: TemporalField = ComputedDayOfField(
        displayName = "WeekBasedYear",
        weekDefinition = this,
        baseUnit = IsoFields.WEEK_BASED_YEARS,
        rangeUnit = ChronoUnit.FOREVER,
        range = ChronoField.YEAR.range,
    )

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is WeekFields &&
            firstDayOfWeek == other.firstDayOfWeek &&
            minimalDaysInFirstWeek == other.minimalDaysInFirstWeek

    override fun hashCode(): Int = firstDayOfWeek.ordinal * 7 + minimalDaysInFirstWeek

    override fun toString(): String =
        "WeekFields[$firstDayOfWeek,$minimalDaysInFirstWeek]"

    private class ComputedDayOfField(
        private val displayName: String,
        private val weekDefinition: WeekFields,
        override val baseUnit: TemporalUnit,
        override val rangeUnit: TemporalUnit,
        override val range: ValueRange,
    ) : TemporalField {
        override val isDateBased: Boolean
            get() = true

        override val isTimeBased: Boolean
            get() = false

        override fun getDisplayName(locale: Locale): String =
            if (rangeUnit === ChronoUnit.YEARS) {
                localizedFieldDisplayName(locale.toLanguageTag(), "week") ?: displayName
            } else {
                displayName
            }

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean {
            if (!temporal.isSupported(ChronoField.DAY_OF_WEEK)) return false
            return when (rangeUnit) {
                ChronoUnit.WEEKS -> true
                ChronoUnit.MONTHS -> temporal.isSupported(ChronoField.DAY_OF_MONTH)
                ChronoUnit.YEARS,
                IsoFields.WEEK_BASED_YEARS,
                -> temporal.isSupported(ChronoField.DAY_OF_YEAR)
                ChronoUnit.FOREVER -> temporal.isSupported(ChronoField.YEAR)
                else -> false
            }
        }

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = when (rangeUnit) {
            ChronoUnit.WEEKS -> range
            ChronoUnit.MONTHS -> rangeByWeek(temporal, ChronoField.DAY_OF_MONTH)
            ChronoUnit.YEARS -> rangeByWeek(temporal, ChronoField.DAY_OF_YEAR)
            IsoFields.WEEK_BASED_YEARS -> rangeWeekOfWeekBasedYear(temporal)
            ChronoUnit.FOREVER -> ChronoField.YEAR.range
            else -> error("Unreachable range unit: $rangeUnit")
        }

        override fun getFrom(temporal: TemporalAccessor): Long = when (rangeUnit) {
            ChronoUnit.WEEKS -> localizedDayOfWeek(temporal).toLong()
            ChronoUnit.MONTHS -> localizedWeekOfMonth(temporal).toLong()
            ChronoUnit.YEARS -> localizedWeekOfYear(temporal).toLong()
            IsoFields.WEEK_BASED_YEARS -> localizedWeekOfWeekBasedYear(temporal).toLong()
            ChronoUnit.FOREVER -> localizedWeekBasedYear(temporal).toLong()
            else -> error("Unreachable range unit: $rangeUnit")
        }

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
            val newValueInt = range.checkValidIntValue(newValue, this)
            val currentValue = temporal.get(this)
            if (newValueInt == currentValue) return temporal

            @Suppress("UNCHECKED_CAST")
            return if (rangeUnit === ChronoUnit.FOREVER) {
                val localizedDayOfWeek = temporal.get(weekDefinition.dayOfWeek)
                val week = temporal.get(weekDefinition.weekOfWeekBasedYear)
                ofWeekBasedYear(
                    Chronology.from(temporal),
                    newValueInt,
                    week,
                    localizedDayOfWeek,
                ) as R
            } else {
                temporal.plus((newValueInt - currentValue).toLong(), baseUnit) as R
            }
        }

        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor? {
            val value = fieldValues[this] ?: return null
            val newValue = toIntExact(value)
            if (rangeUnit === ChronoUnit.WEEKS) {
                val checkedValue = range.checkValidIntValue(value, this)
                val isoDayOfWeek = floorMod(
                    weekDefinition.firstDayOfWeek.value - 1 + checkedValue - 1,
                    7,
                ) + 1
                fieldValues.remove(this)
                fieldValues[ChronoField.DAY_OF_WEEK] = isoDayOfWeek.toLong()
                return null
            }

            val isoDayOfWeek = fieldValues[ChronoField.DAY_OF_WEEK] ?: return null
            val localDayOfWeek = localizedDayOfWeek(
                ChronoField.DAY_OF_WEEK.checkValidIntValue(isoDayOfWeek),
            )
            val chronology = Chronology.from(partialTemporal)
            val yearValue = fieldValues[ChronoField.YEAR]
            if (yearValue != null) {
                val year = ChronoField.YEAR.checkValidIntValue(yearValue)
                if (rangeUnit === ChronoUnit.MONTHS && ChronoField.MONTH_OF_YEAR in fieldValues) {
                    return resolveWeekOfMonth(
                        fieldValues = fieldValues,
                        chronology = chronology,
                        year = year,
                        month = requireNotNull(fieldValues[ChronoField.MONTH_OF_YEAR]),
                        weekOfMonth = newValue.toLong(),
                        localDayOfWeek = localDayOfWeek,
                        resolverStyle = resolverStyle,
                    )
                }
                if (rangeUnit === ChronoUnit.YEARS) {
                    return resolveWeekOfYear(
                        fieldValues = fieldValues,
                        chronology = chronology,
                        year = year,
                        weekOfYear = newValue.toLong(),
                        localDayOfWeek = localDayOfWeek,
                        resolverStyle = resolverStyle,
                    )
                }
            } else if (
                (rangeUnit === IsoFields.WEEK_BASED_YEARS || rangeUnit === ChronoUnit.FOREVER) &&
                weekDefinition.weekBasedYear in fieldValues &&
                weekDefinition.weekOfWeekBasedYear in fieldValues
            ) {
                return resolveWeekBasedYear(
                    fieldValues = fieldValues,
                    chronology = chronology,
                    localDayOfWeek = localDayOfWeek,
                    resolverStyle = resolverStyle,
                )
            }
            return null
        }

        private fun resolveWeekOfMonth(
            fieldValues: MutableMap<TemporalField, Long>,
            chronology: Chronology,
            year: Int,
            month: Long,
            weekOfMonth: Long,
            localDayOfWeek: Int,
            resolverStyle: ResolverStyle,
        ): ChronoLocalDate {
            val date = if (resolverStyle == ResolverStyle.LENIENT) {
                val first = chronology.date(year, 1, 1)
                    .plus(subtractExact(month, 1), ChronoUnit.MONTHS)
                val weeks = subtractExact(weekOfMonth, localizedWeekOfMonth(first).toLong())
                val days = localDayOfWeek - localizedDayOfWeek(first)
                first.plus(
                    addExact(multiplyExact(weeks, 7), days.toLong()),
                    ChronoUnit.DAYS,
                )
            } else {
                val validMonth = ChronoField.MONTH_OF_YEAR.checkValidIntValue(month)
                val first = chronology.date(year, validMonth, 1)
                val validWeek = range.checkValidIntValue(weekOfMonth, this)
                val weeks = validWeek - localizedWeekOfMonth(first)
                val days = localDayOfWeek - localizedDayOfWeek(first)
                first.plus((weeks * 7 + days).toLong(), ChronoUnit.DAYS).also { resolved ->
                    if (
                        resolverStyle == ResolverStyle.STRICT &&
                        resolved.getLong(ChronoField.MONTH_OF_YEAR) != month
                    ) {
                        throw DateTimeException(
                            "Strict mode rejected resolved date as it is in a different month",
                        )
                    }
                }
            }
            fieldValues.remove(this)
            fieldValues.remove(ChronoField.YEAR)
            fieldValues.remove(ChronoField.MONTH_OF_YEAR)
            fieldValues.remove(ChronoField.DAY_OF_WEEK)
            return date
        }

        private fun resolveWeekOfYear(
            fieldValues: MutableMap<TemporalField, Long>,
            chronology: Chronology,
            year: Int,
            weekOfYear: Long,
            localDayOfWeek: Int,
            resolverStyle: ResolverStyle,
        ): ChronoLocalDate {
            val first = chronology.date(year, 1, 1)
            val date = if (resolverStyle == ResolverStyle.LENIENT) {
                val weeks = subtractExact(weekOfYear, localizedWeekOfYear(first).toLong())
                val days = localDayOfWeek - localizedDayOfWeek(first)
                first.plus(
                    addExact(multiplyExact(weeks, 7), days.toLong()),
                    ChronoUnit.DAYS,
                )
            } else {
                val validWeek = range.checkValidIntValue(weekOfYear, this)
                val weeks = validWeek - localizedWeekOfYear(first)
                val days = localDayOfWeek - localizedDayOfWeek(first)
                first.plus((weeks * 7 + days).toLong(), ChronoUnit.DAYS).also { resolved ->
                    if (
                        resolverStyle == ResolverStyle.STRICT &&
                        resolved.getLong(ChronoField.YEAR) != year.toLong()
                    ) {
                        throw DateTimeException(
                            "Strict mode rejected resolved date as it is in a different year",
                        )
                    }
                }
            }
            fieldValues.remove(this)
            fieldValues.remove(ChronoField.YEAR)
            fieldValues.remove(ChronoField.DAY_OF_WEEK)
            return date
        }

        private fun resolveWeekBasedYear(
            fieldValues: MutableMap<TemporalField, Long>,
            chronology: Chronology,
            localDayOfWeek: Int,
            resolverStyle: ResolverStyle,
        ): ChronoLocalDate {
            val weekBasedYear = weekDefinition.weekBasedYear.range.checkValidIntValue(
                requireNotNull(fieldValues[weekDefinition.weekBasedYear]),
                weekDefinition.weekBasedYear,
            )
            val date = if (resolverStyle == ResolverStyle.LENIENT) {
                val first = ofWeekBasedYear(chronology, weekBasedYear, 1, localDayOfWeek)
                val weeks = subtractExact(
                    requireNotNull(fieldValues[weekDefinition.weekOfWeekBasedYear]),
                    1,
                )
                first.plus(weeks, ChronoUnit.WEEKS)
            } else {
                val validWeek = weekDefinition.weekOfWeekBasedYear
                    .rangeRefinedBy(LocalDate.of(weekBasedYear, 7, 2))
                    .checkValidIntValue(
                        requireNotNull(fieldValues[weekDefinition.weekOfWeekBasedYear]),
                        weekDefinition.weekOfWeekBasedYear,
                    )
                ofWeekBasedYear(
                    chronology,
                    weekBasedYear,
                    validWeek,
                    localDayOfWeek,
                ).also { resolved ->
                    if (
                        resolverStyle == ResolverStyle.STRICT &&
                        localizedWeekBasedYear(resolved) != weekBasedYear
                    ) {
                        throw DateTimeException(
                            "Strict mode rejected resolved date as it is in a different week-based-year",
                        )
                    }
                }
            }
            fieldValues.remove(this)
            fieldValues.remove(weekDefinition.weekBasedYear)
            fieldValues.remove(weekDefinition.weekOfWeekBasedYear)
            fieldValues.remove(ChronoField.DAY_OF_WEEK)
            return date
        }

        private fun ofWeekBasedYear(
            chronology: Chronology,
            weekBasedYear: Int,
            week: Int,
            localizedDayOfWeek: Int,
        ): ChronoLocalDate {
            val first = chronology.date(weekBasedYear, 1, 1)
            val offset = startOfWeekOffset(1, localizedDayOfWeek(first))
            val firstWeekOfNextYear = computeWeek(
                offset,
                first.lengthOfYear() + weekDefinition.minimalDaysInFirstWeek,
            )
            val clampedWeek = minOf(week, firstWeekOfNextYear - 1)
            val days = -offset + localizedDayOfWeek - 1 + (clampedWeek - 1) * 7
            return first.plus(days.toLong(), ChronoUnit.DAYS)
        }

        private fun localizedDayOfWeek(temporal: TemporalAccessor): Int =
            localizedDayOfWeek(temporal.get(ChronoField.DAY_OF_WEEK))

        private fun localizedDayOfWeek(isoDayOfWeek: Int): Int =
            floorMod(isoDayOfWeek - weekDefinition.firstDayOfWeek.value, 7) + 1

        private fun localizedWeekOfMonth(temporal: TemporalAccessor): Int {
            val day = temporal.get(ChronoField.DAY_OF_MONTH)
            return computeWeek(startOfWeekOffset(day, localizedDayOfWeek(temporal)), day)
        }

        private fun localizedWeekOfYear(temporal: TemporalAccessor): Int {
            val day = temporal.get(ChronoField.DAY_OF_YEAR)
            return computeWeek(startOfWeekOffset(day, localizedDayOfWeek(temporal)), day)
        }

        private fun localizedWeekBasedYear(temporal: TemporalAccessor): Int {
            val localizedDayOfWeek = localizedDayOfWeek(temporal)
            val year = temporal.get(ChronoField.YEAR)
            val dayOfYear = temporal.get(ChronoField.DAY_OF_YEAR)
            val offset = startOfWeekOffset(dayOfYear, localizedDayOfWeek)
            val week = computeWeek(offset, dayOfYear)
            if (week == 0) return year - 1

            val yearLength = temporal.range(ChronoField.DAY_OF_YEAR).maximum.toInt()
            val firstWeekOfNextYear = computeWeek(
                offset,
                yearLength + weekDefinition.minimalDaysInFirstWeek,
            )
            return if (week >= firstWeekOfNextYear) year + 1 else year
        }

        private fun localizedWeekOfWeekBasedYear(temporal: TemporalAccessor): Int {
            val dayOfYear = temporal.get(ChronoField.DAY_OF_YEAR)
            val offset = startOfWeekOffset(dayOfYear, localizedDayOfWeek(temporal))
            var week = computeWeek(offset, dayOfYear)
            if (week == 0) {
                val previousYear = Chronology.from(temporal)
                    .date(temporal)
                    .minus(dayOfYear.toLong(), ChronoUnit.DAYS)
                return localizedWeekOfWeekBasedYear(previousYear)
            }
            if (week > 50) {
                val yearLength = temporal.range(ChronoField.DAY_OF_YEAR).maximum.toInt()
                val firstWeekOfNextYear = computeWeek(
                    offset,
                    yearLength + weekDefinition.minimalDaysInFirstWeek,
                )
                if (week >= firstWeekOfNextYear) {
                    week = week - firstWeekOfNextYear + 1
                }
            }
            return week
        }

        private fun rangeByWeek(
            temporal: TemporalAccessor,
            field: TemporalField,
        ): ValueRange {
            val offset = startOfWeekOffset(temporal.get(field), localizedDayOfWeek(temporal))
            val fieldRange = temporal.range(field)
            return ValueRange.of(
                computeWeek(offset, fieldRange.minimum.toInt()).toLong(),
                computeWeek(offset, fieldRange.maximum.toInt()).toLong(),
            )
        }

        private fun rangeWeekOfWeekBasedYear(temporal: TemporalAccessor): ValueRange {
            if (!temporal.isSupported(ChronoField.DAY_OF_YEAR)) return WEEK_OF_YEAR_RANGE

            val dayOfYear = temporal.get(ChronoField.DAY_OF_YEAR)
            val offset = startOfWeekOffset(dayOfYear, localizedDayOfWeek(temporal))
            val week = computeWeek(offset, dayOfYear)
            if (week == 0) {
                val previousYear = Chronology.from(temporal)
                    .date(temporal)
                    .minus((dayOfYear + 7).toLong(), ChronoUnit.DAYS)
                return rangeWeekOfWeekBasedYear(previousYear)
            }

            val yearLength = temporal.range(ChronoField.DAY_OF_YEAR).maximum.toInt()
            val firstWeekOfNextYear = computeWeek(
                offset,
                yearLength + weekDefinition.minimalDaysInFirstWeek,
            )
            if (week >= firstWeekOfNextYear) {
                val nextYear = Chronology.from(temporal)
                    .date(temporal)
                    .plus((yearLength - dayOfYear + 8).toLong(), ChronoUnit.DAYS)
                return rangeWeekOfWeekBasedYear(nextYear)
            }
            return ValueRange.of(1, (firstWeekOfNextYear - 1).toLong())
        }

        private fun startOfWeekOffset(day: Int, localizedDayOfWeek: Int): Int {
            val weekStart = floorMod(day - localizedDayOfWeek, 7)
            return if (weekStart + 1 > weekDefinition.minimalDaysInFirstWeek) {
                7 - weekStart
            } else {
                -weekStart
            }
        }

        private fun computeWeek(offset: Int, day: Int): Int =
            (7 + offset + day - 1) / 7

        override fun toString(): String = "$displayName[$weekDefinition]"
    }

    public companion object {
        private val DAY_OF_WEEK_RANGE: ValueRange = ValueRange.of(1, 7)
        private val WEEK_OF_MONTH_RANGE: ValueRange = ValueRange.of(0, 1, 4, 6)
        private val WEEK_OF_YEAR_RANGE: ValueRange = ValueRange.of(0, 1, 52, 54)
        private val WEEK_OF_WEEK_BASED_YEAR_RANGE: ValueRange = ValueRange.of(1, 52, 53)

        private val DEFINITIONS: List<WeekFields> = DayOfWeek.entries.flatMap { firstDay ->
            (1..7).map { minimalDays -> WeekFields(firstDay, minimalDays) }
        }

        /** ISO-8601 weeks, starting Monday with four required days. */
        @JvmField
        public val ISO: WeekFields = of(DayOfWeek.MONDAY, 4)

        /** Weeks starting Sunday where the first partial week is week one. */
        @JvmField
        public val SUNDAY_START: WeekFields = of(DayOfWeek.SUNDAY, 1)

        /** The shared unit for adding and subtracting week-based years. */
        @JvmField
        public val WEEK_BASED_YEARS: TemporalUnit = IsoFields.WEEK_BASED_YEARS

        /** Obtains the canonical definition for the supplied week rules. */
        @JvmStatic
        public fun of(locale: Locale): WeekFields {
            val rules = localeWeekRules(locale.toLanguageTag())
            return of(DayOfWeek.of(rules.firstDayOfWeek), rules.minimalDaysInFirstWeek)
        }

        /** Obtains the canonical definition for the supplied explicit week rules. */
        @JvmStatic
        public fun of(
            firstDayOfWeek: DayOfWeek,
            minimalDaysInFirstWeek: Int,
        ): WeekFields {
            require(minimalDaysInFirstWeek in 1..7) {
                "Minimal number of days is invalid"
            }
            return DEFINITIONS[firstDayOfWeek.ordinal * 7 + minimalDaysInFirstWeek - 1]
        }

        private fun floorMod(value: Int, divisor: Int): Int {
            val remainder = value % divisor
            return if (remainder < 0) remainder + divisor else remainder
        }

        private fun toIntExact(value: Long): Int = value.toInt().also { converted ->
            if (converted.toLong() != value) throw ArithmeticException("integer overflow")
        }
    }
}
