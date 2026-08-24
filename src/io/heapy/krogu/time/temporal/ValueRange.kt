package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DateTimeException

/**
 * The outer range of valid values for a date-time field.
 *
 * A range can have variable minimum and maximum bounds. It does not imply that
 * every value between those outer bounds is valid for a particular temporal.
 */
public class ValueRange private constructor(
    public val minimum: Long,
    public val largestMinimum: Long,
    public val smallestMaximum: Long,
    public val maximum: Long,
) {
    /** Whether both the minimum and maximum are fixed. */
    public val isFixed: Boolean
        get() = minimum == largestMinimum && smallestMaximum == maximum

    /** Whether every value in this range can be represented by [Int]. */
    public val isIntValue: Boolean
        get() = minimum >= Int.MIN_VALUE && maximum <= Int.MAX_VALUE

    /** Returns whether [value] is within the outer bounds. */
    public fun isValidValue(value: Long): Boolean = value in minimum..maximum

    /** Returns whether [value] is valid and this entire range fits in [Int]. */
    public fun isValidIntValue(value: Long): Boolean = isIntValue && isValidValue(value)

    /**
     * Returns [value] if valid, otherwise throws [DateTimeException].
     * [field] is used only to make the exception message more useful.
     */
    public fun checkValidValue(value: Long, field: TemporalField?): Long {
        if (!isValidValue(value)) {
            throw DateTimeException(invalidValueMessage(value, field))
        }
        return value
    }

    /**
     * Returns [value] as an [Int] if valid and the entire range fits in [Int].
     */
    public fun checkValidIntValue(value: Long, field: TemporalField?): Int {
        if (!isValidIntValue(value)) {
            throw DateTimeException(invalidValueMessage(value, field))
        }
        return value.toInt()
    }

    private fun invalidValueMessage(value: Long, field: TemporalField?): String = if (field != null) {
        "Invalid value for $field (valid values $this): $value"
    } else {
        "Invalid value (valid values $this): $value"
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ValueRange &&
            minimum == other.minimum &&
            largestMinimum == other.largestMinimum &&
            smallestMaximum == other.smallestMaximum &&
            maximum == other.maximum

    override fun hashCode(): Int {
        var result = minimum.hashCode()
        result = 31 * result + largestMinimum.hashCode()
        result = 31 * result + smallestMaximum.hashCode()
        result = 31 * result + maximum.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append(minimum)
        if (minimum != largestMinimum) {
            append('/')
            append(largestMinimum)
        }
        append(" - ")
        append(smallestMaximum)
        if (smallestMaximum != maximum) {
            append('/')
            append(maximum)
        }
    }

    public companion object {
        /** Creates a range with fixed minimum and maximum bounds. */
        public fun of(minimum: Long, maximum: Long): ValueRange {
            require(minimum <= maximum) {
                "Minimum value must be less than maximum value"
            }
            return ValueRange(minimum, minimum, maximum, maximum)
        }

        /** Creates a range with a fixed minimum and variable maximum. */
        public fun of(
            minimum: Long,
            smallestMaximum: Long,
            maximum: Long,
        ): ValueRange {
            require(minimum <= smallestMaximum) {
                "Minimum value must be less than smallest maximum value"
            }
            return of(minimum, minimum, smallestMaximum, maximum)
        }

        /** Creates a range with variable minimum and maximum bounds. */
        public fun of(
            minimum: Long,
            largestMinimum: Long,
            smallestMaximum: Long,
            maximum: Long,
        ): ValueRange {
            require(minimum <= largestMinimum) {
                "Smallest minimum value must be less than largest minimum value"
            }
            require(smallestMaximum <= maximum) {
                "Smallest maximum value must be less than largest maximum value"
            }
            require(largestMinimum <= maximum) {
                "Largest minimum value must be less than largest maximum value"
            }
            require(minimum <= smallestMaximum) {
                "Smallest minimum value must be less than smallest maximum value"
            }
            return ValueRange(minimum, largestMinimum, smallestMaximum, maximum)
        }
    }
}
