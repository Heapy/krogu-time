package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.math.abs

/** A fixed offset from UTC in the range -18:00 to +18:00. */
public class ZoneOffset private constructor(
    public val totalSeconds: Int,
) : TemporalAccessor, TemporalAdjuster, Comparable<ZoneOffset> {
    /** The normalized textual identifier for this offset. */
    public val id: String = buildId(totalSeconds)

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) field === ChronoField.OFFSET_SECONDS else field.isSupportedBy(this)

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.OFFSET_SECONDS -> totalSeconds.toLong()
        is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.offset()) {
            @Suppress("UNCHECKED_CAST")
            return this as R
        }
        return super<TemporalAccessor>.query(query)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.OFFSET_SECONDS, totalSeconds.toLong())

    override fun compareTo(other: ZoneOffset): Int = other.totalSeconds - totalSeconds

    override fun equals(other: Any?): Boolean =
        this === other || other is ZoneOffset && totalSeconds == other.totalSeconds

    override fun hashCode(): Int = totalSeconds

    override fun toString(): String = id

    public companion object {
        private const val MAX_SECONDS: Int = 18 * 60 * 60
        private const val SECONDS_PER_QUARTER: Int = 15 * 60
        private const val CACHE_SIZE: Int = MAX_SECONDS * 2 / SECONDS_PER_QUARTER + 1
        private val QUARTER_HOUR_CACHE: Array<ZoneOffset> = Array(CACHE_SIZE) { index ->
            ZoneOffset(-MAX_SECONDS + index * SECONDS_PER_QUARTER)
        }

        public val UTC: ZoneOffset = QUARTER_HOUR_CACHE[MAX_SECONDS / SECONDS_PER_QUARTER]
        public val MIN: ZoneOffset = QUARTER_HOUR_CACHE.first()
        public val MAX: ZoneOffset = QUARTER_HOUR_CACHE.last()

        /** Obtains an offset from its textual identifier. */
        public fun of(offsetId: String): ZoneOffset {
            if (offsetId == "Z") return UTC
            val normalized = if (offsetId.length == 2) {
                "${offsetId[0]}0${offsetId[1]}"
            } else {
                offsetId
            }
            val hours: Int
            val minutes: Int
            val seconds: Int
            when (normalized.length) {
                3 -> {
                    hours = parseNumber(normalized, 1, false)
                    minutes = 0
                    seconds = 0
                }
                5 -> {
                    hours = parseNumber(normalized, 1, false)
                    minutes = parseNumber(normalized, 3, false)
                    seconds = 0
                }
                6 -> {
                    hours = parseNumber(normalized, 1, false)
                    minutes = parseNumber(normalized, 4, true)
                    seconds = 0
                }
                7 -> {
                    hours = parseNumber(normalized, 1, false)
                    minutes = parseNumber(normalized, 3, false)
                    seconds = parseNumber(normalized, 5, false)
                }
                9 -> {
                    hours = parseNumber(normalized, 1, false)
                    minutes = parseNumber(normalized, 4, true)
                    seconds = parseNumber(normalized, 7, true)
                }
                else -> throw DateTimeException(
                    "Invalid ID for ZoneOffset, invalid format: $normalized",
                )
            }
            return when (normalized[0]) {
                '+' -> ofHoursMinutesSeconds(hours, minutes, seconds)
                '-' -> ofHoursMinutesSeconds(-hours, -minutes, -seconds)
                else -> throw DateTimeException(
                    "Invalid ID for ZoneOffset, plus/minus not found when expected: $normalized",
                )
            }
        }

        /** Obtains an offset from a signed hour value. */
        public fun ofHours(hours: Int): ZoneOffset = ofHoursMinutesSeconds(hours, 0, 0)

        /** Obtains an offset from signed hour and minute values. */
        public fun ofHoursMinutes(hours: Int, minutes: Int): ZoneOffset =
            ofHoursMinutesSeconds(hours, minutes, 0)

        /** Obtains an offset from signed hour, minute, and second values. */
        public fun ofHoursMinutesSeconds(hours: Int, minutes: Int, seconds: Int): ZoneOffset {
            validate(hours, minutes, seconds)
            return ofTotalSeconds(hours * 3_600 + minutes * 60 + seconds)
        }

        /** Obtains an offset from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): ZoneOffset {
            val offset = temporal.query(TemporalQueries.offset())
            return offset ?: throw DateTimeException(
                "Unable to obtain ZoneOffset from TemporalAccessor: $temporal",
            )
        }

        /** Obtains an offset from its total number of seconds. */
        public fun ofTotalSeconds(totalSeconds: Int): ZoneOffset {
            if (totalSeconds !in -MAX_SECONDS..MAX_SECONDS) {
                throw DateTimeException("Zone offset not in valid range: -18:00 to +18:00")
            }
            return if (totalSeconds % SECONDS_PER_QUARTER == 0) {
                QUARTER_HOUR_CACHE[(totalSeconds + MAX_SECONDS) / SECONDS_PER_QUARTER]
            } else {
                ZoneOffset(totalSeconds)
            }
        }

        private fun parseNumber(offsetId: String, index: Int, precededByColon: Boolean): Int {
            if (precededByColon && offsetId[index - 1] != ':') {
                throw DateTimeException(
                    "Invalid ID for ZoneOffset, colon not found when expected: $offsetId",
                )
            }
            val first = offsetId[index]
            val second = offsetId[index + 1]
            if (first !in '0'..'9' || second !in '0'..'9') {
                throw DateTimeException(
                    "Invalid ID for ZoneOffset, non numeric characters found: $offsetId",
                )
            }
            return (first - '0') * 10 + (second - '0')
        }

        private fun validate(hours: Int, minutes: Int, seconds: Int) {
            if (hours !in -18..18) {
                throw DateTimeException(
                    "Zone offset hours not in valid range: value $hours is not in the range -18 to 18",
                )
            }
            if (hours > 0 && (minutes < 0 || seconds < 0)) {
                throw DateTimeException(
                    "Zone offset minutes and seconds must be positive because hours is positive",
                )
            }
            if (hours < 0 && (minutes > 0 || seconds > 0)) {
                throw DateTimeException(
                    "Zone offset minutes and seconds must be negative because hours is negative",
                )
            }
            if (hours == 0 && minutes > 0 && seconds < 0 || minutes < 0 && seconds > 0) {
                throw DateTimeException("Zone offset minutes and seconds must have the same sign")
            }
            if (minutes !in -59..59) {
                throw DateTimeException(
                    "Zone offset minutes not in valid range: value $minutes is not in the range -59 to 59",
                )
            }
            if (seconds !in -59..59) {
                throw DateTimeException(
                    "Zone offset seconds not in valid range: value $seconds is not in the range -59 to 59",
                )
            }
            if (abs(hours) == 18 && (minutes != 0 || seconds != 0)) {
                throw DateTimeException("Zone offset not in valid range: -18:00 to +18:00")
            }
        }

        private fun buildId(totalSeconds: Int): String {
            if (totalSeconds == 0) return "Z"
            val absoluteSeconds = abs(totalSeconds)
            val hours = absoluteSeconds / 3_600
            val minutes = absoluteSeconds / 60 % 60
            val seconds = absoluteSeconds % 60
            return buildString {
                append(if (totalSeconds < 0) '-' else '+')
                appendTwoDigits(hours)
                append(':')
                appendTwoDigits(minutes)
                if (seconds != 0) {
                    append(':')
                    appendTwoDigits(seconds)
                }
            }
        }

        private fun StringBuilder.appendTwoDigits(value: Int) {
            if (value < 10) append('0')
            append(value)
        }
    }
}
