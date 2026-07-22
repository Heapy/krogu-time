package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalDateTest {
    @Test
    fun validatesCalendarDates() {
        assertEquals(LocalDate.of(2024, Month.FEBRUARY, 29), LocalDate.of(2024, 2, 29))
        assertFailsWith<DateTimeException> { LocalDate.of(2023, 2, 29) }
        assertFailsWith<DateTimeException> { LocalDate.of(2024, 4, 31) }
        assertFailsWith<DateTimeException> { LocalDate.of(2024, 13, 1) }
        assertFailsWith<DateTimeException> { LocalDate.of(Year.MAX_VALUE + 1, 1, 1) }
    }

    @Test
    fun createsDatesFromDayOfYear() {
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.ofYearDay(2024, 60))
        assertEquals(LocalDate.of(2023, 12, 31), LocalDate.ofYearDay(2023, 365))
        assertFailsWith<DateTimeException> { LocalDate.ofYearDay(2023, 366) }
        assertFailsWith<DateTimeException> { LocalDate.ofYearDay(2024, 0) }
    }

    @Test
    fun convertsEpochDaysAcrossTheFullSupportedRange() {
        val known = listOf(
            LocalDate.of(1969, 12, 31) to -1L,
            LocalDate.of(1970, 1, 1) to 0L,
            LocalDate.of(2000, 2, 29) to 11_016L,
            LocalDate.of(2024, 2, 29) to 19_782L,
        )
        known.forEach { (date, epochDay) ->
            assertEquals(epochDay, date.toEpochDay())
            assertEquals(date, LocalDate.ofEpochDay(epochDay))
        }
        assertEquals(LocalDate.MIN, LocalDate.ofEpochDay(LocalDate.MIN.toEpochDay()))
        assertEquals(LocalDate.MAX, LocalDate.ofEpochDay(LocalDate.MAX.toEpochDay()))
    }

    @Test
    fun exposesCalendarProperties() {
        val leapDay = LocalDate.of(2024, 2, 29)
        assertEquals(2024, leapDay.year)
        assertEquals(Month.FEBRUARY, leapDay.month)
        assertEquals(2, leapDay.monthValue)
        assertEquals(29, leapDay.dayOfMonth)
        assertEquals(60, leapDay.dayOfYear)
        assertEquals(DayOfWeek.THURSDAY, leapDay.dayOfWeek)
        assertTrue(leapDay.isLeapYear)
        assertEquals(29, leapDay.lengthOfMonth())
        assertEquals(366, leapDay.lengthOfYear())

        val commonDate = LocalDate.of(2023, 2, 28)
        assertFalse(commonDate.isLeapYear)
        assertEquals(28, commonDate.lengthOfMonth())
        assertEquals(365, commonDate.lengthOfYear())
    }

    @Test
    fun formatsAndOrdersIsoDates() {
        assertEquals("2024-02-29", LocalDate.of(2024, 2, 29).toString())
        assertEquals("0000-01-01", LocalDate.of(0, 1, 1).toString())
        assertEquals("-0001-01-01", LocalDate.of(-1, 1, 1).toString())
        assertEquals("+10000-01-01", LocalDate.of(10_000, 1, 1).toString())
        assertTrue(LocalDate.of(2024, 1, 1) < LocalDate.of(2024, 1, 2))
        assertEquals(LocalDate.of(1970, 1, 1), LocalDate.EPOCH)
    }
}
