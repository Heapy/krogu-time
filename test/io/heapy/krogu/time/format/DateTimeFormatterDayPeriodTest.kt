package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DateTimeFormatterDayPeriodTest {
    @Test
    fun formatsAndResolvesUsDayPeriods() {
        val formatter = DateTimeFormatter.ofPattern("B", Locale.US)
        val times = listOf(
            LocalTime.MIDNIGHT,
            LocalTime.of(6, 0),
            LocalTime.NOON,
            LocalTime.of(15, 0),
            LocalTime.of(19, 0),
            LocalTime.of(23, 0),
        )

        times.forEach { time ->
            val text = formatter.format(time)
            assertTrue(text.isNotEmpty())
            assertEquals(text, formatter.format(LocalTime.from(formatter.parse(text))))
        }
    }

    @Test
    fun combinesDayPeriodsWithTwelveHourClockFields() {
        val formatter = DateTimeFormatter.ofPattern("h B", Locale.US)
        val dayPeriod = DateTimeFormatter.ofPattern("B", Locale.US)
        val morning = dayPeriod.format(LocalTime.of(3, 0))
        val afternoon = dayPeriod.format(LocalTime.of(15, 0))

        assertEquals(LocalTime.of(3, 0), LocalTime.from(formatter.parse("3 $morning")))
        assertEquals(LocalTime.of(15, 0), LocalTime.from(formatter.parse("3 $afternoon")))
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ofPattern("HH B", Locale.US).parse("03 $afternoon")
        }
        assertEquals(
            LocalTime.of(3, 0),
            LocalTime.from(
                DateTimeFormatter.ofPattern("HH B", Locale.US)
                    .withResolverStyle(ResolverStyle.LENIENT)
                    .parse("03 $afternoon"),
            ),
        )
    }

    @Test
    fun builderNormalizesStandaloneStylesAndSupportsParserControls() {
        val formatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendDayPeriodText(TextStyle.FULL_STANDALONE)
            .toFormatter(Locale.US)

        assertEquals("ParseCaseSensitive(false)DayPeriod(FULL)", formatter.toString())
        val text = formatter.format(LocalTime.of(6, 0))
        val parsed = LocalTime.from(formatter.parse(text.uppercase()))
        assertEquals(text, formatter.format(parsed))
    }

    @Test
    fun validatesDayPeriodPatternWidths() {
        listOf("B", "BBBB", "BBBBB").forEach(DateTimeFormatter::ofPattern)
        listOf("BB", "BBB", "BBBBBB").forEach { pattern ->
            assertFailsWith<IllegalArgumentException>(pattern) {
                DateTimeFormatter.ofPattern(pattern)
            }
        }
    }
}
