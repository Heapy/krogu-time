package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Year
import io.heapy.krogu.time.YearMonth
import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class DateTimeFormatterBuilderOptionalTest {
    @Test
    fun formatsAndParsesNestedOptionalPatternSections() {
        val formatter = DateTimeFormatter.ofPattern("uuuu[-MM[-dd]]")

        assertEquals("2024", formatter.format(Year.of(2024)))
        assertEquals("2024-03", formatter.format(YearMonth.of(2024, 3)))
        assertEquals("2024-03-01", formatter.format(LocalDate.of(2024, 3, 1)))
        assertEquals(Year.of(2024), formatter.parse("2024", Year::from))
        assertEquals(YearMonth.of(2024, 3), formatter.parse("2024-03", YearMonth::from))
        assertEquals(LocalDate.of(2024, 3, 1), formatter.parse("2024-03-01", LocalDate::from))
    }

    @Test
    fun rollsBackTheWholeOptionalSectionWhenParsingFails() {
        val formatter = DateTimeFormatterBuilder()
            .optionalStart()
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('X')
            .optionalEnd()
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()

        val parsed = formatter.parse("03")

        assertFalse(parsed.isSupported(ChronoField.MONTH_OF_YEAR))
        assertEquals(3, parsed.getLong(ChronoField.DAY_OF_MONTH))
    }

    @Test
    fun retainsParserSettingsChangedInsideAFailedOptionalSection() {
        val formatter = DateTimeFormatterBuilder()
            .optionalStart()
            .parseCaseInsensitive()
            .appendLiteral('X')
            .optionalEnd()
            .appendLiteral('Y')
            .toFormatter()

        formatter.parse("y")
    }

    @Test
    fun automaticallyClosesOpenSectionsWhenBuildingAFormatter() {
        val builder = DateTimeFormatterBuilder().appendPattern("uuuu")
        assertSame(builder, builder.optionalStart())
        builder.appendPattern("-MM")

        val formatter = builder.toFormatter()

        assertEquals("2024", formatter.format(Year.of(2024)))
        assertEquals("2024-03", formatter.format(YearMonth.of(2024, 3)))
        assertFailsWith<IllegalStateException> { builder.optionalEnd() }
    }

    @Test
    fun rejectsAnOptionalEndWithoutAStart() {
        assertFailsWith<IllegalStateException> {
            DateTimeFormatterBuilder().optionalEnd()
        }
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatter.ofPattern("uuuu]")
        }
    }
}
