package io.heapy.grogu.time.format

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

    /** Creates an immutable formatter from the elements appended so far. */
    public fun toFormatter(): DateTimeFormatter = DateTimeFormatter.fromPatternTokens(tokens)
}
