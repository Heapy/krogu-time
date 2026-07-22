package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderZoneTest {
    @Test
    fun appendsIsoAndCustomOffsets() {
        val builder = DateTimeFormatterBuilder().appendPattern("HH:mm")
        assertSame(builder, builder.appendOffsetId())
        val isoOffset = builder.toFormatter()
        val time = OffsetTime.of(5, 6, 0, 0, ZoneOffset.ofHoursMinutesSeconds(2, 30, 15))

        assertEquals("05:06+02:30:15", isoOffset.format(time))
        assertEquals(time, isoOffset.parse("05:06+02:30:15", OffsetTime::from))

        val custom = DateTimeFormatterBuilder()
            .appendPattern("HH:mm")
            .appendOffset("+H:mm:ss", "UTC")
            .toFormatter()
        assertEquals("05:06+2:30:15", custom.format(time))
        assertEquals(time, custom.parse("05:06+2:30:15", OffsetTime::from))
        assertEquals("05:06UTC", custom.format(time.withOffsetSameLocal(ZoneOffset.UTC)))
    }

    @Test
    fun appendsZoneIdsUsingTheRequestedQueryMode() {
        val region = ZoneId.of("Europe/Paris")
        val zoned = ZonedDateTime.of(LocalDateTime.of(2024, 3, 1, 5, 6), region)
        val offset = OffsetDateTime.of(
            LocalDateTime.of(2024, 3, 1, 5, 6),
            ZoneOffset.ofHoursMinutes(2, 30),
        )
        val zoneId = DateTimeFormatterBuilder().appendZoneId().toFormatter()
        val zoneRegionId = DateTimeFormatterBuilder().appendZoneRegionId().toFormatter()
        val zoneOrOffsetId = DateTimeFormatterBuilder().appendZoneOrOffsetId().toFormatter()

        assertEquals("Europe/Paris", zoneId.format(zoned))
        assertEquals("Europe/Paris", zoneRegionId.format(zoned))
        assertEquals("+02:30", zoneOrOffsetId.format(offset))
        assertFailsWith<DateTimeException> { zoneId.format(offset) }
        assertFailsWith<DateTimeException> { zoneRegionId.format(ZoneOffset.ofHours(2)) }

        listOf(zoneId, zoneRegionId, zoneOrOffsetId).forEach { formatter ->
            assertEquals(region, formatter.parse("Europe/Paris", ZoneId::from))
            assertEquals(
                ZoneOffset.ofHoursMinutes(2, 30),
                formatter.parse("+02:30", ZoneId::from),
            )
        }

        val bracketed = DateTimeFormatterBuilder()
            .appendZoneId()
            .appendLiteral(']')
            .toFormatter()
        assertEquals("Europe/Paris]", bracketed.format(zoned))
        assertEquals(region, bracketed.parse("Europe/Paris]", ZoneId::from))
    }

    @Test
    fun rejectsUnknownOffsetPatterns() {
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatterBuilder().appendOffset("HH:mm", "Z")
        }
    }
}
