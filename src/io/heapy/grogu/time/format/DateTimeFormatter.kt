package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.temporal.ChronoField
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
) {
    /** Formats [temporal] into a string. */
    public fun format(temporal: TemporalAccessor): String = printer(temporal)

    /** Formats [temporal] and appends the result to [appendable]. */
    public fun formatTo(temporal: TemporalAccessor, appendable: Appendable) {
        appendable.append(format(temporal))
    }

    /** Parses [text] into a temporal accessor. */
    public fun parse(text: CharSequence): TemporalAccessor = parser(text)

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

    override fun toString(): String = description

    public companion object {
        /** The strict ISO formatter for a date without a time or offset. */
        public val ISO_LOCAL_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> LocalDate.from(temporal).toString() },
            parser = { text -> LocalDate.parse(text) },
            description =
                "Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)'-'Value(DayOfMonth,2)",
        )

        /** The strict ISO formatter for a date with a required offset. */
        public val ISO_OFFSET_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                LocalDate.from(temporal).toString() + ZoneOffset.from(temporal)
            },
            parser = { text -> parseIsoDate(text, offsetRequired = true) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))Offset(+HH:MM:ss,'Z')",
        )

        /** The strict ISO formatter for a date with an optional offset. */
        public val ISO_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(LocalDate.from(temporal))
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
        )

        /** The strict ISO formatter for a time without a date or offset. */
        public val ISO_LOCAL_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> formatIsoLocalTime(LocalTime.from(temporal)) },
            parser = { text -> LocalTime.parse(text) },
            description =
                "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]",
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
        )

        /** The strict ISO formatter for a date-time without an offset. */
        public val ISO_LOCAL_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                formatIsoLocalDateTime(LocalDateTime.from(temporal))
            },
            parser = { text -> LocalDateTime.parse(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "(Value(Year,4,10,EXCEEDS_PAD)'-'Value(MonthOfYear,2)" +
                    "'-'Value(DayOfMonth,2))'T'" +
                    "(Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]])",
        )

        /** The strict ISO formatter for a date-time with an optional offset and region zone. */
        public val ISO_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                buildString {
                    append(formatIsoLocalDateTime(LocalDateTime.from(temporal)))
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
        )

        /** The strict ISO formatter for a year and day-of-year with an optional offset. */
        public val ISO_ORDINAL_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val date = LocalDate.from(temporal)
                buildString {
                    append(formatIsoYear(date.year))
                    append('-')
                    append(date.dayOfYear.toString().padStart(3, '0'))
                    appendOptionalIsoOffset(temporal)
                }
            },
            parser = { text -> parseIsoOrdinalDate(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "Value(Year,4,10,EXCEEDS_PAD)'-'Value(DayOfYear,3)" +
                    "[Offset(+HH:MM:ss,'Z')]",
        )

        /** The strict ISO formatter for a week-based date with an optional offset. */
        public val ISO_WEEK_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val date = LocalDate.from(temporal)
                buildString {
                    append(formatIsoYear(date.get(IsoFields.WEEK_BASED_YEAR)))
                    append("-W")
                    append(date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0'))
                    append('-')
                    append(date.dayOfWeek.value)
                    appendOptionalIsoOffset(temporal)
                }
            },
            parser = { text -> parseIsoWeekDate(text) },
            description =
                "ParseCaseSensitive(false)" +
                    "Value(WeekBasedYear,4,10,EXCEEDS_PAD)'-W'" +
                    "Value(WeekOfWeekBasedYear,2)'-'Value(DayOfWeek,1)" +
                    "[Offset(+HH:MM:ss,'Z')]",
        )

        /** The strict basic ISO date formatter with an optional compact offset. */
        public val BASIC_ISO_DATE: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val date = LocalDate.from(temporal)
                if (date.year !in 0..9_999) {
                    throw DateTimeException("Year cannot be printed as four digits: ${date.year}")
                }
                buildString {
                    append(date.year.toString().padStart(4, '0'))
                    append(date.monthValue.toString().padStart(2, '0'))
                    append(date.dayOfMonth.toString().padStart(2, '0'))
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
        )

        /** The strict ISO formatter for an instant in UTC. */
        public val ISO_INSTANT: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> Instant.from(temporal).toString() },
            parser = { text -> Instant.parse(text) },
            description = "ParseCaseSensitive(false)Instant()",
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
        )

        /** The strict ISO formatter for a date-time with an offset. */
        public val ISO_OFFSET_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val dateTime = OffsetDateTime.from(temporal)
                formatIsoLocalDateTime(dateTime.dateTime) + dateTime.offset
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
        )

        /** The strict ISO formatter for a date-time with an offset and optional region zone. */
        public val ISO_ZONED_DATE_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal ->
                val dateTime = ZonedDateTime.from(temporal)
                buildString {
                    append(formatIsoLocalDateTime(dateTime.dateTime))
                    append(dateTime.offset)
                    if (dateTime.zone != dateTime.offset) {
                        append('[')
                        append(dateTime.zone)
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
        )
    }
}

private fun formatIsoLocalDateTime(dateTime: LocalDateTime): String =
    "${dateTime.date}T${formatIsoLocalTime(dateTime.time)}"

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
    val date = LocalDate.from(temporal)
    val time = LocalTime.from(temporal)
    val offset = ZoneOffset.from(temporal)
    if (date.year !in 0..9_999) {
        throw DateTimeException("Year cannot be printed as four digits: ${date.year}")
    }
    if (offset.totalSeconds % 60 != 0) {
        throw DateTimeException("Offset seconds cannot be printed by RFC 1123: $offset")
    }
    return buildString {
        append(RFC_DAY_NAMES[date.dayOfWeek.value - 1])
        append(", ")
        append(date.dayOfMonth)
        append(' ')
        append(RFC_MONTH_NAMES[date.monthValue - 1])
        append(' ')
        append(date.year.toString().padStart(4, '0'))
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

private fun parseIsoDate(
    text: CharSequence,
    offsetRequired: Boolean,
): TemporalAccessor {
    val input = text.toString()
    val offsetStart = isoOffsetStart(input)
    if (offsetRequired && offsetStart == null) {
        throw DateTimeParseException("Text cannot be parsed to an ISO date", input, input.length)
    }
    val dateEnd = offsetStart ?: input.length
    val date = try {
        LocalDate.parse(input.substring(0, dateEnd))
    } catch (exception: DateTimeParseException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO date",
            input,
            exception.errorIndex,
            exception,
        )
    }
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

private fun parseIsoTime(text: CharSequence): TemporalAccessor {
    val input = text.toString()
    val offsetStart = isoOffsetStart(input)
    val time = try {
        LocalTime.parse(input.substring(0, offsetStart ?: input.length))
    } catch (exception: DateTimeParseException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO time",
            input,
            exception.errorIndex,
            exception,
        )
    }
    val offset = offsetStart?.let { index -> parseIsoOffset(input, index, "ISO time") }
    return ParsedTemporalAccessor(time = time, offset = offset)
}

private fun parseIsoDateTime(text: CharSequence): TemporalAccessor {
    val input = text.toString()
    val bracketStart = input.lastIndexOf('[')
    val hasBracket = bracketStart >= 0 || ']' in input
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
    if (zone != null && offsetStart == null) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO date-time",
            input,
            bracketStart,
        )
    }
    val dateTime = try {
        LocalDateTime.parse(mainText.substring(0, offsetStart ?: mainText.length))
    } catch (exception: DateTimeParseException) {
        throw DateTimeParseException(
            "Text cannot be parsed to an ISO date-time",
            input,
            exception.errorIndex,
            exception,
        )
    }
    val offset = offsetStart?.let { index -> parseIsoOffset(mainText, index, "ISO date-time") }
    return ParsedTemporalAccessor(
        date = dateTime.date,
        time = dateTime.time,
        offset = offset,
        zone = zone,
    )
}

private fun parseIsoOrdinalDate(text: CharSequence): TemporalAccessor {
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
        LocalDate.ofYearDay(year, dayOfYear)
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

private fun parseIsoWeekDate(text: CharSequence): TemporalAccessor {
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
        if (week !in 1..53 || dayOfWeek !in 1..7) throw DateTimeException("Invalid ISO week date")
        val januaryFourth = LocalDate.of(weekBasedYear, 1, 4)
        val firstMonday = januaryFourth.minusDays((januaryFourth.dayOfWeek.value - 1).toLong())
        firstMonday.plusWeeks((week - 1).toLong()).plusDays((dayOfWeek - 1).toLong()).also {
            if (
                it.get(IsoFields.WEEK_BASED_YEAR) != weekBasedYear ||
                it.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != week
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

private fun parseBasicIsoDate(text: CharSequence): TemporalAccessor {
    val input = text.toString()
    if (input.length < 8) {
        throw DateTimeParseException("Text cannot be parsed to a basic ISO date", input, input.length)
    }
    val year = parseFixedDigits(input, 0, 4, input, "basic ISO date")
    val month = parseFixedDigits(input, 4, 2, input, "basic ISO date")
    val day = parseFixedDigits(input, 6, 2, input, "basic ISO date")
    val date = try {
        LocalDate.of(year, month, day)
    } catch (exception: RuntimeException) {
        throw DateTimeParseException(
            "Text cannot be parsed to a basic ISO date",
            input,
            0,
            exception,
        )
    }
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

private fun parseRfc1123(text: CharSequence): TemporalAccessor {
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

    var date = try {
        LocalDate.of(yearText.toInt(), month, dayText.toInt())
    } catch (exception: RuntimeException) {
        throw rfcParseFailure(input, 0, exception)
    }
    if (weekday != null && date.dayOfWeek.value != weekday) {
        throw rfcParseFailure(input, 0)
    }
    val hour = timeParts[0].toInt()
    val minute = timeParts[1].toInt()
    val second = timeParts.getOrNull(2)?.toInt() ?: 0
    val time = try {
        if (hour == 24 && minute == 0 && second == 0) {
            date = date.plusDays(1)
            LocalTime.MIDNIGHT
        } else {
            LocalTime.of(hour, minute, second)
        }
    } catch (exception: RuntimeException) {
        throw rfcParseFailure(input, input.indexOf(parts[3]), exception)
    }
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
    return ParsedTemporalAccessor(date = date, time = time, offset = offset)
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

private class ParsedTemporalAccessor(
    private val date: LocalDate? = null,
    private val time: LocalTime? = null,
    private val offset: ZoneOffset? = null,
    private val zone: ZoneId? = null,
) : TemporalAccessor {
    override fun isSupported(field: TemporalField): Boolean = when (field) {
        ChronoField.INSTANT_SECONDS -> date != null && time != null && (offset != null || zone != null)
        ChronoField.OFFSET_SECONDS -> offset != null
        is ChronoField if field.isDateBased -> date?.isSupported(field) == true
        is ChronoField if field.isTimeBased -> time?.isSupported(field) == true
        is ChronoField -> false
        else -> field.isSupportedBy(this)
    }

    override fun range(field: TemporalField): ValueRange = when (field) {
        ChronoField.INSTANT_SECONDS,
        ChronoField.OFFSET_SECONDS,
        -> field.range
        is ChronoField if field.isDateBased -> date?.range(field) ?: unsupported(field)
        is ChronoField if field.isTimeBased -> time?.range(field) ?: unsupported(field)
        is ChronoField -> unsupported(field)
        else -> field.rangeRefinedBy(this)
    }

    override fun getLong(field: TemporalField): Long = when (field) {
        ChronoField.INSTANT_SECONDS -> {
            val dateTime = LocalDateTime.of(date ?: unsupported(field), time ?: unsupported(field))
            val resolvedOffset = offset ?: zone?.rules?.getOffset(dateTime) ?: unsupported(field)
            dateTime.toEpochSecond(resolvedOffset)
        }
        ChronoField.OFFSET_SECONDS -> offset?.totalSeconds?.toLong() ?: unsupported(field)
        is ChronoField if field.isDateBased -> date?.getLong(field) ?: unsupported(field)
        is ChronoField if field.isTimeBased -> time?.getLong(field) ?: unsupported(field)
        is ChronoField -> unsupported(field)
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        val result: Any? = when (query) {
            TemporalQueries.chronology() -> if (date == null) null else IsoChronology
            TemporalQueries.localDate() -> date
            TemporalQueries.localTime() -> time
            TemporalQueries.offset() -> offset
            TemporalQueries.zoneId() -> zone
            TemporalQueries.precision() -> null
            else -> return super<TemporalAccessor>.query(query)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    override fun toString(): String = buildString {
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
