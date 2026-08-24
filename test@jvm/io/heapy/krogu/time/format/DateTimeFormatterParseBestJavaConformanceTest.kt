package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.ZonedDateTime
import io.heapy.krogu.time.temporal.TemporalQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterParseBestJavaConformanceTest {
    @Test
    fun bestMatchSelectionMatchesJavaTime() {
        val texts = listOf(
            "2024-02-29T12:30",
            "2024-02-29T12:30+02:00",
            "2024-02-29T12:30+01:00[Europe/Paris]",
        )
        texts.forEach { text ->
            val expected = java.time.format.DateTimeFormatter.ISO_DATE_TIME.parseBest(
                text,
                java.time.ZonedDateTime::from,
                java.time.OffsetDateTime::from,
                java.time.LocalDateTime::from,
            )
            val actual = DateTimeFormatter.ISO_DATE_TIME.parseBest(
                text,
                TemporalQuery(ZonedDateTime::from),
                TemporalQuery(OffsetDateTime::from),
                TemporalQuery(LocalDateTime::from),
            )
            assertEquals(expected.toString(), actual.toString(), text)
        }
    }
}
