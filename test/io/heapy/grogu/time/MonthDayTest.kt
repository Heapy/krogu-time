package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonthDayTest {
    @Test
    fun validatesAndExposesMonthDayValues() {
        val leapDay = MonthDay.of(Month.FEBRUARY, 29)
        assertEquals(Month.FEBRUARY, leapDay.month)
        assertEquals(2, leapDay.monthValue)
        assertEquals(29, leapDay.dayOfMonth)
        assertEquals(leapDay, MonthDay.of(2, 29))
        assertEquals(leapDay, MonthDay.from(leapDay))
        assertEquals(leapDay, MonthDay.from(LocalDate.of(2024, 2, 29)))

        assertFailsWith<DateTimeException> { MonthDay.of(0, 1) }
        assertFailsWith<DateTimeException> { MonthDay.of(13, 1) }
        assertFailsWith<DateTimeException> { MonthDay.of(2, 30) }
        assertFailsWith<DateTimeException> { MonthDay.of(4, 31) }
        assertFailsWith<DateTimeException> { MonthDay.of(1, 0) }
    }

    @Test
    fun exposesFieldsAndRefinedDayRange() {
        val monthDay = MonthDay.of(2, 29)
        assertTrue(monthDay.isSupported(ChronoField.MONTH_OF_YEAR))
        assertTrue(monthDay.isSupported(ChronoField.DAY_OF_MONTH))
        assertFalse(monthDay.isSupported(ChronoField.YEAR))
        assertEquals(2, monthDay.get(ChronoField.MONTH_OF_YEAR))
        assertEquals(29, monthDay.get(ChronoField.DAY_OF_MONTH))
        assertEquals(ValueRange.of(1, 28, 29), monthDay.range(ChronoField.DAY_OF_MONTH))
        assertFailsWith<UnsupportedTemporalTypeException> {
            monthDay.getLong(ChronoField.YEAR)
        }
    }

    @Test
    fun replacesAdjustsAndCombinesWithYears() {
        val january31 = MonthDay.of(1, 31)
        assertEquals(MonthDay.of(2, 29), january31.withMonth(2))
        assertEquals(MonthDay.of(4, 30), january31.with(Month.APRIL))
        assertEquals(MonthDay.of(1, 20), january31.withDayOfMonth(20))

        val leapDay = MonthDay.of(2, 29)
        assertTrue(leapDay.isValidYear(2024))
        assertFalse(leapDay.isValidYear(2023))
        assertEquals(LocalDate.of(2024, 2, 29), leapDay.atYear(2024))
        assertEquals(LocalDate.of(2023, 2, 28), leapDay.atYear(2023))
        assertEquals(LocalDate.of(2023, 2, 28), leapDay.adjustInto(LocalDate.of(2023, 1, 31)))

        assertTrue(Year.of(2024).isValidMonthDay(leapDay))
        assertFalse(Year.of(2023).isValidMonthDay(leapDay))
        assertFalse(Year.of(2024).isValidMonthDay(null))
        assertEquals(LocalDate.of(2023, 2, 28), Year.of(2023).atMonthDay(leapDay))
    }

    @Test
    fun parsesFormatsAndOrdersMonthDays() {
        val cases = mapOf(
            "--01-01" to MonthDay.of(1, 1),
            "--02-29" to MonthDay.of(2, 29),
            "--12-31" to MonthDay.of(12, 31),
        )
        cases.forEach { (text, expected) ->
            assertEquals(expected, MonthDay.parse(text), text)
            assertEquals(text, expected.toString(), text)
        }
        assertTrue(MonthDay.of(1, 31) < MonthDay.of(2, 1))
        assertTrue(MonthDay.of(12, 31).isAfter(MonthDay.of(2, 29)))
        assertTrue(MonthDay.of(2, 28).isBefore(MonthDay.of(2, 29)))

        val invalidInputs = mapOf(
            "" to 0,
            "-02-29" to 0,
            "02-29" to 0,
            "--2-29" to 2,
            "--02-9" to 5,
            "--02/29" to 4,
            "--02-30" to 0,
            "--13-01" to 0,
            "--02-29Z" to 7,
            "--０２-２９" to 2,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) { MonthDay.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }
}
