package io.heapy.krogu.time.format

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.Year
import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DateTimeFormatterBuilderInstantTest {
    @Test
    fun defaultInstantUsesGroupsOfThreeFractionalDigits() {
        val builder = DateTimeFormatterBuilder()
        assertSame(builder, builder.appendInstant())
        val formatter = builder.toFormatter()

        mapOf(
            0 to "1970-01-01T00:00:00Z",
            1_000_000 to "1970-01-01T00:00:00.001Z",
            1_000 to "1970-01-01T00:00:00.000001Z",
            1 to "1970-01-01T00:00:00.000000001Z",
            123_400_000 to "1970-01-01T00:00:00.123400Z",
        ).forEach { (nano, expected) ->
            assertEquals(expected, formatter.format(Instant.ofEpochSecond(0, nano.toLong())))
        }
    }

    @Test
    fun requestedFractionWidthsTrimOmitOrTruncateWithoutRounding() {
        val instant = Instant.ofEpochSecond(0, 123_456_789)

        mapOf(
            -1 to "1970-01-01T00:00:00.123456789Z",
            0 to "1970-01-01T00:00:00Z",
            1 to "1970-01-01T00:00:00.1Z",
            4 to "1970-01-01T00:00:00.1234Z",
            9 to "1970-01-01T00:00:00.123456789Z",
        ).forEach { (fractionalDigits, expected) ->
            val builder = DateTimeFormatterBuilder()
            assertSame(builder, builder.appendInstant(fractionalDigits))
            assertEquals(expected, builder.toFormatter().format(instant))
        }

        assertEquals(
            "1970-01-01T00:00:00Z",
            DateTimeFormatterBuilder().appendInstant(-1).toFormatter().format(Instant.EPOCH),
        )
        assertEquals(
            "1970-01-01T00:00:00.000Z",
            DateTimeFormatterBuilder().appendInstant(3).toFormatter().format(Instant.EPOCH),
        )
    }

    @Test
    fun parsesAnInstantInsideSurroundingBuilderElements() {
        val formatter = DateTimeFormatterBuilder()
            .appendLiteral('<')
            .appendInstant(3)
            .appendLiteral('>')
            .toFormatter()

        val parsed = formatter.parse("<1970-01-01T00:00:00.123+01:00>")

        assertEquals(Instant.ofEpochSecond(-3_600, 123_000_000), Instant.from(parsed))
        assertEquals(-3_600, parsed.getLong(ChronoField.INSTANT_SECONDS))
        assertEquals(123_000_000, parsed.getLong(ChronoField.NANO_OF_SECOND))
    }

    @Test
    fun strictParsingRequiresTheConfiguredFractionWhileLenientAllowsZeroToNineDigits() {
        val strict = DateTimeFormatterBuilder().appendInstant(3).toFormatter()
        val lenient = DateTimeFormatterBuilder()
            .parseLenient()
            .appendInstant(3)
            .toFormatter()

        assertEquals(
            Instant.ofEpochSecond(0, 123_000_000),
            Instant.from(strict.parse("1970-01-01T00:00:00.123Z")),
        )
        listOf(
            "1970-01-01T00:00:00Z",
            "1970-01-01T00:00:00.12Z",
            "1970-01-01T00:00:00.1234Z",
        ).forEach { text ->
            assertFailsWith<DateTimeParseException>(text) { strict.parse(text) }
        }

        mapOf(
            "1970-01-01T00:00:00Z" to Instant.EPOCH,
            "1970-01-01T00:00:00.1Z" to Instant.ofEpochSecond(0, 100_000_000),
            "1970-01-01T00:00:00.123456789Z" to Instant.ofEpochSecond(0, 123_456_789),
        ).forEach { (text, expected) ->
            assertEquals(expected, Instant.from(lenient.parse(text)))
        }
    }

    @Test
    fun preservesSpecialInstantParseState() {
        val formatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant()
            .toFormatter()

        assertEquals(
            Instant.ofEpochSecond(86_400),
            Instant.from(formatter.parse("1970-01-01T24:00:00Z")),
        )
        val leapSecond = formatter.parse("2016-12-31t23:59:60z")
        assertEquals(Instant.parse("2016-12-31T23:59:60Z"), Instant.from(leapSecond))
        assertTrue(leapSecond.query(DateTimeFormatter.parsedLeapSecond()))
        assertFalse(
            formatter.parse("2016-12-31T23:59:59Z")
                .query(DateTimeFormatter.parsedLeapSecond()),
        )
    }

    @Test
    fun validatesFractionalDigitsAndSupportsOptionalInstantElements() {
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatterBuilder().appendInstant(-2)
        }
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatterBuilder().appendInstant(10)
        }
        val zeroFraction = DateTimeFormatterBuilder().appendInstant(0).toFormatter()
        assertFailsWith<DateTimeParseException> {
            zeroFraction.parse("1970-01-01T00:00:00Z")
        }

        val formatter = DateTimeFormatterBuilder()
            .appendLiteral('[')
            .optionalStart()
            .appendInstant()
            .optionalEnd()
            .appendLiteral(']')
            .toFormatter()

        assertEquals("[]", formatter.format(Year.of(2024)))
        assertEquals("[1970-01-01T00:00:00Z]", formatter.format(Instant.EPOCH))
        formatter.parse("[]")
    }
}
