package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.Year
import io.heapy.krogu.time.YearMonth
import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderPaddingTest {
    @Test
    fun padsOnlyTheNextElementWithSpacesOrACustomCharacter() {
        val builder = DateTimeFormatterBuilder()
        assertSame(builder, builder.padNext(3))
        builder.appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('|')
        assertSame(builder, builder.padNext(4, '_'))
        val formatter = builder.appendLiteral('X').appendLiteral('!').toFormatter()

        assertEquals("  3|___X!", formatter.format(LocalDate.of(2024, 1, 3)))
        assertEquals(
            3,
            formatter.parse("  3|___X!").getLong(ChronoField.DAY_OF_MONTH),
        )
    }

    @Test
    fun rejectsInvalidWidthsAndOutputThatExceedsThePad() {
        assertFailsWith<IllegalArgumentException> { DateTimeFormatterBuilder().padNext(0) }

        val formatter = DateTimeFormatterBuilder()
            .padNext(1)
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter()
        assertFailsWith<io.heapy.krogu.time.DateTimeException> {
            formatter.format(LocalDate.of(2024, 1, 12))
        }
    }

    @Test
    fun strictParsingRequiresTheFullPadButLenientParsingUsesAMaximum() {
        val strict = DateTimeFormatterBuilder()
            .padNext(3)
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter()
        val lenient = DateTimeFormatterBuilder()
            .parseLenient()
            .padNext(3)
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter()

        assertEquals(3, strict.parse("  3").getLong(ChronoField.DAY_OF_MONTH))
        assertFailsWith<DateTimeParseException> { strict.parse("3") }
        assertEquals(3, lenient.parse("3").getLong(ChronoField.DAY_OF_MONTH))
    }

    @Test
    fun matchesCustomPadCharactersUsingTheActiveCaseSetting() {
        val insensitive = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .padNext(3, 'A')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter()
        val sensitive = DateTimeFormatterBuilder()
            .padNext(3, 'A')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .toFormatter()

        assertEquals(3, insensitive.parse("aa3").getLong(ChronoField.DAY_OF_MONTH))
        assertFailsWith<DateTimeParseException> { sensitive.parse("aa3") }
    }

    @Test
    fun canPadAnEntireOptionalSection() {
        val formatter = DateTimeFormatterBuilder()
            .padNext(3, '_')
            .optionalStart()
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .optionalEnd()
            .toFormatter()

        assertEquals("___", formatter.format(Year.of(2024)))
        assertEquals("__3", formatter.format(YearMonth.of(2024, 3)))
    }

    @Test
    fun supportsThePadPatternModifier() {
        val formatter = DateTimeFormatter.ofPattern("ppH:mm")

        assertEquals(" 3:05", formatter.format(LocalTime.of(3, 5)))
        assertEquals(LocalTime.of(3, 5), formatter.parse(" 3:05", LocalTime::from))
        assertFailsWith<IllegalArgumentException> { DateTimeFormatter.ofPattern("pp") }
    }
}
