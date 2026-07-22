package io.heapy.grogu.time.format

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalQuery

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

        /** The strict ISO formatter for a time without a date or offset. */
        public val ISO_LOCAL_TIME: DateTimeFormatter = DateTimeFormatter(
            printer = { temporal -> formatIsoLocalTime(LocalTime.from(temporal)) },
            parser = { text -> LocalTime.parse(text) },
            description =
                "Value(HourOfDay,2)':'Value(MinuteOfHour,2)" +
                    "[':'Value(SecondOfMinute,2)" +
                    "[Fraction(NanoOfSecond,0,9,DecimalPoint)]]",
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
