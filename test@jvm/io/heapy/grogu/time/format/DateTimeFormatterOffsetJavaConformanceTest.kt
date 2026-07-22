package io.heapy.grogu.time.format

import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.OffsetTime
import io.heapy.grogu.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterOffsetJavaConformanceTest {
    @Test
    fun offsetTimeFormatterMatchesJavaTime() {
        val texts = listOf(
            "00:00Z",
            "12:30:45.120000000+02:30",
            "23:59:59.123456789-08:00:30",
        )
        texts.forEach { text ->
            val expected = java.time.OffsetTime.parse(text)
            val actual = OffsetTime.parse(text)
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_OFFSET_TIME.format(expected),
                DateTimeFormatter.ISO_OFFSET_TIME.format(actual),
                text,
            )
            assertEquals(
                expected.toString(),
                OffsetTime.parse(text, DateTimeFormatter.ISO_OFFSET_TIME).toString(),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_OFFSET_TIME.toString(),
            DateTimeFormatter.ISO_OFFSET_TIME.toString(),
        )
    }

    @Test
    fun offsetDateTimeFormatterMatchesJavaTime() {
        val texts = listOf(
            "-0001-01-01T00:00Z",
            "2024-02-29T12:30:45.120000000+02:30",
            "+12345-12-31T23:59:59.123456789-08:00:30",
        )
        texts.forEach { text ->
            val expected = java.time.OffsetDateTime.parse(text)
            val actual = OffsetDateTime.parse(text)
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expected),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(actual),
                text,
            )
            assertEquals(
                expected.toString(),
                OffsetDateTime.parse(
                    text,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                ).toString(),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.toString(),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.toString(),
        )
    }

    @Test
    fun zonedDateTimeFormatterMatchesJavaTime() {
        val texts = listOf(
            "2024-02-29T12:30Z",
            "2024-02-29T12:30+01:00[Europe/Paris]",
            "2024-10-27T02:30+02:00[Europe/Paris]",
        )
        texts.forEach { text ->
            val expected = java.time.ZonedDateTime.parse(text)
            val actual = ZonedDateTime.parse(text)
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME.format(expected),
                DateTimeFormatter.ISO_ZONED_DATE_TIME.format(actual),
                text,
            )
            assertEquals(
                expected.toString(),
                ZonedDateTime.parse(
                    text,
                    DateTimeFormatter.ISO_ZONED_DATE_TIME,
                ).toString(),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME.toString(),
            DateTimeFormatter.ISO_ZONED_DATE_TIME.toString(),
        )
    }
}
