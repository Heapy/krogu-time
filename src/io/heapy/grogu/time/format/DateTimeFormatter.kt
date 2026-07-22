package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.Period
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.chrono.ChronoLocalDate
import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorDiv
import io.heapy.grogu.time.internal.floorMod
import io.heapy.grogu.time.internal.multiplyExact
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.IsoFields
import io.heapy.grogu.time.temporal.JulianFields
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange
import io.heapy.grogu.time.temporal.WeekFields

/** A formatter that prints and parses date-time objects. */
public class DateTimeFormatter private constructor(
    private val printer: (TemporalAccessor) -> String,
    private val parser: (CharSequence) -> TemporalAccessor,
    private val description: String,
    private val builderTokens: List<PatternToken>,
    private val decimalStyleScope: DecimalStyleScope = DecimalStyleScope.ALL,
    private val resolverParser: ((String, ResolverStyle) -> TemporalAccessor)? = null,
    private val patternTokens: List<PatternToken>? = null,
    public val locale: Locale = Locale.getDefault(),
    public val decimalStyle: DecimalStyle = DecimalStyle.STANDARD,
    public val resolverStyle: ResolverStyle = ResolverStyle.STRICT,
    public val resolverFields: Set<TemporalField>? = null,
    public val chronology: Chronology? = null,
    public val zone: ZoneId? = null,
) {
    /** Formats [temporal] into a string. */
    public fun format(temporal: TemporalAccessor): String {
        val adjusted = adjustForFormatting(temporal)
        val formatted = patternTokens?.let { tokens ->
            formatPattern(tokens, adjusted, locale)
        } ?: printer(adjusted)
        return decimalStyleScope.localize(formatted, decimalStyle)
    }

    /** Formats [temporal] and appends the result to [appendable]. */
    public fun formatTo(temporal: TemporalAccessor, appendable: Appendable) {
        appendable.append(format(temporal))
    }

    /** Parses [text] into a temporal accessor. */
    public fun parse(text: CharSequence): TemporalAccessor {
        val standardized = decimalStyleScope.standardize(text, decimalStyle)
        val parsed = when {
            resolverFields != null -> parsePattern(
                tokens = patternTokens ?: builderTokens,
                text = standardized,
                resolverStyle = resolverStyle,
                chronology = chronology,
                resolverFields = resolverFields,
                locale = locale,
            )
            patternTokens != null -> parsePattern(
                tokens = patternTokens,
                text = standardized,
                resolverStyle = resolverStyle,
                chronology = chronology,
                locale = locale,
            )
            resolverParser != null -> resolverParser.invoke(standardized, resolverStyle)
            else -> parser(standardized)
        }
        return try {
            applyOverrides(parsed)
        } catch (exception: DateTimeParseException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw createParseError(text, exception)
        }
    }

    internal fun tokensForBuilder(): List<PatternToken> = builderTokens

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
                patternTokens = patternTokens,
                builderTokens = builderTokens,
                locale = locale,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                resolverFields = resolverFields,
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
                patternTokens = patternTokens,
                builderTokens = builderTokens,
                locale = locale,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                resolverFields = resolverFields,
                chronology = chronology,
                zone = zone,
            )
        }

    /** Returns a formatter that resolves only the supplied parsed [resolverFields]. */
    public fun withResolverFields(
        vararg resolverFields: TemporalField,
    ): DateTimeFormatter = withResolverFields(resolverFields.toSet())

    /** Returns a formatter that resolves only [resolverFields], or all fields when `null`. */
    public fun withResolverFields(resolverFields: Set<TemporalField>?): DateTimeFormatter {
        if (resolverFields == this.resolverFields) return this
        return DateTimeFormatter(
            printer = printer,
            parser = parser,
            description = description,
            decimalStyleScope = decimalStyleScope,
            resolverParser = resolverParser,
            patternTokens = patternTokens,
            builderTokens = builderTokens,
            locale = locale,
            decimalStyle = decimalStyle,
            resolverStyle = resolverStyle,
            resolverFields = resolverFields?.toSet(),
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
                patternTokens = patternTokens,
                builderTokens = builderTokens,
                locale = locale,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                resolverFields = resolverFields,
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
                patternTokens = patternTokens,
                builderTokens = builderTokens,
                locale = locale,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                resolverFields = resolverFields,
                chronology = chronology,
                zone = zone,
            )
        }

    /** Returns a formatter using [locale] for locale-sensitive elements. */
    public fun withLocale(locale: Locale): DateTimeFormatter =
        if (locale == this.locale) {
            this
        } else {
            DateTimeFormatter(
                printer = printer,
                parser = parser,
                description = description,
                decimalStyleScope = decimalStyleScope,
                resolverParser = resolverParser,
                patternTokens = patternTokens,
                builderTokens = builderTokens,
                locale = locale,
                decimalStyle = decimalStyle,
                resolverStyle = resolverStyle,
                resolverFields = resolverFields,
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
            val effectiveChronology = if (patternTokens != null) {
                parsed.query(TemporalQueries.chronology()) ?: IsoChronology
            } else {
                chronology ?: IsoChronology
            }
            return parsed
                .withChronology(effectiveChronology, resolverStyle)
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
        /** Returns a locale-specific date formatter for the ISO chronology. */
        public fun ofLocalizedDate(dateStyle: FormatStyle): DateTimeFormatter =
            localizedFormatter(dateStyle, null)

        /** Returns a locale-specific time formatter for the ISO chronology. */
        public fun ofLocalizedTime(timeStyle: FormatStyle): DateTimeFormatter =
            localizedFormatter(null, timeStyle)

        /** Returns a locale-specific date-time formatter using one style for both parts. */
        public fun ofLocalizedDateTime(dateTimeStyle: FormatStyle): DateTimeFormatter =
            localizedFormatter(dateTimeStyle, dateTimeStyle)

        /** Returns a locale-specific date-time formatter using independent styles. */
        public fun ofLocalizedDateTime(
            dateStyle: FormatStyle,
            timeStyle: FormatStyle,
        ): DateTimeFormatter = localizedFormatter(dateStyle, timeStyle)

        /** Returns a locale-specific formatter selected from [requestedTemplate]. */
        public fun ofLocalizedPattern(requestedTemplate: String): DateTimeFormatter =
            DateTimeFormatterBuilder()
                .appendLocalized(requestedTemplate)
                .toFormatter()
                .withChronology(IsoChronology)

        private fun localizedFormatter(
            dateStyle: FormatStyle?,
            timeStyle: FormatStyle?,
        ): DateTimeFormatter = fromPatternTokens(
            listOf(PatternToken.Localized(dateStyle, timeStyle)),
        ).withChronology(IsoChronology)

        /** Creates a formatter from a date-time pattern. */
        public fun ofPattern(pattern: String): DateTimeFormatter =
            ofPattern(pattern, Locale.getDefault())

        /** Creates a formatter from a date-time [pattern] using [locale]. */
        public fun ofPattern(
            pattern: String,
            locale: Locale,
        ): DateTimeFormatter = fromPatternTokens(compilePattern(pattern), locale)

        internal fun fromPatternTokens(
            tokens: List<PatternToken>,
            locale: Locale = Locale.getDefault(),
        ): DateTimeFormatter {
            val snapshot = tokens.toList()
            return DateTimeFormatter(
                printer = { temporal -> formatPattern(snapshot, temporal, locale) },
                parser = { text ->
                    parsePattern(
                        snapshot,
                        text.toString(),
                        ResolverStyle.SMART,
                        locale = locale,
                    )
                },
                description = describePattern(snapshot),
                builderTokens = snapshot,
                locale = locale,
                resolverParser = { text, style ->
                    parsePattern(snapshot, text, style, locale = locale)
                },
                patternTokens = snapshot,
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
            builderTokens = isoLocalDateBuilderTokens(),
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
            builderTokens = isoOffsetDateBuilderTokens(),
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
            builderTokens = isoDateBuilderTokens(),
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
            builderTokens = isoLocalTimeBuilderTokens(),
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
            builderTokens = isoTimeBuilderTokens(),
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
            builderTokens = isoLocalDateTimeBuilderTokens(),
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
            builderTokens = isoDateTimeBuilderTokens(),
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
            builderTokens = isoOrdinalDateBuilderTokens(),
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
            builderTokens = isoWeekDateBuilderTokens(),
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
            builderTokens = basicIsoDateBuilderTokens(),
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
            builderTokens = rfc1123BuilderTokens(),
            resolverStyle = ResolverStyle.SMART,
            chronology = IsoChronology,
        )

        /** The strict ISO formatter for an instant in UTC. */
        public val ISO_INSTANT: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> Instant.from(temporal).toString() },
            parser = { text -> parseIsoInstant(text) },
            description = "ParseCaseSensitive(false)Instant()",
            decimalStyleScope = DecimalStyleScope.NONE,
            builderTokens = isoInstantBuilderTokens(),
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
            builderTokens = isoOffsetTimeBuilderTokens(),
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
            builderTokens = isoOffsetDateTimeBuilderTokens(),
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
            builderTokens = isoZonedDateTimeBuilderTokens(),
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

private fun isoLocalDateBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.Value(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD),
    PatternToken.Literal("-"),
    PatternToken.Value(ChronoField.MONTH_OF_YEAR, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Literal("-"),
    PatternToken.Value(ChronoField.DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE),
)

private fun isoOffsetDateBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Composite(
        isoLocalDateBuilderTokens(),
        "Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)",
    ),
    PatternToken.Offset("+HH:MM:ss", "Z"),
)

private fun isoDateBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Composite(
        isoLocalDateBuilderTokens(),
        "Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)",
    ),
    PatternToken.Optional(listOf(PatternToken.Offset("+HH:MM:ss", "Z"))),
)

private fun isoLocalTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.Value(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Literal(":"),
    PatternToken.Value(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Optional(
        listOf(
            PatternToken.Literal(":"),
            PatternToken.Value(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NOT_NEGATIVE),
            PatternToken.Optional(
                listOf(PatternToken.Fraction(ChronoField.NANO_OF_SECOND, 0, 9, true)),
            ),
        ),
    ),
)

private fun isoTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Composite(
        isoLocalTimeBuilderTokens(),
        "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
            "[':'Value(SecondOfMinute,2)[Fraction(NanoOfSecond,0,9,DecimalPoint)]]",
    ),
    PatternToken.Optional(listOf(PatternToken.Offset("+HH:MM:ss", "Z"))),
)

private fun isoLocalDateTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Composite(
        isoLocalDateBuilderTokens(),
        "Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)",
    ),
    PatternToken.Literal("T"),
    PatternToken.Composite(
        isoLocalTimeBuilderTokens(),
        "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
            "[':'Value(SecondOfMinute,2)[Fraction(NanoOfSecond,0,9,DecimalPoint)]]",
    ),
)

private fun isoDateTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.Composite(
        isoLocalDateTimeBuilderTokens(),
        "ParseCaseSensitive(false)" +
            "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
            "'-'Value(DayOfMonth,2))'T'" +
            "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
            "[':'Value(SecondOfMinute,2)" +
            "[Fraction(NanoOfSecond,0,9,DecimalPoint)]])",
    ),
    PatternToken.Optional(
        listOf(
            PatternToken.Offset("+HH:MM:ss", "Z"),
            PatternToken.Optional(
                listOf(
                    PatternToken.Literal("["),
                    PatternToken.ParseSetting(ParserSetting.CASE_SENSITIVE),
                    PatternToken.ZoneId(ZoneQueryMode.REGION_ONLY),
                    PatternToken.Literal("]"),
                ),
            ),
        ),
    ),
)

private fun isoOrdinalDateBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Value(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD),
    PatternToken.Literal("-"),
    PatternToken.Value(ChronoField.DAY_OF_YEAR, 3, 3, SignStyle.NOT_NEGATIVE),
    PatternToken.Optional(listOf(PatternToken.Offset("+HH:MM:ss", "Z"))),
)

private fun isoWeekDateBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Value(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD),
    PatternToken.Literal("-W"),
    PatternToken.Value(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Literal("-"),
    PatternToken.Value(ChronoField.DAY_OF_WEEK, 1, 1, SignStyle.NOT_NEGATIVE),
    PatternToken.Optional(listOf(PatternToken.Offset("+HH:MM:ss", "Z"))),
)

private fun basicIsoDateBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Value(ChronoField.YEAR, 4, 4, SignStyle.NOT_NEGATIVE),
    PatternToken.Value(ChronoField.MONTH_OF_YEAR, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Value(ChronoField.DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Optional(
        listOf(
            PatternToken.ParseSetting(ParserSetting.LENIENT),
            PatternToken.Offset("+HHMMss", "Z"),
            PatternToken.ParseSetting(ParserSetting.STRICT),
        ),
    ),
)

private fun rfc1123BuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.ParseSetting(ParserSetting.LENIENT),
    PatternToken.Optional(
        listOf(
            PatternToken.Text(
                ChronoField.DAY_OF_WEEK,
                linkedMapOf(
                    1L to "Mon",
                    2L to "Tue",
                    3L to "Wed",
                    4L to "Thu",
                    5L to "Fri",
                    6L to "Sat",
                    7L to "Sun",
                ),
            ),
            PatternToken.Literal(", "),
        ),
    ),
    PatternToken.Value(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Literal(" "),
    PatternToken.Text(
        ChronoField.MONTH_OF_YEAR,
        linkedMapOf(
            1L to "Jan",
            2L to "Feb",
            3L to "Mar",
            4L to "Apr",
            5L to "May",
            6L to "Jun",
            7L to "Jul",
            8L to "Aug",
            9L to "Sep",
            10L to "Oct",
            11L to "Nov",
            12L to "Dec",
        ),
    ),
    PatternToken.Literal(" "),
    PatternToken.Value(ChronoField.YEAR, 4, 4, SignStyle.NOT_NEGATIVE),
    PatternToken.Literal(" "),
    PatternToken.Value(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Literal(":"),
    PatternToken.Value(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NOT_NEGATIVE),
    PatternToken.Optional(
        listOf(
            PatternToken.Literal(":"),
            PatternToken.Value(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NOT_NEGATIVE),
        ),
    ),
    PatternToken.Literal(" "),
    PatternToken.Offset("+HHMM", "GMT"),
)

private fun isoOffsetTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Composite(
        isoLocalTimeBuilderTokens(),
        "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
            "[':'Value(SecondOfMinute,2)[Fraction(NanoOfSecond,0,9,DecimalPoint)]]",
    ),
    PatternToken.Offset("+HH:MM:ss", "Z"),
)

private fun isoOffsetDateTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Composite(
        isoLocalDateTimeBuilderTokens(),
        "ParseCaseSensitive(false)" +
            "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
            "'-'Value(DayOfMonth,2))'T'" +
            "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
            "[':'Value(SecondOfMinute,2)" +
            "[Fraction(NanoOfSecond,0,9,DecimalPoint)]])",
    ),
    PatternToken.ParseSetting(ParserSetting.LENIENT),
    PatternToken.Offset("+HH:MM:ss", "Z"),
    PatternToken.ParseSetting(ParserSetting.STRICT),
)

private fun isoZonedDateTimeBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.Composite(
        isoOffsetDateTimeBuilderTokens(),
        "ParseCaseSensitive(false)" +
            "(ParseCaseSensitive(false)" +
            "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
            "'-'Value(DayOfMonth,2))'T'" +
            "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
            "[':'Value(SecondOfMinute,2)" +
            "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]))" +
            "ParseStrict(false)Offset(+HH:MM:ss,'Z')ParseStrict(true)",
    ),
    PatternToken.Optional(
        listOf(
            PatternToken.Literal("["),
            PatternToken.ParseSetting(ParserSetting.CASE_SENSITIVE),
            PatternToken.ZoneId(ZoneQueryMode.REGION_ONLY),
            PatternToken.Literal("]"),
        ),
    ),
)

private fun isoInstantBuilderTokens(): List<PatternToken> = listOf(
    PatternToken.ParseSetting(ParserSetting.CASE_INSENSITIVE),
    PatternToken.Instant(-2),
)

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

    data class ReducedValue(
        val field: TemporalField,
        val minWidth: Int,
        val maxWidth: Int,
        val base: ReducedValueBase,
    ) : PatternToken

    data class Fraction(
        val field: TemporalField,
        val minWidth: Int,
        val maxWidth: Int,
        val decimalPoint: Boolean,
    ) : PatternToken

    data class Text(
        val field: TemporalField,
        val textLookup: Map<Long, String>,
    ) : PatternToken {
        val parseLookup: List<Pair<String, Long>> = run {
            val valueByText = linkedMapOf<String, Long>()
            textLookup.forEach { (value, fieldText) -> valueByText[fieldText] = value }
            valueByText.entries
                .sortedByDescending { (fieldText) -> fieldText.length }
                .map { (fieldText, value) -> fieldText to value }
        }
    }

    data class LocalizedText(
        val field: TemporalField,
        val style: TextStyle,
    ) : PatternToken

    data class DayPeriod(val style: TextStyle) : PatternToken

    data class LocalizedWeek(
        val symbol: Char,
        val count: Int,
    ) : PatternToken

    data class Localized(
        val dateStyle: FormatStyle?,
        val timeStyle: FormatStyle?,
        val requestedTemplate: String? = null,
    ) : PatternToken

    data class Instant(val fractionalDigits: Int) : PatternToken

    data class Composite(
        val tokens: List<PatternToken>,
        val description: String,
    ) : PatternToken

    data class Offset(
        val pattern: String,
        val noOffsetText: String,
    ) : PatternToken

    data class LocalizedOffset(val style: TextStyle) : PatternToken

    data class ZoneText(
        val style: TextStyle,
        val generic: Boolean,
        val preferredZoneIds: Set<String> = emptySet(),
    ) : PatternToken

    data class ZoneId(val queryMode: ZoneQueryMode) : PatternToken

    data object ChronologyId : PatternToken

    data class ChronologyText(val style: TextStyle) : PatternToken

    data class ParseSetting(val setting: ParserSetting) : PatternToken

    data class DefaultValue(
        val field: TemporalField,
        val value: Long,
    ) : PatternToken

    data class Optional(val tokens: List<PatternToken>) : PatternToken

    data class Padded(
        val token: PatternToken,
        val width: Int,
        val padCharacter: Char,
    ) : PatternToken
}

internal class PatternSection(
    val tokens: MutableList<PatternToken> = mutableListOf(),
    var padWidth: Int = 0,
    var padCharacter: Char = ' ',
)

internal enum class ParserSetting {
    CASE_SENSITIVE,
    CASE_INSENSITIVE,
    STRICT,
    LENIENT,
}

internal sealed interface ReducedValueBase {
    data class Value(val value: Int) : ReducedValueBase

    data class Date(val date: ChronoLocalDate) : ReducedValueBase
}

internal enum class ZoneQueryMode {
    ZONE_ID,
    REGION_ONLY,
    ZONE_OR_OFFSET,
}

private val OFFSET_PATTERNS: List<String> = listOf(
    "+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM",
    "+HHMMss", "+HH:MM:ss", "+HHMMSS", "+HH:MM:SS", "+HHmmss", "+HH:mm:ss",
    "+H", "+Hmm", "+H:mm", "+HMM", "+H:MM",
    "+HMMss", "+H:MM:ss", "+HMMSS", "+H:MM:SS", "+Hmmss", "+H:mm:ss",
)

internal fun validateOffsetPattern(pattern: String): Int =
    OFFSET_PATTERNS.indexOf(pattern).takeIf { it >= 0 }
        ?: throw IllegalArgumentException("Invalid zone offset pattern: $pattern")

internal fun compilePattern(pattern: String): List<PatternToken> {
    val rootSection = PatternSection()
    val optionalSections = mutableListOf<PatternSection>()
    fun activeSection(): PatternSection = optionalSections.lastOrNull() ?: rootSection
    visitPattern(
        pattern = pattern,
        appendToken = { token -> activeSection().appendToken(token) },
        padNext = { width ->
            activeSection().padWidth = width
            activeSection().padCharacter = ' '
        },
        optionalStart = { optionalSections.add(PatternSection()) },
        optionalEnd = {
            require(optionalSections.isNotEmpty()) {
                "Pattern invalid as it contains ] without previous ["
            }
            val optionalSection = optionalSections.removeAt(optionalSections.lastIndex)
            if (optionalSection.tokens.isNotEmpty()) {
                activeSection().appendToken(PatternToken.Optional(optionalSection.tokens.toList()))
            }
        },
    )
    while (optionalSections.isNotEmpty()) {
        val optionalSection = optionalSections.removeAt(optionalSections.lastIndex)
        if (optionalSection.tokens.isNotEmpty()) {
            activeSection().appendToken(PatternToken.Optional(optionalSection.tokens.toList()))
        }
    }
    return rootSection.tokens
}

internal fun visitPattern(
    pattern: String,
    appendToken: (PatternToken) -> Unit,
    padNext: (Int) -> Unit,
    optionalStart: () -> Unit,
    optionalEnd: () -> Unit,
) {
    var index = 0
    while (index < pattern.length) {
        val character = pattern[index]
        when {
            character == '\'' -> {
                if (pattern.getOrNull(index + 1) == '\'') {
                    appendToken(PatternToken.Literal("'"))
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
                appendToken(PatternToken.Literal(literal.toString()))
            }
            character == '[' -> {
                optionalStart()
                index++
            }
            character == ']' -> {
                optionalEnd()
                index++
            }
            character == '#' || character == '{' || character == '}' ->
                throw IllegalArgumentException("Pattern includes reserved character: '$character'")
            character.isAsciiLetter() -> {
                var end = index + 1
                while (end < pattern.length && pattern[end] == character) end++
                val count = end - index
                if (character == 'p') {
                    if (end == pattern.length || !pattern[end].isAsciiLetter()) {
                        throw IllegalArgumentException(
                            "Pad letter 'p' must be followed by valid pad pattern: $pattern",
                        )
                    }
                    padNext(count)
                    index = end
                    continue
                }
                validatePatternField(character, count)
                appendToken(
                    createPatternFieldToken(character, count),
                )
                index = end
            }
            else -> {
                appendToken(PatternToken.Literal(character.toString()))
                index++
            }
        }
    }
}

internal fun PatternSection.appendToken(token: PatternToken) {
    val appendedToken = if (padWidth > 0) {
        PatternToken.Padded(token, padWidth, padCharacter)
    } else {
        token
    }
    padWidth = 0
    padCharacter = ' '
    tokens.appendPatternToken(appendedToken)
}

internal fun MutableList<PatternToken>.appendPatternToken(token: PatternToken) {
    if (token !is PatternToken.Literal || token.text.isNotEmpty()) add(token)
}

private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

private fun validatePatternField(symbol: Char, count: Int) {
    when (symbol) {
        'u', 'y', 'Y', 'g' -> require(count <= 19) {
            "The count of pattern letters must not exceed 19: $symbol"
        }
        'Q', 'q', 'M', 'L' -> require(count <= 5) { "Too many pattern letters: $symbol" }
        'G', 'E', 'e' -> require(count <= 5) { "Too many pattern letters: $symbol" }
        'c' -> require(count != 2 && count <= 5) {
            if (count == 2) "Invalid pattern \"cc\"" else "Too many pattern letters: $symbol"
        }
        'w' -> require(count <= 2) { "Too many pattern letters: $symbol" }
        'W' -> require(count == 1) { "Too many pattern letters: $symbol" }
        'a' -> require(count == 1) { "Too many pattern letters: $symbol" }
        'B' -> require(count == 1 || count == 4 || count == 5) {
            "Wrong number of pattern letters: $symbol"
        }
        'd', 'H', 'k', 'K', 'h', 'm', 's' -> require(count <= 2) {
            "Too many pattern letters: $symbol"
        }
        'D' -> require(count <= 3) { "Too many pattern letters: $symbol" }
        'F' -> require(count == 1) { "Too many pattern letters: $symbol" }
        'A', 'n', 'N' -> require(count <= 19) {
            "The count of pattern letters must not exceed 19: $symbol"
        }
        'S' -> require(count <= 9) { "Minimum width must be from 0 to 9 inclusive but was $count" }
        'O' -> require(count == 1 || count == 4) {
            "Pattern letter count must be 1 or 4: $symbol"
        }
        'X', 'x', 'Z' -> require(count <= 5) { "Too many pattern letters: $symbol" }
        'z' -> require(count <= 4) { "Too many pattern letters: $symbol" }
        'v' -> require(count == 1 || count == 4) {
            "Pattern letter count must be 1 or 4: $symbol"
        }
        'V' -> require(count == 2) { "Pattern letter count must be 2: V" }
        else -> throw IllegalArgumentException("Unknown pattern letter: $symbol")
    }
}

private fun createPatternFieldToken(
    symbol: Char,
    count: Int,
): PatternToken = when (symbol) {
    'O' -> PatternToken.LocalizedOffset(if (count == 1) TextStyle.SHORT else TextStyle.FULL)
    'Z' -> if (count == 4) {
        PatternToken.LocalizedOffset(TextStyle.FULL)
    } else {
        PatternToken.Field(symbol, count)
    }
    'z' -> PatternToken.ZoneText(
        style = if (count == 4) TextStyle.FULL else TextStyle.SHORT,
        generic = false,
    )
    'v' -> PatternToken.ZoneText(
        style = if (count == 4) TextStyle.FULL else TextStyle.SHORT,
        generic = true,
    )
    'D' -> when (count) {
        1 -> variablePatternValue(ChronoField.DAY_OF_YEAR)
        2 -> PatternToken.Value(ChronoField.DAY_OF_YEAR, 2, 3, SignStyle.NOT_NEGATIVE)
        else -> fixedPatternValue(ChronoField.DAY_OF_YEAR, 3)
    }
    'F' -> variablePatternValue(ChronoField.ALIGNED_WEEK_OF_MONTH)
    'Q', 'q' -> if (count <= 2) {
        timePatternValue(IsoFields.QUARTER_OF_YEAR, count)
    } else {
        localizedPatternText(IsoFields.QUARTER_OF_YEAR, count, symbol == 'q')
    }
    'M', 'L' -> if (count <= 2) {
        timePatternValue(ChronoField.MONTH_OF_YEAR, count)
    } else {
        localizedPatternText(ChronoField.MONTH_OF_YEAR, count, symbol == 'L')
    }
    'Y', 'w', 'W' -> PatternToken.LocalizedWeek(symbol, count)
    'e', 'c' -> if (count <= 2) {
        PatternToken.LocalizedWeek(symbol, count)
    } else {
        localizedPatternText(ChronoField.DAY_OF_WEEK, count, symbol == 'c')
    }
    'E' -> localizedPatternText(ChronoField.DAY_OF_WEEK, maxOf(count, 3), false)
    'G' -> localizedPatternText(ChronoField.ERA, maxOf(count, 3), false)
    'a' -> PatternToken.LocalizedText(ChronoField.AMPM_OF_DAY, TextStyle.SHORT)
    'B' -> PatternToken.DayPeriod(
        when (count) {
            1 -> TextStyle.SHORT
            4 -> TextStyle.FULL
            else -> TextStyle.NARROW
        },
    )
    'g' -> PatternToken.Value(JulianFields.MODIFIED_JULIAN_DAY, count, 19, SignStyle.NORMAL)
    'k' -> timePatternValue(ChronoField.CLOCK_HOUR_OF_DAY, count)
    'K' -> timePatternValue(ChronoField.HOUR_OF_AMPM, count)
    'h' -> timePatternValue(ChronoField.CLOCK_HOUR_OF_AMPM, count)
    'A' -> PatternToken.Value(ChronoField.MILLI_OF_DAY, count, 19, SignStyle.NOT_NEGATIVE)
    'n' -> PatternToken.Value(ChronoField.NANO_OF_SECOND, count, 19, SignStyle.NOT_NEGATIVE)
    'N' -> PatternToken.Value(ChronoField.NANO_OF_DAY, count, 19, SignStyle.NOT_NEGATIVE)
    else -> PatternToken.Field(symbol, count)
}

private fun variablePatternValue(field: TemporalField): PatternToken.Value =
    PatternToken.Value(field, 1, 19, SignStyle.NORMAL)

private fun fixedPatternValue(
    field: TemporalField,
    width: Int,
): PatternToken.Value = PatternToken.Value(field, width, width, SignStyle.NOT_NEGATIVE)

private fun timePatternValue(
    field: TemporalField,
    count: Int,
): PatternToken.Value = if (count == 1) variablePatternValue(field) else fixedPatternValue(field, count)

private fun localizedPatternText(
    field: TemporalField,
    count: Int,
    standalone: Boolean,
): PatternToken.LocalizedText {
    val normalStyle = when (count) {
        3 -> TextStyle.SHORT
        4 -> TextStyle.FULL
        5 -> TextStyle.NARROW
        else -> error("Unsupported localized text width: $count")
    }
    return PatternToken.LocalizedText(
        field = field,
        style = if (standalone) normalStyle.asStandalone() else normalStyle,
    )
}

private fun formatPattern(
    tokens: List<PatternToken>,
    temporal: TemporalAccessor,
    locale: Locale,
): String = buildString {
    tokens.forEach { token ->
        when (token) {
            is PatternToken.Literal -> append(token.text)
            is PatternToken.Field -> append(formatPatternField(token, temporal))
            is PatternToken.Value -> append(formatPatternValue(token, temporal))
            is PatternToken.ReducedValue -> append(formatPatternReducedValue(token, temporal))
            is PatternToken.Fraction -> append(formatPatternFraction(token, temporal))
            is PatternToken.Text -> append(formatPatternText(token, temporal))
            is PatternToken.LocalizedText -> append(formatPatternLocalizedText(token, temporal, locale))
            is PatternToken.DayPeriod -> append(formatPatternDayPeriod(token, temporal, locale))
            is PatternToken.LocalizedWeek -> append(formatPatternLocalizedWeek(token, temporal, locale))
            is PatternToken.Localized -> append(
                formatPattern(token.patternTokens(locale, temporal.chronologyId()), temporal, locale),
            )
            is PatternToken.Instant -> append(formatPatternInstant(token, temporal))
            is PatternToken.Composite -> append(formatPattern(token.tokens, temporal, locale))
            is PatternToken.Offset -> append(formatBuilderOffset(token, temporal))
            is PatternToken.LocalizedOffset -> append(formatBuilderLocalizedOffset(token, temporal))
            is PatternToken.ZoneText -> append(formatPatternZoneText(token, temporal, locale))
            is PatternToken.ZoneId -> append(formatBuilderZoneId(token, temporal))
            PatternToken.ChronologyId -> append(formatBuilderChronologyId(temporal))
            is PatternToken.ChronologyText -> append(
                formatBuilderChronologyText(token, temporal, locale),
            )
            is PatternToken.Optional -> try {
                append(formatPattern(token.tokens, temporal, locale))
            } catch (_: DateTimeException) {
                // Missing data suppresses the complete optional section.
            }
            is PatternToken.Padded -> {
                val formatted = formatPattern(listOf(token.token), temporal, locale)
                if (formatted.length > token.width) {
                    throw DateTimeException(
                        "Cannot print as output of ${formatted.length} characters " +
                            "exceeds pad width of ${token.width}",
                    )
                }
                repeat(token.width - formatted.length) { append(token.padCharacter) }
                append(formatted)
            }
            is PatternToken.ParseSetting,
            is PatternToken.DefaultValue,
            -> Unit
        }
    }
}

private fun formatPatternText(
    token: PatternToken.Text,
    temporal: TemporalAccessor,
): String {
    val value = temporal.getLong(token.field)
    return token.textLookup[value] ?: value.toString()
}

private fun formatPatternLocalizedText(
    token: PatternToken.LocalizedText,
    temporal: TemporalAccessor,
    locale: Locale,
): String {
    val value = temporal.getLong(token.field)
    val field = token.field.toLocaleTextField() ?: return value.toString()
    val chronologyId = temporal.query(TemporalQueries.chronology())?.id ?: IsoChronology.id
    return localeTextValues(locale.toLanguageTag(), chronologyId, field, token.style)
        .firstOrNull { candidate -> candidate.value == value }
        ?.text
        ?: value.toString()
}

private fun formatPatternDayPeriod(
    token: PatternToken.DayPeriod,
    temporal: TemporalAccessor,
    locale: Locale,
): String {
    val hour = temporal.get(ChronoField.HOUR_OF_DAY)
    val minute = if (temporal.isSupported(ChronoField.MINUTE_OF_HOUR)) {
        temporal.get(ChronoField.MINUTE_OF_HOUR)
    } else {
        0
    }
    return formatLocaleDayPeriod(locale.toLanguageTag(), hour, minute, token.style)
}

private fun formatPatternLocalizedWeek(
    token: PatternToken.LocalizedWeek,
    temporal: TemporalAccessor,
    locale: Locale,
): String = when (val numericToken = token.numericToken(locale)) {
    is PatternToken.Value -> formatPatternValue(numericToken, temporal)
    is PatternToken.ReducedValue -> formatPatternReducedValue(numericToken, temporal)
    else -> error("Unsupported localized week token: $numericToken")
}

private fun PatternToken.LocalizedWeek.numericToken(locale: Locale): PatternToken {
    val weekFields = WeekFields.of(locale)
    val field = when (symbol) {
        'Y' -> weekFields.weekBasedYear
        'w' -> weekFields.weekOfWeekBasedYear
        'W' -> weekFields.weekOfMonth
        'e', 'c' -> weekFields.dayOfWeek
        else -> error("Unsupported localized week pattern: $symbol")
    }
    return when {
        symbol == 'Y' && count == 2 -> PatternToken.ReducedValue(
            field = field,
            minWidth = 2,
            maxWidth = 2,
            base = ReducedValueBase.Date(LocalDate.of(2000, 1, 1)),
        )
        symbol == 'Y' -> PatternToken.Value(
            field = field,
            minWidth = count,
            maxWidth = 19,
            signStyle = if (count < 4) SignStyle.NORMAL else SignStyle.EXCEEDS_PAD,
        )
        else -> PatternToken.Value(
            field = field,
            minWidth = count,
            maxWidth = if (symbol == 'w') 2 else count,
            signStyle = SignStyle.NOT_NEGATIVE,
        )
    }
}

private fun PatternToken.Localized.patternTokens(
    locale: Locale,
    chronologyId: String,
): List<PatternToken> = compilePattern(
    requestedTemplate?.let { template ->
        localizedDateTimePattern(
            languageTag = locale.toLanguageTag(),
            chronologyId = chronologyId,
            requestedTemplate = template,
        )
    } ?: localizedDateTimePattern(
        languageTag = locale.toLanguageTag(),
        chronologyId = chronologyId,
        dateStyle = dateStyle,
        timeStyle = timeStyle,
    ),
)

private fun TemporalAccessor.chronologyId(): String =
    query(TemporalQueries.chronology())?.id ?: IsoChronology.id

private fun formatBuilderChronologyId(temporal: TemporalAccessor): String =
    temporal.query(TemporalQueries.chronology())?.id
        ?: throw DateTimeException("Unable to extract chronology from temporal $temporal")

private fun formatBuilderChronologyText(
    token: PatternToken.ChronologyText,
    temporal: TemporalAccessor,
    locale: Locale,
): String {
    val chronology = temporal.query(TemporalQueries.chronology())
        ?: throw DateTimeException("Unable to extract chronology from temporal $temporal")
    return chronology.getDisplayName(token.style, locale)
}

private fun formatPatternZoneText(
    token: PatternToken.ZoneText,
    temporal: TemporalAccessor,
    locale: Locale,
): String {
    val zone = temporal.query(TemporalQueries.zoneId())
        ?: throw DateTimeException("Unable to extract ZoneId from temporal $temporal")
    if (zone is ZoneOffset) return zone.id

    val epochSecond = when {
        token.generic -> null
        temporal.isSupported(ChronoField.INSTANT_SECONDS) ->
            temporal.getLong(ChronoField.INSTANT_SECONDS)
        temporal.isSupported(ChronoField.EPOCH_DAY) &&
            temporal.isSupported(ChronoField.NANO_OF_DAY) -> {
            val dateTime = LocalDateTime.of(
                LocalDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY)),
                LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY)),
            )
            if (zone.rules.getTransition(dateTime) == null) {
                ZonedDateTime.of(dateTime, zone).toEpochSecond()
            } else {
                null
            }
        }
        else -> null
    }
    return formatLocaleZoneText(
        languageTag = locale.toLanguageTag(),
        zoneId = zone.id,
        epochSecond = epochSecond,
        style = token.style,
        generic = token.generic,
    ) ?: zone.id
}

private fun formatPatternInstant(
    token: PatternToken.Instant,
    temporal: TemporalAccessor,
): String {
    val epochSecond = temporal.getLong(ChronoField.INSTANT_SECONDS)
    val nano = if (temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
        ChronoField.NANO_OF_SECOND.checkValidIntValue(
            temporal.getLong(ChronoField.NANO_OF_SECOND),
        )
    } else {
        0
    }
    val canonical = Instant.ofEpochSecond(epochSecond, nano.toLong()).toString()
    if (token.fractionalDigits == -2) return canonical

    val timeSeparator = canonical.indexOf('T')
    val fractionSeparator = canonical.indexOf('.', timeSeparator)
    val seconds = if (fractionSeparator >= 0) {
        canonical.substring(0, fractionSeparator)
    } else {
        canonical.dropLast(1)
    }
    val fraction = when (token.fractionalDigits) {
        -1 -> nano.toString().padStart(9, '0').trimEnd('0')
        0 -> ""
        else -> nano.toString().padStart(9, '0').take(token.fractionalDigits)
    }
    return if (fraction.isEmpty()) "${seconds}Z" else "$seconds.${fraction}Z"
}

private fun formatBuilderZoneId(
    token: PatternToken.ZoneId,
    temporal: TemporalAccessor,
): String {
    val zone = when (token.queryMode) {
        ZoneQueryMode.ZONE_ID,
        ZoneQueryMode.REGION_ONLY,
        -> temporal.query(TemporalQueries.zoneId())
        ZoneQueryMode.ZONE_OR_OFFSET -> temporal.query(TemporalQueries.zone())
    } ?: throw DateTimeException("Unable to extract ZoneId from temporal $temporal")
    if (token.queryMode == ZoneQueryMode.REGION_ONLY && zone is ZoneOffset) {
        throw DateTimeException("Unable to extract region-based ZoneId from temporal $temporal")
    }
    return zone.id
}

private fun formatBuilderLocalizedOffset(
    token: PatternToken.LocalizedOffset,
    temporal: TemporalAccessor,
): String = "GMT" + formatBuilderOffset(token.offsetToken(), temporal)

private fun PatternToken.LocalizedOffset.offsetToken(): PatternToken.Offset = PatternToken.Offset(
    pattern = if (style == TextStyle.FULL) "+HH:MM:ss" else "+H:mm:ss",
    noOffsetText = "",
)

private fun formatBuilderOffset(
    token: PatternToken.Offset,
    temporal: TemporalAccessor,
): String {
    val totalSeconds = temporal.getLong(ChronoField.OFFSET_SECONDS).toInt()
    if (totalSeconds == 0) return token.noOffsetText

    val type = validateOffsetPattern(token.pattern)
    val style = type % 11
    val paddedHour = type < 11
    val colon = style > 0 && style % 2 == 0
    val absoluteHours = kotlin.math.abs(totalSeconds / 3_600 % 100)
    val absoluteMinutes = kotlin.math.abs(totalSeconds / 60 % 60)
    val absoluteSeconds = kotlin.math.abs(totalSeconds % 60)
    var output = absoluteHours
    val result = buildString {
        append(if (totalSeconds < 0) '-' else '+')
        if (paddedHour || absoluteHours >= 10) {
            appendTwoOffsetDigits(absoluteHours)
        } else {
            append(absoluteHours)
        }
        if (style in 3..8 || style >= 9 && absoluteSeconds > 0 || style >= 1 && absoluteMinutes > 0) {
            if (colon) append(':')
            appendTwoOffsetDigits(absoluteMinutes)
            output += absoluteMinutes
            if (style == 7 || style == 8 || style >= 5 && absoluteSeconds > 0) {
                if (colon) append(':')
                appendTwoOffsetDigits(absoluteSeconds)
                output += absoluteSeconds
            }
        }
    }
    return if (output == 0) token.noOffsetText else result
}

private fun StringBuilder.appendTwoOffsetDigits(value: Int) {
    append(('0'.code + value / 10).toChar())
    append(('0'.code + value % 10).toChar())
}

private fun formatPatternFraction(
    token: PatternToken.Fraction,
    temporal: TemporalAccessor,
): String {
    val range = token.field.range
    val value = temporal.getLong(token.field)
    if (!range.isValidValue(value)) {
        throw DateTimeException("Invalid value for ${token.field}: $value")
    }
    val rangeSize = range.maximum - range.minimum + 1
    var remainder = value - range.minimum
    val digits = buildString(token.maxWidth) {
        repeat(token.maxWidth) {
            remainder *= 10
            append(remainder / rangeSize)
            remainder %= rangeSize
        }
    }.trimEnd('0').padEnd(token.minWidth, '0')
    if (digits.isEmpty()) return ""
    return if (token.decimalPoint) ".$digits" else digits
}

private fun formatPatternReducedValue(
    token: PatternToken.ReducedValue,
    temporal: TemporalAccessor,
): String {
    val value = temporal.getLong(token.field)
    val baseValue = token.resolveBaseValue(
        temporal.query(TemporalQueries.chronology()) ?: IsoChronology,
    )
    val minRange = reducedPowerOfTen(token.minWidth)
    val maxRange = reducedPowerOfTen(token.maxWidth)
    val divisor = if (value >= baseValue && value < baseValue + minRange) {
        minRange
    } else {
        maxRange
    }
    val reduced = kotlin.math.abs(value % divisor)
    return reduced.toString().padStart(token.minWidth, '0')
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
    chronology: Chronology? = null,
    resolverFields: Set<TemporalField>? = null,
    locale: Locale = Locale.ROOT,
): TemporalAccessor {
    val values = mutableMapOf<TemporalField, Long>()
    var offset: ZoneOffset? = null
    var zone: ZoneId? = null
    var parsedChronology: Chronology? = null
    var leapSecond = false
    var dayPeriod: LocaleDayPeriod? = null
    val chronologySensitiveReducedValues = mutableListOf<ParsedChronologySensitiveReducedValue>()
    var index = 0
    var caseSensitive = true
    var strict = true
    fun effectiveChronology(): Chronology = parsedChronology ?: chronology ?: IsoChronology
    fun refreshChronologySensitiveReducedValues() {
        chronologySensitiveReducedValues.forEach { reduced ->
            values[reduced.token.field] = reduced.token.resolveReducedValue(
                rawValue = reduced.rawValue,
                digitCount = reduced.digitCount,
                chronology = effectiveChronology(),
            )
        }
    }
    fun storeOffset(
        parsed: ParsedPatternOffset,
        errorIndex: Int,
        input: String,
    ) {
        val totalSeconds = parsed.offset.totalSeconds.toLong()
        val previousValue = values.put(ChronoField.OFFSET_SECONDS, totalSeconds)
        if (previousValue != null && previousValue != totalSeconds) {
            throw DateTimeParseException("Conflict found for offset", input, errorIndex)
        }
        if (offset != null && offset != parsed.offset) {
            throw DateTimeParseException("Conflict found for offset", input, errorIndex)
        }
        offset = parsed.offset
        index = parsed.endIndex
    }
    fun parseTokens(
        currentTokens: List<PatternToken>,
        input: String,
    ) {
        currentTokens.forEachIndexed { tokenIndex, token ->
            when (token) {
                is PatternToken.Literal -> {
                    if (!input.matchesAt(index, token.text, caseSensitive)) {
                        throw DateTimeParseException(
                            "Text could not be parsed at index $index",
                            input,
                            index,
                        )
                    }
                    index += token.text.length
                }
                is PatternToken.Field -> {
                    when (token.symbol) {
                        'X', 'x', 'Z' -> {
                            val parsed = parsePatternOffset(token, input, index, caseSensitive)
                            storeOffset(parsed, index, input)
                        }
                        'V' -> {
                            val parsed = parsePatternZone(input, index, caseSensitive)
                            if (zone != null && zone != parsed.zone) {
                                throw DateTimeParseException("Conflict found for zone", input, index)
                            }
                            zone = parsed.zone
                            index = parsed.endIndex
                        }
                        else -> {
                            val parsed = parsePatternField(token, input, index, strict)
                            val field = token.symbol.toPatternField()
                            val previous = values.put(field, parsed.value)
                            if (previous != null && previous != parsed.value) {
                                throw DateTimeParseException(
                                    "Conflict found for pattern field ${token.symbol}",
                                    input,
                                    index,
                                )
                            }
                            index = parsed.endIndex
                        }
                    }
                }
                is PatternToken.Value -> {
                    val parsed = parsePatternValue(currentTokens, tokenIndex, token, input, index, strict)
                    val previous = values.put(token.field, parsed.value)
                    if (previous != null && previous != parsed.value) {
                        throw DateTimeParseException(
                            "Conflict found for field ${token.field}",
                            input,
                            index,
                        )
                    }
                    index = parsed.endIndex
                }
                is PatternToken.ReducedValue -> {
                    val parsed = parsePatternReducedValue(
                        currentTokens,
                        tokenIndex,
                        token,
                        input,
                        index,
                        effectiveChronology(),
                        strict,
                    )
                    val previous = values.put(token.field, parsed.value)
                    if (previous != null && previous != parsed.value) {
                        throw DateTimeParseException(
                            "Conflict found for field ${token.field}",
                            input,
                            index,
                        )
                    }
                    if (token.base is ReducedValueBase.Date) {
                        chronologySensitiveReducedValues += ParsedChronologySensitiveReducedValue(
                            token = token,
                            rawValue = parsed.rawValue,
                            digitCount = parsed.digitCount,
                        )
                    }
                    index = parsed.endIndex
                }
                is PatternToken.Fraction -> {
                    val parsed = parsePatternFraction(token, input, index, strict)
                    parsed.value?.let { value ->
                        val previous = values.put(token.field, value)
                        if (previous != null && previous != value) {
                            throw DateTimeParseException(
                                "Conflict found for field ${token.field}",
                                input,
                                index,
                            )
                        }
                    }
                    index = parsed.endIndex
                }
                is PatternToken.Text -> {
                    val parsed = parsePatternText(token, input, index, caseSensitive, strict)
                    val previous = values.put(token.field, parsed.value)
                    if (previous != null && previous != parsed.value) {
                        throw DateTimeParseException(
                            "Conflict found for field ${token.field}",
                            input,
                            index,
                        )
                    }
                    index = parsed.endIndex
                }
                is PatternToken.LocalizedText -> {
                    val parsed = parsePatternLocalizedText(
                        token = token,
                        text = input,
                        startIndex = index,
                        caseSensitive = caseSensitive,
                        strict = strict,
                        locale = locale,
                        chronologyId = effectiveChronology().id,
                    )
                    val previous = values.put(token.field, parsed.value)
                    if (previous != null && previous != parsed.value) {
                        throw DateTimeParseException(
                            "Conflict found for field ${token.field}",
                            input,
                            index,
                        )
                    }
                    index = parsed.endIndex
                }
                is PatternToken.DayPeriod -> {
                    val parsed = parsePatternDayPeriod(
                        token = token,
                        text = input,
                        startIndex = index,
                        caseSensitive = caseSensitive,
                        strict = strict,
                        locale = locale,
                    )
                    dayPeriod = parsed.dayPeriod
                    index = parsed.endIndex
                }
                is PatternToken.LocalizedWeek -> {
                    val numericToken = token.numericToken(locale)
                    val (field, parsedValue, parsedEndIndex) = when (numericToken) {
                        is PatternToken.Value -> parsePatternValue(
                            tokens = currentTokens,
                            tokenIndex = tokenIndex,
                            token = numericToken,
                            text = input,
                            startIndex = index,
                            strict = strict,
                        ).let { parsed ->
                            Triple(numericToken.field, parsed.value, parsed.endIndex)
                        }
                        is PatternToken.ReducedValue -> parsePatternReducedValue(
                            tokens = currentTokens,
                            tokenIndex = tokenIndex,
                            token = numericToken,
                            text = input,
                            startIndex = index,
                            chronology = effectiveChronology(),
                            strict = strict,
                        ).let { parsed ->
                            if (numericToken.base is ReducedValueBase.Date) {
                                chronologySensitiveReducedValues += ParsedChronologySensitiveReducedValue(
                                    token = numericToken,
                                    rawValue = parsed.rawValue,
                                    digitCount = parsed.digitCount,
                                )
                            }
                            Triple(numericToken.field, parsed.value, parsed.endIndex)
                        }
                        else -> error("Unsupported localized week token: $numericToken")
                    }
                    val previous = values.put(field, parsedValue)
                    if (previous != null && previous != parsedValue) {
                        throw DateTimeParseException(
                            "Conflict found for field $field",
                            input,
                            index,
                        )
                    }
                    index = parsedEndIndex
                }
                is PatternToken.Localized -> parseTokens(
                    token.patternTokens(locale, effectiveChronology().id),
                    input,
                )
                is PatternToken.Instant -> {
                    val parsed = parsePatternInstant(token, input, index, caseSensitive, strict)
                    mapOf(
                        ChronoField.INSTANT_SECONDS to parsed.instant.epochSecond,
                        ChronoField.NANO_OF_SECOND to parsed.instant.nano.toLong(),
                    ).forEach { (field, value) ->
                        val previous = values.put(field, value)
                        if (previous != null && previous != value) {
                            throw DateTimeParseException(
                                "Conflict found for field $field",
                                input,
                                index,
                            )
                        }
                    }
                    leapSecond = leapSecond || parsed.leapSecond
                    index = parsed.endIndex
                }
                is PatternToken.Composite -> parseTokens(token.tokens, input)
                is PatternToken.Offset -> {
                    val parsed = parseBuilderOffset(token, input, index, caseSensitive, strict)
                    storeOffset(parsed, index, input)
                }
                is PatternToken.LocalizedOffset -> {
                    val startIndex = index
                    if (!input.matchesAt(startIndex, "GMT", caseSensitive)) {
                        throw DateTimeParseException(
                            "Text could not be parsed at index $startIndex",
                            input,
                            startIndex,
                        )
                    }
                    val parsed = parseBuilderOffset(
                        token = token.offsetToken(),
                        text = input,
                        startIndex = startIndex + 3,
                        caseSensitive = caseSensitive,
                        strict = strict,
                    )
                    storeOffset(parsed, startIndex, input)
                }
                is PatternToken.ZoneText -> {
                    val parsed = parsePatternZoneText(
                        token = token,
                        text = input,
                        startIndex = index,
                        locale = locale,
                        caseSensitive = caseSensitive,
                    )
                    if (zone != null && zone != parsed.zone) {
                        throw DateTimeParseException("Conflict found for zone", input, index)
                    }
                    zone = parsed.zone
                    index = parsed.endIndex
                }
                is PatternToken.ZoneId -> {
                    val parsed = parsePatternZone(input, index, caseSensitive)
                    if (zone != null && zone != parsed.zone) {
                        throw DateTimeParseException("Conflict found for zone", input, index)
                    }
                    zone = parsed.zone
                    index = parsed.endIndex
                }
                PatternToken.ChronologyId -> {
                    val parsed = parsePatternChronology(input, index, caseSensitive)
                    parsedChronology = parsed.chronology
                    refreshChronologySensitiveReducedValues()
                    index = parsed.endIndex
                }
                is PatternToken.ChronologyText -> {
                    val parsed = parsePatternChronologyText(
                        token = token,
                        text = input,
                        startIndex = index,
                        locale = locale,
                        caseSensitive = caseSensitive,
                    )
                    parsedChronology = parsed.chronology
                    refreshChronologySensitiveReducedValues()
                    index = parsed.endIndex
                }
                is PatternToken.Optional -> {
                    val previousValues = values.toMap()
                    val previousOffset = offset
                    val previousZone = zone
                    val previousChronology = parsedChronology
                    val previousLeapSecond = leapSecond
                    val previousDayPeriod = dayPeriod
                    val previousReducedValueCount = chronologySensitiveReducedValues.size
                    val previousIndex = index
                    try {
                        parseTokens(token.tokens, input)
                    } catch (_: DateTimeParseException) {
                        values.clear()
                        values.putAll(previousValues)
                        offset = previousOffset
                        zone = previousZone
                        parsedChronology = previousChronology
                        leapSecond = previousLeapSecond
                        dayPeriod = previousDayPeriod
                        while (chronologySensitiveReducedValues.size > previousReducedValueCount) {
                            chronologySensitiveReducedValues.removeLast()
                        }
                        index = previousIndex
                    }
                }
                is PatternToken.Padded -> {
                    val strictAtStart = strict
                    val startIndex = index
                    if (startIndex >= input.length) {
                        throw DateTimeParseException(
                            "Text could not be parsed at index $startIndex",
                            input,
                            startIndex,
                        )
                    }
                    val requestedEnd = startIndex.toLong() + token.width
                    val endIndex = if (requestedEnd > input.length) {
                        if (strictAtStart) {
                            throw DateTimeParseException(
                                "Text could not be parsed at index $startIndex",
                                input,
                                startIndex,
                            )
                        }
                        input.length
                    } else {
                        requestedEnd.toInt()
                    }
                    while (
                        index < endIndex &&
                        input[index].equals(token.padCharacter, ignoreCase = !caseSensitive)
                    ) {
                        index++
                    }
                    parseTokens(listOf(token.token), input.substring(0, endIndex))
                    if (strictAtStart && index != endIndex) {
                        throw DateTimeParseException(
                            "Text could not be parsed at index $startIndex",
                            input,
                            startIndex,
                        )
                    }
                }
                is PatternToken.ParseSetting -> when (token.setting) {
                    ParserSetting.CASE_SENSITIVE -> caseSensitive = true
                    ParserSetting.CASE_INSENSITIVE -> caseSensitive = false
                    ParserSetting.STRICT -> strict = true
                    ParserSetting.LENIENT -> strict = false
                }
                is PatternToken.DefaultValue -> if (token.field !in values) {
                    values[token.field] = token.value
                }
            }
        }
    }
    parseTokens(tokens, text)
    if (index != text.length) {
        throw DateTimeParseException("Text could not be parsed, unparsed text found", text, index)
    }
    val resolvingValues = resolverFields?.let { fields ->
        values.filterKeys(fields::contains)
    } ?: values
    return resolvePatternValues(
        values = resolvingValues,
        text = text,
        resolverStyle = resolverStyle,
        chronology = effectiveChronology(),
        offset = offset.takeIf { ChronoField.OFFSET_SECONDS in resolvingValues },
        zone = zone,
        leapSecond = leapSecond,
        dayPeriod = dayPeriod,
    )
}

private data class ParsedPatternChronology(
    val chronology: Chronology,
    val endIndex: Int,
)

private fun parsePatternChronology(
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
): ParsedPatternChronology {
    val chronology = Chronology.getAvailableChronologies()
        .filter { candidate -> text.matchesAt(startIndex, candidate.id, caseSensitive) }
        .maxByOrNull { candidate -> candidate.id.length }
        ?: throw DateTimeParseException(
            "Text could not be parsed at index $startIndex",
            text,
            startIndex,
        )
    return ParsedPatternChronology(chronology, startIndex + chronology.id.length)
}

private fun parsePatternChronologyText(
    token: PatternToken.ChronologyText,
    text: String,
    startIndex: Int,
    locale: Locale,
    caseSensitive: Boolean,
): ParsedPatternChronology {
    val match = Chronology.getAvailableChronologies()
        .map { chronology -> chronology to chronology.getDisplayName(token.style, locale) }
        .filter { (_, name) -> text.matchesAt(startIndex, name, caseSensitive) }
        .maxByOrNull { (_, name) -> name.length }
        ?: throw DateTimeParseException(
            "Text could not be parsed at index $startIndex",
            text,
            startIndex,
        )
    return ParsedPatternChronology(match.first, startIndex + match.second.length)
}

private data class ParsedPatternInstant(
    val instant: Instant,
    val endIndex: Int,
    val leapSecond: Boolean,
)

private fun parsePatternInstant(
    token: PatternToken.Instant,
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
    strict: Boolean,
): ParsedPatternInstant {
    if (token.fractionalDigits == 0) {
        throw DateTimeParseException(
            "Text could not be parsed at index $startIndex",
            text,
            startIndex,
        )
    }
    for (endIndex in text.length downTo startIndex + 1) {
        val candidate = text.substring(startIndex, endIndex)
        if (caseSensitive && candidate.any { it == 't' || it == 'z' }) continue
        val instant = try {
            Instant.parse(candidate)
        } catch (_: DateTimeException) {
            continue
        }
        val timeSeparator = candidate.indexOfFirst { it == 'T' || it == 't' }
        val fractionStart = timeSeparator + 9
        val fractionDigits = if (candidate.getOrNull(fractionStart) == '.') {
            var digitEnd = fractionStart + 1
            while (candidate.getOrNull(digitEnd) in '0'..'9') digitEnd++
            digitEnd - fractionStart - 1
        } else {
            0
        }
        if (strict && token.fractionalDigits >= 0 && fractionDigits != token.fractionalDigits) {
            continue
        }
        val leapSecond =
            candidate.getOrNull(timeSeparator + 7) == '6' &&
                candidate.getOrNull(timeSeparator + 8) == '0'
        return ParsedPatternInstant(instant, endIndex, leapSecond)
    }
    throw DateTimeParseException(
        "Text could not be parsed at index $startIndex",
        text,
        startIndex,
    )
}

private data class ParsedPatternOffset(
    val offset: ZoneOffset,
    val endIndex: Int,
)

private fun parsePatternOffset(
    token: PatternToken.Field,
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
): ParsedPatternOffset {
    val firstCharacter = text.getOrNull(startIndex)
    if (firstCharacter == 'Z' || !caseSensitive && firstCharacter == 'z') {
        if (token.symbol == 'x' || token.symbol == 'Z' && token.count != 5) {
            throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
        }
        return ParsedPatternOffset(ZoneOffset.UTC, startIndex + 1)
    }
    if (token.symbol == 'Z' && token.count == 4) {
        if (!text.matchesAt(startIndex, "GMT", caseSensitive)) {
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

private fun parseBuilderOffset(
    token: PatternToken.Offset,
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
    strict: Boolean,
): ParsedPatternOffset {
    if (token.noOffsetText.isNotEmpty() && text.matchesAt(startIndex, token.noOffsetText, caseSensitive)) {
        return ParsedPatternOffset(ZoneOffset.UTC, startIndex + token.noOffsetText.length)
    }

    val sign = text.getOrNull(startIndex)
    if (sign != '+' && sign != '-') {
        if (token.noOffsetText.isEmpty()) return ParsedPatternOffset(ZoneOffset.UTC, startIndex)
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }

    val type = validateOffsetPattern(token.pattern)
    var parseType = type
    val style = type % 11
    val paddedHour = type < 11
    var colon = style > 0 && style % 2 == 0
    if (!strict) {
        if (paddedHour) {
            if (colon || type == 0 && text.getOrNull(startIndex + 3) == ':') {
                colon = true
                parseType = 10
            } else {
                parseType = 9
            }
        } else {
            if (colon || type == 11 && (
                    text.getOrNull(startIndex + 2) == ':' ||
                        text.getOrNull(startIndex + 3) == ':'
                    )
            ) {
                colon = true
                parseType = 21
            } else {
                parseType = 20
            }
        }
    }
    val state = IntArray(4)
    state[0] = startIndex + 1
    val valid = when (parseType) {
        0, 11 -> parseOffsetHour(text, paddedHour, state)
        1, 2, 13 -> parseOffsetHour(text, paddedHour, state).also { parsedHour ->
            if (parsedHour) parseOffsetDigits(text, colon, 2, state)
        }
        3, 4, 15 -> parseOffsetHour(text, paddedHour, state) &&
            parseOffsetDigits(text, colon, 2, state)
        5, 6, 17 -> parseOffsetHour(text, paddedHour, state) &&
            parseOffsetDigits(text, colon, 2, state).also { parsedMinute ->
                if (parsedMinute) parseOffsetDigits(text, colon, 3, state)
            }
        7, 8, 19 -> parseOffsetHour(text, paddedHour, state) &&
            parseOffsetDigits(text, colon, 2, state) &&
            parseOffsetDigits(text, colon, 3, state)
        9, 10, 21 -> parseOffsetHour(text, paddedHour, state).also { parsedHour ->
            if (parsedHour && parseOffsetDigits(text, colon, 2, state)) {
                parseOffsetDigits(text, colon, 3, state)
            }
        }
        12 -> parseVariableOffsetDigits(text, 1, 4, state)
        14 -> parseVariableOffsetDigits(text, 3, 4, state)
        16 -> parseVariableOffsetDigits(text, 3, 6, state)
        18 -> parseVariableOffsetDigits(text, 5, 6, state)
        20 -> parseVariableOffsetDigits(text, 1, 6, state)
        else -> error("Unsupported offset pattern: ${token.pattern}")
    }
    if (!valid || state[1] > 23 || state[2] > 59 || state[3] > 59) {
        throw DateTimeParseException("Invalid offset", text, startIndex)
    }
    val direction = if (sign == '-') -1 else 1
    val totalSeconds = direction * (state[1] * 3_600 + state[2] * 60 + state[3])
    return ParsedPatternOffset(ZoneOffset.ofTotalSeconds(totalSeconds), state[0])
}

private fun parseOffsetHour(
    text: String,
    paddedHour: Boolean,
    state: IntArray,
): Boolean = if (paddedHour) {
    parseOffsetDigits(text, colon = false, arrayIndex = 1, state = state)
} else {
    parseVariableOffsetDigits(text, 1, 2, state)
}

private fun parseOffsetDigits(
    text: String,
    colon: Boolean,
    arrayIndex: Int,
    state: IntArray,
): Boolean {
    var position = state[0]
    if (colon && arrayIndex != 1) {
        if (text.getOrNull(position) != ':') return false
        position++
    }
    val first = text.getOrNull(position)
    val second = text.getOrNull(position + 1)
    if (first == null || second == null || first !in '0'..'9' || second !in '0'..'9') return false
    val value = (first.code - '0'.code) * 10 + second.code - '0'.code
    if (value !in 0..59) return false
    state[arrayIndex] = value
    state[0] = position + 2
    return true
}

private fun parseVariableOffsetDigits(
    text: String,
    minDigits: Int,
    maxDigits: Int,
    state: IntArray,
): Boolean {
    var position = state[0]
    val digits = buildString(maxDigits) {
        while (length < maxDigits && text.getOrNull(position) in '0'..'9') {
            append(text[position])
            position++
        }
    }
    if (digits.length < minDigits) return false
    when (digits.length) {
        1 -> state[1] = digits.substring(0, 1).toInt()
        2 -> state[1] = digits.substring(0, 2).toInt()
        3 -> {
            state[1] = digits.substring(0, 1).toInt()
            state[2] = digits.substring(1, 3).toInt()
        }
        4 -> {
            state[1] = digits.substring(0, 2).toInt()
            state[2] = digits.substring(2, 4).toInt()
        }
        5 -> {
            state[1] = digits.substring(0, 1).toInt()
            state[2] = digits.substring(1, 3).toInt()
            state[3] = digits.substring(3, 5).toInt()
        }
        6 -> {
            state[1] = digits.substring(0, 2).toInt()
            state[2] = digits.substring(2, 4).toInt()
            state[3] = digits.substring(4, 6).toInt()
        }
    }
    state[0] = position
    return true
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
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
): ParsedPatternZone {
    for (endIndex in text.length downTo startIndex + 1) {
        parseZoneCandidate(text.substring(startIndex, endIndex), caseSensitive)?.let { zone ->
            return ParsedPatternZone(zone, endIndex)
        }
    }
    throw DateTimeParseException("Invalid zone", text, startIndex)
}

private fun parsePatternZoneText(
    token: PatternToken.ZoneText,
    text: String,
    startIndex: Int,
    locale: Locale,
    caseSensitive: Boolean,
): ParsedPatternZone {
    val localized = parseLocaleZoneText(
        languageTag = locale.toLanguageTag(),
        text = text,
        startIndex = startIndex,
        style = token.style,
        generic = token.generic,
        caseSensitive = caseSensitive,
        preferredZoneIds = token.preferredZoneIds,
    )
    if (localized != null) {
        val parsedZone = try {
            ZoneId.of(localized.zoneId)
        } catch (exception: RuntimeException) {
            throw DateTimeParseException("Invalid zone", text, startIndex, exception)
        }
        return ParsedPatternZone(parsedZone, localized.endIndex)
    }
    return parsePatternZone(text, startIndex, caseSensitive)
}

private fun parseZoneCandidate(
    candidate: String,
    caseSensitive: Boolean,
): ZoneId? {
    try {
        return ZoneId.of(candidate)
    } catch (_: RuntimeException) {
        if (caseSensitive) return null
    }

    val normalized = when {
        candidate.equals("Z", ignoreCase = true) -> "Z"
        candidate.startsWith("UTC", ignoreCase = true) -> "UTC" + candidate.drop(3)
        candidate.startsWith("GMT", ignoreCase = true) -> "GMT" + candidate.drop(3)
        candidate.startsWith("UT", ignoreCase = true) -> "UT" + candidate.drop(2)
        else -> ZoneId.getAvailableZoneIds()
            .firstOrNull { zoneId -> zoneId.equals(candidate, ignoreCase = true) }
            ?: return null
    }
    return try {
        ZoneId.of(normalized)
    } catch (_: RuntimeException) {
        null
    }
}

private fun String.matchesAt(
    startIndex: Int,
    expected: String,
    caseSensitive: Boolean,
): Boolean = startIndex >= 0 && startIndex + expected.length <= length &&
    expected.indices.all { offset ->
        this[startIndex + offset].equals(expected[offset], ignoreCase = !caseSensitive)
    }

private data class ParsedPatternField(
    val value: Long,
    val endIndex: Int,
)

private fun parsePatternText(
    token: PatternToken.Text,
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
    strict: Boolean,
): ParsedPatternField {
    token.parseLookup
        .firstOrNull { (fieldText) -> text.matchesAt(startIndex, fieldText, caseSensitive) }
        ?.let { (fieldText, value) ->
            return ParsedPatternField(value, startIndex + fieldText.length)
        }
    if (strict) {
        throw DateTimeParseException(
            "Text could not be parsed at index $startIndex",
            text,
            startIndex,
        )
    }
    return parsePatternValue(
        tokens = emptyList(),
        tokenIndex = 0,
        token = PatternToken.Value(token.field, 1, 19, SignStyle.NORMAL),
        text = text,
        startIndex = startIndex,
        strict = false,
    )
}

private fun parsePatternLocalizedText(
    token: PatternToken.LocalizedText,
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
    strict: Boolean,
    locale: Locale,
    chronologyId: String,
): ParsedPatternField {
    val field = token.field.toLocaleTextField()
    val candidates = if (field == null) {
        emptyList()
    } else {
        val styles = if (strict) listOf(token.style) else TextStyle.entries
        val valueByText = linkedMapOf<String, Long>()
        styles.forEach { style ->
            localeTextValues(locale.toLanguageTag(), chronologyId, field, style).forEach { value ->
                valueByText[value.text] = value.value
            }
        }
        valueByText.entries
            .sortedByDescending { entry -> entry.key.length }
            .map { entry -> LocaleTextValue(entry.value, entry.key) }
    }
    candidates.firstOrNull { candidate ->
        text.matchesAt(startIndex, candidate.text, caseSensitive)
    }?.let { candidate ->
        return ParsedPatternField(candidate.value, startIndex + candidate.text.length)
    }
    if (strict && candidates.isNotEmpty()) {
        throw DateTimeParseException(
            "Text could not be parsed at index $startIndex",
            text,
            startIndex,
        )
    }
    return parsePatternValue(
        tokens = emptyList(),
        tokenIndex = 0,
        token = PatternToken.Value(token.field, 1, 19, SignStyle.NORMAL),
        text = text,
        startIndex = startIndex,
        strict = false,
    )
}

private data class ParsedPatternDayPeriod(
    val dayPeriod: LocaleDayPeriod,
    val endIndex: Int,
)

private fun parsePatternDayPeriod(
    token: PatternToken.DayPeriod,
    text: String,
    startIndex: Int,
    caseSensitive: Boolean,
    strict: Boolean,
    locale: Locale,
): ParsedPatternDayPeriod {
    val styles = if (strict) {
        listOf(token.style)
    } else {
        listOf(TextStyle.FULL, TextStyle.SHORT, TextStyle.NARROW)
    }
    val candidates = styles
        .flatMap { style -> localeDayPeriods(locale.toLanguageTag(), style) }
        .distinct()
        .sortedByDescending { candidate -> candidate.text.length }
    val candidate = candidates.firstOrNull { value ->
        text.matchesAt(startIndex, value.text, caseSensitive)
    } ?: throw DateTimeParseException(
        "Text could not be parsed at index $startIndex",
        text,
        startIndex,
    )
    return ParsedPatternDayPeriod(candidate, startIndex + candidate.text.length)
}

private data class ParsedPatternFraction(
    val value: Long?,
    val endIndex: Int,
)

private fun parsePatternFraction(
    token: PatternToken.Fraction,
    text: String,
    startIndex: Int,
    strict: Boolean,
): ParsedPatternFraction {
    var index = startIndex
    if (token.decimalPoint) {
        if (text.getOrNull(index) != '.') {
            if (!strict || token.minWidth == 0) return ParsedPatternFraction(null, startIndex)
            throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
        }
        index++
    }

    val minimumWidth = if (strict) token.minWidth else 0
    val maximumWidth = if (strict) token.maxWidth else 9
    val digitsStart = index
    while (index < text.length && text[index] in '0'..'9' && index - digitsStart < maximumWidth) {
        index++
    }
    val digitCount = index - digitsStart
    if (digitCount < minimumWidth) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }
    if (digitCount == 0) {
        return ParsedPatternFraction(token.field.range.minimum, index)
    }

    val numerator = text.substring(digitsStart, index).toLongOrNull()
        ?: throw DateTimeParseException("Invalid fraction", text, startIndex)
    val range = token.field.range
    val rangeSize = range.maximum - range.minimum + 1
    val value = range.minimum + numerator * rangeSize / reducedPowerOfTen(digitCount)
    return ParsedPatternFraction(value, index)
}

private fun parsePatternValue(
    tokens: List<PatternToken>,
    tokenIndex: Int,
    token: PatternToken.Value,
    text: String,
    startIndex: Int,
    strict: Boolean,
): ParsedPatternField {
    var index = startIndex
    val sign = text.getOrNull(index).takeIf { it == '+' || it == '-' }
    val signAllowed = when (sign) {
        '+' -> token.signStyle.acceptsSign(true, strict, token.minWidth == token.maxWidth)
        '-' -> token.signStyle.acceptsSign(false, strict, token.minWidth == token.maxWidth)
        else -> token.signStyle != SignStyle.ALWAYS || !strict
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
    val minimumDigits = if (strict) token.minWidth else 1
    val configuredMaximum = if (strict) token.maxWidth else 9
    val maximumDigits = minOf(configuredMaximum, digitRunEnd - digitsStart - reservedWidth)
    while (index < digitRunEnd && index - digitsStart < maximumDigits) {
        index++
    }
    val digitCount = index - digitsStart
    if (digitCount < minimumDigits ||
        strict && token.signStyle == SignStyle.EXCEEDS_PAD &&
        ((sign == '+' && digitCount <= token.minWidth) || (sign == null && digitCount > token.minWidth))
    ) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }

    val unsigned = text.substring(digitsStart, index)
    val numericText = if (sign == '-') "-$unsigned" else unsigned
    val value = numericText.toLongOrNull()
        ?: throw DateTimeParseException("Invalid numeric value", text, startIndex)
    if (strict && sign == '-' && value == 0L) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }
    return ParsedPatternField(value, index)
}

private fun SignStyle.acceptsSign(
    positive: Boolean,
    strict: Boolean,
    fixedWidth: Boolean,
): Boolean = when (this) {
    SignStyle.NORMAL -> !positive || !strict
    SignStyle.ALWAYS,
    SignStyle.EXCEEDS_PAD,
    -> true
    SignStyle.NEVER,
    SignStyle.NOT_NEGATIVE,
    -> !strict && !fixedWidth
}

private fun PatternToken.adjacentFixedNumericWidth(): Int? = when (this) {
    is PatternToken.Value -> minWidth.takeIf {
        minWidth == maxWidth && signStyle == SignStyle.NOT_NEGATIVE
    }
    is PatternToken.Field -> count.takeIf {
        symbol == 'S' || symbol in listOf('M', 'd', 'H', 'm', 's') && count > 1
    }
    is PatternToken.ReducedValue -> minWidth.takeIf { minWidth == maxWidth }
    is PatternToken.Fraction -> minWidth.takeIf {
        minWidth == maxWidth && !decimalPoint
    }
    is PatternToken.LocalizedWeek -> when (symbol) {
        'Y', 'w' -> count.takeIf { count == 2 }
        'W', 'e', 'c' -> count
        else -> null
    }
    is PatternToken.Text,
    is PatternToken.LocalizedText,
    is PatternToken.DayPeriod,
    is PatternToken.Localized,
    is PatternToken.Instant,
    is PatternToken.Composite,
    is PatternToken.Offset,
    is PatternToken.LocalizedOffset,
    is PatternToken.ZoneText,
    is PatternToken.ZoneId,
    PatternToken.ChronologyId,
    is PatternToken.ChronologyText,
    is PatternToken.ParseSetting,
    is PatternToken.DefaultValue,
    is PatternToken.Optional,
    is PatternToken.Padded,
    is PatternToken.Literal -> null
}

private data class ParsedReducedPatternField(
    val value: Long,
    val endIndex: Int,
    val rawValue: Long,
    val digitCount: Int,
)

private data class ParsedChronologySensitiveReducedValue(
    val token: PatternToken.ReducedValue,
    val rawValue: Long,
    val digitCount: Int,
)

private fun parsePatternReducedValue(
    tokens: List<PatternToken>,
    tokenIndex: Int,
    token: PatternToken.ReducedValue,
    text: String,
    startIndex: Int,
    chronology: Chronology,
    strict: Boolean,
): ParsedReducedPatternField {
    var digitRunEnd = startIndex
    while (digitRunEnd < text.length && text[digitRunEnd] in '0'..'9') digitRunEnd++
    val reservedWidth = tokens.drop(tokenIndex + 1)
        .map { it.adjacentFixedNumericWidth() }
        .takeWhile { it != null }
        .sumOf { it ?: 0 }
    val minimumDigits = if (strict) token.minWidth else 1
    val maximumDigits = if (strict) token.maxWidth else 9
    val digitCount = minOf(maximumDigits, digitRunEnd - startIndex - reservedWidth)
    if (digitCount < minimumDigits) {
        throw DateTimeParseException("Text could not be parsed at index $startIndex", text, startIndex)
    }

    val endIndex = startIndex + digitCount
    val rawValue = text.substring(startIndex, endIndex).toLongOrNull()
        ?: throw DateTimeParseException("Invalid numeric value", text, startIndex)
    return ParsedReducedPatternField(
        value = token.resolveReducedValue(rawValue, digitCount, chronology),
        endIndex = endIndex,
        rawValue = rawValue,
        digitCount = digitCount,
    )
}

private fun PatternToken.ReducedValue.resolveReducedValue(
    rawValue: Long,
    digitCount: Int,
    chronology: Chronology,
): Long {
    if (digitCount != minWidth) return rawValue
    val baseValue = resolveBaseValue(chronology)
    val range = reducedPowerOfTen(minWidth)
    val lastPart = baseValue % range
    val basePart = baseValue - lastPart
    var value = if (baseValue > 0) basePart + rawValue else basePart - rawValue
    if (value < baseValue) value += range
    return value
}

private fun PatternToken.ReducedValue.resolveBaseValue(chronology: Chronology): Int =
    when (val reducedBase = base) {
        is ReducedValueBase.Value -> reducedBase.value
        is ReducedValueBase.Date -> chronology.date(reducedBase.date).get(field)
    }

private fun parsePatternField(
    token: PatternToken.Field,
    text: String,
    startIndex: Int,
    strict: Boolean,
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
    val minimumDigits = if (strict && fixedWidth) token.count else 1
    val maximumDigits = when {
        strict && fixedWidth -> token.count
        !strict -> 9
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
    chronology: Chronology,
    offset: ZoneOffset?,
    zone: ZoneId?,
    leapSecond: Boolean,
    dayPeriod: LocaleDayPeriod?,
): TemporalAccessor {
    val fieldValues = values.toMutableMap()
    try {
        resolveTimeFields(fieldValues, resolverStyle, dayPeriod)
    } catch (exception: RuntimeException) {
        throw DateTimeParseException("Text cannot be parsed to a time", text, 0, exception)
    }
    val resolvedOffset = offset ?: fieldValues[ChronoField.OFFSET_SECONDS]?.let { totalSeconds ->
        try {
            ZoneOffset.ofTotalSeconds(ChronoField.OFFSET_SECONDS.checkValidIntValue(totalSeconds))
        } catch (exception: RuntimeException) {
            throw DateTimeParseException("Invalid value for OffsetSeconds: $totalSeconds", text, 0, exception)
        }
    }
    val instant = fieldValues[ChronoField.INSTANT_SECONDS]?.let { epochSecond ->
        try {
            val nano = fieldValues[ChronoField.NANO_OF_SECOND]
                ?.let(ChronoField.NANO_OF_SECOND::checkValidIntValue) ?: 0
            Instant.ofEpochSecond(epochSecond, nano.toLong())
        } catch (exception: RuntimeException) {
            throw DateTimeParseException("Invalid instant fields", text, 0, exception)
        }
    }
    val year = try {
        resolvePatternYear(fieldValues, chronology, resolverStyle)
    } catch (exception: RuntimeException) {
        throw DateTimeParseException("Text cannot be parsed to a date", text, 0, exception)
    }
    val month = fieldValues[ChronoField.MONTH_OF_YEAR]
    val day = fieldValues[ChronoField.DAY_OF_MONTH]
    val calendarDate = if (year != null && month != null && day != null) {
        try {
            resolveDateFieldsInChronology(
                chronology = chronology,
                year = year.toPatternInt('u', text),
                month = month.toPatternInt('M', text),
                day = day.toPatternInt('d', text),
                resolverStyle = resolverStyle,
            )
        } catch (exception: RuntimeException) {
            throw DateTimeParseException("Text cannot be parsed to a date", text, 0, exception)
        }
    } else {
        null
    }
    val dayOfYear = fieldValues[ChronoField.DAY_OF_YEAR]
    val ordinalDate = if (year != null && dayOfYear != null) {
        try {
            resolveOrdinalDateInChronology(
                chronology = chronology,
                year = year.toPatternInt('u', text),
                dayOfYear = dayOfYear.toPatternInt('D', text),
                resolverStyle = resolverStyle,
            )
        } catch (exception: RuntimeException) {
            throw DateTimeParseException("Text cannot be parsed to a date", text, 0, exception)
        }
    } else {
        null
    }
    val customDates = try {
        resolveCustomDateFields(fieldValues, chronology, resolverStyle)
    } catch (exception: RuntimeException) {
        throw DateTimeParseException("Text cannot be parsed to a date", text, 0, exception)
    }
    val dayOfWeek = fieldValues[ChronoField.DAY_OF_WEEK]
    val resolvedDates = listOfNotNull(calendarDate, ordinalDate) + customDates
    val date = resolvedDates.firstOrNull()
    if (
        date != null &&
        resolvedDates.any { candidate -> candidate.toEpochDay() != date.toEpochDay() }
    ) {
        throw DateTimeParseException("Conflict found: resolved dates differ", text, 0)
    }
    if (
        date != null &&
        dayOfWeek != null &&
        date.getLong(ChronoField.DAY_OF_WEEK) != dayOfWeek
    ) {
        throw DateTimeParseException(
            "Conflict found: resolved day-of-week differs from parsed value",
            text,
            0,
        )
    }

    val hour = fieldValues[ChronoField.HOUR_OF_DAY]
    val minute = fieldValues[ChronoField.MINUTE_OF_HOUR]
    val second = fieldValues[ChronoField.SECOND_OF_MINUTE]
    val fraction = fieldValues[ChronoField.NANO_OF_SECOND]
    val hasResolvableTimeFields = hour != null &&
        (minute != null || second == null && fraction == null) &&
        (second != null || fraction == null)
    val resolvedTime = if (hasResolvableTimeFields) {
        resolvePatternTime(
            hour = requireNotNull(hour),
            minute = minute ?: 0,
            second = second ?: 0,
            nano = fraction ?: 0,
            resolverStyle = resolverStyle,
            text = text,
        )
    } else {
        null
    }

    val resolvedDate = if (date != null && resolvedTime != null) {
        date.plus(resolvedTime.excessDays.days.toLong(), ChronoUnit.DAYS)
    } else {
        date
    }
    return ParsedTemporalAccessor(
        date = resolvedDate,
        time = resolvedTime?.time,
        offset = resolvedOffset,
        zone = zone,
        instant = instant,
        chronology = chronology,
        fields = fieldValues,
        excessDays = if (date == null) resolvedTime?.excessDays ?: Period.ZERO else Period.ZERO,
        leapSecond = leapSecond,
    )
}

private fun resolveCustomDateFields(
    fieldValues: MutableMap<TemporalField, Long>,
    chronology: Chronology,
    resolverStyle: ResolverStyle,
): List<ChronoLocalDate> {
    val partialTemporal = ParsedTemporalAccessor(
        chronology = chronology,
        fields = fieldValues,
    )
    val resolvedDates = mutableListOf<ChronoLocalDate>()
    repeat(MAX_FIELD_RESOLVE_PASSES) {
        var changed = false
        for (field in fieldValues.keys.toList()) {
            val previousValues = fieldValues.toMap()
            val resolved = field.resolve(fieldValues, partialTemporal, resolverStyle)
            resolved?.let { temporal ->
                resolvedDates += if (temporal is ChronoLocalDate) {
                    temporal
                } else {
                    chronology.date(temporal)
                }
            }
            if (resolved != null || fieldValues != previousValues) {
                changed = true
                break
            }
        }
        if (!changed) return resolvedDates
    }
    throw DateTimeException("One of the parsed fields has an incorrectly implemented resolve method")
}

private const val MAX_FIELD_RESOLVE_PASSES: Int = 50

private fun resolveTimeFields(
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
    dayPeriod: LocaleDayPeriod?,
) {
    var remainingDayPeriod = dayPeriod
    fieldValues.remove(ChronoField.CLOCK_HOUR_OF_DAY)?.let { clockHour ->
        if (
            resolverStyle == ResolverStyle.STRICT ||
            resolverStyle == ResolverStyle.SMART && clockHour != 0L
        ) {
            ChronoField.CLOCK_HOUR_OF_DAY.checkValidValue(clockHour)
        }
        fieldValues.updateTimeField(
            source = ChronoField.CLOCK_HOUR_OF_DAY,
            target = ChronoField.HOUR_OF_DAY,
            value = if (clockHour == 24L) 0 else clockHour,
        )
    }
    fieldValues.remove(ChronoField.CLOCK_HOUR_OF_AMPM)?.let { clockHour ->
        if (
            resolverStyle == ResolverStyle.STRICT ||
            resolverStyle == ResolverStyle.SMART && clockHour != 0L
        ) {
            ChronoField.CLOCK_HOUR_OF_AMPM.checkValidValue(clockHour)
        }
        fieldValues.updateTimeField(
            source = ChronoField.CLOCK_HOUR_OF_AMPM,
            target = ChronoField.HOUR_OF_AMPM,
            value = if (clockHour == 12L) 0 else clockHour,
        )
    }
    if (
        ChronoField.AMPM_OF_DAY in fieldValues &&
        ChronoField.HOUR_OF_AMPM in fieldValues
    ) {
        val amPm = requireNotNull(fieldValues.remove(ChronoField.AMPM_OF_DAY))
        val hour = requireNotNull(fieldValues.remove(ChronoField.HOUR_OF_AMPM))
        val hourOfDay = if (resolverStyle == ResolverStyle.LENIENT) {
            addExact(multiplyExact(amPm, 12), hour)
        } else {
            ChronoField.AMPM_OF_DAY.checkValidValue(amPm)
            ChronoField.HOUR_OF_AMPM.checkValidValue(hour)
            amPm * 12 + hour
        }
        fieldValues.updateTimeField(
            source = ChronoField.AMPM_OF_DAY,
            target = ChronoField.HOUR_OF_DAY,
            value = hourOfDay,
        )
    }
    fieldValues.remove(ChronoField.NANO_OF_DAY)?.let { nanoOfDay ->
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.NANO_OF_DAY.checkValidValue(nanoOfDay)
        }
        fieldValues.updateTimeField(
            ChronoField.NANO_OF_DAY,
            ChronoField.HOUR_OF_DAY,
            nanoOfDay / NANOS_PER_HOUR,
        )
        fieldValues.updateTimeField(
            ChronoField.NANO_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            nanoOfDay / NANOS_PER_MINUTE % 60,
        )
        fieldValues.updateTimeField(
            ChronoField.NANO_OF_DAY,
            ChronoField.SECOND_OF_MINUTE,
            nanoOfDay / NANOS_PER_SECOND % 60,
        )
        fieldValues.updateTimeField(
            ChronoField.NANO_OF_DAY,
            ChronoField.NANO_OF_SECOND,
            nanoOfDay % NANOS_PER_SECOND,
        )
    }
    fieldValues.remove(ChronoField.MICRO_OF_DAY)?.let { microOfDay ->
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.MICRO_OF_DAY.checkValidValue(microOfDay)
        }
        fieldValues.updateTimeField(
            ChronoField.MICRO_OF_DAY,
            ChronoField.SECOND_OF_DAY,
            microOfDay / MICROS_PER_SECOND,
        )
        fieldValues.updateTimeField(
            ChronoField.MICRO_OF_DAY,
            ChronoField.MICRO_OF_SECOND,
            microOfDay % MICROS_PER_SECOND,
        )
    }
    fieldValues.remove(ChronoField.MILLI_OF_DAY)?.let { milliOfDay ->
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.MILLI_OF_DAY.checkValidValue(milliOfDay)
        }
        fieldValues.updateTimeField(
            ChronoField.MILLI_OF_DAY,
            ChronoField.SECOND_OF_DAY,
            milliOfDay / MILLIS_PER_SECOND,
        )
        fieldValues.updateTimeField(
            ChronoField.MILLI_OF_DAY,
            ChronoField.MILLI_OF_SECOND,
            milliOfDay % MILLIS_PER_SECOND,
        )
    }
    fieldValues.remove(ChronoField.SECOND_OF_DAY)?.let { secondOfDay ->
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.SECOND_OF_DAY.checkValidValue(secondOfDay)
        }
        fieldValues.updateTimeField(
            ChronoField.SECOND_OF_DAY,
            ChronoField.HOUR_OF_DAY,
            secondOfDay / SECONDS_PER_HOUR,
        )
        fieldValues.updateTimeField(
            ChronoField.SECOND_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            secondOfDay / SECONDS_PER_MINUTE % 60,
        )
        fieldValues.updateTimeField(
            ChronoField.SECOND_OF_DAY,
            ChronoField.SECOND_OF_MINUTE,
            secondOfDay % SECONDS_PER_MINUTE,
        )
    }
    fieldValues.remove(ChronoField.MINUTE_OF_DAY)?.let { minuteOfDay ->
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.MINUTE_OF_DAY.checkValidValue(minuteOfDay)
        }
        fieldValues.updateTimeField(
            ChronoField.MINUTE_OF_DAY,
            ChronoField.HOUR_OF_DAY,
            minuteOfDay / MINUTES_PER_HOUR,
        )
        fieldValues.updateTimeField(
            ChronoField.MINUTE_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            minuteOfDay % MINUTES_PER_HOUR,
        )
    }

    val nano = fieldValues[ChronoField.NANO_OF_SECOND]
    if (nano != null) {
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.NANO_OF_SECOND.checkValidValue(nano)
        }
        fieldValues.remove(ChronoField.MICRO_OF_SECOND)?.let { micro ->
            if (resolverStyle != ResolverStyle.LENIENT) {
                ChronoField.MICRO_OF_SECOND.checkValidValue(micro)
            }
            fieldValues.updateTimeField(
                ChronoField.MICRO_OF_SECOND,
                ChronoField.NANO_OF_SECOND,
                micro * NANOS_PER_MICRO + nano % NANOS_PER_MICRO,
            )
        }
        fieldValues.remove(ChronoField.MILLI_OF_SECOND)?.let { milli ->
            if (resolverStyle != ResolverStyle.LENIENT) {
                ChronoField.MILLI_OF_SECOND.checkValidValue(milli)
            }
            val currentNano = requireNotNull(fieldValues[ChronoField.NANO_OF_SECOND])
            fieldValues.updateTimeField(
                ChronoField.MILLI_OF_SECOND,
                ChronoField.NANO_OF_SECOND,
                milli * NANOS_PER_MILLI + currentNano % NANOS_PER_MILLI,
            )
        }
    } else {
        val milli = fieldValues.remove(ChronoField.MILLI_OF_SECOND)
        val micro = fieldValues.remove(ChronoField.MICRO_OF_SECOND)
        if (resolverStyle != ResolverStyle.LENIENT) {
            milli?.let(ChronoField.MILLI_OF_SECOND::checkValidValue)
            micro?.let(ChronoField.MICRO_OF_SECOND::checkValidValue)
        }
        when {
            milli != null && micro != null -> {
                val mergedMicro = milli * MICROS_PER_MILLI + micro % MICROS_PER_MILLI
                if (micro != mergedMicro) {
                    throw DateTimeException(
                        "Conflict found: ${ChronoField.MICRO_OF_SECOND} $micro differs from " +
                            "${ChronoField.MICRO_OF_SECOND} $mergedMicro while resolving " +
                            ChronoField.MILLI_OF_SECOND,
                    )
                }
                fieldValues[ChronoField.NANO_OF_SECOND] = mergedMicro * NANOS_PER_MICRO
            }
            milli != null -> fieldValues[ChronoField.NANO_OF_SECOND] = milli * NANOS_PER_MILLI
            micro != null -> fieldValues[ChronoField.NANO_OF_SECOND] = micro * NANOS_PER_MICRO
        }
    }

    if (remainingDayPeriod != null && ChronoField.HOUR_OF_AMPM in fieldValues) {
        val period = requireNotNull(remainingDayPeriod)
        val hourOfAmPm = requireNotNull(fieldValues.remove(ChronoField.HOUR_OF_AMPM))
        if (resolverStyle != ResolverStyle.LENIENT) {
            ChronoField.HOUR_OF_AMPM.checkValidValue(hourOfAmPm)
        }
        val minute = floorMod(fieldValues[ChronoField.MINUTE_OF_HOUR] ?: 0, 60)
        val pmMinute = (floorMod(hourOfAmPm, 12) + 12) * 60 + minute
        val hourOfDay = addExact(hourOfAmPm, if (period.includes(pmMinute)) 12 else 0)
        fieldValues.updateTimeField(
            ChronoField.HOUR_OF_AMPM,
            ChronoField.HOUR_OF_DAY,
            hourOfDay,
        )
        remainingDayPeriod = null
    }

    if (
        resolverStyle != ResolverStyle.STRICT &&
        ChronoField.HOUR_OF_DAY !in fieldValues &&
        ChronoField.MINUTE_OF_HOUR !in fieldValues &&
        ChronoField.SECOND_OF_MINUTE !in fieldValues &&
        ChronoField.NANO_OF_SECOND !in fieldValues
    ) {
        if (remainingDayPeriod != null) {
            val midpoint = requireNotNull(remainingDayPeriod).midpoint()
            fieldValues[ChronoField.HOUR_OF_DAY] = (midpoint / 60).toLong()
            fieldValues[ChronoField.MINUTE_OF_HOUR] = (midpoint % 60).toLong()
            remainingDayPeriod = null
        } else {
            fieldValues.remove(ChronoField.AMPM_OF_DAY)?.let { amPm ->
                val hour = if (resolverStyle == ResolverStyle.LENIENT) {
                    addExact(multiplyExact(amPm, 12), 6)
                } else {
                    ChronoField.AMPM_OF_DAY.checkValidValue(amPm)
                    amPm * 12 + 6
                }
                fieldValues[ChronoField.HOUR_OF_DAY] = hour
            }
        }
    }

    val resolvedHour = fieldValues[ChronoField.HOUR_OF_DAY]
    if (
        remainingDayPeriod != null &&
        resolvedHour != null &&
        resolverStyle != ResolverStyle.LENIENT
    ) {
        val minute = fieldValues[ChronoField.MINUTE_OF_HOUR] ?: 0
        if (!requireNotNull(remainingDayPeriod).includes(resolvedHour * 60 + minute)) {
            throw DateTimeException(
                "Conflict found: resolved time conflicts with day period ${remainingDayPeriod.text}",
            )
        }
    }
}

private fun MutableMap<TemporalField, Long>.updateTimeField(
    source: TemporalField,
    target: TemporalField,
    value: Long,
) {
    val previous = put(target, value)
    if (previous != null && previous != value) {
        throw DateTimeException(
            "Conflict found: $target $previous differs from $target $value while resolving $source",
        )
    }
}

private fun resolvePatternYear(
    values: MutableMap<TemporalField, Long>,
    chronology: Chronology,
    resolverStyle: ResolverStyle,
): Long? {
    val prolepticYear = values[ChronoField.YEAR]
    val yearOfEraValue = values[ChronoField.YEAR_OF_ERA]
    val eraValue = values[ChronoField.ERA]
    if (yearOfEraValue == null) {
        eraValue?.let { chronology.range(ChronoField.ERA).checkValidValue(it, ChronoField.ERA) }
        return prolepticYear
    }

    val yearOfEra = if (resolverStyle == ResolverStyle.LENIENT) {
        yearOfEraValue.toInt().takeIf { it.toLong() == yearOfEraValue }
            ?: throw DateTimeException("Invalid value for YearOfEra: $yearOfEraValue")
    } else {
        chronology.range(ChronoField.YEAR_OF_ERA)
            .checkValidIntValue(yearOfEraValue, ChronoField.YEAR_OF_ERA)
    }
    val resolvedYear = when {
        eraValue != null -> {
            val era = chronology.eraOf(
                chronology.range(ChronoField.ERA)
                    .checkValidIntValue(eraValue, ChronoField.ERA),
            )
            chronology.prolepticYear(era, yearOfEra).toLong()
        }
        prolepticYear != null -> {
            val year = chronology.range(ChronoField.YEAR)
                .checkValidIntValue(prolepticYear, ChronoField.YEAR)
            chronology.prolepticYear(chronology.dateYearDay(year, 1).era, yearOfEra).toLong()
        }
        resolverStyle == ResolverStyle.STRICT -> return null
        else -> chronology.eras().lastOrNull()
            ?.let { era -> chronology.prolepticYear(era, yearOfEra).toLong() }
            ?: yearOfEra.toLong()
    }
    if (prolepticYear != null && prolepticYear != resolvedYear) {
        throw DateTimeException(
            "Conflict found: Year $prolepticYear differs from YearOfEra $yearOfEraValue",
        )
    }
    values.remove(ChronoField.YEAR_OF_ERA)
    values.remove(ChronoField.ERA)
    values[ChronoField.YEAR] = resolvedYear
    return resolvedYear
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

private const val NANOS_PER_MICRO: Long = 1_000
private const val NANOS_PER_MILLI: Long = 1_000_000
private const val NANOS_PER_SECOND: Long = 1_000_000_000
private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
private const val NANOS_PER_HOUR: Long = 60 * NANOS_PER_MINUTE
private const val NANOS_PER_DAY: Long = 24 * NANOS_PER_HOUR
private const val MICROS_PER_MILLI: Long = 1_000
private const val MICROS_PER_SECOND: Long = 1_000_000
private const val MILLIS_PER_SECOND: Long = 1_000
private const val SECONDS_PER_MINUTE: Long = 60
private const val SECONDS_PER_HOUR: Long = 60 * SECONDS_PER_MINUTE
private const val MINUTES_PER_HOUR: Long = 60

private fun resolvePatternTime(
    hour: Long,
    minute: Long,
    second: Long,
    nano: Long,
    resolverStyle: ResolverStyle,
    text: String,
): ResolvedTime = try {
    when (resolverStyle) {
        ResolverStyle.STRICT -> ResolvedTime(
            LocalTime.of(
                ChronoField.HOUR_OF_DAY.checkValidIntValue(hour),
                ChronoField.MINUTE_OF_HOUR.checkValidIntValue(minute),
                ChronoField.SECOND_OF_MINUTE.checkValidIntValue(second),
                ChronoField.NANO_OF_SECOND.checkValidIntValue(nano),
            ),
            Period.ZERO,
        )
        ResolverStyle.SMART -> if (hour == 24L && minute == 0L && second == 0L && nano == 0L) {
            ResolvedTime(LocalTime.MIDNIGHT, Period.ofDays(1))
        } else {
            ResolvedTime(
                LocalTime.of(
                    ChronoField.HOUR_OF_DAY.checkValidIntValue(hour),
                    ChronoField.MINUTE_OF_HOUR.checkValidIntValue(minute),
                    ChronoField.SECOND_OF_MINUTE.checkValidIntValue(second),
                    ChronoField.NANO_OF_SECOND.checkValidIntValue(nano),
                ),
                Period.ZERO,
            )
        }
        ResolverStyle.LENIENT -> {
            val totalNanos = addExact(
                addExact(
                    addExact(
                        multiplyExact(hour, NANOS_PER_HOUR),
                        multiplyExact(minute, NANOS_PER_MINUTE),
                    ),
                    multiplyExact(second, NANOS_PER_SECOND),
                ),
                nano,
            )
            ResolvedTime(
                time = LocalTime.ofNanoOfDay(floorMod(totalNanos, NANOS_PER_DAY)),
                excessDays = Period.ofDays(floorDiv(totalNanos, NANOS_PER_DAY).toInt()),
            )
        }
    }
} catch (exception: RuntimeException) {
    throw DateTimeParseException("Text cannot be parsed to a time", text, 0, exception)
}

private fun describePattern(tokens: List<PatternToken>): String = buildString {
    tokens.forEach { token ->
        when (token) {
            is PatternToken.Literal -> append('\'')
                .append(token.text.replace("'", "''"))
                .append('\'')
            is PatternToken.Field -> append(describePatternField(token))
            is PatternToken.Value -> append(describePatternValue(token))
            is PatternToken.ReducedValue -> append("ReducedValue(")
                .append(token.field)
                .append(',')
                .append(token.minWidth)
                .append(',')
                .append(token.maxWidth)
                .append(',')
                .append(
                    when (val reducedBase = token.base) {
                        is ReducedValueBase.Value -> reducedBase.value
                        is ReducedValueBase.Date -> reducedBase.date
                    },
                )
                .append(')')
            is PatternToken.Fraction -> append("Fraction(")
                .append(token.field)
                .append(',')
                .append(token.minWidth)
                .append(',')
                .append(token.maxWidth)
                .append(',')
                .append(if (token.decimalPoint) "DecimalPoint" else "")
                .append(')')
            is PatternToken.Text -> append("Text(")
                .append(token.field)
                .append(')')
            is PatternToken.LocalizedText -> append("Text(")
                .append(token.field)
                .apply {
                    if (token.style != TextStyle.FULL) append(',').append(token.style)
                }
                .append(')')
            is PatternToken.DayPeriod -> append("DayPeriod(")
                .append(token.style)
                .append(')')
            is PatternToken.LocalizedWeek -> append(describeLocalizedWeek(token))
            is PatternToken.Localized -> append("Localized(")
                .apply {
                    if (token.requestedTemplate != null) {
                        append(token.requestedTemplate)
                    } else {
                        append(token.dateStyle ?: "")
                        append(',')
                        append(token.timeStyle ?: "")
                    }
                }
                .append(')')
            is PatternToken.Instant -> append("Instant()")
            is PatternToken.Composite -> append('(')
                .append(token.description)
                .append(')')
            is PatternToken.Offset -> append("Offset(")
                .append(token.pattern)
                .append(",'")
                .append(token.noOffsetText.replace("'", "''"))
                .append("')")
            is PatternToken.LocalizedOffset -> append("LocalizedOffset(")
                .append(token.style)
                .append(')')
            is PatternToken.ZoneText -> append("ZoneText(")
                .append(token.style)
                .append(')')
            is PatternToken.ZoneId -> append(
                when (token.queryMode) {
                    ZoneQueryMode.ZONE_ID -> "ZoneId()"
                    ZoneQueryMode.REGION_ONLY -> "ZoneRegionId()"
                    ZoneQueryMode.ZONE_OR_OFFSET -> "ZoneOrOffsetId()"
                },
            )
            PatternToken.ChronologyId -> append("ChronologyId()")
            is PatternToken.ChronologyText -> append("ChronologyText(")
                .append(token.style)
                .append(')')
            is PatternToken.ParseSetting -> append(
                when (token.setting) {
                    ParserSetting.CASE_SENSITIVE -> "ParseCaseSensitive(true)"
                    ParserSetting.CASE_INSENSITIVE -> "ParseCaseSensitive(false)"
                    ParserSetting.STRICT -> "ParseStrict(true)"
                    ParserSetting.LENIENT -> "ParseStrict(false)"
                },
            )
            is PatternToken.DefaultValue -> append("DefaultValue(")
                .append(token.field)
                .append(',')
                .append(token.value)
                .append(')')
            is PatternToken.Optional -> append('[')
                .append(
                    when (val optionalToken = token.tokens.singleOrNull()) {
                        is PatternToken.Composite -> optionalToken.description
                        else -> describePattern(token.tokens)
                    },
                )
                .append(']')
            is PatternToken.Padded -> append("Pad(")
                .append(describePattern(listOf(token.token)))
                .append(',')
                .append(token.width)
                .apply {
                    if (token.padCharacter != ' ') {
                        append(",'").append(token.padCharacter).append('\'')
                    }
                }
                .append(')')
        }
    }
}

private fun TemporalField.toLocaleTextField(): LocaleTextField? = when (this) {
    ChronoField.ERA -> LocaleTextField.ERA
    ChronoField.MONTH_OF_YEAR -> LocaleTextField.MONTH_OF_YEAR
    ChronoField.DAY_OF_WEEK -> LocaleTextField.DAY_OF_WEEK
    ChronoField.AMPM_OF_DAY -> LocaleTextField.AMPM_OF_DAY
    IsoFields.QUARTER_OF_YEAR -> LocaleTextField.QUARTER_OF_YEAR
    else -> null
}

private fun describeLocalizedWeek(token: PatternToken.LocalizedWeek): String = buildString {
    append("Localized(")
    if (token.symbol == 'Y') {
        when (token.count) {
            1 -> append("WeekBasedYear")
            2 -> append("ReducedValue(WeekBasedYear,2,2,2000-01-01)")
            else -> append("WeekBasedYear,")
                .append(token.count)
                .append(",19,")
                .append(if (token.count < 4) SignStyle.NORMAL else SignStyle.EXCEEDS_PAD)
        }
    } else {
        append(
            when (token.symbol) {
                'e', 'c' -> "DayOfWeek"
                'w' -> "WeekOfWeekBasedYear"
                'W' -> "WeekOfMonth"
                else -> error("Unsupported localized week pattern: ${token.symbol}")
            },
        )
        append(',').append(token.count)
    }
    append(')')
}

private fun describePatternValue(token: PatternToken.Value): String = when {
    token.minWidth == 1 && token.maxWidth == 19 && token.signStyle == SignStyle.NORMAL ->
        "Value(${token.field})"
    token.minWidth == token.maxWidth && token.signStyle == SignStyle.NOT_NEGATIVE ->
        "Value(${token.field},${token.minWidth})"
    else -> "Value(${token.field},${token.minWidth},${token.maxWidth},${token.signStyle})"
}

private fun describePatternField(token: PatternToken.Field): String {
    val field = when (token.symbol) {
        'u' -> ChronoField.YEAR
        'y' -> ChronoField.YEAR_OF_ERA
        'M' -> ChronoField.MONTH_OF_YEAR
        'd' -> ChronoField.DAY_OF_MONTH
        'H' -> ChronoField.HOUR_OF_DAY
        'm' -> ChronoField.MINUTE_OF_HOUR
        's' -> ChronoField.SECOND_OF_MINUTE
        'S' -> ChronoField.NANO_OF_SECOND
        'V' -> return "ZoneId()"
        else -> return "Value(${token.symbol},${token.count})"
    }
    return when {
        token.symbol in listOf('u', 'y') && token.count == 2 ->
            "ReducedValue($field,2,2,2000-01-01)"
        token.symbol in listOf('u', 'y') -> {
            val signStyle = if (token.count < 4) SignStyle.NORMAL else SignStyle.EXCEEDS_PAD
            if (token.count == 1) {
                "Value($field)"
            } else {
                "Value($field,${token.count},19,$signStyle)"
            }
        }
        token.symbol == 'S' -> "Fraction($field,${token.count},${token.count})"
        token.count == 1 -> "Value($field)"
        else -> "Value($field,${token.count})"
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
        val fieldValues = mutableMapOf<TemporalField, Long>(
            IsoFields.WEEK_BASED_YEAR to weekBasedYear.toLong(),
            IsoFields.WEEK_OF_WEEK_BASED_YEAR to week.toLong(),
            ChronoField.DAY_OF_WEEK to dayOfWeek.toLong(),
        )
        LocalDate.from(
            requireNotNull(
                IsoFields.WEEK_OF_WEEK_BASED_YEAR.resolve(
                    fieldValues,
                    LocalDate.EPOCH,
                    resolverStyle,
                ),
            ),
        )
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
): ChronoLocalDate = resolveDateFieldsInChronology(
    chronology = chronology,
    year = parsedDate.get(ChronoField.YEAR),
    month = parsedDate.get(ChronoField.MONTH_OF_YEAR),
    day = parsedDate.get(ChronoField.DAY_OF_MONTH),
    resolverStyle = resolverStyle,
)

private fun resolveDateFieldsInChronology(
    chronology: Chronology,
    year: Int,
    month: Int,
    day: Int,
    resolverStyle: ResolverStyle,
): ChronoLocalDate = when (resolverStyle) {
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

private fun resolveOrdinalDateInChronology(
    chronology: Chronology,
    year: Int,
    dayOfYear: Int,
    resolverStyle: ResolverStyle,
): ChronoLocalDate = if (resolverStyle == ResolverStyle.LENIENT) {
    chronology.dateYearDay(year, 1).plus(dayOfYear.toLong() - 1, ChronoUnit.DAYS)
} else {
    chronology.dateYearDay(year, dayOfYear)
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
