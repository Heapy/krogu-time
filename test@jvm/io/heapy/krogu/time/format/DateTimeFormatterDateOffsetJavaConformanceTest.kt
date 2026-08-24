package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterDateOffsetJavaConformanceTest {
    @Test
    fun requiredOffsetDateMatchesJavaTime() {
        val texts = listOf(
            "-0001-01-01Z",
            "2024-02-29+02:30",
            "+12345-12-31-08:00:30",
        )
        texts.forEach { text ->
            val javaParsed = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE.parse(text)
            val parsed = DateTimeFormatter.ISO_OFFSET_DATE.parse(text)
            assertEquals(java.time.LocalDate.from(javaParsed).toString(), LocalDate.from(parsed).toString(), text)
            assertEquals(java.time.ZoneOffset.from(javaParsed).toString(), ZoneOffset.from(parsed).toString(), text)

            val offsetDateTime = OffsetDateTime.parse(
                LocalDate.from(parsed).toString() + "T12:30" + ZoneOffset.from(parsed),
            )
            val javaOffsetDateTime = java.time.OffsetDateTime.parse(
                java.time.LocalDate.from(javaParsed).toString() +
                    "T12:30" + java.time.ZoneOffset.from(javaParsed),
            )
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_OFFSET_DATE.format(javaOffsetDateTime),
                DateTimeFormatter.ISO_OFFSET_DATE.format(offsetDateTime),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE.toString(),
            DateTimeFormatter.ISO_OFFSET_DATE.toString(),
        )
    }

    @Test
    fun optionalOffsetDateMatchesJavaTime() {
        val texts = listOf(
            "2024-02-29",
            "2024-02-29Z",
            "+12345-12-31+08:00:30",
        )
        texts.forEach { text ->
            val javaParsed = java.time.format.DateTimeFormatter.ISO_DATE.parse(text)
            val parsed = DateTimeFormatter.ISO_DATE.parse(text)
            assertEquals(java.time.LocalDate.from(javaParsed).toString(), LocalDate.from(parsed).toString(), text)
            assertEquals(
                javaParsed.query(java.time.temporal.TemporalQueries.offset())?.toString(),
                parsed.query(TemporalQueries.offset())?.toString(),
                text,
            )
        }
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_DATE.toString(),
            DateTimeFormatter.ISO_DATE.toString(),
        )
    }
}
