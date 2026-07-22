package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterAlternativeDateJavaConformanceTest {
    @Test
    fun ordinalDateMatchesJavaTime() {
        assertAlternativeDateFormatter(
            java.time.format.DateTimeFormatter.ISO_ORDINAL_DATE,
            DateTimeFormatter.ISO_ORDINAL_DATE,
            listOf(
                "-0001-001",
                "2024-060+02:30",
                "+12345-365-08:00:30",
            ),
        )
    }

    @Test
    fun weekDateMatchesJavaTime() {
        assertAlternativeDateFormatter(
            java.time.format.DateTimeFormatter.ISO_WEEK_DATE,
            DateTimeFormatter.ISO_WEEK_DATE,
            listOf(
                "-0001-W01-1",
                "2020-W01-1+02:30",
                "2020-W53-7-08:00:30",
                "+12345-W52-7",
            ),
        )
    }

    @Test
    fun basicDateMatchesJavaTime() {
        assertAlternativeDateFormatter(
            java.time.format.DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.BASIC_ISO_DATE,
            listOf(
                "00000101",
                "20240229+02",
                "20240229+0230",
                "20240229-080030",
            ),
        )
    }

    private fun assertAlternativeDateFormatter(
        javaFormatter: java.time.format.DateTimeFormatter,
        formatter: DateTimeFormatter,
        texts: List<String>,
    ) {
        texts.forEach { text ->
            val javaParsed = javaFormatter.parse(text)
            val parsed = formatter.parse(text)
            assertEquals(java.time.LocalDate.from(javaParsed).toString(), LocalDate.from(parsed).toString(), text)
            assertEquals(
                javaParsed.query(java.time.temporal.TemporalQueries.offset())?.toString(),
                parsed.query(TemporalQueries.offset())?.toString(),
                text,
            )

            val offsetText = parsed.query(TemporalQueries.offset())?.toString() ?: "Z"
            val actualTemporal = OffsetDateTime.parse("${LocalDate.from(parsed)}T12:30$offsetText")
            val expectedTemporal = java.time.OffsetDateTime.parse(
                "${java.time.LocalDate.from(javaParsed)}T12:30$offsetText",
            )
            assertEquals(
                javaFormatter.format(expectedTemporal),
                formatter.format(actualTemporal),
                text,
            )
        }
        assertEquals(javaFormatter.toString(), formatter.toString())
    }
}
