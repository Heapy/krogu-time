package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DayOfWeekTest {
    @Test
    fun daysAreOrderedFromMondayThroughSunday() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
            DayOfWeek.entries,
        )
    }

    @Test
    fun valueUsesIso8601Numbering() {
        DayOfWeek.entries.forEachIndexed { index, day ->
            assertEquals(index + 1, day.value)
        }
    }

    @Test
    fun ofReturnsTheCorrespondingDay() {
        DayOfWeek.entries.forEach { day ->
            assertSame(day, DayOfWeek.of(day.value))
        }
    }

    @Test
    fun ofRejectsValuesOutsideTheIsoRange() {
        listOf(Int.MIN_VALUE, -1, 0, 8, 9, Int.MAX_VALUE).forEach { value ->
            val error = assertFailsWith<DateTimeException> {
                DayOfWeek.of(value)
            }
            assertEquals("Invalid value for DayOfWeek: $value", error.message)
        }
    }

    @Test
    fun plusWrapsInBothDirections() {
        assertSame(DayOfWeek.SUNDAY, DayOfWeek.MONDAY.plus(-1))
        assertSame(DayOfWeek.MONDAY, DayOfWeek.MONDAY.plus(0))
        assertSame(DayOfWeek.TUESDAY, DayOfWeek.MONDAY.plus(1))
        assertSame(DayOfWeek.SUNDAY, DayOfWeek.MONDAY.plus(6))
        assertSame(DayOfWeek.MONDAY, DayOfWeek.MONDAY.plus(7))
        assertSame(DayOfWeek.TUESDAY, DayOfWeek.MONDAY.plus(8))
        assertSame(DayOfWeek.MONDAY, DayOfWeek.MONDAY.plus(Long.MAX_VALUE))
        assertSame(DayOfWeek.SUNDAY, DayOfWeek.MONDAY.plus(Long.MIN_VALUE))
    }

    @Test
    fun minusWrapsWithoutOverflow() {
        assertSame(DayOfWeek.TUESDAY, DayOfWeek.MONDAY.minus(-1))
        assertSame(DayOfWeek.MONDAY, DayOfWeek.MONDAY.minus(0))
        assertSame(DayOfWeek.SUNDAY, DayOfWeek.MONDAY.minus(1))
        assertSame(DayOfWeek.TUESDAY, DayOfWeek.MONDAY.minus(6))
        assertSame(DayOfWeek.MONDAY, DayOfWeek.MONDAY.minus(7))
        assertSame(DayOfWeek.SUNDAY, DayOfWeek.MONDAY.minus(8))
        assertSame(DayOfWeek.MONDAY, DayOfWeek.MONDAY.minus(Long.MAX_VALUE))
        assertSame(DayOfWeek.TUESDAY, DayOfWeek.MONDAY.minus(Long.MIN_VALUE))
    }

    @Test
    fun toStringUsesTheEnumConstantName() {
        assertEquals("MONDAY", DayOfWeek.MONDAY.toString())
        assertEquals("SUNDAY", DayOfWeek.SUNDAY.toString())
    }
}
