package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalQueries
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalQueries as JavaTemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalQueriesJavaConformanceTest {
    @Test
    fun localDateLocalTimeAndPrecisionQueriesMatchJavaTime() {
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
    }
}
