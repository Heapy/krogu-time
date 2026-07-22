package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterRfc1123JavaConformanceTest {
    @Test
    fun parsingFormattingAndDescriptionMatchJavaTime() {
        val texts = listOf(
            "Tue, 3 Jun 2008 11:05:30 GMT",
            "3 Jun 2008 11:05 +02",
            "Thu, 29 Feb 2024 12:30:45 +0230",
            "sun, 27 oct 2024 02:30 +0200",
        )
        texts.forEach { text ->
            val javaParsed = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(text)
            val parsed = DateTimeFormatter.RFC_1123_DATE_TIME.parse(text)
            assertEquals(
                java.time.LocalDateTime.from(javaParsed).toString(),
                LocalDateTime.from(parsed).toString(),
                text,
            )
            assertEquals(
                java.time.ZoneOffset.from(javaParsed).toString(),
                parsed.query(TemporalQueries.offset()).toString(),
                text,
            )

            val expected = java.time.OffsetDateTime.from(javaParsed)
            val actual = OffsetDateTime.from(parsed)
            assertEquals(
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(expected),
                DateTimeFormatter.RFC_1123_DATE_TIME.format(actual),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.toString(),
            DateTimeFormatter.RFC_1123_DATE_TIME.toString(),
        )
    }
}
