package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterRfc1123Test {
    @Test
    fun formatsEnglishRfc1123DateTimes() {
        assertEquals(
            "Tue, 3 Jun 2008 11:05:30 GMT",
            DateTimeFormatter.RFC_1123_DATE_TIME.format(
                OffsetDateTime.parse("2008-06-03T11:05:30Z"),
            ),
        )
        assertEquals(
            "Thu, 29 Feb 2024 12:30:00 +0230",
            DateTimeFormatter.RFC_1123_DATE_TIME.format(
                OffsetDateTime.parse("2024-02-29T12:30+02:30"),
            ),
        )
    }

    @Test
    fun parsesOptionalWeekdaysAndSecondsCaseInsensitively() {
        val withWeekday = DateTimeFormatter.RFC_1123_DATE_TIME.parse(
            "tue, 3 jun 2008 11:05:30 gmt",
        )
        assertEquals(
            OffsetDateTime.parse("2008-06-03T11:05:30Z"),
            OffsetDateTime.from(withWeekday),
        )

        val withoutOptionalFields = DateTimeFormatter.RFC_1123_DATE_TIME.parse(
            "3 Jun 2008 11:05 +02",
        )
        assertEquals(
            LocalDateTime.parse("2008-06-03T11:05"),
            LocalDateTime.from(withoutOptionalFields),
        )
        assertEquals(ZoneOffset.ofHours(2), withoutOptionalFields.query(TemporalQueries.offset()))
    }

    @Test
    fun crossChecksAnExplicitWeekday() {
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.RFC_1123_DATE_TIME.parse(
                "Mon, 3 Jun 2008 11:05:30 GMT",
            )
        }
    }
}
