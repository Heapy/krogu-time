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

class MinguoChronologyTest {
    @Test
    fun exposesIdentityLookupRangesAndFactories() {
        assertEquals("Minguo", MinguoChronology.id)
        assertEquals("roc", MinguoChronology.calendarType)
        assertTrue(MinguoChronology.isIsoBased)
        assertSame(MinguoChronology, Chronology.of("Minguo"))
        assertSame(MinguoChronology, Chronology.of("roc"))
        assertTrue(MinguoChronology in Chronology.getAvailableChronologies())
        assertEquals(MinguoEra.entries, MinguoChronology.eras())
        assertSame(MinguoEra.ROC, MinguoChronology.eraOf(1))

        assertEquals("-1000001910 - 999998088", MinguoChronology.range(ChronoField.YEAR).toString())
        assertEquals("1 - 999998088/1000001911", MinguoChronology.range(ChronoField.YEAR_OF_ERA).toString())

        val leapDate = MinguoChronology.date(113, 2, 29)
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), leapDate.toEpochDay())
        assertEquals(leapDate, MinguoChronology.dateEpochDay(leapDate.toEpochDay()))
        assertEquals(leapDate, MinguoChronology.date(LocalDate.of(2024, 2, 29)))
        assertEquals(leapDate, MinguoChronology.dateYearDay(113, 60))
        assertEquals(leapDate, MinguoChronology.date(MinguoEra.ROC, 113, 2, 29))

        val clock = Clock.fixed(Instant.parse("2024-02-29T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(leapDate, MinguoDate.now(clock))
        assertEquals(leapDate, MinguoChronology.dateNow(clock))
    }

    @Test
    fun exposesDateFieldsArithmeticPeriodsAndChronologyComposition() {
        val date = MinguoDate.of(113, 2, 29)
        assertSame(MinguoChronology, date.chronology)
        assertSame(MinguoEra.ROC, date.era)
        assertTrue(date.isLeapYear)
        assertEquals(29, date.lengthOfMonth())
        assertEquals(366, date.lengthOfYear())
        assertEquals(113, date.get(ChronoField.YEAR))
        assertEquals(113, date.get(ChronoField.YEAR_OF_ERA))
        assertEquals(1, date.get(ChronoField.ERA))
        assertEquals(1_357L, date.getLong(ChronoField.PROLEPTIC_MONTH))
        assertSame(date, MinguoDate.from(date))
        assertEquals(date, MinguoDate.from(LocalDate.of(2024, 2, 29)))

        assertEquals("Minguo ROC 114-02-28", date.plus(1, ChronoUnit.YEARS).toString())
        assertEquals("Minguo ROC 113-03-01", date.plus(1, ChronoUnit.DAYS).toString())
        assertEquals("Minguo ROC 112-02-28", date.with(ChronoField.YEAR, 112).toString())
        assertEquals("Minguo BEFORE_ROC 113-02-28", date.with(ChronoField.ERA, 0).toString())

        val end = MinguoDate.of(114, 3, 2)
        assertEquals(367L, date.until(end, ChronoUnit.DAYS))
        assertEquals("Minguo P1Y2D", date.until(end).toString())

        val dateTime = date.atTime(LocalTime.of(23, 59, 59, 999_999_999))
        assertSame(MinguoChronology, dateTime.query(TemporalQueries.chronology()))
        assertEquals(
            "Minguo ROC 113-03-01T00:00",
            dateTime.plus(1, ChronoUnit.NANOS).toString(),
        )
        val zoned = dateTime.atZone(ZoneOffset.ofHours(2))
        assertSame(MinguoChronology, zoned.chronology)
        assertEquals("2024-02-29T21:59:59.999999999Z", zoned.toInstant().toString())
    }
}
