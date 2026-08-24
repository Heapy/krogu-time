package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Clock
import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JapaneseChronologyTest {
    @Test
    fun exposesIdentityRangesFactoriesAndHistoricalBoundaries() {
        assertEquals("Japanese", JapaneseChronology.id)
        assertEquals("japanese", JapaneseChronology.calendarType)
        assertTrue(JapaneseChronology.isIsoBased)
        assertSame(JapaneseChronology, Chronology.of("Japanese"))
        assertSame(JapaneseChronology, Chronology.of("japanese"))
        assertEquals(JapaneseEra.values().toList(), JapaneseChronology.eras())
        assertEquals("1873 - 999999999", JapaneseChronology.range(ChronoField.YEAR).toString())
        assertEquals("-1 - 3", JapaneseChronology.range(ChronoField.ERA).toString())
        assertEquals("1 - 7/366", JapaneseChronology.range(ChronoField.DAY_OF_YEAR).toString())
        assertEquals("1 - 15/999997980", JapaneseChronology.range(ChronoField.YEAR_OF_ERA).toString())

        val showaEnd = JapaneseDate.of(JapaneseEra.SHOWA, 64, 1, 7)
        val heiseiStart = JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 8)
        val heiseiEnd = JapaneseDate.of(JapaneseEra.HEISEI, 31, 4, 30)
        val reiwaStart = JapaneseDate.of(JapaneseEra.REIWA, 1, 5, 1)
        assertEquals("Japanese Showa 64-01-07", showaEnd.toString())
        assertEquals("Japanese Heisei 1-01-08", showaEnd.plusDays(1).toString())
        assertEquals(7, showaEnd.lengthOfYear())
        assertEquals(7, showaEnd.get(ChronoField.DAY_OF_YEAR))
        assertEquals(358, heiseiStart.lengthOfYear())
        assertEquals(1, heiseiStart.get(ChronoField.DAY_OF_YEAR))
        assertEquals(120, heiseiEnd.lengthOfYear())
        assertEquals(120, heiseiEnd.get(ChronoField.DAY_OF_YEAR))
        assertEquals(245, reiwaStart.lengthOfYear())
        assertEquals(1, reiwaStart.get(ChronoField.DAY_OF_YEAR))
        assertEquals(reiwaStart, heiseiEnd.plusDays(1))
        assertEquals(heiseiStart, JapaneseChronology.dateYearDay(JapaneseEra.HEISEI, 1, 1))

        assertFailsWith<DateTimeException> { JapaneseDate.of(1872, 12, 31) }
        assertFailsWith<DateTimeException> { JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 7) }
        assertFailsWith<DateTimeException> { JapaneseChronology.dateYearDay(JapaneseEra.SHOWA, 64, 8) }
    }

    @Test
    fun exposesFieldsArithmeticPeriodsClocksAndChronologyComposition() {
        val date = JapaneseDate.of(2024, 2, 29)
        assertSame(JapaneseChronology, date.chronology)
        assertSame(JapaneseEra.REIWA, date.era)
        assertTrue(date.isLeapYear)
        assertEquals(6, date.get(ChronoField.YEAR_OF_ERA))
        assertEquals(3, date.get(ChronoField.ERA))
        assertEquals(2024, date.get(ChronoField.YEAR))
        assertFalse(date.isSupported(ChronoField.ALIGNED_WEEK_OF_YEAR))
        assertFailsWith<DateTimeException> { date.getLong(ChronoField.ALIGNED_WEEK_OF_YEAR) }

        assertEquals("Japanese Reiwa 7-02-28", date.plusYears(1).toString())
        assertEquals("Japanese Reiwa 5-02-28", date.with(ChronoField.YEAR_OF_ERA, 5).toString())
        assertEquals("Japanese Heisei 6-02-28", date.with(ChronoField.ERA, 2).toString())
        val end = JapaneseDate.of(2025, 3, 2)
        assertEquals(367L, date.until(end, ChronoUnit.DAYS))
        assertEquals("Japanese P1Y2D", date.until(end).toString())

        val clock = Clock.fixed(Instant.parse("2024-02-29T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(date, JapaneseDate.now(clock))
        assertEquals(date, JapaneseChronology.dateNow(clock))
        assertSame(date, JapaneseDate.from(date))
        assertEquals(date, JapaneseDate.from(LocalDate.of(2024, 2, 29)))

        val dateTime = date.atTime(LocalTime.of(23, 59, 59, 999_999_999))
        assertSame(JapaneseChronology, dateTime.query(TemporalQueries.chronology()))
        assertEquals("Japanese Reiwa 6-03-01T00:00", dateTime.plus(1, ChronoUnit.NANOS).toString())
        val zoned = dateTime.atZone(ZoneOffset.ofHours(2))
        assertSame(JapaneseChronology, zoned.chronology)
        assertEquals("2024-02-29T21:59:59.999999999Z", zoned.toInstant().toString())
    }
}
