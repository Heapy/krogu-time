package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.chrono.JapaneseChronology
import io.heapy.grogu.time.chrono.JapaneseDate
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class DateTimeFormatterResolverFieldsTest {
    @Test
    fun exposesImmutableResolverFieldCopies() {
        val original = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        assertNull(original.resolverFields)

        val requested = linkedSetOf(
            ChronoField.YEAR,
            ChronoField.MONTH_OF_YEAR,
            ChronoField.DAY_OF_MONTH,
        )
        val filtered = original.withResolverFields(requested)
        assertNotSame(original, filtered)
        assertEquals(requested, filtered.resolverFields)
        assertSame(filtered, filtered.withResolverFields(requested))

        requested.clear()
        assertEquals(
            setOf(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH),
            filtered.resolverFields,
        )
        assertEquals(
            setOf(ChronoField.YEAR, ChronoField.MONTH_OF_YEAR),
            original.withResolverFields(
                ChronoField.YEAR,
                ChronoField.MONTH_OF_YEAR,
                ChronoField.YEAR,
            ).resolverFields,
        )
        assertEquals(emptySet(), original.withResolverFields().resolverFields)
        assertNull(filtered.withResolverFields(null).resolverFields)
    }

    @Test
    fun choosesBetweenCalendarRepresentationsBeforeResolution() {
        val formatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_YEAR, 3)
            .toFormatter()
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(formatter.parse("2024-02-29/060")))
        assertFailsWith<DateTimeParseException> { formatter.parse("2024-02-29/061") }

        val calendarFields = formatter.withResolverFields(
            ChronoField.YEAR,
            ChronoField.MONTH_OF_YEAR,
            ChronoField.DAY_OF_MONTH,
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.from(calendarFields.parse("2024-02-29/061")),
        )

        val ordinalFields = formatter.withResolverFields(
            ChronoField.YEAR,
            ChronoField.DAY_OF_YEAR,
        )
        assertEquals(
            LocalDate.of(2024, 3, 1),
            LocalDate.from(ordinalFields.parse("2024-02-29/061")),
        )
    }

    @Test
    fun independentlyFiltersDateTimeAndOffsetFields() {
        val formatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm")
            .appendOffsetId()
            .toFormatter()
        val text = "2024-02-29T23:45+02:30"

        val dateOnly = formatter.withResolverFields(
            ChronoField.YEAR,
            ChronoField.MONTH_OF_YEAR,
            ChronoField.DAY_OF_MONTH,
        ).parse(text)
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(dateOnly))
        assertFailsWith<RuntimeException> { LocalTime.from(dateOnly) }
        assertFailsWith<RuntimeException> { ZoneOffset.from(dateOnly) }

        val timeOnly = formatter.withResolverFields(
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
        ).parse(text)
        assertEquals(LocalTime.of(23, 45), LocalTime.from(timeOnly))
        assertFailsWith<RuntimeException> { LocalDate.from(timeOnly) }
        assertFailsWith<RuntimeException> { ZoneOffset.from(timeOnly) }

        val offsetOnly = formatter.withResolverFields(ChronoField.OFFSET_SECONDS).parse(text)
        assertEquals(ZoneOffset.ofHoursMinutes(2, 30), ZoneOffset.from(offsetOnly))
        assertFailsWith<RuntimeException> { LocalDate.from(offsetOnly) }
        assertFailsWith<RuntimeException> { LocalTime.from(offsetOnly) }

        val empty = formatter.withResolverFields().parse(text)
        ChronoField.entries.forEach { field -> assertEquals(false, empty.isSupported(field), field.name) }
        assertEquals(
            text,
            formatter.withResolverFields().format(
                io.heapy.grogu.time.OffsetDateTime.parse(text),
            ),
        )
    }

    @Test
    fun appliesToAlternativeDateConstantsAndCrossChecksRetainedWeekdays() {
        val ordinal = DateTimeFormatter.ISO_ORDINAL_DATE.withResolverFields(
            ChronoField.YEAR,
            ChronoField.DAY_OF_YEAR,
        )
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(ordinal.parse("2024-060")))

        val week = DateTimeFormatter.ISO_WEEK_DATE.withResolverFields(
            io.heapy.grogu.time.temporal.IsoFields.WEEK_BASED_YEAR,
            io.heapy.grogu.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            ChronoField.DAY_OF_WEEK,
        )
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(week.parse("2024-W09-4")))

        val wrongWeekday = "Mon, 3 Jun 2008 11:05:30 GMT"
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.RFC_1123_DATE_TIME.parse(wrongWeekday)
        }
        val retainedWeekday = DateTimeFormatter.RFC_1123_DATE_TIME.withResolverFields(
            ChronoField.YEAR,
            ChronoField.MONTH_OF_YEAR,
            ChronoField.DAY_OF_MONTH,
            ChronoField.DAY_OF_WEEK,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.OFFSET_SECONDS,
        )
        assertFailsWith<DateTimeParseException> { retainedWeekday.parse(wrongWeekday) }

        val ignoredWeekday = retainedWeekday.withResolverFields(
            ChronoField.YEAR,
            ChronoField.MONTH_OF_YEAR,
            ChronoField.DAY_OF_MONTH,
            ChronoField.HOUR_OF_DAY,
            ChronoField.MINUTE_OF_HOUR,
            ChronoField.SECOND_OF_MINUTE,
            ChronoField.OFFSET_SECONDS,
        )
        assertEquals(
            LocalDate.of(2008, 6, 3),
            LocalDate.from(ignoredWeekday.parse(wrongWeekday)),
        )
    }

    @Test
    fun resolvesProlepticMonthAlignedWeeksAndJapaneseEraYears() {
        val prolepticMonth = DateTimeFormatterBuilder()
            .appendValue(ChronoField.PROLEPTIC_MONTH)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT)
        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.from(prolepticMonth.parse("24289/29")),
        )

        val alignedMonth = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.ALIGNED_WEEK_OF_MONTH)
            .appendLiteral('/')
            .appendValue(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH)
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT)
        assertEquals(
            LocalDate.of(2024, 2, 10),
            LocalDate.from(alignedMonth.parse("2024/2/2/3")),
        )

        val japaneseOrdinal = DateTimeFormatterBuilder()
            .appendValue(ChronoField.ERA)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR_OF_ERA)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_YEAR)
            .toFormatter()
            .withChronology(JapaneseChronology)
            .withResolverStyle(ResolverStyle.STRICT)
        assertEquals(
            JapaneseDate.of(2019, 5, 1),
            JapaneseDate.from(japaneseOrdinal.parse("3/1/1")),
        )
    }
}
