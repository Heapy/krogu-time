package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterAlternativeDateTest {
    @Test
    fun ordinalDatesUseYearAndDayOfYear() {
        val date = LocalDate.of(2024, 2, 29)
        val offsetDateTime = OffsetDateTime.parse("2024-02-29T12:30+02:30")

        assertEquals("2024-060", DateTimeFormatter.ISO_ORDINAL_DATE.format(date))
        assertEquals(
            "2024-060+02:30",
            DateTimeFormatter.ISO_ORDINAL_DATE.format(offsetDateTime),
        )
        assertEquals(
            date,
            LocalDate.parse("2024-060", DateTimeFormatter.ISO_ORDINAL_DATE),
        )
        assertEquals(
            ZoneOffset.ofHoursMinutes(2, 30),
            DateTimeFormatter.ISO_ORDINAL_DATE.parse("2024-060+02:30")
                .query(TemporalQueries.offset()),
        )
    }

    @Test
    fun weekDatesResolveAcrossCalendarYearBoundaries() {
        assertEquals(
            "2020-W01-1",
            DateTimeFormatter.ISO_WEEK_DATE.format(LocalDate.of(2019, 12, 30)),
        )
        assertEquals(
            "2020-W53-7",
            DateTimeFormatter.ISO_WEEK_DATE.format(LocalDate.of(2021, 1, 3)),
        )
        assertEquals(
            LocalDate.of(2019, 12, 30),
            LocalDate.parse("2020-W01-1", DateTimeFormatter.ISO_WEEK_DATE),
        )
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_WEEK_DATE.parse("2021-W53-1")
        }
    }

    @Test
    fun basicDatesUseCompactFieldsAndOffsets() {
        val date = LocalDate.of(2024, 2, 29)
        val offsetDateTime = OffsetDateTime.parse("2024-02-29T12:30+02:00")

        assertEquals("20240229", DateTimeFormatter.BASIC_ISO_DATE.format(date))
        assertEquals("20240229+0200", DateTimeFormatter.BASIC_ISO_DATE.format(offsetDateTime))
        assertEquals(date, LocalDate.parse("20240229", DateTimeFormatter.BASIC_ISO_DATE))
        assertEquals(
            ZoneOffset.ofHours(2),
            DateTimeFormatter.BASIC_ISO_DATE.parse("20240229+02")
                .query(TemporalQueries.offset()),
        )
    }
}
