package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterLocalizedWeekTest {
    @Test
    fun numericWeekPatternsUseTheFormatterLocale() {
        val date = LocalDate.of(2021, 1, 1)
        val pattern = "Y-w-W-e"

        assertEquals("2021-1-1-6", DateTimeFormatter.ofPattern(pattern, Locale.US).format(date))
        assertEquals("2020-53-0-5", DateTimeFormatter.ofPattern(pattern, Locale.UK).format(date))
    }

    @Test
    fun changingTheLocaleChangesWeekRulesWithoutRebuildingThePattern() {
        val american = DateTimeFormatter.ofPattern("YYYY-'W'ww-e", Locale.US)
        val british = american.withLocale(Locale.UK)
        val date = LocalDate.of(2021, 1, 1)

        assertEquals("2021-W01-6", american.format(date))
        assertEquals("2020-W53-5", british.format(date))
        assertEquals(date, LocalDate.from(american.parse("2021-W01-6")))
        assertEquals(date, LocalDate.from(british.parse("2020-W53-5")))
    }

    @Test
    fun parsesWeekBasedAndWeekOfMonthDates() {
        val britishWeekDate = DateTimeFormatter.ofPattern("YYYY-'W'ww-e", Locale.UK)
            .withResolverStyle(ResolverStyle.STRICT)
        val britishMonthWeek = DateTimeFormatter.ofPattern("uuuu-MM-W-e", Locale.UK)
            .withResolverStyle(ResolverStyle.STRICT)
        val americanReducedWeekDate = DateTimeFormatter.ofPattern("YY-w-e", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT)
        val britishTextWeekDate = DateTimeFormatter.ofPattern("YYYY-'W'ww-eee", Locale.UK)
            .withResolverStyle(ResolverStyle.STRICT)

        assertEquals(LocalDate.of(2021, 1, 1), LocalDate.from(britishWeekDate.parse("2020-W53-5")))
        assertEquals(LocalDate.of(2021, 1, 1), LocalDate.from(britishMonthWeek.parse("2021-01-0-5")))
        assertEquals(LocalDate.of(2021, 1, 1), LocalDate.from(americanReducedWeekDate.parse("21-1-6")))
        assertEquals(LocalDate.of(2021, 1, 1), LocalDate.from(britishTextWeekDate.parse("2020-W53-Fri")))
    }

    @Test
    fun crossChecksLocalizedDayNumbersAgainstResolvedCalendarDates() {
        val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-e", Locale.UK)

        assertEquals(LocalDate.of(2021, 1, 1), LocalDate.from(formatter.parse("2021-01-01-5")))
        assertFailsWith<DateTimeParseException> {
            formatter.parse("2021-01-01-4")
        }
    }

    @Test
    fun textualLocalizedDayPatternsUseFormatAndStandaloneStyles() {
        val date = LocalDate.of(2021, 1, 1)

        assertEquals("Fri Friday F", DateTimeFormatter.ofPattern("eee eeee eeeee", Locale.US).format(date))
        assertEquals("Fri Friday F", DateTimeFormatter.ofPattern("ccc cccc ccccc", Locale.US).format(date))
    }

    @Test
    fun descriptionsAndPatternValidationMatchJavaTime() {
        assertEquals(
            "Localized(WeekBasedYear)'-'Localized(WeekOfWeekBasedYear,2)'-'" +
                "Localized(WeekOfMonth,1)'-'Localized(DayOfWeek,1)",
            DateTimeFormatter.ofPattern("Y-ww-W-e").toString(),
        )
        assertEquals(
            "Localized(ReducedValue(WeekBasedYear,2,2,2000-01-01))",
            DateTimeFormatter.ofPattern("YY").toString(),
        )
        assertEquals(
            "Localized(WeekBasedYear,4,19,EXCEEDS_PAD)",
            DateTimeFormatter.ofPattern("YYYY").toString(),
        )

        listOf("Y".repeat(20), "www", "WW", "eeeeee", "cc", "cccccc").forEach { pattern ->
            assertFailsWith<IllegalArgumentException>(pattern) {
                DateTimeFormatter.ofPattern(pattern)
            }
        }
    }
}
