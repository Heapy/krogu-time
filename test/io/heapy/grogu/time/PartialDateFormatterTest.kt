package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeFormatter
import io.heapy.grogu.time.format.DateTimeParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PartialDateFormatterTest {
    @Test
    fun yearsSupportCustomFormattingAndParsing() {
        val formatter = DateTimeFormatter.ofPattern("'year='uuuu")
        val year = Year.of(-42)

        assertEquals("year=-0042", year.format(formatter))
        assertEquals(year, Year.parse("year=-0042", formatter))
    }

    @Test
    fun yearMonthsSupportCustomFormattingAndParsing() {
        val formatter = DateTimeFormatter.ofPattern("MM/uuuu")
        val yearMonth = YearMonth.of(2024, 3)

        assertEquals("03/2024", yearMonth.format(formatter))
        assertEquals(yearMonth, YearMonth.parse("03/2024", formatter))
    }

    @Test
    fun monthDaysSupportCustomFormattingAndParsing() {
        val formatter = DateTimeFormatter.ofPattern("dd.MM")
        val monthDay = MonthDay.of(2, 29)

        assertEquals("29.02", monthDay.format(formatter))
        assertEquals(monthDay, MonthDay.parse("29.02", formatter))
    }

    @Test
    fun customFormattersRetainFormattingAndParsingFailures() {
        val fullDate = DateTimeFormatter.ofPattern("uuuu-MM-dd")

        assertFailsWith<DateTimeException> { Year.of(2024).format(fullDate) }
        assertFailsWith<DateTimeParseException> { Year.parse("03", DateTimeFormatter.ofPattern("MM")) }
        assertFailsWith<DateTimeParseException> {
            YearMonth.parse("2024", DateTimeFormatter.ofPattern("uuuu"))
        }
        assertFailsWith<DateTimeParseException> {
            MonthDay.parse("02", DateTimeFormatter.ofPattern("MM"))
        }
    }
}
