package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class DateTimeFormatterZoneOverrideTest {
    @Test
    fun exposesAndCopiesZoneOverridesImmutably() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        assertNull(formatter.zone)
        assertSame(formatter, formatter.withZone(null))

        val paris = ZoneId.of("Europe/Paris")
        val zoned = formatter.withZone(paris)
        assertNotSame(formatter, zoned)
        assertEquals(paris, zoned.zone)
        assertSame(zoned, zoned.withZone(paris))
        assertNull(formatter.zone)
        assertEquals(formatter.toString(), zoned.toString())
    }

    @Test
    fun convertsInstantCapableValuesBeforeFormatting() {
        val instant = Instant.parse("2024-02-29T23:30:00Z")
        val plusTwo = ZoneOffset.ofHours(2)

        assertEquals(
            "2024-03-01",
            DateTimeFormatter.ISO_LOCAL_DATE.withZone(plusTwo).format(instant),
        )
        assertEquals(
            "2024-03-01T01:30:00+02:00",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(plusTwo).format(instant),
        )
        assertEquals(
            "2024-03-01T00:30:00+01:00[Europe/Paris]",
            DateTimeFormatter.ISO_ZONED_DATE_TIME
                .withZone(ZoneId.of("Europe/Paris"))
                .format(instant),
        )
    }

    @Test
    fun appliesOverrideZoneToParsedResultsUnlessTextProvidesOne() {
        val paris = ZoneId.of("Europe/Paris")
        val localParsed = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(paris)
            .parse("2024-02-29T12:30")
        assertEquals(paris, localParsed.query(TemporalQueries.zoneId()))
        assertEquals(
            "2024-02-29T12:30+01:00[Europe/Paris]",
            ZonedDateTime.from(localParsed).toString(),
        )

        val instantParsed = DateTimeFormatter.ISO_INSTANT
            .withZone(paris)
            .parse("2024-02-29T12:30:00Z")
        assertEquals(
            "2024-02-29T13:30+01:00[Europe/Paris]",
            ZonedDateTime.from(instantParsed).toString(),
        )

        val explicit = DateTimeFormatter.ISO_DATE_TIME
            .withZone(ZoneOffset.UTC)
            .parse("2024-02-29T12:30+01:00[Europe/Paris]")
        assertEquals(paris, explicit.query(TemporalQueries.zoneId()))
    }

    @Test
    fun rejectsChangingAnOffsetWithoutAnInstantToADifferentFixedZone() {
        val offsetTime = OffsetTime.parse("12:30+02:00")
        assertFailsWith<DateTimeException> {
            DateTimeFormatter.ISO_OFFSET_TIME
                .withZone(ZoneOffset.UTC)
                .format(offsetTime)
        }
        assertEquals(
            "12:30:00+02:00",
            DateTimeFormatter.ISO_OFFSET_TIME
                .withZone(ZoneOffset.ofHours(2))
                .format(offsetTime),
        )
    }
}
