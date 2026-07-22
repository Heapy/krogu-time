package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalQueries
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalQueries as JavaTemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalQueriesJavaConformanceTest {
    @Test
    fun querySingletonNamesMatchJavaTime() {
        val queries = listOf(
            JavaTemporalQueries.chronology() to TemporalQueries.chronology(),
            JavaTemporalQueries.localDate() to TemporalQueries.localDate(),
            JavaTemporalQueries.localTime() to TemporalQueries.localTime(),
            JavaTemporalQueries.precision() to TemporalQueries.precision(),
            JavaTemporalQueries.zoneId() to TemporalQueries.zoneId(),
            JavaTemporalQueries.offset() to TemporalQueries.offset(),
            JavaTemporalQueries.zone() to TemporalQueries.zone(),
        )

        queries.forEach { (expected, actual) ->
            assertEquals(expected.toString(), actual.toString())
        }
    }

    @Test
    fun directStrictZoneQueryMatchesJavaTime() {
        val javaZoned = java.time.ZonedDateTime.parse("2024-06-01T12:30+02:00[Europe/Paris]")
        val zoned = ZonedDateTime.parse("2024-06-01T12:30+02:00[Europe/Paris]")

        assertEquals(
            JavaTemporalQueries.zoneId().queryFrom(javaZoned).toString(),
            TemporalQueries.zoneId().queryFrom(zoned).toString(),
        )
        assertEquals(
            JavaTemporalQueries.zoneId()
                .queryFrom(java.time.LocalDate.of(2024, 6, 1))
                ?.toString(),
            TemporalQueries.zoneId()
                .queryFrom(LocalDate.of(2024, 6, 1))
                ?.toString(),
        )
    }

    @Test
    fun localDateLocalTimePrecisionAndChronologyQueriesMatchJavaTime() {
        val values = listOf(
            java.time.LocalDate.of(2024, 6, 1) to LocalDate.of(2024, 6, 1),
            java.time.LocalTime.of(12, 30, 45, 123_456_789) to
                LocalTime.of(12, 30, 45, 123_456_789),
            java.time.LocalDateTime.of(2024, 6, 1, 12, 30) to
                LocalDateTime.of(2024, 6, 1, 12, 30),
            java.time.OffsetDateTime.parse("2024-06-01T12:30+02:00") to
                OffsetDateTime.parse("2024-06-01T12:30+02:00"),
            java.time.Instant.EPOCH to Instant.EPOCH,
            java.time.Year.of(2024) to Year.of(2024),
            java.time.YearMonth.of(2024, 6) to YearMonth.of(2024, 6),
            java.time.MonthDay.of(6, 1) to MonthDay.of(6, 1),
            java.time.ZoneOffset.UTC to ZoneOffset.UTC,
        )

        values.forEach { (javaValue, value) ->
            assertQueriesEqual(javaValue, value)
        }
    }

    private fun assertQueriesEqual(
        expected: JavaTemporalAccessor,
        actual: TemporalAccessor,
    ) {
        assertEquals(
            expected.query(JavaTemporalQueries.localDate())?.toString(),
            actual.query(TemporalQueries.localDate())?.toString(),
            actual.toString(),
        )
        assertEquals(
            expected.query(JavaTemporalQueries.localTime())?.toString(),
            actual.query(TemporalQueries.localTime())?.toString(),
            actual.toString(),
        )
        assertEquals(
            expected.query(JavaTemporalQueries.precision())?.toString(),
            actual.query(TemporalQueries.precision())?.toString(),
            actual.toString(),
        )
        assertEquals(
            expected.query(JavaTemporalQueries.chronology())?.toString(),
            actual.query(TemporalQueries.chronology())?.toString(),
            actual.toString(),
        )
    }
}
