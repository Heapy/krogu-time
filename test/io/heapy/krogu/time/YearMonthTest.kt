package io.heapy.krogu.time

import io.heapy.krogu.time.chrono.HijrahDate
import io.heapy.krogu.time.format.DateTimeParseException
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class YearMonthTest {
    @Test
    fun convertsNonIsoTemporalsThroughTheIsoEpochDay() {
        val hijrahDate = HijrahDate.of(1445, 9, 1)
        val isoDate = LocalDate.ofEpochDay(hijrahDate.toEpochDay())

        assertEquals(YearMonth.of(isoDate.year, isoDate.month), YearMonth.from(hijrahDate))
    }

    @Test
    fun validatesAndExposesYearMonthValues() {
        val yearMonth = YearMonth.of(2024, Month.FEBRUARY)
        assertEquals(2024, yearMonth.year)
        assertEquals(Month.FEBRUARY, yearMonth.month)
        assertEquals(2, yearMonth.monthValue)
        assertTrue(yearMonth.isLeapYear)
        assertTrue(yearMonth.isValidDay(29))
        assertFalse(yearMonth.isValidDay(30))
        assertEquals(29, yearMonth.lengthOfMonth())
        assertEquals(366, yearMonth.lengthOfYear())
        assertEquals(yearMonth, YearMonth.of(2024, 2))
        assertSame(yearMonth, YearMonth.from(yearMonth))
        assertEquals(yearMonth, YearMonth.from(LocalDate.of(2024, 2, 29)))

        assertFailsWith<DateTimeException> { YearMonth.of(Year.MAX_VALUE + 1, 1) }
        assertFailsWith<DateTimeException> { YearMonth.of(2024, 0) }
        assertFailsWith<DateTimeException> { YearMonth.of(2024, 13) }
    }

    @Test
    fun exposesSupportedFieldsUnitsAndRefinedRanges() {
        val yearMonth = YearMonth.of(-1, 2)
        val fields = mapOf(
            ChronoField.MONTH_OF_YEAR to 2L,
            ChronoField.PROLEPTIC_MONTH to -11L,
            ChronoField.YEAR_OF_ERA to 2L,
            ChronoField.YEAR to -1L,
            ChronoField.ERA to 0L,
        )
        fields.forEach { (field, expected) ->
            assertTrue(yearMonth.isSupported(field), field.toString())
            assertEquals(expected, yearMonth.getLong(field), field.toString())
        }
        assertEquals(2, yearMonth.get(ChronoField.MONTH_OF_YEAR))
        assertEquals(2, yearMonth.get(ChronoField.YEAR_OF_ERA))
        assertEquals(-1, yearMonth.get(ChronoField.YEAR))
        assertEquals(0, yearMonth.get(ChronoField.ERA))
        val prolepticMonthException = assertFailsWith<DateTimeException> {
            yearMonth.get(ChronoField.PROLEPTIC_MONTH)
        }
        assertFalse(prolepticMonthException is UnsupportedTemporalTypeException)
        assertEquals(
            "Invalid value for ProlepticMonth " +
                "(valid values -11999999988 - 11999999999): -11",
            prolepticMonthException.message,
        )
        assertEquals(
            ValueRange.of(1, Year.MAX_VALUE.toLong() + 1),
            yearMonth.range(ChronoField.YEAR_OF_ERA),
        )
        assertFalse(yearMonth.isSupported(ChronoField.DAY_OF_MONTH))
        assertFailsWith<UnsupportedTemporalTypeException> {
            yearMonth.getLong(ChronoField.DAY_OF_MONTH)
        }

        ChronoUnit.entries.forEach { unit ->
            val expected = unit in setOf(
                ChronoUnit.MONTHS,
                ChronoUnit.YEARS,
                ChronoUnit.DECADES,
                ChronoUnit.CENTURIES,
                ChronoUnit.MILLENNIA,
                ChronoUnit.ERAS,
            )
            assertEquals(expected, yearMonth.isSupported(unit), unit.toString())
        }
    }

    @Test
    fun replacesAndCalculatesWithCalendarFields() {
        val yearMonth = YearMonth.of(2024, 2)
        assertSame(yearMonth, yearMonth.withYear(2024))
        assertSame(yearMonth, yearMonth.withMonth(2))
        assertEquals(YearMonth.of(2023, 2), yearMonth.withYear(2023))
        assertEquals(YearMonth.of(2024, 12), yearMonth.withMonth(12))
        assertEquals(YearMonth.of(2025, 2), yearMonth.with(ChronoField.YEAR, 2025))
        assertEquals(YearMonth.of(-2023, 2), yearMonth.with(ChronoField.ERA, 0))
        assertEquals(YearMonth.of(2024, 3), yearMonth.with(ChronoField.PROLEPTIC_MONTH, 24_290))

        assertEquals(YearMonth.of(2025, 4), yearMonth.plusYears(1).plusMonths(2))
        assertEquals(YearMonth.of(2023, 12), yearMonth.minusMonths(2))
        assertEquals(YearMonth.of(2034, 2), yearMonth.plus(1, ChronoUnit.DECADES))
        assertEquals(YearMonth.of(-2023, 2), yearMonth.minus(1, ChronoUnit.ERAS))
        assertEquals(YearMonth.of(2025, 4), yearMonth.plus(Period.of(1, 2, 0)))
        assertFailsWith<UnsupportedTemporalTypeException> {
            yearMonth.plus(Period.ofDays(1))
        }
        assertFailsWith<DateTimeException> { YearMonth.of(Year.MAX_VALUE, 12).plusMonths(1) }
    }

    @Test
    fun measuresAdjustsAndProducesDates() {
        val start = YearMonth.of(2024, 2)
        val end = YearMonth.of(2025, 5)
        assertEquals(15, start.until(end, ChronoUnit.MONTHS))
        assertEquals(1, start.until(end, ChronoUnit.YEARS))
        assertEquals(-15, end.until(start, ChronoUnit.MONTHS))
        assertFailsWith<UnsupportedTemporalTypeException> {
            start.until(end, ChronoUnit.DAYS)
        }

        assertEquals(LocalDate.of(2024, 2, 20), start.atDay(20))
        assertEquals(LocalDate.of(2024, 2, 29), start.atEndOfMonth())
        assertFailsWith<DateTimeException> { start.atDay(30) }
        assertEquals(LocalDate.of(2024, 2, 29), start.adjustInto(LocalDate.of(2023, 1, 31)))
        assertFailsWith<DateTimeException> {
            YearMonth.of(1445, 9).adjustInto(HijrahDate.of(1445, 9, 1))
        }

        assertEquals(start, Year.of(2024).atMonth(Month.FEBRUARY))
        assertEquals(start, Year.of(2024).atMonth(2))
    }

    @Test
    fun parsesFormatsAndOrdersYearMonths() {
        val cases = mapOf(
            "0000-01" to YearMonth.of(0, 1),
            "2024-02" to YearMonth.of(2024, 2),
            "-0001-12" to YearMonth.of(-1, 12),
            "+10000-03" to YearMonth.of(10_000, 3),
            "-999999999-01" to YearMonth.of(Year.MIN_VALUE, 1),
            "+999999999-12" to YearMonth.of(Year.MAX_VALUE, 12),
        )
        cases.forEach { (text, expected) ->
            assertEquals(expected, YearMonth.parse(text), text)
        }
        assertEquals("0000-01", YearMonth.of(0, 1).toString())
        assertEquals("-0001-12", YearMonth.of(-1, 12).toString())
        assertEquals("10000-03", YearMonth.of(10_000, 3).toString())
        assertEquals("999999999-12", YearMonth.of(Year.MAX_VALUE, 12).toString())
        assertTrue(YearMonth.of(2024, 1) < YearMonth.of(2024, 2))
        assertTrue(YearMonth.of(2025, 1).isAfter(YearMonth.of(2024, 12)))
        assertTrue(YearMonth.of(2024, 11).isBefore(YearMonth.of(2024, 12)))

        val invalidInputs = mapOf(
            "" to 0,
            "2024" to 4,
            "2024-2" to 5,
            "2024/02" to 4,
            "2024-13" to 0,
            "+2024-02" to 0,
            "10000-02" to 0,
            "+12345678901-02" to 11,
            "2024-02Z" to 7,
            "２０２４-０２" to 0,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) { YearMonth.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }
}
