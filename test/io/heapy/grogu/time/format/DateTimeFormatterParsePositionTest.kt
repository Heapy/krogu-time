package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DateTimeFormatterParsePositionTest {
    @Test
    fun parsePositionsHaveMutableCursorValueSemantics() {
        val first = ParsePosition(3)
        val second = ParsePosition(3)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        second.errorIndex = 5
        assertNotEquals(first, second)
        first.errorIndex = 5
        assertEquals(first, second)
        assertTrue(first.toString().endsWith("[index=3,errorIndex=5]"))
    }

    @Test
    fun resolvedParsingStartsAtAndAdvancesTheSuppliedPosition() {
        val position = ParsePosition(7)

        val parsed = DateTimeFormatter.ISO_LOCAL_DATE.parse(
            "prefix:2024-02-29:suffix",
            position,
        )

        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(parsed))
        assertEquals(17, position.index)
        assertEquals(-1, position.errorIndex)
    }

    @Test
    fun resolvedParsingRecordsSyntaxErrorsAndThrows() {
        val position = ParsePosition(7)

        val exception = assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_LOCAL_DATE.parse("prefix:2024-0x-29", position)
        }

        assertEquals(12, exception.errorIndex)
        assertEquals(7, position.index)
        assertEquals(12, position.errorIndex)
    }

    @Test
    fun unresolvedParsingRetainsRawInvalidFields() {
        val position = ParsePosition(7)

        val parsed = DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(
            "prefix:2024-13-65:suffix",
            position,
        )

        requireNotNull(parsed)
        assertEquals(2024, parsed.getLong(ChronoField.YEAR))
        assertEquals(13, parsed.getLong(ChronoField.MONTH_OF_YEAR))
        assertEquals(65, parsed.getLong(ChronoField.DAY_OF_MONTH))
        assertNull(parsed.query(TemporalQueries.chronology()))
        assertFalse(parsed.isSupported(ChronoField.EPOCH_DAY))
        assertEquals(17, position.index)
        assertEquals(-1, position.errorIndex)
    }

    @Test
    fun unresolvedParsingReturnsNullAndRecordsSyntaxErrors() {
        val position = ParsePosition(7)

        val parsed = DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(
            "prefix:2024-0x-29",
            position,
        )

        assertNull(parsed)
        assertEquals(7, position.index)
        assertEquals(12, position.errorIndex)
    }

    @Test
    fun parsePositionRejectsIndexesOutsideTheInput() {
        assertFailsWith<IndexOutOfBoundsException> {
            DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("2024-02-29", ParsePosition(-1))
        }
        assertFailsWith<IndexOutOfBoundsException> {
            DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved("2024-02-29", ParsePosition(11))
        }
    }
}
