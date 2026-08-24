package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsoFieldsTest {
    @Test
    fun exposesQuarterAndWeekFieldMetadata() {
        assertEquals(ValueRange.of(1, 90, 92), IsoFields.DAY_OF_QUARTER.range)
        assertEquals(ValueRange.of(1, 4), IsoFields.QUARTER_OF_YEAR.range)
        assertEquals(ValueRange.of(1, 52, 53), IsoFields.WEEK_OF_WEEK_BASED_YEAR.range)
        assertEquals(ChronoField.YEAR.range, IsoFields.WEEK_BASED_YEAR.range)
        listOf(
            IsoFields.DAY_OF_QUARTER,
            IsoFields.QUARTER_OF_YEAR,
            IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            IsoFields.WEEK_BASED_YEAR,
        ).forEach { field ->
            assertTrue(field.isDateBased)
            assertFalse(field.isTimeBased)
            assertFalse(field.isSupportedBy(LocalTime.NOON))
        }
    }

    @Test
    fun readsQuarterAndIsoWeekValuesAtBoundaries() {
        val leapDay = LocalDate.of(2024, 2, 29)
        val fridayAtYearStart = LocalDate.of(2021, 1, 1)
        val firstMonday = LocalDate.of(2021, 1, 4)

        assertEquals(1, leapDay.getLong(IsoFields.QUARTER_OF_YEAR))
        assertEquals(60, leapDay.getLong(IsoFields.DAY_OF_QUARTER))
        assertEquals(53, fridayAtYearStart.getLong(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        assertEquals(2020, fridayAtYearStart.getLong(IsoFields.WEEK_BASED_YEAR))
        assertEquals(1, firstMonday.getLong(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        assertEquals(2021, firstMonday.getLong(IsoFields.WEEK_BASED_YEAR))
        assertEquals(
            ValueRange.of(1, 91),
            IsoFields.DAY_OF_QUARTER.rangeRefinedBy(leapDay),
        )
        assertEquals(
            ValueRange.of(1, 53),
            IsoFields.WEEK_OF_WEEK_BASED_YEAR.rangeRefinedBy(fridayAtYearStart),
        )
    }

    @Test
    fun adjustsQuarterAndWeekFields() {
        val leapDay = LocalDate.of(2024, 2, 29)
        val week53 = LocalDate.of(2020, 12, 31)

        assertEquals(LocalDate.of(2024, 11, 29), leapDay.with(IsoFields.QUARTER_OF_YEAR, 4))
        assertEquals(LocalDate.of(2024, 3, 1), leapDay.with(IsoFields.DAY_OF_QUARTER, 61))
        assertEquals(
            LocalDate.of(2024, 3, 7),
            leapDay.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 10),
        )
        assertEquals(LocalDate.of(2021, 12, 30), week53.with(IsoFields.WEEK_BASED_YEAR, 2021))
    }

    @Test
    fun exposesAndAppliesIsoCalendarUnits() {
        assertEquals(Duration.ofSeconds(31_556_952), IsoFields.WEEK_BASED_YEARS.duration)
        assertEquals(Duration.ofSeconds(7_889_238), IsoFields.QUARTER_YEARS.duration)
        listOf(IsoFields.WEEK_BASED_YEARS, IsoFields.QUARTER_YEARS).forEach { unit ->
            assertTrue(unit.isDurationEstimated)
            assertTrue(unit.isDateBased)
            assertFalse(unit.isTimeBased)
            assertTrue(unit.isSupportedBy(LocalDate.EPOCH))
            assertFalse(unit.isSupportedBy(LocalTime.NOON))
        }

        val start = LocalDate.of(2020, 12, 31)
        assertEquals(LocalDate.of(2021, 12, 30), start.plus(1, IsoFields.WEEK_BASED_YEARS))
        assertEquals(LocalDate.of(2022, 3, 31), start.plus(5, IsoFields.QUARTER_YEARS))
        assertEquals(
            2,
            IsoFields.WEEK_BASED_YEARS.between(start, LocalDate.of(2022, 12, 29)),
        )
        assertEquals(
            5,
            IsoFields.QUARTER_YEARS.between(start, LocalDate.of(2022, 3, 31)),
        )
    }
}
