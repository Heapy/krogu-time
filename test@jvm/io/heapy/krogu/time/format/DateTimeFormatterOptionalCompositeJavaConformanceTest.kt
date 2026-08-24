package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterOptionalCompositeJavaConformanceTest {
    @Test
    fun optionalOffsetTimeMatchesJavaTime() {
        val texts = listOf(
            "00:00",
            "12:30:45.120000000+02:30",
            "23:59:59.123456789-08:00:30",
        )
        texts.forEach { text ->
            val javaParsed = java.time.format.DateTimeFormatter.ISO_TIME.parse(text)
            val parsed = DateTimeFormatter.ISO_TIME.parse(text)
            assertEquals(java.time.LocalTime.from(javaParsed).toString(), LocalTime.from(parsed).toString(), text)
            assertEquals(
                javaParsed.query(java.time.temporal.TemporalQueries.offset())?.toString(),
                parsed.query(TemporalQueries.offset())?.toString(),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_TIME.toString(),
            DateTimeFormatter.ISO_TIME.toString(),
        )
    }

    @Test
    fun optionalOffsetAndZoneDateTimeMatchesJavaTime() {
        val texts = listOf(
            "-0001-01-01T00:00",
            "2024-02-29T12:30:45.120000000+02:30",
            "+12345-12-31T23:59:59.123456789-08:00:30[UTC]",
            "2024-10-27T02:30+02:00[Europe/Paris]",
        )
        texts.forEach { text ->
            val javaParsed = java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(text)
            val parsed = DateTimeFormatter.ISO_DATE_TIME.parse(text)
            assertEquals(
                java.time.LocalDateTime.from(javaParsed).toString(),
                LocalDateTime.from(parsed).toString(),
                text,
            )
            assertEquals(
                javaParsed.query(java.time.temporal.TemporalQueries.offset())?.toString(),
                parsed.query(TemporalQueries.offset())?.toString(),
                text,
            )
            assertEquals(
                javaParsed.query(java.time.temporal.TemporalQueries.zoneId())?.toString(),
                parsed.query(TemporalQueries.zoneId())?.toString(),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_DATE_TIME.toString(),
            DateTimeFormatter.ISO_DATE_TIME.toString(),
        )
    }
}
