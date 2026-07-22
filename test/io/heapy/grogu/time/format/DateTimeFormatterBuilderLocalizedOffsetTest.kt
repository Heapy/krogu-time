package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderLocalizedOffsetTest {
    @Test
    fun formatsAndParsesFullAndShortGmtOffsets() {
        val offsets = listOf(
            ZoneOffset.UTC,
            ZoneOffset.ofHours(2),
            ZoneOffset.ofHoursMinutes(2, 30),
            ZoneOffset.ofHoursMinutesSeconds(-5, -30, -45),
        )
        val full = DateTimeFormatterBuilder().appendLocalizedOffset(TextStyle.FULL).toFormatter()
        val short = DateTimeFormatterBuilder().appendLocalizedOffset(TextStyle.SHORT).toFormatter()

        assertEquals(
            listOf("GMT", "GMT+02:00", "GMT+02:30", "GMT-05:30:45"),
            offsets.map { offset -> full.format(OffsetTime.of(LocalTime.NOON, offset)) },
        )
        assertEquals(
            listOf("GMT", "GMT+2", "GMT+2:30", "GMT-5:30:45"),
            offsets.map { offset -> short.format(OffsetTime.of(LocalTime.NOON, offset)) },
        )
        offsets.forEach { offset ->
            assertEquals(offset, ZoneOffset.from(full.parse(full.format(OffsetTime.of(LocalTime.NOON, offset)))))
            assertEquals(offset, ZoneOffset.from(short.parse(short.format(OffsetTime.of(LocalTime.NOON, offset)))))
        }
    }

    @Test
    fun validatesStylesAndUsesSequentialCaseSettings() {
        val builder = DateTimeFormatterBuilder()
        assertSame(builder, builder.appendLocalizedOffset(TextStyle.FULL))
        listOf(
            TextStyle.FULL_STANDALONE,
            TextStyle.SHORT_STANDALONE,
            TextStyle.NARROW,
            TextStyle.NARROW_STANDALONE,
        ).forEach { style ->
            assertFailsWith<IllegalArgumentException> {
                DateTimeFormatterBuilder().appendLocalizedOffset(style)
            }
        }

        val sensitive = DateTimeFormatterBuilder()
            .appendLocalizedOffset(TextStyle.SHORT)
            .toFormatter()
        assertFailsWith<DateTimeParseException> { sensitive.parse("gmt+2") }
        val insensitive = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLocalizedOffset(TextStyle.SHORT)
            .toFormatter()
        assertEquals(ZoneOffset.ofHours(2), ZoneOffset.from(insensitive.parse("gmt+2")))
    }

    @Test
    fun fullAndShortParsingEnforceTheirDistinctShapes() {
        val full = DateTimeFormatterBuilder().appendLocalizedOffset(TextStyle.FULL).toFormatter()
        val short = DateTimeFormatterBuilder().appendLocalizedOffset(TextStyle.SHORT).toFormatter()

        listOf("GMT+02:00", "GMT+02:00:45", "GMT-05:30").forEach { text ->
            assertEquals(ZoneOffset.of(text.removePrefix("GMT")), ZoneOffset.from(full.parse(text)))
        }
        listOf(
            "GMT+2" to ZoneOffset.ofHours(2),
            "GMT+02" to ZoneOffset.ofHours(2),
            "GMT+2:30" to ZoneOffset.ofHoursMinutes(2, 30),
            "GMT+02:30:45" to ZoneOffset.ofHoursMinutesSeconds(2, 30, 45),
        ).forEach { (text, offset) ->
            assertEquals(offset, ZoneOffset.from(short.parse(text)))
        }
        listOf("GMT+2", "GMT+02", "GMT+02:3", "UTC+02:00").forEach { text ->
            assertFailsWith<DateTimeParseException>(text) { full.parse(text) }
        }
        listOf("GMT+", "GMT+2:3", "UTC+2").forEach { text ->
            assertFailsWith<DateTimeParseException>(text) { short.parse(text) }
        }
    }

    @Test
    fun composesWithPatternsPaddingAndOptionalSections() {
        val fullPattern = DateTimeFormatter.ofPattern("OOOO")
        val shortPattern = DateTimeFormatter.ofPattern("O")
        val offset = OffsetTime.of(LocalTime.NOON, ZoneOffset.ofHoursMinutes(2, 30))
        assertEquals("GMT+02:30", fullPattern.format(offset))
        assertEquals("GMT+2:30", shortPattern.format(offset))
        assertEquals("GMT+02:30", DateTimeFormatter.ofPattern("ZZZZ").format(offset))
        listOf("OO", "OOO", "OOOOO").forEach { pattern ->
            assertFailsWith<IllegalArgumentException>(pattern) { DateTimeFormatter.ofPattern(pattern) }
        }

        val padded = DateTimeFormatterBuilder()
            .padNext(12, '_')
            .appendLocalizedOffset(TextStyle.FULL)
            .toFormatter()
        assertEquals("___GMT+02:30", padded.format(offset))
        assertEquals(
            ZoneOffset.ofHoursMinutes(2, 30),
            ZoneOffset.from(padded.parse("___GMT+02:30")),
        )

        val optional = DateTimeFormatterBuilder()
            .optionalStart()
            .appendLocalizedOffset(TextStyle.FULL)
            .optionalEnd()
            .appendLiteral('!')
            .toFormatter()
        assertEquals("!", optional.format(LocalTime.NOON))
        assertEquals("GMT!", optional.format(OffsetTime.of(LocalTime.NOON, ZoneOffset.UTC)))
        assertSame(ZoneOffset.UTC, ZoneOffset.from(optional.parse("GMT!")))
        assertFailsWith<DateTimeException> { ZoneOffset.from(optional.parse("!")) }
    }
}
