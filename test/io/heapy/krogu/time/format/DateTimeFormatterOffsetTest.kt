package io.heapy.krogu.time.format

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.OffsetTime
import io.heapy.krogu.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterOffsetTest {
    @Test
    fun formatsAndParsesOffsetAndZonedTypes() {
        val offsetTime = OffsetTime.parse("12:30:45.123400000+02:30")
        val offsetDateTime = OffsetDateTime.parse("2024-02-29T12:30:45.123400000+02:30")
        val zonedDateTime = ZonedDateTime.parse(
            "2024-02-29T12:30:45.123400000+01:00[Europe/Paris]",
        )

        assertEquals(
            "12:30:45.1234+02:30",
            DateTimeFormatter.ISO_OFFSET_TIME.format(offsetTime),
        )
        assertEquals(
            "2024-02-29T12:30:45.1234+02:30",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime),
        )
        assertEquals(
            "2024-02-29T12:30:45.1234+01:00[Europe/Paris]",
            DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zonedDateTime),
        )

        assertEquals(
            offsetTime,
            OffsetTime.from(DateTimeFormatter.ISO_OFFSET_TIME.parse(offsetTime.toString())),
        )
        assertEquals(
            offsetDateTime,
            OffsetDateTime.from(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(offsetDateTime.toString()),
            ),
        )
        assertEquals(
            zonedDateTime,
            ZonedDateTime.from(
                DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(zonedDateTime.toString()),
            ),
        )
    }

    @Test
    fun supportsValueTypeFormatterOverloads() {
        val offsetTime = OffsetTime.parse("12:30+02:00")
        val offsetDateTime = OffsetDateTime.parse("2024-02-29T12:30+02:00")
        val zonedDateTime = ZonedDateTime.parse("2024-02-29T12:30+01:00[Europe/Paris]")

        assertEquals("12:30:00+02:00", offsetTime.format(DateTimeFormatter.ISO_OFFSET_TIME))
        assertEquals(
            "2024-02-29T12:30:00+02:00",
            offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        assertEquals(
            "2024-02-29T12:30:00+01:00[Europe/Paris]",
            zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
        )

        assertEquals(
            offsetTime,
            OffsetTime.parse("12:30+02:00", DateTimeFormatter.ISO_OFFSET_TIME),
        )
        assertEquals(
            offsetDateTime,
            OffsetDateTime.parse(
                "2024-02-29T12:30+02:00",
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            ),
        )
        assertEquals(
            zonedDateTime,
            ZonedDateTime.parse(
                "2024-02-29T12:30+01:00[Europe/Paris]",
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
            ),
        )
    }

    @Test
    fun rejectsTemporalsWithoutTheRequiredOffsetOrZone() {
        assertFailsWith<DateTimeException> {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(LocalDate.EPOCH)
        }
        assertFailsWith<DateTimeException> {
            DateTimeFormatter.ISO_ZONED_DATE_TIME.format(LocalDate.EPOCH)
        }
    }
}
