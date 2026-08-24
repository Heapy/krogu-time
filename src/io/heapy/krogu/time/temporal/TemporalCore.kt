package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.Locale
import io.heapy.krogu.time.chrono.ChronoLocalDate
import io.heapy.krogu.time.chrono.ChronoLocalDateTime
import io.heapy.krogu.time.chrono.ChronoZonedDateTime
import io.heapy.krogu.time.format.ResolverStyle

/** A strategy for querying information from a [TemporalAccessor]. */
public fun interface TemporalQuery<R> {
    public fun queryFrom(temporal: TemporalAccessor): R
}

/** Read-only access to date-time fields. */
public interface TemporalAccessor {
    /** Returns whether this object supports [field]. */
    public fun isSupported(field: TemporalField?): Boolean

    /** Returns the refined valid range for [field]. */
    public fun range(field: TemporalField): ValueRange {
        if (field is ChronoField) {
            if (isSupported(field)) return field.range
            throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }
        return field.rangeRefinedBy(this)
    }

    /** Returns [field] as an [Int], validating that its range fits. */
    public fun get(field: TemporalField): Int {
        val fieldRange = range(field)
        if (!fieldRange.isIntValue) {
            throw UnsupportedTemporalTypeException(
                "Invalid field $field for get() method, use getLong() instead",
            )
        }
        return fieldRange.checkValidIntValue(getLong(field), field)
    }

    /** Returns [field] as a [Long]. */
    public fun getLong(field: TemporalField): Long

    /** Runs [query] against this object. */
    public fun <R> query(query: TemporalQuery<R>): R {
        if (
            query === TemporalQueries.zoneId() ||
            query === TemporalQueries.chronology() ||
            query === TemporalQueries.precision()
        ) {
            @Suppress("UNCHECKED_CAST")
            return null as R
        }
        return query.queryFrom(this)
    }
}

/** A unit used to measure a temporal amount. */
public interface TemporalUnit {
    public val duration: Duration
    public val isDurationEstimated: Boolean
    public val isDateBased: Boolean
    public val isTimeBased: Boolean

    /** Probes whether [temporal] can add this unit. */
    public fun isSupportedBy(temporal: Temporal): Boolean {
        when (temporal) {
            is LocalTime -> return isTimeBased
            is ChronoLocalDate -> return isDateBased
            is ChronoLocalDateTime<*>, is ChronoZonedDateTime<*> -> return true
        }
        try {
            temporal.plus(1, this)
            return true
        } catch (_: UnsupportedTemporalTypeException) {
            return false
        } catch (_: RuntimeException) {
            return try {
                temporal.plus(-1, this)
                true
            } catch (_: RuntimeException) {
                false
            }
        }
    }

    /** Adds [amount] of this unit to [temporal]. */
    public fun <R : Temporal> addTo(temporal: R, amount: Long): R

    /** Measures complete units between two temporal objects. */
    public fun between(temporal1Inclusive: Temporal, temporal2Exclusive: Temporal): Long
}

/** A field of date-time, such as month-of-year or hour-of-day. */
public interface TemporalField {
    /** Returns a localized field name, falling back to this field's string representation. */
    public fun getDisplayName(locale: Locale): String = toString()

    public val baseUnit: TemporalUnit
    public val rangeUnit: TemporalUnit
    public val range: ValueRange
    public val isDateBased: Boolean
    public val isTimeBased: Boolean

    public fun isSupportedBy(temporal: TemporalAccessor): Boolean

    public fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange

    public fun getFrom(temporal: TemporalAccessor): Long

    public fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R

    /**
     * Resolves this field with the other parsed [fieldValues], removing fields
     * that were consumed when a date or time is produced.
     */
    public fun resolve(
        fieldValues: MutableMap<TemporalField, Long>,
        partialTemporal: TemporalAccessor,
        resolverStyle: ResolverStyle,
    ): TemporalAccessor? = null
}

/** A strategy that adjusts a temporal object. */
public fun interface TemporalAdjuster {
    public fun adjustInto(temporal: Temporal): Temporal
}

/** An amount expressed using one or more [TemporalUnit] values. */
public interface TemporalAmount {
    public val units: List<TemporalUnit>

    public fun get(unit: TemporalUnit): Long

    public fun addTo(temporal: Temporal): Temporal

    public fun subtractFrom(temporal: Temporal): Temporal
}

/** A date-time object that supports field adjustment and unit arithmetic. */
public interface Temporal : TemporalAccessor {
    public fun isSupported(unit: TemporalUnit?): Boolean

    public fun with(adjuster: TemporalAdjuster): Temporal = adjuster.adjustInto(this)

    public fun with(field: TemporalField, newValue: Long): Temporal

    public fun plus(amount: TemporalAmount): Temporal = amount.addTo(this)

    public fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal

    public fun minus(amount: TemporalAmount): Temporal = amount.subtractFrom(this)

    public fun minus(amountToSubtract: Long, unit: TemporalUnit): Temporal =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    public fun until(endExclusive: Temporal, unit: TemporalUnit): Long
}
