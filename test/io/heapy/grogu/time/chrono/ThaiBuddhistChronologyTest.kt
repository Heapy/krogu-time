package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThaiBuddhistChronologyTest {
    @Test
    fun exposesIdentityLookupRangesAndFactories() {
        assertEquals("ThaiBuddhist", ThaiBuddhistChronology.id)
        assertEquals("buddhist", ThaiBuddhistChronology.calendarType)
        assertTrue(ThaiBuddhistChronology.isIsoBased)
        assertSame(ThaiBuddhistChronology, Chronology.of("ThaiBuddhist"))
        assertSame(ThaiBuddhistChronology, Chronology.of("buddhist"))
        assertTrue(ThaiBuddhistChronology in Chronology.getAvailableChronologies())
        assertEquals(ThaiBuddhistEra.entries, ThaiBuddhistChronology.eras())
        assertSame(ThaiBuddhistEra.BE, ThaiBuddhistChronology.eraOf(1))

        assertEquals("-999999456 - 1000000542", ThaiBuddhistChronology.range(ChronoField.YEAR).toString())
        assertEquals("1 - 999999457/1000000542", ThaiBuddhistChronology.range(ChronoField.YEAR_OF_ERA).toString())

        val leapDate = ThaiBuddhistChronology.date(2_567, 2, 29)
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), leapDate.toEpochDay())
        assertEquals(leapDate, ThaiBuddhistChronology.dateEpochDay(leapDate.toEpochDay()))
        assertEquals(leapDate, ThaiBuddhistChronology.date(LocalDate.of(2024, 2, 29)))
        assertEquals(leapDate, ThaiBuddhistChronology.dateYearDay(2_567, 60))
        assertEquals(leapDate, ThaiBuddhistChronology.date(ThaiBuddhistEra.BE, 2_567, 2, 29))

        val clock = Clock.fixed(Instant.parse("2024-02-29T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(leapDate, ThaiBuddhistDate.now(clock))
        assertEquals(leapDate, ThaiBuddhistChronology.dateNow(clock))
    }

    @Test
    fun exposesDateFieldsArithmeticPeriodsAndChronologyComposition() {
        val date = ThaiBuddhistDate.of(2_567, 2, 29)
        assertSame(ThaiBuddhistChronology, date.chronology)
        assertSame(ThaiBuddhistEra.BE, date.era)
        assertTrue(date.isLeapYear)
        assertEquals(29, date.lengthOfMonth())
        assertEquals(366, date.lengthOfYear())
        assertEquals(2_567, date.get(ChronoField.YEAR))
        assertEquals(2_567, date.get(ChronoField.YEAR_OF_ERA))
        assertEquals(1, date.get(ChronoField.ERA))
        assertEquals(30_805L, date.getLong(ChronoField.PROLEPTIC_MONTH))
        assertSame(date, ThaiBuddhistDate.from(date))
        assertEquals(date, ThaiBuddhistDate.from(LocalDate.of(2024, 2, 29)))

        assertEquals("ThaiBuddhist BE 2568-02-28", date.plus(1, ChronoUnit.YEARS).toString())
        assertEquals("ThaiBuddhist BE 2567-03-01", date.plus(1, ChronoUnit.DAYS).toString())
        assertEquals("ThaiBuddhist BE 2566-02-28", date.with(ChronoField.YEAR, 2_566).toString())
        assertEquals("ThaiBuddhist BEFORE_BE 2567-02-28", date.with(ChronoField.ERA, 0).toString())

        val end = ThaiBuddhistDate.of(2_568, 3, 2)
        assertEquals(367L, date.until(end, ChronoUnit.DAYS))
        assertEquals("ThaiBuddhist P1Y2D", date.until(end).toString())

        val dateTime = date.atTime(LocalTime.of(23, 59, 59, 999_999_999))
        assertSame(ThaiBuddhistChronology, dateTime.query(TemporalQueries.chronology()))
        assertEquals(
            "ThaiBuddhist BE 2567-03-01T00:00",
            dateTime.plus(1, ChronoUnit.NANOS).toString(),
        )
        val zoned = dateTime.atZone(ZoneOffset.ofHours(2))
        assertSame(ThaiBuddhistChronology, zoned.chronology)
        assertEquals("2024-02-29T21:59:59.999999999Z", zoned.toInstant().toString())
    }
}
