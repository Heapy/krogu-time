package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Year
import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderTextMapTest {
    @Test
    fun formatsMappedValuesAndFallsBackToNumbers() {
        val builder = DateTimeFormatterBuilder()
        assertSame(
            builder,
            builder.appendText(
                ChronoField.MONTH_OF_YEAR,
                mapOf(1L to "JNY", 2L to "FBY"),
            ),
        )
        val formatter = builder.toFormatter()

        assertEquals("JNY", formatter.format(LocalDate.of(2024, 1, 1)))
        assertEquals("FBY", formatter.format(LocalDate.of(2024, 2, 1)))
        assertEquals("3", formatter.format(LocalDate.of(2024, 3, 1)))
    }

    @Test
    fun snapshotsTheLookupMapWhenItIsAppended() {
        val lookup = mutableMapOf(1L to "Original")
        val formatter = DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()

        lookup[1L] = "Changed"
        lookup[2L] = "Added"

        assertEquals("Original", formatter.format(LocalDate.of(2024, 1, 1)))
        assertEquals("2", formatter.format(LocalDate.of(2024, 2, 1)))
    }

    @Test
    fun strictParsingUsesTheLongestCaseSensitiveTextMatch() {
        val formatter = DateTimeFormatterBuilder()
            .appendText(
                ChronoField.MONTH_OF_YEAR,
                linkedMapOf(1L to "Jan", 2L to "January"),
            )
            .toFormatter()

        assertEquals(2, formatter.parse("January").getLong(ChronoField.MONTH_OF_YEAR))
        assertEquals(1, formatter.parse("Jan").getLong(ChronoField.MONTH_OF_YEAR))
        assertFailsWith<DateTimeParseException> { formatter.parse("january") }
        assertFailsWith<DateTimeParseException> { formatter.parse("2") }
    }

    @Test
    fun parserControlsApplyToTextAndLenientNumericFallback() {
        val insensitive = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendText(ChronoField.MONTH_OF_YEAR, mapOf(2L to "February"))
            .toFormatter()
        val lenient = DateTimeFormatterBuilder()
            .parseLenient()
            .appendText(ChronoField.MONTH_OF_YEAR, mapOf(2L to "February"))
            .toFormatter()

        assertEquals(2, insensitive.parse("february").getLong(ChronoField.MONTH_OF_YEAR))
        assertEquals(2, lenient.parse("February").getLong(ChronoField.MONTH_OF_YEAR))
        assertEquals(3, lenient.parse("3").getLong(ChronoField.MONTH_OF_YEAR))
    }

    @Test
    fun composesWithPaddingLiteralsAndOptionalSections() {
        val padded = DateTimeFormatterBuilder()
            .padNext(5, '_')
            .appendText(ChronoField.MONTH_OF_YEAR, mapOf(2L to "Feb"))
            .appendLiteral('!')
            .toFormatter()
        assertEquals("__Feb!", padded.format(LocalDate.of(2024, 2, 1)))
        assertEquals(2, padded.parse("__Feb!").getLong(ChronoField.MONTH_OF_YEAR))

        val optional = DateTimeFormatterBuilder()
            .appendLiteral('[')
            .optionalStart()
            .appendText(ChronoField.MONTH_OF_YEAR, mapOf(2L to "Feb"))
            .optionalEnd()
            .appendLiteral(']')
            .toFormatter()
        assertEquals("[]", optional.format(Year.of(2024)))
        optional.parse("[]")
    }

    @Test
    fun reportsConflictingTextAndNumericFields() {
        val formatter = DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR, mapOf(1L to "Jan"))
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .toFormatter()

        assertEquals(1, formatter.parse("Jan-1").getLong(ChronoField.MONTH_OF_YEAR))
        assertFailsWith<DateTimeParseException> { formatter.parse("Jan-2") }
    }
}
