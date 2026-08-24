package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.format.DateTimeFormatter
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/** A date whose calendar system is supplied by a [Chronology]. */
public interface ChronoLocalDate : Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {
    /** The calendar system that defines this date. */
    public val chronology: Chronology

    /** The chronology-specific era containing this date. */
    public val era: Era
        get() = chronology.eraOf(get(ChronoField.ERA))

    /** Whether this date occurs in a leap year. */
    public val isLeapYear: Boolean
        get() = chronology.isLeapYear(getLong(ChronoField.YEAR))

    /** Returns the number of days in this date's month. */
    public fun lengthOfMonth(): Int

    /** Returns the number of days in this date's year. */
    public fun lengthOfYear(): Int = if (isLeapYear) 366 else 365

    override fun isSupported(field: TemporalField?): Boolean =
        if (field is ChronoField) field.isDateBased else field != null && field.isSupportedBy(this)

    override fun isSupported(unit: TemporalUnit?): Boolean =
        if (unit is ChronoUnit) unit.isDateBased else unit != null && unit.isSupportedBy(this)

    override fun with(adjuster: TemporalAdjuster): ChronoLocalDate =
        ensureValid(chronology, super<Temporal>.with(adjuster))

    override fun with(field: TemporalField, newValue: Long): ChronoLocalDate {
        if (field is ChronoField) {
            throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
        return ensureValid(chronology, field.adjustInto(this, newValue))
    }

    override fun plus(amount: TemporalAmount): ChronoLocalDate =
        ensureValid(chronology, super<Temporal>.plus(amount))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ChronoLocalDate {
        if (unit is ChronoUnit) {
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
        return ensureValid(chronology, unit.addTo(this, amountToAdd))
    }

    override fun minus(amount: TemporalAmount): ChronoLocalDate =
        ensureValid(chronology, super<Temporal>.minus(amount))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): ChronoLocalDate =
        ensureValid(chronology, super<Temporal>.minus(amountToSubtract, unit))

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any? = when (query) {
            TemporalQueries.zoneId(),
            TemporalQueries.zone(),
            TemporalQueries.offset(),
            TemporalQueries.localTime(),
            -> null
            TemporalQueries.chronology() -> chronology
            TemporalQueries.precision() -> ChronoUnit.DAYS
            else -> return query.queryFrom(this)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.EPOCH_DAY, toEpochDay())

    /** Converts this date to the shared epoch-day count. */
    public fun toEpochDay(): Long = getLong(ChronoField.EPOCH_DAY)

    /** Calculates the chronology-specific period until [endDateExclusive]. */
    public fun until(endDateExclusive: ChronoLocalDate): ChronoPeriod

    /** Formats this date using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    /** Combines this date with [localTime]. */
    public fun atTime(localTime: LocalTime): ChronoLocalDateTime<*> =
        ChronoLocalDateTimeImpl.of(this, localTime)

    override fun compareTo(other: ChronoLocalDate): Int {
        val epochComparison = toEpochDay().compareTo(other.toEpochDay())
        return if (epochComparison != 0) epochComparison else chronology.compareTo(other.chronology)
    }

    /** Whether this date is after [other] on the shared local timeline. */
    public fun isAfter(other: ChronoLocalDate): Boolean = toEpochDay() > other.toEpochDay()

    /** Whether this date is before [other] on the shared local timeline. */
    public fun isBefore(other: ChronoLocalDate): Boolean = toEpochDay() < other.toEpochDay()

    /** Whether this date represents the same epoch day as [other]. */
    public fun isEqual(other: ChronoLocalDate): Boolean = toEpochDay() == other.toEpochDay()

    public companion object {
        private val TIME_LINE_ORDER: Comparator<ChronoLocalDate> =
            Comparator { first, second -> first.toEpochDay().compareTo(second.toEpochDay()) }

        /** Returns a comparator that ignores chronology and compares epoch days only. */
        public fun timeLineOrder(): Comparator<ChronoLocalDate> = TIME_LINE_ORDER

        /** Obtains a chronology-aware date from [temporal]. */
        public fun from(temporal: TemporalAccessor): ChronoLocalDate {
            if (temporal is ChronoLocalDate) return temporal
            val chronology = temporal.query(TemporalQueries.chronology())
                ?: throw DateTimeException(
                    "Unable to obtain ChronoLocalDate from TemporalAccessor: $temporal",
                )
            return chronology.date(temporal)
        }
    }
}

private fun ensureValid(
    chronology: Chronology,
    temporal: Temporal,
): ChronoLocalDate {
    val date = temporal as? ChronoLocalDate
        ?: throw ClassCastException("Temporal is not a ChronoLocalDate: $temporal")
    if (date.chronology != chronology) {
        throw ClassCastException(
            "Chronology mismatch, expected: ${chronology.id}, actual: ${date.chronology.id}",
        )
    }
    return date
}
