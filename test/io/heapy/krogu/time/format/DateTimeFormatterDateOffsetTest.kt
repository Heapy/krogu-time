package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DateTimeFormatterDateOffsetTest {
    @Test
    fun formatsDatesWithRequiredOrOptionalOffsets() {
        val date = LocalDate.of(2024, 2, 29)
        val offsetDateTime = OffsetDateTime.parse("2024-02-29T12:30+02:30")

        assertEquals(
            "2024-02-29+02:30",
            DateTimeFormatter.ISO_OFFSET_DATE.format(offsetDateTime),
        )
        assertEquals("2024-02-29", DateTimeFormatter.ISO_DATE.format(date))
        assertEquals("2024-02-29+02:30", DateTimeFormatter.ISO_DATE.format(offsetDateTime))
    }

    @Test
    fun parsedAccessorsRetainDatesAndOptionalOffsets() {
        val withOffset = DateTimeFormatter.ISO_DATE.parse("2024-02-29+02:30")
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(withOffset))
        assertEquals(ZoneOffset.ofHoursMinutes(2, 30), ZoneOffset.from(withOffset))
        assertEquals(
            ZoneOffset.ofHoursMinutes(2, 30),
            withOffset.query(TemporalQueries.offset()),
        )

        val withoutOffset = DateTimeFormatter.ISO_DATE.parse("2024-02-29")
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(withoutOffset))
        assertNull(withoutOffset.query(TemporalQueries.offset()))

        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.parse("2024-02-29+02:30", DateTimeFormatter.ISO_OFFSET_DATE),
        )
    }

    @Test
    fun requiredOffsetDateRejectsMissingOffsets() {
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_OFFSET_DATE.parse("2024-02-29")
        }
    }
}
