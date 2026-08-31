package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.format.DateTimeFormatter
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
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A local date-time whose calendar system is supplied by a [Chronology]. */
public interface ChronoLocalDateTime<out D : ChronoLocalDate> :
    Temporal,
    TemporalAdjuster,
    Comparable<ChronoLocalDateTime<*>> {
    /** The chronology-specific local date part. */
    public val date: D

    /** The ISO local time part. */
    public val time: LocalTime

    /** The calendar system that defines the date part. */
    public val chronology: Chronology
        get() = date.chronology

    /** Returns the local-date part. */
    public fun toLocalDate(): D = date

    /** Returns the local-time part. */
    public fun toLocalTime(): LocalTime = time

    override fun isSupported(unit: TemporalUnit?): Boolean =
        if (unit is ChronoUnit) unit !== ChronoUnit.FOREVER else unit != null && unit.isSupportedBy(this)

    override fun with(adjuster: TemporalAdjuster): ChronoLocalDateTime<D> =
        ChronoLocalDateTimeImpl.ensureValid<D>(
            chronology,
            super<Temporal>.with(adjuster),
        )

    override fun with(field: TemporalField, newValue: Long): ChronoLocalDateTime<D>

    override fun plus(amount: TemporalAmount): ChronoLocalDateTime<D> =
        ChronoLocalDateTimeImpl.ensureValid<D>(
            chronology,
            super<Temporal>.plus(amount),
        )

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ChronoLocalDateTime<D>

    override fun minus(amount: TemporalAmount): ChronoLocalDateTime<D> =
        ChronoLocalDateTimeImpl.ensureValid<D>(
            chronology,
            super<Temporal>.minus(amount),
        )

    override fun minus(
        amountToSubtract: Long,
        unit: TemporalUnit,
    ): ChronoLocalDateTime<D> = ChronoLocalDateTimeImpl.ensureValid<D>(
        chronology,
        super<Temporal>.minus(amountToSubtract, unit),
    )

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any? = when (query) {
            TemporalQueries.zoneId(),
            TemporalQueries.zone(),
            TemporalQueries.offset(),
            -> null
            TemporalQueries.localTime() -> time
            TemporalQueries.chronology() -> chronology
            TemporalQueries.precision() -> ChronoUnit.NANOS
            else -> return query.queryFrom(this)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay())
            .with(ChronoField.NANO_OF_DAY, time.toNanoOfDay())

    /** Formats this local date-time using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    /** Converts this local date-time to epoch seconds using [offset]. */
    public fun toEpochSecond(offset: ZoneOffset): Long =
        date.toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay() - offset.totalSeconds

    /** Combines this local date-time with [offset] to create an instant. */
    public fun toInstant(offset: ZoneOffset): Instant =
        Instant.ofEpochSecond(toEpochSecond(offset), time.nano.toLong())

    /** Resolves this local date-time in [zone]. */
    public fun atZone(zone: ZoneId): ChronoZonedDateTime<D>

    override fun compareTo(other: ChronoLocalDateTime<*>): Int {
        val dateComparison = date.compareTo(other.date)
        if (dateComparison != 0) return dateComparison
        val timeComparison = time.compareTo(other.time)
        return if (timeComparison != 0) timeComparison else chronology.compareTo(other.chronology)
    }

    /** Whether this value is after [other] on the shared local timeline. */
    public fun isAfter(other: ChronoLocalDateTime<*>): Boolean {
        val epochDay = date.toEpochDay()
        val otherEpochDay = other.date.toEpochDay()
        return epochDay > otherEpochDay ||
            epochDay == otherEpochDay && time.toNanoOfDay() > other.time.toNanoOfDay()
    }

    /** Whether this value is before [other] on the shared local timeline. */
    public fun isBefore(other: ChronoLocalDateTime<*>): Boolean {
        val epochDay = date.toEpochDay()
        val otherEpochDay = other.date.toEpochDay()
        return epochDay < otherEpochDay ||
            epochDay == otherEpochDay && time.toNanoOfDay() < other.time.toNanoOfDay()
    }

    /** Whether this value represents the same date and time as [other]. */
    public fun isEqual(other: ChronoLocalDateTime<*>): Boolean =
        time.toNanoOfDay() == other.time.toNanoOfDay() &&
            date.toEpochDay() == other.date.toEpochDay()

    public companion object {
        private const val SECONDS_PER_DAY: Long = 86_400

        private val TIME_LINE_ORDER: Comparator<ChronoLocalDateTime<*>> = Comparator { first, second ->
            val dateComparison = first.date.toEpochDay().compareTo(second.date.toEpochDay())
            if (dateComparison != 0) {
                dateComparison
            } else {
                first.time.toNanoOfDay().compareTo(second.time.toNanoOfDay())
            }
        }

        /** Returns a comparator that ignores chronology and compares the local timeline only. */
        @JvmStatic
        public fun timeLineOrder(): Comparator<ChronoLocalDateTime<*>> = TIME_LINE_ORDER

        /** Obtains a chronology-aware local date-time from [temporal]. */
        @JvmStatic
        public fun from(temporal: TemporalAccessor): ChronoLocalDateTime<*> {
            if (temporal is ChronoLocalDateTime<*>) return temporal
            val chronology = temporal.query(TemporalQueries.chronology())
                ?: throw DateTimeException(
                    "Unable to obtain ChronoLocalDateTime from TemporalAccessor: $temporal",
                )
            return chronology.localDateTime(temporal)
        }
    }
}
