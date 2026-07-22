package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.format.LocaleTextField
import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.format.localeTextValues
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/**
 * A day of the week in the ISO-8601 calendar system, from Monday to Sunday.
 *
 * The enum declaration order and [value] both follow ISO-8601: Monday is 1
 * and Sunday is 7.
 */
public enum class DayOfWeek : TemporalAccessor, TemporalAdjuster {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    /** The ISO-8601 day number, from 1 (Monday) to 7 (Sunday). */
    public val value: Int
        get() = ordinal + 1

    /** Returns this day's localized display name, or its numeric value when unavailable. */
    public fun getDisplayName(style: TextStyle, locale: Locale): String =
        localeTextValues(
            locale.toLanguageTag(),
            IsoChronology.id,
            LocaleTextField.DAY_OF_WEEK,
            style,
        ).firstOrNull { it.value == value.toLong() }?.text ?: value.toString()

    /** Returns this day with [days] added, wrapping around the seven-day week. */
    public fun plus(days: Long): DayOfWeek {
        val amount = (days % DAYS_PER_WEEK).toInt()
        return entries[(ordinal + amount + DAYS_PER_WEEK) % DAYS_PER_WEEK]
    }

    /** Returns this day with [days] subtracted, wrapping around the seven-day week. */
    public fun minus(days: Long): DayOfWeek = plus(-(days % DAYS_PER_WEEK))

    override fun isSupported(field: TemporalField): Boolean = if (field is ChronoField) {
        field == ChronoField.DAY_OF_WEEK
    } else {
        field.isSupportedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when {
        field == ChronoField.DAY_OF_WEEK -> value.toLong()
        field is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.DAYS as R
        }
        return super<TemporalAccessor>.query(query)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.DAY_OF_WEEK, value.toLong())

    public companion object {
        private const val DAYS_PER_WEEK: Int = 7

        /** Returns the day identified by its ISO-8601 value. */
        public fun of(dayOfWeek: Int): DayOfWeek {
            if (dayOfWeek !in 1..DAYS_PER_WEEK) {
                throw DateTimeException("Invalid value for DayOfWeek: $dayOfWeek")
            }
            return entries[dayOfWeek - 1]
        }

        /** Obtains a day-of-week from [temporal]. */
        public fun from(temporal: TemporalAccessor): DayOfWeek {
            if (temporal is DayOfWeek) return temporal
            return try {
                of(temporal.get(ChronoField.DAY_OF_WEEK))
            } catch (exception: DateTimeException) {
                throw DateTimeException(
                    "Unable to obtain DayOfWeek from TemporalAccessor: $temporal",
                    exception,
                )
            }
        }
    }
}
