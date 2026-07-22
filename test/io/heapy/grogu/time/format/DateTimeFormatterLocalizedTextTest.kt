package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterLocalizedTextTest {
    @Test
    fun builderFormatsAndParsesStyledTextUsingItsLocale() {
        val formatter = DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
            .toFormatter(Locale.US)

        assertEquals("February", formatter.format(LocalDate.of(2024, 2, 29)))
        assertEquals(
            2L,
            formatter.parse("February").getLong(ChronoField.MONTH_OF_YEAR),
        )
        assertFailsWith<DateTimeParseException> { formatter.parse("february") }

        val insensitive = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
            .toFormatter(Locale.US)
        assertEquals(2L, insensitive.parse("february").getLong(ChronoField.MONTH_OF_YEAR))
    }

    @Test
    fun changingTheFormatterLocaleChangesLocalizedTokens() {
        val english = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.US)
        val french = english.withLocale(Locale.forLanguageTag("fr-FR"))
        val date = LocalDate.of(2024, 2, 29)

        assertEquals("29 February 2024", english.format(date))
        assertEquals("29 février 2024", french.format(date))
        assertEquals(date, LocalDate.from(french.parse("29 février 2024")))
    }

    @Test
    fun textualPatternsCoverEraMonthWeekdayAmPmAndQuarter() {
        val formatter = DateTimeFormatter.ofPattern(
            "G uuuu MMMM d EEEE h a QQQ",
            Locale.US,
        )
        val dateTime = LocalDateTime.of(LocalDate.of(2024, 2, 29), LocalTime.of(15, 0))

        assertEquals("AD 2024 February 29 Thursday 3 PM Q1", formatter.format(dateTime))
        assertEquals(
            "Text(Era,SHORT)' 'Value(Year,4,19,EXCEEDS_PAD)' 'Text(MonthOfYear)' '" +
                "Value(DayOfMonth)' 'Text(DayOfWeek)' 'Value(ClockHourOfAmPm)' '" +
                "Text(AmPmOfDay,SHORT)' 'Text(QuarterOfYear,SHORT)",
            formatter.toString(),
        )
    }

    @Test
    fun noStyleBuilderOverloadUsesFullText() {
        val formatter = DateTimeFormatterBuilder()
            .appendText(ChronoField.DAY_OF_WEEK)
            .toFormatter(Locale.US)

        assertEquals("Thursday", formatter.format(LocalDate.of(2024, 2, 29)))
        assertEquals("Text(DayOfWeek)", formatter.toString())
    }
}
