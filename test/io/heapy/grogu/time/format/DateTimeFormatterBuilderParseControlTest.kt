package io.heapy.grogu.time.format

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderParseControlTest {
    @Test
    fun appliesCaseSensitivityChangesOnlyToSubsequentElements() {
        val builder = DateTimeFormatterBuilder().appendLiteral("Ab")
        assertSame(builder, builder.parseCaseInsensitive())
        builder.appendLiteral("Cd")
        assertSame(builder, builder.parseCaseSensitive())
        builder.appendLiteral("Ef")
        val formatter = builder.toFormatter()

        formatter.parse("AbcDEf")
        assertFailsWith<DateTimeParseException> { formatter.parse("abcDEf") }
        assertFailsWith<DateTimeParseException> { formatter.parse("AbcDef") }
    }

    @Test
    fun parsesZoneIdsAndOffsetTextCaseInsensitively() {
        val zone = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendZoneId()
            .toFormatter()
        val offset = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendOffset("+HH", "ZERO")
            .toFormatter()

        assertEquals(ZoneId.of("Europe/Paris"), zone.parse("eUrOpE/pArIs", ZoneId::from))
        assertEquals(0, offset.parse("zero").getLong(ChronoField.OFFSET_SECONDS))
    }

    @Test
    fun switchesBetweenLenientAndStrictNumericParsing() {
        val builder = DateTimeFormatterBuilder()
        assertSame(builder, builder.parseLenient())
        builder.appendValue(ChronoField.MONTH_OF_YEAR, 2)
        assertSame(builder, builder.parseStrict())
        builder.appendLiteral('/').appendValue(ChronoField.DAY_OF_MONTH, 2)
        val formatter = builder.toFormatter()

        assertEquals(3, formatter.parse("3/01").getLong(ChronoField.MONTH_OF_YEAR))
        assertFailsWith<DateTimeParseException> { formatter.parse("03/1") }
    }

    @Test
    fun suppliesDefaultFieldsOnlyWhenTheyWereNotParsed() {
        val builder = DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd")
        assertSame(builder, builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 12))
        builder.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 34)
        val formatter = builder.toFormatter()

        assertEquals(
            LocalDateTime.of(2024, 3, 1, 12, 34),
            formatter.parse("2024-03-01", LocalDateTime::from),
        )
        assertEquals("2024-03-01", formatter.format(LocalDateTime.of(2024, 3, 1, 5, 6)))

        val existing = DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 12)
            .toFormatter()
        assertEquals(5, existing.parse("5").getLong(ChronoField.HOUR_OF_DAY))
    }

    @Test
    fun integratesOffsetDefaultsWithExplicitOffsetParsing() {
        val defaultOffset = DateTimeFormatterBuilder()
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 3_600)
            .toFormatter()
        val parsedOffset = DateTimeFormatterBuilder()
            .appendOffsetId()
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
            .toFormatter()
        val conflictingOffset = DateTimeFormatterBuilder()
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
            .appendOffsetId()
            .toFormatter()

        assertEquals(ZoneOffset.ofHours(1), defaultOffset.parse("", ZoneOffset::from))
        assertEquals(ZoneOffset.ofHours(1), parsedOffset.parse("+01:00", ZoneOffset::from))
        assertFailsWith<DateTimeParseException> { conflictingOffset.parse("+01:00") }
    }

    @Test
    fun resolvesDefaultInstantSeconds() {
        val formatter = DateTimeFormatterBuilder()
            .parseDefaulting(ChronoField.INSTANT_SECONDS, 123)
            .toFormatter()

        assertEquals(Instant.ofEpochSecond(123), formatter.parse("", Instant::from))
    }
}
