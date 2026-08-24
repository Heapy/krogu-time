package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalAdjustersTest {
    @Test
    fun adjustsMonthAndYearBoundaries() {
        val date = LocalDate.of(2024, 2, 15)
        assertEquals(LocalDate.of(2024, 2, 1), date.with(TemporalAdjusters.firstDayOfMonth()))
        assertEquals(LocalDate.of(2024, 2, 29), date.with(TemporalAdjusters.lastDayOfMonth()))
        assertEquals(LocalDate.of(2024, 3, 1), date.with(TemporalAdjusters.firstDayOfNextMonth()))
        assertEquals(LocalDate.of(2024, 1, 1), date.with(TemporalAdjusters.firstDayOfYear()))
        assertEquals(LocalDate.of(2024, 12, 31), date.with(TemporalAdjusters.lastDayOfYear()))
        assertEquals(LocalDate.of(2025, 1, 1), date.with(TemporalAdjusters.firstDayOfNextYear()))
    }

    @Test
    fun selectsOrdinalWeekdaysWithinAndAroundAMonth() {
        val date = LocalDate.of(2024, 3, 15)
        assertEquals(
            LocalDate.of(2024, 3, 1),
            date.with(TemporalAdjusters.firstInMonth(DayOfWeek.FRIDAY)),
        )
        assertEquals(
            LocalDate.of(2024, 3, 29),
            date.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY)),
        )
        assertEquals(
            LocalDate.of(2024, 3, 8),
            date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY)),
        )
        assertEquals(
            LocalDate.of(2024, 3, 22),
            date.with(TemporalAdjusters.dayOfWeekInMonth(-2, DayOfWeek.FRIDAY)),
        )
        assertEquals(
            LocalDate.of(2024, 2, 23),
            date.with(TemporalAdjusters.dayOfWeekInMonth(0, DayOfWeek.FRIDAY)),
        )
    }

    @Test
    fun movesToAdjacentWeekdaysWithOrWithoutKeepingTheSameDay() {
        val friday = LocalDate.of(2024, 3, 15)
        assertEquals(
            LocalDate.of(2024, 3, 22),
            friday.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)),
        )
        assertEquals(friday, friday.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)))
        assertEquals(
            LocalDate.of(2024, 3, 8),
            friday.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)),
        )
        assertEquals(friday, friday.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)))
        assertEquals(
            LocalDate.of(2024, 3, 18),
            friday.with(TemporalAdjusters.next(DayOfWeek.MONDAY)),
        )
        assertEquals(
            LocalDate.of(2024, 3, 11),
            friday.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)),
        )
    }

    @Test
    fun wrapsDateOperatorsAndPreservesLocalTime() {
        val dateTime = LocalDateTime.of(2024, 2, 28, 13, 14, 15, 123_456_789)
        val plusTwoDays = TemporalAdjusters.ofDateAdjuster { it.plusDays(2) }
        assertEquals(
            LocalDateTime.of(2024, 3, 1, 13, 14, 15, 123_456_789),
            dateTime.with(plusTwoDays),
        )
    }
}
