package io.heapy.grogu.time.format

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
        query.queryFrom(parse(text))

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
