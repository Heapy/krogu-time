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

class HijrahChronologyTest {
    @Test
    fun exposesIdentityLookupRangesFactoriesAndBoundaries() {
        assertEquals("Hijrah-umalqura", HijrahChronology.id)
        assertEquals("islamic-umalqura", HijrahChronology.calendarType)
        assertFalse(HijrahChronology.isIsoBased)
        listOf("Hijrah-umalqura", "islamic-umalqura", "Hijrah", "islamic").forEach { id ->
            assertSame(HijrahChronology, Chronology.of(id))
        }
        assertTrue(HijrahChronology in Chronology.getAvailableChronologies())
        assertEquals(HijrahEra.entries, HijrahChronology.eras())
        assertSame(HijrahEra.AH, HijrahChronology.eraOf(1))

        assertEquals("1300 - 1600", HijrahChronology.range(ChronoField.YEAR).toString())
        assertEquals("1300 - 1600", HijrahChronology.range(ChronoField.YEAR_OF_ERA).toString())
        assertEquals("1 - 29/30", HijrahChronology.range(ChronoField.DAY_OF_MONTH).toString())
        assertEquals("1 - 355", HijrahChronology.range(ChronoField.DAY_OF_YEAR).toString())

        val date = HijrahChronology.date(1_445, 8, 19)
        assertEquals("Hijrah-umalqura AH 1445-08-19", date.toString())
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), date.toEpochDay())
        assertEquals(date, HijrahChronology.dateEpochDay(date.toEpochDay()))
        assertEquals(date, HijrahChronology.date(LocalDate.of(2024, 2, 29)))
        assertEquals(date, HijrahChronology.dateYearDay(1_445, 226))
        assertEquals(date, HijrahChronology.date(HijrahEra.AH, 1_445, 8, 19))

        assertEquals(-31_826L, HijrahDate.of(1_300, 1, 1).toEpochDay())
        assertEquals(74_838L, HijrahDate.of(1_600, 12, 30).toEpochDay())
        assertFailsWith<DateTimeException> { HijrahDate.of(1_299, 12, 29) }
        assertFailsWith<DateTimeException> { HijrahDate.of(1_601, 1, 1) }
        assertFailsWith<DateTimeException> { HijrahDate.of(1_445, 8, 30) }
    }

    @Test
    fun exposesDateFieldsArithmeticPeriodsClocksAndChronologyComposition() {
        val date = HijrahDate.of(1_445, 8, 19)
        assertSame(HijrahChronology, date.chronology)
        assertSame(HijrahEra.AH, date.era)
        assertFalse(date.isLeapYear)
        assertEquals(29, date.lengthOfMonth())
        assertEquals(354, date.lengthOfYear())
        assertEquals(1_445, date.get(ChronoField.YEAR))
        assertEquals(1_445, date.get(ChronoField.YEAR_OF_ERA))
        assertEquals(1, date.get(ChronoField.ERA))
        assertEquals(17_347L, date.getLong(ChronoField.PROLEPTIC_MONTH))
        assertEquals(226, date.get(ChronoField.DAY_OF_YEAR))
        assertSame(date, date.withVariant(HijrahChronology))
        assertSame(date, HijrahDate.from(date))
        assertEquals(date, HijrahDate.from(LocalDate.of(2024, 2, 29)))

        assertEquals("Hijrah-umalqura AH 1445-09-19", date.plusMonths(1).toString())
        assertEquals("Hijrah-umalqura AH 1445-08-20", date.plusDays(1).toString())
        assertEquals(
            "Hijrah-umalqura AH 1446-12-29",
            HijrahDate.of(1_445, 12, 30).plusYears(1).toString(),
        )
        assertEquals("Hijrah-umalqura AH 1446-08-19", date.with(ChronoField.YEAR, 1_446).toString())

        val end = HijrahDate.of(1_446, 8, 21)
        assertEquals(end.toEpochDay() - date.toEpochDay(), date.until(end, ChronoUnit.DAYS))
        assertEquals("Hijrah-umalqura P1Y2D", date.until(end).toString())

        val clock = Clock.fixed(Instant.parse("2024-02-29T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(date, HijrahDate.now(clock))
        assertEquals(date, HijrahChronology.dateNow(clock))

        val dateTime = date.atTime(LocalTime.of(23, 59, 59, 999_999_999))
        assertSame(HijrahChronology, dateTime.query(TemporalQueries.chronology()))
        assertEquals(
            "Hijrah-umalqura AH 1445-08-20T00:00",
            dateTime.plus(1, ChronoUnit.NANOS).toString(),
        )
        val zoned = dateTime.atZone(ZoneOffset.ofHours(2))
        assertSame(HijrahChronology, zoned.chronology)
        assertEquals("2024-02-29T21:59:59.999999999Z", zoned.toInstant().toString())
    }
}
