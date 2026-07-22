package io.heapy.grogu.time.format

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

    /** Creates an immutable formatter from the elements appended so far. */
    public fun toFormatter(): DateTimeFormatter = DateTimeFormatter.fromPatternTokens(tokens)
}
