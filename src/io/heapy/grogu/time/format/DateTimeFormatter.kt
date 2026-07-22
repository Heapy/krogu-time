package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.Period
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.chrono.ChronoLocalDate
import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.IsoFields
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange

/** A formatter that prints and parses date-time objects. */
public class DateTimeFormatter private constructor(
    private val printer: (TemporalAccessor) -> String,
    private val parser: (CharSequence) -> TemporalAccessor,
    private val description: String,
    private val decimalStyleScope: DecimalStyleScope = DecimalStyleScope.ALL,
    private val resolverParser: ((String, ResolverStyle) -> TemporalAccessor)? = null,
    public val decimalStyle: DecimalStyle = DecimalStyle.STANDARD,
    public val resolverStyle: ResolverStyle = ResolverStyle.STRICT,
    public val chronology: Chronology? = null,
    public val zone: ZoneId? = null,
) {
    /** Formats [temporal] into a string. */
    public fun format(temporal: TemporalAccessor): String =
        decimalStyleScope.localize(printer(adjustForFormatting(temporal)), decimalStyle)

    /** Formats [temporal] and appends the result to [appendable]. */
    public fun formatTo(temporal: TemporalAccessor, appendable: Appendable) {
        appendable.append(format(temporal))
    }

    /** Parses [text] into a temporal accessor. */
    public fun parse(text: CharSequence): TemporalAccessor {
        val standardized = decimalStyleScope.standardize(text, decimalStyle)
        val parsed = resolverParser?.invoke(standardized, resolverStyle) ?: parser(standardized)
        return try {
            applyOverrides(parsed)
        } catch (exception: DateTimeParseException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw createParseError(text, exception)
        }
    }

    /** Returns a formatter using [decimalStyle] for numeric symbols. */
    public fun withDecimalStyle(decimalStyle: DecimalStyle): DateTimeFormatter =
        if (decimalStyle == this.decimalStyle) {
            this
        } else {
            DateTimeFormatter(
                printer = printer,
                parser = parser,
                description = description,
                decimalStyleScope = decimalStyleScope,
                resolverParser = resolverParser,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                chronology = chronology,
                zone = zone,
            )
        }

    /** Returns a formatter using [resolverStyle] while resolving parsed fields. */
    public fun withResolverStyle(resolverStyle: ResolverStyle): DateTimeFormatter =
        if (resolverStyle == this.resolverStyle) {
            this
        } else {
            DateTimeFormatter(
                printer = printer,
                parser = parser,
                description = description,
                decimalStyleScope = decimalStyleScope,
                resolverParser = resolverParser,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                chronology = chronology,
                zone = zone,
            )
        }

    /** Returns a formatter that overrides the chronology while formatting and parsing. */
    public fun withChronology(chronology: Chronology?): DateTimeFormatter =
        if (chronology == this.chronology) {
            this
        } else {
            DateTimeFormatter(
                printer = printer,
                parser = parser,
                description = description,
                decimalStyleScope = decimalStyleScope,
                resolverParser = resolverParser,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                chronology = chronology,
                zone = zone,
            )
        }

    /** Returns a formatter that overrides the zone while formatting and parsing. */
    public fun withZone(zone: ZoneId?): DateTimeFormatter =
        if (zone == this.zone) {
            this
        } else {
            DateTimeFormatter(
                printer = printer,
                parser = parser,
                description = description,
                decimalStyleScope = decimalStyleScope,
                resolverParser = resolverParser,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                chronology = chronology,
                zone = zone,
            )
        }

    /** Parses [text] and applies [query] to the result. */
    public fun <T> parse(text: CharSequence, query: TemporalQuery<T>): T =
        try {
            query.queryFrom(parse(text))
        } catch (exception: DateTimeParseException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw createParseError(text, exception)
        }

    /**
     * Parses [text], returning the result of the first query that can convert
     * the parsed temporal object.
     */
    public fun parseBest(
        text: CharSequence,
        vararg queries: TemporalQuery<*>,
    ): TemporalAccessor {
        require(queries.size >= 2) { "At least two queries must be specified" }

        try {
            val resolved = parse(text)
            queries.forEach { query ->
                try {
                    return query.queryFrom(resolved) as TemporalAccessor
                } catch (_: RuntimeException) {
                    // Try the next query in priority order.
                }
            }
            throw DateTimeException(
                "Unable to convert parsed text using any of the specified queries",
            )
        } catch (exception: DateTimeParseException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw createParseError(text, exception)
        }
    }

    private fun createParseError(
        text: CharSequence,
        cause: RuntimeException,
    ): DateTimeParseException {
        val input = text.toString()
        val displayText = if (input.length > 64) "${input.take(64)}..." else input
        return DateTimeParseException(
            "Text '$displayText' could not be parsed: ${cause.message}",
            input,
            0,
            cause,
        )
    }

    private fun adjustForFormatting(temporal: TemporalAccessor): TemporalAccessor {
        val temporalChronology = temporal.query(TemporalQueries.chronology())
        val temporalZone = temporal.query(TemporalQueries.zoneId())
        val overrideChronology = chronology?.takeUnless { it == temporalChronology }
        val overrideZone = zone?.takeUnless { it == temporalZone }
        if (overrideChronology == null && overrideZone == null) return temporal

        val effectiveChronology = overrideChronology ?: temporalChronology
        if (overrideZone != null && temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            val instantChronology = effectiveChronology ?: IsoChronology
            return instantChronology.zonedDateTime(Instant.from(temporal), overrideZone)
        }

        if (overrideZone != null) {
            val normalizedZone = overrideZone.normalized()
            if (
                normalizedZone is ZoneOffset &&
                temporal.isSupported(ChronoField.OFFSET_SECONDS) &&
                temporal.getLong(ChronoField.OFFSET_SECONDS) != normalizedZone.totalSeconds.toLong()
            ) {
                throw DateTimeException(
                    "Unable to apply override zone '$overrideZone' because the temporal object " +
                        "being formatted has a different offset but does not represent an instant: " +
                        temporal,
                )
            }
        }

        val effectiveDate = if (overrideChronology != null) {
            if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
                overrideChronology.date(temporal)
            } else {
                if (overrideChronology != IsoChronology || temporalChronology != null) {
                    ChronoField.entries.firstOrNull { field ->
                        field.isDateBased && temporal.isSupported(field)
                    }?.let {
                        throw DateTimeException(
                            "Unable to apply override chronology '$overrideChronology' because the temporal " +
                                "object being formatted contains date fields but does not represent a whole date: " +
                                temporal,
                        )
                    }
                }
                null
            }
        } else {
            null
        }

        return FormatterOverrideTemporalAccessor(
            delegate = temporal,
            date = effectiveDate,
            chronology = effectiveChronology,
            zone = overrideZone ?: temporalZone,
        )
    }

    private fun applyOverrides(parsed: TemporalAccessor): TemporalAccessor {
        if (parsed is ParsedTemporalAccessor) {
            return parsed
                .withChronology(chronology ?: IsoChronology, resolverStyle)
                .let { resolved -> zone?.let(resolved::withDefaultZone) ?: resolved }
        }

        val parsedChronology = parsed.query(TemporalQueries.chronology())
        val parsedZone = parsed.query(TemporalQueries.zoneId())
        if (chronology == null && (zone == null || parsedZone != null)) return parsed
        return FormatterOverrideTemporalAccessor(
            delegate = parsed,
            chronology = parsedChronology ?: chronology ?: IsoChronology,
            zone = parsedZone ?: zone,
        )
    }

    override fun toString(): String = description

    public companion object {
        /** Creates a formatter from a date-time pattern. */
        public fun ofPattern(pattern: String): DateTimeFormatter =
            fromPatternTokens(compilePattern(pattern))

        internal fun fromPatternTokens(tokens: List<PatternToken>): DateTimeFormatter {
            val snapshot = tokens.toList()
            return DateTimeFormatter(
                printer = { temporal -> formatPattern(snapshot, temporal) },
                parser = { text -> parsePattern(snapshot, text.toString(), ResolverStyle.SMART) },
                description = describePattern(snapshot),
                resolverParser = { text, style -> parsePattern(snapshot, text, style) },
                resolverStyle = ResolverStyle.SMART,
            )
        }

        /** The strict ISO formatter for a date without a time or offset. */
        public val ISO_LOCAL_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = ::formatIsoDateFields,
            parser = { text -> LocalDate.parse(text) },
            description =
                "Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)",
            resolverParser = { text, style ->
                ParsedTemporalAccessor(
                    date = parseResolvedIsoDate(text, style, "ISO local date"),
                )
            },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a date with a required offset. */
        public val ISO_OFFSET_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                formatIsoDateFields(temporal) + ZoneOffset.from(temporal)
            },
            parser = { text -> parseIsoDate(text, offsetRequired = true) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))Offset(+HH:MM:ss,'Z')",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoDate(text, offsetRequired = true, resolverStyle = style)
            },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a date with an optional offset. */
        public val ISO_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(formatIsoDateFields(temporal))
                    if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
                        append(ZoneOffset.from(temporal))
                    }
                }
            },
            parser = { text -> parseIsoDate(text, offsetRequired = false) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))[Offset(+HH:MM:ss,'Z')]",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoDate(text, offsetRequired = false, resolverStyle = style)
            },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a time without a date or offset. */
        public val ISO_LOCAL_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> formatIsoLocalTime(LocalTime.from(temporal)) },
            parser = { text -> LocalTime.parse(text) },
            description =
                "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]",
            resolverParser = { text, style -> parseResolvedIsoTimeAccessor(text, style) },
        )

        /** The strict ISO formatter for a time with an optional offset. */
        public val ISO_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(formatIsoLocalTime(LocalTime.from(temporal)))
                    if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
                        append(ZoneOffset.from(temporal))
                    }
                }
            },
            parser = { text -> parseIsoTime(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]])" +
                    "[Offset(+HH:MM:ss,'Z')]",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoTime(text, offsetRequired = false, resolverStyle = style)
            },
        )

        /** The strict ISO formatter for a date-time without an offset. */
        public val ISO_LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = ::formatIsoLocalDateTime,
            parser = { text -> LocalDateTime.parse(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))'T'" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]])",
            resolverParser = { text, style ->
                val dateTime = parseResolvedIsoDateTime(text, style)
                ParsedTemporalAccessor(date = dateTime.date, time = dateTime.time)
            },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a date-time with an optional offset and region zone. */
        public val ISO_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(formatIsoLocalDateTime(temporal))
                    if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
                        append(ZoneOffset.from(temporal))
                        temporal.query(TemporalQueries.zoneId())?.let { zone ->
                            append('[')
                            append(zone)
                            append(']')
                        }
                    }
                }
            },
            parser = { text -> parseIsoDateTime(text) },
            description =
                "(ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))'T'" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]))" +
                    "[Offset(+HH:MM:ss,'Z')" +
                    "['['ParseCaseSensitive(true)ZoneRegionId()']']]",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoDateTime(
                    text,
                    offsetRequired = false,
                    regionAllowed = true,
                    resolverStyle = style,
                )
            },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a year and day-of-year with an optional offset. */
        public val ISO_ORDINAL_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(formatIsoYear(temporal.get(ChronoField.YEAR)))
                    append('-')
                    append(temporal.get(ChronoField.DAY_OF_YEAR).toString().padStart(3, '0'))
                    appendOptionalIsoOffset(temporal)
                }
            },
            parser = { text -> parseIsoOrdinalDate(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "Value(Year,4,10,EXCEEDS_PAD)'-'Value(DayOfYear,3)" +
                    "[Offset(+HH:MM:ss,'Z')]",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style -> parseIsoOrdinalDate(text, style) },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a week-based date with an optional offset. */
        public val ISO_WEEK_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(formatIsoYear(temporal.get(IsoFields.WEEK_BASED_YEAR)))
                    append("-W")
                    append(temporal.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0'))
                    append('-')
                    append(temporal.get(ChronoField.DAY_OF_WEEK))
                    appendOptionalIsoOffset(temporal)
                }
            },
            parser = { text -> parseIsoWeekDate(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "Value(WeekBasedYear,4,10,EXCEEDS_PAD)'-W'" +
                    "Value(WeekOfWeekBasedYear,2)'-'Value(DayOfWeek,1)" +
                    "[Offset(+HH:MM:ss,'Z')]",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style -> parseIsoWeekDate(text, style) },
            chronology = IsoChronology,
        )

        /** The strict basic ISO date formatter with an optional compact offset. */
        public val BASIC_ISO_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val year = temporal.get(ChronoField.YEAR)
                if (year !in 0..9_999) {
                    throw DateTimeException("Year cannot be printed as four digits: $year")
                }
                buildString {
                    append(year.toString().padStart(4, '0'))
                    append(temporal.get(ChronoField.MONTH_OF_YEAR).toString().padStart(2, '0'))
                    append(temporal.get(ChronoField.DAY_OF_MONTH).toString().padStart(2, '0'))
                    if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
                        append(formatBasicOffset(ZoneOffset.from(temporal)))
                    }
                }
            },
            parser = { text -> parseBasicIsoDate(text) },
            description =
                "ParseCaseSensitive(false)Value(Year,4)" +
                    "Value(MonthOfYear,2)Value(DayOfMonth,2)" +
                    "[ParseStrict(false)Offset(+HHMMss,'Z')ParseStrict(true)]",
            decimalStyleScope = DecimalStyleScope.BASIC_DATE,
            resolverParser = { text, style -> parseBasicIsoDate(text, style) },
            chronology = IsoChronology,
        )

        /** The English RFC 1123 date-time formatter. */
        public val RFC_1123_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> formatRfc1123(temporal) },
            parser = { text -> parseRfc1123(text) },
            description =
                "ParseCaseSensitive(false)ParseStrict(false)" +
                    "[Text(DayOfWeek)', ']" +
                    "Value(DayOfMonth,1,2,NOT_NEGATIVE)' '" +
                    "Text(MonthOfYear)' 'Value(Year,4)' '" +
                    "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)]' 'Offset(+HHMM,'GMT')",
            decimalStyleScope = DecimalStyleScope.BEFORE_RFC_OFFSET,
            resolverParser = { text, style -> parseRfc1123(text, style) },
            resolverStyle = ResolverStyle.SMART,
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for an instant in UTC. */
        public val ISO_INSTANT: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> Instant.from(temporal).toString() },
            parser = { text -> parseIsoInstant(text) },
            description = "ParseCaseSensitive(false)Instant()",
            decimalStyleScope = DecimalStyleScope.NONE,
        )

        /** The strict ISO formatter for a time with an offset. */
        public val ISO_OFFSET_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val offsetTime = OffsetTime.from(temporal)
                formatIsoLocalTime(offsetTime.time) + offsetTime.offset
            },
            parser = { text -> OffsetTime.parse(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]])" +
                    "Offset(+HH:MM:ss,'Z')",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoTime(text, offsetRequired = true, resolverStyle = style)
            },
        )

        /** The strict ISO formatter for a date-time with an offset. */
        public val ISO_OFFSET_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                formatIsoLocalDateTime(temporal) + ZoneOffset.from(temporal)
            },
            parser = { text -> OffsetDateTime.parse(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "(ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))'T'" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]))" +
                    "ParseStrict(false)Offset(+HH:MM:ss,'Z')ParseStrict(true)",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoDateTime(
                    text,
                    offsetRequired = true,
                    regionAllowed = false,
                    resolverStyle = style,
                )
            },
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for a date-time with an offset and optional region zone. */
        public val ISO_ZONED_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val offset = ZoneOffset.from(temporal)
                val zone = temporal.query(TemporalQueries.zoneId()) ?: offset
                buildString {
                    append(formatIsoLocalDateTime(temporal))
                    append(offset)
                    if (zone != offset) {
                        append('[')
                        append(zone)
                        append(']')
                    }
                }
            },
            parser = { text -> ZonedDateTime.parse(text) },
            description =
                "(ParseCaseSensitive(false)" +
                    "(ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))'T'" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]))" +
                    "ParseStrict(false)Offset(+HH:MM:ss,'Z')ParseStrict(true))" +
                    "['['ParseCaseSensitive(true)ZoneRegionId()']']",
            decimalStyleScope = DecimalStyleScope.BEFORE_ISO_OFFSET,
            resolverParser = { text, style ->
                parseIsoDateTime(
                    text,
                    offsetRequired = true,
                    regionAllowed = true,
                    resolverStyle = style,
                )
            },
            chronology = IsoChronology,
        )

        /** Returns a singleton query for the excess days produced while resolving. */
        public fun parsedExcessDays(): TemporalQuery<Period> = PARSED_EXCESS_DAYS

        /** Returns a singleton query indicating whether an instant contained a leap second. */
        public fun parsedLeapSecond(): TemporalQuery<Boolean> = PARSED_LEAP_SECOND

        private val PARSED_EXCESS_DAYS: TemporalQuery<Period> = TemporalQuery { temporal ->
            (temporal as? ParsedState)?.excessDays ?: Period.ZERO
        }

        private val PARSED_LEAP_SECOND: TemporalQuery<Boolean> = TemporalQuery { temporal ->
            (temporal as? ParsedState)?.leapSecond ?: false
        }
    }
}

internal sealed interface PatternToken {
    data class Literal(val text: String) : PatternToken

    data class Field(
        val symbol: Char,
        val count: Int,
    ) : PatternToken

    data class Value(
        val field: TemporalField,
        val minWidth: Int,
        val maxWidth: Int,
        val signStyle: SignStyle,
    ) : PatternToken
}

internal fun compilePattern(pattern: String): List<PatternToken> {
    val tokens = mutableListOf<PatternToken>()
    var index = 0
    while (index < pattern.length) {
        val character = pattern[index]
        when {
            character == '\'' -> {
                if (pattern.getOrNull(index + 1) == '\'') {
                    tokens.appendPatternLiteral("'")
                    index += 2
                    continue
                }
                val literal = StringBuilder()
                var closed = false
                index++
                while (index < pattern.length) {
                    if (pattern[index] == '\'') {
                        if (pattern.getOrNull(index + 1) == '\'') {
                            literal.append('\'')
                            index += 2
                        } else {
                            closed = true
                            index++
                            break
                        }
                    } else {
                        literal.append(pattern[index])
                        index++
                    }
                }
                require(closed) { "Pattern ends with an incomplete string literal: $pattern" }
                tokens.appendPatternLiteral(literal.toString())
            }
            character.isAsciiLetter() -> {
                var end = index + 1
                while (end < pattern.length && pattern[end] == character) end++
                val count = end - index
                validatePatternField(character, count)
                tokens += PatternToken.Field(character, count)
                index = end
            }
            else -> {
                tokens.appendPatternLiteral(character.toString())
                index++
            }
        }
    }
    return tokens
}

internal fun MutableList<PatternToken>.appendPatternLiteral(text: String) {
    if (text.isEmpty()) return
    val previous = lastOrNull()
    if (previous is PatternToken.Literal) {
        this[lastIndex] = PatternToken.Literal(previous.text + text)
    } else {
        add(PatternToken.Literal(text))
    }
}

private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

private fun validatePatternField(symbol: Char, count: Int) {
    when (symbol) {
        'u', 'y' -> require(count <= 19) { "The count of pattern letters must not exceed 19: $symbol" }
        'M', 'd', 'H', 'm', 's' -> require(count <= 2) {
            "Too many pattern letters: $symbol"
        }
        'S' -> require(count <= 9) { "Minimum width must be from 0 to 9 inclusive but was $count" }
        'X', 'x', 'Z' -> require(count <= 5) { "Too many pattern letters: $symbol" }
        'V' -> require(count == 2) { "Pattern letter count must be 2: V" }
        else -> throw IllegalArgumentException("Unknown pattern letter: $symbol")
    }
}

private fun formatPattern(
    tokens: List<PatternToken>,
    temporal: TemporalAccessor,
): String = buildString {
    tokens.forEach { token ->
        when (token) {
            is PatternToken.Literal -> append(token.text)
            is PatternToken.Field -> append(formatPatternField(token, temporal))
            is PatternToken.Value -> append(formatPatternValue(token, temporal))
        }
    }
}

private fun formatPatternValue(
    token: PatternToken.Value,
    temporal: TemporalAccessor,
): String {
    val value = temporal.getLong(token.field)
    val negative = value < 0
    val digits = value.toString().let { if (negative) it.drop(1) else it }
    if (digits.length > token.maxWidth) {
        throw DateTimeException(
            "Field ${token.field} cannot be printed as the value $value exceeds the maximum print width of ${token.maxWidth}",
        )
    }
    val prefix = when {
        negative && token.signStyle == SignStyle.NOT_NEGATIVE -> throw DateTimeException(
            "Field ${token.field} cannot be printed as the value $value cannot be negative according to the SignStyle",
        )
        negative && token.signStyle != SignStyle.NEVER -> "-"
        !negative && token.signStyle == SignStyle.ALWAYS -> "+"
        !negative && token.signStyle == SignStyle.EXCEEDS_PAD && digits.length > token.minWidth -> "+"
        else -> ""
    }
    return prefix + digits.padStart(token.minWidth, '0')
}

private fun formatPatternField(
    token: PatternToken.Field,
    temporal: TemporalAccessor,
): String = when (token.symbol) {
    'u' -> formatPatternYear(temporal.get(ChronoField.YEAR), token.count, signed = true)
    'y' -> formatPatternYear(temporal.get(ChronoField.YEAR_OF_ERA), token.count, signed = false)
    'M' -> formatPatternNumber(temporal.get(ChronoField.MONTH_OF_YEAR), token.count)
    'd' -> formatPatternNumber(temporal.get(ChronoField.DAY_OF_MONTH), token.count)
    'H' -> formatPatternNumber(temporal.get(ChronoField.HOUR_OF_DAY), token.count)
    'm' -> formatPatternNumber(temporal.get(ChronoField.MINUTE_OF_HOUR), token.count)
    's' -> formatPatternNumber(temporal.get(ChronoField.SECOND_OF_MINUTE), token.count)
    'S' -> temporal.get(ChronoField.NANO_OF_SECOND)
        .toString()
        .padStart(9, '0')
        .take(token.count)
    'X', 'x', 'Z' -> formatPatternOffset(
        offset = ZoneOffset.from(temporal),
        symbol = token.symbol,
        count = token.count,
    )
    'V' -> temporal.query(TemporalQueries.zoneId())?.id
        ?: throw DateTimeException("Unable to extract ZoneId from temporal $temporal")
    else -> error("Unsupported pattern field: ${token.symbol}")
}

private fun formatPatternOffset(
    offset: ZoneOffset,
    symbol: Char,
    count: Int,
): String {
    if (offset == ZoneOffset.UTC) {
        return when {
            symbol == 'X' -> "Z"
            symbol == 'Z' && count == 4 -> "GMT"
            symbol == 'Z' && count == 5 -> "Z"
            symbol == 'Z' -> "+0000"
            count == 1 -> "+00"
            count == 3 || count == 5 -> "+00:00"
            else -> "+0000"
        }
    }

    val totalSeconds = kotlin.math.abs(offset.totalSeconds)
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    val sign = if (offset.totalSeconds < 0) '-' else '+'
    val hourText = hours.toString().padStart(2, '0')
    val minuteText = minutes.toString().padStart(2, '0')
    val secondText = seconds.toString().padStart(2, '0')
    return when {
        symbol == 'Z' && count in 1..3 -> "$sign$hourText$minuteText"
        symbol == 'Z' && count == 4 -> "GMT$sign$hourText:$minuteText" +
            if (seconds == 0) "" else ":$secondText"
        count == 1 -> "$sign$hourText" + if (minutes == 0) "" else minuteText
        count == 2 -> "$sign$hourText$minuteText"
        count == 3 -> "$sign$hourText:$minuteText"
        count == 4 -> "$sign$hourText$minuteText" + if (seconds == 0) "" else secondText
        else -> "$sign$hourText:$minuteText" + if (seconds == 0) "" else ":$secondText"
    }
}

private fun formatPatternNumber(value: Int, count: Int): String =
    if (count == 1) value.toString() else value.toString().padStart(count, '0')

private fun formatPatternYear(
    value: Int,
    count: Int,
    signed: Boolean,
): String {
    val absolute = if (value < 0) -value.toLong() else value.toLong()
    if (count == 2) return (absolute % 100).toString().padStart(2, '0')
    if (count == 1) return if (signed) value.toString() else absolute.toString()

    val digits = absolute.toString().padStart(count, '0')
    return when {
        signed && value < 0 -> "-$digits"
        signed && count >= 4 && digits.length > count -> "+$digits"
        else -> digits
    }
}

private fun parsePattern(
    tokens: List<PatternToken>,
    text: String,
    resolverStyle: ResolverStyle,
): TemporalAccessor {
    val values = mutableMapOf<TemporalField, Long>()
    var offset: ZoneOffset? = null
    var zone: ZoneId? = null
    var index = 0
    tokens.forEachIndexed { tokenIndex, token ->
        when (token) {
            is PatternToken.Literal -> {
                if (!text.startsWith(token.text, index)) {
                    throw DateTimeParseException(
                        "Text could not be parsed at index $index",
                        text,
                        index,
                    )
                }
                index += token.text.length
            }
            is PatternToken.Field -> {
                when (token.symbol) {
                    'X', 'x', 'Z' -> {
                        val parsed = parsePatternOffset(token, text, index)
                        if (offset != null && offset != parsed.offset) {
                            throw DateTimeParseException("Conflict found for offset", text, index)
                        }
                        offset = parsed.offset
                        index = parsed.endIndex
                    }
                    'V' -> {
                        val parsed = parsePatternZone(tokens, tokenIndex, text, index)
                        if (zone != null && zone != parsed.zone) {
                            throw DateTimeParseException("Conflict found for zone", text, index)
                        }
                        zone = parsed.zone
                        index = parsed.endIndex
                    }
                    else -> {
                        val parsed = parsePatternField(token, text, index)
                        val field = token.symbol.toPatternField()
                        val previous = values.put(field, parsed.value)
                        if (previous != null && previous != parsed.value) {
                            throw DateTimeParseException(
                                "Conflict found for pattern field ${token.symbol}",
                                text,
                                index,
                            )
                        }
                        index = parsed.endIndex
                    }
                }
            }
            is PatternToken.Value -> {
                val parsed = parsePatternValue(tokens, tokenIndex, token, text, index)
                val previous = values.put(token.field, parsed.value)
                if (previous != null && previous != parsed.value) {
                    throw DateTimeParseException(
                        "Conflict found for field ${token.field}",
                        text,
                        index,
                    )
                }
                index = parsed.endIndex
            }
        }
    }
    if (index != text.length) {
        throw DateTimeParseException("Text could not be parsed, unparsed text found", text, index)
    }
    return resolvePatternValues(values, text, resolverStyle, offset, zone)
}

private data class ParsedPatternOffset(
    val offset: ZoneOffset,
    val endIndex: Int,
)

private fun parsePatternOffset(
    token: PatternToken.Field,
    text: String,
    startIndex: Int,
): ParsedPatternOffset {
    val firstCharacter = text.getOrNull(startIndex)
    if (firstCharacter == 'Z' || firstCharacter == 'z') {
        if (token.symbol == 'x' || token.symbol == 'Z' && token.count != 5) {
            throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
        }
        return ParsedPatternOffset(ZoneOffset.UTC, startIndex + 1)
    }
    if (token.symbol == 'Z' && token.count == 4) {
        if (!text.startsWith("GMT", startIndex)) {
            throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
        }
        val offsetStart = startIndex + 3
        if (offsetStart == text.length) return ParsedPatternOffset(ZoneOffset.UTC, offsetStart)
        val end = patternOffsetEnd(text, offsetStart, colon = true, optionalSeconds = true)
        return ParsedPatternOffset(parsePatternZoneOffset(text, offsetStart, end), end)
    }

    val colon = token.count == 3 || token.count == 5
    val optionalSeconds = token.count == 4 || token.count == 5
    val end = when {
        token.symbol == 'Z' && token.count in 1..3 -> startIndex + 5
        token.count == 1 -> {
            val minuteEnd = startIndex + 5
            if (minuteEnd <= text.length && text.substring(startIndex + 3, minuteEnd).all { it in '0'..'9' }) {
                minuteEnd
            } else {
                startIndex + 3
            }
        }
        else -> patternOffsetEnd(text, startIndex, colon, optionalSeconds)
    }
    if (end > text.length) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }
    return ParsedPatternOffset(parsePatternZoneOffset(text, startIndex, end), end)
}

private fun patternOffsetEnd(
    text: String,
    startIndex: Int,
    colon: Boolean,
    optionalSeconds: Boolean,
): Int {
    val minuteEnd = startIndex + if (colon) 6 else 5
    if (!optionalSeconds) return minuteEnd
    val secondEnd = minuteEnd + if (colon) 3 else 2
    return if (secondEnd <= text.length) secondEnd else minuteEnd
}

private fun parsePatternZoneOffset(
    text: String,
    startIndex: Int,
    endIndex: Int,
): ZoneOffset = try {
    ZoneOffset.of(text.substring(startIndex, endIndex))
} catch (exception: RuntimeException) {
    throw DateTimeParseException("Invalid offset", text, startIndex, exception)
}

private data class ParsedPatternZone(
    val zone: ZoneId,
    val endIndex: Int,
)

private fun parsePatternZone(
    tokens: List<PatternToken>,
    tokenIndex: Int,
    text: String,
    startIndex: Int,
): ParsedPatternZone {
    val nextLiteral = tokens.drop(tokenIndex + 1)
        .filterIsInstance<PatternToken.Literal>()
        .firstOrNull { it.text.isNotEmpty() }
    val endIndex = nextLiteral?.let { literal -> text.indexOf(literal.text, startIndex) }
        ?.takeIf { it >= startIndex }
        ?: text.length
    if (endIndex == startIndex) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }
    val zone = try {
        ZoneId.of(text.substring(startIndex, endIndex))
    } catch (exception: RuntimeException) {
        throw DateTimeParseException("Invalid zone", text, startIndex, exception)
    }
    return ParsedPatternZone(zone, endIndex)
}

private data class ParsedPatternField(
    val value: Long,
    val endIndex: Int,
)

private fun parsePatternValue(
    tokens: List<PatternToken>,
    tokenIndex: Int,
    token: PatternToken.Value,
    text: String,
    startIndex: Int,
): ParsedPatternField {
    var index = startIndex
    val sign = text.getOrNull(index).takeIf { it == '+' || it == '-' }
    val signAllowed = when (sign) {
        '+' -> token.signStyle == SignStyle.ALWAYS || token.signStyle == SignStyle.EXCEEDS_PAD
        '-' -> token.signStyle in listOf(SignStyle.NORMAL, SignStyle.ALWAYS, SignStyle.EXCEEDS_PAD)
        else -> token.signStyle != SignStyle.ALWAYS
    }
    if (!signAllowed) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }
    if (sign != null) index++

    val digitsStart = index
    var digitRunEnd = digitsStart
    while (digitRunEnd < text.length && text[digitRunEnd] in '0'..'9') digitRunEnd++
    val reservedWidth = tokens.drop(tokenIndex + 1)
        .map { it.adjacentFixedNumericWidth() }
        .takeWhile { it != null }
        .sumOf { it ?: 0 }
    val maximumDigits = minOf(token.maxWidth, digitRunEnd - digitsStart - reservedWidth)
    while (index < digitRunEnd && index - digitsStart < maximumDigits) {
        index++
    }
    val digitCount = index - digitsStart
    if (digitCount < token.minWidth ||
        token.signStyle == SignStyle.EXCEEDS_PAD &&
        ((sign == '+' && digitCount <= token.minWidth) || (sign == null && digitCount > token.minWidth))
    ) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }

    val unsigned = text.substring(digitsStart, index)
    val numericText = if (sign == '-') "-$unsigned" else unsigned
    val value = numericText.toLongOrNull()
        ?: throw DateTimeParseException("Invalid numeric value", text, startIndex)
    return ParsedPatternField(value, index)
}

private fun PatternToken.adjacentFixedNumericWidth(): Int? = when (this) {
    is PatternToken.Value -> minWidth.takeIf {
        minWidth == maxWidth && signStyle == SignStyle.NOT_NEGATIVE
    }
    is PatternToken.Field -> count.takeIf {
        symbol == 'S' || symbol in listOf('M', 'd', 'H', 'm', 's') && count > 1
    }
    is PatternToken.Literal -> null
}

private fun parsePatternField(
    token: PatternToken.Field,
    text: String,
    startIndex: Int,
): ParsedPatternField {
    var index = startIndex
    var negative = false
    val sign = text.getOrNull(index)
    if (token.symbol == 'u' && token.count != 2 && (sign == '+' || sign == '-')) {
        negative = sign == '-'
        index++
    }

    val fixedWidth = token.symbol == 'S' ||
        token.count > 1 && !(token.symbol == 'u' && token.count >= 3 && index > startIndex)
    val minimumDigits = if (fixedWidth) token.count else 1
    val maximumDigits = when {
        fixedWidth -> token.count
        token.symbol in listOf('u', 'y') -> 19
        else -> 2
    }
    val digitsStart = index
    while (index < text.length && text[index] in '0'..'9' && index - digitsStart < maximumDigits) {
        index++
    }
    val digitCount = index - digitsStart
    if (digitCount < minimumDigits) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }

    var value = text.substring(digitsStart, index).toLongOrNull()
        ?: throw DateTimeParseException("Invalid numeric value", text, startIndex)
    if (token.count == 2 && token.symbol in listOf('u', 'y')) value += 2_000
    if (token.symbol == 'S') value *= POWERS_OF_TEN[9 - token.count]
    if (negative) value = -value
    return ParsedPatternField(value, index)
}

private fun resolvePatternValues(
    values: Map<TemporalField, Long>,
    text: String,
    resolverStyle: ResolverStyle,
    offset: ZoneOffset?,
    zone: ZoneId?,
): TemporalAccessor {
    val year = values[ChronoField.YEAR] ?: values[ChronoField.YEAR_OF_ERA]
    val month = values[ChronoField.MONTH_OF_YEAR]
    val day = values[ChronoField.DAY_OF_MONTH]
    val date = if (year != null && month != null && day != null) {
        resolveIsoDateFields(
            year = year.toPatternInt('u', text),
            month = month.toPatternInt('M', text),
            day = day.toPatternInt('d', text),
            resolverStyle = resolverStyle,
            input = text,
            target = "date",
        )
    } else {
        null
    }

    val hour = values[ChronoField.HOUR_OF_DAY]
    val minute = values[ChronoField.MINUTE_OF_HOUR]
    val second = values[ChronoField.SECOND_OF_MINUTE]
    val fraction = values[ChronoField.NANO_OF_SECOND]
    val resolvedTime = if (hour != null && minute != null) {
        resolvePatternTime(
            hour = hour.toPatternInt('H', text),
            minute = minute.toPatternInt('m', text),
            second = (second ?: 0).toPatternInt('s', text),
            nano = (fraction ?: 0).toPatternInt('S', text),
            resolverStyle = resolverStyle,
            text = text,
        )
    } else {
        null
    }

    val resolvedDate = if (date != null && resolvedTime != null) {
        date.plusDays(resolvedTime.excessDays.days.toLong())
    } else {
        date
    }
    return ParsedTemporalAccessor(
        date = resolvedDate,
        time = resolvedTime?.time,
        offset = offset,
        zone = zone,
        fields = values,
        excessDays = if (date == null) resolvedTime?.excessDays ?: Period.ZERO else Period.ZERO,
    )
}

private fun Char.toPatternField(): TemporalField = when (this) {
    'u' -> ChronoField.YEAR
    'y' -> ChronoField.YEAR_OF_ERA
    'M' -> ChronoField.MONTH_OF_YEAR
    'd' -> ChronoField.DAY_OF_MONTH
    'H' -> ChronoField.HOUR_OF_DAY
    'm' -> ChronoField.MINUTE_OF_HOUR
    's' -> ChronoField.SECOND_OF_MINUTE
    'S' -> ChronoField.NANO_OF_SECOND
    else -> error("Unsupported numeric pattern field: $this")
}

private fun Long.toPatternInt(field: Char, text: String): Int =
    toInt().takeIf { it.toLong() == this }
        ?: throw DateTimeParseException("Invalid value for pattern field $field: $this", text, 0)

private val POWERS_OF_TEN: IntArray = intArrayOf(
    1,
    10,
    100,
    1_000,
    10_000,
    100_000,
    1_000_000,
    10_000_000,
    100_000_000,
    1_000_000_000,
)

private fun resolvePatternTime(
    hour: Int,
    minute: Int,
    second: Int,
    nano: Int,
    resolverStyle: ResolverStyle,
    text: String,
): ResolvedTime = try {
    when (resolverStyle) {
        ResolverStyle.STRICT -> ResolvedTime(LocalTime.of(hour, minute, second, nano), Period.ZERO)
        ResolverStyle.SMART -> if (hour == 24 && minute == 0 && second == 0 && nano == 0) {
            ResolvedTime(LocalTime.MIDNIGHT, Period.ofDays(1))
        } else {
            ResolvedTime(LocalTime.of(hour, minute, second, nano), Period.ZERO)
        }
        ResolverStyle.LENIENT -> {
            val totalNanos = (hour * 3_600L + minute * 60L + second) * 1_000_000_000L + nano
            ResolvedTime(
                time = LocalTime.ofNanoOfDay(totalNanos % 86_400_000_000_000L),
                excessDays = Period.ofDays((totalNanos / 86_400_000_000_000L).toInt()),
            )
        }
    }
} catch (exception: RuntimeException) {
    throw DateTimeParseException("Text cannot be parsed to a time", text, 0, exception)
}

private fun describePattern(tokens: List<PatternToken>): String = buildString {
    tokens.forEach { token ->
        when (token) {
            is PatternToken.Literal -> append('\'').append(token.text).append('\'')
            is PatternToken.Field -> append("Value(").append(token.symbol).append(',').append(token.count).append(')')
            is PatternToken.Value -> append("Value(")
                .append(token.field)
                .append(',')
                .append(token.minWidth)
                .append(',')
                .append(token.maxWidth)
                .append(',')
                .append(token.signStyle)
                .append(')')
        }
    }
}

private enum class DecimalStyleScope {
    ALL,
    BEFORE_ISO_OFFSET,
    BASIC_DATE,
    BEFORE_RFC_OFFSET,
    NONE,
    ;

    fun localize(text: String, style: DecimalStyle): String =
        convert(text, style, toLocalized = true)

    fun standardize(text: CharSequence, style: DecimalStyle): String =
        convert(text.toString(), style, toLocalized = false)

    private fun convert(
        text: String,
        style: DecimalStyle,
        toLocalized: Boolean,
    ): String {
        if (style == DecimalStyle.STANDARD || this == NONE) return text
        val sectionEnd = sectionEnd(text)
        return buildString(text.length) {
            text.forEachIndexed { index, character ->
                append(
                    if (index >= sectionEnd) {
                        character
                    } else if (toLocalized) {
                        character.toLocalized(index, style)
                    } else {
                        character.toStandard(index, style)
                    },
                )
            }
        }
    }

    private fun sectionEnd(text: String): Int = when (this) {
        ALL -> text.length
        BEFORE_ISO_OFFSET -> {
            val mainEnd = text.indexOf('[').takeIf { it >= 0 } ?: text.length
            isoOffsetStart(text.substring(0, mainEnd)) ?: mainEnd
        }
        BASIC_DATE -> minOf(8, text.length)
        BEFORE_RFC_OFFSET -> text.lastIndexOf(' ').takeIf { it >= 0 } ?: text.length
        NONE -> 0
    }

    private fun Char.toLocalized(index: Int, style: DecimalStyle): Char = when {
        index == 0 && this == '+' -> style.positiveSign
        index == 0 && this == '-' -> style.negativeSign
        this in '0'..'9' -> (style.zeroDigit.code + (this - '0')).toChar()
        this == '.' -> style.decimalSeparator
        else -> this
    }

    private fun Char.toStandard(index: Int, style: DecimalStyle): Char = when {
        index == 0 && this == style.positiveSign -> '+'
        index == 0 && this == style.negativeSign -> '-'
        this == style.decimalSeparator -> '.'
        else -> style.convertToDigit(this).takeIf { it >= 0 }?.digitToChar() ?: this
    }
}

private fun formatIsoDateFields(temporal: TemporalAccessor): String = buildString {
    append(formatIsoYear(temporal.get(ChronoField.YEAR)))
    append('-')
    append(temporal.get(ChronoField.MONTH_OF_YEAR).toString().padStart(2, '0'))
    append('-')
    append(temporal.get(ChronoField.DAY_OF_MONTH).toString().padStart(2, '0'))
}

private fun formatIsoLocalDateTime(temporal: TemporalAccessor): String =
    "${formatIsoDateFields(temporal)}T${formatIsoLocalTime(LocalTime.from(temporal))}"

private fun formatIsoYear(year: Int): String = when {
    year in 0..999 -> year.toString().padStart(4, '0')
    year in -999..-1 -> "-" + (-year).toString().padStart(4, '0')
    year > 9_999 -> "+$year"
    else -> year.toString()
}

private fun StringBuilder.appendOptionalIsoOffset(temporal: TemporalAccessor) {
    if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
        append(ZoneOffset.from(temporal))
    }
}

private fun formatBasicOffset(offset: ZoneOffset): String {
    if (offset == ZoneOffset.UTC) return "Z"
    val totalSeconds = kotlin.math.abs(offset.totalSeconds)
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    return buildString {
        append(if (offset.totalSeconds < 0) '-' else '+')
        append(hours.toString().padStart(2, '0'))
        append(minutes.toString().padStart(2, '0'))
        if (seconds != 0) append(seconds.toString().padStart(2, '0'))
    }
}

private fun formatRfc1123(temporal: TemporalAccessor): String {
    val time = LocalTime.from(temporal)
    val offset = ZoneOffset.from(temporal)
    val year = temporal.get(ChronoField.YEAR)
    if (year !in 0..9_999) {
        throw DateTimeException("Year cannot be printed as four digits: $year")
    }
    if (offset.totalSeconds % 60 != 0) {
        throw DateTimeException("Offset seconds cannot be printed by RFC 1123: $offset")
    }
    return buildString {
        append(RFC_DAY_NAMES[temporal.get(ChronoField.DAY_OF_WEEK) - 1])
        append(", ")
        append(temporal.get(ChronoField.DAY_OF_MONTH))
        append(' ')
        append(RFC_MONTH_NAMES[temporal.get(ChronoField.MONTH_OF_YEAR) - 1])
        append(' ')
        append(year.toString().padStart(4, '0'))
        append(' ')
        append(time.hour.toString().padStart(2, '0'))
        append(':')
        append(time.minute.toString().padStart(2, '0'))
        append(':')
        append(time.second.toString().padStart(2, '0'))
        append(' ')
        append(formatRfc1123Offset(offset))
    }
}

private fun formatRfc1123Offset(offset: ZoneOffset): String {
    if (offset == ZoneOffset.UTC) return "GMT"
    val totalMinutes = kotlin.math.abs(offset.totalSeconds) / 60
    return buildString {
        append(if (offset.totalSeconds < 0) '-' else '+')
        append((totalMinutes / 60).toString().padStart(2, '0'))
        append((totalMinutes % 60).toString().padStart(2, '0'))
    }
}

private fun parseIsoInstant(text: CharSequence): TemporalAccessor {
    val input = text.toString()
    val instant = Instant.parse(input)
    val timeSeparator = input.indexOfFirst { it == 'T' || it == 't' }
    val leapSecond =
        input.getOrNull(timeSeparator + 7) == '6' &&
            input.getOrNull(timeSeparator + 8) == '0'
    return ParsedTemporalAccessor(
        instant = instant,
        leapSecond = leapSecond,
    )
}

private fun parseResolvedIsoDate(
    input: String,
    resolverStyle: ResolverStyle,
    target: String,
): LocalDate {
    val daySeparator = input.lastIndexOf('-')
    val monthSeparator = input.lastIndexOf('-', daySeparator - 1)
    if (
        monthSeparator <= 0 ||
        daySeparator != monthSeparator + 3 ||
        input.length != daySeparator + 3
    ) {
        throw DateTimeParseException("Text cannot be parsed to an $target", input, 0)
    }
    val year = parseIsoYear(input.substring(0, monthSeparator), input, target)
    val month = parseFixedDigits(input, monthSeparator + 1, 2, input, target)
    val day = parseFixedDigits(input, daySeparator + 1, 2, input, target)

    return resolveIsoDateFields(year, month, day, resolverStyle, input, target)
}

private fun resolveIsoDateFields(
    year: Int,
    month: Int,
    day: Int,
    resolverStyle: ResolverStyle,
    input: String,
    target: String,
): LocalDate = try {
        when (resolverStyle) {
            ResolverStyle.STRICT -> LocalDate.of(year, month, day)
            ResolverStyle.SMART -> {
                ChronoField.MONTH_OF_YEAR.checkValidValue(month.toLong())
                ChronoField.DAY_OF_MONTH.checkValidValue(day.toLong())
                val firstOfMonth = LocalDate.of(year, month, 1)
                LocalDate.of(year, month, minOf(day, firstOfMonth.lengthOfMonth()))
            }
            ResolverStyle.LENIENT -> LocalDate.of(year, 1, 1)
                .plusMonths(month.toLong() - 1)
                .plusDays(day.toLong() - 1)
        }
    } catch (exception: RuntimeException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an $target",
            input,
            0,
            exception,
        )
    }

private data class ResolvedTime(
    val time: LocalTime,
    val excessDays: Period,
)

private fun parseResolvedIsoTimeAccessor(
    input: String,
    resolverStyle: ResolverStyle,
): TemporalAccessor {
    val resolved = parseResolvedIsoTime(input, resolverStyle, "ISO local time")
    return ParsedTemporalAccessor(
        time = resolved.time,
        excessDays = resolved.excessDays,
    )
}

private fun parseResolvedIsoTime(
    input: String,
    resolverStyle: ResolverStyle,
    target: String,
): ResolvedTime {
    if (
        input.length < 5 ||
        input[2] != ':' ||
        input.getOrNull(5)?.let { it != ':' } == true
    ) {
        throw DateTimeParseException("Text cannot be parsed to an $target", input, 0)
    }
    val hour = parseFixedDigits(input, 0, 2, input, target)
    val minute = parseFixedDigits(input, 3, 2, input, target)
    var second = 0
    var nano = 0
    if (input.length > 5) {
        if (input.length < 8) {
            throw DateTimeParseException("Text cannot be parsed to an $target", input, 5)
        }
        second = parseFixedDigits(input, 6, 2, input, target)
        if (input.length > 8) {
            if (input[8] != '.') {
                throw DateTimeParseException("Text cannot be parsed to an $target", input, 8)
            }
            val fractionLength = input.length - 9
            if (fractionLength > 9) {
                throw DateTimeParseException("Text cannot be parsed to an $target", input, 18)
            }
            var index = 9
            while (index < input.length) {
                val digit = input[index]
                if (digit !in '0'..'9') {
                    throw DateTimeParseException("Text cannot be parsed to an $target", input, index)
                }
                nano = nano * 10 + (digit - '0')
                index++
            }
            repeat(9 - fractionLength) { nano *= 10 }
        }
    }

    return try {
        when (resolverStyle) {
            ResolverStyle.STRICT -> ResolvedTime(
                LocalTime.of(hour, minute, second, nano),
                Period.ZERO,
            )
            ResolverStyle.SMART -> if (hour == 24 && minute == 0 && second == 0 && nano == 0) {
                ResolvedTime(LocalTime.MIDNIGHT, Period.ofDays(1))
            } else {
                ResolvedTime(LocalTime.of(hour, minute, second, nano), Period.ZERO)
            }
            ResolverStyle.LENIENT -> {
                val totalNanos =
                    (hour * 3_600L + minute * 60L + second) * 1_000_000_000L + nano
                val excessDays = totalNanos / 86_400_000_000_000L
                val nanoOfDay = totalNanos % 86_400_000_000_000L
                ResolvedTime(
                    LocalTime.ofNanoOfDay(nanoOfDay),
                    Period.ofDays(excessDays.toInt()),
                )
            }
        }
    } catch (exception: RuntimeException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an $target",
            input,
            0,
            exception,
        )
    }
}

private fun parseResolvedIsoDateTime(
    input: String,
    resolverStyle: ResolverStyle,
): LocalDateTime {
    val separator = input.indexOfFirst { it == 'T' || it == 't' }
    if (separator < 0) {
        throw DateTimeParseException("Text cannot be parsed to an ISO local date-time", input, 0)
    }
    val date = parseResolvedIsoDate(
        input.substring(0, separator),
        resolverStyle,
        "ISO local date-time",
    )
    val resolvedTime = parseResolvedIsoTime(
        input.substring(separator + 1),
        resolverStyle,
        "ISO local date-time",
    )
    return LocalDateTime.of(
        date.plusDays(resolvedTime.excessDays.days.toLong()),
        resolvedTime.time,
    )
}

private fun parseIsoDate(
    text: CharSequence,
    offsetRequired: Boolean,
    resolverStyle: ResolverStyle = ResolverStyle.STRICT,
): TemporalAccessor {
    val input = text.toString()
    val offsetStart = isoOffsetStart(input)
    if (offsetRequired && offsetStart == null) {
        throw DateTimeParseException("Text cannot be parsed to an ISO date", input, input.length)
    }
    val dateEnd = offsetStart ?: input.length
    val date = parseResolvedIsoDate(
        input.substring(0, dateEnd),
        resolverStyle,
        "ISO date",
    )
    val offset = offsetStart?.let { index ->
        val offsetText = input.substring(index)
        try {
            ZoneOffset.of(if (offsetText.equals("z", ignoreCase = true)) "Z" else offsetText)
        } catch (exception: RuntimeException) {
            throw DateTimeParseException(
                "Text cannot be parsed to an ISO date",
                input,
                index,
                exception,
            )
        }
    }
    return ParsedTemporalAccessor(date = date, offset = offset)
}

private fun parseIsoTime(
    text: CharSequence,
    offsetRequired: Boolean = false,
    resolverStyle: ResolverStyle = ResolverStyle.STRICT,
): TemporalAccessor {
    val input = text.toString()
    val offsetStart = isoOffsetStart(input)
    if (offsetRequired && offsetStart == null) {
        throw DateTimeParseException("Text cannot be parsed to an ISO time", input, input.length)
    }
    val resolvedTime = parseResolvedIsoTime(
        input.substring(0, offsetStart ?: input.length),
        resolverStyle,
        "ISO time",
    )
    val offset = offsetStart?.let { index -> parseIsoOffset(input, index, "ISO time") }
    return ParsedTemporalAccessor(
        time = resolvedTime.time,
        offset = offset,
        excessDays = resolvedTime.excessDays,
    )
}

private fun parseIsoDateTime(
    text: CharSequence,
    offsetRequired: Boolean = false,
    regionAllowed: Boolean = true,
    resolverStyle: ResolverStyle = ResolverStyle.STRICT,
): TemporalAccessor {
    val input = text.toString()
    val bracketStart = input.lastIndexOf('[')
    val hasBracket = bracketStart >= 0 || ']' in input
    if (hasBracket && !regionAllowed) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO date-time",
            input,
            maxOf(bracketStart, 0),
        )
    }
    val zone = if (hasBracket) {
        if (bracketStart < 0 || !input.endsWith(']') || bracketStart == input.lastIndex) {
            throw DateTimeParseException(
                "Text cannot be parsed to an ISO date-time",
                input,
                maxOf(bracketStart, 0),
            )
        }
        val zoneText = input.substring(bracketStart + 1, input.lastIndex)
        try {
            ZoneId.of(zoneText).also { parsedZone ->
                if (parsedZone is ZoneOffset) {
                    throw DateTimeParseException(
                        "Text cannot be parsed to an ISO date-time",
                        input,
                        bracketStart + 1,
                    )
                }
            }
        } catch (exception: DateTimeParseException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw DateTimeParseException(
                "Text cannot be parsed to an ISO date-time",
                input,
                bracketStart + 1,
                exception,
            )
        }
    } else {
        null
    }
    val mainEnd = if (hasBracket) bracketStart else input.length
    val mainText = input.substring(0, mainEnd)
    val offsetStart = isoOffsetStart(mainText)
    if ((zone != null || offsetRequired) && offsetStart == null) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO date-time",
            input,
            if (zone != null) bracketStart else mainEnd,
        )
    }
    val dateTime = parseResolvedIsoDateTime(
        mainText.substring(0, offsetStart ?: mainText.length),
        resolverStyle,
    )
    val offset = offsetStart?.let { index -> parseIsoOffset(mainText, index, "ISO date-time") }
    return ParsedTemporalAccessor(
        date = dateTime.date,
        time = dateTime.time,
        offset = offset,
        zone = zone,
    )
}

private fun parseIsoOrdinalDate(
    text: CharSequence,
    resolverStyle: ResolverStyle = ResolverStyle.STRICT,
): TemporalAccessor {
    val input = text.toString()
    val offsetStart = isoOffsetStart(input)
    val mainText = input.substring(0, offsetStart ?: input.length)
    val separator = mainText.lastIndexOf('-')
    if (separator <= 0 || mainText.length - separator != 4) {
        throw DateTimeParseException("Text cannot be parsed to an ISO ordinal date", input, 0)
    }
    val year = parseIsoYear(mainText.substring(0, separator), input, "ISO ordinal date")
    val dayOfYear = parseFixedDigits(
        mainText,
        separator + 1,
        3,
        input,
        "ISO ordinal date",
    )
    val date = try {
        if (resolverStyle == ResolverStyle.LENIENT) {
            LocalDate.of(year, 1, 1).plusDays(dayOfYear.toLong() - 1)
        } else {
            LocalDate.ofYearDay(year, dayOfYear)
        }
    } catch (exception: RuntimeException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO ordinal date",
            input,
            0,
            exception,
        )
    }
    val offset = offsetStart?.let { index -> parseIsoOffset(input, index, "ISO ordinal date") }
    return ParsedTemporalAccessor(date = date, offset = offset)
}

private fun parseIsoWeekDate(
    text: CharSequence,
    resolverStyle: ResolverStyle = ResolverStyle.STRICT,
): TemporalAccessor {
    val input = text.toString()
    val offsetStart = isoOffsetStart(input)
    val mainText = input.substring(0, offsetStart ?: input.length)
    val marker = (1..<mainText.length - 1).firstOrNull { index ->
        mainText[index] == '-' && mainText[index + 1].equals('W', ignoreCase = true)
    } ?: throw DateTimeParseException("Text cannot be parsed to an ISO week date", input, 0)
    if (mainText.length != marker + 6 || mainText[marker + 4] != '-') {
        throw DateTimeParseException("Text cannot be parsed to an ISO week date", input, marker)
    }
    val weekBasedYear = parseIsoYear(
        mainText.substring(0, marker),
        input,
        "ISO week date",
    )
    val week = parseFixedDigits(mainText, marker + 2, 2, input, "ISO week date")
    val dayOfWeek = parseFixedDigits(mainText, marker + 5, 1, input, "ISO week date")
    val date = try {
        var base = LocalDate.of(weekBasedYear, 1, 4)
        var normalizedDay = dayOfWeek
        if (resolverStyle == ResolverStyle.LENIENT) {
            if (normalizedDay > 7) {
                base = base.plusWeeks(((normalizedDay - 1) / 7).toLong())
                normalizedDay = (normalizedDay - 1) % 7 + 1
            } else if (normalizedDay < 1) {
                base = base.plusWeeks(((normalizedDay - 7) / 7).toLong())
                normalizedDay = (normalizedDay + 6) % 7 + 1
            }
        } else {
            if (normalizedDay !in 1..7) throw DateTimeException("Invalid ISO week date")
            if (week !in 1..53) throw DateTimeException("Invalid ISO week date")
        }
        base.plusWeeks((week - 1).toLong())
            .plusDays((normalizedDay - base.dayOfWeek.value).toLong())
            .also {
                if (
                    resolverStyle == ResolverStyle.STRICT &&
                    (
                        it.get(IsoFields.WEEK_BASED_YEAR) != weekBasedYear ||
                            it.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != week
                    )
                ) {
                    throw DateTimeException("Invalid ISO week date")
                }
            }
    } catch (exception: RuntimeException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO week date",
            input,
            0,
            exception,
        )
    }
    val offset = offsetStart?.let { index -> parseIsoOffset(input, index, "ISO week date") }
    return ParsedTemporalAccessor(date = date, offset = offset)
}

private fun parseBasicIsoDate(
    text: CharSequence,
    resolverStyle: ResolverStyle = ResolverStyle.STRICT,
): TemporalAccessor {
    val input = text.toString()
    if (input.length < 8) {
        throw DateTimeParseException("Text cannot be parsed to a basic ISO date", input, input.length)
    }
    val year = parseFixedDigits(input, 0, 4, input, "basic ISO date")
    val month = parseFixedDigits(input, 4, 2, input, "basic ISO date")
    val day = parseFixedDigits(input, 6, 2, input, "basic ISO date")
    val date = resolveIsoDateFields(
        year,
        month,
        day,
        resolverStyle,
        input,
        "basic ISO date",
    )
    val offset = if (input.length == 8) {
        null
    } else {
        val offsetText = input.substring(8)
        val validOffset = offsetText.equals("Z", ignoreCase = true) ||
            offsetText.length in listOf(3, 5, 7) &&
            offsetText[0] in "+-" &&
            offsetText.drop(1).all { it in '0'..'9' }
        if (!validOffset) {
            throw DateTimeParseException("Text cannot be parsed to a basic ISO date", input, 8)
        }
        try {
            ZoneOffset.of(if (offsetText.equals("z", ignoreCase = true)) "Z" else offsetText)
        } catch (exception: RuntimeException) {
            throw DateTimeParseException(
                "Text cannot be parsed to a basic ISO date",
                input,
                8,
                exception,
            )
        }
    }
    return ParsedTemporalAccessor(date = date, offset = offset)
}

private fun parseRfc1123(
    text: CharSequence,
    resolverStyle: ResolverStyle = ResolverStyle.SMART,
): TemporalAccessor {
    val input = text.toString()
    var remaining = input
    val weekday = if (input.length >= 5 && input[3] == ',' && input[4] == ' ') {
        val dayName = input.substring(0, 3)
        val dayIndex = RFC_DAY_NAMES.indexOfFirst { it.equals(dayName, ignoreCase = true) }
        if (dayIndex < 0) throw rfcParseFailure(input, 0)
        remaining = input.substring(5)
        dayIndex + 1
    } else {
        null
    }

    val parts = remaining.split(' ')
    if (parts.size != 5 || parts.any(String::isEmpty)) throw rfcParseFailure(input, 0)
    val dayText = parts[0]
    if (dayText.length !in 1..2 || dayText.any { it !in '0'..'9' }) {
        throw rfcParseFailure(input, 0)
    }
    val month = RFC_MONTH_NAMES.indexOfFirst { it.equals(parts[1], ignoreCase = true) } + 1
    if (month == 0) throw rfcParseFailure(input, input.indexOf(parts[1]))
    val yearText = parts[2]
    if (yearText.length != 4 || yearText.any { it !in '0'..'9' }) {
        throw rfcParseFailure(input, input.indexOf(yearText))
    }
    val timeParts = parts[3].split(':')
    if (
        timeParts.size !in 2..3 ||
        timeParts.any { component -> component.length != 2 || component.any { it !in '0'..'9' } }
    ) {
        throw rfcParseFailure(input, input.indexOf(parts[3]))
    }

    var date = resolveIsoDateFields(
        yearText.toInt(),
        month,
        dayText.toInt(),
        resolverStyle,
        input,
        "RFC 1123 date-time",
    )
    if (weekday != null && date.dayOfWeek.value != weekday) {
        throw rfcParseFailure(input, 0)
    }
    val resolvedTime = parseResolvedIsoTime(
        parts[3],
        resolverStyle,
        "RFC 1123 date-time",
    )
    date = date.plusDays(resolvedTime.excessDays.days.toLong())
    val offsetText = parts[4]
    val validOffset = offsetText.equals("GMT", ignoreCase = true) ||
        offsetText.length in listOf(3, 5) &&
        offsetText[0] in "+-" &&
        offsetText.drop(1).all { it in '0'..'9' }
    if (!validOffset) throw rfcParseFailure(input, input.lastIndexOf(offsetText))
    val offset = try {
        ZoneOffset.of(if (offsetText.equals("GMT", ignoreCase = true)) "Z" else offsetText)
    } catch (exception: RuntimeException) {
        throw rfcParseFailure(input, input.lastIndexOf(offsetText), exception)
    }
    return ParsedTemporalAccessor(date = date, time = resolvedTime.time, offset = offset)
}

private fun rfcParseFailure(
    input: String,
    errorIndex: Int,
    cause: Throwable? = null,
): DateTimeParseException = DateTimeParseException(
    "Text cannot be parsed to an RFC 1123 date-time",
    input,
    errorIndex,
    cause,
)

private fun parseIsoYear(text: String, input: String, target: String): Int = try {
    LocalDate.parse("$text-01-01").year
} catch (exception: RuntimeException) {
    throw DateTimeParseException("Text cannot be parsed to an $target", input, 0, exception)
}

private fun parseFixedDigits(
    text: String,
    start: Int,
    length: Int,
    input: String,
    target: String,
): Int {
    val end = start + length
    if (start < 0 || end > text.length || (start..<end).any { text[it] !in '0'..'9' }) {
        throw DateTimeParseException("Text cannot be parsed to an $target", input, start.coerceAtLeast(0))
    }
    var value = 0
    for (index in start..<end) value = value * 10 + (text[index] - '0')
    return value
}

private fun parseIsoOffset(input: String, offsetStart: Int, target: String): ZoneOffset {
    val offsetText = input.substring(offsetStart)
    return try {
        ZoneOffset.of(if (offsetText.equals("z", ignoreCase = true)) "Z" else offsetText)
    } catch (exception: RuntimeException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an $target",
            input,
            offsetStart,
            exception,
        )
    }
}

private fun isoOffsetStart(input: String): Int? {
    if (input.endsWith('Z', ignoreCase = true)) return input.lastIndex
    val offsetWithSeconds = input.length - 9
    if (
        offsetWithSeconds > 0 &&
        input[offsetWithSeconds] in "+-" &&
        input[offsetWithSeconds + 3] == ':' &&
        input[offsetWithSeconds + 6] == ':'
    ) {
        return offsetWithSeconds
    }
    val offsetWithoutSeconds = input.length - 6
    if (
        offsetWithoutSeconds > 0 &&
        input[offsetWithoutSeconds] in "+-" &&
        input[offsetWithoutSeconds + 3] == ':'
    ) {
        return offsetWithoutSeconds
    }
    return null
}

private fun formatIsoLocalTime(time: LocalTime): String = buildString {
    append(time.hour.toString().padStart(2, '0'))
    append(':')
    append(time.minute.toString().padStart(2, '0'))
    append(':')
    append(time.second.toString().padStart(2, '0'))
    if (time.nano != 0) {
        append('.')
        append(time.nano.toString().padStart(9, '0').trimEnd('0'))
    }
}

private interface ParsedState {
    val excessDays: Period
    val leapSecond: Boolean
}

private class FormatterOverrideTemporalAccessor(
    private val delegate: TemporalAccessor,
    private val date: ChronoLocalDate? = null,
    private val chronology: Chronology? = null,
    private val zone: ZoneId? = null,
) : TemporalAccessor {
    override fun isSupported(field: TemporalField): Boolean =
        if (date != null && field.isDateBased) date.isSupported(field) else delegate.isSupported(field)

    override fun range(field: TemporalField): ValueRange =
        if (date != null && field.isDateBased) date.range(field) else delegate.range(field)

    override fun getLong(field: TemporalField): Long =
        if (date != null && field.isDateBased) date.getLong(field) else delegate.getLong(field)

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any? = when (query) {
            TemporalQueries.chronology() -> chronology
            TemporalQueries.zoneId() -> zone
            TemporalQueries.precision() -> return delegate.query(query)
            else -> return super<TemporalAccessor>.query(query)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    override fun toString(): String = buildString {
        append(delegate)
        chronology?.let { append(" with chronology ").append(it) }
        zone?.let { append(" with zone ").append(it) }
    }
}

private fun resolveDateInChronology(
    parsedDate: ChronoLocalDate,
    chronology: Chronology,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = parsedDate.get(ChronoField.YEAR)
    val month = parsedDate.get(ChronoField.MONTH_OF_YEAR)
    val day = parsedDate.get(ChronoField.DAY_OF_MONTH)
    return when (resolverStyle) {
        ResolverStyle.STRICT -> chronology.date(year, month, day)
        ResolverStyle.SMART -> {
            chronology.range(ChronoField.YEAR).checkValidValue(year.toLong(), ChronoField.YEAR)
            chronology.range(ChronoField.MONTH_OF_YEAR)
                .checkValidValue(month.toLong(), ChronoField.MONTH_OF_YEAR)
            ChronoField.DAY_OF_MONTH.checkValidValue(day.toLong())
            val firstOfMonth = chronology.date(year, month, 1)
            chronology.date(year, month, minOf(day, firstOfMonth.lengthOfMonth()))
        }
        ResolverStyle.LENIENT -> chronology.date(year, 1, 1)
            .plus(month.toLong() - 1, ChronoUnit.MONTHS)
            .plus(day.toLong() - 1, ChronoUnit.DAYS)
    }
}

private class ParsedTemporalAccessor(
    private val date: ChronoLocalDate? = null,
    private val time: LocalTime? = null,
    private val offset: ZoneOffset? = null,
    private val zone: ZoneId? = null,
    private val instant: Instant? = null,
    private val chronology: Chronology = date?.chronology ?: IsoChronology,
    private val fields: Map<TemporalField, Long> = emptyMap(),
    override val excessDays: Period = Period.ZERO,
    override val leapSecond: Boolean = false,
) : TemporalAccessor, ParsedState {
    fun withChronology(
        chronology: Chronology,
        resolverStyle: ResolverStyle,
    ): ParsedTemporalAccessor {
        val resolvedDate = when {
            date == null -> null
            date.chronology == chronology -> date
            else -> resolveDateInChronology(date, chronology, resolverStyle)
        }
        if (this.chronology == chronology && resolvedDate === date) return this
        return ParsedTemporalAccessor(
            date = resolvedDate,
            time = time,
            offset = offset,
            zone = zone,
            instant = instant,
            chronology = chronology,
            fields = fields,
            excessDays = excessDays,
            leapSecond = leapSecond,
        )
    }

    fun withDefaultZone(defaultZone: ZoneId): ParsedTemporalAccessor =
        if (zone != null) {
            this
        } else {
            val resolvedInstant = instant?.let { chronology.zonedDateTime(it, defaultZone) }
            ParsedTemporalAccessor(
                date = resolvedInstant?.date ?: date,
                time = resolvedInstant?.time ?: time,
                offset = resolvedInstant?.offset ?: offset,
                zone = defaultZone,
                instant = instant,
                chronology = chronology,
                fields = fields,
                excessDays = excessDays,
                leapSecond = leapSecond,
            )
        }

    override fun isSupported(field: TemporalField): Boolean = when (field) {
        ChronoField.INSTANT_SECONDS ->
            instant != null || date != null && time != null && (offset != null || zone != null)
        ChronoField.OFFSET_SECONDS -> offset != null || zone is ZoneOffset
        is ChronoField if date != null && field.isDateBased -> date.isSupported(field)
        is ChronoField if time != null && field.isTimeBased -> time.isSupported(field)
        is ChronoField if instant != null -> instant.isSupported(field)
        is ChronoField if field in fields -> true
        is ChronoField -> false
        else -> field in fields || field.isSupportedBy(this)
    }

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.INSTANT_SECONDS,
        ChronoField.OFFSET_SECONDS,
        -> field.range
        is ChronoField if date != null && field.isDateBased -> date.range(field)
        is ChronoField if time != null && field.isTimeBased -> time.range(field)
        is ChronoField if instant != null -> instant.range(field)
        is ChronoField if field in fields -> field.range
        is ChronoField -> unsupported(field)
        else -> if (field in fields) field.range else field.rangeRefinedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.INSTANT_SECONDS -> {
            instant?.let { return it.epochSecond }
            val isoDate = LocalDate.ofEpochDay(date?.toEpochDay() ?: unsupported(field))
            val dateTime = LocalDateTime.of(isoDate, time ?: unsupported(field))
            val resolvedOffset = offset ?: zone?.rules?.getOffset(dateTime) ?: unsupported(field)
            dateTime.toEpochSecond(resolvedOffset)
        }
        ChronoField.OFFSET_SECONDS ->
            (offset ?: (zone as? ZoneOffset))?.totalSeconds?.toLong() ?: unsupported(field)
        is ChronoField if date != null && field.isDateBased -> date.getLong(field)
        is ChronoField if time != null && field.isTimeBased -> time.getLong(field)
        is ChronoField if instant != null -> instant.getLong(field)
        is ChronoField if field in fields -> fields.getValue(field)
        is ChronoField -> unsupported(field)
        else -> fields[field] ?: field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any? = when (query) {
            TemporalQueries.chronology() -> chronology
            TemporalQueries.localDate() -> date?.let { LocalDate.ofEpochDay(it.toEpochDay()) }
            TemporalQueries.localTime() -> time
            TemporalQueries.offset() -> offset ?: (zone as? ZoneOffset)
            TemporalQueries.zoneId() -> zone
            TemporalQueries.precision() -> instant?.query(TemporalQueries.precision())
            else -> return super<TemporalAccessor>.query(query)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    override fun toString(): String = buildString {
        instant?.let(::append)
        date?.let(::append)
        time?.let {
            if (date != null) append('T')
            append(it)
        }
        offset?.let(::append)
        zone?.let {
            append('[')
            append(it)
            append(']')
        }
    }

    private fun unsupported(field: TemporalField): Nothing =
        throw UnsupportedTemporalTypeException("Unsupported field: $field")
}

private val RFC_DAY_NAMES: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private val RFC_MONTH_NAMES: List<String> = listOf(
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
)
