package io.heapy.krogu.time.format

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.OffsetDateTime
import io.heapy.krogu.time.ZonedDateTime
import io.heapy.krogu.time.temporal.TemporalQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DateTimeFormatterParseBestTest {
    @Test
    fun returnsTheFirstQueryThatCanConvertTheParsedResult() {
        val zoned = DateTimeFormatter.ISO_DATE_TIME.parseBest(
            "2024-02-29T12:30+01:00[Europe/Paris]",
            TemporalQuery(ZonedDateTime::from),
            TemporalQuery(OffsetDateTime::from),
            TemporalQuery(LocalDateTime::from),
        )
        assertIs<ZonedDateTime>(zoned)
        assertEquals("Europe/Paris", zoned.zone.id)

        val offset = DateTimeFormatter.ISO_DATE_TIME.parseBest(
            "2024-02-29T12:30+02:00",
            TemporalQuery(OffsetDateTime::from),
            TemporalQuery(LocalDateTime::from),
        )
        assertIs<OffsetDateTime>(offset)

        val local = DateTimeFormatter.ISO_DATE_TIME.parseBest(
            "2024-02-29T12:30",
            TemporalQuery(OffsetDateTime::from),
            TemporalQuery(LocalDateTime::from),
        )
        assertIs<LocalDateTime>(local)
    }

    @Test
    fun validatesQueriesAndWrapsConversionFailures() {
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatter.ISO_DATE.parseBest(
                "2024-02-29",
                TemporalQuery(LocalDate::from),
            )
        }
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_DATE.parseBest(
                "2024-02-29",
                TemporalQuery(Instant::from),
                TemporalQuery(LocalTime::from),
            )
        }
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_DATE.parse(
                "2024-02-29",
                TemporalQuery(LocalTime::from),
            )
        }
    }
}
