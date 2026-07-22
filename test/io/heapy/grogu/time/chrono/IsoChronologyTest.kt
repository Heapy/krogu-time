package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Period
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IsoChronologyTest {
    @Test
    fun exposesIdentityLookupAndIsoMetadata() {
        assertEquals("ISO", IsoChronology.id)
        assertEquals("iso8601", IsoChronology.calendarType)
        assertEquals("ISO", IsoChronology.toString())
        assertTrue(IsoChronology.isIsoBased)
        assertSame(IsoChronology, Chronology.of("ISO"))
        assertSame(IsoChronology, Chronology.of("iso8601"))
        assertEquals(
            setOf(
                IsoChronology,
                JapaneseChronology,
                HijrahChronology,
                MinguoChronology,
                ThaiBuddhistChronology,
            ),
            Chronology.getAvailableChronologies(),
        )
        assertSame(IsoChronology, Chronology.from(LocalDate.of(2024, 6, 1)))
        assertSame(IsoChronology, Chronology.from(LocalTime.NOON))
        assertFailsWith<DateTimeException> { Chronology.of("Unknown") }
    }

    @Test
    fun createsIsoDatesDateTimesAndPeriods() {
        val date = LocalDate.of(2024, 2, 29)
        val dateTime = LocalDateTime.of(date, LocalTime.of(12, 30))

        assertEquals(date, IsoChronology.date(2024, 2, 29))
        assertEquals(LocalDate.of(0, 1, 1), IsoChronology.date(IsoEra.BCE, 1, 1, 1))
        assertEquals(date, IsoChronology.dateYearDay(2024, 60))
        assertEquals(date, IsoChronology.dateEpochDay(date.toEpochDay()))
        assertEquals(date, IsoChronology.date(dateTime))
        assertEquals(dateTime, IsoChronology.localDateTime(dateTime))
        assertEquals(
            ZonedDateTime.of(dateTime, ZoneOffset.UTC),
            IsoChronology.zonedDateTime(dateTime.atZone(ZoneOffset.UTC)),
        )
        assertEquals(
            ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC),
            IsoChronology.zonedDateTime(Instant.EPOCH, ZoneOffset.UTC),
        )
        assertEquals(Period.of(1, 2, 3), IsoChronology.period(1, 2, 3))
    }

    @Test
    fun handlesEraLeapRangeEpochAndClockOperations() {
        assertTrue(IsoChronology.isLeapYear(2000))
        assertEquals(2024, IsoChronology.prolepticYear(IsoEra.CE, 2024))
        assertEquals(-2023, IsoChronology.prolepticYear(IsoEra.BCE, 2024))
        assertSame(IsoEra.BCE, IsoChronology.eraOf(0))
        assertEquals(listOf(IsoEra.BCE, IsoEra.CE), IsoChronology.eras())
        assertEquals(ChronoField.YEAR.range, IsoChronology.range(ChronoField.YEAR))
        assertFailsWith<ClassCastException> { IsoChronology.prolepticYear(OtherEra, 1) }

        val offset = ZoneOffset.ofHours(2)
        val expectedEpochSecond = LocalDateTime.of(2024, 2, 29, 12, 30).toEpochSecond(offset)
        assertEquals(
            expectedEpochSecond,
            IsoChronology.epochSecond(2024, 2, 29, 12, 30, 0, offset),
        )
        assertEquals(
            expectedEpochSecond,
            IsoChronology.epochSecond(IsoEra.CE, 2024, 2, 29, 12, 30, 0, offset),
        )

        val fixed = Clock.fixed(Instant.parse("2024-02-29T23:30:00Z"), offset)
        assertEquals(LocalDate.of(2024, 3, 1), IsoChronology.dateNow(fixed))
    }

    private data object OtherEra : Era {
        override val value: Int = 1
    }
}
