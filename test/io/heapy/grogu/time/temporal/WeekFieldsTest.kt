package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WeekFieldsTest {
    @Test
    fun createsAndCachesValidatedWeekDefinitions() {
        assertEquals(DayOfWeek.MONDAY, WeekFields.ISO.firstDayOfWeek)
        assertEquals(4, WeekFields.ISO.minimalDaysInFirstWeek)
        assertEquals(DayOfWeek.SUNDAY, WeekFields.SUNDAY_START.firstDayOfWeek)
        assertEquals(1, WeekFields.SUNDAY_START.minimalDaysInFirstWeek)
        assertSame(WeekFields.ISO, WeekFields.of(DayOfWeek.MONDAY, 4))
        assertSame(WeekFields.SUNDAY_START, WeekFields.of(DayOfWeek.SUNDAY, 1))
        assertSame(IsoFields.WEEK_BASED_YEARS, WeekFields.WEEK_BASED_YEARS)
        assertEquals("WeekFields[MONDAY,4]", WeekFields.ISO.toString())
        assertEquals(WeekFields.of(DayOfWeek.THURSDAY, 7), WeekFields.of(DayOfWeek.THURSDAY, 7))
        assertFailsWith<IllegalArgumentException> { WeekFields.of(DayOfWeek.MONDAY, 0) }
        assertFailsWith<IllegalArgumentException> { WeekFields.of(DayOfWeek.MONDAY, 8) }
    }

    @Test
    fun obtainsWeekRulesFromLocales() {
        assertSame(WeekFields.SUNDAY_START, WeekFields.of(Locale.US))
        assertSame(WeekFields.ISO, WeekFields.of(Locale.UK))

        val defaultRules = WeekFields.of(Locale.getDefault())
        assertTrue(defaultRules.minimalDaysInFirstWeek in 1..7)
    }

    @Test
    fun exposesComputedFieldMetadata() {
        val fields = WeekFields.ISO
        assertField(fields.dayOfWeek, ChronoUnit.DAYS, ChronoUnit.WEEKS, ValueRange.of(1, 7))
        assertField(
            fields.weekOfMonth,
            ChronoUnit.WEEKS,
            ChronoUnit.MONTHS,
            ValueRange.of(0, 1, 4, 6),
        )
        assertField(
            fields.weekOfYear,
            ChronoUnit.WEEKS,
            ChronoUnit.YEARS,
            ValueRange.of(0, 1, 52, 54),
        )
        assertField(
            fields.weekOfWeekBasedYear,
            ChronoUnit.WEEKS,
            IsoFields.WEEK_BASED_YEARS,
            ValueRange.of(1, 52, 53),
        )
        assertField(
            fields.weekBasedYear,
            IsoFields.WEEK_BASED_YEARS,
            ChronoUnit.FOREVER,
            ChronoField.YEAR.range,
        )
    }

    @Test
    fun computesWeeksFromDifferentCulturalRules() {
        val date = LocalDate.of(2009, 1, 1)
        val iso = WeekFields.ISO
        val mondayFive = WeekFields.of(DayOfWeek.MONDAY, 5)

        assertEquals(4L, date.getLong(iso.dayOfWeek))
        assertEquals(1L, date.getLong(iso.weekOfMonth))
        assertEquals(1L, date.getLong(iso.weekOfYear))
        assertEquals(1L, date.getLong(iso.weekOfWeekBasedYear))
        assertEquals(2009L, date.getLong(iso.weekBasedYear))

        assertEquals(0L, date.getLong(mondayFive.weekOfMonth))
        assertEquals(0L, date.getLong(mondayFive.weekOfYear))
        assertEquals(53L, date.getLong(mondayFive.weekOfWeekBasedYear))
        assertEquals(2008L, date.getLong(mondayFive.weekBasedYear))
        assertEquals(6L, LocalDate.of(2021, 1, 1).getLong(WeekFields.SUNDAY_START.dayOfWeek))
    }

    @Test
    fun refinesRangesAndAdjustsComputedFields() {
        val date = LocalDate.of(2009, 1, 1)
        val iso = WeekFields.ISO
        val mondayFive = WeekFields.of(DayOfWeek.MONDAY, 5)

        assertEquals(ValueRange.of(1, 5), date.range(iso.weekOfMonth))
        assertEquals(ValueRange.of(0, 4), date.range(mondayFive.weekOfMonth))
        assertEquals(ValueRange.of(1, 53), date.range(mondayFive.weekOfWeekBasedYear))
        assertEquals(LocalDate.of(2009, 1, 8), date.with(iso.weekOfMonth, 2))
        assertEquals(LocalDate.of(2008, 12, 28), date.with(WeekFields.SUNDAY_START.dayOfWeek, 1))

        val week53 = LocalDate.of(2020, 12, 31)
        assertEquals(LocalDate.of(2021, 12, 30), week53.with(iso.weekBasedYear, 2021))
    }

    private fun assertField(
        field: TemporalField,
        baseUnit: TemporalUnit,
        rangeUnit: TemporalUnit,
        range: ValueRange,
    ) {
        assertEquals(baseUnit, field.baseUnit)
        assertEquals(rangeUnit, field.rangeUnit)
        assertEquals(range, field.range)
        assertTrue(field.isDateBased)
        assertFalse(field.isTimeBased)
        assertTrue(field.isSupportedBy(LocalDate.EPOCH))
        assertFalse(field.isSupportedBy(LocalTime.NOON))
    }
}
