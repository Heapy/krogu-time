package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateTimeFormatterOptionalCompositeTest {
    @Test
    fun isoTimeFormatsAndParsesOptionalOffsets() {
        val time = LocalTime.of(12, 30)
        val offsetTime = OffsetTime.parse("12:30:45.123400000+02:30")

        assertEquals("12:30:00", DateTimeFormatter.ISO_TIME.format(time))
        assertEquals("12:30:45.1234+02:30", DateTimeFormatter.ISO_TIME.format(offsetTime))

        val localParsed = DateTimeFormatter.ISO_TIME.parse("12:30")
        assertEquals(time, LocalTime.from(localParsed))
        assertNull(localParsed.query(TemporalQueries.offset()))

        val offsetParsed = DateTimeFormatter.ISO_TIME.parse("12:30:45.1234+02:30")
        assertEquals(offsetTime, OffsetTime.from(offsetParsed))
        assertEquals(ZoneOffset.ofHoursMinutes(2, 30), offsetParsed.query(TemporalQueries.offset()))

        assertEquals(time, LocalTime.parse("12:30+02:30", DateTimeFormatter.ISO_TIME))
        assertEquals(
            OffsetTime.parse("12:30+02:30"),
            OffsetTime.parse("12:30+02:30", DateTimeFormatter.ISO_TIME),
        )
    }

    @Test
    fun isoDateTimeFormatsEveryOptionalLevel() {
        val local = LocalDateTime.of(
            LocalDate.of(2024, 2, 29),
            LocalTime.of(12, 30),
        )
        val offset = OffsetDateTime.parse("2024-02-29T12:30+02:30")
        val zoned = ZonedDateTime.parse("2024-02-29T12:30+01:00[Europe/Paris]")

        assertEquals("2024-02-29T12:30:00", DateTimeFormatter.ISO_DATE_TIME.format(local))
        assertEquals(
            "2024-02-29T12:30:00+02:30",
            DateTimeFormatter.ISO_DATE_TIME.format(offset),
        )
        assertEquals(
            "2024-02-29T12:30:00+01:00[Europe/Paris]",
            DateTimeFormatter.ISO_DATE_TIME.format(zoned),
        )
    }

    @Test
    fun isoDateTimeParsingRetainsOptionalOffsetAndRegionZone() {
        val localParsed = DateTimeFormatter.ISO_DATE_TIME.parse("2024-02-29T12:30")
        assertEquals(
            LocalDateTime.parse("2024-02-29T12:30"),
            LocalDateTime.from(localParsed),
        )
        assertNull(localParsed.query(TemporalQueries.offset()))
        assertNull(localParsed.query(TemporalQueries.zoneId()))

        val offsetParsed = DateTimeFormatter.ISO_DATE_TIME.parse("2024-02-29T12:30+02:30")
        assertEquals(
            OffsetDateTime.parse("2024-02-29T12:30+02:30"),
            OffsetDateTime.from(offsetParsed),
        )
        assertNull(offsetParsed.query(TemporalQueries.zoneId()))

        val zonedParsed = DateTimeFormatter.ISO_DATE_TIME.parse(
            "2024-02-29T12:30+01:00[Europe/Paris]",
        )
        assertEquals(
            ZonedDateTime.parse("2024-02-29T12:30+01:00[Europe/Paris]"),
            ZonedDateTime.from(zonedParsed),
        )
        assertEquals(ZoneId.of("Europe/Paris"), zonedParsed.query(TemporalQueries.zoneId()))
    }
}
