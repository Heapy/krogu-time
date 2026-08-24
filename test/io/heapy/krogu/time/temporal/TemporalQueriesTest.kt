package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.MonthDay
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.OffsetTime
import io.heapy.krogu.time.Year
import io.heapy.krogu.time.YearMonth
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.ZonedDateTime
import io.heapy.krogu.time.chrono.IsoChronology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class TemporalQueriesTest {
    @Test
    fun extractsLocalDatesAndTimesFromSupportedFields() {
        val date = LocalDate.of(2024, 6, 1)
        val time = LocalTime.of(12, 30, 45, 123_456_789)
        val dateTime = LocalDateTime.of(date, time)
        val offsetDateTime = OffsetDateTime.of(dateTime, ZoneOffset.ofHours(2))
        val zonedDateTime = ZonedDateTime.of(dateTime, ZoneOffset.ofHours(2))

        assertSame(date, date.query(TemporalQueries.localDate()))
        assertSame(time, time.query(TemporalQueries.localTime()))
        assertSame(date, dateTime.query(TemporalQueries.localDate()))
        assertSame(time, dateTime.query(TemporalQueries.localTime()))
        assertSame(date, offsetDateTime.query(TemporalQueries.localDate()))
        assertSame(time, offsetDateTime.query(TemporalQueries.localTime()))
        assertSame(date, zonedDateTime.query(TemporalQueries.localDate()))
        assertSame(time, zonedDateTime.query(TemporalQueries.localTime()))
        assertNull(date.query(TemporalQueries.localTime()))
        assertNull(time.query(TemporalQueries.localDate()))
        assertNull(Instant.EPOCH.query(TemporalQueries.localDate()))
    }

    @Test
    fun reportsTheSmallestSupportedStandardUnit() {
        val date = LocalDate.of(2024, 6, 1)
        val time = LocalTime.NOON
        val dateTime = LocalDateTime.of(date, time)
        assertEquals(ChronoUnit.DAYS, date.query(TemporalQueries.precision()))
        assertEquals(ChronoUnit.NANOS, time.query(TemporalQueries.precision()))
        assertEquals(ChronoUnit.NANOS, dateTime.query(TemporalQueries.precision()))
        assertEquals(ChronoUnit.NANOS, OffsetTime.of(time, ZoneOffset.UTC).query(TemporalQueries.precision()))
        assertEquals(
            ChronoUnit.NANOS,
            OffsetDateTime.of(dateTime, ZoneOffset.UTC).query(TemporalQueries.precision()),
        )
        assertEquals(
            ChronoUnit.NANOS,
            ZonedDateTime.of(dateTime, ZoneOffset.UTC).query(TemporalQueries.precision()),
        )
        assertEquals(ChronoUnit.NANOS, Instant.EPOCH.query(TemporalQueries.precision()))
        assertEquals(ChronoUnit.YEARS, Year.of(2024).query(TemporalQueries.precision()))
        assertEquals(ChronoUnit.MONTHS, YearMonth.of(2024, 6).query(TemporalQueries.precision()))
        assertNull(MonthDay.of(6, 1).query(TemporalQueries.precision()))
        assertNull(ZoneOffset.UTC.query(TemporalQueries.precision()))
    }

    @Test
    fun querySingletonsHaveJavaCompatibleNames() {
        assertEquals("Chronology", TemporalQueries.chronology().toString())
        assertEquals("LocalDate", TemporalQueries.localDate().toString())
        assertEquals("LocalTime", TemporalQueries.localTime().toString())
        assertEquals("Precision", TemporalQueries.precision().toString())
        assertEquals("ZoneId", TemporalQueries.zoneId().toString())
        assertEquals("ZoneOffset", TemporalQueries.offset().toString())
        assertEquals("Zone", TemporalQueries.zone().toString())
        assertEquals(
            ChronoUnit.NANOS,
            TemporalQueries.precision().queryFrom(Instant.EPOCH),
        )
    }

    @Test
    fun strictZoneQueryDelegatesBackToTheTemporal() {
        val zone = ZoneId.of("Europe/Paris")
        val zoned = ZonedDateTime.of(LocalDateTime.of(2024, 6, 1, 12, 30), zone)

        assertSame(zone, TemporalQueries.zoneId().queryFrom(zoned))
        assertNull(TemporalQueries.zoneId().queryFrom(LocalDate.of(2024, 6, 1)))
    }

    @Test
    fun reportsIsoChronologyOnlyForIsoCalendarTypes() {
        val date = LocalDate.of(2024, 6, 1)
        val time = LocalTime.NOON
        val dateTime = LocalDateTime.of(date, time)

        assertSame(IsoChronology, date.query(TemporalQueries.chronology()))
        assertSame(IsoChronology, dateTime.query(TemporalQueries.chronology()))
        assertSame(IsoChronology, Year.of(2024).query(TemporalQueries.chronology()))
        assertSame(IsoChronology, YearMonth.of(2024, 6).query(TemporalQueries.chronology()))
        assertSame(IsoChronology, MonthDay.of(6, 1).query(TemporalQueries.chronology()))
        assertSame(
            IsoChronology,
            OffsetDateTime.of(dateTime, ZoneOffset.UTC).query(TemporalQueries.chronology()),
        )
        assertSame(
            IsoChronology,
            ZonedDateTime.of(dateTime, ZoneOffset.UTC).query(TemporalQueries.chronology()),
        )
        assertNull(time.query(TemporalQueries.chronology()))
        assertNull(OffsetTime.of(time, ZoneOffset.UTC).query(TemporalQueries.chronology()))
        assertNull(Instant.EPOCH.query(TemporalQueries.chronology()))
        assertNull(ZoneOffset.UTC.query(TemporalQueries.chronology()))
    }
}
