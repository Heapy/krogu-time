package io.heapy.krogu.time.format

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterJavaConformanceTest {
    @Test
    fun coreIsoConstantsFormatParseAndDescribeLikeJavaTime() {
        val dateTexts = listOf("-0001-01-01", "2024-02-29", "+12345-12-31")
        dateTexts.forEach { text ->
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(java.time.LocalDate.parse(text)),
                DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.parse(text)),
                text,
            )
            assertEquals(
                java.time.LocalDate.parse(text, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).toString(),
                LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).toString(),
                text,
            )
        }

        val timeTexts = listOf("00:00", "12:30:45", "23:59:59.123456789")
        timeTexts.forEach { text ->
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_LOCAL_TIME.format(java.time.LocalTime.parse(text)),
                DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.parse(text)),
                text,
            )
            assertEquals(
                java.time.LocalTime.parse(text, java.time.format.DateTimeFormatter.ISO_LOCAL_TIME).toString(),
                LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME).toString(),
                text,
            )
        }

        val dateTimeText = "2024-02-29T23:59:59.123456789"
        assertEquals(
            java.time.LocalDateTime.parse(
                dateTimeText,
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            ).toString(),
            LocalDateTime.parse(dateTimeText, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString(),
        )

        val instantTexts = listOf(
            "1970-01-01T00:00:00Z",
            "2024-02-29T23:59:59.123456789Z",
            "+10000-01-01T00:00:00Z",
        )
        instantTexts.forEach { text ->
            assertEquals(
                java.time.Instant.parse(text).toString(),
                Instant.parse(text, DateTimeFormatter.ISO_INSTANT).toString(),
                text,
            )
        }

        assertEquals(
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toString(),
            DateTimeFormatter.ISO_LOCAL_DATE.toString(),
        )
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_LOCAL_TIME.toString(),
            DateTimeFormatter.ISO_LOCAL_TIME.toString(),
        )
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.toString(),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.toString(),
        )
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_INSTANT.toString(),
            DateTimeFormatter.ISO_INSTANT.toString(),
        )
    }
}
