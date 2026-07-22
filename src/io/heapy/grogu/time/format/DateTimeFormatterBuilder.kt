package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.temporal.TemporalField

/**
 * Builds date-time formatters from a sequence of pattern and literal elements.
 *
 * Each formatter returned by [toFormatter] is an immutable snapshot of the
 * builder at the time it is created.
 */
public class DateTimeFormatterBuilder {
    private val tokens: MutableList<PatternToken> = mutableListOf()

    /** Appends the elements described by [pattern]. */
    public fun appendPattern(pattern: String): DateTimeFormatterBuilder = apply {
        tokens += compilePattern(pattern)
    }

    /** Appends [literal] without interpreting it as a pattern. */
    public fun appendLiteral(literal: Char): DateTimeFormatterBuilder =
        appendLiteral(literal.toString())

    /** Appends [literal] without interpreting it as a pattern. */
    public fun appendLiteral(literal: String): DateTimeFormatterBuilder = apply {
        tokens.appendPatternLiteral(literal)
    }

    /** Appends a variable-width numeric [field]. */
    public fun appendValue(field: TemporalField): DateTimeFormatterBuilder =
        appendValue(field, 1, 19, SignStyle.NORMAL)

    /** Appends a fixed-width, non-negative numeric [field]. */
    public fun appendValue(
        field: TemporalField,
        width: Int,
    ): DateTimeFormatterBuilder = appendValue(field, width, width, SignStyle.NOT_NEGATIVE)

    /** Appends a numeric [field] using the requested widths and [signStyle]. */
    public fun appendValue(
        field: TemporalField,
        minWidth: Int,
        maxWidth: Int,
        signStyle: SignStyle,
    ): DateTimeFormatterBuilder = apply {
        require(minWidth in 1..19) { "Minimum width must be from 1 to 19 inclusive but was $minWidth" }
        require(maxWidth in 1..19) { "Maximum width must be from 1 to 19 inclusive but was $maxWidth" }
        require(maxWidth >= minWidth) {
            "Maximum width must exceed or equal the minimum width but $maxWidth < $minWidth"
        }
        tokens += PatternToken.Value(field, minWidth, maxWidth, signStyle)
    }

    /** Appends a reduced numeric [field] interpreted relative to [baseValue]. */
    public fun appendValueReduced(
        field: TemporalField,
        width: Int,
        maxWidth: Int,
        baseValue: Int,
    ): DateTimeFormatterBuilder = apply {
        require(width in 1..10) { "The width must be from 1 to 10 inclusive but was $width" }
        require(maxWidth in 1..10) { "The maxWidth must be from 1 to 10 inclusive but was $maxWidth" }
        require(maxWidth >= width) {
            "Maximum width must exceed or equal the minimum width but $maxWidth < $width"
        }
        require(field.range.isValidValue(baseValue.toLong())) {
            "The base value must be within the range of the field"
        }
        if (baseValue.toLong() + reducedPowerOfTen(maxWidth) > Int.MAX_VALUE) {
            throw DateTimeException(
                "Unable to add printer-parser as the range exceeds the capacity of an int",
            )
        }
        tokens += PatternToken.ReducedValue(field, width, maxWidth, baseValue)
    }

    /** Appends the fractional value of a fixed-range [field]. */
    public fun appendFraction(
        field: TemporalField,
        minWidth: Int,
        maxWidth: Int,
        decimalPoint: Boolean,
    ): DateTimeFormatterBuilder = apply {
        require(field.range.isFixed) { "Field must have a fixed set of values: $field" }
        require(minWidth in 0..9) {
            "Minimum width must be from 0 to 9 inclusive but was $minWidth"
        }
        require(maxWidth in 1..9) {
            "Maximum width must be from 1 to 9 inclusive but was $maxWidth"
        }
        require(maxWidth >= minWidth) {
            "Maximum width must exceed or equal the minimum width but $maxWidth < $minWidth"
        }
        tokens += PatternToken.Fraction(field, minWidth, maxWidth, decimalPoint)
    }

    /** Appends an ISO zone offset ID, using `Z` for zero. */
    public fun appendOffsetId(): DateTimeFormatterBuilder = appendOffset("+HH:MM:ss", "Z")

    /** Appends a zone offset using [pattern] and [noOffsetText] for zero. */
    public fun appendOffset(
        pattern: String,
        noOffsetText: String,
    ): DateTimeFormatterBuilder = apply {
        validateOffsetPattern(pattern)
        tokens += PatternToken.Offset(pattern, noOffsetText)
    }

    /** Appends an explicit zone ID, without falling back to a bare offset. */
    public fun appendZoneId(): DateTimeFormatterBuilder = apply {
        tokens += PatternToken.ZoneId(ZoneQueryMode.ZONE_ID)
    }

    /** Appends a region zone ID and rejects bare offsets while formatting. */
    public fun appendZoneRegionId(): DateTimeFormatterBuilder = apply {
        tokens += PatternToken.ZoneId(ZoneQueryMode.REGION_ONLY)
    }

    /** Appends the best available zone ID or offset ID. */
    public fun appendZoneOrOffsetId(): DateTimeFormatterBuilder = apply {
        tokens += PatternToken.ZoneId(ZoneQueryMode.ZONE_OR_OFFSET)
    }

    /** Creates an immutable formatter from the elements appended so far. */
    public fun toFormatter(): DateTimeFormatter = DateTimeFormatter.fromPatternTokens(tokens)
}

internal fun reducedPowerOfTen(power: Int): Long {
    var result = 1L
    repeat(power) { result *= 10 }
    return result
}
