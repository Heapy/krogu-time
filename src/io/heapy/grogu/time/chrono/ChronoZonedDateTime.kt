package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
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
import io.heapy.grogu.time.temporal.ValueRange

/** A zoned date-time whose calendar system is supplied by a [Chronology]. */
public interface ChronoZonedDateTime<out D : ChronoLocalDate> :
    Temporal,
    Comparable<ChronoZonedDateTime<*>> {
    /** The chronology-specific local date-time part. */
    public val dateTime: ChronoLocalDateTime<D>

    /** The local date part. */
    public val date: D
        get() = dateTime.date

    /** The local time part. */
    public val time: LocalTime
        get() = dateTime.time

    /** The calendar system that defines the date part. */
    public val chronology: Chronology
        get() = date.chronology

    /** The offset from UTC/Greenwich. */
    public val offset: ZoneOffset

    /** The stored time-zone identifier. */
    public val zone: ZoneId

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.INSTANT_SECONDS,
        ChronoField.OFFSET_SECONDS,
        -> field.range
        is ChronoField -> dateTime.range(field)
        else -> field.rangeRefinedBy(this)
    }

    override fun get(field: TemporalField): Int = when (field) {
        ChronoField.INSTANT_SECONDS -> throw UnsupportedTemporalTypeException(
            "Invalid field 'InstantSeconds' for get() method, use getLong() instead",
        )
        ChronoField.OFFSET_SECONDS -> offset.totalSeconds
        is ChronoField -> dateTime.get(field)
        else -> super<Temporal>.get(field)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.INSTANT_SECONDS -> toEpochSecond()
        ChronoField.OFFSET_SECONDS -> offset.totalSeconds.toLong()
        is ChronoField -> dateTime.getLong(field)
        else -> field.getFrom(this)
    }

    /** Returns the local-date-time part. */
    public fun toLocalDateTime(): ChronoLocalDateTime<D> = dateTime

    /** Returns the local-date part. */
    public fun toLocalDate(): D = date

    /** Returns the local-time part. */
    public fun toLocalTime(): LocalTime = time

    public fun withEarlierOffsetAtOverlap(): ChronoZonedDateTime<D>

    public fun withLaterOffsetAtOverlap(): ChronoZonedDateTime<D>

    public fun withZoneSameLocal(zone: ZoneId): ChronoZonedDateTime<D>

    public fun withZoneSameInstant(zone: ZoneId): ChronoZonedDateTime<D>

    override fun isSupported(unit: TemporalUnit?): Boolean =
        if (unit is ChronoUnit) unit !== ChronoUnit.FOREVER else unit != null && unit.isSupportedBy(this)

    override fun with(adjuster: TemporalAdjuster): ChronoZonedDateTime<D> =
        ChronoZonedDateTimeImpl.ensureValid<D>(
            chronology,
            super<Temporal>.with(adjuster),
        )

    override fun with(field: TemporalField, newValue: Long): ChronoZonedDateTime<D>

    override fun plus(amount: TemporalAmount): ChronoZonedDateTime<D> =
        ChronoZonedDateTimeImpl.ensureValid<D>(
            chronology,
            super<Temporal>.plus(amount),
        )

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ChronoZonedDateTime<D>

    override fun minus(amount: TemporalAmount): ChronoZonedDateTime<D> =
        ChronoZonedDateTimeImpl.ensureValid<D>(
            chronology,
            super<Temporal>.minus(amount),
        )

    override fun minus(
        amountToSubtract: Long,
        unit: TemporalUnit,
    ): ChronoZonedDateTime<D> = ChronoZonedDateTimeImpl.ensureValid<D>(
        chronology,
        super<Temporal>.minus(amountToSubtract, unit),
    )

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any = when (query) {
            TemporalQueries.zoneId(),
            TemporalQueries.zone(),
            -> zone
            TemporalQueries.offset() -> offset
            TemporalQueries.localTime() -> time
            TemporalQueries.chronology() -> chronology
            TemporalQueries.precision() -> ChronoUnit.NANOS
            else -> return query.queryFrom(this)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    /** Formats this zoned date-time using [formatter]. */
    public fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    /** Converts this value to the corresponding instant. */
    public fun toInstant(): Instant =
        Instant.ofEpochSecond(toEpochSecond(), time.nano.toLong())

    /** Converts this value to epoch seconds. */
    public fun toEpochSecond(): Long =
        date.toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay() - offset.totalSeconds

    override fun compareTo(other: ChronoZonedDateTime<*>): Int {
        val epochComparison = toEpochSecond().compareTo(other.toEpochSecond())
        if (epochComparison != 0) return epochComparison
        val nanoComparison = time.nano - other.time.nano
        if (nanoComparison != 0) return nanoComparison
        val localComparison = dateTime.compareTo(other.dateTime)
        if (localComparison != 0) return localComparison
        val zoneComparison = zone.id.compareTo(other.zone.id)
        return if (zoneComparison != 0) zoneComparison else chronology.compareTo(other.chronology)
    }

    /** Whether this value's instant is before [other]. */
    public fun isBefore(other: ChronoZonedDateTime<*>): Boolean {
        val epochSecond = toEpochSecond()
        val otherEpochSecond = other.toEpochSecond()
        return epochSecond < otherEpochSecond ||
            epochSecond == otherEpochSecond && time.nano < other.time.nano
    }

    /** Whether this value's instant is after [other]. */
    public fun isAfter(other: ChronoZonedDateTime<*>): Boolean {
        val epochSecond = toEpochSecond()
        val otherEpochSecond = other.toEpochSecond()
        return epochSecond > otherEpochSecond ||
            epochSecond == otherEpochSecond && time.nano > other.time.nano
    }

    /** Whether this value represents the same instant as [other]. */
    public fun isEqual(other: ChronoZonedDateTime<*>): Boolean =
        toEpochSecond() == other.toEpochSecond() && time.nano == other.time.nano

    public companion object {
        private const val SECONDS_PER_DAY: Long = 86_400

        private val TIME_LINE_ORDER: Comparator<ChronoZonedDateTime<*>> =
            Comparator { first, second ->
                val epochComparison = first.toEpochSecond().compareTo(second.toEpochSecond())
                if (epochComparison != 0) {
                    epochComparison
                } else {
                    first.time.nano.compareTo(second.time.nano)
                }
            }

        /** Returns a comparator that ignores chronology and compares instants only. */
        public fun timeLineOrder(): Comparator<ChronoZonedDateTime<*>> = TIME_LINE_ORDER

        /** Obtains a chronology-aware zoned date-time from [temporal]. */
        public fun from(temporal: TemporalAccessor): ChronoZonedDateTime<*> {
            if (temporal is ChronoZonedDateTime<*>) return temporal
            val chronology = temporal.query(TemporalQueries.chronology())
                ?: throw DateTimeException(
                    "Unable to obtain ChronoZonedDateTime from TemporalAccessor: $temporal",
                )
            return chronology.zonedDateTime(temporal)
        }
    }
}
