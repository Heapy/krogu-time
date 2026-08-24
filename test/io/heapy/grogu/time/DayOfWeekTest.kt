package io.heapy.grogu.time

import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
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
    fun fromObtainsTheDayFromATemporalAccessor() {
        assertSame(DayOfWeek.MONDAY, DayOfWeek.from(DayOfWeek.MONDAY))
        assertSame(DayOfWeek.THURSDAY, DayOfWeek.from(LocalDate.of(2024, 2, 29)))
        assertFailsWith<DateTimeException> { DayOfWeek.from(LocalTime.NOON) }
    }

    @Test
    fun displayNameUsesLocalizedText() {
        assertEquals("Monday", DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
        assertEquals("Mon", DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
        assertEquals("M", DayOfWeek.MONDAY.getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
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

    @Test
    fun exposesTheDayOfWeekTemporalField() {
        assertEquals(true, DayOfWeek.MONDAY.isSupported(ChronoField.DAY_OF_WEEK))
        assertEquals(false, DayOfWeek.MONDAY.isSupported(ChronoField.DAY_OF_MONTH))
        assertEquals(1, DayOfWeek.MONDAY.get(ChronoField.DAY_OF_WEEK))
        assertEquals(7, DayOfWeek.SUNDAY.getLong(ChronoField.DAY_OF_WEEK))
        assertFailsWith<UnsupportedTemporalTypeException> {
            DayOfWeek.MONDAY.getLong(ChronoField.DAY_OF_MONTH)
        }
    }

    @Test
    fun reportsDaysAsItsTemporalPrecision() {
        assertSame(ChronoUnit.DAYS, DayOfWeek.MONDAY.query(TemporalQueries.precision()))
    }

    @Test
    fun adjustsTheDayOfWeekFieldOnATemporal() {
        assertEquals(
            AdjustableDay(5),
            DayOfWeek.FRIDAY.adjustInto(AdjustableDay(1)),
        )
    }

    private data class AdjustableDay(private val day: Long) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.DAY_OF_WEEK

        override fun isSupported(unit: TemporalUnit?): Boolean = unit === ChronoUnit.DAYS

        override fun getLong(field: TemporalField): Long = day

        override fun with(field: TemporalField, newValue: Long): Temporal = copy(day = newValue)

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            copy(day = day + amountToAdd)

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            (endExclusive as AdjustableDay).day - day
    }
}
