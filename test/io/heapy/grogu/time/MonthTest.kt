package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.chrono.IsoChronology
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

class MonthTest {
    @Test
    fun monthsUseCalendarOrderAndIsoValues() {
        assertEquals(
            listOf(
                Month.JANUARY,
                Month.FEBRUARY,
                Month.MARCH,
                Month.APRIL,
                Month.MAY,
                Month.JUNE,
                Month.JULY,
                Month.AUGUST,
                Month.SEPTEMBER,
                Month.OCTOBER,
                Month.NOVEMBER,
                Month.DECEMBER,
            ),
            Month.entries,
        )
        Month.entries.forEachIndexed { index, month ->
            assertEquals(index + 1, month.value)
            assertSame(month, Month.of(index + 1))
        }
    }

    @Test
    fun ofRejectsValuesOutsideTheMonthRange() {
        listOf(Int.MIN_VALUE, -1, 0, 13, Int.MAX_VALUE).forEach { value ->
            val error = assertFailsWith<DateTimeException> { Month.of(value) }
            assertEquals("Invalid value for MonthOfYear: $value", error.message)
        }
    }

    @Test
    fun fromObtainsTheIsoMonthFromATemporalAccessor() {
        assertSame(Month.MARCH, Month.from(Month.MARCH))
        assertSame(Month.FEBRUARY, Month.from(LocalDate.of(2024, 2, 29)))

        val hijrahDate = HijrahDate.of(1445, 9, 1)
        assertSame(
            LocalDate.ofEpochDay(hijrahDate.toEpochDay()).month,
            Month.from(hijrahDate),
        )
        assertFailsWith<DateTimeException> { Month.from(LocalTime.NOON) }
    }

    @Test
    fun displayNameUsesLocalizedText() {
        assertEquals("January", Month.JANUARY.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
        assertEquals("Jan", Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
        assertEquals("J", Month.JANUARY.getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
    }

    @Test
    fun plusAndMinusWrapWithoutOverflow() {
        assertSame(Month.DECEMBER, Month.JANUARY.plus(-1))
        assertSame(Month.JANUARY, Month.JANUARY.plus(12))
        assertSame(Month.FEBRUARY, Month.JANUARY.plus(13))
        assertSame(Month.MAY, Month.JANUARY.plus(Long.MIN_VALUE))

        assertSame(Month.FEBRUARY, Month.JANUARY.minus(-1))
        assertSame(Month.JANUARY, Month.JANUARY.minus(12))
        assertSame(Month.DECEMBER, Month.JANUARY.minus(13))
        assertSame(Month.SEPTEMBER, Month.JANUARY.minus(Long.MIN_VALUE))
    }

    @Test
    fun lengthAccountsForLeapYears() {
        val commonYearLengths = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val leapYearLengths = listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        Month.entries.forEachIndexed { index, month ->
            assertEquals(commonYearLengths[index], month.length(isLeapYear = false))
            assertEquals(leapYearLengths[index], month.length(isLeapYear = true))
        }
    }

    @Test
    fun minimumAndMaximumLengthsCoverFebruary() {
        assertEquals(28, Month.FEBRUARY.minLength())
        assertEquals(29, Month.FEBRUARY.maxLength())
        assertEquals(30, Month.APRIL.minLength())
        assertEquals(30, Month.APRIL.maxLength())
        assertEquals(31, Month.JANUARY.minLength())
        assertEquals(31, Month.JANUARY.maxLength())
    }

    @Test
    fun firstDayOfYearAccountsForLeapDay() {
        val commonYearStarts = listOf(1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)
        val leapYearStarts = listOf(1, 32, 61, 92, 122, 153, 183, 214, 245, 275, 306, 336)

        Month.entries.forEachIndexed { index, month ->
            assertEquals(commonYearStarts[index], month.firstDayOfYear(isLeapYear = false))
            assertEquals(leapYearStarts[index], month.firstDayOfYear(isLeapYear = true))
        }
    }

    @Test
    fun firstMonthOfQuarterReturnsTheQuarterBoundary() {
        assertSame(Month.JANUARY, Month.MARCH.firstMonthOfQuarter())
        assertSame(Month.APRIL, Month.JUNE.firstMonthOfQuarter())
        assertSame(Month.JULY, Month.SEPTEMBER.firstMonthOfQuarter())
        assertSame(Month.OCTOBER, Month.DECEMBER.firstMonthOfQuarter())
    }

    @Test
    fun exposesTheMonthOfYearTemporalField() {
        assertEquals(true, Month.JANUARY.isSupported(ChronoField.MONTH_OF_YEAR))
        assertEquals(false, Month.JANUARY.isSupported(ChronoField.DAY_OF_MONTH))
        assertEquals(1, Month.JANUARY.get(ChronoField.MONTH_OF_YEAR))
        assertEquals(12, Month.DECEMBER.getLong(ChronoField.MONTH_OF_YEAR))
        assertFailsWith<UnsupportedTemporalTypeException> {
            Month.JANUARY.getLong(ChronoField.DAY_OF_MONTH)
        }
    }

    @Test
    fun reportsIsoChronologyAndMonthsAsItsTemporalPrecision() {
        assertSame(IsoChronology, Month.JANUARY.query(TemporalQueries.chronology()))
        assertSame(ChronoUnit.MONTHS, Month.JANUARY.query(TemporalQueries.precision()))
    }

    @Test
    fun adjustsTheMonthOfYearFieldOnATemporal() {
        assertEquals(
            AdjustableMonth(9),
            Month.SEPTEMBER.adjustInto(AdjustableMonth(1)),
        )
        assertFailsWith<DateTimeException> {
            Month.SEPTEMBER.adjustInto(HijrahDate.of(1445, 9, 1))
        }
    }

    private data class AdjustableMonth(private val month: Long) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.MONTH_OF_YEAR

        override fun isSupported(unit: TemporalUnit?): Boolean = unit === ChronoUnit.MONTHS

        override fun getLong(field: TemporalField): Long = month

        override fun with(field: TemporalField, newValue: Long): Temporal = copy(month = newValue)

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            copy(month = month + amountToAdd)

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            (endExclusive as AdjustableMonth).month - month
    }
}
