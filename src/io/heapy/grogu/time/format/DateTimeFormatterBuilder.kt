package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.chrono.ChronoLocalDate
import io.heapy.grogu.time.temporal.TemporalField

/**
 * Builds date-time formatters from a sequence of pattern and literal elements.
 *
 * Each formatter returned by [toFormatter] is an immutable snapshot of the
 * builder at the time it is created.
 */
public class DateTimeFormatterBuilder {
    private val rootSection: PatternSection = PatternSection()
    private val optionalSections: MutableList<PatternSection> = mutableListOf()

    private val activeSection: PatternSection
        get() = optionalSections.lastOrNull() ?: rootSection

    /** Makes parsing case-sensitive for subsequently appended elements. */
    public fun parseCaseSensitive(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ParseSetting(ParserSetting.CASE_SENSITIVE))
    }

    /** Makes parsing case-insensitive for subsequently appended elements. */
    public fun parseCaseInsensitive(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE))
    }

    /** Makes parsing strict for subsequently appended elements. */
    public fun parseStrict(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ParseSetting(ParserSetting.STRICT))
    }

    /** Makes parsing lenient for subsequently appended elements. */
    public fun parseLenient(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ParseSetting(ParserSetting.LENIENT))
    }

    /** Supplies [value] for [field] during parsing when it is still absent. */
    public fun parseDefaulting(
        field: TemporalField,
        value: Long,
    ): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.DefaultValue(field, value))
    }

    /** Appends the elements described by [pattern]. */
    public fun appendPattern(pattern: String): DateTimeFormatterBuilder = apply {
        visitPattern(
            pattern = pattern,
            appendToken = { token -> activeSection.appendToken(token) },
            padNext = { width -> padNext(width) },
            optionalStart = { optionalStart() },
            optionalEnd = {
                if (optionalSections.isEmpty()) {
                    throw IllegalArgumentException(
                        "Pattern invalid as it contains ] without previous [",
                    )
                }
                optionalEnd()
            },
        )
    }

    /** Appends [literal] without interpreting it as a pattern. */
    public fun appendLiteral(literal: Char): DateTimeFormatterBuilder =
        appendLiteral(literal.toString())

    /** Appends [literal] without interpreting it as a pattern. */
    public fun appendLiteral(literal: String): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.Literal(literal))
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
        activeSection.appendToken(PatternToken.Value(field, minWidth, maxWidth, signStyle))
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
        activeSection.appendToken(
            PatternToken.ReducedValue(
                field,
                width,
                maxWidth,
                ReducedValueBase.Value(baseValue),
            ),
        )
    }

    /** Appends a reduced numeric [field] relative to a chronology-aware [baseDate]. */
    public fun appendValueReduced(
        field: TemporalField,
        width: Int,
        maxWidth: Int,
        baseDate: ChronoLocalDate,
    ): DateTimeFormatterBuilder = apply {
        require(width in 1..10) { "The width must be from 1 to 10 inclusive but was $width" }
        require(maxWidth in 1..10) { "The maxWidth must be from 1 to 10 inclusive but was $maxWidth" }
        require(maxWidth >= width) {
            "Maximum width must exceed or equal the minimum width but $maxWidth < $width"
        }
        activeSection.appendToken(
            PatternToken.ReducedValue(
                field,
                width,
                maxWidth,
                ReducedValueBase.Date(baseDate),
            ),
        )
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
        activeSection.appendToken(PatternToken.Fraction(field, minWidth, maxWidth, decimalPoint))
    }

    /** Appends an instant using zero, three, six, or nine fractional digits as needed. */
    public fun appendInstant(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.Instant(-2))
    }

    /** Appends an instant with a variable (`-1`) or exact (`0..9`) fractional width. */
    public fun appendInstant(fractionalDigits: Int): DateTimeFormatterBuilder = apply {
        require(fractionalDigits in -1..9) {
            "The fractional digits must be from -1 to 9 inclusive but was $fractionalDigits"
        }
        activeSection.appendToken(PatternToken.Instant(fractionalDigits))
    }

    /** Appends an ISO zone offset ID, using `Z` for zero. */
    public fun appendOffsetId(): DateTimeFormatterBuilder = appendOffset("+HH:MM:ss", "Z")

    /** Appends a zone offset using [pattern] and [noOffsetText] for zero. */
    public fun appendOffset(
        pattern: String,
        noOffsetText: String,
    ): DateTimeFormatterBuilder = apply {
        validateOffsetPattern(pattern)
        activeSection.appendToken(PatternToken.Offset(pattern, noOffsetText))
    }

    /** Appends an explicit zone ID, without falling back to a bare offset. */
    public fun appendZoneId(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ZoneId(ZoneQueryMode.ZONE_ID))
    }

    /** Appends a region zone ID and rejects bare offsets while formatting. */
    public fun appendZoneRegionId(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ZoneId(ZoneQueryMode.REGION_ONLY))
    }

    /** Appends the best available zone ID or offset ID. */
    public fun appendZoneOrOffsetId(): DateTimeFormatterBuilder = apply {
        activeSection.appendToken(PatternToken.ZoneId(ZoneQueryMode.ZONE_OR_OFFSET))
    }

    /** Pads the next appended element to [padWidth] characters using spaces. */
    public fun padNext(padWidth: Int): DateTimeFormatterBuilder = padNext(padWidth, ' ')

    /** Pads the next appended element to [padWidth] characters using [padChar]. */
    public fun padNext(
        padWidth: Int,
        padChar: Char,
    ): DateTimeFormatterBuilder = apply {
        require(padWidth >= 1) { "The pad width must be at least one but was $padWidth" }
        activeSection.padWidth = padWidth
        activeSection.padCharacter = padChar
    }

    /** Starts a nested section that may be absent while formatting or parsing. */
    public fun optionalStart(): DateTimeFormatterBuilder = apply {
        optionalSections.add(PatternSection())
    }

    /** Ends the current optional section. */
    public fun optionalEnd(): DateTimeFormatterBuilder = apply {
        check(optionalSections.isNotEmpty()) {
            "Cannot call optionalEnd() as there was no previous call to optionalStart()"
        }
        val optionalSection = optionalSections.removeAt(optionalSections.lastIndex)
        if (optionalSection.tokens.isNotEmpty()) {
            activeSection.appendToken(PatternToken.Optional(optionalSection.tokens.toList()))
        }
    }

    /** Creates an immutable formatter from the elements appended so far. */
    public fun toFormatter(): DateTimeFormatter {
        while (optionalSections.isNotEmpty()) optionalEnd()
        return DateTimeFormatter.fromPatternTokens(rootSection.tokens)
    }
}

internal fun reducedPowerOfTen(power: Int): Long {
    var result = 1L
    repeat(power) { result *= 10 }
    return result
}
